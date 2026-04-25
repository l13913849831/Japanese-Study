# 日语学习系统接口定义文档

## 1. 文档目标

这份文档记录当前后端已经落地的 HTTP 接口契约，给前端联调、手工验证和后续补测试时使用。

如果本文和代码不一致，以当前 controller、dto、`.trellis/spec/` 为准，再回写本文。

## 2. 基础约定

基础路径：

```text
/api
```

请求格式：

- JSON：`application/json`
- 文件上传：`multipart/form-data`

下载格式：

- `text/csv`
- `text/tab-separated-values`
- `text/markdown`
- `application/octet-stream`

时间格式：

- 日期：`yyyy-MM-dd`
- 日期时间：ISO 8601

统一返回：

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-04-24T12:00:00Z"
}
```

列表分页：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

常见错误码：

- `VALIDATION_ERROR`
- `NOT_FOUND`
- `CONFLICT`
- `BAD_REQUEST`
- `IMPORT_ERROR`
- `TEMPLATE_RENDER_ERROR`
- `EXPORT_ERROR`
- `INTERNAL_ERROR`

## 3. 核心对象

### 3.1 WordSet

关键字段：

- `id`
- `name`
- `description`
- `createdAt`
- `updatedAt`

### 3.2 WordEntry

关键字段：

- `id`
- `wordSetId`
- `expression`
- `reading`
- `meaning`
- `partOfSpeech`
- `exampleJp`
- `exampleZh`
- `level`
- `tags`
- `sourceOrder`

### 3.3 StudyPlan

关键字段：

- `id`
- `name`
- `wordSetId`
- `startDate`
- `dailyNewCount`
- `reviewOffsets`
- `ankiTemplateId`
- `mdTemplateId`
- `status`

状态值：

- `DRAFT`
- `ACTIVE`
- `PAUSED`
- `ARCHIVED`

### 3.4 TodayCard

关键字段：

- `id`
- `planId`
- `wordEntryId`
- `cardType`
- `sequenceNo`
- `stageNo`
- `dueDate`
- `status`
- `expression`
- `reading`
- `meaning`
- `exampleJp`
- `exampleZh`

### 3.5 ReviewLog

关键字段：

- `id`
- `cardInstanceId`
- `reviewedAt`
- `rating`
- `responseTimeMs`
- `note`
- `createdAt`

评分值：

- `AGAIN`
- `HARD`
- `GOOD`
- `EASY`

### 3.6 Note

关键字段：

- `id`
- `title`
- `content`
- `tags`
- `reviewCount`
- `masteryStatus`
- `dueAt`
- `lastReviewedAt`
- `createdAt`
- `updatedAt`

掌握状态：

- `UNSTARTED`
- `LEARNING`
- `CONSOLIDATING`
- `MASTERED`

### 3.7 NoteReviewLog

关键字段：

- `id`
- `noteId`
- `reviewedAt`
- `rating`
- `responseTimeMs`
- `note`
- `createdAt`

### 3.8 Template

Anki 模板关键字段：

- `id`
- `name`
- `description`
- `fieldMapping`
- `frontTemplate`
- `backTemplate`
- `cssTemplate`

Markdown 模板关键字段：

- `id`
- `name`
- `description`
- `templateContent`

### 3.9 ExportJob

关键字段：

- `id`
- `planId`
- `exportType`
- `targetDate`
- `fileName`
- `filePath`
- `status`
- `createdAt`
- `updatedAt`

导出类型：

- `ANKI_CSV`
- `ANKI_TSV`
- `MARKDOWN`

## 4. 健康检查

### `GET /api/health`

返回服务状态。

## 5. 词库与词条接口

### `POST /api/word-sets`

创建词库。

### `GET /api/word-sets?page=&pageSize=`

分页查询词库。

### `GET /api/word-sets/{wordSetId}/words?page=&pageSize=&keyword=&level=&tag=`

分页查询词条，支持关键词、等级、标签过滤。

### `POST /api/word-sets/{wordSetId}/words`

新增单词条。

### `PUT /api/words/{wordId}`

更新单词条。

### `DELETE /api/words/{wordId}`

删除单词条。

### `POST /api/word-sets/{wordSetId}/import/preview`

上传 CSV 或 `.apkg`，只做预览分析，不落库。

请求：

- `file`

返回关键字段：

- `sourceType`
- `totalRows`
- `readyCount`
- `duplicateCount`
- `errorCount`
- `fieldMappings`
- `previewRows`

### `POST /api/word-sets/{wordSetId}/import`

上传文件并正式导入。

请求：

- `file`

返回关键字段：

- `importedCount`
- `skippedCount`
- `errors`

## 6. 学习计划接口

### `GET /api/study-plans?page=&pageSize=`

分页查询学习计划。

### `GET /api/study-plans/{id}`

查询学习计划详情。

### `POST /api/study-plans`

创建学习计划。

约束：

- `reviewOffsets` 必须非空
- `reviewOffsets` 必须从 `0` 开始
- `reviewOffsets` 必须升序

说明：

- `dailyNewCount` 继续控制新词引入节奏
- `reviewOffsets` 现在是兼容字段，不再驱动单词长期复习排期

### `PUT /api/study-plans/{id}`

更新学习计划。

约束：

- 只允许 `DRAFT` 和 `PAUSED` 状态更新
- 更新后会重建该计划的卡片实例

### `POST /api/study-plans/{id}/activate`

把计划切到 `ACTIVE`。

### `POST /api/study-plans/{id}/pause`

把计划切到 `PAUSED`。

### `POST /api/study-plans/{id}/archive`

把计划切到 `ARCHIVED`。

## 7. 单词卡片与复习接口

### `GET /api/study-plans/{planId}/cards/today?date=YYYY-MM-DD`

查询某计划某天的卡片列表。

当前返回是 `TodayCard[]`，不是旧版“按天聚合 + cards 字段”的包裹结构。

说明：

- 返回的是当前仍处于 `PENDING` 的运行时卡片
- 查询条件是“`dueAt` 早于所选日期结束时刻”，所以会包含逾期卡

### `GET /api/study-plans/{planId}/cards/calendar?start=YYYY-MM-DD&end=YYYY-MM-DD`

查询某计划某时间段的学习量预览。

当前返回元素字段：

- `date`
- `newCards`
- `reviewCards`

### `POST /api/cards/{cardId}/review`

提交单词卡片复习结果。

请求示例：

```json
{
  "rating": "GOOD",
  "responseTimeMs": 3200,
  "sessionAgainCount": 0,
  "note": "能认出但造句不稳"
}
```

返回关键字段：

- `reviewId`
- `cardId`
- `rating`
- `cardStatus`
- `reviewedAt`
- `weak`
- `weakMarkedAt`
- `todayAction`

说明：

- 当前实现会写入 `review_log`
- 当前实现会把当前卡片标记为 `DONE`
- 当前实现会追加一条新的待复习 FSRS 卡片实例，作为下次长期复习入口

### `GET /api/cards/{cardId}/reviews`

查询某张卡片的复习历史。

## 8. 单词学习仪表盘接口

### `GET /api/dashboard?date=YYYY-MM-DD`

返回单词学习聚合数据。

返回结构：

- `overview`
- `activePlans`
- `recentTrend`

`overview` 关键字段：

- `date`
- `activePlanCount`
- `totalDueToday`
- `newDueToday`
- `reviewDueToday`
- `pendingDueToday`
- `reviewedToday`

说明：

- 单词线统计基于当前 `PENDING` 且 `dueAt` 已到期的运行时卡片

## 9. 知识点接口

### `GET /api/notes?page=&pageSize=&keyword=&tag=&masteryStatus=`

分页查询知识点，支持关键词、标签、掌握状态过滤。

### `POST /api/notes`

创建知识点。

### `PUT /api/notes/{noteId}`

更新知识点。

### `DELETE /api/notes/{noteId}`

删除知识点。

### `POST /api/notes/import/preview`

上传 Markdown，做拆分预览，不落库。

请求：

- `file`
- `splitMode`
- `commonTagsText`

`splitMode` 取值：

- `H1`
- `H1_H2`
- `ALL`

返回关键字段：

- `splitMode`
- `totalItems`
- `readyCount`
- `errorCount`
- `previewItems`

`previewItems` 关键字段：

- `itemId`
- `title`
- `content`
- `tags`
- `status`
- `message`

### `POST /api/notes/import`

确认导入知识点草稿。

请求结构：

```json
{
  "items": [
    {
      "title": "Java / Stream",
      "content": "map / filter",
      "tags": ["java", "stream"]
    }
  ]
}
```

返回关键字段：

- `importedCount`
- `skippedCount`
- `errors`

## 10. 知识点复习与仪表盘接口

### `GET /api/notes/reviews/today?date=YYYY-MM-DD`

查询当日到期知识点队列。

返回元素关键字段：

- `id`
- `title`
- `content`
- `tags`
- `masteryStatus`
- `reviewCount`
- `dueAt`
- `lastReviewedAt`

### `POST /api/notes/{noteId}/reviews`

提交知识点复习结果。

请求示例：

```json
{
  "rating": "GOOD",
  "responseTimeMs": 2800,
  "note": "标题能回忆出来"
}
```

返回关键字段：

- `reviewId`
- `noteId`
- `rating`
- `masteryStatus`
- `reviewedAt`
- `dueAt`

说明：

- 评分固定映射到 FSRS
- 会写入 `note_review_log`
- 会更新 `note.reviewCount`、`note.masteryStatus`、`note.dueAt`、`note.lastReviewedAt`

### `GET /api/notes/{noteId}/reviews`

查询某条知识点的复习历史。

### `GET /api/notes/dashboard?date=YYYY-MM-DD`

查询知识点仪表盘。

返回结构：

- `overview`
- `masteryDistribution`
- `recentTrend`
- `recentNotes`

`overview` 关键字段：

- `date`
- `dueToday`
- `totalNotes`
- `reviewedNotes`

## 11. 模板接口

### `GET /api/templates/anki`

查询 Anki 模板列表。

### `POST /api/templates/anki`

创建 Anki 模板。

### `PUT /api/templates/anki/{id}`

更新 Anki 模板。

### `POST /api/templates/anki/preview`

预览 Anki 模板。

当前预览接口直接接收模板内容和示例数据，不是旧版 `/api/templates/anki/{templateId}/preview`。

### `GET /api/templates/md`

查询 Markdown 模板列表。

### `POST /api/templates/md`

创建 Markdown 模板。

### `PUT /api/templates/md/{id}`

更新 Markdown 模板。

### `POST /api/templates/md/preview`

预览 Markdown 模板。

## 12. 导出接口

### `GET /api/export-jobs?page=&pageSize=`

分页查询导出任务。

### `POST /api/export-jobs`

创建导出任务。

请求关键字段：

- `planId`
- `exportType`
- `targetDate`

### `GET /api/export-jobs/{id}/download`

下载导出结果。

当前导出接口统一走 `export-jobs` 路由，不再使用旧版 `/api/exports/*`。
