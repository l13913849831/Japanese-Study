# 日语单词记忆系统接口定义文档

## 1. 文档目标

本文件定义第一阶段前后端接口契约，作为以下内容的统一依据：

- 后端 Controller 设计
- 前端 API 调用封装
- DTO 与 VO 定义
- 联调与测试基准

接口风格采用 REST 风格，返回 JSON。下载接口返回文件流。

## 2. 基础约定

## 2.1 Base URL

默认基础路径：

```text
/api
```

## 2.2 数据格式

- 请求：`application/json`
- 文件上传：`multipart/form-data`
- 下载：`text/csv`、`text/tab-separated-values`、`text/markdown` 或 `application/octet-stream`

## 2.3 时间格式

- 日期：`yyyy-MM-dd`
- 日期时间：ISO 8601，例如 `2026-04-13T10:15:30+09:00`

## 2.4 分页约定

第一阶段列表接口默认可以不强制分页，但建议保留分页参数：

- `page`：从 `1` 开始
- `pageSize`：默认 `20`

分页返回结构：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

## 2.5 统一返回结构

成功响应：

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

失败响应：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "dailyNewCount must be greater than 0",
    "details": [
      {
        "field": "dailyNewCount",
        "message": "must be greater than 0"
      }
    ]
  },
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 2.6 通用错误码

- `VALIDATION_ERROR`
- `NOT_FOUND`
- `CONFLICT`
- `BAD_REQUEST`
- `IMPORT_ERROR`
- `TEMPLATE_RENDER_ERROR`
- `EXPORT_ERROR`
- `INTERNAL_ERROR`

## 3. 数据对象定义

## 3.1 WordSet

```json
{
  "id": 1,
  "name": "N4 核心词汇",
  "description": "N4 高频词",
  "createdAt": "2026-04-13T10:15:30+09:00",
  "updatedAt": "2026-04-13T10:15:30+09:00"
}
```

## 3.2 WordEntry

```json
{
  "id": 1001,
  "wordSetId": 1,
  "expression": "準備",
  "reading": "じゅんび",
  "meaning": "准备",
  "partOfSpeech": "名词/サ变",
  "exampleJp": "明日の会議を準備する。",
  "exampleZh": "为明天的会议做准备。",
  "level": "N4",
  "tags": ["N4", "工作"],
  "sourceOrder": 1,
  "createdAt": "2026-04-13T10:15:30+09:00",
  "updatedAt": "2026-04-13T10:15:30+09:00"
}
```

## 3.3 StudyPlan

```json
{
  "id": 1,
  "name": "N4 每日 20 词",
  "wordSetId": 1,
  "startDate": "2026-04-20",
  "dailyNewCount": 20,
  "reviewOffsets": [0, 1, 3, 7, 14, 30],
  "ankiTemplateId": 1,
  "mdTemplateId": 1,
  "status": "ACTIVE",
  "createdAt": "2026-04-13T10:15:30+09:00",
  "updatedAt": "2026-04-13T10:15:30+09:00"
}
```

## 3.4 CardInstance

```json
{
  "id": 5001,
  "planId": 1,
  "wordEntryId": 1001,
  "cardType": "NEW",
  "sequenceNo": 1,
  "stageNo": 0,
  "dueDate": "2026-04-20",
  "status": "PENDING",
  "expression": "準備",
  "reading": "じゅんび",
  "meaning": "准备",
  "exampleJp": "明日の会議を準備する。",
  "exampleZh": "为明天的会议做准备。"
}
```

## 3.5 ReviewLog

```json
{
  "id": 9001,
  "cardInstanceId": 5001,
  "reviewedAt": "2026-04-20T19:30:00+09:00",
  "rating": "GOOD",
  "responseTimeMs": 3200,
  "note": "能认出但造句不稳",
  "createdAt": "2026-04-20T19:30:01+09:00"
}
```

## 3.6 AnkiTemplate

```json
{
  "id": 1,
  "name": "Default Japanese Anki",
  "description": "Default template",
  "fieldMapping": {
    "front": ["expression"],
    "back": ["reading", "meaning", "exampleJp", "exampleZh"]
  },
  "frontTemplate": "{{expression}}",
  "backTemplate": "<div>{{reading}}</div><div>{{meaning}}</div>",
  "cssTemplate": ".card { font-size: 20px; }",
  "createdAt": "2026-04-13T10:15:30+09:00",
  "updatedAt": "2026-04-13T10:15:30+09:00"
}
```

## 3.7 MarkdownTemplate

```json
{
  "id": 1,
  "name": "Default Daily Markdown",
  "description": "Default markdown export template",
  "templateContent": "# {{date}} 今日单词",
  "createdAt": "2026-04-13T10:15:30+09:00",
  "updatedAt": "2026-04-13T10:15:30+09:00"
}
```

## 3.8 ExportJob

```json
{
  "id": 10,
  "planId": 1,
  "exportType": "MARKDOWN",
  "targetDate": "2026-04-20",
  "fileName": "n4-2026-04-20.md",
  "filePath": "/exports/n4-2026-04-20.md",
  "status": "SUCCESS",
  "createdAt": "2026-04-13T10:15:30+09:00",
  "updatedAt": "2026-04-13T10:15:31+09:00"
}
```

## 4. 词库接口

## 4.1 创建词库

### `POST /api/word-sets`

请求：

```json
{
  "name": "N4 核心词汇",
  "description": "N4 高频词"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "N4 核心词汇",
    "description": "N4 高频词"
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

错误：

- `CONFLICT`：词库名称重复
- `VALIDATION_ERROR`：名称为空

## 4.2 查询词库列表

### `GET /api/word-sets?page=1&pageSize=20`

响应：

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "name": "N4 核心词汇",
        "description": "N4 高频词",
        "createdAt": "2026-04-13T10:15:30+09:00",
        "updatedAt": "2026-04-13T10:15:30+09:00"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 4.3 查询词库详情

### `GET /api/word-sets/{wordSetId}`

路径参数：

- `wordSetId`: `long`

## 4.4 更新词库

### `PUT /api/word-sets/{wordSetId}`

请求：

```json
{
  "name": "N4 核心词汇（修订）",
  "description": "更新后的说明"
}
```

## 4.5 删除词库

### `DELETE /api/word-sets/{wordSetId}`

响应：

```json
{
  "success": true,
  "data": {
    "deleted": true
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

说明：

- 删除词库将级联删除词条
- 如果已有学习计划绑定该词库，建议后端返回 `CONFLICT`

## 4.6 查询词条列表

### `GET /api/word-sets/{wordSetId}/words`

查询参数：

- `page`：默认 `1`
- `pageSize`：默认 `20`
- `keyword`：按词条、读音、释义、词性、例句、标签做关键字匹配
- `level`：按等级精确过滤，例如 `N4`
- `tag`：按标签精确过滤，例如 `工作`

响应中的 `items` 为 `WordEntry[]`

说明：

- 当前按 `sourceOrder` 升序返回
- 过滤条件可组合使用
- `wordSetId` 不存在时返回 `NOT_FOUND`

## 4.7 创建词条

### `POST /api/word-sets/{wordSetId}/words`

请求：

```json
{
  "expression": "安心",
  "reading": "あんしん",
  "meaning": "安心，放心",
  "partOfSpeech": "名词/サ变",
  "exampleJp": "それを聞いて安心した。",
  "exampleZh": "听到那个后我放心了。",
  "level": "N4",
  "tags": ["N4", "情感"]
}
```

说明：

- `expression` 和 `meaning` 必填
- `reading`、`partOfSpeech`、`exampleJp`、`exampleZh`、`level` 可为空
- `tags` 为字符串数组，允许为空数组
- 同一词库下若 `(expression, reading)` 语义重复，返回 `CONFLICT`
- 创建成功后系统会自动分配新的 `sourceOrder`

## 4.8 更新词条

### `PUT /api/words/{wordId}`

请求体同创建词条。

说明：

- 更新不会修改词条所属词库
- 若更新后与同词库内其他词条形成重复，返回 `CONFLICT`
- `wordId` 不存在时返回 `NOT_FOUND`

## 4.9 删除词条

### `DELETE /api/words/{wordId}`

响应：

```json
{
  "success": true,
  "data": {
    "deleted": true
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

说明：

- 删除词条会级联删除关联的 `card_instance` 与 `review_log`
- `wordId` 不存在时返回 `NOT_FOUND`

## 4.10 导入词条

### `POST /api/word-sets/{wordSetId}/import`

请求类型：

```text
multipart/form-data
```

表单字段：

- `file`: CSV 或 `.apkg` 文件
- `overwriteDuplicates`: `true/false`，可选，默认 `false`

上传 CSV 时标题建议：

```text
expression,reading,meaning,partOfSpeech,exampleJp,exampleZh,level,tags
```

`.apkg` 导入会从包内 note 字段推断 `expression`、`reading`、`meaning` 等词条字段；当前不导入复习历史、牌组层级和媒体文件。

响应：

```json
{
  "success": true,
  "data": {
    "importedCount": 80,
    "skippedCount": 5,
    "failedCount": 2,
    "errors": [
      {
        "line": 14,
        "message": "meaning is required"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 5. 学习计划接口

## 5.1 创建学习计划
### `POST /api/study-plans`

请求体：
```json
{
  "name": "N4 每日 20 词",
  "wordSetId": 1,
  "startDate": "2026-04-20",
  "dailyNewCount": 20,
  "reviewOffsets": [0, 1, 3, 7, 14, 30],
  "ankiTemplateId": 1,
  "mdTemplateId": 1
}
```

说明：
- 创建接口不接受 `status` 字段
- 服务端创建后默认将学习计划状态设置为 `DRAFT`
- 创建成功后会为该计划生成初始卡片排程

响应示例：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "N4 每日 20 词",
    "wordSetId": 1,
    "startDate": "2026-04-20",
    "dailyNewCount": 20,
    "reviewOffsets": [0, 1, 3, 7, 14, 30],
    "ankiTemplateId": 1,
    "mdTemplateId": 1,
    "status": "DRAFT",
    "createdAt": "2026-04-13T10:15:30+09:00",
    "updatedAt": "2026-04-13T10:15:30+09:00"
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

校验规则：
- `wordSetId` 必须存在
- `dailyNewCount > 0`
- `reviewOffsets` 不能为空
- `reviewOffsets` 第一个值必须为 `0`
- `reviewOffsets` 必须按升序排列

## 5.2 查询学习计划列表

### `GET /api/study-plans?page=1&pageSize=20`

说明：
- 当前实现返回全部学习计划
- 响应中的 `items` 为 `StudyPlan[]`

## 5.3 查询学习计划详情
### `GET /api/study-plans/{planId}`

成功响应示例：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "N4 每日 20 词",
    "wordSetId": 1,
    "startDate": "2026-04-20",
    "dailyNewCount": 20,
    "reviewOffsets": [0, 1, 3, 7, 14, 30],
    "ankiTemplateId": 1,
    "mdTemplateId": 1,
    "status": "ACTIVE",
    "createdAt": "2026-04-13T10:15:30+09:00",
    "updatedAt": "2026-04-13T10:15:30+09:00"
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 5.4 更新学习计划
### `PUT /api/study-plans/{planId}`

请求体：
```json
{
  "name": "N4 每日 25 词",
  "wordSetId": 1,
  "startDate": "2026-04-22",
  "dailyNewCount": 25,
  "reviewOffsets": [0, 1, 3, 7, 14, 30],
  "ankiTemplateId": 1,
  "mdTemplateId": 1
}
```

说明：
- 更新接口不接受 `status` 字段
- 仅 `DRAFT` 和 `PAUSED` 状态的计划允许更新
- 更新成功后会重新生成该计划下的卡片排程

错误场景：
- 当计划处于 `ACTIVE` 或 `ARCHIVED` 状态时，返回 `CONFLICT`

## 5.5 激活学习计划
### `POST /api/study-plans/{planId}/activate`

说明：
- 显式生命周期动作，将计划状态变更为 `ACTIVE`
- 仅允许 `DRAFT` 或 `PAUSED` 状态的计划执行该操作
- 该操作只更新计划状态，不重新生成卡片排程

响应示例：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "N4 每日 20 词",
    "wordSetId": 1,
    "startDate": "2026-04-20",
    "dailyNewCount": 20,
    "reviewOffsets": [0, 1, 3, 7, 14, 30],
    "ankiTemplateId": 1,
    "mdTemplateId": 1,
    "status": "ACTIVE",
    "createdAt": "2026-04-13T10:15:30+09:00",
    "updatedAt": "2026-04-13T10:20:00+09:00"
  },
  "error": null,
  "timestamp": "2026-04-13T10:20:00+09:00"
}
```

错误场景：
- 当前状态不是 `DRAFT` 或 `PAUSED` 时，返回 `CONFLICT`

## 5.6 暂停学习计划
### `POST /api/study-plans/{planId}/pause`

说明：
- 显式生命周期动作，将计划状态变更为 `PAUSED`
- 仅允许 `ACTIVE` 状态的计划执行该操作

响应示例：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "N4 每日 20 词",
    "wordSetId": 1,
    "startDate": "2026-04-20",
    "dailyNewCount": 20,
    "reviewOffsets": [0, 1, 3, 7, 14, 30],
    "ankiTemplateId": 1,
    "mdTemplateId": 1,
    "status": "PAUSED",
    "createdAt": "2026-04-13T10:15:30+09:00",
    "updatedAt": "2026-04-13T10:30:00+09:00"
  },
  "error": null,
  "timestamp": "2026-04-13T10:30:00+09:00"
}
```

错误场景：
- 当前状态不是 `ACTIVE` 时，返回 `CONFLICT`

## 5.7 归档学习计划
### `POST /api/study-plans/{planId}/archive`

说明：
- 显式生命周期动作，将计划状态变更为 `ARCHIVED`
- 允许 `DRAFT`、`ACTIVE`、`PAUSED` 状态的计划执行该操作
- 已经处于 `ARCHIVED` 状态的计划不能重复归档

响应示例：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "N4 每日 20 词",
    "wordSetId": 1,
    "startDate": "2026-04-20",
    "dailyNewCount": 20,
    "reviewOffsets": [0, 1, 3, 7, 14, 30],
    "ankiTemplateId": 1,
    "mdTemplateId": 1,
    "status": "ARCHIVED",
    "createdAt": "2026-04-13T10:15:30+09:00",
    "updatedAt": "2026-04-13T10:40:00+09:00"
  },
  "error": null,
  "timestamp": "2026-04-13T10:40:00+09:00"
}
```

错误场景：
- 当前状态已经是 `ARCHIVED` 时，返回 `CONFLICT`

## 5.8 学习量预览

### `GET /api/study-plans/{planId}/calendar?start=2026-04-20&end=2026-04-30`

响应：

```json
{
  "success": true,
  "data": [
    {
      "date": "2026-04-20",
      "newCount": 20,
      "reviewCount": 0,
      "totalCount": 20
    },
    {
      "date": "2026-04-21",
      "newCount": 20,
      "reviewCount": 20,
      "totalCount": 40
    }
  ],
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 6. 卡片接口

## 6.1 查询某日卡片

### `GET /api/study-plans/{planId}/cards/today?date=2026-04-21`

响应：

```json
{
  "success": true,
  "data": {
    "date": "2026-04-21",
    "planId": 1,
    "planName": "N4 每日 20 词",
    "newCount": 20,
    "reviewCount": 20,
    "cards": [
      {
        "id": 5001,
        "planId": 1,
        "wordEntryId": 1001,
        "cardType": "NEW",
        "sequenceNo": 1,
        "stageNo": 0,
        "dueDate": "2026-04-21",
        "status": "PENDING",
        "expression": "安心",
        "reading": "あんしん",
        "meaning": "安心，放心",
        "exampleJp": "それを聞いて安心した。",
        "exampleZh": "听到那个后我放心了。"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 6.2 查询卡片详情

### `GET /api/cards/{cardId}`

## 6.3 提交复习记录

### `POST /api/cards/{cardId}/review`

请求：

```json
{
  "rating": "GOOD",
  "responseTimeMs": 3200,
  "note": "能认出但造句不稳"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "reviewId": 9001,
    "cardId": 5001,
    "rating": "GOOD",
    "cardStatus": "DONE"
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

说明：

- 第一阶段记录评分但不动态改写后续间隔
- 同一张卡允许多次提交 review log，页面可只展示最后一次结果

## 6.4 查询卡片复习历史

### `GET /api/cards/{cardId}/reviews`

响应中的 `data` 为 `ReviewLog[]`

## 7. 模板接口

## 7.1 创建 Anki 模板

### `POST /api/templates/anki`

请求：

```json
{
  "name": "日语基础 Anki 模板",
  "description": "基础正反卡模板",
  "fieldMapping": {
    "front": ["expression"],
    "back": ["reading", "meaning", "exampleJp", "exampleZh"]
  },
  "frontTemplate": "{{expression}}",
  "backTemplate": "<div>{{reading}}</div><div>{{meaning}}</div><div>{{exampleJp}}</div><div>{{exampleZh}}</div>",
  "cssTemplate": ".card { font-size: 20px; }"
}
```

## 7.2 查询 Anki 模板列表

### `GET /api/templates/anki`

## 7.3 查询 Anki 模板详情

### `GET /api/templates/anki/{templateId}`

## 7.4 更新 Anki 模板

### `PUT /api/templates/anki/{templateId}`

## 7.5 预览 Anki 模板

### `POST /api/templates/anki/{templateId}/preview`

请求：

```json
{
  "sampleData": {
    "expression": "準備",
    "reading": "じゅんび",
    "meaning": "准备",
    "exampleJp": "明日の会議を準備する。",
    "exampleZh": "为明天的会议做准备。"
  }
}
```

响应：

```json
{
  "success": true,
  "data": {
    "frontRendered": "準備",
    "backRendered": "<div>じゅんび</div><div>准备</div><div>明日の会議を準備する。</div><div>为明天的会议做准备。</div>",
    "cssRendered": ".card { font-size: 20px; }"
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 7.6 创建 Markdown 模板

### `POST /api/templates/md`

请求：

```json
{
  "name": "每日学习清单",
  "description": "按日期输出学习内容",
  "templateContent": "# {{date}} 今日单词\n\n## 新词\n{{#newCards}}\n- **{{expression}}**（{{reading}}）：{{meaning}}\n{{/newCards}}"
}
```

## 7.7 查询 Markdown 模板列表

### `GET /api/templates/md`

## 7.8 查询 Markdown 模板详情

### `GET /api/templates/md/{templateId}`

## 7.9 更新 Markdown 模板

### `PUT /api/templates/md/{templateId}`

## 7.10 预览 Markdown 模板

### `POST /api/templates/md/{templateId}/preview`

请求：

```json
{
  "sampleData": {
    "date": "2026-04-20",
    "planName": "N4 每日 20 词",
    "newCards": [
      {
        "expression": "準備",
        "reading": "じゅんび",
        "meaning": "准备"
      }
    ],
    "reviewCards": []
  }
}
```

响应：

```json
{
  "success": true,
  "data": {
    "renderedContent": "# 2026-04-20 今日单词\n\n## 新词\n- **準備**（じゅんび）：准备"
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 8. 导出接口

## 8.1 创建 Anki CSV 导出任务

### `POST /api/exports/anki`

请求：

```json
{
  "planId": 1,
  "targetDate": "2026-04-20",
  "templateId": 1,
  "format": "CSV"
}
```

说明：

- `format` 可选 `CSV` 或 `TSV`
- `targetDate` 为空时表示导出整计划全部卡片

响应：

```json
{
  "success": true,
  "data": {
    "exportJobId": 10,
    "status": "SUCCESS",
    "downloadUrl": "/api/exports/10/download"
  },
  "error": null,
  "timestamp": "2026-04-13T10:15:30+09:00"
}
```

## 8.2 创建 Markdown 导出任务

### `POST /api/exports/md`

请求：

```json
{
  "planId": 1,
  "targetDate": "2026-04-20",
  "templateId": 1
}
```

## 8.3 查询导出记录

### `GET /api/exports?page=1&pageSize=20&planId=1&status=SUCCESS`

响应中的 `items` 为 `ExportJob[]`

## 8.4 下载导出文件

### `GET /api/exports/{exportJobId}/download`

成功响应：

- 返回文件流
- 设置 `Content-Disposition: attachment`

失败情况：

- `NOT_FOUND`
- `EXPORT_ERROR`

## 9. 推荐 DTO 结构

## 9.1 CreateWordSetRequest

```json
{
  "name": "string",
  "description": "string"
}
```

## 9.2 CreateWordEntryRequest

```json
{
  "expression": "string",
  "reading": "string",
  "meaning": "string",
  "partOfSpeech": "string",
  "exampleJp": "string",
  "exampleZh": "string",
  "level": "string",
  "tags": ["string"]
}
```

## 9.3 CreateStudyPlanRequest

```json
{
  "name": "string",
  "wordSetId": 1,
  "startDate": "2026-04-20",
  "dailyNewCount": 20,
  "reviewOffsets": [0, 1, 3, 7, 14, 30],
  "ankiTemplateId": 1,
  "mdTemplateId": 1
}
```

说明：
- 不包含 `status`
- 服务端固定默认创建为 `DRAFT`

## 9.4 UpdateStudyPlanRequest

```json
{
  "name": "string",
  "wordSetId": 1,
  "startDate": "2026-04-20",
  "dailyNewCount": 20,
  "reviewOffsets": [0, 1, 3, 7, 14, 30],
  "ankiTemplateId": 1,
  "mdTemplateId": 1
}
```

说明：
- 不包含 `status`
- 仅 `DRAFT` 和 `PAUSED` 状态的计划允许使用该请求体更新

## 9.5 ReviewCardRequest

```json
{
  "rating": "GOOD",
  "responseTimeMs": 3200,
  "note": "string"
}
```

## 9.6 CreateAnkiExportRequest

```json
{
  "planId": 1,
  "targetDate": "2026-04-20",
  "templateId": 1,
  "format": "CSV"
}
```

## 10. 后端实现建议

Controller 分组建议：

- `WordSetController`
- `WordEntryController`
- `StudyPlanController`
- `CardController`
- `TemplateController`
- `ExportController`

Service 分组建议：

- `WordSetService`
- `WordImportService`
- `StudyPlanService`
- `CardScheduleService`
- `ReviewService`
- `TemplateRenderService`
- `ExportService`

## 11. 前端 API 模块建议

前端建议拆分：

- `api/wordSets.ts`
- `api/words.ts`
- `api/studyPlans.ts`
- `api/cards.ts`
- `api/templates.ts`
- `api/exports.ts`

## 12. 联调优先顺序

为了尽快打通主流程，建议前后端联调按以下顺序：

1. 创建词库
2. 导入词条
3. 创建学习计划
4. 查询计划详情
5. 查询某日卡片
6. 创建模板
7. 预览模板
8. 导出 CSV / Markdown

## 13. 非目标说明

本文件不覆盖以下内容：

- 登录态和权限头设计
- OpenAPI 自动生成配置
- WebSocket 或实时通知
- 高级查询语言

如果后续需要，可以在本文件基础上继续补：

- `swagger/openapi.yaml`
- 统一错误码字典
- 接口测试样例集
## 14. Template CRUD Sync (2026-04-17)

This section overrides the older template notes above where they differ from the current implementation.

### 14.1 Anki template create

`POST /api/templates/anki`

Request:

```json
{
  "name": "Japanese Basic",
  "description": "Default anki card template",
  "fieldMapping": {
    "front": ["expression"],
    "back": ["reading", "meaning", "exampleJp", "exampleZh"]
  },
  "frontTemplate": "{{expression}}",
  "backTemplate": "<div>{{reading}}</div><div>{{meaning}}</div>",
  "cssTemplate": ".card { font-size: 20px; }"
}
```

Behavior:

- validates `frontTemplate` and `backTemplate` with `SimpleTemplateEngine.validate(...)`
- trims `name`, `description`, `frontTemplate`, `backTemplate`, and `cssTemplate`
- rejects duplicate template names with `409 CONFLICT`

### 14.2 Anki template update

`PUT /api/templates/anki/{templateId}`

Request body is identical to create.

Behavior:

- returns `404 NOT_FOUND` when `templateId` does not exist
- applies the same save-time validation and duplicate-name checks as create

### 14.3 Markdown template create

`POST /api/templates/md`

Request:

```json
{
  "name": "Daily Markdown",
  "description": "Daily study export template",
  "templateContent": "# {{date}}\n{{#newCards}}\n- {{expression}} / {{meaning}}\n{{/newCards}}"
}
```

Behavior:

- validates `templateContent` with allowed scalar variables plus `newCards` / `reviewCards` sections
- trims `name`, `description`, and `templateContent`
- rejects duplicate template names with `409 CONFLICT`

### 14.4 Markdown template update

`PUT /api/templates/md/{templateId}`

Request body is identical to create.

Behavior:

- returns `404 NOT_FOUND` when `templateId` does not exist
- applies the same save-time validation and duplicate-name checks as create

### 14.5 Template preview routes

Current preview endpoints are request-body driven and do not require template IDs:

- `POST /api/templates/anki/preview`
- `POST /api/templates/md/preview`

They return:

- rendered front/back/css for Anki preview
- rendered markdown content for Markdown preview

When template variables or sections are unsupported, the backend returns `400 TEMPLATE_RENDER_ERROR`.

### 14.6 Default template selection

There is no separate global "default template center" endpoint in the current implementation.

The effective selection flow is:

- study plan create/update requests continue to carry `ankiTemplateId` and `mdTemplateId`
- template management writes to the same template resources queried by the study plan page
- after template list refresh, newly created or updated templates become selectable in study plans
