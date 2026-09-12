# Privacy Policy for EventSince

Effective date: 12 September 2026

EventSince (package name `me.byang.eventsince`) is developed by Boyuan Yang. This policy
explains what data the app handles and where that data goes. The short version: everything
you enter stays on your device unless you choose to back it up to a server of your own.

[中文版本见下文。](#事起隐私政策)

## Data the app stores

EventSince stores the following on your device only:

- Events, including their label, colour, category, start time, notes and archive state.
- The history of every operation performed on an event.
- Goal reminders and their schedule.
- App settings, such as the language, duration format, colour palette and cloud backup
  configuration.

None of this data is sent to the developer. The developer has no server and no way to see
what you store in the app.

## What the app does not do

- No account or sign-in is required or offered.
- No analytics, usage statistics or crash reports are collected.
- No advertising or tracking libraries are included.
- No device identifiers are read or transmitted.

The app is open source under the Apache License 2.0. Its source code, including every
network request it can make, can be inspected at <https://github.com/hosiet/eventsince>.

## Cloud backup over WebDAV (optional)

If you enable cloud backup, the app uploads a backup archive containing all of the data
listed above to the WebDAV server whose address, user name and password you enter. Backups
are uploaded when you ask for one, and, if you enable automatic backup, after you change
your data.

- The server is chosen and controlled by you. The developer does not operate any server and
  never receives your backups or your credentials.
- Your WebDAV password is stored on the device encrypted with a key held in the Android
  Keystore. It is sent only to the server you configured, for authentication.
- If you enter an `http://` address, or turn on the option to accept self-signed
  certificates, the connection to your server is not protected against eavesdropping. Both
  are your choice; the app validates certificates for `https://` servers by default.
- Backups on the server are kept until you delete them, either through the app's remote
  copy limit or directly on the server.

## Local backup, import and sharing

Exporting a backup writes an archive to a location you pick with the system file chooser.
Importing reads a file you pick. Sharing an event as an image hands the image to the app
you choose. In every case the data goes only where you direct it.

## Android system backup

Android may include the app's data in the device backup that the system performs, for
example to your Google account, if you have enabled device backup in the system settings.
That backup is managed by Android and by the provider of your device backup, not by
EventSince, and is governed by their terms.

## Permissions

- Notifications: to show goal reminders.
- Alarms and reminders (exact alarms): to fire goal reminders at the exact moment a goal is
  reached. If you do not allow this, reminders still fire, but they may be delayed.
- Run at startup: to re-register reminders after the device restarts.
- Network access: used only to talk to the WebDAV server you configured, and only if you
  enable cloud backup.

## Children

EventSince is not directed at children under 13 and does not knowingly collect any
information from anyone, of any age.

## Changes to this policy

Changes are published in this file in the project's repository, with an updated effective
date at the top.

## Contact

Questions about this policy can be raised on the project's issue tracker at
<https://github.com/hosiet/eventsince/issues>.

---

# 事起隐私政策

生效日期：2026 年 9 月 12 日

事起（EventSince，包名 `me.byang.eventsince`）由 Boyuan Yang 开发。本政策说明应用会处理哪些
数据，以及这些数据的去向。简单地说：你输入的一切都只保存在你的设备上，除非你主动选择把它备份到
自己的服务器。

## 应用保存的数据

事起只在你的设备上保存以下内容：

- 事件，包括标签、颜色、分类、起始时间、备注和归档状态。
- 对事件执行的每一次操作的历史记录。
- 目标提醒及其触发时间。
- 应用设置，例如语言、时长格式、调色板和云备份配置。

这些数据都不会发送给开发者。开发者没有任何服务器，也无法看到你在应用中保存的内容。

## 应用不会做的事

- 不需要也不提供任何账号或登录。
- 不收集统计分析、使用数据或崩溃报告。
- 不包含任何广告或跟踪组件。
- 不读取或传输任何设备标识符。

应用以 Apache License 2.0 开源。包括所有可能发出的网络请求在内的完整源代码可在
<https://github.com/hosiet/eventsince> 查阅。

## WebDAV 云备份（可选）

如果你启用云备份，应用会把包含上述全部数据的备份压缩包上传到你填写的 WebDAV 服务器地址，并
使用你填写的用户名和密码。备份在你手动发起时上传；如果你开启了自动备份，数据变更后也会上传。

- 服务器由你选择和控制。开发者不运营任何服务器，也不会收到你的备份或凭据。
- 你的 WebDAV 密码使用 Android Keystore 中的密钥加密后保存在设备上，只会发送给你配置的服务器
  用于身份验证。
- 如果你填写的是 `http://` 地址，或者开启了"接受自签名证书"选项，与服务器之间的连接无法防止
  窃听。这两项都由你决定；对 `https://` 服务器，应用默认校验证书。
- 服务器上的备份会一直保留，直到你通过应用的远程副本数量上限或直接在服务器上删除。

## 本地备份、导入与分享

导出备份会把压缩包写入你通过系统文件选择器指定的位置；导入会读取你选择的文件；把事件分享为
图片时，图片会交给你选择的应用。数据始终只去往你指定的地方。

## Android 系统备份

如果你在系统设置中开启了设备备份，Android 可能会把应用数据纳入系统执行的设备备份，例如备份
到你的 Google 账号。该备份由 Android 和你的设备备份服务提供方管理，不由事起控制，适用它们
的条款。

## 权限

- 通知：用于显示目标提醒。
- 闹钟和提醒（精确闹钟）：用于在目标达成的准确时刻发出提醒。若你不允许，提醒仍会发出，但可能
  延迟。
- 开机启动：用于在设备重启后重新登记提醒。
- 网络访问：仅用于与你配置的 WebDAV 服务器通信，且仅在你启用云备份时使用。

## 儿童

事起不面向 13 岁以下儿童，也不会有意收集任何年龄段用户的任何信息。

## 政策变更

变更会发布在项目仓库中的本文件里，并更新顶部的生效日期。

## 联系方式

关于本政策的问题可在项目的问题跟踪页面提出：<https://github.com/hosiet/eventsince/issues>。
