# P0-b-4: build backups page and navigation

## Goal

实现免费用户可用的“备份与恢复”前端入口：支持创建整体备份并直接下载、上传本地备份包进行恢复准备、先下载安全快照、再明确确认恢复。

## What I already know

* `P0-b-2` 已经提供 `POST /api/backups/export`，直接返回账号整体备份 `zip`。
* `P0-b-3` 已经提供恢复三段链路：
  * `POST /api/backups/restore/prepare`
  * `GET /api/backups/restore/{token}/safety-snapshot`
  * `POST /api/backups/restore/{token}/confirm`
* 免费用户不保留服务端长期托管备份历史。
* `MANUAL_BACKUP` 是用户主动导出的备份包；`SAFETY_SNAPSHOT` 是恢复前自动生成的安全回滚快照。
* 恢复会话是短期的，前端流程需要尽量紧凑，不应假设用户可以长时间中断后再回来确认。

## Requirements

* 需要新增独立 `/backups` 页面，而不是复用 `/export-jobs`。
* 需要在管理区菜单中增加“备份与恢复”入口。
* 页面需要支持“创建备份并直接下载”。
* 页面需要支持上传本地备份 `zip`，触发恢复准备。
* 页面需要在恢复准备成功后，引导用户先下载 `SAFETY_SNAPSHOT`。
* 页面需要在安全快照下载完成后，才允许继续点击确认恢复。
* 页面需要明确提示：
  * 免费用户备份不会长期保存在服务器
  * 恢复会覆盖当前账号学习资产
  * 登录凭据和系统默认资料不在本轮覆盖范围

## Acceptance Criteria

* [ ] 前端存在独立 `/backups` 路由和管理菜单入口。
* [ ] 用户可以点击创建整体备份并下载本地 `zip`。
* [ ] 用户可以上传本地备份包并拿到恢复准备结果。
* [ ] 恢复流程中必须先下载安全快照，再允许确认恢复。
* [ ] 页面不展示免费版“服务端历史备份列表”。
* [ ] `frontend/npm run build` 通过。

## Out of Scope

* 这轮不做 S3 / VIP 托管备份
* 这轮不做服务端备份历史列表
* 这轮不做恢复历史记录页
* 这轮不做拖很久仍可继续的持久化恢复会话

## Technical Notes

* 前端 API 继续遵循 `features/<feature>/api.ts` 模式。
* 下载链路需要复用 `Blob + object URL` 模式，而不是新造请求封装。
* 上传恢复准备可沿用 `postFormData`。
* 为降低误操作风险，确认恢复按钮需要显式依赖“安全快照已下载”这一前端状态。
