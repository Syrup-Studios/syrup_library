package net.syrupstudios.syruplibrary.config;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Element;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Normalizes decimal exponents that the bundled parser can misread. */
public final class Json5InputGuard {
    private static final int MAX_EXPANDED_NUMBER_LENGTH = 10_000;
    private static final Pattern DECIMAL_EXPONENT = Pattern.compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)[eE][+-]?[0-9]+");
    private static final Json5 JSON5 = new Json5();

    private Json5InputGuard() {}

    public static Json5Element parse(String input) {
        return JSON5.parse(expandExponents(input));
    }

    private static String expandExponents(String input) {
        StringBuilder normalized = null;
        int copiedThrough = 0;
        for (int index = 0; index < input.length();) {
            char current = input.charAt(index);
            if (current == '\'' || current == '"') {
                index = skipString(input, index, current);
            } else if (current == '/' && index + 1 < input.length()
                    && (input.charAt(index + 1) == '/' || input.charAt(index + 1) == '*')) {
                index = skipComment(input, index);
            } else if (isNumberStart(input, index)) {
                int end = numberEnd(input, index);
                if (hasExponent(input, index, end)) {
                    String number = input.substring(index, end);
                    String expanded = expandNumber(number);
                    if (expanded != null && !expanded.equals(number)) {
                        if (normalized == null) normalized = new StringBuilder(input.length());
                        normalized.append(input, copiedThrough, index).append(expanded);
                        copiedThrough = end;
                    }
                }
                index = end;
            } else {
                index++;
            }
        }
        if (normalized == null) return input;
        return normalized.append(input, copiedThrough, input.length()).toString();
    }

    private static String expandNumber(String number) {
        for (int index = 0; index < number.length(); index++) {
            if (number.charAt(index) == '_'
                    && (index == 0 || index + 1 == number.length()
                    || !isDigit(number.charAt(index - 1)) || !isDigit(number.charAt(index + 1)))) {
                throw new IllegalArgumentException("Invalid digit separator in decimal exponent number");
            }
        }
        String digits = number.replace("_", "");
        if (!DECIMAL_EXPONENT.matcher(digits).matches()) {
            throw new IllegalArgumentException("Invalid decimal exponent number");
        }
        int exponentAt = Math.max(digits.indexOf('e'), digits.indexOf('E'));
        int exponent;
        try {
            exponent = Integer.parseInt(digits.substring(exponentAt + 1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Decimal exponent expands beyond the supported number length", exception);
        }
        if (Math.abs((long) exponent) > MAX_EXPANDED_NUMBER_LENGTH
                || digits.length() + Math.abs((long) exponent) > MAX_EXPANDED_NUMBER_LENGTH) {
            throw new IllegalArgumentException("Decimal exponent expands beyond the supported number length");
        }
        String expanded = new BigDecimal(digits).toPlainString();
        if (expanded.length() > MAX_EXPANDED_NUMBER_LENGTH) {
            throw new IllegalArgumentException("Decimal exponent expands beyond the supported number length");
        }
        return expanded;
    }

    private static int skipString(String input, int index, char quote) {
        index++;
        while (index < input.length()) {
            char current = input.charAt(index++);
            if (current == '\\' && index < input.length()) index++;
            else if (current == quote) break;
        }
        return index;
    }

    private static int skipComment(String input, int index) {
        if (input.charAt(index + 1) == '/') {
            index += 2;
            while (index < input.length() && input.charAt(index) != '\n' && input.charAt(index) != '\r') index++;
            return index;
        }
        int end = input.indexOf("*/", index + 2);
        return end < 0 ? input.length() : end + 2;
    }

    private static boolean isNumberStart(String input, int index) {
        char current = input.charAt(index);
        int start = index;
        if (current == '+' || current == '-') {
            if (++index == input.length()) return false;
            current = input.charAt(index);
        }
        boolean startsNumber = isDigit(current) || current == '.' && index + 1 < input.length()
                && isDigit(input.charAt(index + 1));
        if (!startsNumber) return false;
        return start == 0 || !isIdentifierPart(input, start);
    }

    private static int numberEnd(String input, int index) {
        if (input.charAt(index) == '+' || input.charAt(index) == '-') index++;
        if (index + 1 < input.length() && input.charAt(index) == '0'
                && "xXbBoO".indexOf(input.charAt(index + 1)) >= 0) {
            index += 2;
            while (index < input.length() && isIdentifierPart(input.codePointAt(index))) {
                index += Character.charCount(input.codePointAt(index));
            }
            return index;
        }
        while (index < input.length() && (isDigit(input.charAt(index)) || input.charAt(index) == '_')) index++;
        if (index < input.length() && input.charAt(index) == '.') {
            index++;
            while (index < input.length() && (isDigit(input.charAt(index)) || input.charAt(index) == '_')) index++;
        }
        if (index < input.length() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
            index++;
            if (index < input.length() && (input.charAt(index) == '+' || input.charAt(index) == '-')) index++;
            while (index < input.length() && (isDigit(input.charAt(index)) || input.charAt(index) == '_')) index++;
        }
        return index;
    }

    private static boolean hasExponent(String input, int start, int end) {
        if (input.charAt(start) == '+' || input.charAt(start) == '-') start++;
        if (start + 1 < input.length() && input.charAt(start) == '0'
                && "xXbBoO".indexOf(input.charAt(start + 1)) >= 0) return false;
        for (int index = start; index < end; index++) {
            if (input.charAt(index) == 'e' || input.charAt(index) == 'E') return true;
        }
        return false;
    }

    private static boolean isDigit(char value) { return value >= '0' && value <= '9'; }

    private static boolean isIdentifierPart(String input, int index) {
        int value = input.codePointBefore(index);
        return isIdentifierPart(value);
    }

    private static boolean isIdentifierPart(int value) {
        int type = Character.getType(value);
        return value == '$' || value == '_' || value == '\\' || value == 0x200C || value == 0x200D
                || Character.isLetterOrDigit(value)
                || type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.CONNECTOR_PUNCTUATION;
    }
}
