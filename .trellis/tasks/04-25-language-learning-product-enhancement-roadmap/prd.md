# brainstorm: language learning product enhancement roadmap

## Goal

基于当前仓库已经落地的单词线和知识点线能力，整理后续“更像语言学习软件”的增强路线，形成一个总任务容器，并把后续重点方向拆成可独立推进的子任务。

## What I already know

* 当前项目已经有：
  * 单词学习线
  * 知识点学习线
  * 统一 dashboard 入口
  * 模板和导出
* 当前项目更像单机学习工具，还不是完整语言学习产品。
* 当前已经盘点出的主要增强方向有三类：
  * 学习闭环增强
  * 综合训练扩展
  * 产品基础设施
* 当前最先收敛出的可执行方向是：
  * `daily weak item recovery loop`

## Requirements

* 需要把产品增强方向整理成一个总任务。
* 需要允许后续把每个方向独立拆成子任务推进。
* 当前阶段优先启动“学习闭环 / 易错项机制”子任务。

## Acceptance Criteria

* [ ] 总任务已建立并可承接多个子任务。
* [ ] 当前已存在至少一个“学习闭环”子任务。
* [ ] 后续方向可以继续按子任务方式补进来。

## Out of Scope

* 这轮不直接实现功能
* 这轮不展开所有方向的详细实现设计

## Active Child Tasks

当前主线收敛为 6 个仍可独立推进的任务：

* `04-28-04-28-minimal-user-ownership-foundation`
  * `P0-a`：最小用户归属底座
* `04-28-04-28-unify-learning-main-path`
  * `P1-A`：统一每日学习主路径
* `04-28-04-28-closure-presentation-and-completion-feedback`
  * `P1-B`：闭环呈现与完成反馈
* `04-28-04-28-long-term-feedback-and-retrospective`
  * `P1-C`：长期反馈与复盘
* `04-28-04-28-sync-and-productization-extension`
  * `P0-b`：同步、备份、恢复与产品化延伸
* `04-29-account-system-and-admin-backend-evolution`
  * `P0-c`：账号治理与管理员运营基础

## Current Priority Adjustment

当前主线优先级建议调整为：

1. `P0-c-1`：先补 session 安全加固与 cookie/CSRF 基线
2. `P0-c-2`：再补管理员角色基础与后台访问控制
3. `P0-c-3`：然后做用户治理后台 MVP
4. `P0-b` 后续再继续往云端托管备份、VIP 与更重产品化管理能力扩展

原因：

* `P0-b` 当前“本地备份 / 本地恢复”MVP 已经落地
* 当前更急迫的产品基础设施短板转向：
  * 账号安全治理
  * 权限边界
  * 用户治理后台
* JWT / 外部身份源仍然重要，但不是眼前第一优先级

## Archived Decision / Planning Inputs

以下任务不再作为独立活跃任务保留，内容已并入上面的主线任务：

* `04-25-language-learning-feature-gaps`
  * 优先级判断来源，已沉淀为 `P0-a / P1-A / P1-B / P1-C / P0-b`
* `04-28-04-28-infrastructure-foundation-gaps`
  * 基础设施缺口已拆入 `P0-a` 和 `P0-b`
* `04-26-04-26-split-learning-mode-and-management-mode-after-login`
  * 登录底座归 `P0-a`，学习/管理信息架构归 `P1-A`
* `04-26-04-26-split-export-into-learning-action-and-management-history`
  * 学习收尾导出归 `P1-B`，历史管理归后续产品化管理能力

## Technical Notes

* 当前总任务只负责承接方向，不直接绑定实现细节。
* 真正进入开发时，应以子任务为单位推进。
