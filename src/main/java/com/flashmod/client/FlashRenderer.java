package com.flashmod.client;

import com.flashmod.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Desenha raios de verdade (linhas tortas e brilhantes, como os do Flash) em volta e atrás do jogador,
 * e faíscas de velocidade a partir do nível 10. Usa a mesma camada de render dos raios do jogo.
 */
@Environment(EnvType.CLIENT)
public final class FlashRenderer {
    private FlashRenderer() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(FlashRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        ClientWorld world = ctx.world();
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (world == null || matrices == null || consumers == null) return;

        boolean any = false;
        for (AbstractClientPlayerEntity p : world.getPlayers()) {
            if (FlashVisuals.of(p) != null) {
                any = true;
                break;
            }
        }
        if (!any) return;

        float td = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());

        for (AbstractClientPlayerEntity p : world.getPlayers()) {
            FlashVisuals.Visual v = FlashVisuals.of(p);
            if (v != null) {
                drawPlayer(vc, m, cam, p, v, world.getTime(), td);
            }
        }
    }

    private static void drawPlayer(VertexConsumer vc, Matrix4f m, Vec3d cam, AbstractClientPlayerEntity p,
                                   FlashVisuals.Visual v, long worldTime, float td) {
        double px = MathHelper.lerp(td, p.prevX, p.getX());
        double py = MathHelper.lerp(td, p.prevY, p.getY());
        double pz = MathHelper.lerp(td, p.prevZ, p.getZ());

        // Direção "para trás" (oposta ao movimento; parado, oposta ao olhar).
        double mx = p.getX() - p.prevX;
        double mz = p.getZ() - p.prevZ;
        double sp = Math.sqrt(mx * mx + mz * mz);
        double bx;
        double bz;
        if (sp > 0.02) {
            bx = -mx / sp;
            bz = -mz / sp;
        } else {
            float yaw = p.getYaw() * 0.017453292F;
            bx = Math.sin(yaw);
            bz = -Math.cos(yaw);
        }

        int level = v.level();
        double time = worldTime + td;
        // O formato dos raios muda a cada 2 ticks (crepitar).
        long bucket = worldTime / 2;
        Random rnd = new Random(p.getId() * 341873128712L + bucket * 132897987541L);
        int base = v.rainbow() ? ColorUtil.rainbow(time * 2.0) : v.color();

        // Raios curtos em volta do corpo: uns enrolam no corpo, outros seguem para trás.
        int nBolts = 3 + level / 2;
        for (int i = 0; i < nBolts; i++) {
            int color = v.rainbow() ? ColorUtil.rainbow(time * 2.0 + i * 9) : base;
            double ang = rnd.nextDouble() * Math.PI * 2;
            double h = 0.1 + rnd.nextDouble() * 1.6;
            double ox = px + Math.cos(ang) * 0.3;
            double oy = py + h;
            double oz = pz + Math.sin(ang) * 0.3;

            double dx;
            double dy;
            double dz;
            if (i % 2 == 0) {
                dx = -Math.sin(ang) * 0.8 + bx * 0.4;
                dz = Math.cos(ang) * 0.8 + bz * 0.4;
                dy = (rnd.nextDouble() - 0.5) * 0.5;
            } else {
                dx = bx * 0.9 + Math.cos(ang) * 0.3;
                dz = bz * 0.9 + Math.sin(ang) * 0.3;
                dy = (rnd.nextDouble() - 0.5) * 0.4;
            }
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-6) continue;
            dx /= len;
            dy /= len;
            dz /= len;

            double length = 0.7 + rnd.nextDouble() * 0.9 + level * 0.07 + (i % 2 == 1 ? sp * 2.0 : 0.0);
            bolt(vc, m, cam, rnd, ox, oy, oz, dx, dy, dz, length, 6, 0.12, 0.10f, 0.022f, color, true);
        }

        // Raios longos que ficam para trás, como o rastro do Flash.
        int longBolts = 1 + level / 6;
        for (int i = 0; i < longBolts; i++) {
            int color = v.rainbow() ? ColorUtil.rainbow(time * 2.0 + i * 15) : base;
            double ox = px + bx * 0.25 + (rnd.nextDouble() - 0.5) * 0.3;
            double oy = py + 0.2 + rnd.nextDouble() * 1.4;
            double oz = pz + bz * 0.25 + (rnd.nextDouble() - 0.5) * 0.3;

            double dx = bx + (rnd.nextDouble() - 0.5) * 0.35;
            double dy = (rnd.nextDouble() - 0.5) * 0.25;
            double dz = bz + (rnd.nextDouble() - 0.5) * 0.35;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-6) continue;
            dx /= len;
            dy /= len;
            dz /= len;

            double length = 2.0 + level * 0.3 + sp * 3.0;
            bolt(vc, m, cam, rnd, ox, oy, oz, dx, dy, dz, length, 10, 0.2, 0.16f, 0.035f, color, true);
        }

        // Faíscas de velocidade (nível 10 ou mais): riscos brilhantes voando para trás em volta do jogador.
        if (level >= FlashClientState.SPARKS_LEVEL) {
            int n = 6 + (level - FlashClientState.SPARKS_LEVEL) * 2;
            for (int i = 0; i < n; i++) {
                double life = 5 + (i % 4);
                double t = (time + i * 3.7) / life;
                long cycle = (long) Math.floor(t);
                double f = t - cycle;
                Random sr = new Random(p.getId() * 7919L + i * 104729L + cycle * 15485863L);

                double ang = sr.nextDouble() * Math.PI * 2;
                double rad = 0.5 + sr.nextDouble() * 0.9;
                double hh = 0.1 + sr.nextDouble() * 1.9;

                double hx = px + Math.cos(ang) * rad + bx * f * 1.6;
                double hy = py + hh;
                double hz = pz + Math.sin(ang) * rad + bz * f * 1.6;
                double len = 0.3 + level * 0.03 + sp * 0.8;
                double tx = hx - bx * len;
                double tz = hz - bz * len;

                int color = v.rainbow() ? ColorUtil.rainbow(time * 2.0 + i * 5) : base;
                int bright = ColorUtil.lighten(color, 0.55f);
                float alpha = (float) (1.0 - f);

                Vec3d head = new Vec3d(hx - cam.x, hy - cam.y, hz - cam.z);
                Vec3d tail = new Vec3d(tx - cam.x, hy - cam.y, tz - cam.z);
                segment(vc, m, tail, head, 0.01f, 0.05f, color, 0.25f * alpha, 0.02);
                segment(vc, m, tail, head, 0.004f, 0.016f, bright, alpha, -0.02);
            }
        }
    }

    /** Um raio torto: caminho com desvios aleatórios, brilho largo + núcleo claro, e possível ramificação. */
    private static void bolt(VertexConsumer vc, Matrix4f m, Vec3d cam, Random rnd,
                             double x, double y, double z, double dx, double dy, double dz,
                             double length, int segs, double jitter, float glowW, float coreW,
                             int rgb, boolean fork) {
        double step = length / segs;
        double[] wx = new double[segs + 1];
        double[] wy = new double[segs + 1];
        double[] wz = new double[segs + 1];
        wx[0] = x;
        wy[0] = y;
        wz[0] = z;
        for (int i = 1; i <= segs; i++) {
            wx[i] = wx[i - 1] + dx * step + (rnd.nextDouble() - 0.5) * 2.0 * jitter;
            wy[i] = wy[i - 1] + dy * step + (rnd.nextDouble() - 0.5) * 2.0 * jitter;
            wz[i] = wz[i - 1] + dz * step + (rnd.nextDouble() - 0.5) * 2.0 * jitter;
        }

        int core = ColorUtil.lighten(rgb, 0.8f);
        for (int i = 0; i < segs; i++) {
            // O raio afina até a ponta.
            float t0 = 1.0f - (float) i / segs * 0.75f;
            float t1 = 1.0f - (float) (i + 1) / segs * 0.75f;
            Vec3d a = new Vec3d(wx[i] - cam.x, wy[i] - cam.y, wz[i] - cam.z);
            Vec3d b = new Vec3d(wx[i + 1] - cam.x, wy[i + 1] - cam.y, wz[i + 1] - cam.z);
            segment(vc, m, a, b, glowW * t0, glowW * t1, rgb, 0.30f, 0.02);
            segment(vc, m, a, b, coreW * t0, coreW * t1, core, 1.0f, -0.02);
        }

        if (fork && segs >= 4) {
            int k = segs / 2;
            double fx = dx + (rnd.nextDouble() - 0.5) * 1.6;
            double fy = dy + (rnd.nextDouble() - 0.5) * 1.6;
            double fz = dz + (rnd.nextDouble() - 0.5) * 1.6;
            double fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
            if (fl > 1.0E-6) {
                bolt(vc, m, cam, rnd, wx[k], wy[k], wz[k], fx / fl, fy / fl, fz / fl,
                        length * 0.45, Math.max(3, segs / 2), jitter, glowW * 0.7f, coreW * 0.7f, rgb, false);
            }
        }
    }

    /**
     * Um pedaço de raio desenhado como uma faixa virada para a câmera.
     * a e b são relativos à câmera. bias > 0 empurra para longe da câmera, bias < 0 para perto
     * (o núcleo fica na frente do brilho, evitando z-fighting).
     */
    private static void segment(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, float wa, float wb,
                                int rgb, float alpha, double bias) {
        Vec3d mid = a.add(b).multiply(0.5);
        double dist = mid.length();
        if (dist < 0.5) return; // perto demais da câmera: não desenha

        Vec3d dir = b.subtract(a);
        if (dir.lengthSquared() < 1.0E-10) return;
        Vec3d side = dir.crossProduct(mid);
        double sl = side.length();
        if (sl < 1.0E-9) return;
        Vec3d sideN = side.multiply(1.0 / sl);
        Vec3d shift = mid.multiply(bias / dist);
        Vec3d dn = dir.normalize();
        double ext = 0.5 * Math.max(wa, wb);

        Vec3d a2 = a.subtract(dn.multiply(ext)).add(shift);
        Vec3d b2 = b.add(dn.multiply(ext)).add(shift);
        Vec3d p0 = a2.subtract(sideN.multiply(wa));
        Vec3d p1 = a2.add(sideN.multiply(wa));
        Vec3d p2 = b2.add(sideN.multiply(wb));
        Vec3d p3 = b2.subtract(sideN.multiply(wb));

        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float bl = (rgb & 0xFF) / 255.0f;

        // Frente e verso (a camada de render descarta faces de costas).
        vertex(vc, m, p0, r, g, bl, alpha);
        vertex(vc, m, p1, r, g, bl, alpha);
        vertex(vc, m, p2, r, g, bl, alpha);
        vertex(vc, m, p3, r, g, bl, alpha);
        vertex(vc, m, p3, r, g, bl, alpha);
        vertex(vc, m, p2, r, g, bl, alpha);
        vertex(vc, m, p1, r, g, bl, alpha);
        vertex(vc, m, p0, r, g, bl, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3d p, float r, float g, float b, float a) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a);
    }
}
