# 测试导入素材

这组文件用于当前仓库的手动回归测试，目标是优先覆盖：

- 词库创建与 CSV 导入预览 / 导入
- 知识点 Markdown 导入预览 / 导入
- 计划创建、卡片生成、复习链路
- 备份 / 恢复后的内容量验证

## 文件说明

- `jlpt-n2-style-vocab-500.csv`
  - 500 条 N2 风格测试词条
  - 可直接用于 `/word-sets` 的 CSV 预览与导入
  - 表头已按当前系统识别字段写好
- `jlpt-n2-style-notes-100.md`
  - 100 条 N2 风格语法/表达知识点
  - 可直接用于 `/notes` 的 Markdown 预览与导入
  - 推荐 `splitMode = H1`

## 单词导入建议

建议字段：

- `expression`
- `reading`
- `meaning`
- `partOfSpeech`
- `exampleJp`
- `exampleZh`
- `level`
- `tags`

建议测试点：

1. 新建一个词库，例如 `N2 Test 500`
2. 上传 `jlpt-n2-style-vocab-500.csv`
3. 先看 preview 统计是否接近：
   - `totalRows = 500`
   - `readyCount = 500`
4. 确认导入
5. 再试一次同文件，检查 duplicate 分支

## 知识点导入建议

建议步骤：

1. 打开 `/notes`
2. 上传 `jlpt-n2-style-notes-100.md`
3. `splitMode` 选择 `H1`
4. 预览应看到：
   - `totalItems = 100`
   - 大多数条目为 `READY`
5. 确认导入

## 关于“完整 N2”说明

这批素材是 **N2 风格测试集**，优先保证：

- 数量足够
- 格式稳定
- 可直接导入
- 适合功能回归和压力测试

它不是官方完整 N2 词汇/语法标准库。

如果你后面要做：

- 更贴近考试的完整 N2 词库
- 更严格校对的 N2 语法点全集
- 带 reading / 例句 / 释义逐条精校的数据包

建议单独接一个外部数据源或整理一版可授权词库。
