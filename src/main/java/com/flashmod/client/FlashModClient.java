package com.flashmod.client;

import com.flashmod.FlashPhase;
import com.flashmod.network.FlashBroadcastPayload;
import com.flashmod.network.FlashStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class FlashModClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static KeyBinding phaseKey;
    private static KeyBinding upKey;
    private static KeyBinding downKey;
    private static KeyBinding colorKey;

    private static int holdUp;
    private static int holdDown;

    @Override
    public void onInitializeClient() {
        FlashConfig.load();

        toggleKey = register("key.flashmod.toggle", GLFW.GLFW_KEY_V);
        phaseKey = register("key.flashmod.phase", GLFW.GLFW_KEY_C);
        upKey = register("key.flashmod.up", GLFW.GLFW_KEY_B);
        downKey = register("key.flashmod.down", GLFW.GLFW_KEY_N);
        colorKey = register("key.flashmod.color", GLFW.GLFW_KEY_K);

        FlashRenderer.init();
        ClientTickEvents.START_CLIENT_TICK.register(FlashModClient::tick);
        HudRenderCallback.EVENT.register(FlashHud::render);

        ClientPlayNetworking.registerGlobalReceiver(FlashBroadcastPayload.ID, (payload, context) -> {
            ClientWorld world = context.client().world;
            FlashVisuals.update(payload, world != null ? world.getTime() : 0L);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetAll());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetAll());
    }

    private static KeyBinding register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(name, InputUtil.Type.KEYSYM, key, "key.categories.flashmod"));
    }

    private static void resetAll() {
        FlashClientState.reset();
        FlashVisuals.clear();
        FlashPhase.CLIENT_NOCLIP.clear();
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return;

        while (toggleKey.wasPressed()) toggle(player, world);
        while (phaseKey.wasPressed()) togglePhase(player);
        while (upKey.wasPressed()) changeLevel(player, 1);
        while (downKey.wasPressed()) changeLevel(player, -1);
        holdUp = repeat(upKey, holdUp, player, 1);
        holdDown = repeat(downKey, holdDown, player, -1);
        while (colorKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new ColorScreen());
            }
        }

        if (FlashClientState.level > 0) {
            active(player, world);
        } else {
            idle(player);
        }
        sync();
    }

    // ---------------------------------------------------------------- teclas

    private static void toggle(ClientPlayerEntity player, ClientWorld world) {
        if (FlashClientState.level > 0) {
            // Não deixa desligar com o corpo dentro de blocos (senão sufoca).
            if (FlashClientState.phaseActive && hasCollision(world, player, player.getBoundingBox())) {
                player.sendMessage(Text.translatable("msg.flashmod.inside"), true);
                return;
            }
            FlashClientState.level = 0;
        } else {
            FlashClientState.level = FlashClientState.selectedLevel;
        }
    }

    private static void togglePhase(ClientPlayerEntity player) {
        FlashClientState.phaseWanted = !FlashClientState.phaseWanted;
        player.sendMessage(Text.translatable("msg.flashmod.phase",
                ScreenTexts.onOrOff(FlashClientState.phaseWanted)), true);
    }

    private static void changeLevel(ClientPlayerEntity player, int delta) {
        int n = MathHelper.clamp(FlashClientState.selectedLevel + delta, 1, FlashClientState.MAX_LEVEL);
        FlashClientState.selectedLevel = n;
        if (FlashClientState.level > 0) {
            FlashClientState.level = n;
        }
        player.sendMessage(Text.translatable("msg.flashmod.level", n, FlashClientState.MAX_LEVEL), true);
    }

    /** Segurar a tecla muda o nível repetidamente. */
    private static int repeat(KeyBinding key, int held, ClientPlayerEntity player, int delta) {
        if (!key.isPressed()) return 0;
        held++;
        if (held > 10 && held % 3 == 0) {
            changeLevel(player, delta);
        }
        return held;
    }

    // ---------------------------------------------------------------- tick

    private static void idle(ClientPlayerEntity player) {
        releaseWater(player);
        FlashClientState.phaseActive = false;
        FlashPhase.CLIENT_NOCLIP.remove(player.getUuid());
    }

    private static void active(ClientPlayerEntity player, ClientWorld world) {
        int level = FlashClientState.level;

        if (player.hasVehicle() || player.isFallFlying() || player.getAbilities().flying || player.isSpectator()) {
            releaseWater(player);
            FlashPhase.CLIENT_NOCLIP.remove(player.getUuid());
            return;
        }

        // Direção desejada, na velocidade do nível.
        float forward = player.input.movementForward;
        float side = player.input.movementSideways;
        boolean moving = forward != 0 || side != 0;
        double vx = 0;
        double vz = 0;
        if (moving) {
            double len = Math.sqrt(forward * forward + side * side);
            double nf = forward / len;
            double ns = side / len;
            double speed = FlashClientState.speedFor(level);
            float yawRad = player.getYaw() * 0.017453292F;
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            vx = (ns * cos - nf * sin) * speed;
            vz = (nf * cos + ns * sin) * speed;
        }

        // Atravessar paredes: só liga o noClip quando há bloco no caminho ou o corpo já está dentro de um.
        boolean inside = hasCollision(world, player, player.getBoundingBox());
        if (FlashClientState.phaseWanted) {
            FlashClientState.phaseActive = true;
        } else if (FlashClientState.phaseActive && !inside) {
            FlashClientState.phaseActive = false;
        }

        boolean noClip = false;
        if (FlashClientState.phaseActive) {
            boolean ahead = moving && hasCollision(world, player, player.getBoundingBox().offset(vx, 0, vz));
            noClip = inside || ahead;
        }
        if (noClip) {
            FlashPhase.CLIENT_NOCLIP.add(player.getUuid());
        } else {
            FlashPhase.CLIENT_NOCLIP.remove(player.getUuid());
        }

        handleWater(player, world, level, noClip);

        if (noClip) {
            // Dentro dos blocos não há gravidade: pular sobe, agachar desce, senão fica parado na altura.
            double vy = player.input.jumping ? 0.5 : (player.input.sneaking ? -0.5 : 0.0);
            player.fallDistance = 0;
            player.setVelocity(vx, vy, vz);
        } else if (moving) {
            double vy = player.getVelocity().y;
            // Correr na parede: bateu em um bloco andando para frente -> sobe.
            if (player.horizontalCollision && forward > 0) {
                vy = FlashClientState.climbFor(level);
                player.fallDistance = 0;
            }
            player.setVelocity(vx, vy, vz);
        }
    }

    /** Andar sobre a água a partir do nível 5 (agachar para afundar, pular para sair). */
    private static void handleWater(ClientPlayerEntity player, ClientWorld world, int level, boolean noClip) {
        boolean stand = false;

        if (!noClip && level >= FlashClientState.WATER_LEVEL
                && !player.input.sneaking && player.getVelocity().y <= 0.05) {
            BlockPos base = player.getBlockPos();
            for (int dy = 0; dy >= -1 && !stand; dy--) {
                BlockPos p = base.add(0, dy, 0);
                FluidState fs = world.getFluidState(p);
                if (fs.isIn(FluidTags.WATER) && world.getFluidState(p.up()).isEmpty()) {
                    double surface = p.getY() + fs.getHeight(world, p);
                    double y = player.getY();
                    if (y <= surface + 0.25 && y >= surface - 1.0) {
                        if (Math.abs(y - surface) > 0.01) {
                            player.setPosition(player.getX(), surface, player.getZ());
                        }
                        stand = true;
                    }
                }
            }
        }

        if (stand) {
            player.setNoGravity(true);
            player.setOnGround(true);
            player.fallDistance = 0;
            Vec3d v = player.getVelocity();
            player.setVelocity(v.x, 0.0, v.z);
            FlashClientState.waterWalking = true;
        } else {
            releaseWater(player);
        }
    }

    private static void releaseWater(ClientPlayerEntity player) {
        if (FlashClientState.waterWalking) {
            player.setNoGravity(false);
            FlashClientState.waterWalking = false;
        }
    }

    private static boolean hasCollision(ClientWorld world, Entity entity, Box box) {
        return world.getBlockCollisions(entity, box).iterator().hasNext();
    }

    // ---------------------------------------------------------------- rede

    /** Envia o estado ao servidor quando muda (e a cada 3 s, por segurança). */
    private static void sync() {
        int level = FlashClientState.level;
        int color = FlashConfig.color;
        boolean rainbow = FlashConfig.rainbow;
        boolean phase = FlashClientState.phaseActive;

        FlashClientState.sentTimer++;
        boolean changed = level != FlashClientState.sentLevel
                || color != FlashClientState.sentColor
                || rainbow != FlashClientState.sentRainbow
                || phase != FlashClientState.sentPhase;
        if (!changed && FlashClientState.sentTimer < 60) return;

        // Servidor sem o mod: não envia nada.
        if (!ClientPlayNetworking.canSend(FlashStatePayload.ID)) return;

        ClientPlayNetworking.send(new FlashStatePayload(level, color, rainbow, phase));
        FlashClientState.sentLevel = level;
        FlashClientState.sentColor = color;
        FlashClientState.sentRainbow = rainbow;
        FlashClientState.sentPhase = phase;
        FlashClientState.sentTimer = 0;
    }
}
