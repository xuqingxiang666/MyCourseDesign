# 进度快照（Progress Snapshot）

> **每完成一个阶段，必须更新本文件并随代码一起提交。**
> 下一个接手者请以本文件 + `git log` 为准判断进度。

**最后更新**：2026-10-20

---

## 当前状态：笔记增删改查 + 置顶归档全部可用（置顶归档待提交）

| 项 | 状态 |
|---|---|
| Android Studio Sync | ✅ 成功 |
| **项目能否编译** | ✅ **`:app:assembleDebug` BUILD SUCCESSFUL** |
| **APK 能否安装运行** | ✅ 已在模拟器实测，无崩溃 |
| **笔记新增** | ✅ 已实测，数据真实落库 |
| **整页编辑器** | ✅ 已实测，多行输入/换行/预填全部正常 |
| **笔记编辑** | ✅ 已实测，`version` 正确递增 |
| **笔记删除** | ✅ 已实测，长按菜单 + 确认框 + 数据库记录清除 |
| **置顶 / 归档** | ✅ 已实测：置顶排序、归档隔离、"查看归档"视图、取消归档全部正常 |
| **暗色主题** | ✅ 已实测：「跟随系统 / 浅色 / 深色」三选项，列表、编辑器、对话框均可读，切换无崩溃 |
| **Repository 抽象层** | ✅ **已全面生效**：列表与编辑器两个 ViewModel 都只依赖 `NoteRepository`，app 层已零 `NoteDao` 引用 |
| 有效提交次数 | **7 次**（要求 ≥10）+ **1 次待提交**（置顶/归档） |
| 模拟器 | ✅ **`Pixel_10_Pro`（E 盘）** —— 见下方"模拟器环境"一节 |

---

## 模拟器环境（★ 重要，避免重复踩坑）

**统一使用 E 盘的 `Pixel_10_Pro`，不要新建 AVD 到 C 盘。**

| 项 | 值 |
|---|---|
| AVD 名称 | `Pixel_10_Pro` |
| AVD 路径 | `E:\.android\avd\Pixel_10_Pro.avd` |
| 系统镜像 | `android-37.2\google_apis_playstore_ps16k\x86_64`（16KB 页大小） |
| 硬件 | 4 核 / **4096 MB 内存** / 1280×2856 / 数据分区 10G |
| 启动方式 | `-gpu auto`（**不要用 swiftshader_indirect**） |

### ★ 必须显式设置的环境变量

emulator 在 Windows 上**不认 `ANDROID_USER_HOME`**，只认 `ANDROID_PREFS_ROOT`。
本项目曾因此把 AVD 误建到 C 盘（多占 5.4 GB），已删除并改用 E 盘。

```powershell
# 启动模拟器前必须执行这一行，否则 emulator 找不到 E 盘的 AVD
$env:ANDROID_PREFS_ROOT = "E:\.android"

# 完整启动命令
Start-Process "E:\android\Android\Sdk\emulator\emulator.exe" `
  -ArgumentList "-avd","Pixel_10_Pro","-no-snapshot-load","-no-boot-anim","-gpu","auto"
```

| 环境变量 | emulator 是否识别 | 说明 |
|---|---|---|
| `ANDROID_USER_HOME` | ❌ **不识别** | 用户已在用户级设置，但对 emulator 无效 |
| `ANDROID_PREFS_ROOT` | ✅ **识别** | **本项目统一使用这个** |
| `ANDROID_AVD_HOME` | ✅ 识别 | 也可用，但本项目不用 |
| `ANDROID_SDK_HOME` | ❌ 不识别 | — |

### 验证方法

```powershell
$env:ANDROID_PREFS_ROOT = "E:\.android"
& "E:\android\Android\Sdk\emulator\emulator.exe" -list-avds   # 应输出 Pixel_10_Pro
```

### 排查经验

- **截图全黑不代表 App 崩溃**：先用 `adb shell dumpsys activity activities | Select-String topResumedActivity`
  确认 Activity 在前台，再等 5~10 秒重新截图。启动动画阶段截图会得到黑图。
  更可靠的判据是日志里的 `Displayed <包名>/.MainActivity for user 0: +1s176ms`。
- 模拟器自身服务（如 `android.hardware.uwb-service`）会持续刷 `FATAL signal 6`，
  **与本项目无关**，不要被误导。

---

## 提交进度对照

> 完整规划见 `PROJECT_PLAN.md` 第 3 节。
> 原则：**先加声明、再加实现、最后动 UI**，保证每次提交都能独立编译。

### 已完成（按真实 Git 历史排序）

| # | 提交信息 | Hash | 类型 |
|---|---|---|---|
| 1 | `feat: 初始化Android课设项目` | `de0231a` | 脚手架 |
| 2 | `feat: 补齐core数据层并修复编译错误，实现笔记新增功能` | `20fa3c1` | **核心代码** |
| 3 | `chore: 完善gitignore并规范仓库卫生，移除误提交的IDE私有文件` | `c08dc43` | 仓库卫生（原计划 #22，提前完成） |
| 4 | `docs: 补充模拟器环境手册与实测记录` | `3764e8f` | 文档 |
| 5 | `refactor(core): 引入NoteRepository抽象层，隔离UI与数据库实现` | `38eae52` | **核心代码** |
| 6 | `feat(app): 整页笔记编辑器与长按删除` | `dba31ec` | **核心代码** |
| 7 | `refactor(app): NoteViewModel改为依赖NoteRepository` | `8951ad8` | **核心代码**（纯机械替换，对外 API 不变） |
| 8 | `feat(app): 笔记置顶/归档与可切换暗色主题` | **待提交** | **核心代码**（置顶归档 + 暗色主题合并为一次提交） |

> **编号说明**：原计划编号与真实提交顺序在 #3 之后出现了偏移，
> 因为 #3、#4 是仓库卫生与文档类提交，它们提前占用了序号。
> 上表以真实 Git 历史为准，下表的"待做"仍沿用 `PROJECT_PLAN.md` 的功能编号。

### 待做（功能编号）

| # | 提交 | 状态 |
|---|---|---|
| 4 | `feat(app): 整页笔记编辑器与长按删除` | ✅ `dba31ec` |
| 5 | `refactor(app): NoteViewModel改为依赖NoteRepository` | ✅ `8951ad8` |
| 6 | `feat(app): 新增笔记置顶与归档功能` | ✅ **代码已完成，待提交** |
| 7 | `feat(app): 适配暗色主题并完善列表视觉` | ✅ **代码已完成，与 #6 合并提交** |
| 8 | `feat(core): Note实体新增加密相关字段与DAO支持` | ⬜ **下一步** |
| 8 | `feat(core): Note实体新增加密相关字段与DAO支持` | ⬜ |
| 9 | `feat(core): 实现笔记加密与密钥管理` | ⬜ |
| 10 | `feat(app): 接入生物识别解锁与加密笔记界面` | ⬜ |
| 11 | `feat(app): 实现Markdown渲染器` | ⬜ |
| 12 | `feat(app): 接入Markdown实时预览与草稿自动保存` | ⬜ |
| 13 | `feat(core): 实现基于lastModified的LWW冲突解决机制` | ⬜ |
| 14 | `feat(sharecard): 定义P4插件对外契约与入口` | ⬜ |
| 15 | `feat(sharecard): 实现分享卡片Bitmap绘制` | ⬜ |
| 16 | `feat(sharecard): 实现FileProvider分享到其他App` | ⬜ |
| 17 | `feat(sharecard): 实现CSV导出(UTF-8 BOM防乱码)` | ⬜ |
| 18 | `feat(app): 主App接入P4插件入口` | ⬜ |
| 19 | `test(core): 补充数据层与领域逻辑单元测试` | ⬜ |
| 20 | `test(app): 补充UI层单元测试并将覆盖率提升至60%` | ⬜ |
| 21 | `docs: 添加README与架构说明` | ⬜ |
| 22 | `chore: 仓库卫生收尾` | ✅ 由第 3 次提交提前完成 |
| — | **轻量压力测试**（功能全部完成后执行，详见 `PROJECT_PLAN.md` 第 9 节） | ⬜ **项目完成后做** |

> **压力测试的范围**（用户明确要求"完全完成项目之后做轻量压力测试，保证真实场景使用"）：
> 数据量（500/1000 条）、长文本、边界输入（空/空格/emoji/超长单词）、
> 生命周期（旋转/后台/暗色/字体缩放）、并发一致性（快速增删、版本号递增）、
> 分享导出（1000 条 CSV）、稳定性（长时间滚动、反复进出编辑页、Monkey）。
> 每轮结果需按统一格式记录在 `PROJECT_PLAN.md` 第 9.7 节。

### 为什么 #2 是两次提交合并而来

原计划把"清理AS模板"和"实现core数据层"分成两次提交。实际执行时发现二者**强耦合**：

- 删除 `FirstFragment` / `SecondFragment` 后，必须同时决定 `viewBinding` 开还是关
  （用户已确认开，路线 B）；
- 而开启 `viewBinding` 后 `MainActivity` 的原写法 `viewModel.activeNotes.observe(this)`
  会编译失败（`StateFlow` 没有 `observe()` 方法），必须一并改成 `repeatOnLifecycle`。

如果强行拆成两次，中间那次提交是**编译不过**的，违反"每次提交都必须可编译"的原则。
**这是唯一一次合并**，后续按 22 次规划逐步推进，不再合并。

---

## 已完成工作明细（按提交分节）

### 第 2 次提交 `20fa3c1`：core 数据层 + 修复编译 + 笔记新增

**新增 core 数据层（4 个文件）**

| 文件 | 作用 |
|---|---|
| `core/data/entity/Note.kt` | Room 实体，含 `lastModified` / `version`（冲突解决核心字段） |
| `core/data/dao/NoteDao.kt` | DAO，含 `Flow` 查询、增删改、`@Transaction` 状态切换 |
| `core/data/NoteDatabase.kt` | RoomDatabase，`version = 1`，`exportSchema = true` |
| `core/data/DatabaseModule.kt` | Hilt 模块，提供数据库与 DAO 单例 |

配套：`core/build.gradle.kts` 增加 `room.schemaLocation`，
schema 已导出到 `core/schemas/com.example.core.data.NoteDatabase/1.json`（已提交）。

**修改 app 模块**

| 文件 | 改动 |
|---|---|
| `app/build.gradle.kts` | `viewBinding = false` → `true` |
| `MainActivity.kt` | `observe()` → `repeatOnLifecycle`；改用 ViewBinding；`trim()` 后再判断空 |
| `NoteAdapter.kt` | `RecyclerView.Adapter` → `ListAdapter` + `DiffUtil`；改用 ViewBinding；增加状态徽章 |
| `item_note.xml` | `CardView` → `MaterialCardView`（省一个依赖）；硬编码颜色改为主题属性；增加 `tvBadge` |
| `strings.xml` | 移除 AS 模板字符串 |

**删除 AS 导航模板残留（11 个文件 + 5 个空目录）**

```
FirstFragment.kt / SecondFragment.kt
fragment_first.xml / fragment_second.xml / content_main.xml
nav_graph.xml / menu_main.xml
dimens.xml / values-land / values-w600dp / values-w1240dp
```

> 这些是 Android Studio "Basic Views Activity" 模板自带的示例代码，
> 与笔记 App 无关，且会因 `viewBinding` 开关状态直接导致编译失败。

**修复的两个隐藏 Bug**

1. **`StateFlow` 没有 `observe()` 方法**。原 `MainActivity` 写的是
   `viewModel.activeNotes.observe(this) { ... }`（LiveData 的 API），
   编译报 `Unresolved reference: observe`。改为 `repeatOnLifecycle(STARTED) + collect`。
2. **`error.NonExistentClass` 的真正根因**不是 Hilt 配置，而是 `core` 模块
   **完全没有 Kotlin 源码**：`NoteViewModel` 引用的 `Note` / `NoteDao` 从未被创建，
   导致 `:core:compileDebugKotlin` 输出 `NO-SOURCE`。

### 第 3 次提交 `c08dc43`：仓库卫生

- `.gitignore` 改用 `.idea/` 整体忽略（原来只列举了部分文件，遗漏
  `misc.xml` / `migrations.xml` 等，其中 `misc.xml` 会记录本机 JDK 路径）
- 从版本控制移除 7 个误提交的 IDE 私有文件（本地文件保留，只是不再被跟踪）
- 新增 `.gitattributes`：统一行尾 LF，消除刷屏警告，保证 `gradlew` 在 Linux 可执行
- `gradle-wrapper.properties` 补上末尾换行
- **`distributionUrl` 保持不变**（改回官方 URL 会导致构建卡死，已实测复现，详见 `PROJECT_PLAN.md` 第 6 节）

### 第 4 次提交 `3764e8f`：文档与实测记录

- `PROJECT_PLAN.md`：提交规划 12 次 → 22 次；新增 `distributionUrl` 专项说明；
  第 7 节补模拟器完整操作命令
- `PROGRESS.md`：新增"模拟器环境"专节；踩坑表扩充
- 新增 E 盘模拟器运行证据截图

### 第 5 次提交 `38eae52`：Repository 抽象层（**当前最新**）

**新增 3 个文件，现有代码零改动**

| 文件 | 作用 |
|---|---|
| `core/data/repository/NoteRepository.kt` | 仓储接口，定义 UI 层可用的全部操作 |
| `core/data/repository/NoteRepositoryImpl.kt` | 默认实现，纯委托给 `NoteDao` |
| `core/di/RepositoryModule.kt` | Hilt `@Binds` 绑定接口 → 实现 |

**为什么加这一层**：原先 `NoteViewModel` 直接注入 `NoteDao`，导致
(a) UI 与数据库实现强耦合，(b) 单元测试必须构造真实 Room 数据库。
加入后依赖方向变为 `ViewModel → NoteRepository(接口) ← NoteRepositoryImpl → NoteDao`，
测试时传假实现即可 —— **这是覆盖率能上 60% 的前提**。

**关键设计点**

- `createNote(title, content)` 由 Repository 填 `lastModified` 与 `version = 1`，
  因为"新笔记该有什么时间戳和版本号"属于业务规则，不该让 UI 层操心
- `@Binds` 模块**必须是 `abstract class`**（`@Binds` 方法无方法体，
  抽象方法不能放进 `object`），这是它与 `@Provides`（可用 `object`）的关键区别

**⚠️ 本步尚未改变运行时行为**：`NoteViewModel` 仍直接注入 `NoteDao`。
这是刻意的拆分 —— 把"搭抽象"与"改调用方"分成两次提交，保证每一步都能独立编译。
下一步接入 ViewModel 后本层才真正生效。

### 第 6 次提交 `dba31ec`：整页笔记编辑器 + 长按删除

> 状态：✅ **已提交并推送**（`dba31ec`，12 个文件，+538/-47 行）

#### 为什么要重做输入界面

之前新建笔记用的是 `AlertDialog` 里塞两个 `EditText`，且**没有设置 `inputType`**，
所以 Android 给了默认的"单行输入框"样式 —— 内容太长时横向滚动而不换行，
完全不像笔记软件。而且对话框空间狭小，写不了长内容。

本次改为**独立的整页编辑界面**，参照主流笔记/文档软件：顶部工具栏（关闭+保存），
下方加粗标题栏，再下方是铺满整屏、从左上角开始书写的正文区。

#### 新增文件（4 个）

| 文件 | 作用 |
|---|---|
| `res/layout/activity_note_editor.xml` | 整页编辑器布局，关键属性逐条注释 |
| `res/menu/menu_note_editor.xml` | 顶部「保存」菜单项 |
| `app/.../NoteEditorViewModel.kt` | 加载/保存单条笔记，用 `SavedStateHandle` 取 noteId |
| `app/.../NoteEditorActivity.kt` | 编辑器界面，含工具栏、预填、保存逻辑 |

#### 修改文件（4 个）

| 文件 | 改动 |
|---|---|
| `MainActivity.kt` | 删除旧对话框；FAB → 新建；点击卡片 → 编辑；新增长按菜单与删除确认 |
| `NoteAdapter.kt` | 新增 `onItemLongClick` 回调；文案改用字符串资源 |
| `res/values/strings.xml` | 集中管理所有界面文字，不再硬编码 |
| `AndroidManifest.xml` | 注册 `NoteEditorActivity`，设 `windowSoftInputMode="adjustResize"` |

#### 关键技术点（答辩可讲）

| 需求 | 实现方式 |
|---|---|
| **多行输入** | `android:inputType="textMultiLine"` —— 不设它就是单行、横向滚动 |
| **从顶部开始写** | `android:gravity="top\|start"` —— 默认是垂直居中，内容少时会浮在屏幕中间 |
| **去掉输入框边框** | `android:background="@null"` —— 视觉上只剩干净书写区 |
| **铺满整屏** | `android:layout_height="0dp"` + 上下约束（ConstraintLayout 中 0dp = 填满约束范围） |
| **键盘不遮挡** | Manifest 里 `windowSoftInputMode="adjustResize"` + 上面那条 0dp，编辑区自动压缩到键盘上方 |
| **长内容可滚动** | `android:scrollbars="vertical"` |
| **参数传递** | `SavedStateHandle` 自动从 Intent extras 读取 noteId，Activity 无需手动传参 |
| **防止覆盖用户输入** | `hasPrefilledInputs` 标记，只在首次收到数据时填充，避免旋转屏幕后重置内容 |
| **删除二次确认** | `MaterialAlertDialogBuilder` 弹确认框，防止手滑误删 |

#### 删除功能的完整链路

```
长按卡片
  → NoteAdapter.onItemLongClick
  → MainActivity.showNoteOptions()          弹出菜单（数组 + when，便于后续加"置顶/归档"）
  → 选「删除」
  → MainActivity.confirmDelete()            二次确认对话框
  → 确认
  → NoteViewModel.deleteNote(note)
  → NoteDao.deleteNote()
  → Room 删除记录
  → getActiveNotes() 的 Flow 自动发射新列表
  → ListAdapter + DiffUtil 自动播放删除动画
```

**注意最后两步**：删除后**不需要手动通知列表刷新**，Flow 会自动推送新数据。
这是 Room + Flow + DiffUtil 组合的核心优势，也是答辩时可以强调的架构优点。

#### 遗留项

列表页的 `NoteViewModel` **仍然直接注入 `NoteDao`**，还没改为 `NoteRepository`。
编辑器（`NoteEditorViewModel`）已经用上了 Repository，所以新代码是正确分层，
只有旧的列表 ViewModel 待迁移 —— 这就是下一步。

### 第 7 次提交 `8951ad8`：NoteViewModel 迁移到 NoteRepository

> 状态：✅ **已提交并推送**（`8951ad8`，3 个文件，+569/-60 行，其中大部分是文档）

#### 改动内容

只改了一个文件：`app/.../NoteViewModel.kt`。

| 项 | 重构前 | 重构后 |
|---|---|---|
| 注入依赖 | `@Inject constructor(private val noteDao: NoteDao)` | `@Inject constructor(private val noteRepository: NoteRepository)` |
| 列表数据源 | `noteDao.getActiveNotes()` | `noteRepository.observeActiveNotes()` |
| 新建笔记 | 自己 `new Note(...)` + `noteDao.insertNote(...)` | `noteRepository.createNote(title, content)` |
| 更新笔记 | `noteDao.updateNote(note.copy(...))` | `noteRepository.updateNote(note.copy(...))` |
| 删除笔记 | `noteDao.deleteNote(note)` | `noteRepository.deleteNote(note)` |
| 对外 API | — | **完全不变**（`activeNotes` 类型、三个方法签名都没动） |

#### 为什么这一步重要

1. **可测试性**：单元测试现在可以传入假的 `NoteRepository` 实现，
   **完全不需要构造真实 Room 数据库**。这是把覆盖率做到 60% 的前提条件。
   之前直接依赖 `NoteDao` 的话，只能用 Robolectric 或仪器测试，慢且依赖环境。
2. **可替换性**：将来把数据源换成"Room + 云端同步"，只改 Repository 实现，ViewModel 不动。
3. **职责清晰**：`NoteDao` 是"SQL 接口"，`NoteRepository` 是"业务接口"。

#### 验证结果

| 验证项 | 结果 |
|---|---|
| `:app:assembleDebug` | ✅ BUILD SUCCESSFUL，APK 7.14 MB |
| **app 层是否还有 NoteDao 代码引用** | ✅ **零引用**（只剩注释里的说明文字） |
| Hilt 生成绑定 | ✅ `NoteViewModel_Factory` 等正常生成 |
| 启动 | ✅ `topResumedActivity=MainActivity`，无崩溃、无 Hilt 错误 |
| 新建笔记 | ✅ `After-Refactor` / `refactor-works\nsecond-line`，`version=1` |
| 列表显示（Flow 链路） | ✅ 通过 `observeActiveNotes()` 正常显示 |
| 长按菜单 | ✅ 弹出「After-Refactor」+「删除」 |
| 删除确认 | ✅ 显示「确定删除「After-Refactor」吗？」 |
| **删除落库** | ✅ 列表清空，数据库 **0 行** |

> **一次成功重构的标志**：对外 API 没变、`MainActivity` 一行没改、行为完全一致。
> 这次达到了 —— 只改了一个文件的内部实现。

### 第 8 次提交（待提交）：笔记置顶与归档

> 状态：**代码已完成、编译通过、模拟器实测全部通过，等待用户确认后提交**

#### 为什么还要做"查看归档"视图

归档有个 UX 陷阱：笔记归档后从主列表消失，如果**没有查看归档的入口**，
用户就再也找不回那条笔记 —— 归档变成"单向黑洞"。
所以本期除了置顶/归档两个动作，还必须提供 `查看归档 / 返回笔记列表` 的切换。

#### 新增文件

| 文件 | 作用 |
|---|---|
| `res/menu/menu_main.xml` | 主界面工具栏菜单，目前只有"切换归档视图"一项 |

#### 修改文件

| 文件 | 改动 |
|---|---|
| `NoteViewModel.kt` | 新增 `setPinned` / `setArchived`；新增 `_showArchived` 状态与 `toggleShowArchived()`；列表数据源改为 `flatMapLatest` 按视图模式切换；`activeNotes` 更名 `notes` |
| `MainActivity.kt` | 新增工具栏初始化 `setupToolbar()`、视图模式订阅 `observeViewMode()`；长按菜单改为**动态构建**（归档态只给"取消归档/删除"，活跃态给"置顶/归档/删除"） |
| `activity_main.xml` | 顶部新增 `MaterialToolbar`；RecyclerView 高度 `match_parent` → `0dp` 并约束到工具栏下方；新增空状态 `tvEmpty` |
| `strings.xml` | 新增置顶/归档/视图切换/Toast/空状态等 14 条文案 |

#### 关键技术点

| 需求 | 实现 | 说明 |
|---|---|---|
| 视图切换 | `showArchived.flatMapLatest { ... }` | 切换时自动取消上一轮订阅，避免两套数据同时更新导致闪烁。等价于 LiveData 的 `switchMap` |
| 视图模式存哪 | ViewModel 里（`MutableStateFlow`） | Activity 会因旋转重建，ViewModel 不会，所以模式能在旋转后保留 |
| 菜单按状态变化 | 两个平行列表 `labels` / `actions`，按索引对应 | 归档态和活跃态提供不同选项，防止对已归档笔记显示"归档"这种无效操作 |
| 置顶排序 | **不需要额外代码** | DAO 的 SQL 已按 `isPinned DESC, lastModified DESC` 排序，`isPinned` 一变列表自动重排 |
| 归档隔离 | **不需要额外代码** | `getActiveNotes()` 自带 `isArchived = 0` 条件 |
| 版本号/时间戳 | DAO 的 `@Transaction` 方法内更新 | `setPinned` / `setArchived` 保证"读-改-写"原子执行 |

#### 实测验证（模拟器 Pixel_10_Pro / Android API 37.2）

| 验证项 | 结果 |
|---|---|
| 长按活跃笔记 | ✅ 菜单三项：置顶 / 归档 / 删除 |
| **置顶** | ✅ Note-A 移到列表最前，显示「★ 已置顶」，数据库 `pinned=1 version=2` |
| **归档** | ✅ Note-B 从主列表消失，数据库 `archived=1 version=2` |
| 切到归档视图 | ✅ 工具栏标题变「已归档」，Note-B 出现并带「已归档」徽章 |
| 归档笔记的长按菜单 | ✅ 只显示「取消归档 / 删除」（不会误给"置顶/归档"） |
| **取消归档** | ✅ 归档视图变空并提示「暂无已归档的笔记」，数据库 `archived=0 version=3` |
| 切回主列表 | ✅ Note-A（置顶）在上、Note-B 在下，顺序正确 |
| 空状态文案 | ✅ 主列表「还没有笔记，点右下角 + 新建一条」；归档视图「暂无已归档的笔记」 |

截图见 `docs/screenshots/10-pinned.png`、`11-archived-view.png`。

### 第 8 次提交（待提交）：置顶归档 + 暗色主题（合并提交）

> 状态：**两项功能均已完成、编译通过、模拟器实测通过，等待提交**
>
> 说明：用户要求把"置顶/归档"和"暗色主题"合并为一次提交，
> 因此本次提交包含两个功能。代价是提交次数少一次（8 而非 9），
> 但后续还有 15+ 个提交点，不影响达标。

#### 暗色主题：改了什么

**先说一个发现**：项目原本就是 `Theme.Material3.DayNight.NoActionBar`，
且所有布局的颜色都用主题属性（`?attr/colorOnSurface` 等），**没有一处硬编码颜色**。
所以"跟随系统深色"其实早就生效了 —— 这次真正新增的是**让用户在 App 内自己选**。

| 新增/修改 | 内容 |
|---|---|
| `ThemePreferences.kt`（新增） | 主题偏好的读写与应用：`SharedPreferences` 存 `AppCompatDelegate` 的三种模式常量 |
| `MyApplication.kt` | `onCreate` 里调用 `ThemePreferences.applyStoredMode(this)`，保证启动即生效 |
| `menu_main.xml` | 新增「主题」菜单项 |
| `strings.xml` | 新增主题相关 5 条文案 |
| `MainActivity.kt` | `showThemeDialog()`：单选对话框列出三种模式，选中即保存并生效 |

#### 关键技术点

| 需求 | 实现 | 说明 |
|---|---|---|
| 三种模式 | `AppCompatDelegate.MODE_NIGHT_{FOLLOW_SYSTEM,NO,YES}` | 直接把框架常量作为存储值，省掉自定义枚举映射 |
| 切换后立即生效 | `setDefaultNightMode(...)` | AppCompat 会**自动重建所有 Activity**，无需手动 refresh |
| 启动即用上次选择 | `Application.onCreate` 里应用 | 放在 Activity 里会先渲染成错误主题再闪一下 |
| 深色下文字可读 | 布局全用 `?attr/colorOnSurface` / `colorOnSurfaceVariant` | 这也是之前把 `#333333` 改成主题属性的回报 |

#### 实测验证

| 验证项 | 结果 |
|---|---|
| ⋮ 菜单 | ✅ 显示「查看归档」「主题」两项 |
| 主题对话框 | ✅ 三个选项，当前模式正确打勾（默认「跟随系统」） |
| 切到深色 | ✅ Activity 重建后界面变深色，无崩溃 |
| 深色 · 列表 | ✅ 深底 + 深紫卡片 + 白色标题 + 浅灰正文 + 紫色 FAB |
| 深色 · 编辑器 | ✅ 深底、白色标题、分隔线可见、正文可读 |
| 深色 · 对话框 | ✅ 深色面板 + 白色标题 + 紫粉色按钮 |
| 深色下置顶 | ✅ 徽章「★ 已置顶」为紫色，排序正确，数据库 `pinned=1` |
| 切到浅色 | ✅ 恢复正常浅色配色 |
| 切回「跟随系统」 | ✅ 三次切换全程无崩溃 |
| 重新打开对话框 | ✅ 正确显示当前已保存的模式 |

截图：`12-dark-mode-list.png`、`13-dark-mode-editor.png`、
`14-dark-mode-menu.png`、`15-dark-mode-dialog.png`、`16-light-mode-list.png`。

#### 一个测试插曲（已澄清）

测试过程中发现数据库里少了 Note-A、Note-B 的 `version` 变成 13。
我一度怀疑是"强制结束模拟器导致 userdata 损坏"，**但经用户说明，这是他自己测试时
删除笔记、反复切换置顶/归档造成的** —— 属于正常操作痕迹，不是 bug。
`version=13` 恰好反过来证明了版本号递增逻辑工作正常。

**教训**：看到异常数据先确认"是否有人操作过"，再怀疑代码或环境。

---

## 实测记录（模拟器 Pixel_10_Pro / E 盘）

### 第 2 次提交后（`20fa3c1`）—— 首次跑通

| 验证项 | 结果 |
|---|---|
| `:core:kspDebugKotlin` | ✅ 通过（原 `error.NonExistentClass` 消失） |
| `:app:assembleDebug` | ✅ BUILD SUCCESSFUL，APK 7.12 MB |
| `adb install` | ✅ Success |
| 启动 `MainActivity` | ✅ `topResumedActivity` 正确，进程存活无崩溃 |
| 空列表渲染 | ✅ Material 3 主题 + FAB 正常 |
| 新建笔记流程 | ✅ 标题 `Hello-Room`、内容 `First-note-from-emulator` |
| **数据库落库校验** | ✅ sqlite3 查询确认 `notes` 表 1 行，字段值全部正确 |
| 列表刷新（Flow 链路） | ✅ DiffUtil 自动刷新，卡片正确渲染 |

### 第 5 次提交后（`38eae52`）—— Repository 层验证

| 验证项 | 结果 |
|---|---|
| `:core:compileDebugKotlin` | ✅ BUILD SUCCESSFUL |
| `:app:assembleDebug` | ✅ BUILD SUCCESSFUL，APK 7.14 MB |
| Hilt 生成绑定代码 | ✅ `RepositoryModule.java` + `NoteRepositoryImpl_Factory.java` |
| 启动 `MainActivity` | ✅ `Displayed +796ms`，**无 Hilt 注入错误** |
| 新增笔记流程 | ✅ 标题 `Repo-Test`、内容 `Repository-layer-verified` |
| **数据库落库校验** | ✅ `notes` 表 1 行，id/title/content/isArchived/isPinned/version 全正确 |
| 现有代码零改动 | ✅ `git status` 对已有文件无任何 `M` 标记 |

> **注意**：第 5 次提交后 Repository 层尚未被使用（`NoteViewModel` 仍注入 DAO），
> 因此这次实测验证的是"加了新层没把原有功能弄坏"，而非"Repository 生效"。

### 第 6 次提交前（编辑器 + 删除）—— 全链路验证

| 验证项 | 操作 | 结果 |
|---|---|---|
| 编辑器布局 | 点 FAB | ✅ 打开 `NoteEditorActivity`，正文区 `[0,544][1280,2784]`，**高 2240px 铺满整屏** |
| **多行输入** | 输入三行文本 | ✅ 内容为 `Line-one\nLine-two\nLine-three`，**真实换行符** |
| 新建保存 | 点「保存」 | ✅ 回到 `MainActivity`，列表显示卡片 |
| **多行落库** | 导出数据库查询 | ✅ `content='Line-one\nLine-two\nLine-three'` |
| 点卡片编辑 | 点列表卡片 | ✅ 打开 `NoteEditorActivity`，标题与正文**全部预填** |
| **编辑保存** | 追加一行后保存 | ✅ **`version` 由 1 变为 2**，`lastModified` 已更新 |
| 长按菜单 | 长按卡片 1 秒 | ✅ 弹出对话框：笔记标题 + 「删除」 |
| 删除确认 | 点「删除」 | ✅ 显示「确定删除「Editor-Test」吗？此操作不可恢复。」 |
| **确认删除** | 点确认 | ✅ 列表清空，数据库 **0 行** |
| 取消删除 | 点「取消」 | ✅ 笔记保留 |

> 截图见 `docs/screenshots/06~09`：编辑器多行、列表、长按菜单、删除确认。
>
> 一个自动化操作的小插曲：用 `adb shell input keyevent 123`（`KEYCODE_MOVE_END`）
> 想移到文末，实际只移到了**当前行**末尾，导致追加的行插在中间。
> 这是测试脚本的问题，不是 App 的 bug。

### 验证方法备忘（可复用于后续每次提交）

```powershell
# 1. 构建
$env:JAVA_HOME="C:\Users\dashen\.jdks\ms-17.0.20.1"
.\gradlew.bat :app:assembleDebug --offline --console=plain

# 2. 启动模拟器（务必设 ANDROID_PREFS_ROOT）
$env:ANDROID_PREFS_ROOT = "E:\.android"
Start-Process "E:\android\Android\Sdk\emulator\emulator.exe" `
  -ArgumentList "-avd","Pixel_10_Pro","-no-snapshot-load","-no-boot-anim","-gpu","auto"

# 3. 安装并启动
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 shell am start -n "com.example.mycoursedesign/.MainActivity"

# 4. 判定界面已渲染（比截图可靠）
adb -s emulator-5554 logcat -d | Select-String "Displayed com.example.mycoursedesign"

# 5. 权威数据校验：把数据库拉出来用 Python 查
adb -s emulator-5554 exec-out run-as com.example.mycoursedesign `
  cat /data/data/com.example.mycoursedesign/databases/note_database.db > _db.db
# 然后用 sqlite3 查询 SELECT * FROM notes
```

---

## 环境注意事项（踩过的坑，别重复踩）

| 坑 | 现象 | 解法 |
|---|---|---|
| 工作区沙箱 ACL 失败 | 所有 shell 命令报 `SetNamedSecurityInfoW failed (Win32 5)` | 执行命令时申请完整权限（danger-full-access） |
| 默认 JDK 是 24 | 构建环境有兼容风险 | 显式设 `JAVA_HOME=C:\Users\dashen\.jdks\ms-17.0.20.1` |
| `--offline` 会缺依赖 | `No cached version of com.google.dagger:hilt-compiler:2.51.1` | **去掉 `--offline`** 首次联网补齐，之后可离线 |
| `StateFlow` 没有 `observe()` | `Unresolved reference: observe` | 用 `lifecycleScope.launch { repeatOnLifecycle(STARTED) { flow.collect {} } }` |
| ADB 点击对话框坐标漂移 | 键盘弹出后对话框上移 410px，按旧坐标点击无效 | 每次点击前重新 `uiautomator dump` 取实时坐标 |
| PowerShell 重定向破坏二进制 | `adb shell ... cat > file` 导出的 .db 无法打开 | 用 `adb exec-out` 配合 `cmd /c` 重定向 |
| AS 模板残留 | 模板 Fragment 与 viewBinding 设置冲突 | 已删除（见上） |
| **改 Gradle Wrapper URL 会让构建卡死** | 改回官方 HTTPS 后 `gradlew` 无限期挂起 | **不要改 URL**。Gradle 用 URL 的哈希定位缓存目录，改 URL = 缓存失效 = 重新下载，而 `services.gradle.org` 在本机被卡死。已实测复现并回滚 |
| `Set-Content -Encoding UTF8` 会写入 BOM | Java 读 properties 文件可能出错 | 用 `[System.IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding $false))` |
| 截图全黑≠崩溃 | 启动动画阶段截图得到全黑图 | 等 5~10 秒重截，或用日志 `Displayed <包名>/.MainActivity` 判定 |
| **模拟器无法输入中文** | 手动敲键盘只能出英文 | **不是 App 的 bug**，是测试环境限制：AVD 的 `hw.keyboard=yes` 直接用物理键盘，且 Gboard 默认无中文布局。App 对中文完全支持（界面中文渲染正常、SQLite 存 TEXT 天然支持 UTF-8）。解决办法见 `PROJECT_PLAN.md` 第 10 节 |

---

## 下一步（给接手者）

**下一步：`feat(core): Note实体新增加密相关字段与DAO支持`（功能编号 #8）**

这是"笔记加密 + 生物识别"三步中的第一步，**本步只加字段与数据层支持，不改 UI**，
所以一定能独立编译通过：

1. `Note` 实体目前已有 `isEncrypted` 字段（当初就预留了），确认是否需要补充
   如 `encryptedContent` 或直接复用 `content` 存密文
2. `NoteDao` 增加按 `isEncrypted` 过滤/统计的查询（若加密笔记需在列表中模糊显示）
3. 编译验证 + 更新本文件 + **等用户确认后**提交

**⚠️ 待提交内容（1 次提交，等用户确认）**：

第 8 次：置顶归档 + 暗色主题（用户要求合并）
- 新增：`res/menu/menu_main.xml`、`ThemePreferences.kt`
- 修改：`NoteViewModel.kt`、`MainActivity.kt`、`MyApplication.kt`、
  `activity_main.xml`、`strings.xml`
- 截图：`docs/screenshots/10~16`（7 张）
- 本文件与 `PROJECT_PLAN.md`

**后续步骤（详见 `PROJECT_PLAN.md` 第 3 节）**：
#8~#10 加密与生物识别 → #11~#12 Markdown 与自动保存 → #13 冲突解决
→ #14~#18 P4 插件 → #19~#20 单元测试 → #21 README
→ **压力测试（`PROJECT_PLAN.md` 第 9 节）**

**已确认的决策**：
1. ~~viewBinding~~ → ✅ 已确认开启（路线 B）
2. ~~MaterialCardView~~ → ✅ 已确认改用
3. ~~gradle-wrapper URL~~ → ❌ **已实测否决**，保持 file:// 现状（改 URL 会让本机构建卡死，详见 `PROJECT_PLAN.md` 第 6 节）
4. ~~模拟器位置~~ → ✅ 统一使用 E 盘 `Pixel_10_Pro`，C 盘 AVD 已删除（释放 5.2 GB）
5. ~~编辑器与 ViewModel 重构的先后顺序~~ → ✅ **编辑器优先**（用户能立刻感知体验提升；
   编辑器是纯新增 UI，不依赖 Repository 重构也能独立编译）。ViewModel 重构顺延。
6. ~~模拟器中文输入~~ → ⏸ **暂缓**（用户决定先不管）。详见 `PROJECT_PLAN.md` 第 10 节：
   App 本身完全支持中文，只是模拟器输入手段受限。
7. ~~压力测试时机~~ → ✅ **项目功能全部完成后再做**（用户明确要求）
8. ~~C 盘空间约束~~ → ✅ **默认不在 C 盘产生大文件**。AVD 固定在 E 盘；
   临时文件放 `E:\dsh-tmp`（构建时把 `TEMP`/`TMP` 指向它）；
   数据库导出等中间产物也放 E 盘并事后删除。Gradle 增量缓存
   （`~/.gradle`，约 3.7 GB）无法避免，但每次构建只增几十 MB。

**提交纪律（用户明确要求，务必遵守）**：
- 课设要求 ≥10 次有效提交，**不要把多个步骤合并成一次提交**
- **绝不主动提交** —— 只有用户明确说"提交"或"推送"才动 Git。
  文档写好、代码验证通过，都只是准备工作，不能自动升级为提交。
- **一次只做用户让做的事** —— 用户说"更新文档"就只改文档，
  不顺手改 `.gitignore`、不顺手提交。发现别的问题先报告，不动手。
- **每完成一块工作，必须同步更新本文件**（用户明确要求）：
  功能说明、实测结果、踩坑记录、下一步，缺一不可。
- 每次提交前必须 `:app:assembleDebug` 编译通过
- 推送失败时**重试一次即可**；若代理已关闭，直连通常可用
