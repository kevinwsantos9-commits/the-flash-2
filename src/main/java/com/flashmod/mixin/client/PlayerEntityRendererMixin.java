package com.flashmod.mixin.client;

import com.flashmod.client.FlashVisuals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/** Faz o modelo do jogador vibrar (tremer) enquanto atravessa paredes ou corre muito rápido. */
@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    private void flashmod$vibrate(AbstractClientPlayerEntity player, float yaw, float tickDelta,
                                  MatrixStack matrices, VertexConsumerProvider consumers, int light,
                                  CallbackInfo ci) {
        double amp = FlashVisuals.vibrationAmplitude(player);
        if (amp <= 0) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        matrices.translate(
                (r.nextDouble() - 0.5) * 2.0 * amp,
                (r.nextDouble() - 0.5) * amp,
                (r.nextDouble() - 0.5) * 2.0 * amp);
    }
}
