package com.flashmod;

import com.flashmod.network.FlashBroadcastPayload;
import com.flashmod.network.FlashStatePayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lado do servidor: guarda o estado de cada jogador, avisa os outros jogadores
 * (para eles verem os raios) e libera o movimento de quem atravessa paredes.
 */
public final class FlashServer {
    private FlashServer() {}

    public static final int MAX_LEVEL = 20;

    private static final class State {
        int level;
        int color;
        boolean rainbow;
        boolean phase;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static void update(ServerPlayerEntity player, FlashStatePayload payload) {
        State s = STATES.computeIfAbsent(player.getUuid(), id -> new State());
        boolean wasOff = s.level == 0;

        s.level = MathHelper.clamp(payload.level(), 0, MAX_LEVEL);
        s.color = payload.color() & 0xFFFFFF;
        s.rainbow = payload.rainbow();
        s.phase = payload.phase() && s.level > 0;

        if (s.phase) {
            FlashPhase.SERVER_PHASING.add(player.getUuid());
        } else {
            FlashPhase.SERVER_PHASING.remove(player.getUuid());
        }

        if (wasOff && s.level > 0) {
            player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.4f, 1.8f);
        }

        broadcast(player, s);
    }

    private static void broadcast(ServerPlayerEntity player, State s) {
        FlashBroadcastPayload out = new FlashBroadcastPayload(
                player.getId(), s.level, s.color, s.rainbow, s.phase);
        Set<ServerPlayerEntity> targets = new HashSet<>(PlayerLookup.tracking(player));
        targets.add(player);
        for (ServerPlayerEntity target : targets) {
            ServerPlayNetworking.send(target, out);
        }
    }

    public static void remove(UUID id) {
        STATES.remove(id);
        FlashPhase.SERVER_PHASING.remove(id);
    }

    public static void tick(MinecraftServer server) {
        long time = server.getTicks();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            State s = STATES.get(player.getUuid());
            if (s == null || s.level == 0 || player.isSpectator()) continue;

            // Sem dano de queda enquanto a Força da Velocidade está ligada.
            player.fallDistance = 0;

            // Estalo de raio baixinho enquanto corre.
            if (s.level >= 3 && time % 6 == 0) {
                float pitch = Math.min(2.0f, 1.5f + s.level * 0.03f);
                player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.06f, pitch);
            }
        }
    }
}
