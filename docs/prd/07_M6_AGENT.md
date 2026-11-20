# M6 — Agent Runtime

**Depends on:** M0, M1, M3, M4, M5 · **Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md
**Goal:** the autonomous loop: LLM ↔ tools, with permissions, sandbox, log trimming, live timeline, stop/resume. The agent writes code; the user never edits files in-app.

---

## 1. Runtime model

- `AgentRunner` executes **one Run** = user message → (LLM call → tool calls → results)* → final answer.
- A Run lives in `AgentRunService` (foreground service, own `CoroutineScope`). **Never in a ViewModel.**
- One active Run per conversation; global limit 1 in MVP.
- Tool calls within one assistant turn execute **sequentially** in MVP.

```
loop (step < maxSteps):
    stream LLM(messages, tools)               → persist assistant message (M5)
    if stopReason != TOOL_USE: finish
    for each tool call:
        decision = PermissionGateway.check(call)       // may suspend for the user
        output   = if allowed: tool.execute(args, ctx) else denied-result
        persist tool_result block (trimmed)
    append results → next step
```

**`AgentConfig` defaults:** `maxSteps = 40`, `maxConsecutiveToolErrors = 5`, `maxBuildRetries = 5`, `toolOutputMaxChars = 8000`, `runTokenBudget = 500_000` (configurable), per-tool timeouts (shell 120 s, build 600 s).

## 2. Agent tools

```kotlin
interface AgentTool {
    val spec: ToolSpec                                   // name, description, JSON schema
    val permission: PermissionLevel                      // default level
    suspend fun execute(args: JsonObject, ctx: ToolContext): ToolOutput
    fun describe(args: JsonObject): ToolSummary          // human title + risk for the approval UI
}
data class ToolContext(val projectId: String, val conversationId: String, val runId: String, val cancel: Job)
data class ToolOutput(val text: String, val isError: Boolean, val artifacts: List<ArtifactRef> = emptyList())
```

| Tool | Args | Default permission | Notes |
|---|---|---|---|
| `list_dir` | `path`, `depth?` | AUTO | via `ProjectFileSystem` |
| `read_file` | `path`, `start_line?`, `end_line?` | AUTO | ≤ 256 KB, returns numbered lines |
| `search_files` | `pattern`, `path?`, `glob?` | AUTO | `rg` if installed, else `grep -rn` |
| `write_file` | `path`, `content` | ASK_ONCE (scope: project) | creates parents; shows diff |
| `edit_file` | `path`, `old_str`, `new_str`, `replace_all?` | ASK_ONCE (project) | exact, unique match required |
| `delete_path` | `path` | ASK_ALWAYS | |
| `shell_exec` | `command`, `cwd?`, `timeout_sec?` | ASK_ALWAYS (+ allowlist, §3) | proot, project cwd |
| `build_apk` | `variant?` | AUTO | returns distilled summary + `build_id` |
| `read_build_log` | `build_id`, `offset`, `limit` | AUTO | paging beyond the distilled summary |
| `install_apk` | `build_id?` | ASK_ALWAYS | installs on the user's phone |
| `git` | `subcommand`, `args` | AUTO for status/diff/log/add/commit; ASK_ALWAYS for push/restore | allowlisted subcommands only |
| `ask_user` | `question`, `options?` | AUTO | renders a card, run waits for the answer |

*P2 (after MVP):* `task_plan(items)` for a visible todo list.

Tool output format (consistent for the model):
```
<exit_code>1</exit_code>
<output>…trimmed…</output>
[132 lines omitted — use read_build_log(build_id, offset, limit)]
```

## 3. Permissions & safety

- `PermissionLevel`: `AUTO | ASK_ONCE | ASK_ALWAYS | DENY`.
- **Run modes** (selector in chat): **Ask** (default) and **Auto-accept edits** (`write_file`/`edit_file` become AUTO for the project; shell/delete/install/push still ask). A "full auto" mode is *not* in MVP.
- `PermissionGateway.check(call)` returns `Allowed | Denied(reason)`; suspends while waiting for the user. Prompt shows: tool, human summary, risk level, details (diff for edits, full command for shell).
- User choices: **Allow once · Allow for this session · Allow for this project · Deny (optional reason, sent back to the model)**. "Project" grants persist in `PermissionGrantEntity(id, projectId, toolName, scopeKey, grantedAt)`; `scopeKey` is a command prefix for shell (e.g. `git status`).
- **Shell allowlist (AUTO without prompt):** read-only commands only (`ls`, `cat`, `pwd`, `which`, `java -version`, `git status|diff|log`).
- **`CommandRiskClassifier`:** `HIGH` for `rm -rf` outside `build/`, `curl|sh`, `sudo`, `chmod -R`, `dd`, `mkfs`, writes outside the project, fork bombs, `:(){`; `HIGH` always prompts even with grants; a small **deny list** (e.g. `rm -rf /`, `rm -rf ~`) is refused outright.
- **Sandbox:** file tools go only through `ProjectFileSystem` (M3). Shell runs in proot with `cwd` = project, no shared-storage bind, env scrubbed of secrets. Network is allowed (needed for downloads) with a Settings switch (M7). proot is defense-in-depth, not a security boundary; the approval prompt is the real control.
- **Loop guards:** same tool + same args 3× → inject a warning message; 5× → stop the run. `maxConsecutiveToolErrors` stops runaway failures.
- **Background approvals:** if the app is backgrounded while waiting, post a high-priority notification with **Allow once / Deny** actions; the run stays alive in the foreground service.

## 4. System prompt (`SystemPromptBuilder`)

Assembled from `res/raw/prompt_agent_system.md` (versioned) + dynamic sections:
1. **Role & style:** concise, act with tools, ask only when truly ambiguous, reply in the user's language.
2. **Environment facts** (from M2 states): Android phone, proot Ubuntu aarch64, installed tools + versions.
3. **Project facts:** name, template, package name, top-level file listing.
4. **Rules:** make small edits; after code changes run `build_apk`; never claim success without a successful build; read diagnostics before fixing; do not use Kotlin/Gradle/AppCompat/Maven libs (not supported yet) — use framework widgets and plain Java; never touch files outside the project; prefer `edit_file` over rewriting whole files.
5. **Tool usage guide** (short).

## 5. Context management (`ContextManager`)

- Estimate tokens with a chars/4 heuristic (provider counting is *Later*); context window per model from config (default if unknown: 100k).
- At > 70%: replace old tool results with one-line stubs (keep error lines), then drop the oldest tool-result pairs. LLM summarization is *Later*.
- `LogDistiller` (M4) is applied to every shell/build output before it enters the context; raw output stays on disk.

## 6. Timeline & UI

- `AgentEvent`: `StepStarted, AssistantText, ToolRequested, PermissionRequested, PermissionResolved, ToolStarted, ToolOutput(partial), ToolFinished, BuildStarted, BuildFinished, Error, Completed, Cancelled`.
- Chat (M5) renders persisted messages plus live events. `ToolCallCard` per tool: icon, title (e.g. "Edit MainActivity.java"), status chip, expandable args/result; **diff view** for `write_file`/`edit_file` (before/after unified diff, ≤ 200 lines then "show more").
- `ApprovalCard` inline in chat; `AskUserCard` for `ask_user`.
- **Stop** cancels the LLM stream and the running tool, marks the run `CANCELLED`.
- Run header: step counter, elapsed time, token usage, mode chip.
- **Resume:** `AgentRunEntity(id, conversationId, status, stepCount, startedAt, lastEventAt)`. After process death a `RUNNING` run becomes `INTERRUPTED`; the chat offers **Continue** (re-enters the loop with the persisted history).
- `FixWithAiLauncher` implementation: starts a run with a prefilled message containing the distilled diagnostics for the given `buildId`.
- Implements `AskAgentAboutFile` (M3): inserts a file reference into the composer.

## 7. Functional requirements

- M6-FR-01 End-to-end: "Create a Hello World app and build it" → project created/edited, build succeeds, APK offered for install.
- M6-FR-02 A failing build triggers read diagnostics → edit → rebuild, up to `maxBuildRetries`, then the agent stops and explains.
- M6-FR-03 Every write/exec tool call is gated by `PermissionGateway` according to the mode and grants.
- M6-FR-04 Path traversal or symlink escape attempts by tools fail with an error result the model can read.
- M6-FR-05 Tool output entering the context never exceeds `toolOutputMaxChars`.
- M6-FR-06 Stop works within 2 s, including during a long shell command.
- M6-FR-07 Foreground service keeps a run alive when the app is backgrounded; wake lock only while a step is active.
- M6-FR-08 Runs resume/continue after process death without duplicating tool executions (tool call ids are idempotency keys).
- M6-FR-09 Auto-checkpoint (git) after a successful build when enabled (M7 setting).
- M6-FR-10 Nothing secret (API keys) is ever included in prompts or tool environments.

## 8. File tree (minimum)

```
app/src/main/java/{ROOT_PKG}/feature/agent/
├─ domain/
│  ├─ model/ AgentConfig.kt · AgentEvent.kt · AgentRun.kt · RunStatus.kt · RunMode.kt · ToolOutput.kt · ToolContext.kt · ToolSummary.kt
│  │         PermissionLevel.kt · PermissionRequest.kt · PermissionDecision.kt · PermissionGrant.kt · RiskLevel.kt
│  ├─ AgentTool.kt · AgentRunner.kt · AgentLoop.kt · PermissionGateway.kt · CommandRiskClassifier.kt · ContextManager.kt · SystemPromptBuilder.kt
│  └─ usecase/ StartRunUseCase.kt · StopRunUseCase.kt · ContinueRunUseCase.kt · ResolvePermissionUseCase.kt · AnswerAskUserUseCase.kt
├─ data/
│  ├─ tools/ ToolRegistryImpl.kt · ListDirTool.kt · ReadFileTool.kt · SearchFilesTool.kt · WriteFileTool.kt · EditFileTool.kt · DeletePathTool.kt
│  │         ShellExecTool.kt · BuildApkTool.kt · ReadBuildLogTool.kt · InstallApkTool.kt · GitTool.kt · AskUserTool.kt
│  ├─ permission/ DefaultPermissionGateway.kt · ShellAllowlist.kt · DenyList.kt · PermissionNotificationFactory.kt
│  ├─ runtime/ DefaultAgentRunner.kt · DefaultAgentLoop.kt · LoopGuard.kt · RunStateStore.kt · DefaultContextManager.kt
│  ├─ prompt/ DefaultSystemPromptBuilder.kt · EnvironmentFacts.kt
│  ├─ local/ AgentRunEntity.kt · AgentRunDao.kt · PermissionGrantEntity.kt · PermissionGrantDao.kt
│  ├─ service/ AgentRunService.kt · AgentNotificationFactory.kt
│  ├─ integration/ FixWithAiLauncherImpl.kt · AskAgentAboutFileImpl.kt
│  └─ di/ AgentModule.kt
└─ presentation/
   ├─ cards/ ToolCallCard.kt · DiffView.kt · ApprovalCard.kt · AskUserCard.kt · BuildResultCard.kt · cards/ToolCardRegistrar.kt
   ├─ timeline/ RunHeader.kt · RunModeSelector.kt · StopButton.kt
   └─ AgentChatBridge.kt
app/src/main/res/raw/prompt_agent_system.md
```

## 9. Tests

- Unit: each tool's arg parsing + error cases, `CommandRiskClassifier` matrix, permission policy matrix (mode × tool × grant), `PathSandbox` via tools, `LoopGuard`, `ContextManager` trimming.
- Integration (JVM): `AgentLoop` with a **scripted `FakeLlmProvider`** and `FakeShellBackend`: success path, build-fail→fix→success, denied permission, cancel mid-tool, resume after "crash".
- Device: real provider + real toolchain end-to-end.

## 10. Acceptance criteria

- [ ] With a real provider key: "make a flashlight-style app, build it" produces an APK without manual file edits.
- [ ] A seeded compile error is fixed by the agent within `maxBuildRetries`; otherwise the agent stops with an explanation.
- [ ] Shell command prompts show the full command; `rm -rf /` is refused; `curl | sh` is HIGH risk and always prompts.
- [ ] Traversal tests (`../`, symlink) fail for every file tool.
- [ ] Stop cancels a running `sleep 100` within 2 s.
- [ ] App backgrounded during a run: run continues; approval notification works.
- [ ] Kill the process mid-run: chat shows Interrupted and Continue works without re-running finished tool calls.
- [ ] No tool output in context exceeds the cap; a raw 5 MB log stays on disk only.
