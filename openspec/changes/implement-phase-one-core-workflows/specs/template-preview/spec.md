## ADDED Requirements

### Requirement: System SHALL preview Anki template rendering with sample context
系统 MUST 支持使用示例上下文渲染 Anki 模板，并返回 front、back 和 CSS 预览结果。

#### Scenario: User previews an Anki template
- **WHEN** 用户提交 Anki 模板内容和示例变量
- **THEN** 系统返回渲染后的 front、back 和 CSS 结果

### Requirement: System SHALL preview Markdown template rendering with sample context
系统 MUST 支持使用示例上下文渲染 Markdown 模板，并返回最终文本内容。

#### Scenario: User previews a Markdown template
- **WHEN** 用户提交 Markdown 模板内容和示例变量
- **THEN** 系统返回渲染后的 Markdown 文本

### Requirement: System SHALL reject unsupported template variables or syntax errors
系统 MUST 对不在白名单内的模板变量和模板语法错误返回标准化错误响应。

#### Scenario: Preview request uses unsupported variable
- **WHEN** 用户提交包含非法变量名或模板语法错误的预览请求
- **THEN** 系统返回 `TEMPLATE_RENDER_ERROR` 或字段级错误信息
