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

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebDavClientTest {
    private val server = MockWebServer()
    private lateinit var client: WebDavClient

    @Before
    fun setUp() {
        server.start()
        client = WebDavClient(WebDavConfig(server.url("/dav/").toString(), "alice", "s3cret"))
    }

    @After
    fun tearDown() = server.shutdown()

    private val multistatus = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
          <d:response>
            <d:href>/dav/EventSince/</d:href>
            <d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>
          </d:response>
          <d:response>
            <d:href>/dav/EventSince/eventsince-20260901-1200.zip</d:href>
            <d:propstat><d:prop>
              <d:resourcetype/>
              <d:getcontentlength>1234</d:getcontentlength>
              <d:getlastmodified>Tue, 01 Sep 2026 12:00:00 GMT</d:getlastmodified>
            </d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>
          </d:response>
          <d:response>
            <d:href>/dav/EventSince/notes%20old.txt</d:href>
            <d:propstat><d:prop><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>
          </d:response>
        </d:multistatus>
    """.trimIndent()

    @Test
    fun `list sends PROPFIND with auth and parses entries`() {
        server.enqueue(MockResponse().setResponseCode(207).setBody(multistatus))
        val files = client.list("/EventSince/")

        val request = server.takeRequest()
        assertEquals("PROPFIND", request.method)
        assertEquals("/dav/EventSince/", request.path)
        assertEquals("1", request.getHeader("Depth"))
        assertEquals("Basic YWxpY2U6czNjcmV0", request.getHeader("Authorization"))

        assertEquals(2, files.size)
        val backup = files[0]
        assertEquals("eventsince-20260901-1200.zip", backup.name)
        assertFalse(backup.isDirectory)
        assertEquals(1234L, backup.size)
        assertEquals(java.time.ZonedDateTime.parse("2026-09-01T12:00:00Z").toInstant().toEpochMilli(), backup.lastModified)
        assertEquals("notes old.txt", files[1].name)
        assertNull(files[1].size)
        assertTrue(CloudBackupService.isBackupFile(backup))
        assertFalse(CloudBackupService.isBackupFile(files[1]))
    }

    @Test
    fun `put get delete`() {
        server.enqueue(MockResponse().setResponseCode(201))
        client.put("/EventSince/a.json", "{}".toByteArray())
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/dav/EventSince/a.json", put.path)
        assertEquals("{}", put.body.readUtf8())
        assertTrue(put.getHeader("Content-Type")!!.startsWith("application/json"))

        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"x\":1}"))
        assertEquals("{\"x\":1}", String(client.get("/EventSince/a.json")))
        assertEquals("GET", server.takeRequest().method)

        server.enqueue(MockResponse().setResponseCode(204))
        client.delete("/EventSince/a.json")
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `ensureDirectory creates missing segments and tolerates existing ones`() {
        server.enqueue(MockResponse().setResponseCode(404)) // PROPFIND backups/
        server.enqueue(MockResponse().setResponseCode(201)) // MKCOL backups/
        server.enqueue(MockResponse().setResponseCode(207).setBody(multistatus)) // PROPFIND backups/x/ exists
        client.ensureDirectory("/backups/x/")
        assertEquals("PROPFIND", server.takeRequest().method)
        val mkcol = server.takeRequest()
        assertEquals("MKCOL", mkcol.method)
        assertEquals("/dav/backups/", mkcol.path)
        assertEquals("/dav/backups/x/", server.takeRequest().path)

        server.enqueue(MockResponse().setResponseCode(405))
        client.mkcol("/backups/") // already exists: no exception
    }

    @Test
    fun `errors carry the HTTP status`() {
        server.enqueue(MockResponse().setResponseCode(401))
        val e = assertFailsWith<WebDavException> { client.list("/EventSince/") }
        assertEquals(401, e.status)
        assertTrue(e.message!!.contains("401"))
    }

    @Test
    fun `base url normalisation`() {
        assertEquals("https://cloud.example.com/remote.php/dav/files/me/", WebDavClient.normaliseBase("cloud.example.com/remote.php/dav/files/me").toString())
        assertEquals("http://h:8080/", WebDavClient.normaliseBase("http://h:8080").toString())
        assertEquals("/EventSince/", me.byang.eventsince.data.prefs.WebDavSettingsRepository.normaliseDir("EventSince"))
        assertEquals("/a/b/", me.byang.eventsince.data.prefs.WebDavSettingsRepository.normaliseDir(" /a/b "))
    }
}
