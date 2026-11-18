# MyCourseDesign —— 离线优先笔记（C4） + 分享卡片插件（P4）

移动互联网开发课程设计。一个 **多模块 Android 笔记应用**：
主应用负责笔记的离线增删改查、Markdown 编辑预览、加密与同步冲突解决；
独立的 `feature_sharecard` 模块提供"分享卡片图片"与"CSV 导出"能力。

---

## 一、功能清单（对照课设要求）

### 必做

| 要求 | 实现 | 提交 | 验证方式 |
|---|---|---|---|
| **Markdown 实时渲染** | 自研解析器 + 渲染器，支持标题/粗体/斜体/删除线/行内代码/链接/列表/引用/代码块/分割线 | `a7c38e7`、`bdc6a8a` | 27 个单元测试 + 模拟器实测 |
| **草稿自动保存** | 输入停顿 600ms 自动落库，工具栏显示保存时间 | `bdc6a8a` | 模拟器实测（不点保存也已入库） |
| **离线-同步冲突解决** | 软删除 + 远端抽象 + LWW 三级判定（时间戳/版本号/内容哈希）+ 模拟同步演示页 | `f7d83ca`、`41ea502`、`ca5650a` | 29 个单元测试 + 端到端实测 |

### 自选

| 要求 | 实现 | 提交 |
|---|---|---|
| 笔记加密 + 生物识别 | AES-256-GCM + Android Keystore + BiometricPrompt（指纹/锁屏密码） | `9e466e4`、`811b293` |

### 伴生模块 P4

| 能力 | 实现 | 提交 |
|---|---|---|
| 分享卡片图片 | Canvas 手绘 900×1200 PNG，纯文本正文 | `77bc10e` |
| FileProvider 分享 | content:// Uri + 临时授权，先落盘再分享 | `2f500a4` |
| CSV 导出 | UTF-8 BOM + CRLF + 字段转义 | `cf3c82b` |
| 主应用接入 | 长按菜单「分享卡片 / 导出 CSV」 | `9c16652` |

### 附加

- 置顶 / 归档 / 归档视图
- 暗色主题（跟随系统 / 浅色 / 深色，持久化）
- Markdown 格式工具栏（选中文字点按钮加粗/斜体/列表）
- 模拟同步演示页（答辩用）

---

## 二、架构

```
:app                  UI 层：Activity + ViewModel + Adapter（MVVM）
  │
  ├── :core           数据与领域层
  │     ├── data/entity         Room 实体（Note、RemoteNote）
  │     ├── data/dao            DAO（Room，编译期生成实现）
  │     ├── data/repository     Repository 接口 + 实现
  │     ├── data/remote         "云端"抽象（RemoteDataSource）
  │     ├── domain              ConflictResolver / SyncEngine（纯逻辑）
  │     └── security            CryptoEngine / CryptoManager / NoteCrypto
  │
  └── :feature_sharecard  伴生插件
        └── share               ShareCardPlugin 契约 + CSV + 卡片绘制 + FileProvider
```

**依赖方向**：`app → （core、feature_sharecard）`，两个下层模块**互不依赖**。

**分层原则**：把"纯逻辑"和"平台依赖"分开，纯逻辑才能被单元测试覆盖：

| 纯逻辑（可 JVM 单测） | 平台实现（需要 Android） |
|---|---|
| `MarkdownParser` / `MarkdownFormatter` | `MarkdownRenderer`（Spannable） |
| `ConflictResolver` / `SyncEngine` | `LocalRemoteDataSource`（Room） |
| `CryptoEngine`（AES-GCM） | `CryptoManager`（Keystore） |
| `CsvExporter` / `TextWrapper` | `ShareCardRenderer`（Canvas）、`ShareCardFileStore` |

---

## 三、技术栈（版本锁定，未升级任何依赖）

| 组件 | 版本 |
|---|---|
| Gradle | 8.2 |
| Android Gradle Plugin | 8.2.2 |
| Kotlin | 1.9.22 |
| KSP | 1.9.22-1.0.17 |
| Java | 17 |
| compileSdk / targetSdk / minSdk | 34 / 34 / 24 |
| Room | 2.6.1 |
| Hilt | 2.51.1 |
| 生物识别 | androidx.biometric 1.1.0 |

---

## 四、如何运行

```bash
# 编译并安装（在项目根目录）
./gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 跑全部单元测试
./gradlew.bat :core:testDebugUnitTest :app:testDebugUnitTest :feature_sharecard:testDebugUnitTest
```

> 本项目依赖本地已缓存的依赖。若网络可用可去掉 `--offline`。

### 功能入口

| 功能 | 入口 |
|---|---|
| 新建笔记 | 右下角 ✚ |
| Markdown 预览 | 编辑器右上角「预览 / 编辑」 |
| 格式工具栏 | 编辑器底部 B / I / S / `</>` / • / 1. |
| 置顶 / 归档 / 加密 / 分享 / 导出 / 删除 | **长按笔记卡片** |
| 查看归档、主题、同步演示 | 列表页右上角 ⋮ |
| 解锁加密笔记 | 点击加密笔记 → 指纹或锁屏密码 |

---

## 五、测试

### 单元测试：163 个，全部通过

| 模块 | 用例数 | 主要测试类 |
|---|---|---|
| core | 83 | `CryptoEngineTest`(17)、`ConflictResolverTest`(16)、`SyncEngineTest`(13)、`NoteRepositoryImplTest`(22)、`SyncRepositoryTest`(14) |
| app | 49 | `MarkdownParserTest`(27)、`MarkdownFormatterTest`(21) |
| feature_sharecard | 31 | `CsvExporterTest`(15)、`TextWrapperTest`(15) |

### 覆盖率（JaCoCo 0.8.11）

| 统计口径 | 行覆盖 |
|---|---|
| **逻辑层**（排除 UI 与生成代码） | **68.0%** ✅ |
| core 模块（整体） | **81.4%** ✅ |
| 全项目（含 Activity/Renderer/DataBinding） | 33.2% |

> Activity、Adapter、Canvas/Spannable 渲染器依赖 Android 运行时，
> 需要 Robolectric 或仪器测试，按通用做法不计入单元测试覆盖率。

复现：

```bash
gradlew --init-script docs/tools/jacoco.init.gradle :core:coverageReport
# 报告：core/build/reports/jacoco/coverageReport/html/index.html
```

### 轻量压力测试

**JVM 级（作为性能回归测试保留）**

| 场景 | 数据量 | 耗时 |
|---|---|---|
| `SyncEngine.plan` | 1000 + 1000 条（全冲突） | 47 ms |
| `ConflictResolver.resolve` | 10000 次 | 2 ms |
| AES-256-GCM 加解密 | 500 次（约 1 MB） | 135 ms |
| `MarkdownParser.parse` | 500 节 / 36890 字符 | 31 ms |
| `MarkdownFormatter` 全选加粗 | 200 次 | 31 ms |

**设备级（模拟器，注入 504 条笔记）**

| 场景 | 结果 |
|---|---|
| 冷启动 | 1184 ms |
| 滚动 133 帧 | 90 分位 18 ms，Janky 8.27% |
| 同步推送 502 条 | 约 2.6 s |
| 同步比对+写入 504 条 | 约 2.4 s（结果正确：上传 2、下载 1、冲突 1） |

---

## 六、关键设计说明

### 1. Markdown：为什么自己实现

课设要求"防抄袭的定制特色功能"，引入第三方库无法体现实现能力；
项目依赖版本全部锁定且处于离线环境，新增依赖有风险。
自己实现子集反而更容易讲清原理，也便于单元测试（27 个用例）。

### 2. 草稿自动保存：防抖

```
输入 → 取消上一个任务 → 重新计时 600ms → 无新输入才写库
```

避免用户连续打字时每敲一个字就写一次数据库。
另外"内容未变则跳过写库"，防止 `version` 被无意义刷高（它是冲突判定的依据）。

### 3. 冲突解决：LWW 三级判定

| 级别 | 比较 | 作用 |
|---|---|---|
| 1 | `lastModified` | 后写的赢 |
| 2 | `version` | 同毫秒修改时用版本号 |
| 3 | 内容 SHA-256 | 保证结果确定可复现，不随机 |

**删除参与比较**：已删除记录的有效时间取 `max(deletedAt, lastModified)`，
让"删除"退化为普通的一次修改，不需要单独规则。

### 4. 同步：为什么不需要云服务器

课设考的是**冲突解决机制**。远端抽象成接口：

```kotlin
interface RemoteDataSource { ... }
class LocalRemoteDataSource : RemoteDataSource   // 当前：本地表模拟
// 将来：HttpRemoteDataSource                    // 接真实后端，其余代码不改
```

好处：离线可演示、可重复演示、可单元测试。

### 5. 加密

- AES-256-GCM（自带完整性校验，能发现密文被篡改）
- 数据密钥存在 `EncryptedSharedPreferences`，主密钥在 Android Keystore
- 加密笔记**不参与明文分享**：卡片要画明文，插件没有密钥；
  强行解密会把密文写进缓存文件，等于绕过保护

### 6. 分享卡片：为什么先写文件再分享

Intent 的 Binder 事务缓冲区约 1MB，900×1200 ARGB 位图约 4.3MB，
直接塞会抛 `TransactionTooLargeException`。
标准做法：写文件 → 生成 content:// Uri → 只传 Uri。

---

## 七、目录结构

```
MyCourseDesign/
├── app/                     主应用
│   └── src/main/java/com/example/mycoursedesign/
│       ├── MainActivity.kt            列表页
│       ├── NoteEditorActivity.kt      编辑页（预览 + 自动保存 + 格式栏）
│       ├── SyncDemoActivity.kt        同步演示页
│       ├── ShareHelper.kt             分享 Intent 组装
│       ├── markdown/                  自研 Markdown 解析/渲染/格式化
│       └── ...
├── core/                    数据与领域层
│   ├── src/main/java/com/example/core/
│   │   ├── data/                      实体、DAO、Repository、远端抽象
│   │   ├── domain/                    ConflictResolver、SyncEngine
│   │   └── security/                  加密
│   └── schemas/              Room 导出的表结构（v1/v2/v3）
├── feature_sharecard/       分享卡片伴生模块
├── docs/
│   ├── PROGRESS.md          开发进度与实测记录（详细）
│   ├── PROJECT_PLAN.md      项目计划
│   └── tools/jacoco.init.gradle  覆盖率统计脚本
└── README.md
```

---

## 八、已知限制

| 限制 | 说明 |
|---|---|
| 加密笔记只读预览 | 编辑器保存明文，允许直接编辑会造成"标记已加密但内容是明文"的不一致，故要求先取消加密 |
| 同步为本地模拟 | 详见第四节；接口已抽象，可替换为真实后端 |
| 冲突采用 LWW | 不做逐字段合并；同一毫秒内修改靠版本号与内容哈希兜底 |
| 中文输入 | 模拟器输入法限制，真机正常 |

---

## 九、提交历史

共 **21 次提交**，按功能粒度拆分，每次提交均可独立编译通过。

```bash
git log --oneline
```
