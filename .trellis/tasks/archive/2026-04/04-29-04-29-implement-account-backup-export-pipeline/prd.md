# P0-b-2: implement account backup export pipeline

## Goal

实现免费用户的账号整体备份导出后端链路：当前账号点击创建备份后，后端生成 `MANUAL_BACKUP.zip` 并直接返回下载，同时通过 `BackupStorage` 抽象为后续 S3 / VIP 托管预留切换点。

## What I already know

* `P0-b-1` 已经定义了备份包格式和 `manifest.json` 基础类型。
* 第一版免费用户不保留服务端长期备份历史。
* 当前后端已有文件导出经验，`exportjob` 使用本地文件目录。
* 当前任务只实现“创建备份并下载”，不实现恢复。

## Requirements

* 需要新增 `backup` 领域的导出接口。
* 需要从当前账号收集第一版约定的学习资产。
* 需要生成符合 `backup-package-format.md` 的 `zip` 包。
* 需要通过 `BackupStorage` 抽象写入备份内容，再返回下载结果。
* 免费版导出完成后不保留长期服务端文件。

## Acceptance Criteria

* [ ] 后端存在 `POST /api/backups/export` 接口。
* [ ] 已登录用户调用该接口时，会收到一个 `application/zip` 下载响应。
* [ ] 包内包含 `manifest.json` 和第一版约定的 `data/*.json` 文件。
* [ ] 当前账号学习资产都按稳定顺序写入备份包。
* [ ] 服务端临时文件在读取响应内容后会删除。
* [ ] 已有测试覆盖备份包内容和导出链路关键行为。

## Out of Scope

* 这轮不做恢复接口
* 这轮不做前端 `/backups` 页面
* 这轮不做 S3 真接入
* 这轮不做托管历史

## Technical Notes

* 这一步优先补仓储查询、导出服务、存储抽象和控制器。
* 为了降低恢复实现复杂度，第一版导出数据文件不强依赖当前 `user_id` / `owner_user_id` 作为恢复时的主键绑定。
* 接口文档需要同步更新到 `docs/api-specification.md`。
