# Implement Study Plan Lifecycle

## Goal

将学习计划从“可随意编辑状态字段”的模型，收敛为明确的生命周期动作模型。用户创建计划后默认进入 `DRAFT`，通过显式的 `activate`、`pause`、`archive` 操作推进状态；前端不再允许直接编辑任意状态，后端负责校验合法流转并限制可编辑状态。

## Requirements

- 创建学习计划时默认状态为 `DRAFT`
- 更新学习计划时不再接受手工传入 `status`
- 仅允许 `DRAFT` 和 `PAUSED` 状态的计划执行常规编辑
- 新增生命周期动作接口：
  - `POST /api/study-plans/{id}/activate`
  - `POST /api/study-plans/{id}/pause`
  - `POST /api/study-plans/{id}/archive`
- 生命周期动作需校验合法迁移关系，并返回最新计划详情
- 前端学习计划页面改为：
  - 表单只编辑计划内容，不编辑状态
  - 选中计划后展示可执行动作
  - 动作执行后刷新列表与表单

## Acceptance Criteria

- [ ] 新建学习计划时，即使前端未传 `status`，返回结果仍为 `DRAFT`
- [ ] 对 `ACTIVE` 或 `ARCHIVED` 计划调用常规更新接口时，后端返回业务错误
- [ ] `DRAFT` 或 `PAUSED` 计划可以成功 `activate`
- [ ] `ACTIVE` 计划可以成功 `pause`
- [ ] 非归档计划可以成功 `archive`，已归档计划重复归档会被拒绝
- [ ] 前端页面不再出现状态下拉框，改为显式动作按钮
- [ ] 生命周期动作执行成功后，列表中的状态与当前表单状态同步刷新

## Technical Notes

- 复用现有 `StudyPlanResponse`，不新增专门的生命周期响应 DTO
- 状态规则优先收敛到 `StudyPlanEntity` 或 `StudyPlanService`，避免散落在 controller/frontend
- 更新接口仍需在成功后触发 `cardGenerationService.regenerateForPlan(...)`
- 生命周期动作本身不重建卡片，只更新计划状态
- 错误路径沿用 `BusinessException` + `ErrorCode`

## Out of Scope

- 学习计划日历接口
- 计划详情聚合更多统计字段
- 基于状态的复杂权限系统
- 自动归档、自动暂停等定时任务
