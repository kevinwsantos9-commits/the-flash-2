package com.flashmod;

import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

/** Utilidades de cor usadas no cliente e no servidor. */
public final class ColorUtil {
    private ColorUtil() {}

    /** Cor arco-íris (RGB) a partir de um contador de ticks. */
    public static int rainbow(double ticks) {
        float hue = (float) ((ticks % 80.0) / 80.0);
        return MathHelper.hsvToRgb(hue, 1.0f, 1.0f);
    }

    /** Mistura a cor com branco (t = 0 mantém, t = 1 vira branco). Usado no brilho do raio. */
    public static int lighten(int rgb, float t) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = (int) (r + (255 - r) * t);
        g = (int) (g + (255 - g) * t);
        b = (int) (b + (255 - b) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Converte RGB inteiro em vetor 0..1 (formato das partículas de poeira). */
    public static Vector3f toVector(int rgb) {
        return new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f
        );
    }
}
