# 进度快照（Progress Snapshot）

> **每完成一个阶段，必须更新本文件并随代码一起提交。**
> 下一个接手者请以本文件 + `git log` 为准判断进度。

**最后更新**：2026-10-16

---

## 当前状态：项目已可编译、可运行、功能已实测通过

| 项 | 状态 |
|---|---|
| Android Studio Sync | ✅ 成功 |
| **项目能否编译** | ✅ **`:app:assembleDebug` BUILD SUCCESSFUL** |
| **APK 能否安装运行** | ✅ 已在模拟器实测，无崩溃 |
| **笔记新增功能** | ✅ 已实测，数据真实落库 |
| 有效提交次数 | **2 / 22**（要求 ≥10） |
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

> 完整规划见 `PROJECT_PLAN.md` 第 3 节，共 22 次提交。
> 原则：**先加声明、再加实现、最后动 UI**，保证每次提交都能独立编译。

| # | 提交 | 状态 |
|---|---|---|
| 1 | `feat: 初始化Android课设项目` | ✅ `de0231a` |
| 2 | `feat: 补齐core数据层并修复编译错误，实现笔记新增功能` | ✅ `20fa3c1` |
| 3 | `refactor(core): 引入NoteRepository抽象，隔离UI与DAO` | ⬜ 下一步 |
| 4 | `refactor(app): NoteViewModel改为依赖NoteRepository` | ⬜ |
| 5 | `feat(app): 新增编辑与删除笔记功能` | ⬜ |
| 6 | `feat(app): 新增笔记置顶与归档功能` | ⬜ |
| 7 | `feat(app): 适配暗色主题并完善列表视觉` | ⬜ |
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
| 22 | `chore: 完善gitignore与gitattributes，规范仓库卫生` | ⬜ |

### 为什么 #2 是两次提交合并而来

原计划把"清理AS模板"和"实现core数据层"分成两次提交。实际执行时发现二者**强耦合**：

- 删除 `FirstFragment` / `SecondFragment` 后，必须同时决定 `viewBinding` 开还是关
  （用户已确认开，路线 B）；
- 而开启 `viewBinding` 后 `MainActivity` 的原写法 `viewModel.activeNotes.observe(this)`
  会编译失败（`StateFlow` 没有 `observe()` 方法），必须一并改成 `repeatOnLifecycle`。

如果强行拆成两次，中间那次提交是**编译不过**的，违反"每次提交都必须可编译"的原则。
**这是唯一一次合并**，后续按 22 次规划逐步推进，不再合并。

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
| **改 Gradle Wrapper URL 会让构建卡死** | 改回官方 HTTPS 后 `gradlew` 无限期挂起 | **不要改 URL**。Gradle 用 URL 的哈希定位缓存目录，改 URL = 缓存失效 = 重新下载，而 `services.gradle.org` 在本机被卡死。已实测复现并回滚 |
| `Set-Content -Encoding UTF8` 会写入 BOM | Java 读 properties 文件可能出错 | 用 `[System.IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding $false))` |
| 截图全黑≠崩溃 | 启动动画阶段截图得到全黑图 | 等 5~10 秒重截，或用日志 `Displayed <包名>/.MainActivity` 判定 |

---

## 下一步（给接手者）

**提交 #3：`refactor(core): 引入NoteRepository抽象，隔离UI与DAO`**

本步是**纯新增**，不改动任何现有代码行为，因此一定能单独编译通过：

1. 新建 `core/data/repository/NoteRepository.kt`（接口），转发 `NoteDao` 的全部方法
2. 新建 `core/data/repository/NoteRepositoryImpl.kt`，
   用 `@Inject constructor(private val noteDao: NoteDao)` 实现接口
3. 新建 `core/di/RepositoryModule.kt`，
   用 `@Binds` 把 `NoteRepositoryImpl` 绑定到 `NoteRepository`
4. 编译验证 → 更新本文件 → 提交

**后续步骤（详见 `PROJECT_PLAN.md` 第 3 节）**：
#4 ViewModel 改依赖 Repository → #5 编辑/删除 → #6 置顶/归档 → #7 暗色主题
→ #8~#10 加密与生物识别 → #11~#12 Markdown 与自动保存 → #13 冲突解决
→ #14~#18 P4 插件 → #19~#20 单元测试 → #21 README → #22 仓库收尾

**已确认的决策**：
1. ~~viewBinding~~ → ✅ 已确认开启（路线 B）
2. ~~MaterialCardView~~ → ✅ 已确认改用
3. ~~gradle-wrapper URL~~ → ❌ **已实测否决**，保持 file:// 现状（改 URL 会让本机构建卡死，详见 `PROJECT_PLAN.md` 第 6 节）
4. ~~模拟器位置~~ → ✅ 统一使用 E 盘 `Pixel_10_Pro`，C 盘 AVD 已删除（释放 5.2 GB）

**提交纪律（用户明确要求）**：
- 课设要求 ≥10 次有效提交，**不要把多个步骤合并成一次提交**
- 每次提交前必须 `:app:assembleDebug` 编译通过
- 推送失败时**重试一次即可** —— DevSidecar 边车会自动切换到可用 IP
