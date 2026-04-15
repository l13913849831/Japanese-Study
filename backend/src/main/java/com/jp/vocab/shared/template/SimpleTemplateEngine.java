package com.jp.vocab.shared.template;

import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SimpleTemplateEngine {

    private static final Pattern SECTION_PATTERN = Pattern.compile("\\{\\{#\\s*([\\w.]+)\\s*}}(.*?)\\{\\{/\\s*\\1\\s*}}", Pattern.DOTALL);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([#/])?\\s*([\\w.]+)\\s*}}");

    public String render(String template, Map<String, Object> context) {
        try {
            return renderInternal(template, context);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.TEMPLATE_RENDER_ERROR, "Template rendering failed");
        }
    }

    public void validate(String template, Set<String> allowedVariables, Set<String> allowedSections) {
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(template);
        while (tokenMatcher.find()) {
            String prefix = tokenMatcher.group(1);
            String name = tokenMatcher.group(2);
            if ("#".equals(prefix) || "/".equals(prefix)) {
                if (!allowedSections.contains(name)) {
                    throw new BusinessException(ErrorCode.TEMPLATE_RENDER_ERROR, "Unsupported section: " + name);
                }
            } else if (!allowedVariables.contains(name)) {
                throw new BusinessException(ErrorCode.TEMPLATE_RENDER_ERROR, "Unsupported variable: " + name);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String renderInternal(String template, Map<String, Object> context) {
        String rendered = template;

        Matcher sectionMatcher = SECTION_PATTERN.matcher(rendered);
        StringBuffer sectionBuffer = new StringBuffer();
        while (sectionMatcher.find()) {
            String sectionName = sectionMatcher.group(1);
            String body = sectionMatcher.group(2);
            Object sectionValue = context.get(sectionName);
            String replacement = "";

            if (sectionValue instanceof List<?> list) {
                StringBuilder builder = new StringBuilder();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        Map<String, Object> nested = new HashMap<>(context);
                        nested.putAll((Map<String, Object>) map);
                        builder.append(renderInternal(body, nested));
                    }
                }
                replacement = builder.toString();
            } else if (sectionValue instanceof Boolean bool && bool) {
                replacement = renderInternal(body, context);
            }

            sectionMatcher.appendReplacement(sectionBuffer, Matcher.quoteReplacement(replacement));
        }
        sectionMatcher.appendTail(sectionBuffer);
        rendered = sectionBuffer.toString();

        if (rendered.contains("{{#") || rendered.contains("{{/")) {
            throw new BusinessException(ErrorCode.TEMPLATE_RENDER_ERROR, "Template section syntax is invalid");
        }

        Matcher variableMatcher = VARIABLE_PATTERN.matcher(rendered);
        StringBuffer variableBuffer = new StringBuffer();
        while (variableMatcher.find()) {
            String key = variableMatcher.group(1);
            Object value = context.getOrDefault(key, "");
            variableMatcher.appendReplacement(variableBuffer, Matcher.quoteReplacement(String.valueOf(value == null ? "" : value)));
        }
        variableMatcher.appendTail(variableBuffer);

        return variableBuffer.toString();
    }
}
