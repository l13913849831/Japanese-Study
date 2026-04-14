insert into anki_template (
    name,
    description,
    field_mapping,
    front_template,
    back_template,
    css_template
) values (
    'Default Japanese Anki',
    'Default template for Japanese vocabulary export',
    '{
      "front": ["expression"],
      "back": ["reading", "meaning", "exampleJp", "exampleZh"]
    }'::jsonb,
    '{{expression}}',
    '<div>{{reading}}</div><div>{{meaning}}</div><div>{{exampleJp}}</div><div>{{exampleZh}}</div>',
    '.card { font-family: Arial, sans-serif; font-size: 20px; }'
);

insert into md_template (
    name,
    description,
    template_content
) values (
    'Default Daily Markdown',
    'Default markdown export template for daily study cards',
    '# {{date}} 今日单词

## 新词
{{#newCards}}
- **{{expression}}**（{{reading}}）：{{meaning}}
  - 例句：{{exampleJp}}
{{/newCards}}

## 复习
{{#reviewCards}}
- **{{expression}}**（{{reading}}）：{{meaning}}
{{/reviewCards}}'
);
