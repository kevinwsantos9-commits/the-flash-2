package com.flashmod.mixin;

import com.flashmod.FlashPhase;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O jogo faz "noClip = isSpectator()" no começo do tick do jogador.
 * Aqui ligamos o noClip logo depois disso e antes do movimento, para atravessar blocos.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;tick()V"))
    private void flashmod$phase(CallbackInfo ci) {
        boolean phasing = this.getWorld().isClient
                ? FlashPhase.CLIENT_NOCLIP.contains(this.getUuid())
                : FlashPhase.SERVER_PHASING.contains(this.getUuid());
        if (phasing) {
            this.noClip = true;
        }
    }
}
