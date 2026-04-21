# Implement Review Loop

## Goal

补齐当前系统的最小复习闭环，使用户可以在 cards 页面完成一次卡片复习、提交评分、写入复习记录、刷新卡片状态，并查看该卡片的复习历史。

## Requirements

- 后端提供单卡复习提交接口 `POST /api/cards/{cardId}/review`
- 后端提供单卡复习历史查询接口 `GET /api/cards/{cardId}/reviews`
- 复习提交后写入 `review_log`
- 复习提交后更新对应 `card_instance.status`
- 评分支持 `AGAIN`、`HARD`、`GOOD`、`EASY`
- 前端 `cards` 页面提供复习提交入口
- 前端 `cards` 页面展示最近一次复习结果和历史记录
- 提交成功后刷新今日卡片状态
- 错误响应保持现有 `ApiResponse` / `BusinessException` 风格

## Acceptance Criteria

- [ ] 用户可以在 cards 页面选择一张卡并提交一次复习评分
- [ ] 后端能成功保存一条 `review_log`
- [ ] 被复习卡片的 `status` 从待处理状态更新为完成状态
- [ ] 前端在提交后能看到刷新后的卡片状态
- [ ] 用户可以查询并展示某张卡片的复习历史
- [ ] 非法卡片 ID、非法评分或非法状态场景会返回清晰错误

## Technical Notes

- 这是一个 fullstack 跨层功能，涉及前端页面、后端 controller/service/repository/entity/dto，以及持久化读写
- 优先完成最小闭环，不在本任务中引入 FSRS / SM-2 等高级调度算法
- 以现有 `review_log` 和 `card_instance` 表结构为基础实现，不新增数据库迁移，除非实现中发现当前结构无法满足最小闭环
- 需要保持与现有 Today Cards 查询返回结构兼容；如需扩展字段，应同步更新前端类型定义
- 实现后应补充必要文档或任务记录，方便后续继续做统计和更复杂调度

## Out of Scope

- 学习统计与仪表盘
- 连续学习天数
- 高级调度算法
- 多设备同步
- 音频、发音、例句播放
