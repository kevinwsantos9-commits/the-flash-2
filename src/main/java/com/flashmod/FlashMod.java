package com.flashmod;

import com.flashmod.network.FlashBroadcastPayload;
import com.flashmod.network.FlashStatePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class FlashMod implements ModInitializer {
    public static final String MOD_ID = "flashmod";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(FlashStatePayload.ID, FlashStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FlashBroadcastPayload.ID, FlashBroadcastPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(FlashStatePayload.ID,
                (payload, context) -> FlashServer.update(context.player(), payload));

        ServerTickEvents.END_SERVER_TICK.register(FlashServer::tick);

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> FlashServer.remove(handler.getPlayer().getUuid()));
    }
}
