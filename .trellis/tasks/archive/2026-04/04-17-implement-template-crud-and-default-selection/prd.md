# Implement Template Crud And Default Selection

## Goal

把模板模块从“读取种子模板并预览”的展示能力，升级为真正可管理的模板系统。用户应能创建和更新 Anki / Markdown 模板，并在学习计划或导出流程中显式选择默认模板，使模板从静态演示资产变成可长期配置的业务对象。

## What I already know

- 当前后端 `TemplateController` 只支持：
  - `GET /api/templates/anki`
  - `GET /api/templates/md`
  - `POST /api/templates/anki/preview`
  - `POST /api/templates/md/preview`
- 当前前端 `TemplatePage` 只支持列表浏览和预览
- `study_plan` 已经存在 `ankiTemplateId`、`mdTemplateId` 字段，因此学习计划层面已经具备模板绑定基础
- 当前导出链路已有模板选择能力，但还缺少真正的模板维护入口
- 实体层 `AnkiTemplateEntity`、`MarkdownTemplateEntity` 已存在，尚无 create/update 领域方法

## Requirements

- 新增 Anki 模板创建接口：`POST /api/templates/anki`
- 新增 Anki 模板更新接口：`PUT /api/templates/anki/{id}`
- 新增 Markdown 模板创建接口：`POST /api/templates/md`
- 新增 Markdown 模板更新接口：`PUT /api/templates/md/{id}`
- 写接口应复用现有模板预览校验逻辑，避免保存非法模板
- 前端模板页需增加：
  - 新建模板入口
  - 编辑模板入口
  - 保存前预览或保存后快速预览能力
- 默认模板选择的第一阶段范围限定为：
  - 学习计划创建 / 编辑时明确选择模板
  - 不新增全局“系统默认模板”配置中心

## Acceptance Criteria

- [ ] 用户可以创建一条 Anki 模板
- [ ] 用户可以更新现有 Anki 模板
- [ ] 用户可以创建一条 Markdown 模板
- [ ] 用户可以更新现有 Markdown 模板
- [ ] 保存前或保存后，模板仍可通过现有预览接口正确预览
- [ ] 学习计划页面可以使用这些新建模板进行选择
- [ ] 非法模板内容会被后端清晰拒绝，而不是保存后在预览或导出阶段才失败

## Technical Approach

- 后端继续以 `TemplateService` 作为模板业务入口，补齐 create/update DTO 与保存逻辑
- 模板合法性校验优先复用 `SimpleTemplateEngine.validate(...)`
- 模板名称冲突策略优先采用清晰业务错误，不引入自动覆盖
- 前端在现有 `TemplatePage` 上增加编辑表单和预览动作，不新开独立路由
- 默认模板选择先依赖学习计划已有的 `ankiTemplateId` / `mdTemplateId`，不扩展系统级偏好表

## Decision (ADR-lite)

**Context**: 模板模块当前已经有实体、列表和预览，但没有写能力，导致它更像只读样例页。  
**Decision**: 先做模板 CRUD 中的 create/update，并把“默认模板选择”收敛为学习计划层的显式模板绑定，不扩展全局默认配置。  
**Consequences**: 该范围足以让模板成为可用业务对象，同时避免过早引入跨模块默认规则、配置优先级和迁移问题。

## Out of Scope

- 删除模板
- 系统级默认模板配置中心
- 模板共享、复制、版本管理
- 导出任务层面的复杂模板回退优先级

## Technical Notes

- 相关代码：
  - `backend/src/main/java/com/jp/vocab/template/controller/TemplateController.java`
  - `backend/src/main/java/com/jp/vocab/template/service/TemplateService.java`
  - `backend/src/main/java/com/jp/vocab/template/entity/AnkiTemplateEntity.java`
  - `backend/src/main/java/com/jp/vocab/template/entity/MarkdownTemplateEntity.java`
  - `frontend/src/features/templates/TemplatePage.tsx`
  - `frontend/src/features/templates/api.ts`
- 学习计划页面已存在模板选择字段，可直接复用，无需额外 schema migration
- 若模板写接口落地，需同步回写 `docs/api-specification.md` 中模板章节，避免继续停留在预设计状态

## Implementation Sync (2026-04-17)

Status: implemented

Completed in code:

- `POST /api/templates/anki`
- `PUT /api/templates/anki/{id}`
- `POST /api/templates/md`
- `PUT /api/templates/md/{id}`
- save-time template validation through `SimpleTemplateEngine.validate(...)`
- template page create / edit / draft preview flows
- study plan side template selectors automatically refresh against the updated template lists

Acceptance status:

- [x] create Anki template
- [x] update Anki template
- [x] create Markdown template
- [x] update Markdown template
- [x] save-time validation blocks unsupported variables and sections
- [x] study plan page can use newly saved templates after list refresh
- [x] API specification synced with current behavior
