package org.spongepowered.asm.mixin.injection.callback;

public class CallbackInfoReturnable<R> extends CallbackInfo {
    private R returnValue;

    public void setReturnValue(R returnValue) {
        this.returnValue = returnValue;
        cancel();
    }

    public R getReturnValue() {
        return returnValue;
    }
}
