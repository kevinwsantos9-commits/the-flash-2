package com.flashmod.client;

import com.flashmod.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

/** Barra de velocidade (20 quadradinhos) e linhas de velocidade nas bordas da tela. */
@Environment(EnvType.CLIENT)
public final class FlashHud {
    private FlashHud() {}

    private static int colorAt(MinecraftClient mc, int index) {
        if (FlashConfig.rainbow) {
            long t = mc.world != null ? mc.world.getTime() : 0;
            return ColorUtil.rainbow(t * 2.0 + index * 4);
        }
        return FlashConfig.color;
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;

        int level = FlashClientState.level;
        if (level <= 0) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        // Linhas de velocidade nas laterais, na cor do raio (a partir do nível 5).
        if (level >= 5) {
            Random r = new Random(mc.world.getTime());
            int lines = 4 + level;
            int color = colorAt(mc, 0);
            for (int i = 0; i < lines; i++) {
                int ly = r.nextInt(h);
                int len = 30 + r.nextInt(40 + level * 5);
                int lx = r.nextBoolean() ? 0 : w - len;
                ctx.fill(lx, ly, lx + len, ly + 1, 0x55000000 | color);
            }
        }

        // Barra: 20 quadrados, cada um vale 1 ponto de velocidade.
        int sq = 7;
        int gap = 2;
        int x = 10;
        int y = 24;

        Text label = Text.translatable("hud.flashmod.speed", level, FlashClientState.MAX_LEVEL);
        if (FlashClientState.phaseActive) {
            label = Text.empty().append(label).append("   ")
                    .append(Text.translatable("hud.flashmod.phase").formatted(Formatting.AQUA));
        }
        ctx.drawText(mc.textRenderer, label, x, y - 12, 0xFFFFFFFF, true);

        for (int i = 0; i < FlashClientState.MAX_LEVEL; i++) {
            int sx = x + i * (sq + gap);
            boolean on = i < level;
            ctx.fill(sx - 1, y - 1, sx + sq + 1, y + sq + 1, 0xAA000000);
            ctx.fill(sx, y, sx + sq, y + sq, on ? (0xFF000000 | colorAt(mc, i)) : 0xFF2A2A2A);
            if (on) {
                ctx.fill(sx, y, sx + sq, y + 1, 0x66FFFFFF);
            }
        }

        // Marcas dos poderes que liberam em cada nível.
        drawMark(ctx, mc, x, y + sq + 3, FlashClientState.WATER_LEVEL, sq, gap, level,
                Text.translatable("hud.flashmod.water"));
        drawMark(ctx, mc, x, y + sq + 3, FlashClientState.SPARKS_LEVEL, sq, gap, level,
                Text.translatable("hud.flashmod.sparks"));
    }

    private static void drawMark(DrawContext ctx, MinecraftClient mc, int x, int y, int markLevel,
                                 int sq, int gap, int level, Text text) {
        int sx = x + (markLevel - 1) * (sq + gap);
        boolean unlocked = level >= markLevel;
        ctx.fill(sx + 2, y - 2, sx + sq - 2, y - 1, unlocked ? 0xFFFFFFFF : 0xFF777777);
        ctx.drawText(mc.textRenderer, text, sx, y, unlocked ? 0xFFFFFFFF : 0xFF888888, false);
    }
}
