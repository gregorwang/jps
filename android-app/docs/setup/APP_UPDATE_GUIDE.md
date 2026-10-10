# Android 应用内更新说明

最后核对：**2026-07-13（Asia/Hong_Kong）**

## 现在的方案

```text
设置页“检查更新”
  -> 独立 Cloudflare Update Worker
  -> 私有 R2 存储桶
  -> Android DownloadManager 下载 APK
  -> 校验文件 SHA-256
  -> 校验 applicationId、versionCode 和签名证书
  -> Android 系统安装器要求用户确认
  -> 覆盖安装，学习数据保留
```

- R2 桶：`anime-japanese-lab-android-updates`
- Worker：`anime-japanese-lab-android-updates`
- 更新服务：`https://anime-japanese-lab-android-updates.ishallnotwant123.workers.dev`
- Android 清单入口：`/v1/latest`
- APK 下载入口：`/v1/releases/{versionCode}/apk`

R2 桶本身不开放公共目录，也没有从手机端上传或删除文件的接口。独立 Worker 只允许读取最新版清单和由数字 `versionCode` 指定的 APK。

## 为什么不是“静默更新”

普通 Android APP 不能绕过用户静默覆盖自己。第一次安装应用内下载的 APK 时，系统会要求允许 Nihongo Lab“安装未知应用”；之后每次安装新版本，仍会显示 Android 系统安装确认页。这是系统安全边界，不是代码缺陷。

## 第一次需要做什么

包含更新功能的首个版本是 `0.2.0` / `versionCode 2`。手机上的旧版没有更新代码，因此仍需最后手动安装一次这个版本。安装 `0.2.0` 后，未来版本都可以从 APP 设置页完成下载和覆盖安装。

不要卸载旧版再安装；直接覆盖安装才能保留本机学习数据。

手机浏览器可直接打开首个版本，无需再经过电脑或微信：

```text
https://anime-japanese-lab-android-updates.ishallnotwant123.workers.dev/v1/releases/2/apk
```

这一次由浏览器下载时，Android 可能要求给浏览器“安装未知应用”权限。以后由 Nihongo Lab 自己下载时，系统会单独要求给 Nihongo Lab 该权限；二者是 Android 按来源分别管理的授权。

## 当前已发布版本

- 版本：`0.2.0` / `versionCode 2`
- 大小：`23,309,589` 字节
- SHA-256：`D2B551CA6D30BE62E993F2A35221E310FD21A4D5D548AF3C5A6A01B12853BEC3`
- Worker 部署版本：`a70eb72c-6fa7-4a9f-8add-2a4191714013`
- 云端已验证：最新版 JSON、HEAD、`0-1023` Range、完整 APK 下载及本地/远端 SHA-256 一致。

## 以后怎样发布新版本

每次发布前先把 `app/build.gradle.kts` 中的 `versionCode` 增加，并按需要修改 `versionName`。`versionCode` 必须严格递增。

然后在 PowerShell 运行：

```powershell
Set-Location 'C:\Users\汪家俊\jps\android-app'
.\scripts\publish-app-update.ps1 -ReleaseNotes '本次修复和新增内容'
```

脚本会自动：

1. 构建 `localSlim`。
2. 从 Gradle 输出读取真实版本号。
3. 计算 APK 大小和 SHA-256。
4. 先上传 APK。
5. 再上传该版本不可变清单。
6. 最后更新 `latest.json`，避免客户端看到尚未上传完成的版本。

如果 APK 已经构建完成，可使用 `-SkipBuild`。默认禁止覆盖同一或更低 `versionCode`；只有明确需要修复一次错误上传时才使用 `-AllowRepublish`。

## 签名是最重要的条件

Android 只允许使用相同签名证书的 APK 覆盖安装。当前 `localSlim` 沿用这台电脑的 Android debug keystore：

```text
%USERPROFILE%\.android\debug.keystore
```

只要一直用同一份 keystore 构建，应用内更新就能覆盖。换电脑前需要安全备份这份文件；如果签名密钥丢失，新 APK 无法覆盖旧 APP。

正式对外发布时应迁移到独立 release keystore，但不能在不安排数据迁移的情况下突然给已安装版本换签名。

## 手机端会做哪些安全检查

下载完成后，APP 不会立即打开文件，而是依次检查：

1. 实际文件大小等于云端清单。
2. 实际 SHA-256 等于云端清单。
3. APK 的 applicationId 是 `com.animejapaneselab.nativeapp`。
4. APK 内部 `versionCode` 等于清单版本。
5. APK 当前签名证书与已安装 APP 完全一致。
6. 只把文件交给系统级安装器，不把 APK URI 广播给任意第三方 APP。

任一检查失败都会删除下载记录并阻止安装。

## 文件位置与进程重启

下载由 Android `DownloadManager` 执行，任务 ID 和版本清单保存在独立 SharedPreferences 中。即使关闭 APP 或 Activity 重建，系统仍可继续下载；回到设置页后会恢复进度。APK 位于 APP 自己的 external-files `Download/app-updates/` 目录，不需要申请通用存储权限。

## Cloudflare 文件

- `update-worker/wrangler.jsonc`：独立 Worker 与 R2 binding。
- `update-worker/src/index.ts`：只读 HTTP 路由、Range 下载和响应头。
- `update-worker/src/releaseManifest.ts`：R2 清单白名单与校验。
- `scripts/publish-app-update.ps1`：一条命令构建并发布。

部署更新 Worker：

```powershell
Set-Location 'C:\Users\汪家俊\jps'
.\node_modules\.bin\wrangler.cmd deploy --config android-app\update-worker\wrangler.jsonc
```
