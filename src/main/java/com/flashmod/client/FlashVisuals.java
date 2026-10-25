package com.flashmod.client;

import com.flashmod.network.FlashBroadcastPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.HashMap;
import java.util.Map;

/** Estado visual de cada jogador (você e os outros) usado para desenhar raios e vibração. */
@Environment(EnvType.CLIENT)
public final class FlashVisuals {
    private FlashVisuals() {}

    public record Visual(int level, int color, boolean rainbow, boolean phase) {}

    private static final class Remote {
        int level;
        int color;
        boolean rainbow;
        boolean phase;
        long tick;
    }

    private static final Map<Integer, Remote> REMOTE = new HashMap<>();

    public static void update(FlashBroadcastPayload p, long worldTime) {
        if (p.level() <= 0) {
            REMOTE.remove(p.entityId());
            return;
        }
        Remote r = REMOTE.computeIfAbsent(p.entityId(), id -> new Remote());
        r.level = p.level();
        r.color = p.color();
        r.rainbow = p.rainbow();
        r.phase = p.phase();
        r.tick = worldTime;
    }

    public static void clear() {
        REMOTE.clear();
    }

    /** Estado visual do jogador, ou null se ele não está com o poder ligado. */
    public static Visual of(AbstractClientPlayerEntity p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (p == mc.player) {
            if (FlashClientState.level <= 0) return null;
            return new Visual(FlashClientState.level, FlashConfig.color, FlashConfig.rainbow,
                    FlashClientState.phaseActive);
        }
        Remote r = REMOTE.get(p.getId());
        if (r == null || r.level <= 0) return null;
        if (p.getWorld().getTime() - r.tick > 140) return null;
        return new Visual(r.level, r.color, r.rainbow, r.phase);
    }

    /** Intensidade da vibração do modelo do jogador. */
    public static double vibrationAmplitude(AbstractClientPlayerEntity p) {
        Visual v = of(p);
        if (v == null) return 0;
        if (v.phase()) return 0.05;
        if (v.level() >= FlashClientState.SPARKS_LEVEL) return 0.012;
        return 0;
    }
}
