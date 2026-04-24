# implement: docs and regression foundation

## Goal

同步当前明显滞后的产品文档，并补上关键主链路的基础回归保护，降低后续连续迭代的认知偏差和回归风险。

## What I already know

* 多份 `docs/` 文档仍停留在 notes MVP 之前的认知。
* 后端当前只有少量测试，前端暂无测试文件。
* 如果继续扩核心学习链路，文档和测试的薄弱会持续放大风险。

## Requirements

* 更新过期文档，使其与当前代码状态一致。
* 为关键主链路补基础回归覆盖。
* 收紧当前功能契约，避免后续规划继续基于旧前提。

## Acceptance Criteria

* [x] 关键说明文档与当前代码状态一致。
* [x] 至少补到核心主路径的基础回归覆盖。
* [x] 后续规划任务不再把已完成能力误判为未完成。

## Out of Scope

* 不做大规模重构。
* 不追求一次性补齐所有测试。

## Technical Notes

* 重点关注：
  * `docs/system-usage-guide.md`
  * `docs/recommended-next-features.md`
  * `docs/api-specification.md`
  * `backend/src/test/**`

## Final Notes

* 三份 `docs/` 文档已改成当前真实产品状态，不再把 notes、dashboard、review session 等已完成能力写成待办。
* 后端补了单词复习、知识点导入、知识点评分三条服务层基础回归，优先兜住当前最容易回归的主路径。
* 这轮没有追求数据库集成测试，先保证不依赖 PostgreSQL 方言环境也能稳定跑基础单测。
