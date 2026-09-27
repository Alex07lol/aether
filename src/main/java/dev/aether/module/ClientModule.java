package dev.aether.module;

import dev.aether.module.setting.Setting;

import java.util.List;

public interface ClientModule {
    ModuleMetadata metadata();

    ModuleState state();

    void enable();

    void disable();

    List<Setting<?>> settings();

    enum ModuleState {
        ENABLED,
        DISABLED
    }

    enum ModuleCategory {
        GENERAL,
        PERFORMANCE,
        GRAPHICS,
        RENDER,
        INTERFACE,
        MOVEMENT,
        AUDIO,
        HUD,
        PVP,
        COSMETICS,
        ACCESSIBILITY,
        THEMES
    }

    final class ModuleMetadata {
        private final String id;
        private final String name;
        private final ModuleCategory category;
        private final String description;
        private final boolean favoriteByDefault;
        private final String group;

        private ModuleMetadata(Builder builder) {
            this.id = builder.id;
            this.name = builder.name;
            this.category = builder.category;
            this.description = builder.description;
            this.favoriteByDefault = builder.favoriteByDefault;
            this.group = builder.group;
        }

        public static Builder builder(String id, String name) {
            return new Builder(id, name);
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public ModuleCategory category() {
            return category;
        }

        public String description() {
            return description;
        }

        public boolean favoriteByDefault() {
            return favoriteByDefault;
        }

        /**
         * Optional mutual-exclusion group. Modules that share a group behave like a
         * radio: enabling one disables the rest, which is how the theme modules keep a
         * single active palette without every screen re-implementing the rule.
         */
        public String group() {
            return group;
        }

        public static final class Builder {
            private final String id;
            private final String name;
            private ModuleCategory category = ModuleCategory.GENERAL;
            private String description = "";
            private boolean favoriteByDefault;
            private String group;

            private Builder(String id, String name) {
                if (id == null || id.trim().isEmpty()) {
                    throw new IllegalArgumentException("Module id cannot be blank.");
                }
                if (name == null || name.trim().isEmpty()) {
                    throw new IllegalArgumentException("Module name cannot be blank.");
                }
                this.id = id;
                this.name = name;
            }

            public Builder category(ModuleCategory category) {
                this.category = category == null ? ModuleCategory.GENERAL : category;
                return this;
            }

            public Builder description(String description) {
                this.description = description == null ? "" : description;
                return this;
            }

            public Builder favoriteByDefault(boolean favoriteByDefault) {
                this.favoriteByDefault = favoriteByDefault;
                return this;
            }

            public Builder group(String group) {
                this.group = group == null || group.trim().isEmpty() ? null : group.trim();
                return this;
            }

            public ModuleMetadata build() {
                return new ModuleMetadata(this);
            }
        }
    }
}
