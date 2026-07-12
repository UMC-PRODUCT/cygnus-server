package com.umc.product.global.config;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class SqlLogRedactor {

    public static final String REDACTED = "[REDACTED]";

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("--[^\\r\\n]*");
    private static final Pattern TAGGED_DOLLAR_QUOTED = Pattern.compile(
        "\\$([A-Za-z_][A-Za-z0-9_]*)\\$.*?\\$\\1\\$",
        Pattern.DOTALL
    );
    private static final Pattern DOLLAR_QUOTED = Pattern.compile("\\$\\$.*?\\$\\$", Pattern.DOTALL);
    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:''|[^'])*'", Pattern.DOTALL);

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

        String redacted = BLOCK_COMMENT.matcher(sql).replaceAll("/* " + REDACTED + " */");
        redacted = LINE_COMMENT.matcher(redacted).replaceAll("-- " + REDACTED);
        redacted = TAGGED_DOLLAR_QUOTED.matcher(redacted).replaceAll("'" + REDACTED + "'");
        redacted = DOLLAR_QUOTED.matcher(redacted).replaceAll("'" + REDACTED + "'");
        return STRING_LITERAL.matcher(redacted).replaceAll("'" + REDACTED + "'");
    }
}
