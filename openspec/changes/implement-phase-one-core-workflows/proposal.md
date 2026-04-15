## Why

当前仓库已经具备第一阶段的前后端工程骨架和数据库基线，但核心业务流程仍停留在占位状态，导致系统只能展示结构，无法完成真实学习流程。现在补齐词条导入、计划创建、卡片生成、模板预览和导出链路，才能让第一阶段从“可启动”进入“可使用”。

## What Changes

- 完成词库词条导入与词条列表查询，支持基于 CSV 的批量入库和基础去重。
- 完成学习计划创建、编辑与状态切换，并校验词库、模板、复习间隔等关键字段。
- 在学习计划创建或重建时预生成卡片实例，并让今日卡片与日历查询返回真实结果。
- 补齐模板预览接口和前端模板预览入口，支持 Anki 与 Markdown 模板渲染验证。
- 补齐导出任务创建、查询与文件下载链路，覆盖 Anki CSV/TSV 与 Markdown。

## Capabilities

### New Capabilities
- `word-entry-import-and-browse`: 定义词条批量导入、去重、分页查询与前端词条浏览能力。
- `study-plan-lifecycle`: 定义学习计划创建、编辑、状态管理以及模板/词库绑定规则。
- `card-pre-generation`: 定义学习计划驱动的卡片预生成、今日卡片查询和日历统计能力。
- `template-preview`: 定义 Anki / Markdown 模板预览输入、变量替换和预览输出行为。
- `export-delivery`: 定义导出任务创建、结果下载和导出记录查询能力。

### Modified Capabilities
- None.

## Impact

- Affected code: `backend/src/main/java/com/jp/vocab/**`、`frontend/src/features/**`、必要的 Flyway 增量脚本与开发文档。
- Affected APIs: `/api/word-sets/**`、`/api/study-plans/**`、`/api/templates/**`、`/api/export-jobs/**`。
- Dependencies: PostgreSQL、Flyway、Spring Validation、React Query、Ant Design Upload/Form/Table。
- Systems: 本地 PostgreSQL 数据库、前后端联调流程、CSV/Markdown 文件生成与下载。
