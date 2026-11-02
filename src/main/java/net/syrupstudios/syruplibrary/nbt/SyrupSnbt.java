package net.syrupstudios.syruplibrary.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

/** Utilities for formatting and parsing SNBT. */
public final class SyrupSnbt {
    private SyrupSnbt() {}

    /** Formats valid SNBT. Whitespace outside quoted strings is normalized. */
    public static String format(String snbt) {
        StringBuilder result = new StringBuilder(snbt.length());
        int indent = 0;
        char quote = 0;
        boolean escaped = false;
        boolean typedArray = false;

        for (int i = 0; i < snbt.length(); i++) {
            char c = snbt.charAt(i);
            if (quote != 0) {
                result.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                result.append(c);
            } else if (typedArray) {
                if (!Character.isWhitespace(c)) {
                    if (c == ',') result.append(", ");
                    else result.append(c);
                    if (c == ']') typedArray = false;
                }
            } else if (Character.isWhitespace(c)) {
                continue;
            } else if (c == '[' && isTypedArray(snbt, i)) {
                typedArray = true;
                result.append(c);
            } else if (c == '{' || c == '[') {
                result.append(c);
                if (!isEmptyContainer(snbt, i)) {
                    result.append('\n');
                    indent++;
                    appendIndent(result, indent);
                }
            } else if (c == '}' || c == ']') {
                if (!isClosingEmptyContainer(result)) {
                    result.append('\n');
                    appendIndent(result, --indent);
                }
                result.append(c);
            } else if (c == ',') {
                result.append(",\n");
                appendIndent(result, indent);
            } else if (c == ':') {
                result.append(": ");
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /** Formats an NBT tag using its SNBT representation. */
    public static String format(CompoundTag tag) {
        return format(tag.toString());
    }

    /** Parses a compound from SNBT and propagates Minecraft's syntax error. */
    public static CompoundTag parseCompound(String snbt) throws CommandSyntaxException {
        //? if >=1.21.11 {
        /*return TagParser.parseCompoundFully(snbt);
        *///?} else {
        return TagParser.parseTag(snbt);
        //?}
    }

    private static void appendIndent(StringBuilder result, int indent) {
        result.append("  ".repeat(indent));
    }

    private static boolean isEmptyContainer(String snbt, int openIndex) {
        for (int i = openIndex + 1; i < snbt.length(); i++) {
            if (!Character.isWhitespace(snbt.charAt(i))) return snbt.charAt(i) == (snbt.charAt(openIndex) == '{' ? '}' : ']');
        }
        return false;
    }

    private static boolean isClosingEmptyContainer(StringBuilder result) {
        int length = result.length();
        return length > 0 && (result.charAt(length - 1) == '{' || result.charAt(length - 1) == '[');
    }

    private static boolean isTypedArray(String snbt, int openIndex) {
        int i = openIndex + 1;
        while (i < snbt.length() && Character.isWhitespace(snbt.charAt(i))) i++;
        if (i >= snbt.length()) return false;
        char type = snbt.charAt(i++);
        if (type != 'B' && type != 'I' && type != 'L') return false;
        while (i < snbt.length() && Character.isWhitespace(snbt.charAt(i))) i++;
        return i < snbt.length() && snbt.charAt(i) == ';';
    }
}
