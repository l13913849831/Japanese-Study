# Implement Word Entry Management And Filtering

## Goal

把当前“词库导入 + 词条浏览”的只读流程，升级为可长期维护的词条管理模块。用户应能新增、编辑、删除单个词条，并按关键词、等级、标签过滤词条列表，从而把系统从一次性导入工具推进到可持续维护的个人词库系统。

## What I already know

- 当前后端已有 `WordSetController`、`WordEntryService`、`WordEntryRepository`，但只覆盖词条列表和导入
- 当前前端 `WordSetPage` 仅支持词库选择、词条列表查看、CSV / APKG 导入
- API 文档已经预留了这些接口：
  - `POST /api/word-sets/{wordSetId}/words`
  - `PUT /api/words/{wordId}`
  - `DELETE /api/words/{wordId}`
- 当前实体已有这些字段：`expression`、`reading`、`meaning`、`partOfSpeech`、`exampleJp`、`exampleZh`、`level`、`tags`、`sourceOrder`
- 现有导入去重规则依赖 `(wordSetId, expression, reading)` 语义唯一性

## Requirements

- 新增单词条创建接口：`POST /api/word-sets/{wordSetId}/words`
- 新增单词条更新接口：`PUT /api/words/{wordId}`
- 新增单词条删除接口：`DELETE /api/words/{wordId}`
- 扩展词条列表查询，支持至少以下过滤条件：
  - `keyword`
  - `level`
  - `tag`
- 创建和更新时复用当前字段模型，不额外引入复杂字段
- 写接口需沿用当前重复规则，防止同一词库下重复词条进入系统
- 前端词库页面需增加：
  - 词条新增 / 编辑表单
  - 删除操作
  - 过滤栏
  - 提交成功后的列表刷新

## Acceptance Criteria

- [ ] 用户可以在指定词库下创建一个词条
- [ ] 用户可以修改现有词条内容并看到列表刷新
- [ ] 用户可以删除现有词条
- [ ] 用户可以按关键词过滤词条列表
- [ ] 用户可以按等级和标签过滤词条列表
- [ ] 创建或更新重复词条时，后端返回清晰业务错误
- [ ] 前端页面在无词库、无词条、过滤无结果、提交失败时都有清晰状态反馈

## Technical Approach

- 后端优先在 `wordset` 模块内补齐 DTO、controller 路由和 service 写操作
- 词条列表过滤优先走 repository 查询扩展，不先引入复杂搜索组件
- 更新和创建共享字段归一化逻辑，避免和导入逻辑各写一套
- 前端继续复用 `WordSetPage` 作为主入口，不先拆独立页面
- 过滤交互优先使用词库页顶部表单 + React Query 刷新，不增加全局状态

## Decision (ADR-lite)

**Context**: 词条长期维护能力当前明显缺失，但现有系统已经具备完整词条字段和导入链路，适合沿着现有模型补写能力。  
**Decision**: 先做单词条 CRUD + 轻量过滤，不同时引入批量编辑、批量删除、导入预览等更重交互。  
**Consequences**: 该方案能最快补齐真实维护能力，并保持与当前词库页结构兼容；高级导入治理和大规模编辑会留到后续任务。

## Out of Scope

- 批量编辑 / 批量删除
- 导入预览和导入冲突解决向导
- 全文检索或模糊搜索引擎
- 词条版本历史和审计记录

## Technical Notes

- 相关代码：
  - `backend/src/main/java/com/jp/vocab/wordset/service/WordEntryService.java`
  - `backend/src/main/java/com/jp/vocab/wordset/entity/WordEntryEntity.java`
  - `frontend/src/features/word-sets/WordSetPage.tsx`
  - `frontend/src/features/word-sets/api.ts`
- 当前后端还没有单独的 `WordEntryController`，实现时需要决定继续挂在 `WordSetController` 还是拆分新控制器
- 若引入过滤参数，需同步检查 `docs/api-specification.md` 和 frontend API wrapper 的 contract
