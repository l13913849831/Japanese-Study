## Context

当前词条导入由 `WordEntryService#importCsv()` 直接读取上传文件内容，经过 CSV 解析、字段校验、去重和持久化后写入 `word_entry`。这条链路已经覆盖前端上传、错误汇总和后续学习计划依赖，因此新增 `.apkg` 支持时应尽量复用现有持久化规则，而不是再做一套独立导入模型。

`.apkg` 本质上是一个 zip 包，内部包含 Anki 集合数据库文件和媒体索引。系统这次只需要把 note 字段映射成词条，不需要迁移复习记录、牌组层级、模板样式或媒体文件本体。

## Goals / Non-Goals

**Goals:**

- 在同一个词条导入接口上支持 `.csv` 与 `.apkg`
- 能从常见 `.apkg` 包中的 `collection.anki21` 或 `collection.anki2` 读取 note 数据
- 基于 note type 字段名做字段映射，生成 `expression`、`reading`、`meaning` 及可选扩展字段
- 继续沿用现有重复键判定、`source_order` 递增和错误汇总返回结构
- 前端上传控件和使用文档同步更新

**Non-Goals:**

- 不导入 Anki 卡片调度、复习历史、牌组层级或模板 CSS
- 不保存媒体文件到系统，也不做资源路径重写
- 不新增独立 `.apkg` 导入接口或异步任务
- 不覆盖 `.colpkg`、Anki 全量集合导入等更大范围兼容能力

## Decisions

### 1. 保持单一导入接口，按文件扩展名分流

`WordSetController` 继续保留 `POST /api/word-sets/{wordSetId}/import`，服务层新增统一 `importEntries()` 方法，根据文件名后缀在 CSV 和 `.apkg` 之间切换。

- Why: 前端入口、权限边界和调用方都已围绕现有接口建立，扩展同一路径成本最低。
- Alternative considered: 新增 `/import-apkg` 专用接口，但这会复制前端调用和服务层校验逻辑。

### 2. 使用 zip 解包 + SQLite JDBC 直接读取包内集合数据库

后端新增一个 `.apkg` 解析服务，把压缩包中的 `collection.anki21` 或 `collection.anki2` 写入临时文件，再通过 SQLite JDBC 查询 `col` 和 `notes` 表。

- Why: `.apkg` 的核心结构稳定，直接读取集合库最直接，也能拿到 note type 的字段定义。
- Alternative considered: 仅按二进制或文本规则猜测字段，不可靠，且无法区分不同 note type 的字段顺序。

### 3. 基于 note type 字段名做启发式映射，并对内容做轻量清洗

解析 `col.models` 中的字段定义后，对字段名做归一化匹配：
- `expression`: expression, word, vocab, term, japanese, front
- `reading`: reading, kana, yomi, pronunciation, furigana
- `meaning`: meaning, definition, gloss, translation, back, english, chinese
- 其余字段按 `partOfSpeech`、`exampleJp`、`exampleZh`、`level`、`tags` 继续尝试映射

字段值会移除 HTML 标签、Anki 声音标记和多余空白，再进入既有词条持久化逻辑。

- Why: 不同牌组的字段命名并不统一，必须有一层可解释的兼容规则。
- Alternative considered: 只支持固定字段名，但这会导致绝大多数社区牌组无法直接导入。

### 4. `.apkg` 导入继续复用现有去重与错误返回模型

`.apkg` 提取出的 note 先转换成内部导入记录，再走与 CSV 相同的校验与保存流程。错误仍返回 `lineNumber`、`field`、`message`，其中 `lineNumber` 表示导入记录顺序。

- Why: 这样不会引入新的 API 响应模型，前端也无需改动结果展示结构。
- Alternative considered: 为 `.apkg` 单独定义 noteId 级别错误结构，但会让前后端出现两套导入结果格式。

## Risks / Trade-offs

- [部分牌组字段命名非常随意] -> 通过多组同义词匹配和首个非空字段兜底减少失败，并在无法识别 `expression`/`meaning` 时返回明确错误
- [包内集合文件格式可能因 Anki 版本不同而变化] -> 先支持 `collection.anki21` 和 `collection.anki2`，找不到或无法读取时返回清晰的 `IMPORT_ERROR`
- [字段内容常带 HTML 或媒体标记] -> 只做轻量文本清洗，避免把复杂富文本语义错误地写入词条字段
- [新增 SQLite 依赖会影响后端构建] -> 依赖范围只限后端运行时读取 `.apkg`，不修改数据库模型或 Flyway

## Migration Plan

1. 增加 `.apkg` 解析服务和 Maven 依赖，并把现有 CSV 导入逻辑整理成统一持久化入口
2. 改造控制器与前端上传入口，允许上传 `.apkg`
3. 更新使用文档，明确支持范围和不支持项
4. 若上线后发现兼容性问题，可回退到仅允许 CSV 的导入分支，数据库无需额外回滚

## Open Questions

- 是否还需要把 Anki deck 名称或 note type 名称记录到后续扩展字段中；本次先不落库
- 是否需要支持更激进的字段推断规则；本次先以字段名启发式为主，避免错误映射
