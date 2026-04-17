# 日语词汇系统：后续推荐功能

## 1. 文档目的

本文档用于沉淀当前 phase-one 日语词汇系统的后续推荐功能，供后续迭代直接使用。

适用场景：

- 路线图讨论
- OpenSpec 变更提案准备
- 任务拆分
- 迭代优先级排序

本文档基于 `2026-04-17` 时点的代码库状态整理。

---

## 2. 当前项目的实际用途

目前这个项目更准确的定位是：一个面向日语词汇学习的第一阶段学习准备系统。

它已经支持这些核心流程：

- 创建并浏览词集
- 从 CSV 和 `.apkg` 导入词条
- 基于 `reviewOffsets` 创建和更新学习计划
- 查询今日卡片和日历汇总
- 预览 Anki 与 Markdown 模板
- 生成导出任务并下载导出文件

从实际使用价值看，它现在适合：

- 建立个人日语词库
- 生成计划化学习节奏
- 查看某天应该学习哪些词
- 将学习内容导出到 Anki 或 Markdown

它目前还不是一个完整闭环的学习产品，因为真正的复习执行与学习进度记录仍然不完整。

---

## 3. 已实现能力与未落地能力

### 3.1 代码里已经实现的能力

- 词集创建、列表、词条浏览、词条导入
- 学习计划创建、列表、详情查询、更新
- 今日卡片查询与学习日历查询
- Anki / Markdown 模板列表与预览
- 导出任务创建、列表与下载

代表性代码：

- `backend/src/main/java/com/jp/vocab/wordset/controller/WordSetController.java`
- `backend/src/main/java/com/jp/vocab/studyplan/controller/StudyPlanController.java`
- `backend/src/main/java/com/jp/vocab/card/controller/CardQueryController.java`
- `backend/src/main/java/com/jp/vocab/template/controller/TemplateController.java`
- `backend/src/main/java/com/jp/vocab/exportjob/controller/ExportJobController.java`

### 3.2 文档已设计但还没完整落地的能力

- 卡片复习提交与复习记录查询
- 学习计划 activate / pause / archive 生命周期动作
- 词条编辑 / 删除 / 高级过滤
- 模板创建 / 编辑 / 默认模板管理
- 学习统计与仪表盘视图
- 更完整的导入校验与去重处理流程

相关设计参考：

- `docs/api-specification.md`
- `docs/mvp-implementation-plan.md`
- `docs/japanese-vocab-system-proposal.md`

---

## 4. 优先级概览

### P0：当前最值得优先做的功能

1. 复习闭环

原因：

- 这个功能会把项目从“学习计划与导出工具”升级成“真正可执行学习的系统”

### P1：核心产品补全类功能

1. 学习计划生命周期管理
2. 词条管理与筛选
3. 模板 CRUD 与默认模板选择

### P2：产品成熟度提升类功能

1. 学习统计与仪表盘
2. 导入质量增强

---

## 5. 推荐功能说明

## 5.1 复习闭环

### 为什么要做

系统已经能生成每日卡片，但用户还不能在系统内真正完成一次复习。

如果没有这个功能，系统只能“准备学习任务”，不能“记录学习结果”。

### 建议范围

- 增加卡片复习提交
- 增加复习记录查询
- 复习完成后更新卡片状态
- 支持 `AGAIN`、`HARD`、`GOOD`、`EASY` 这类评分
- 在卡片页面展示最近一次复习结果

### 后端涉及点

- 在 `card` 模块下增加 review 相关接口
- 以 `review_log` 作为写入模型
- 成功提交复习后更新 `card_instance.status`

候选 API：

- `POST /api/cards/{cardId}/review`
- `GET /api/cards/{cardId}/reviews`

### 前端涉及点

- 在卡片页面增加复习操作按钮
- 展示最近一次结果和可选的历史记录
- 复习提交后刷新今日卡片状态

### 验收标准

- 用户可以为一张卡片提交一次复习结果
- 系统会写入一条 `review_log`
- 卡片状态会在复习后保持一致性更新
- 用户可以查询单张卡片的复习历史

---

## 5.2 学习计划生命周期管理

### 为什么要做

学习计划虽然已经有 `status` 字段，但当前实际 API 和页面还没有清晰表达计划的生命周期。

这会导致系统对计划是否生效、是否暂停、是否归档的控制不够明确。

### 建议范围

- 增加 `activate`、`pause`、`archive` 操作
- 定义不同状态下允许的编辑限制
- 让卡片 / 日历行为与计划状态联动

### 后端涉及点

- 在学习计划控制器上增加状态动作接口
- 校验合法的状态流转
- 在必要处限制编辑和生成行为

候选 API：

- `POST /api/study-plans/{id}/activate`
- `POST /api/study-plans/{id}/pause`
- `POST /api/study-plans/{id}/archive`

### 前端涉及点

- 在学习计划页面增加状态操作按钮
- 更明确展示计划状态
- 在 UI 层防止无效操作

### 验收标准

- 用户可以激活、暂停、归档一个学习计划
- 非法状态流转会被清晰拒绝
- 激活 / 暂停 / 归档状态在 UI 中可见

---

## 5.3 词条管理与筛选

### 为什么要做

当前词集流程在“导入”和“浏览”上已经比较强，但长期维护能力偏弱。

真实使用里，用户一定会需要修正词条、删除坏数据，并按关键词、等级或标签查找内容。

### 建议范围

- 增加单词条创建 / 编辑 / 删除
- 增加关键词、等级、标签过滤
- 改善重复项处理与冲突提示

### 后端涉及点

- 增加词条写接口
- 扩展词条列表查询条件
- 保持重复校验和当前 `(wordSetId, expression, reading)` 唯一规则一致

候选 API：

- `POST /api/word-sets/{wordSetId}/words`
- `PUT /api/words/{wordId}`
- `DELETE /api/words/{wordId}`

### 前端涉及点

- 增加词条编辑表单或抽屉
- 增加删除操作
- 在词条表格上方增加过滤栏

### 验收标准

- 用户可以编辑和删除单个词条
- 用户可以按关键词、等级、标签过滤词条
- 重复项错误提示清晰可理解

---

## 5.4 模板 CRUD 与默认模板选择

### 为什么要做

模板预览已经存在，但系统整体还更像“预置模板演示”，还不是一个真正完整的模板管理模块。

用户应该能够直接拥有并管理自己的导出模板。

### 建议范围

- 增加 Anki 模板创建与更新
- 增加 Markdown 模板创建与更新
- 允许为学习计划或导出流程选择默认模板
- 编辑过程中保留预览能力

### 后端涉及点

- 为两类模板增加写接口
- 定义唯一性和校验规则
- 视需要支持“默认模板”查找规则

候选 API：

- `POST /api/templates/anki`
- `PUT /api/templates/anki/{id}`
- `POST /api/templates/md`
- `PUT /api/templates/md/{id}`

### 前端涉及点

- 增加模板编辑表单
- 支持保存前预览
- 在学习计划 / 导出流程里选择默认模板

### 验收标准

- 用户可以创建和更新两类模板
- 编辑后的模板仍然可以正常预览
- 导出流程可以使用选择的模板或默认模板

---

## 5.5 学习统计与仪表盘

### 为什么要做

一个学习系统真正变得有黏性，往往依赖可见的进度反馈。

当前产品已经具备计划和导出价值，但缺少足够强的激励与反馈回路。

### 建议范围

- 今日完成数量
- 最近复习趋势
- 每个计划的完成进度
- 连续学习天数
- 新卡 / 复习卡量对比

### 后端涉及点

- 从 `card_instance` 与 `review_log` 聚合统计数据
- 增加轻量统计接口

### 前端涉及点

- 增加仪表盘卡片或独立 dashboard 页面
- 在 cards / study-plans 页面展示摘要信息

### 验收标准

- 用户至少可以看到一类每日进度摘要
- 用户可以看到计划维度的进度
- 指标来自真实复习数据而不是占位值

---

## 5.6 导入质量增强

### 为什么要做

导入已经是这个项目当前最强的一块能力之一，但随着使用增多，导入数据质量会成为真正的产品问题。

尤其是 `.apkg`，源数据可能会很脏。

### 建议范围

- 正式写入前先做导入预览
- 增加重复项处理策略选项
- 提供字段映射辅助
- 提供更清晰的错误详情和失败项修正流程

### 后端涉及点

- 为导入增加 preview / validate 模式
- 支持显式重复策略
- 改善导入错误响应结构

### 前端涉及点

- 正式提交前展示预览表格
- 增加重复处理选项
- 丰富导入结果面板

### 验收标准

- 用户可以在保存前预览导入行
- 用户可以选择重复处理策略
- 失败项更容易定位和修正

---

## 6. 建议实施顺序

建议的落地顺序：

1. 复习闭环
2. 学习计划生命周期管理
3. 词条管理与筛选
4. 模板 CRUD 与默认模板选择
5. 学习统计与仪表盘
6. 导入质量增强

原因：

- 第 1 到 3 步补全的是核心学习闭环
- 第 4 步强化的是导出与模板可用性
- 第 5 到 6 步提升的是产品成熟度和长期使用体验

---

## 7. 当前最推荐的下一项任务

如果下一轮只做一个功能，优先选择：

`复习闭环`

原因：

- 它补上了当前系统最大的功能缺口
- 它会直接把产品从“学习准备”升级成“学习执行”
- 它会为后续统计、连击天数、进度追踪等能力解锁基础数据

详细任务清单见：

- `docs/review-loop-task-list.md`

---

## 8. 后续使用方式

后续准备下一轮迭代时，可以按下面的方式使用本文档：

1. 从本文档选择一个功能项
2. 将该功能转换成 OpenSpec proposal
3. 定义清晰的 API 合同与 UI 流程
4. 联动实现前后端
5. 功能落地后回写并更新本文档
## 9. 2026-04-17 Sync

The following items from the previous recommended list are now implemented:

- review loop close-the-loop APIs and task artifacts
- study plan lifecycle: `activate`, `pause`, `archive`
- word entry create, update, delete, and list filtering
- template create/update flows and plan-side template selection refresh

After removing those completed items, the remaining recommended priorities are:

### P1 Remaining

1. Study dashboard and aggregate visibility

Scope:

- daily / weekly study overview
- plan-level progress summary
- new vs review card trend visibility

Why it is next:

- the core CRUD and lifecycle paths are now usable
- the biggest product gap is visibility, not data entry

2. Import enhancement workflow

Scope:

- pre-import validation and duplicate preview
- clearer field mapping diagnostics
- better import error surfacing in UI

Why it is next:

- import quality still determines the quality of downstream study cards
- this reduces rework after bad CSV or `.apkg` imports

### P2 Remaining

1. Export workflow enhancement

Scope:

- explicit template selection in export creation UI
- export preview / validation hints
- clearer failed export diagnostics

2. Metrics and long-range planning views

Scope:

- streaks, completion stats, and stage distribution
- longer horizon plan forecasting
