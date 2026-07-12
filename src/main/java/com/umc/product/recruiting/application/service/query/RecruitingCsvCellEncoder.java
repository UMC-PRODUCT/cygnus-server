package com.umc.product.recruiting.application.service.query;

final class RecruitingCsvCellEncoder {

    private RecruitingCsvCellEncoder() {
    }

    static String encode(Object value) {
        if (value == null) {
            return "";
        }
        String encoded = String.valueOf(value);
        if (!encoded.isEmpty() && isFormulaPrefix(encoded.charAt(0))) {
            encoded = "'" + encoded;
        }
        if (requiresRfc4180Escaping(encoded)) {
            return "\"" + encoded.replace("\"", "\"\"") + "\"";
        }
        return encoded;
    }

    private static boolean isFormulaPrefix(char firstCharacter) {
        return firstCharacter == '='
            || firstCharacter == '+'
            || firstCharacter == '-'
            || firstCharacter == '@'
            || firstCharacter == '\t'
            || firstCharacter == '\r'
            || firstCharacter == '\n';
    }

    private static boolean requiresRfc4180Escaping(String value) {
        return value.indexOf(',') >= 0
            || value.indexOf('"') >= 0
            || value.indexOf('\r') >= 0
            || value.indexOf('\n') >= 0;
    }
}
