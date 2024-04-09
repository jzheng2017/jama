package nl.jiankai.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TypeUtil {
    public static <T> T getDefaultValue(String type) {
        return (T) switch (type) {
            case "int", "Integer", "short", "Short", "byte", "Byte" -> 0;
            case "long", "Long" -> 0L;
            case "char" -> '\u0000';
            case "String", "char[]" -> "";
            case "float", "Float" -> 0.0f;
            case "double", "Double" -> 0.0d;
            case "BigDecimal" -> BigDecimal.ZERO;
            case "BigInteger" -> BigInteger.ZERO;
            case "boolean", "Boolean" -> false;
            default -> null;
        };
    }
}
