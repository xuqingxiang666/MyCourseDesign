# 进度快照（Progress Snapshot）

> **每完成一个阶段，必须更新本文件并随代码一起提交。**
> 下一个接手者请以本文件 + `git log` 为准判断进度。

**最后更新**：2026-10-20

---

## 当前状态：笔记增删改查 + 置顶归档 + 暗色主题全部可用（加密功能开发中）

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
| 有效提交次数 | **8 次**（要求 ≥10） |
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
| 8 | `feat(app): 笔记置顶/归档与可切换暗色主题` | `eb10fab` | **核心代码**（两功能合并提交） |
| 9 | `feat(core): Note实体加密字段 + 数据库迁移 + DAO支持` | **待提交** | **核心代码** |
| 10 | `feat(core): AES-256-GCM加密引擎与密钥管理` | **待提交** | **核心代码 + 单元测试** |
| 11 | `feat(app): 加密笔记界面与生物识别解锁` | `811b293` | **核心代码** |
| 12 | `feat(app): Markdown渲染器（解析器+渲染器+27个单测）` | `a7c38e7` | **核心代码 + 单元测试** |
| 13 | `feat(app): Markdown实时预览与草稿自动保存` | **待提交** | **核心代码（必做特色）** |

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
| 8 | `feat(core): Note实体新增加密相关字段与DAO支持` | ✅ **代码已完成，待提交** |
| 9 | `feat(core): 实现笔记加密与密钥管理` | ✅ **代码已完成，待提交** |
| 10 | `feat(app): 接入生物识别解锁与加密笔记界面` | ✅ **代码已完成，待验收** |
| 11 | `feat(app): 实现Markdown渲染器` | ✅ **代码已完成，待提交** |
| 12 | `feat(app): 接入Markdown实时预览与草稿自动保存` | ✅ **代码已完成，待提交** |
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

### 第 8 次提交 `eb10fab`：笔记置顶与归档 + 暗色主题

> 状态：✅ **已提交并推送**（`eb10fab`，16 个文件，+575/-125 行）

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

#### 暗色主题部分（与上同属第 8 次提交）

> 说明：用户要求把"置顶/归档"和"暗色主题"合并为一次提交，
> 因此 `eb10fab` 同时包含两个功能。

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

### 第 9 次提交（待提交）：Note 加密字段 + 数据库迁移 + DAO 支持

> 状态：**代码已完成、`:app:assembleDebug` 通过、schema 静态校验通过、
> 运行时迁移已在真实设备上验证通过** ✅

这是"笔记加密 + 生物识别"三步中的第一步，**只动数据层，不改 UI**。

#### 改动文件

| 文件 | 改动 |
|---|---|
| `core/.../entity/Note.kt` | 新增 `encryptedAt: Long`，标注 `@ColumnInfo(defaultValue = "0")` |
| `core/.../NoteDatabase.kt` | 版本 1 → 2；新增 `MIGRATION_1_2` |
| `core/.../DatabaseModule.kt` | 注册 `.addMigrations(MIGRATION_1_2)` |
| `core/.../dao/NoteDao.kt` | 新增 `getEncryptedNotes()`、`getEncryptedNoteCount()`、`setEncryption()` |
| `core/.../repository/NoteRepository.kt` | 接口新增对应三个方法 |
| `core/.../repository/NoteRepositoryImpl.kt` | 实现委托给 DAO |
| `core/schemas/.../2.json` | Room 自动导出的 v2 schema（已纳入版本控制） |

#### 为什么复用 `content` 存密文，而不新开字段

加密时把 `content` 从明文换成 `Base64(IV + AES-GCM 密文)`，`isEncrypted = true` 标记。
这样列表、搜索、排序等既有逻辑完全不用改 —— 加密只是"同一字段的另一种编码"。

#### 关键技术点：Room 迁移与 `@ColumnInfo(defaultValue)`

**这是本次踩到的真实坑，值得记下来。**

一开始只写了迁移 SQL：

```kotlin
db.execSQL("ALTER TABLE notes ADD COLUMN encryptedAt INTEGER NOT NULL DEFAULT 0")
```

但 Room 导出的 v2 schema 里，该字段的 `defaultValue` 是 `None`（因为 Kotlin 的
`= 0L` 只是代码层默认值，Room 建表时不会写进 DDL）。于是：

- SQLite 实际表结构：`encryptedAt INTEGER NOT NULL DEFAULT 0`
- Room 期望的表结构：`encryptedAt INTEGER NOT NULL`

两者不一致，Room 启动校验时会抛
`IllegalStateException: Migration didn't properly handle: notes(...)`。

**修法**：给字段加 `@ColumnInfo(defaultValue = "0")`，让实体侧也声明默认值。

修正后静态校验结果：

| 检查项 | 结果 |
|---|---|
| v2 新增字段 | `encryptedAt`（唯一新增） |
| affinity | `INTEGER` |
| notNull | `true` |
| defaultValue | `"0"` |
| 已有字段定义是否变化 | 无 |
| 迁移 SQL 与实体定义是否一致 | ✅ 一致 |

#### 为什么必须写 Migration，而不是只改 version

`DatabaseModule` 里虽然保留了 `fallbackToDestructiveMigration()`，但它只在
**找不到迁移**时才生效，后果是**删库重建、用户笔记全丢**。
写了 `MIGRATION_1_2` 之后，升级时数据完整保留。

#### 运行时迁移实测结果（已验证 ✅）

模拟器上原本是 v1 数据库（含 `Dark-A`、`Dark-B` 两条笔记），
从 Android Studio 运行新版 APK 后自动触发 1 → 2 迁移，结果：

| 检查项 | 升级前 | 升级后 |
|---|---|---|
| `PRAGMA user_version` | 1 | **2** ✅ |
| `notes` 表列 | 8 列（无 `encryptedAt`） | 9 列（**含 `encryptedAt`**）✅ |
| 笔记数量 | 2 | **2**（未丢失）✅ |
| `Dark-A` | pinned=1, version=2 | pinned=1, version=2（未变）✅ |
| `Dark-B` | pinned=0, version=3 | pinned=0, version=3（未变）✅ |
| `encryptedAt` 默认值 | — | 两条均为 **0** ✅ |
| 崩溃 / 迁移异常 | — | **无**（`FATAL EXCEPTION`、`Migration didn't properly handle` 均未出现）✅ |

> 验证方法：`adb exec-out run-as ... cat` 导出 db/wal/shm 三个文件后用 sqlite3 查询。
> **注意**：MSYS2 bash 会把 `/data/data/...` 自动转换成 Windows 路径，
> 必须先 `export MSYS2_ARG_CONV_EXCL='*'`，否则 `run-as` 找不到文件。

---

### 第 10 次提交（待提交）：AES-256-GCM 加密引擎与密钥管理

> 状态：**代码已完成、编译通过、17 个单元测试全部通过；
> 尚未接入 UI（第 11 次提交负责界面与生物识别）**

#### 新增文件

| 文件 | 作用 |
|---|---|
| `core/security/CryptoEngine.kt` | 纯逻辑 AES-256-GCM 加解密，**零 Android 依赖**，可在 JVM 上测 |
| `core/security/CryptoManager.kt` | 密钥管理（EncryptedSharedPreferences + Android Keystore）+ Base64 编解码 |
| `core/src/test/.../CryptoEngineTest.kt` | 17 个单元测试 |

#### 修改文件

| 文件 | 改动 |
|---|---|
| `NoteRepository.kt` | 新增 `encryptNote(note)`、`decryptNote(note)` |
| `NoteRepositoryImpl.kt` | 注入 `CryptoManager`，实现两个方法 |

#### 为什么拆成 Engine + Manager 两个类

| 类 | 依赖 | 能否 JVM 单测 |
|---|---|---|
| `CryptoEngine` | 只用 `javax.crypto` / `java.security` | ✅ 能（17 个测试秒级跑完） |
| `CryptoManager` | `EncryptedSharedPreferences`、`android.util.Base64` | ❌ 必须在设备上测 |

这是本项目的分层原则：**把纯逻辑和平台依赖分开，纯逻辑就能被单元测试覆盖**。
课设要求"单元测试覆盖率 ≥ 60%"，靠的就是这种拆法。

#### 为什么选 AES-GCM 而不是 AES-CBC

| | AES-CBC | AES-GCM |
|---|---|---|
| 机密性 | ✅ | ✅ |
| 完整性/防篡改 | ❌ 需额外加 HMAC | ✅ 自带认证标签 |
| 是否需要 padding | 需要 | 不需要 |

GCM 能发现"密文被改动过"，CBC 不能。笔记被偷偷篡改是能想到的现实风险，所以选 GCM。

#### 密钥怎么保护

1. `MasterKey` 由 **Android Keystore** 生成并保管，密钥材料永不出 Keystore；
2. 用这个主密钥加密 SharedPreferences 的键和值（`EncryptedSharedPreferences`）；
3. 我们的 AES-256 数据密钥存在这个加密 prefs 里。

即使把 prefs 文件拷走，没有 Keystore 里的主密钥也解不开。

#### 输出格式

```
数据库里存的字符串 = Base64( IV(12字节) || AES-GCM 密文(含16字节认证标签) )
```

IV 直接拼在密文前面，解密时按固定长度切回来 —— **不需要额外的 IV 列**。

#### 设计上的两个防御点

1. **加密失败不改数据库**：宁可返回失败，也不出现"标记成已加密但内容是明文"的不一致状态
2. **已加密的不重复加密**：`encryptNote` 遇到 `isEncrypted = true` 直接返回成功，
   否则会把密文再加密一次，之后解密必然失败

#### 实测证据

**单元测试**：`gradlew :core:testDebugUnitTest` → 17 个用例全部通过（0 失败）

覆盖场景：英文/中文/emoji/换行/空串/5 万字符长文本往返、IV 随机性、
错误密钥返回 null、密文篡改返回 null、截断返回 null、IV 篡改返回 null、
密钥长度校验、密钥生成随机性。

**真实运行演示**（用编译产物直接跑，不依赖设备）：

```
原始明文 : 会议纪要：项目代号 Athena，口令 8f3k2x
加密后长度: 78 字节 (IV 12 + 密文 66)
入库字符串: gxF++lYuNGmr+ZlJyQ8CmUYkunoftPonLbahIYwNbgWswRJGt0OiK0nYnJiVTd681acnwPBvVFoC5omOli0fdZ//KJfs7hekkrlc/u+3
解密还原 : 会议纪要：项目代号 Athena，口令 8f3k2x
错误密钥解密: null
篡改后解密  : null
两次密文是否相同: false
```

#### 待验证（第 11 步一并完成）

- [ ] UI 接入后，在设备上实际加密一条笔记并解锁查看
- [ ] `EncryptedSharedPreferences` 在模拟器上的创建是否正常（代码已做异常降级）

---

### 第 11 次提交（待提交）：加密界面 + 生物识别解锁

> 状态：✅ **已提交（`811b293`）并在模拟器上完成端到端实测**

这是加密功能**用户终于能看见**的一步。

#### 改动文件

| 文件 | 改动 |
|---|---|
| `app/build.gradle.kts` | 显式引入 `libs.androidx.biometric`（版本已在版本目录锁定，非新增依赖） |
| `res/values/strings.xml` | 新增 14 条加密/生物识别文案 |
| `NoteAdapter.kt` | 加密笔记不显示正文（避免露出 Base64 密文），改显示「🔒 已加密，解锁后查看」；徽章优先级 已加密 > 置顶 > 归档 |
| `NoteViewModel.kt` | 新增 `encryptNote` / `removeEncryption` / `readDecryptedContent` |
| `BiometricGate.kt`（新） | BiometricPrompt 统一封装 |
| `MainActivity.kt` | 长按菜单加「加密笔记/取消加密」；点击加密笔记先验证身份再只读预览 |

#### 用户视角的完整流程

**加密**
1. 长按笔记 → 菜单出现「加密笔记」
2. 点击 → 正文被 AES-256-GCM 加密，数据库里 content 变成 Base64 密文
3. 列表卡片正文变成「🔒 已加密，解锁后查看」，徽章显示「🔒 已加密」

**查看**
1. 点击加密笔记 → 弹出系统的指纹/锁屏密码验证
2. 验证通过 → 弹出只读预览，显示解密后的正文
3. 验证失败/取消 → 提示"身份验证未通过，无法查看内容"，**不泄露任何内容**

**取消加密**
1. 长按 → 「取消加密」→ 解密后以明文写回，恢复可编辑状态

#### 关键设计决策：加密笔记为什么是只读预览，不进编辑器

编辑器保存时写的是**明文**。如果允许直接编辑加密笔记，保存后就会出现
`isEncrypted = true` 但 `content` 是明文的不一致状态，之后解密必然失败、甚至把密文覆盖掉。

所以设计成：加密状态下只做**只读预览**；要编辑必须先「取消加密」。
用一步额外操作换取数据一致性，这是刻意的取舍。

#### 生物识别的两个细节

1. **同时允许锁屏密码**（`BIOMETRIC_WEAK or DEVICE_CREDENTIAL`）
   只允许指纹的话，用户指纹失灵（手湿、冬天干裂）就永远打不开自己的笔记了。
   注意：允许 DEVICE_CREDENTIAL 时**不能**再调 `setNegativeButtonText`，否则抛异常。

2. **先检查再弹出**（`canAuthenticate`）
   设备没设置指纹/锁屏时，直接给"请先在系统设置里设置锁屏密码"的指引，
   而不是弹一个必然失败的验证框。

#### 端到端实测结果（模拟器 Pixel_10_Pro，全部通过）

| 步骤 | 操作 | 结果 |
|---|---|---|
| 1 | 长按笔记 | ✅ 菜单出现「置顶 / 归档 / **加密笔记** / 删除」 |
| 2 | 点「加密笔记」 | ✅ 列表正文变「🔒 已加密，解锁后查看」，徽章变「🔒 已加密」 |
| 3 | 查数据库 | ✅ `isEncrypted=1`、`encryptedAt=1793894809421`、<br>`content` 是 Base64 密文（`bGWFZ/jvG3L0a7Bm...`） |
| 4 | 点击加密笔记 | ✅ 系统弹出「解锁加密笔记 / 请验证指纹或锁屏密码」 |
| 5 | 输入 PIN `1234` | ✅ 验证通过，弹出只读预览，显示明文 `dark-content-B` |
| 6 | 长按再进菜单 | ✅ 该项已变为「**取消加密**」 |
| 7 | 点「取消加密」 | ✅ 列表恢复显示明文；数据库 `isEncrypted=0`、`encryptedAt=0`、<br>`content='dark-content-B'` |

截图：`17-encrypted-list.png`（加密后的列表）、`19-decrypted-preview.png`（解密预览）、
`20-decrypted-again.png`（取消加密后）。

**测试环境说明**：
- 模拟器没有指纹硬件，所以走的是 `DEVICE_CREDENTIAL` 降级路径（PIN 键盘）。
  这正是当初"同时允许锁屏密码"的设计目的 —— 也验证了降级路径可用。
- PIN 通过 `adb shell locksettings set-pin 1234` 设置。
- **PIN 输入界面截图是纯黑**，因为生物识别/凭证界面启用了 `FLAG_SECURE`
  防截屏，这是系统行为，不是崩溃。判断该界面是否正常要用
  `uiautomator dump`（能看到「解锁加密笔记」和数字键盘）。

---

### 第 12 次提交（待提交）：Markdown 渲染器

> 状态：**代码已完成、27 个单元测试通过、APK 构建通过；尚未接入编辑器界面（下一步）**

课设必做项「Markdown 实时渲染」的**解析与渲染核心**，本步只做工具类，不改界面。

#### 新增文件

| 文件 | 作用 |
|---|---|
| `markdown/MarkdownModel.kt` | 结构化模型：`MdBlock`（标题/段落/列表/引用/代码块/分割线）、`MdInline`、`InlineStyle` |
| `markdown/MarkdownParser.kt` | 纯 Kotlin 解析器，把 Markdown 转成模型（可 JVM 单测） |
| `markdown/MarkdownRenderer.kt` | 把模型渲染成 `SpannableString`（依赖 Android） |
| `app/src/test/.../MarkdownParserTest.kt` | **27 个单元测试** |

#### 为什么要拆成"解析器 + 渲染器"

`SpannableString` 是 Android 类型，在 JVM 单元测试里不可用（会抛 `Method not mocked`）。
拆开之后：

- 解析器零 Android 依赖 → **27 个测试秒级跑完**，不需要模拟器
- 渲染器负责把模型变成带样式文本

这与 `CryptoEngine` / `CryptoManager` 的拆法一致，是项目统一的"纯逻辑与平台依赖分离"原则。

#### 支持的语法

| 类型 | 语法 |
|---|---|
| 标题 | `#` ~ `######` |
| 段落 | 连续行合并；空行分段 |
| 无序列表 | `-` / `*` / `+` |
| 有序列表 | `1.` / `1)` |
| 引用 | `>` |
| 代码块 | ``` 围栏（内部不解析行内标记） |
| 分割线 | `---` / `***` / `___` |
| 粗体 | `**文字**` / `__文字__` |
| 斜体 | `*文字*` / `_文字_` |
| 粗斜体 | `***文字***` |
| 删除线 | `~~文字~~` |
| 行内代码 | `` `代码` `` |
| 链接 | `[文字](网址)` |

#### 自己实现而不引第三方库的理由

1. 课设要求"防抄袭的定制特色功能"，用现成库难以体现自己的实现；
2. 项目依赖版本全部锁定、处于离线环境，加新依赖有风险；
3. 只做子集反而更容易讲清原理，也便于单元测试覆盖。

#### 两个设计细节

1. **未闭合的标记当普通文字**：输入 `**没有闭合` 时不会被吞掉后面的内容，
   而是原样显示 —— 用户边输入边预览时不会看到文字"消失"。
2. **代码块内不解析行内标记**：`` `**不该是粗体**` `` 会原样显示星号，符合预期。

#### 实测

- `:app:testDebugUnitTest` → **28 个用例全部通过**（27 个 Markdown + 1 个模板）
- `:app:assembleDebug` → BUILD SUCCESSFUL（APK 7.54 MB）

#### 待验证（下一步接入界面后）

- [ ] 编辑器里输入 Markdown 能实时看到富文本效果
- [ ] 暗色模式下链接色、代码底色是否协调
- [ ] 列表卡片预览是否已去掉标记符号

---

### 第 13 次提交（待提交）：Markdown 实时预览 + 草稿自动保存

> 状态：**代码已完成、编译通过、模拟器端到端实测通过（含截图）**
>
> 这是课设**必做项「Markdown 实时渲染」真正完成**的标志 —— 上一提交只是渲染器，
> 本提交把它接进界面，并且补上「草稿自动保存」。

#### 改动文件

| 文件 | 改动 |
|---|---|
| `NoteEditorViewModel.kt` | `noteId` 改为可变；新增 `autoSave()`；`persist()` 统一两条保存路径；新增 `lastAutoSavedAt` 状态 |
| `NoteEditorActivity.kt` | 预览/编辑切换、`TextWatcher` + 600ms 防抖、工具栏副标题显示保存时间 |
| `res/layout/activity_note_editor.xml` | 新增 `NestedScrollView` + `tvPreview` 预览区（与输入区同位置，二选一显示） |
| `res/menu/menu_note_editor.xml` | 新增「预览 / 编辑」菜单项 |
| `NoteAdapter.kt` | 列表卡片正文改用 `MarkdownParser.toPlainText()`，去掉 `**` 等符号 |
| `res/values/strings.xml` | 新增 5 条文案 |

#### 草稿自动保存的实现

```
用户输入 → TextWatcher.afterTextChanged
        → 取消上一个 Job，重新 delay(600ms)
        → 600ms 内无新输入 → viewModel.autoSave() → 写 Room
        → 成功后更新 lastAutoSavedAt → 工具栏副标题「草稿已自动保存 10:58:27」
```

**这是防抖（debounce）**：连续打字时不会每敲一个字写一次库。

#### 三个设计要点

1. **`noteId` 必须是可变字段**
   新建笔记第一次自动保存时才真正插入数据库并拿到 id。如果保持 `val`，
   每次自动保存都会新建一条重复笔记。

2. **内容未变则跳过写库**
   `persist()` 里比较 `existing.title/content` 与新内容，相同就直接返回成功。
   否则用户每停一次输入就会把 `version` 无意义地刷高 —— 而 `version` 是后面
   "冲突解决机制"判断数据新旧的依据，被污染会导致误判。

3. **切换预览前先落库一次**
   保证预览显示的是最新内容，同时取消待执行的自动保存任务，避免切换瞬间重复写库。

#### 端到端实测结果（模拟器，全部通过）

| 步骤 | 操作 | 结果 |
|---|---|---|
| 1 | 新建笔记，输入 `# Meeting Notes` 等 5 行 Markdown | ✅ 输入正常 |
| 2 | 不点保存，等待输入停顿 | ✅ 工具栏出现「**草稿已自动保存 02:58:27**」 |
| 3 | 点「预览」 | ✅ 按钮变「编辑」；渲染结果：标题变大加粗、`bold text` 加粗、`italic` 斜体、列表加 `•`、引用加 `▎` 且变淡、所有 `#/*/-/>` 符号消失 |
| 4 | 返回列表 | ✅ 卡片正文显示纯文本（`• item one` 等），无 Markdown 符号 |
| 5 | 查数据库 | ✅ 笔记已入库（**未点保存按钮**），`version=4`，`content` 保留原始 Markdown |

截图：`21-markdown-edit.png`（编辑态）、`22-markdown-preview.png`（预览态）、
`23-markdown-list-preview.png`（列表纯文本预览）。

#### 测试中遇到的一个坑（已解决）

用 `adb shell input text '# Meeting Notes'` 输入 Markdown 时，`#` 被设备端 shell
当成注释、`>` 被当成重定向（报 `can't create quoted: Read-only file system`）。
正确写法是把整个命令作为一个字符串传入，让引号在设备端生效：

```bash
adb shell "input text '# Meeting Notes'"
adb shell "input text '> quoted line'"
```

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

**下一步：`feat(app): 接入生物识别解锁与加密笔记界面`（功能编号 #10）**

数据层与算法都已完成，下一步做**用户能看见的部分**：

1. 长按菜单增加「加密笔记 / 取消加密」两项
   （`showNoteOptions()` 已是"数组 + actions"结构，追加即可）
2. 加密后列表卡片正文显示「🔒 已加密，点击解锁」，不显示密文
3. 点击加密笔记时，先用 `BiometricPrompt` 验证指纹/锁屏密码，
   通过后再调用 `repository.decryptNote(note)` 取明文进入编辑器
4. 列表卡片增加锁图标（`item_note.xml` 已有 `tvBadge` 可复用，或新增 `tvLock`）
5. 忘记密码/指纹不可用时的降级提示
6. 编译 + 模拟器实测（**指纹在模拟器上支持很弱，可能需要真机，或用锁屏密码兜底**）
   + 更新本文件 + 等用户确认后提交

**⚠️ 待提交内容（1 次提交，等用户确认）**：

第 9 次：Note 加密字段 + 数据库迁移 + DAO 支持
- 修改：`Note.kt`、`NoteDatabase.kt`、`DatabaseModule.kt`、
  `NoteDao.kt`、`NoteRepository.kt`、`NoteRepositoryImpl.kt`
- 新增：`core/schemas/.../2.json`
- 本文件
- **待办**：运行时迁移实测（需用户先启动模拟器）

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
