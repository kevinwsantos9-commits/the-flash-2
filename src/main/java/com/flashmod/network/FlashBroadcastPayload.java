package com.flashmod.network;

import com.flashmod.FlashMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> clientes: estado de um jogador, para os outros jogadores verem os raios dele. */
public record FlashBroadcastPayload(int entityId, int level, int color, boolean rainbow, boolean phase)
        implements CustomPayload {

    public static final CustomPayload.Id<FlashBroadcastPayload> ID =
            new CustomPayload.Id<>(Identifier.of(FlashMod.MOD_ID, "broadcast"));

    public static final PacketCodec<RegistryByteBuf, FlashBroadcastPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, FlashBroadcastPayload::entityId,
            PacketCodecs.VAR_INT, FlashBroadcastPayload::level,
            PacketCodecs.INTEGER, FlashBroadcastPayload::color,
            PacketCodecs.BOOL, FlashBroadcastPayload::rainbow,
            PacketCodecs.BOOL, FlashBroadcastPayload::phase,
            FlashBroadcastPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
