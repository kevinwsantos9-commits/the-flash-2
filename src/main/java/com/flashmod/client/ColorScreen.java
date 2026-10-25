package com.flashmod.client;

import com.flashmod.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.IntConsumer;
import java.util.regex.Pattern;

/** Menu para escolher a cor do raio: cores prontas, sliders RGB, código hexadecimal e modo arco-íris. */
@Environment(EnvType.CLIENT)
public class ColorScreen extends Screen {

    private static final String[] PRESET_NAMES = {
            "yellow", "red", "blue", "purple", "green", "white", "pink", "orange"
    };
    private static final int[] PRESET_COLORS = {
            0xFFD21F, 0xE5202A, 0x2FA4FF, 0xA64DFF, 0x35E06B, 0xFFFFFF, 0xFF5FC8, 0xFF7A18
    };
    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");

    private ChannelSlider red;
    private ChannelSlider green;
    private ChannelSlider blue;
    private TextFieldWidget hex;
    private ButtonWidget rainbowButton;
    private boolean updating;

    private int top;
    private int rightX;

    public ColorScreen() {
        super(Text.translatable("screen.flashmod.title"));
    }

    @Override
    protected void init() {
        top = Math.max(26, this.height / 2 - 80);
        int leftX = this.width / 2 - 160;
        rightX = this.width / 2 + 10;
        int color = FlashConfig.color;

        // Coluna da esquerda: sliders, hex, arco-íris, concluir.
        red = addDrawableChild(new ChannelSlider(leftX, top, 150, 20,
                Text.literal("R").formatted(Formatting.RED), (color >> 16) & 0xFF, v -> onSliders()));
        green = addDrawableChild(new ChannelSlider(leftX, top + 24, 150, 20,
                Text.literal("G").formatted(Formatting.GREEN), (color >> 8) & 0xFF, v -> onSliders()));
        blue = addDrawableChild(new ChannelSlider(leftX, top + 48, 150, 20,
                Text.literal("B").formatted(Formatting.BLUE), color & 0xFF, v -> onSliders()));

        hex = new TextFieldWidget(this.textRenderer, leftX, top + 76, 70, 20, Text.literal("Hex"));
        hex.setMaxLength(7);
        hex.setText(String.format("#%06X", color));
        hex.setChangedListener(this::onHexChanged);
        addDrawableChild(hex);

        rainbowButton = addDrawableChild(ButtonWidget.builder(rainbowLabel(), b -> {
            FlashConfig.rainbow = !FlashConfig.rainbow;
            b.setMessage(rainbowLabel());
        }).dimensions(leftX + 74, top + 76, 76, 20).build());

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close())
                .dimensions(leftX, top + 108, 150, 20).build());

        // Coluna da direita: cores prontas (2 colunas x 4 linhas).
        for (int i = 0; i < PRESET_NAMES.length; i++) {
            final int rgb = PRESET_COLORS[i];
            int col = i % 2;
            int row = i / 2;
            Text name = Text.translatable("color.flashmod." + PRESET_NAMES[i]).styled(s -> s.withColor(rgb));
            addDrawableChild(ButtonWidget.builder(name, b -> applyColor(rgb, true))
                    .dimensions(rightX + col * 78, top + 64 + row * 22, 72, 20).build());
        }
    }

    private Text rainbowLabel() {
        return Text.translatable("gui.flashmod.rainbow").append(": ").append(ScreenTexts.onOrOff(FlashConfig.rainbow));
    }

    private void onSliders() {
        applyColor((red.get() << 16) | (green.get() << 8) | blue.get(), true);
    }

    private void onHexChanged(String text) {
        if (updating) return;
        if (!HEX.matcher(text).matches()) return;
        String digits = text.startsWith("#") ? text.substring(1) : text;
        applyColor(Integer.parseInt(digits, 16), false);
    }

    /** Aplica a cor escolhida (desliga o arco-íris) e atualiza os controles. */
    private void applyColor(int rgb, boolean updateHex) {
        FlashConfig.color = rgb & 0xFFFFFF;
        FlashConfig.rainbow = false;
        red.set((rgb >> 16) & 0xFF);
        green.set((rgb >> 8) & 0xFF);
        blue.set(rgb & 0xFF);
        if (updateHex) {
            boolean prev = updating;
            updating = true;
            hex.setText(String.format("#%06X", FlashConfig.color));
            updating = prev;
        }
        rainbowButton.setMessage(rainbowLabel());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top - 16, 0xFFFFFF);
        drawPreview(ctx);
    }

    /** Prévia: um raio desenhado com brilho (cor clara) e núcleo (cor escolhida). */
    private void drawPreview(DrawContext ctx) {
        int x = rightX;
        int y = top;
        ctx.fill(x, y, x + 150, y + 56, 0xCC000000);

        int c = FlashConfig.rainbow ? ColorUtil.rainbow(System.currentTimeMillis() / 50.0) : FlashConfig.color;
        int glow = 0x55000000 | ColorUtil.lighten(c, 0.35f);
        int core = 0xFF000000 | ColorUtil.lighten(c, 0.0f);

        int[][] pts = {
                {x + 95, y + 4}, {x + 70, y + 26}, {x + 86, y + 28}, {x + 58, y + 52}
        };
        for (int i = 0; i < pts.length - 1; i++) {
            line(ctx, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], 7, glow);
        }
        for (int i = 0; i < pts.length - 1; i++) {
            line(ctx, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], 2, core);
        }
    }

    private static void line(DrawContext ctx, int x1, int y1, int x2, int y2, int th, int argb) {
        int steps = Math.max(1, Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)));
        for (int i = 0; i <= steps; i++) {
            int px = x1 + (x2 - x1) * i / steps - th / 2;
            int py = y1 + (y2 - y1) * i / steps - th / 2;
            ctx.fill(px, py, px + th, py + th, argb);
        }
    }

    /** Salva ao fechar (Esc ou botão Concluir). */
    @Override
    public void removed() {
        FlashConfig.save();
    }

    /** Slider de 0 a 255 para um canal de cor. */
    private static class ChannelSlider extends SliderWidget {
        private final Text label;
        private final IntConsumer onChange;

        ChannelSlider(int x, int y, int w, int h, Text label, int value, IntConsumer onChange) {
            super(x, y, w, h, Text.empty(), value / 255.0);
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        int get() {
            return (int) Math.round(this.value * 255.0);
        }

        void set(int v) {
            this.value = v / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Text.empty().append(label).append(": " + get()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(get());
        }
    }
}
