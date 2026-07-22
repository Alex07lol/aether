package dev.aether.forge189.mixin;

import dev.aether.forge189.Mc189Compat;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    /** Static flag set by ForgeClientEventBridge when graphics.animation toggles. */
    public static boolean animationEnabled = false;

    @Inject(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemModelForEntity(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V"))
    public void renderItem(EntityLivingBase entity, ItemStack item, ItemCameraTransforms.TransformType transformType, CallbackInfo ci) {
        if (!animationEnabled) return;
        if (item == null || !(item.getItem() instanceof ItemSword)) return;
        if (!(entity instanceof EntityPlayer)) return;
        if (transformType != ItemCameraTransforms.TransformType.THIRD_PERSON) return;

        Mc189Compat.rotate(-45.0F, 0.0F, 1.0F, 0.0F);
        Mc189Compat.rotate(-20.0F, 1.0F, 0.0F, 0.0F);
        Mc189Compat.rotate(-60.0F, 0.0F, 0.0F, 1.0F);
        Mc189Compat.translate(-0.04F, -0.04F, 0.0F);
    }

    @Inject(method = "doBlockTransformations", at = @At("HEAD"), cancellable = true)
    public void swordBlockTransformations(CallbackInfo ci) {
        if (!animationEnabled) return;
        Mc189Compat.translate(-0.24F, 0.17F, 0.0F);
        Mc189Compat.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        Mc189Compat.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        Mc189Compat.rotate(60.0F, 0.0F, 1.0F, 0.0F);
        Mc189Compat.translate(0.0F, 0.18F, 0.00F);
        ci.cancel();
    }
}
