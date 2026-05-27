# WorkScribe

Android 工作记录应用（`club.cyxc.workscribe`）。

## 构建

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release 产物：`app/build/outputs/apk/release/WorkScribe-{versionName}.apk`

## 版本号

版本单一来源为根目录 [`version.properties`](version.properties)：

| 键 | 含义 |
| --- | --- |
| `VERSION_CODE` | 整数，单调递增 |
| `VERSION_NAME` | 语义化版本（如 `1.0.0`），须与 Git tag `v*` 一致 |

发版流程：

```bash
./scripts/bump-version.sh 1.0.0
git add version.properties && git commit -m "chore: bump version to 1.0.0"
git tag v1.0.0
git push origin main && git push origin v1.0.0
```

CI Release job 会校验 tag 与 `VERSION_NAME` 是否一致。

## Release 签名

| 场景 | 行为 |
| --- | --- |
| 存在根目录 `keystore.properties` | 使用正式 keystore 签名（见 [`keystore.properties.example`](keystore.properties.example)） |
| 不存在 | **自动退回 debug keystore**，便于本地验证 Release 包，**不可用于商店发布** |

生成 keystore 示例：

```bash
keytool -genkeypair -v \
  -keystore ~/.android/workscribe-release.jks \
  -alias workscribe-release \
  -keyalg RSA -keysize 4096 -validity 36500 \
  -storepass '<store-password>' -keypass '<key-password>' \
  -dname "CN=WorkScribe, O=Cyxc, C=CN"
```

`keystore.properties` 已列入 `.gitignore`，请勿提交密钥。

## 持续集成

工作流见 [`.github/workflows/android.yml`](.github/workflows/android.yml)。

| 触发 | Job | 说明 |
| --- | --- | --- |
| `main` / `master` 的 push、PR | `build` | `assembleDebug` + `lintDebug` |
| 推送 tag `v*` | `release` | 使用 Secrets 签名 `assembleRelease`，并附到 GitHub Release |

发布 tag 前，在仓库 **Settings → Secrets and variables → Actions** 配置：

| Secret | 说明 |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | keystore 文件 Base64（`base64 -w0 release.jks`） |
| `RELEASE_STORE_PASSWORD` | keystore 密码 |
| `RELEASE_KEY_ALIAS` | 密钥别名 |
| `RELEASE_KEY_PASSWORD` | 密钥密码 |
