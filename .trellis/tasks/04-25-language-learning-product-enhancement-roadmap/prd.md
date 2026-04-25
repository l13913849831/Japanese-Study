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

## Child Tasks

* `04-25-language-learning-feature-gaps`
  * 负责“作为语言学习软件还缺什么”的总体盘点
* `04-25-daily-weak-item-recovery-loop`
  * 负责“今日回捞 + WEAK 状态 + 易错项页面入口”的学习闭环设计与实现准备
* `04-25-migrate-word-review-scheduling-to-fsrs`
  * 负责把单词线从固定 `reviewOffsets` 调度迁到 FSRS 的独立设计任务

## Technical Notes

* 当前总任务只负责承接方向，不直接绑定实现细节。
* 真正进入开发时，应以子任务为单位推进。
