# M3 — Projects, Storage & Git

**Depends on:** M0, M1 · **Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md
**Goal:** where projects live, how they are created/imported/exported, templates, git checkpoints, and the read-only file tree/viewer.

---

## 1. Storage decision (D5)

- Projects live in the rootfs home: **guest** `/root/projects/<slug>/` = **host** `PathProvider.projectsRoot/<slug>/`.
- Why: compilers and git need a Linux filesystem (symlinks, exec bits, fast IO). Shared storage is slow and lacks these [VERIFY on target device].
- The Android app reads/writes project files **directly through the host path** (file tree, viewer, agent file tools). Shell commands reach the same files through proot at the guest path.
- Finished artifacts (APK, zip) are **exported** to shared storage (`Downloads/{APP_NAME}/`).
- Uninstall or "Clear data" removes projects. The app must warn about this (see Backup reminders).
- Working inside an arbitrary shared-storage folder is *not* supported in MVP. Import copies it into a project.

## 2. Functional requirements

- M3-FR-01 **Create project**: name → slug (`ProjectSlugger`, unique, `[a-z0-9-]`), template picker, Android package name (default `com.example.<slug_without_dashes>`).
- M3-FR-02 **Templates** (assets): `empty`, `hello-java` (XML Views, framework widgets only). `hello-jni` shown disabled "Coming later".
- M3-FR-03 **List** projects: name, template, last modified, size, last build status; sort by recent; search.
- M3-FR-04 **Rename / duplicate / delete** (delete needs typed-name confirmation).
- M3-FR-05 **Import**: zip from system picker (SAF) with zip-slip protection; or "Import from shared folder" which copies the folder into the rootfs.
- M3-FR-06 **Export**: zip to a user-chosen SAF location or `Downloads/{APP_NAME}/`; excludes `build/` and `.git` by default (toggles).
- M3-FR-07 **Git** (required tool, D8): auto `git init` on create/import; **checkpoint** = `git add -A && git commit`; list history; diff summary; **restore** a checkpoint (creates a safety commit first). Optional remote + push with a token from `SecretStore` using an ephemeral askpass script (push is P2; the rest is MVP).
- M3-FR-08 **File tree**: lazy-loaded, sorted dirs first, icons by extension, sizes; hides `.git` and `build/` by default (toggle).
- M3-FR-09 **File viewer**: read-only, monospace, light syntax highlighting (java, xml, json, md, sh, gradle, properties), 512 KB cap with "show first 512 KB", line numbers, jump-to-line (used by build diagnostics), copy path, "Ask agent about this file" (emits a chat reference through a domain interface).
- M3-FR-10 **Backup reminder**: if ≥ 10 agent checkpoints since last export/push, or 7 days since last backup, show a non-blocking banner and a notification.
- M3-FR-11 **Per-project metadata** in `<project>/.agent/project.json` (id, name, slug, template, packageName, createdAt, lastBackupAt) mirrored in Room (`ProjectEntity`) as an index. If they disagree, the file wins; `ProjectIndexReconciler` repairs Room at app start.

## 3. Public interfaces

```kotlin
interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    suspend fun create(name: String, template: TemplateId, packageName: String?): AppResult<Project>
    suspend fun import(source: ImportSource): AppResult<Project>
    suspend fun export(projectId: String, target: ExportTarget, options: ExportOptions): AppResult<Uri>
    suspend fun rename(projectId: String, newName: String): AppResult<Project>
    suspend fun duplicate(projectId: String): AppResult<Project>
    suspend fun delete(projectId: String): AppResult<Unit>
    suspend fun get(projectId: String): Project?
}

/** The ONLY way other modules touch project files. Sandbox is enforced here. */
interface ProjectFileSystem {
    fun root(projectId: String): File
    fun resolve(projectId: String, relativePath: String): AppResult<File>   // rejects .., absolute escapes, symlink escapes
    suspend fun list(projectId: String, relativeDir: String, depth: Int = 1): AppResult<List<FileEntry>>
    suspend fun read(projectId: String, relativePath: String, maxBytes: Int = 262_144): AppResult<FileContent>
    suspend fun write(projectId: String, relativePath: String, content: String): AppResult<Unit>
    suspend fun delete(projectId: String, relativePath: String): AppResult<Unit>
    fun guestPath(projectId: String): String                                   // "/root/projects/<slug>"
}

interface GitService {
    suspend fun init(projectId: String): AppResult<Unit>
    suspend fun status(projectId: String): AppResult<GitStatus>
    suspend fun checkpoint(projectId: String, message: String): AppResult<GitCommit?>   // null if nothing to commit
    suspend fun log(projectId: String, limit: Int = 50): AppResult<List<GitCommit>>
    suspend fun diffStat(projectId: String, from: String? = null): AppResult<String>
    suspend fun restore(projectId: String, commit: String): AppResult<Unit>
    suspend fun setRemote(projectId: String, url: String?): AppResult<Unit>
    suspend fun push(projectId: String): AppResult<Unit>                       // P2
}

interface AskAgentAboutFile { fun ask(projectId: String, relativePath: String, lineRange: IntRange?) }   // implemented by M6/M5
```

`GitService` executes `git` through `ShellBackend` (proot, `cwd = guestPath`). Default identity: `Agent <agent@localhost>`; user can set name/email in Settings (M7).

## 4. `hello-java` template content

```
AndroidManifest.xml            (package, minSdk 24, targetSdk = installed platform, MainActivity launcher)
res/layout/activity_main.xml   (LinearLayout + TextView + Button)
res/values/strings.xml
src/<package path>/MainActivity.java
.gitignore                     (build/, *.apk, .agent/cache/)
.agent/project.json
```

No launcher icon asset is required (use the framework default icon in the manifest). Template variables: `${PACKAGE}`, `${APP_LABEL}`, `${MIN_SDK}`, `${TARGET_SDK}`.

## 5. File tree (minimum)

```
app/src/main/java/{ROOT_PKG}/feature/projects/
├─ domain/
│  ├─ model/ Project.kt · TemplateId.kt · FileEntry.kt · FileContent.kt · GitStatus.kt · GitCommit.kt · ImportSource.kt · ExportTarget.kt · ExportOptions.kt
│  ├─ repository/ ProjectRepository.kt · ProjectFileSystem.kt · GitService.kt · AskAgentAboutFile.kt
│  └─ usecase/ CreateProjectUseCase.kt · ImportProjectUseCase.kt · ExportProjectUseCase.kt · DeleteProjectUseCase.kt
│              ObserveProjectsUseCase.kt · CheckpointUseCase.kt · RestoreCheckpointUseCase.kt · EvaluateBackupReminderUseCase.kt
├─ data/
│  ├─ local/ ProjectEntity.kt · ProjectDao.kt · ProjectIndexReconciler.kt · ProjectMetadataFile.kt
│  ├─ fs/    DefaultProjectFileSystem.kt · PathSandbox.kt · ProjectSlugger.kt · ZipImporter.kt · ZipExporter.kt · ZipSlipGuard.kt
│  ├─ template/ TemplateRepository.kt · TemplateRenderer.kt
│  ├─ git/   ShellGitService.kt · GitOutputParser.kt · AskpassScriptWriter.kt
│  ├─ RoomProjectRepository.kt
│  └─ di/    ProjectsModule.kt
└─ presentation/
   ├─ list/    ProjectListRoute.kt · ProjectListScreen.kt · ProjectListViewModel.kt · components/ProjectCard.kt · BackupBanner.kt
   ├─ create/  CreateProjectSheet.kt · CreateProjectViewModel.kt · TemplatePicker.kt
   ├─ detail/  ProjectDetailRoute.kt · ProjectDetailScreen.kt · ProjectDetailViewModel.kt · GitHistorySheet.kt
   ├─ tree/    FileTreeScreen.kt · FileTreeViewModel.kt · components/FileTreeNode.kt
   └─ viewer/  FileViewerRoute.kt · FileViewerScreen.kt · FileViewerViewModel.kt · SyntaxHighlighter.kt · tokenizers/{Java,Xml,Json,Markdown,Shell}Tokenizer.kt
app/src/main/assets/templates/{empty,hello-java}/…
```

## 6. Edge cases

- Slug collision → suffix `-2`, `-3`.
- Zip with `../` entries or absolute paths → reject the whole import.
- Symlink inside a project pointing outside → `resolve` rejects it.
- Binary or huge file in viewer → show "binary/large file" placeholder with size.
- `git` missing (tool not installed) → disable git actions and link to Tool Manager.
- Concurrent writes (agent vs. UI) → writes go through `ProjectFileSystem` only; atomic write via temp file + rename.

## 7. Tests

- Unit: `PathSandbox` (`../`, absolute, symlink escape, unicode), `ZipSlipGuard`, `ProjectSlugger`, `TemplateRenderer`, `GitOutputParser`, backup-reminder rules.
- Instrumented: create → checkpoint → modify → restore round-trip using the real toolchain.

## 8. Acceptance criteria

- [ ] Creating `hello-java` produces the expected tree at the host path and the same files appear at the guest path in a terminal session.
- [ ] Path traversal attempts via `ProjectFileSystem.resolve` all fail (test matrix).
- [ ] Import of a malicious zip is rejected; a valid zip imports.
- [ ] Export excludes `build/` and `.git` by default and produces a valid zip.
- [ ] Checkpoint and restore work; restore creates a safety commit first.
- [ ] File viewer opens a 400 KB file smoothly and truncates a 5 MB file with a notice.
- [ ] Backup banner appears after the configured threshold.
