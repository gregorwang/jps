# 应用内更新安全对齐报告

审计日期：**2026-07-13（Asia/Hong_Kong）**  
功能区域：Android 应用内 APK 更新、外部 Intent、FileProvider 与 Cloudflare 发布入口  
优先级：**High**——该功能跨越网络下载、文件共享和系统安装边界。

## 结论

更新链路没有让云端 APK 直接获得安装权限。客户端只接受固定 HTTPS 更新服务，下载后验证文件大小、SHA-256、applicationId、versionCode 和当前已安装应用的签名证书；验证通过后仅把只读 `content://` URI 交给系统级安装器，最终仍由用户确认安装。

R2 桶保持私有。公网 Worker 只提供读取 `latest` 清单和按纯数字 versionCode 下载 APK 的路由，没有上传、列举、删除或任意 object key 读取入口。

## 审计和修改文件

| 文件 | 对齐内容 |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | 声明安装权限与包可见性查询；FileProvider `exported=false`、只授予 URI 权限 |
| `app/src/main/res/xml/app_update_file_paths.xml` | 仅共享 APP external-files 下的 `Download/app-updates/` |
| `app/src/main/java/com/animejapaneselab/nativeapp/update/AppUpdateClient.kt` | HTTPS、禁止重定向、同源同端口及固定下载路径校验、64 KiB 清单上限 |
| `AppUpdateModels.kt` | 严格字段长度/范围、SHA-256 与 ISO-8601 发布时间校验 |
| `AppUpdateManager.kt` | DownloadManager、哈希/包/版本/签名复核、显式系统组件 Intent、只读 URI grant |
| `AppUpdateViewModel.kt` / `AppUpdateRoute.kt` | 单向状态流、一次性外部导航事件、避免授权返回时重复拉起安装器、保留协程取消 |
| `update-worker/src/*.ts` | 固定 R2 key 规则、只读路由、Range/HEAD、内容类型和缓存头 |
| `scripts/publish-app-update.ps1` | APK 先上传、`latest` 最后更新；云端版本无法确认时失败关闭 |

## Intent 与文件共享的精确变更

1. 未知来源设置只使用当前包：

   ```kotlin
   Intent(
       Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
       Uri.parse("package:${appContext.packageName}"),
   )
   ```

2. 安装文件通过 FileProvider 生成 URI，不使用 `file://`：

   ```kotlin
   val contentUri = FileProvider.getUriForFile(
       appContext,
       "${appContext.packageName}.fileprovider",
       file,
   )
   ```

3. 安装 Intent 仅授予读取权限，并把 URI 放入 `ClipData` 以覆盖各 Android 版本的 grant 传播：

   ```kotlin
   Intent(Intent.ACTION_VIEW).apply {
       setDataAndType(contentUri, ApkMimeType)
       clipData = ClipData.newRawUri("Nihongo Lab update", contentUri)
       addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
   }
   ```

4. 在启动设置页或安装器前查询候选组件，只选择带 `FLAG_SYSTEM` 或 `FLAG_UPDATED_SYSTEM_APP` 的 Activity，再通过 `ComponentName` 变成显式 Intent。没有可信系统组件时直接停止。

5. Manifest 中的 provider 配置为：

   ```xml
   android:authorities="${applicationId}.fileprovider"
   android:exported="false"
   android:grantUriPermissions="true"
   ```

## 下载包信任判定

安装前必须同时满足：

1. 云端清单由固定 HTTPS 同源路径返回，且不接受 HTTP redirect。
2. 实际字节数与清单一致。
3. 实际 SHA-256 与清单一致。
4. APK 内 package name 等于当前应用 ID。
5. APK 内 versionCode 等于清单 versionCode。
6. APK signer 集合非空并与当前已安装应用 signer 集合完全相同。

这里真正防止 Cloudflare 账号或清单同时被篡改后安装恶意 APK 的最后边界是 Android 签名一致性；SHA-256 同时负责发现传输损坏和清单/文件不一致。

## 已验证

- Android：148 个 JVM 测试通过；Lint 0 error；`localSlim` 经 R8、资源收缩、Vital Lint、签名验证并成功打包。
- APK：package `com.animejapaneselab.nativeapp`、versionCode 2、versionName 0.2.0；APK Signature Scheme v2 验证通过；单一 signer 为当前 Android debug certificate。
- Worker：5 个 Vitest 测试通过；TypeScript typecheck 和 Wrangler deploy 通过。
- 生产端：health 200、latest 200、APK HEAD 200、Range 206 且 `Content-Range` 正确、完整远端 APK SHA-256 与本地产物一致。

## 剩余边界与后续要求

- Android 不允许普通 APP 静默覆盖安装，系统确认页是预期安全边界。
- 当前 `localSlim` 使用 `%USERPROFILE%\.android\debug.keystore`。密钥丢失或换签会导致后续 APK 无法覆盖；换电脑前必须备份。
- `REQUEST_INSTALL_PACKAGES` 适合当前个人侧载渠道；若未来进入 Google Play，需要重新评估 Play 政策和改用 Play In-App Updates。
- 下载 URL 对任何知道地址的人可读，但桶、对象列表和写入口不公开。若未来 APK 内容本身需要访问控制，应另行设计短期授权，不能把长期秘密硬编码进 APP。
- 模拟器只能证明安装、启动和系统集成信号；真实手机上的厂商安装器行为仍应做一次人工点击回归。
