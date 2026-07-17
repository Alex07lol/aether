package net.minecraftforge.fml.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Mod {
    String modid();
    String name();
    String version();
    String acceptedMinecraftVersions() default "";
    boolean clientSideOnly() default false;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface EventHandler {}
}
