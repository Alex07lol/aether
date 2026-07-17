package dev.aether.fairplay;

import dev.aether.module.ClientModule.ModuleMetadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class FairPlayPolicy {
    private final Set<String> prohibitedTokens;

    private FairPlayPolicy(Set<String> prohibitedTokens) {
        this.prohibitedTokens = Collections.unmodifiableSet(new LinkedHashSet<String>(prohibitedTokens));
    }

    public static FairPlayPolicy standard() {
        return new FairPlayPolicy(new LinkedHashSet<String>(Arrays.asList(
            "killaura",
            "aimassist",
            "aimbot",
            "autoclicker",
            "triggerbot",
            "reachhack",
            "velocity",
            "antiknockback",
            "fly",
            "speedhack",
            "scaffold",
            "xray",
            "esp",
            "blink",
            "noslow"
        )));
    }

    public void validate(ModuleMetadata metadata) {
        String id = metadata.id();
        String name = metadata.name();
        for (String token : prohibitedTokens) {
            if (matches(id, token) || matches(name, token)) {
                throw new FairPlayViolationException("Rejected prohibited module: " + metadata.id());
            }
        }
    }

    public Set<String> prohibitedTokens() {
        return prohibitedTokens;
    }

    private static String normalize(String value) {
        String lower = value.toLowerCase(Locale.ENGLISH);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean matches(String value, String prohibitedToken) {
        String normalized = normalize(value);
        if (normalized.equals(prohibitedToken)) {
            return true;
        }
        if (prohibitedToken.length() > 3 && normalized.contains(prohibitedToken)) {
            return true;
        }

        String[] words = value.toLowerCase(Locale.ENGLISH).split("[^a-z0-9]+");
        for (String word : words) {
            if (word.equals(prohibitedToken)) {
                return true;
            }
        }
        return false;
    }
}
