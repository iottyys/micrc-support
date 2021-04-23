package io.ttyys.micrc.sad.gradle.plugin.common;

/**
 * Utility methods for working with {@link String}s.
 */
public class Strings {
    /**
     * Not intended for instantiation.
     */
    private Strings() {
    }

    /**
     * Checks if a {@link String} is empty ({@code ""}) or {@code null}.
     *
     * @param str the String to check, may be {@code null}
     * @return true if the String is empty or {@code null}
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if a {@link String} is not empty ({@code ""}) and not {@code null}.
     *
     * @param str the String to check, may be {@code null}
     * @return true if the String is not empty and not {@code null}
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Requires that a {@link String} is not empty ({@code ""}) and not {@code null}.
     * If the requirement is violated, an {@link IllegalArgumentException} will be thrown.
     *
     * @param str     the String to check, may be {@code null}
     * @param message the message to include in
     * @return the String, if the requirement was not violated
     * @throws IllegalArgumentException if the requirement was violated
     */
    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    public static String requireNotEmpty(String str, String message) {
        if (isEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    public static String firstCharLower(String word) {
        return word.substring(0, 1).toLowerCase() + word.substring(1);
    }

    public static String firstCharUpper(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }
}
