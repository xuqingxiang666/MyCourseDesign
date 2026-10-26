# MyCourseDesign 项目全貌与开发计划

> 移动互联网开发课程设计 · C4 离线优先笔记 + P4 分享卡片
> 本文档是项目的唯一事实来源（Single Source of Truth）。任何接手者先读本文档，再读代码。

---

## 0. 文档修订记录

| 日期 | 修订内容 |
|---|---|
| 2026-10-15 | 初版。修正"core 模块已完成"的错误认知——实际 core 无任何 Kotlin 源码 |

---

## 1. 项目最终目标

一个**多模块 Android 应用**，由两部分组成：

| 组成 | 名称 | 定位 |
|---|---|---|
| 主应用 | **C4 离线优先笔记** | 完整笔记 App：增删改查、归档置顶、Markdown 预览、加密生物识别、冲突解决 |
| 伴生模块 | **P4 分享卡片与导出** | 插件式模块：把笔记渲染成分享卡片图片、导出 CSV、通过 FileProvider 分享到其他 App |

---

## 2. 交付物清单（老师验收时会看什么）

| 交付物 | 位置 | 状态 |
|---|---|---|
| 可运行 APK / 可编译工程 | 整个仓库 | 待完成 |
| Git 提交历史 ≥ 10 次有效提交 | GitHub 仓库 | 进行中（1/12） |
| 项目说明文档 | `README.md` | 待完成 |
| 开发计划与设计文档 | `docs/PROJECT_PLAN.md`（本文档） | 已完成 |
| 单元测试 + 覆盖率报告 | `app/src/test/`、`core/src/test/` | 待完成 |

---

## 3. 开发阶段与提交规划（细粒度，共 20+ 次提交）

**两条必须同时满足的约束**：

1. **课设硬性要求**：Git 提交次数 ≥ 10 次有效提交 → 因此**步骤要切细，不轻易合并**。
2. **工程底线**：每次提交都必须 `:app:assembleDebug` 编译通过 → 中间状态不能是坏的。

**为同时满足两者而采用的排序技巧**：**先加声明、再加实现、最后动 UI**。
例如先建好 `NoteRepository` 接口（不影响编译），再写实现，最后才改 ViewModel。
这样每个中间态都是绿的，但步骤数足够多。

| # | 阶段 | 提交内容 | 能否独立编译 | 状态 |
|---|---|---|---|---|
| 1 | 环境 | `feat: 初始化Android课设项目` | ✅ | ✅ `de0231a` |
| 2 | 阶段1+2 | `feat: 补齐core数据层并修复编译错误，实现笔记新增功能` | ✅ | ✅ `20fa3c1` |
| 3 | 阶段3 | `refactor(core): 引入NoteRepository抽象，隔离UI与DAO` | ✅ 纯新增，无行为变化 | ✅ `38eae52` |
| 4 | 阶段3 | `feat(app): 整页笔记编辑器与长按删除` | ✅ | ✅ `dba31ec` |
| 5 | 阶段3 | `refactor(app): NoteViewModel改为依赖NoteRepository` | ✅ DAO 方法全量委托 | ✅ `8951ad8` |
| 6 | 阶段3 | `feat(app): 新增笔记置顶与归档功能` | ✅ | ✅ 已完成（与 #7 合并提交） |
| 7 | 阶段3 | `feat(app): 适配暗色主题并完善列表视觉` | ✅ | ✅ 已完成（与 #6 合并提交），**待提交** |
| 8 | 阶段5 | `feat(core): Note实体新增加密相关字段与DAO支持` | ✅ 纯加字段 | ⬜ 下一步 |
| 9 | 阶段5 | `feat(core): 实现笔记加密与密钥管理` | ✅ | ⬜ |
| 10 | 阶段5 | `feat(app): 接入生物识别解锁与加密笔记界面` | ✅ | ⬜ |
| 11 | 阶段5 | `feat(app): 实现Markdown渲染器` | ✅ 纯工具类 | ⬜ |
| 12 | 阶段5 | `feat(app): 接入Markdown实时预览与草稿自动保存` | ✅ | ⬜ |
| 13 | 阶段5 | `feat(core): 实现基于lastModified的LWW冲突解决机制` | ✅ 纯逻辑类 | ⬜ |
| 14 | 阶段4 | `feat(sharecard): 定义P4插件对外契约与入口` | ✅ 纯接口 | ⬜ |
| 15 | 阶段4 | `feat(sharecard): 实现分享卡片Bitmap绘制` | ✅ | ⬜ |
| 16 | 阶段4 | `feat(sharecard): 实现FileProvider分享到其他App` | ✅ | ⬜ |
| 17 | 阶段4 | `feat(sharecard): 实现CSV导出(UTF-8 BOM防乱码)` | ✅ | ⬜ |
| 18 | 阶段4 | `feat(app): 主App接入P4插件入口` | ✅ | ⬜ |
| 19 | 阶段6 | `test(core): 补充数据层与领域逻辑单元测试` | ✅ | ⬜ |
| 20 | 阶段6 | `test(app): 补充UI层单元测试并将覆盖率提升至60%` | ✅ | ⬜ |
| 21 | 阶段6 | `docs: 添加README与架构说明` | ✅ | ⬜ |
| 22 | 阶段6 | `chore: 完善gitignore与gitattributes，规范仓库卫生` | ✅ | ✅ 已提前完成 `c08dc43` |

> **注意 1**：#2 由原计划的两次提交合并而来（原因见 `PROGRESS.md`）。
> 合并后仍有充沛余量，相对 ≥10 次的要求完全够用。
>
> **注意 2（顺序调整）**：原计划是先做 `NoteViewModel` 重构（#4）再做编辑/删除（#5），
> 实际调整为**先做整页编辑器与删除**。原因：编辑器是纯新增 UI，不依赖 Repository
> 重构也能独立编译；而且用户能立刻感知输入体验提升（从单行输入框变成整页编辑器）。
> ViewModel 重构顺延为 #5，不影响后续任何步骤。
>
> **关于"有效提交"的判断标准**：每次提交都有明确的、可独立描述的功能增量，
> 且能编译通过。不使用"格式化代码"、"修个错别字"这类无意义提交充数。

---

## 4. 模块架构设计

### 4.1 整体依赖方向

```
              ┌─────────────────────────────┐
              │            app              │  可运行 APK
              │  UI 层 + ViewModel 层        │  MainActivity / NoteListFragment
              │  NoteEditorFragment         │  NoteDetailFragment
              └──────┬───────────────┬──────┘
                     │               │
        implementation│               │implementation
                     ▼               ▼
   ┌─────────────────────┐   ┌──────────────────────────┐
   │        core         │◄──│    feature_sharecard     │
   │  数据层 + 领域逻辑    │   │  P4 分享卡片插件          │
   │  Entity/DAO/Database│   │  ShareCardActivity       │
   │  Repository         │   │  CardRenderer            │
   │  Hilt Module        │   │  CsvExporter             │
   │  ConflictResolver   │   │  ShareCardEntry (API)    │
   └─────────────────────┘   └──────────────────────────┘
            ▲
            │ 二者都只依赖 core，core 不依赖任何业务模块
```

**铁律**：
- `core` **绝不**依赖 `app` 或 `feature_sharecard`（否则循环依赖，Gradle 直接报错）
- `app` 和 `feature_sharecard` **互不依赖**
- 两者通过 `core` 的 Repository 交换数据，**不直接互相 import 对方的类**

`app/build.gradle.kts` 需要新增一行才能启用 P4：
```kotlin
implementation(project(":feature_sharecard"))
```

### 4.2 `core` 模块设计 —— 数据层与领域逻辑

**包结构**
```
core/src/main/java/com/example/core/
├── data/
│   ├── entity/
│   │   └── Note.kt                    # Room 实体
│   ├── dao/
│   │   └── NoteDao.kt                 # 数据访问接口
│   ├── NoteDatabase.kt                # RoomDatabase 抽象类
│   ├── DatabaseModule.kt              # Hilt 模块，提供 DB 与 DAO
│   └── repository/
│       ├── NoteRepository.kt          # 仓储接口
│       └── NoteRepositoryImpl.kt      # 仓储实现（Hilt 绑定）
├── domain/
│   └── ConflictResolver.kt            # LWW 冲突解决策略（纯 Kotlin，好测）
└── security/
    ├── CryptoManager.kt               # 字段加解密
    └── BiometricAuthenticator.kt      # 生物识别封装
```

**`Note` 实体（已定稿，后续所有功能依赖此 schema）**

```kotlin
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val isArchived: Boolean = false,      // 归档
    val isPinned: Boolean = false,        // 置顶
    val isEncrypted: Boolean = false,     // 是否加密（阶段5）
    val lastModified: Long = System.currentTimeMillis(),  // ★ LWW 冲突解决核心字段
    val version: Int = 1                                   // ★ 冲突解决版本号
)
```

> **注意**：`lastModified` 和 `version` 不是可选项。现有的 `NoteViewModel.kt` 已经在调用
> `note.copy(lastModified = ..., version = note.version + 1)`，缺了这两个字段会立刻编译失败。

**`NoteDao` 接口设计**

```kotlin
@Dao
interface NoteDao {
    // ===== 查询 =====
    /** 活跃笔记：未归档。置顶优先，其次按最后修改时间倒序。UI 列表用 */
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, lastModified DESC")
    fun getActiveNotes(): Flow<List<Note>>

    /** 已归档笔记 */
    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY lastModified DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    /** 全部笔记。P4 模块导出 CSV / 生成卡片用 */
    @Query("SELECT * FROM notes ORDER BY lastModified DESC")
    suspend fun getAllNotesOnce(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    // ===== 增删改 =====
    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    // ===== 状态切换（用 @Transaction 保证读-改-写原子性）=====
    @Transaction
    suspend fun setArchived(id: Long, archived: Boolean) {
        getNoteById(id)?.let {
            updateNote(it.copy(isArchived = archived, lastModified = System.currentTimeMillis(), version = it.version + 1))
        }
    }

    @Transaction
    suspend fun setPinned(id: Long, pinned: Boolean) {
        getNoteById(id)?.let {
            updateNote(it.copy(isPinned = pinned, lastModified = System.currentTimeMillis(), version = it.version + 1))
        }
    }
}
```

**为什么 DAO 方法上有 `@Transaction` 的默认实现**：这是 Kotlin 接口默认方法 + Room 的组合用法。
`getNoteById` → `copy` → `updateNote` 三步必须原子，否则并发时可能覆盖别人的修改。

**`NoteDatabase` 与 `DatabaseModule`**

```kotlin
@Database(entities = [Note::class], version = 1, exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
```

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideNoteDatabase(@ApplicationContext context: Context): NoteDatabase =
        Room.databaseBuilder(context, NoteDatabase::class.java, "note_database.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoteDao(database: NoteDatabase): NoteDao = database.noteDao()
}
```

> **你之前贴的 `DatabaseModule.kt` 代码本身是正确的**，问题只在于它从未被写入磁盘。
> 唯一调整：我把 `=` 表达式体替代了 `return`，语义完全相同。

**`NoteRepository` 为什么需要**：Clean Architecture 要求 UI 层不直接接触 DAO。
`app` 的 ViewModel 只依赖 `NoteRepository` 接口，这样单元测试可以传入假实现，
不需要真实的 Room 数据库——**这是覆盖率能到 60% 的关键前提**。

### 4.3 `app` 模块设计 —— UI 层

**包结构**
```
app/src/main/java/com/example/mycoursedesign/
├── MyApplication.kt              # @HiltAndroidApp
├── MainActivity.kt               # 承载 Fragment，提供 P4 分享入口
├── ui/
│   ├── list/
│   │   ├── NoteListFragment.kt   # 笔记列表
│   │   ├── NoteListViewModel.kt
│   │   └── NoteAdapter.kt        # ListAdapter + DiffUtil
│   ├── detail/
│   │   ├── NoteDetailFragment.kt # 查看 / 编辑 / 删除
│   │   └── NoteDetailViewModel.kt
│   └── editor/
│       ├── NoteEditorFragment.kt # 新建 / 编辑 + Markdown 预览
│       └── NoteEditorViewModel.kt # 含草稿自动保存
└── util/
    └── MarkdownRenderer.kt       # Markdown → SpannableString
```

**关键决策：启用 `viewBinding`，用 Fragment 而非单 Activity 堆 View**

`app/build.gradle.kts` 现在是 `viewBinding = false`，这是你为绕开 AGP 报错临时关掉的。
阶段3 我会**重新开启**它，理由：

- 项目里 `FirstFragment` / `SecondFragment` 已经在用 `FragmentFirstBinding`（AS 模板自带），
  关掉 viewBinding 会让这两个文件编译失败——**这就是当前报错的第二个来源**。
- 更省事的路是删掉这两个模板 Fragment（阶段1 就做），那确实可以永久保持 `viewBinding = false`。
- 但**开启 viewBinding 后写 UI 更不容易出错**（`binding.fabAdd` 而不是 `findViewById<FloatingActionButton>(R.id.fabAdd)`），
  而且这是企业级项目的标准做法。用户明确要求"不要乱改配置"，所以**这一项我会先问你**再动。

**两条路线，二选一：**

| 路线 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| **A（保守）** | 保持 `viewBinding = false`，全程 `findViewById` | 零配置改动，符合你的"别乱动"要求 | 代码啰嗦，Fragment 里要手写 `lateinit var` |
| **B（推荐）** | 开启 `viewBinding = true`，用 Binding 类 | 代码更短更安全，符合现代 Android 实践，老师印象分更高 | 改了一行 build 配置（不涉及版本号） |

> 注意：**两条路线都不涉及任何依赖版本变更**，只是 `buildFeatures` 里的一个布尔值。

**`NoteAdapter` 改造要点**

现在是 `notifyDataSetChanged()`（全量刷新，有闪烁、无动画）。
阶段3 改用 `ListAdapter` + `DiffUtil.ItemCallback`：

```kotlin
class NoteAdapter(private val onClick: (Note) -> Unit) :
    ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(a: Note, b: Note) = a.id == b.id
            override fun areContentsTheSame(a: Note, b: Note) = a == b
        }
    }
    // ...
}
```

**这与"冲突解决机制"是同一个考点**：`DiffUtil` 判断"哪些条目变了"靠的是 `id` 稳定 + 内容比较，
和 LWW 判断"哪份数据更新"靠的是时间戳 + 版本号，思路一脉相承。写报告时可以这样串起来讲。

**✅ 已实现**：`NoteAdapter` 已是 `ListAdapter` + `DiffUtil`，并新增了长按回调
`onItemLongClick`，用于弹出删除菜单。

#### 4.3.1 笔记编辑器（已实现）

**已落地的包结构**（`app` 模块当前实际结构）：

```
app/src/main/java/com/example/mycoursedesign/
├── MyApplication.kt            # @HiltAndroidApp
├── MainActivity.kt             # 列表页：新建入口、点击编辑、长按删除
├── NoteAdapter.kt              # ListAdapter + DiffUtil + 点击/长按回调
├── NoteViewModel.kt            # 列表 ViewModel（⚠️ 仍直接注入 NoteDao，待迁移）
├── NoteEditorActivity.kt       # ★ 整页编辑器
└── NoteEditorViewModel.kt      # ★ 编辑器 ViewModel（已使用 NoteRepository）
```

**为什么用独立的 Activity 而不是 Fragment**

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| 独立 Activity | 返回栈天然正确；`adjustResize` 行为简单；全屏编辑不受列表布局干扰 | 两个 Activity 间靠 Intent 传参 | ✅ **采用** |
| Fragment（单 Activity） | 切换动画灵活 | 需要 Navigation 组件 + 返回栈手动管理，键盘处理更麻烦 | ❌ 不采用 |

**两种模式的参数传递**

```kotlin
// 调用方：新建
startActivity(NoteEditorActivity.newIntent(this, NoteEditorActivity.NEW_NOTE_ID))
// 调用方：编辑
startActivity(NoteEditorActivity.newIntent(this, note.id))
```

`noteId` 通过 Intent extra 传入，`NoteEditorViewModel` 用 `SavedStateHandle`
直接读出来，**Activity 不需要手动往 ViewModel 传参**。

**多行输入的关键属性**（这是从"单行输入框"升级到"整页编辑器"的核心）

| 属性 | 值 | 作用 |
|---|---|---|
| `inputType` | `textMultiLine` | ★ 不设它就会横向滚动而不换行 |
| `gravity` | `top\|start` | 默认垂直居中，不设的话内容少时浮在屏幕中间 |
| `background` | `@null` | 去掉输入框边框，看起来像一张白纸 |
| `layout_height` | `0dp` + 上下约束 | 填满标题下方到屏幕底部的全部空间 |
| `scrollbars` | `vertical` | 长内容可滚动 |
| `windowSoftInputMode` | `adjustResize`（Manifest） | 键盘弹出时压缩窗口而非推走界面 |

#### 4.3.2 删除功能的完整链路（已实现）

```
长按卡片
  → NoteAdapter.onItemLongClick
  → MainActivity.showNoteOptions()      弹出菜单（数组 + when，便于后续加"置顶/归档"）
  → 选「删除」
  → MainActivity.confirmDelete()        二次确认，防手滑
  → NoteViewModel.deleteNote(note)
  → NoteDao.deleteNote()
  → Room 删除记录
  → getActiveNotes() 的 Flow 自动发射新列表
  → ListAdapter + DiffUtil 自动播放删除动画
```

**注意最后两步：删除后不需要手动通知列表刷新**，Flow 会自动推送新数据。
这是 Room + Flow + DiffUtil 组合的核心优势，答辩时可以重点强调。

### 4.4 `feature_sharecard` 模块设计 —— P4 插件

**定位（按用户明确要求）**：一个**插件式的独立功能模块**——它有自己的 `ShareCardActivity` 入口，
可以独立运行、独立开发、在 Git 中独立追溯；同时它被 `app` 依赖，用户从主 App 也能进入。

**包结构**
```
feature_sharecard/src/main/java/com/example/feature_sharecard/
├── ShareCardEntry.kt          # ★ 对外唯一入口（插件 API 契约）
├── ShareCardActivity.kt       # 独立可运行的界面
├── ShareCardViewModel.kt      # 依赖 core 的 NoteRepository 读数据
├── render/
│   └── CardRenderer.kt        # Bitmap + Canvas 绘制分享卡片
├── export/
│   └── CsvExporter.kt         # CSV 导出（UTF-8 BOM 防中文乱码）
└── share/
    └── FileShareHelper.kt     # FileProvider + Intent.ACTION_SEND
```

**插件契约 `ShareCardEntry.kt`**

```kotlin
/**
 * P4 插件对外唯一入口。
 * app 模块只通过这个类启动插件，不 import 插件内部的任何其他类，
 * 这样插件的内部实现可以随意重构，而不影响主 App。
 */
object ShareCardEntry {
    /** 主 App 调用：启动分享卡片界面 */
    fun newIntent(context: Context, noteId: Long? = null): Intent =
        Intent(context, ShareCardActivity::class.java).apply {
            noteId?.let { putExtra(ShareCardActivity.EXTRA_NOTE_ID, it) }
        }
}
```

`app` 侧只需要两行：
```kotlin
startActivity(ShareCardEntry.newIntent(this, note.id))
```

**三个核心功能的实现要点**

| 功能 | 技术点 | 易踩的坑 |
|---|---|---|
| 生成分享卡片 | `Bitmap.createBitmap()` + `Canvas.drawText()` + `StaticLayout` 自动换行 | 中文字体测量要用 `Paint.measureText` 逐行截断；`StaticLayout` 在 API 23+ 才支持 `Builder` |
| 导出 CSV | `OutputStreamWriter(out, "UTF-8")` 先写 `\uFEFF` BOM | **不写 BOM 时 Excel 打开中文必然乱码**（这是 P4 的经典考点） |
| FileProvider 分享 | `FileProvider.getUriForFile()` + `ACTION_SEND` + `FLAG_GRANT_READ_URI_PERMISSION` | 必须在 Manifest 配 `<provider>` 和 `res/xml/file_paths.xml`；漏了 flag 对方 App 会读不到文件 |

**关于"独立 APK"的架构说明（重要，写报告时用）**

需求写的是 P4"独立可运行，读取操作主 App 数据"。这里有**两种理解**，我们采用方案一：

| 方案 | 能否读到主 App 数据 | 结论 |
|---|---|---|
| **方案一（采用）**：`feature_sharecard` 是 library 模块，被 `app` 依赖，有独立 `ShareCardActivity` 入口 | ✅ 同进程、同数据库，直接读 | **采用**。运行时共享 App 沙箱，代码上完全独立，Git 历史独立 |
| 方案二（不采用）：`feature_sharecard` 编译成独立 APK | ❌ Android 应用沙箱隔离，跨 App 读不到 Room 数据库 | 除非改用 ContentProvider / sharedUserId，工程量激增且偏离课设重点 |

**所以"独立运行"我们这样满足**：它可以独立开发、独立测试、独立提交，
通过 `ShareCardActivity` 独立作为界面入口存在；同时作为插件被主 App 装载。
这既满足了"独立可追溯"，又解决了数据共享的现实约束。

---

## 5. 关键技术点考核对照表（老师会逐条问）

| 考核点 | 实现位置 | 怎么讲 |
|---|---|---|
| Room + Flow + 协程 | `core/data/` | Room 提供 `Flow`，数据一变 UI 自动刷新，无需手动 requery |
| Clean Architecture | 三层：`ui/` → `repository/` → `dao/` | UI 不知道数据库存在，只认 Repository 接口 |
| MVVM | `app/ui/*/​*ViewModel.kt` | ViewModel 持有 `StateFlow`，Fragment 只 observe，旋转屏幕不丢数据 |
| Hilt 依赖注入 | `@HiltAndroidApp` / `@AndroidEntryPoint` / `@HiltViewModel` / `@Module` | 对照 Java 的 Spring `@Autowired` |
| 单元测试 ≥60% | `core/src/test/`、`app/src/test/` | 优先测 `ConflictResolver`、`CsvExporter`、`MarkdownRenderer` 等纯逻辑类 |
| 冲突解决机制 | `core/domain/ConflictResolver.kt` | LWW：比较 `lastModified`，相同则比 `version`，再相同取内容哈希 |
| Markdown 实时预览 | `app/util/MarkdownRenderer.kt` | `TextWatcher` → `SpannableString`，不用第三方库（防抄袭） |
| 草稿自动保存 | `NoteEditorViewModel` | 输入停止 800ms 后写 Room，用 `Flow` debounce |
| 加密 + 生物识别 | `core/security/` | `EncryptedSharedPreferences` 存主密钥，`BiometricPrompt` 校验后解密 |
| 归档置顶 | `NoteDao` + 列表排序 | 置顶优先 + 时间倒序的 SQL 排序 |
| 暗色主题 | `values-night/themes.xml` | 已存在，跟随系统自动切换 |

---

## 6. 版本锁定清单（绝对不可修改）

| 项 | 锁定值 | 说明 |
|---|---|---|
| Gradle | 8.2 | 本地已部署，**wrapper 的 file:// URL 不要改**，原因见下 |
| AGP | 8.2.2 | |
| Kotlin | 1.9.22 | |
| KSP | 1.9.22-1.0.17 | 必须与 Kotlin 严格对应 |
| Java | 17 | 构建时用 `C:\Users\dashen\.jdks\ms-17.0.20.1` |
| compileSdk / targetSdk | 34 | |
| minSdk | 24 | |
| Room | 2.6.1 | 本地已缓存 |
| Hilt | 2.51.1 | 本地已缓存 |
| Navigation | 2.7.7 | 本地已缓存 |

**新增依赖一律不加**——所有需要的库（Room / Hilt / security-crypto / biometric / material / lifecycle / 协程）
都已经写在 `libs.versions.toml` 和 `core/build.gradle.kts` 里，且本地缓存齐全。**不需要联网。**

### ★ 关于 `gradle-wrapper.properties` 的 `distributionUrl`

当前值（**保持不动**）：
```properties
distributionUrl=file\:///C:/Users/dashen/.gradle/wrapper/dists/gradle-8.2-bin/manual/gradle-8.2-bin.zip
```

**曾尝试改回官方 HTTPS 地址，实测失败并已回滚。** 原因：

Gradle Wrapper 用 **`distributionUrl` 的哈希值**作为缓存目录名，放在
`~/.gradle/wrapper/dists/gradle-8.2-bin/<hash>/` 下。一旦 URL 变化，哈希随之改变，
Gradle 会认为"这个发行包从未下载过"，于是**尝试重新下载**。
而本机网络访问 `services.gradle.org` 会被卡死（与 GitHub 同样的封锁问题），
构建将无限期挂起。**实测已复现：命令卡死超过 10 分钟，只能强制终止。**

| 影响 | 说明 |
|---|---|
| 本机开发 | ✅ 用 file:// 地址，构建正常 |
| 老师 clone 后构建 | ⚠️ **会失败**（他的机器上没有这个 zip） |

**结论与建议（答辩时若被问起，可这样回答）**：
这是"极端网络环境下的妥协方案"。要在评分机器上也能构建，需要把 Gradle 发行包
一并交付（或改用校内 Maven 镜像），而不是简单改 URL —— 那会把本机的构建也弄坏。
**因此此项不列入提交计划**，保持现状。

> 如果老师明确要求"clone 下来必须能直接跑"，可行的做法是：
> 把 `gradle-8.2-bin.zip`（约 100 MB）放到仓库的 `gradle/wrapper/` 下并提交，
> 同时把 URL 改成相对路径 `distributionUrl=gradle-8.2-bin.zip`。
> 这会让仓库体积变大，但能保证任何机器可构建。**需用户确认后再做。**

---

## 7. 构建与验证命令

```powershell
# 固定使用 JDK 17（默认 JAVA_HOME 是 JDK 24，有兼容风险）
$env:JAVA_HOME="C:\Users\dashen\.jdks\ms-17.0.20.1"

# 编译主 App
# 注意：首次构建若报 "No cached version of com.google.dagger:hilt-compiler"，
#       去掉 --offline 联网补齐一次即可，之后可继续离线构建。
.\gradlew.bat :app:assembleDebug --offline --console=plain

# 只编译 core 模块（Android Studio 没有这个菜单项，命令行才有）
.\gradlew.bat :core:compileDebugKotlin --offline --console=plain

# 跑单元测试
.\gradlew.bat testDebugUnitTest --offline --console=plain

# 查看某个模块的详细报错（AS 里看不到的，都靠这个）
.\gradlew.bat :core:compileDebugKotlin --stacktrace --info
```

### 模拟器（统一使用 E 盘的 Pixel_10_Pro）

```powershell
# ★ 关键：不设这个变量，emulator 会去 C:\Users\<用户>\.android 找 AVD 并新建到 C 盘
$env:ANDROID_PREFS_ROOT = "E:\.android"

# 确认能找到 AVD（应输出 Pixel_10_Pro）
& "E:\android\Android\Sdk\emulator\emulator.exe" -list-avds

# 启动
Start-Process "E:\android\Android\Sdk\emulator\emulator.exe" `
  -ArgumentList "-avd","Pixel_10_Pro","-no-snapshot-load","-no-boot-anim","-gpu","auto"

# 等待引导完成（返回 1 即可）
& "E:\android\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell getprop sys.boot_completed

# 安装并启动
& "E:\android\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
& "E:\android\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell am start -n "com.example.mycoursedesign/.MainActivity"

# 关闭
& "E:\android\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 emu kill
```

> **`ANDROID_USER_HOME` vs `ANDROID_PREFS_ROOT`**：Windows 版 emulator **只认后者**。
> 用户级已设置 `ANDROID_USER_HOME=E:\.android`，但 emulator 不读它，
> 因此每次启动前必须显式设 `ANDROID_PREFS_ROOT`。详见 `PROGRESS.md` "模拟器环境"。

> **回答"Android Studio 里没有单独编译 core 的选项"**：AS 的 Build 菜单只提供
> `Make Project` / `Make Module 'app'`。要看某个 library 模块的真实报错，
> 正确做法是 **Build 面板里展开完整的 Gradle task 树**，或者用上面的命令行。
> 另外 `:core:compileDebugKotlin NO-SOURCE` 的含义是"没有源码可编译"——
> 这正是我们当前的问题。

---

## 8. 会话交接说明（Token 用尽后如何继续）

**本节写给"下一个 AI"和"三天后的你自己"。**

### 8.1 接手时先读什么

按顺序读，不要跳：

1. `docs/PROJECT_PLAN.md`（本文档）—— 目标、架构、22 次提交规划
2. `docs/PROGRESS.md` —— 当前进度快照，写明"上一次做到哪、下一步做什么"
3. `git log --oneline --stat` —— 看真实提交历史，**以 Git 为准，不要相信口头描述**
4. `core/src/main/java/` 与 `app/src/main/java/` —— 看实际代码

### 8.2 每完成一个阶段必须做的事

1. 实际运行 `.\gradlew.bat :app:assembleDebug --offline` 确认编译通过
2. `git add` + `git commit`（提交信息按第 3 节的表格）
3. **更新 `docs/PROGRESS.md`**：勾选完成项、写清下一步
4. `git push`

**这样即使对话中断，下一个 AI 只要 `git clone` 就能无缝接手。**

### 8.3 给下一个 AI 的开场白模板

```
我在做 Android 移动应用开发课设，项目在 E:\MyCourseDesign，GitHub 仓库已配好。
请先读 docs/PROJECT_PLAN.md 和 docs/PROGRESS.md，然后运行 git log --oneline 和
git status 确认真实进度，再继续开发。

重要约束：
1. 严禁升级任何依赖版本（详见 PROJECT_PLAN.md 第 6 节），不要加新依赖，全部本地已缓存
2. 构建必须用 JDK 17：$env:JAVA_HOME="C:\Users\dashen\.jdks\ms-17.0.20.1"
3. 构建命令：.\gradlew.bat :app:assembleDebug --offline --console=plain
4. 每完成一个阶段要跑通编译再提交，并更新 docs/PROGRESS.md
5. 代码注释请附 Java 对照说明（我是 Java 转 Android 的学生）
```

### 8.4 环境注意事项（踩过的坑，别重复踩）

| 坑 | 现象 | 解法 |
|---|---|---|
| 工作区沙箱 ACL 失败 | 所有 shell 命令报 `SetNamedSecurityInfoW failed (Win32 5)` | 执行命令时申请完整权限（danger-full-access） |
| 默认 JDK 是 24 | Gradle 8.2 有兼容风险 | 显式设 `JAVA_HOME` 指向 JDK 17 |
| Gradle Wrapper 写死本机路径 | 别人 clone 后无法构建 | **不要改 URL**（实测会导致本机构建卡死），见第 6 节说明 |
| `core` 无源码 | `:core:compileDebugKotlin NO-SOURCE`，app 报 `error.NonExistentClass` | 补齐 core 四件套（提交 #3） |
| AS 模板残留 | `FirstFragment` 用 `FragmentFirstBinding` 但 viewBinding 已关 | 删除模板文件（提交 #2） |
| 无 AVD / 无真机 | `adb devices` 为空 | 需在 AS 里创建模拟器，或真机开 USB 调试 |

### 8.5 当前已知的待决问题

1. **viewBinding 开还是不开**？—— ✅ 已确认开启（路线 B），已在提交 `20fa3c1` 落地
2. **`item_note.xml` 的 `CardView` 是否改用 `MaterialCardView`**？—— ✅ 已确认改用，省掉一个依赖
3. **`gradle-wrapper.properties` 的 URL 是否改回官方**？—— ❌ **已实测否决**：改 URL 会使缓存哈希失效、触发重新下载，而 `services.gradle.org` 在本机被卡死，构建无限期挂起。**保持 file:// 现状**，详见第 6 节。
4. **模拟器中文输入**？—— ⚠️ 待解决，见第 9 节说明

---

## 9. 压力测试计划（项目功能完成后执行）

> **触发条件**：主功能（增删改查、Markdown、加密、P4 分享）全部完成后执行。
> **目的**：验证"真实场景可用"，而不是"点两下能跑"。
> **原则**：轻量、可重复、有量化指标，**不追求压测工具的重型方案**。

### 9.1 数据量压力（验证列表性能）

| 用例 | 方法 | 通过标准 |
|---|---|---|
| 批量插入 | 写一个临时 `@Test` 或用 `adb shell` 循环插入 500 / 1000 条笔记 | App 启动不 ANR，列表滚动流畅 |
| 长文本 | 单条笔记正文 10 万字符（约 100 KB） | 编辑器打开 < 2s，输入不卡顿 |
| 超长标题 | 标题 500 字符 | 列表卡片正确截断显示，不撑破布局 |
| 空数据 | 清空数据库后打开 | 显示空状态，不崩溃 |

**量化方式**：`adb shell dumpsys gfxinfo com.example.mycoursedesign` 看帧率；
`logcat` 里搜 `Skipped XXX frames` 判断卡顿。

### 9.2 边界与异常输入

| 用例 | 输入 | 通过标准 |
|---|---|---|
| 全空 | 标题空 + 内容空 → 保存 | 拒绝保存并提示 |
| 仅空格 | 全空格 → 保存 | 应 trim 后视为空，拒绝保存 |
| emoji / 特殊符号 | `😀🎉 <>&"'\n\t` | 正确显示与存储，不崩溃、不破坏 XML |
| 极长单词 | 无空格连续 1000 字符 | 自动换行而非横向溢出 |
| 快速连点保存 | 连续点保存按钮 10 次 | 不产生重复记录（或至少不崩溃） |

### 9.3 生命周期与配置变更

| 用例 | 方法 | 通过标准 |
|---|---|---|
| 旋转屏幕 | 编辑器内旋转 | 已输入内容不丢失 |
| 后台杀进程 | 编辑中按 Home → `adb shell am kill` → 回来 | 不崩溃（数据可能丢，属预期，后续草稿功能解决） |
| 深色模式切换 | 编辑中切暗色 | 文字保持可读，无黑底黑字 |
| 字体缩放 | 设置里把字体调到最大 | 布局不错位、不重叠 |
| 分屏 / 小窗 | 进入分屏模式 | 列表与编辑器均可用 |

### 9.4 并发与数据一致性

| 用例 | 方法 | 通过标准 |
|---|---|---|
| 快速增删 | 脚本连续 50 次"新建→删除" | 最终数据库行数正确，无残留 |
| 编辑中删除 | 打开编辑页，用 adb 从数据库删掉该条，再点保存 | 不崩溃，给出合理提示 |
| 版本号递增 | 连续编辑同一条 20 次 | `version` 最终为 21，`lastModified` 单调递增 |
| LWW 冲突 | 构造两条同 id 不同 `lastModified` 的记录，跑冲突解决逻辑 | 时间戳大的胜出（功能完成后验证） |

### 9.5 分享与导出（P4 完成后）

| 用例 | 通过标准 |
|---|---|
| 导出 1000 条到 CSV | 文件完整，Excel 打开中文不乱码（BOM 生效） |
| 超长笔记生成分享卡片 | 文字自动截断/省略，不溢出图片边界 |
| 分享到其他 App | 微信/文件管理器能正常打开 |

### 9.6 内存与稳定性

| 用例 | 方法 | 通过标准 |
|---|---|---|
| 长时间滚动 | 1000 条笔记列表来回滚动 2 分钟 | 内存不持续增长（`adb shell dumpsys meminfo`） |
| 反复进出编辑页 | 进出 30 次 | 无内存泄漏（`LeakCanary` 可选，或观察 meminfo） |
| Monkey 测试（可选） | `adb shell monkey -p com.example.mycoursedesign -v 2000` | 无崩溃 |

### 9.7 执行记录

**每轮压测的结果必须记录在本节**，格式：

```
日期 | 用例编号 | 环境 | 结果 | 发现的问题 | 修复提交
```

---

## 10. 关于模拟器中文输入（已知限制）

**现象**：在模拟器里无法输入中文（手动敲键盘只能出英文）。

**这是模拟器/输入法的问题，不是 App 的问题。证据**：

- App 的界面文字全是中文（「新建笔记」「保存」「删除」），**渲染完全正常**
- `strings.xml` 全部是 UTF-8 中文，构建与显示均无问题
- 只是"输入"环节没有中文输入法可用

**原因**：

1. AVD 配置里 `hw.keyboard=yes`，直接使用**物理键盘**（主机键盘），
   而物理键盘只发送 ASCII/英文字符，没有拼音输入法参与；
2. 硬件键盘启用时，Android 通常**不弹出软键盘**，所以看不到 Gboard；
3. 即使弹出，全新 AVD 的 Gboard 默认只装了 English (US) 布局，
   需要手动添加中文（拼音）语言包。

**解决方案（按推荐顺序）**：

| 方案 | 做法 | 说明 |
|---|---|---|
| **A. 关闭硬件键盘 + 启用中文** | 把 AVD 的 `hw.keyboard` 改为 `no`，重启模拟器；然后在 设置 → 系统 → 语言和输入法 → 屏幕键盘 → Gboard → 语言 → 添加键盘 → 中文（拼音） | 最接近真实手机体验；需要能联网下载语言包 |
| **B. 用 `adb` 注入中文** | 安装 ADBKeyboard 之类的辅助输入法，用 `adb shell am broadcast` 发送 Unicode 文本 | 适合自动化测试，但**不是真实用户输入路径** |
| **C. 真机测试** | 用 USB 连接真实 Android 手机 | 最真实，且能验证指纹识别等模拟器不支持的硬件能力 |

**重要**：App 本身对中文**完全支持**（数据库是 SQLite TEXT、Room 默认 UTF-8），
不需要任何代码改动。这纯粹是测试环境的输入手段问题。

**验证中文存储的正确方法**（不依赖输入法）：
用 `adb exec-out` 导出数据库，用 Python/sqlite3 直接查询，确认中文内容正确落库。
文档已多次使用该方法验证英文内容，同样适用于中文。
