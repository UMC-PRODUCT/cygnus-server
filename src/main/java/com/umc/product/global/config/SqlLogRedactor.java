package com.umc.product.global.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class SqlLogRedactor {

    public static final String REDACTED = "[REDACTED]";

    private static final Pattern SQL_TOKEN_PATTERN = Pattern.compile(
        "(?s)(/\\*.*?\\*/)|(--[^\\r\\n]*)|(\\$([A-Za-z_][A-Za-z0-9_]*)\\$.*?\\$\\4\\$)|(\\$\\$.*?\\$\\$)|('(?:''|[^'])*')"
    );

    private SqlLogRedactor() {
    }

    public static String redact(String prepared, String boundSql) {
        String sqlStructure = StringUtils.hasText(prepared) ? prepared : boundSql;
        return redact(sqlStructure);
    }

    public static String redact(String sql) {
        if (!StringUtils.hasText(sql)) {
            return sql;
        }

        Matcher matcher = SQL_TOKEN_PATTERN.matcher(sql);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement;
            if (matcher.group(1) != null) {
                replacement = "/* " + REDACTED + " */";
            } else if (matcher.group(2) != null) {
                replacement = "-- " + REDACTED;
            } else {
                replacement = "'" + REDACTED + "'";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
