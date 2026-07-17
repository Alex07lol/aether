package dev.aether.cosmetic;

public final class CosmeticValidationResult {
    private final boolean valid;
    private final String message;

    private CosmeticValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static CosmeticValidationResult valid(String message) {
        return new CosmeticValidationResult(true, message);
    }

    public static CosmeticValidationResult invalid(String message) {
        return new CosmeticValidationResult(false, message);
    }

    public boolean valid() {
        return valid;
    }

    public String message() {
        return message;
    }
}

