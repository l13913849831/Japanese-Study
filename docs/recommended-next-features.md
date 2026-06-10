# 日语学习系统后续推荐功能

## 1. 文档目的

这份文档只保留当前代码还没完成、但值得继续排优先级的功能。

它不再把下面这些已经落地的能力当成待办：

- 单词复习闭环
- 学习计划生命周期动作
- 词条 CRUD 与过滤
- 模板创建、更新、预览
- 导入预览流程
- 知识点 CRUD、Markdown 导入、知识点复习
- 统一今日学习工作台
- 连续复习会话体验

当前状态以 `2026-06-10` 的代码为准。

## 2. 当前产品定位

现在这个项目已经是一个可用的单机学习 MVP：

- 单词学习线能从导入一路走到计划、复习、历史、看板
- 知识点学习线能从 Markdown 导入一路走到复习和看板
- `/dashboard` 已经承担统一入口

所以后面的优先级，不该再围绕“有没有复习”“有没有 dashboard”“有没有 notes”这类旧问题展开。

## 3. 当前推荐优先级

### P2：导出工作流增强

当前状态：

- 导出创建、历史列表、下载、模板预检查和空日期阻断已经具备基础版
- 后续不再按“补全导出主流程”立项

建议增强范围：

- 更细的导出历史管理
- 更复杂的模板规则
- 更明确的失败分级与重试提示
- 长期报告导出到 Markdown 复盘材料

主影响面：

- `frontend/src/features/export-jobs/**`
- `backend/src/main/java/com/jp/vocab/exportjob/**`
- `docs/api-specification.md`

### P2：长周期学习指标

当前状态：

- MVP 已覆盖当前连续学习天数、最长连续学习天数、近 90 天单词 / 知识点趋势和未来 7 / 14 / 30 天负载预测
- 后续不再按“从零建设长期指标”立项

建议增强范围：

- 周 / 月复习量
- 计划筛选和知识点标签筛选
- 完成率：已完成量 / 到期量
- 平均响应耗时、评分分布、`AGAIN` 比例
- 薄弱项新增 / 消除趋势
- 掌握度阶段迁移
- 长期负载过载阈值配置
- 长期报告导出到 Markdown 复盘材料

主影响面：

- `backend/src/main/java/com/jp/vocab/dashboard/**`
- `backend/src/main/java/com/jp/vocab/note/service/NoteDashboardService.java`
- `frontend/src/features/dashboard/**`
- `frontend/src/features/notes/NoteDashboardPage.tsx`

### P2：复习体验继续打磨

为什么还要做：

- 这轮已经把复习主路径改成连续会话
- 但还有不少体验层细节没补

建议范围：

- 会话完成后的收尾页
- 跳过、回退、重做等更细动作
- 移动端更顺手的复习布局
- 更明确的“已完成 / 剩余 / 本轮表现”反馈

主影响面：

- `frontend/src/features/cards/TodayCardsPage.tsx`
- `frontend/src/features/notes/NoteReviewPage.tsx`
- `frontend/src/features/review/session.ts`

## 4. 暂时不要再重复立项的内容

下面这些内容，后续规划时默认视为“已完成基础版”，不要再按缺失能力提新任务：

- 词条导入预览
- 学习计划激活、暂停、归档
- 卡片复习提交与复习历史
- 模板创建、编辑、预览
- 单词学习仪表盘
- 知识点列表、导入、复习、仪表盘
- 统一学习工作台
- 连续复习会话

如果要继续做，应该用“增强”而不是“从 0 到 1”的方式描述。

## 5. 任务拆法建议

后续要开新任务，建议按下面三类拆：

1. 用户可见功能增强
2. 回归与质量补强
3. 文档与契约收口

不要把“新功能 + 文档补债 + 测试兜底”混成一条大任务。

## 6. 事实来源

当前真状态优先看：

- `docs/system-usage-guide.md`
- `.trellis/spec/backend/current-feature-contracts.md`
- `.trellis/spec/frontend/current-feature-workflows.md`

如果这里和代码不一致，以代码为准，再更新本文档。
