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
| 3 | 阶段3 | `refactor(core): 引入NoteRepository抽象，隔离UI与DAO` | ✅ 纯新增，无行为变化 | ⬜ |
| 4 | 阶段3 | `refactor(app): NoteViewModel改为依赖NoteRepository` | ✅ DAO 方法全量委托 | ⬜ |
| 5 | 阶段3 | `feat(app): 新增编辑与删除笔记功能` | ✅ | ⬜ |
| 6 | 阶段3 | `feat(app): 新增笔记置顶与归档功能` | ✅ | ⬜ |
| 7 | 阶段3 | `feat(app): 适配暗色主题并完善列表视觉` | ✅ | ⬜ |
| 8 | 阶段5 | `feat(core): Note实体新增加密相关字段与DAO支持` | ✅ 纯加字段 | ⬜ |
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
| 22 | 阶段6 | `chore: 完善gitignore与gitattributes，规范仓库卫生` | ✅ | ⬜ |

> **注意**：#2 由原计划的两次提交合并而来（原因见 `PROGRESS.md`）。
> 合并后仍有 22 次提交，相对 ≥10 次的要求有充足余量。
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

1. **viewBinding 开还是不开**？（第 4.3 节路线 A / B）—— 需用户确认
2. **`item_note.xml` 的 `CardView` 是否改用 `MaterialCardView`**？—— 建议改，可省一个依赖
3. **`gradle-wrapper.properties` 的 URL 是否改回官方**？—— ❌ **已实测否决**：改 URL 会使缓存哈希失效、触发重新下载，而 `services.gradle.org` 在本机被卡死，构建无限期挂起。**保持 file:// 现状**，详见第 6 节。
