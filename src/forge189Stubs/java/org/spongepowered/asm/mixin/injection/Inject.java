package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Inject {
    String[] method() default {};
    At[] at() default {};
    boolean cancellable() default false;

    /**
     * How many targets the injection must find: a positive number is a hard requirement, 0 means
     * "apply if you can, skip quietly if you cannot" and -1 defers to the config's defaultRequire.
     */
    int require() default -1;
}
