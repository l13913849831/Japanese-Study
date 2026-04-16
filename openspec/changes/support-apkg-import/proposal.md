## Why

当前系统只能通过 CSV 导入词条，而用户的现成学习资料很多直接来自 Anki 导出的 `.apkg` 牌组包。继续要求手工转 CSV 会增加一次格式转换和字段清洗成本，也会让现有导入流程无法承接最常见的资料来源。

## What Changes

- 在现有 `POST /api/word-sets/{wordSetId}/import` 导入接口上增加 `.apkg` 文件支持，保持 CSV 入口不变。
- 后端增加 `.apkg` 解包、读取 Anki SQLite 集合文件、提取 note 字段并映射为系统 `word_entry` 的能力。
- 导入流程继续复用当前去重、错误汇总和 `source_order` 生成规则，保证 CSV 与 `.apkg` 的行为一致。
- 前端词库页面上传入口改为同时接受 `.csv` 与 `.apkg`，并更新使用说明文档。

## Capabilities

### New Capabilities
- `anki-package-import`: 支持将 `.apkg` 牌组包中的 note 数据导入到现有词库，并给出逐条导入结果。

### Modified Capabilities
- None.

## Impact

- Affected code: `backend/src/main/java/com/jp/vocab/wordset/**`, `frontend/src/features/word-sets/**`, `docs/system-usage-guide.md`
- Affected APIs: `POST /api/word-sets/{wordSetId}/import`
- Dependencies: 后端需要新增 SQLite JDBC 依赖来读取 `.apkg` 内部集合文件
- Systems: 现有 PostgreSQL 数据模型不变，导入来源从 CSV 扩展到 CSV + `.apkg`
