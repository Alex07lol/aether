package dev.aether;

public final class TestSupport {
    private TestSupport() {
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " Expected <" + expected + "> but got <" + actual + ">.");
        }
    }
}

