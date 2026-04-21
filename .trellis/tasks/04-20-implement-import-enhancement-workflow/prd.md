# Implement Import Enhancement Workflow

## Goal

把现有词条导入从“上传即导入”升级为“先预览、再确认导入”的流程，让用户在入库前就能看到字段映射、重复项和错误行。

## What I already know

- 当前 `/word-sets` 页面已经支持 `.csv` 和 `.apkg` 上传导入。
- 现有后端会在真正入库时做必填校验和去重，但前端拿到的只是最终结果。
- 现有导入流程缺少：
  - 预校验
  - 重复项预览
  - 更明确的字段映射说明
  - 更直观的错误呈现

## Requirements

- 新增导入预览接口而不直接入库。
- 支持 CSV 和 `.apkg` 两种导入源的预览。
- 预览结果至少包含：
  - source type
  - total / ready / duplicate / error 计数
  - 字段映射诊断
  - 行级状态预览
- 实际导入时只导入 `READY` 行。
- CSV 头部支持 alias 级字段匹配，并在预览中显式展示。
- 前端词库页把导入流程改为：
  - 上传
  - 预览
  - 确认导入

## Acceptance Criteria

- [ ] `/api/word-sets/{wordSetId}/import/preview` 可用
- [ ] CSV 预览能返回字段映射诊断
- [ ] `.apkg` 预览能返回推断字段映射和行级状态
- [ ] 重复项在预览阶段可见，而不是只有导入后才知道
- [ ] 前端可在预览弹窗中确认导入
- [ ] 实际导入后只写入 `READY` 行
- [ ] 导入结果和错误表仍保留在词库页中

## Technical Approach

- 后端在 `wordset` 模块内新增 preview DTO，不新建独立业务模块。
- CSV 和 `.apkg` 都统一收敛到同一套 `ParsedImportPayload` / `ImportAnalysis` 流程。
- 用字段映射支持类统一处理 canonical 字段和 alias 匹配。
- 前端继续沿用 `WordSetPage`，通过 modal 展示预览，而不是新增独立页面。

## Out of Scope

- 持久化导入会话历史
- 预览阶段的逐行编辑
- 自定义拖拽式字段映射
- 导入后的回滚工作流
