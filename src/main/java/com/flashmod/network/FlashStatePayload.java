package com.flashmod.network;

import com.flashmod.FlashMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente -> servidor: nível (0 = desligado), cor, arco-íris e se está atravessando paredes. */
public record FlashStatePayload(int level, int color, boolean rainbow, boolean phase) implements CustomPayload {

    public static final CustomPayload.Id<FlashStatePayload> ID =
            new CustomPayload.Id<>(Identifier.of(FlashMod.MOD_ID, "state"));

    public static final PacketCodec<RegistryByteBuf, FlashStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, FlashStatePayload::level,
            PacketCodecs.INTEGER, FlashStatePayload::color,
            PacketCodecs.BOOL, FlashStatePayload::rainbow,
            PacketCodecs.BOOL, FlashStatePayload::phase,
            FlashStatePayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
