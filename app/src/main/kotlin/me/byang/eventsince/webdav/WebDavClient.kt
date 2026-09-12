/*
 * Copyright 2026 Boyuan Yang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.byang.eventsince.webdav

import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.xml.parsers.DocumentBuilderFactory

data class WebDavConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
    val trustAllCertificates: Boolean = false,
)

data class RemoteFile(
    val name: String,
    /** Path relative to the server root, as returned in the PROPFIND href. */
    val href: String,
    val isDirectory: Boolean,
    val size: Long?,
    val lastModified: Long?,
)

class WebDavException(val status: Int, message: String) : IOException(message)

/**
 * Minimal WebDAV client over OkHttp: enough to keep a folder of backup files.
 * Paths passed in are relative to [WebDavConfig.baseUrl].
 */
class WebDavClient(private val config: WebDavConfig, client: OkHttpClient? = null) {

    private val http: OkHttpClient = (client ?: OkHttpClient()).newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .apply { if (config.trustAllCertificates) trustAll() }
        .build()

    private val base: HttpUrl = normaliseBase(config.baseUrl)
    private val auth: String = Credentials.basic(config.username, config.password)

    fun url(path: String): HttpUrl {
        val builder = base.newBuilder()
        path.trim('/').split('/').filter { it.isNotEmpty() }.forEach { builder.addPathSegment(it) }
        if (path.endsWith("/")) builder.addPathSegment("")
        return builder.build()
    }

    /** PROPFIND depth 0: succeeds when the resource exists and the credentials are accepted. */
    @Throws(IOException::class)
    fun exists(path: String): Boolean {
        execute(request(path).method("PROPFIND", PROPFIND_BODY.toRequestBody(XML)).header("Depth", "0"), allow = setOf(404)).use {
            return it.code != 404
        }
    }

    /** PROPFIND depth 1: the entries of a directory, excluding the directory itself. */
    @Throws(IOException::class)
    fun list(dirPath: String): List<RemoteFile> {
        val dir = dirPath.trimEnd('/') + "/"
        execute(request(dir).method("PROPFIND", PROPFIND_BODY.toRequestBody(XML)).header("Depth", "1")).use { response ->
            val body = response.body.string()
            val selfPath = url(dir).encodedPath.trimEnd('/')
            return parseMultiStatus(body).filter { it.href.trimEnd('/') != selfPath }
        }
    }

    /** Creates a directory; an existing one (405) is not an error. */
    @Throws(IOException::class)
    fun mkcol(dirPath: String) {
        val dir = dirPath.trimEnd('/') + "/"
        execute(request(dir).method("MKCOL", null), allow = setOf(405)).close()
    }

    /** Creates every missing segment of [dirPath]. */
    @Throws(IOException::class)
    fun ensureDirectory(dirPath: String) {
        val segments = dirPath.trim('/').split('/').filter { it.isNotEmpty() }
        var current = ""
        segments.forEach { segment ->
            current += "$segment/"
            if (!exists(current)) mkcol(current)
        }
    }

    @Throws(IOException::class)
    fun put(path: String, bytes: ByteArray, contentType: String = "application/json") {
        execute(request(path).put(bytes.toRequestBody(contentType.toMediaType()))).close()
    }

    @Throws(IOException::class)
    fun get(path: String): ByteArray = execute(request(path).get()).use { it.body.bytes() }

    @Throws(IOException::class)
    fun delete(path: String) {
        execute(request(path).delete(), allow = setOf(404)).close()
    }

    private fun request(path: String): Request.Builder =
        Request.Builder().url(url(path)).header("Authorization", auth).header("User-Agent", "EventSince")

    @Throws(IOException::class)
    private fun execute(builder: Request.Builder, allow: Set<Int> = emptySet()): Response {
        val response = http.newCall(builder.build()).execute()
        if (response.isSuccessful || response.code in allow) return response
        val message = response.message.ifBlank { statusText(response.code) }
        response.close()
        throw WebDavException(response.code, "HTTP ${response.code} $message")
    }

    companion object {
        private val XML = "application/xml; charset=utf-8".toMediaType()
        private const val PROPFIND_BODY =
            """<?xml version="1.0" encoding="utf-8"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/><d:getcontentlength/><d:getlastmodified/><d:displayname/></d:prop></d:propfind>"""

        fun normaliseBase(raw: String): HttpUrl {
            val text = raw.trim().let { if (it.contains("://")) it else "https://$it" }.trimEnd('/') + "/"
            return text.toHttpUrlOrNull() ?: throw IllegalArgumentException("invalid URL: $raw")
        }

        fun statusText(code: Int): String = when (code) {
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            409 -> "Conflict"
            423 -> "Locked"
            507 -> "Insufficient Storage"
            else -> ""
        }

        /** Parses a WebDAV multistatus document into files. Unknown or malformed entries are skipped. */
        fun parseMultiStatus(xml: String): List<RemoteFile> {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            }
            val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))
            val responses = doc.getElementsByTagNameNS("DAV:", "response")
            val result = mutableListOf<RemoteFile>()
            for (i in 0 until responses.length) {
                val response = responses.item(i) as? Element ?: continue
                val href = response.firstText("href")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: continue
                val isDir = response.getElementsByTagNameNS("DAV:", "collection").length > 0
                val size = response.firstText("getcontentlength")?.trim()?.toLongOrNull()
                val modified = response.firstText("getlastmodified")?.let { parseHttpDate(it) }
                val name = href.trimEnd('/').substringAfterLast('/')
                result += RemoteFile(name = name, href = href, isDirectory = isDir, size = size, lastModified = modified)
            }
            return result
        }

        private fun Element.firstText(localName: String): String? {
            val nodes = getElementsByTagNameNS("DAV:", localName)
            return if (nodes.length == 0) null else nodes.item(0).textContent
        }

        fun parseHttpDate(text: String): Long? = runCatching {
            ZonedDateTime.parse(text.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        }.getOrNull()

        private fun OkHttpClient.Builder.trustAll() {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), SecureRandom()) }
            sslSocketFactory(context.socketFactory, trustManager)
            hostnameVerifier { _, _ -> true }
        }
    }
}
