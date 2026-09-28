package dev.aether.forge189.mixin;

import dev.aether.forge189.Mc189Compat;
import dev.aether.graphics.FirstPersonAnims;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 animations for {@code graphics.animation}.
 * <p>
 * The first-person poses are produced by rewriting the arguments of vanilla's own render
 * methods rather than by reimplementing them, so the item geometry stays vanilla's:
 * <ul>
 *   <li>1.8 freezes the arm swing while an item is in use (it passes {@code 0} as the swing
 *       progress), which is what stopped the blocking sword and the food from bobbing. The
 *       transform redirect passes the live swing progress instead, for BLOCK and EAT/DRINK.</li>
 *   <li>1.8 eases the bow draw with a smoothstep where 1.7 used a square. The bow redirect
 *       calls vanilla's bow transform with a compensated partial tick, so its rotations follow
 *       the 1.7 curve. See {@link FirstPersonAnims}.</li>
 *   <li>Third-person poses (the sword block pose and the lowered fishing rod) are inline
 *       transforms, because there is no vanilla argument to steer.</li>
 * </ul>
 * <p>
 * This mixin deliberately declares no {@code @Shadow} members. A shadow is a hard requirement: if
 * the member cannot be resolved the whole mixin fails and the client crashes on start, which is what
 * a production runtime without a generated refmap would do. Vanilla's own transforms and the held
 * item are reached through {@link Mc189Compat} instead, which tries the development name and then
 * the SRG name and simply does not enhance the pose when neither is present.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    /** Set by ForgeClientEventBridge while graphics.animation > block_animation is on. */
    public static boolean blockAnimationEnabled = false;

    /** Set by ForgeClientEventBridge while graphics.animation > eat_drink_animation is on. */
    public static boolean eatDrinkAnimationEnabled = false;

    /** Set by ForgeClientEventBridge while graphics.animation > bow_animation is on. */
    public static boolean bowAnimationEnabled = false;

    /** Set by ForgeClientEventBridge while graphics.animation > rod_animation is on. */
    public static boolean fishingRodAnimationEnabled = false;

    /** MCP first, then the SRG name a production runtime uses. */
    private static final String[] ITEM_TO_RENDER = {"itemToRender", "field_78453_b"};
    private static final String[] TRANSFORM_FIRST_PERSON = {"transformFirstPersonItem", "func_178096_b"};
    private static final String[] DO_BOW_TRANSFORMATIONS = {"doBowTransformations", "func_178098_a"};

    /** Partial ticks of the frame being rendered; the redirects need them for the arm swing. */
    private static float renderingPartialTicks;

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"), require = 0)
    private void rememberPartialTicks(float partialTicks, CallbackInfo ci) {
        renderingPartialTicks = partialTicks;
    }

    /**
     * Handles every item transform call in the first-person pass. The vanilla world-swing path
     * already passes the live swing, so only the in-use actions change: 1.7 kept the arm swing
     * for a blocking sword and for food/drink, which is the "block hit" bob.
     */
    @Redirect(method = "renderItemInFirstPerson", require = 0, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V"))
    private void legacyFirstPersonTransform(ItemRenderer renderer, float equipProgress, float swingProgress) {
        Mc189Compat.call(renderer, TRANSFORM_FIRST_PERSON, new Class<?>[] {Float.TYPE, Float.TYPE},
            Float.valueOf(equipProgress), Float.valueOf(legacySwing(swingProgress)));
    }

    /** Lets vanilla build the bow pose, but on the 1.7 draw curve. */
    @Redirect(method = "renderItemInFirstPerson", require = 0, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/ItemRenderer;doBowTransformations(FLnet/minecraft/entity/player/EntityPlayer;)V"))
    private void legacyBowTransformations(ItemRenderer renderer, float partialTicks, EntityPlayer player) {
        if (bowAnimationEnabled) {
            Float compensated = Mc189Compat.legacyBowPartialTicks(player, partialTicks);
            if (compensated != null) {
                callBowTransformations(renderer, compensated.floatValue(), player);
                return;
            }
        }
        callBowTransformations(renderer, partialTicks, player);
    }

    private static void callBowTransformations(ItemRenderer renderer, float partialTicks, EntityPlayer player) {
        Mc189Compat.call(renderer, DO_BOW_TRANSFORMATIONS, new Class<?>[] {Float.TYPE, EntityPlayer.class},
            Float.valueOf(partialTicks), player);
    }

    private float legacySwing(float swingProgress) {
        if (!blockAnimationEnabled && !eatDrinkAnimationEnabled) {
            return swingProgress;
        }
        FirstPersonAnims.Action action = Mc189Compat.useActionKind(Mc189Compat.read(this, ITEM_TO_RENDER));
        if (action == FirstPersonAnims.Action.NONE || action == FirstPersonAnims.Action.BOW) {
            return swingProgress;
        }
        float armSwing = Mc189Compat.swingProgress(Mc189Compat.localPlayer(), renderingPartialTicks);
        return FirstPersonAnims.itemTransformSwing(action, swingProgress, armSwing,
            blockAnimationEnabled, eatDrinkAnimationEnabled);
    }

    @Inject(method = "renderItem", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemModelForEntity(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V"))
    public void renderItem(EntityLivingBase entity, ItemStack item, ItemCameraTransforms.TransformType transformType, CallbackInfo ci) {
        if (item == null) return;

        // 1.7 holds a cast rod closer to the camera than 1.8 does.
        if (fishingRodAnimationEnabled && isInHand(transformType) && Mc189Compat.isFishingRod(item)) {
            Mc189Compat.translate(0.0F, 0.0F, -0.35F);
        }

        if (!blockAnimationEnabled) return;
        if (!(item.getItem() instanceof ItemSword)) return;
        if (!(entity instanceof EntityPlayer)) return;
        if (transformType != ItemCameraTransforms.TransformType.THIRD_PERSON) return;

        Mc189Compat.rotate(-45.0F, 0.0F, 1.0F, 0.0F);
        Mc189Compat.rotate(-20.0F, 1.0F, 0.0F, 0.0F);
        Mc189Compat.rotate(-60.0F, 0.0F, 0.0F, 1.0F);
        Mc189Compat.translate(-0.04F, -0.04F, 0.0F);
    }

    @Inject(method = "doBlockTransformations", at = @At("HEAD"), cancellable = true, require = 0)
    public void swordBlockTransformations(CallbackInfo ci) {
        if (!blockAnimationEnabled) return;
        Mc189Compat.translate(-0.24F, 0.17F, 0.0F);
        Mc189Compat.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        Mc189Compat.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        Mc189Compat.rotate(60.0F, 0.0F, 1.0F, 0.0F);
        Mc189Compat.translate(0.0F, 0.18F, 0.00F);
        ci.cancel();
    }

    private static boolean isInHand(ItemCameraTransforms.TransformType transformType) {
        return transformType == ItemCameraTransforms.TransformType.FIRST_PERSON
            || transformType == ItemCameraTransforms.TransformType.THIRD_PERSON;
    }
}
