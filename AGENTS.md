<!-- TRELLIS:START -->
# Trellis Instructions

These instructions are for AI assistants working in this project.

Use the `/trellis:start` command when starting a new session to:
- Initialize your developer identity
- Understand current project context
- Read relevant guidelines

Use `@/.trellis/` to learn:
- Development workflow (`workflow.md`)
- Project structure guidelines (`spec/`)
- Developer workspace (`workspace/`)

If you're using Codex, project-scoped helpers may also live in:
- `.agents/skills/` for reusable Trellis skills
- `.codex/agents/` for optional custom subagents

Keep this managed block so 'trellis update' can refresh the instructions.

<!-- TRELLIS:END -->

## 项目级沟通规则

- 后续所有与用户的对话、进度同步、问题确认、结果说明，默认全部使用中文。
- 只有当用户明确要求使用其他语言时，才切换对应语言回复。

## 本地工具路径规则

- Python 统一使用 `C:\Users\luyh\AppData\Local\Python\bin\python.exe`。
- Maven 统一使用 `D:\apache-maven-3.9.12\bin\mvn.cmd`。
- 不使用 PATH 中的 Microsoft Store Python 占位符。

## 双前端覆盖规则

- 本项目同时维护纯 Web 前端 `frontend/` 和小程序前端 `miniapp/`。
- 任何学习、复习、工作台、弱项、账号、导出等用户可见行为变更，都必须同时评估 Web 和小程序影响面。
- 如果两端都存在对应入口，默认两端都要同步实现、同步类型契约、同步文档说明，并分别执行对应校验。
- Web 校验优先使用 `frontend` 下的 `npm run build`。
- 小程序校验优先使用 `miniapp` 下的 `npm run typecheck` 和 `npm run build:weapp`。
- 如果某次改动只覆盖其中一端，必须在任务 PRD、结果说明或文档中明确写出原因和后续补齐项。
