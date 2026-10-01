package dev.aether.cosmetic;

public enum CosmeticType {
    STATIC_CAPE,
    ANIMATED_CAPE,
    WINGS,
    HALO,
    HAT,
    BANDANA,
    MASK,
    SHOULDER_PET,
    BACK_ACCESSORY,
    CLOAK,
    NAMEPLATE,
    TRAIL,
    EMOTE;

    /**
     * The label a screen shows for this slot: the handful of types users recognise by
     * name are written out, and the rest title-case themselves from the enum constant,
     * so adding a type never requires touching a screen.
     */
    public String displayName() {
        switch (this) {
            case STATIC_CAPE: return "Cape";
            case ANIMATED_CAPE: return "Animated Cape";
            case WINGS: return "Wings";
            case HALO: return "Halo";
            case HAT: return "Hat";
            case BANDANA: return "Bandana";
            case MASK: return "Mask";
            case SHOULDER_PET: return "Shoulder Pet";
            case BACK_ACCESSORY: return "Back Accessory";
            case CLOAK: return "Cloak";
            case NAMEPLATE: return "Nameplate";
            case TRAIL: return "Trail";
            case EMOTE: return "Emote";
            default: {
                String raw = name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
                StringBuilder out = new StringBuilder(raw.length());
                boolean capitalize = true;
                for (int i = 0; i < raw.length(); i++) {
                    char c = raw.charAt(i);
                    if (c == ' ') {
                        capitalize = true;
                        out.append(c);
                    } else if (capitalize) {
                        out.append(Character.toUpperCase(c));
                        capitalize = false;
                    } else {
                        out.append(c);
                    }
                }
                return out.toString();
            }
        }
    }
}
