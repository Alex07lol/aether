package org.spongepowered.asm.mixin.injection.callback;

public class CallbackInfoReturnable<R> extends CallbackInfo {
    private R returnValue;

    public void setReturnValue(R returnValue) {
        this.returnValue = returnValue;
        cancel();
    }

    /** Primitive overload, mirroring Sponge's API: avoids boxing a value once per hook call. */
    @SuppressWarnings("unchecked")
    public void setReturnValue(float returnValue) {
        this.returnValue = (R) Float.valueOf(returnValue);
        cancel();
    }

    public R getReturnValue() {
        return returnValue;
    }

    /** Primitive getter, mirroring Sponge's API. */
    public float getReturnValueF() {
        return this.returnValue instanceof Number ? ((Number) this.returnValue).floatValue() : 0.0F;
    }
}
