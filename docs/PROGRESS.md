# 进度快照（Progress Snapshot）

> **每完成一个阶段，必须更新本文件并随代码一起提交。**
> 下一个接手者请以本文件 + `git log` 为准判断进度。

**最后更新**：2026-10-15

---

## 当前状态：项目已可编译、可运行、功能已实测通过

| 项 | 状态 |
|---|---|
| Android Studio Sync | ✅ 成功 |
| **项目能否编译** | ✅ **`:app:assembleDebug` BUILD SUCCESSFUL** |
| **APK 能否安装运行** | ✅ 已在模拟器实测，无崩溃 |
| **笔记新增功能** | ✅ 已实测，数据真实落库 |
| 有效提交次数 | **2 / 12**（要求 ≥10） |
| 模拟器 | ✅ `MyCourseDesign_API37`（Android 17 / API 37.2 / 16KB 页大小） |

---

## 提交进度对照

| # | 提交 | 状态 |
|---|---|---|
| 1 | `feat: 初始化Android课设项目` | ✅ `de0231a` |
| 2+3 | `feat: 补齐core数据层并修复编译错误` | ✅ **本次提交**（原计划的 #2、#3 已合并，原因见下） |
| 4 | `feat(app): 补全笔记增删改查与DiffUtil刷新` | ⬜ 待做 |
| 5 | `feat(app): 新增归档置顶与暗色主题` | ⬜ 待做 |
| 6 | `feat(app): Markdown实时预览与草稿自动保存` | ⬜ 待做 |
| 7 | `feat(core): 笔记加密与生物识别解锁` | ⬜ 待做 |
| 8 | `feat(core): 基于lastModified的LWW冲突解决机制` | ⬜ 待做 |
| 9 | `feat(sharecard): 分享卡片生成/CSV导出/FileProvider分享` | ⬜ 待做 |
| 10 | `test: 补充单元测试并将覆盖率提升至60%以上` | ⬜ 待做 |
| 11 | `docs: 添加README与架构说明` | ⬜ 待做 |
| 12 | `chore: 修复Gradle Wrapper跨机器可用性并完善gitignore` | ⬜ 待做 |

### 为什么 #2 和 #3 合并成了一次提交

原计划把"清理AS模板"和"实现core数据层"分成两次提交。实际执行时发现二者**强耦合**：

- 删除 `FirstFragment` / `SecondFragment` 后，必须同时决定 `viewBinding` 开还是关
  （用户已确认开，路线 B）；
- 而开启 `viewBinding` 后 `MainActivity` 的原写法 `viewModel.activeNotes.observe(this)`
  会编译失败（`StateFlow` 没有 `observe()` 方法），必须一并改成 `repeatOnLifecycle`。

如果强行拆成两次，中间那次提交是**编译不过**的，违反"每次提交都必须可编译"的原则。
因此合并为一次语义完整的提交。

---

## 本次提交完成的工作

### 新增：core 数据层（4 个文件）

| 文件 | 作用 |
|---|---|
| `core/data/entity/Note.kt` | Room 实体，含 `lastModified` / `version`（冲突解决核心字段） |
| `core/data/dao/NoteDao.kt` | DAO，含 `Flow` 查询、增删改、`@Transaction` 状态切换 |
| `core/data/NoteDatabase.kt` | RoomDatabase，`version = 1`，`exportSchema = true` |
| `core/data/DatabaseModule.kt` | Hilt 模块，提供数据库与 DAO 单例 |

配套：`core/build.gradle.kts` 增加 `room.schemaLocation`，
schema 已导出到 `core/schemas/com.example.core.data.NoteDatabase/1.json`（已提交）。

### 修改：app 模块

| 文件 | 改动 |
|---|---|
| `app/build.gradle.kts` | `viewBinding = false` → `true` |
| `MainActivity.kt` | `observe()` → `repeatOnLifecycle`；改用 ViewBinding；`trim()` 后再判断空 |
| `NoteAdapter.kt` | `RecyclerView.Adapter` → `ListAdapter` + `DiffUtil`；改用 ViewBinding；增加状态徽章 |
| `item_note.xml` | `CardView` → `MaterialCardView`（省一个依赖）；硬编码颜色改为主题属性；增加 `tvBadge` |
| `strings.xml` | 移除 AS 模板字符串 |

### 删除：AS 导航模板残留（11 个文件 + 5 个空目录）

```
FirstFragment.kt / SecondFragment.kt
fragment_first.xml / fragment_second.xml / content_main.xml
nav_graph.xml / menu_main.xml
dimens.xml / values-land / values-w600dp / values-w1240dp
```

> 这些是 Android Studio "Basic Views Activity" 模板自带的示例代码，
> 与笔记 App 无关，且会因 `viewBinding` 开关状态直接导致编译失败。

---

## 实测结果（模拟器 emulator-5554）

| 验证项 | 结果 |
|---|---|
| `:core:kspDebugKotlin` | ✅ 通过（原 `error.NonExistentClass` 消失） |
| `:app:assembleDebug` | ✅ BUILD SUCCESSFUL，APK 7.12 MB |
| `adb install` | ✅ Success |
| 启动 `MainActivity` | ✅ `topResumedActivity` 正确，进程存活无崩溃 |
| 空列表渲染 | ✅ Material 3 主题 + FAB 正常（`docs/screenshots/`） |
| 新建笔记流程 | ✅ 标题 `Hello-Room`、内容 `First-note-from-emulator` |
| **数据库落库校验** | ✅ `sqlite3` 查询确认 `notes` 表 1 行，字段值全部正确 |
| 列表刷新（Flow 链路） | ✅ DiffUtil 自动刷新，卡片正确渲染 |

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
| Gradle Wrapper 写死本机路径 | 别人 clone 后无法构建 | 提交 #12 改回官方 URL |

---

## 下一步（给接手者）

**提交 #4：补全笔记增删改查**

1. 在 `core` 增加 `data/repository/NoteRepository.kt` + `NoteRepositoryImpl.kt`，
   用 Hilt 绑定接口（Clean Architecture 要求 UI 不直接依赖 DAO）
2. `NoteViewModel` 改为依赖 `NoteRepository`，并补上 `setPinned` / `setArchived` / `deleteNote`
3. `MainActivity` 增加长按菜单（置顶 / 归档 / 删除，删除带确认对话框）
4. 点击笔记进入编辑界面（`NoteEditorActivity` 或 Fragment）
5. 编译 + 模拟器实测 + 更新本文件 + 提交

**待用户确认的决策**（原 3 项已全部确认）：
1. ~~viewBinding~~ → ✅ 已确认开启（路线 B）
2. ~~MaterialCardView~~ → ✅ 已确认改用
3. ~~gradle-wrapper URL~~ → ✅ 已确认改，放在提交 #12
