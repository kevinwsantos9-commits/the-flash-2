package com.flashmod.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuração do jogador (cor do raio). Salva em config/flashmod.properties. */
@Environment(EnvType.CLIENT)
public final class FlashConfig {
    private FlashConfig() {}

    public static final int DEFAULT_COLOR = 0xFFD21F;

    public static int color = DEFAULT_COLOR;
    public static boolean rainbow = false;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("flashmod.properties");
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) return;
        try (InputStream in = Files.newInputStream(p)) {
            Properties props = new Properties();
            props.load(in);
            color = Integer.parseInt(props.getProperty("color", "FFD21F"), 16) & 0xFFFFFF;
            rainbow = Boolean.parseBoolean(props.getProperty("rainbow", "false"));
        } catch (IOException | NumberFormatException e) {
            color = DEFAULT_COLOR;
            rainbow = false;
        }
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("color", String.format("%06X", color & 0xFFFFFF));
        props.setProperty("rainbow", Boolean.toString(rainbow));
        try (OutputStream out = Files.newOutputStream(path())) {
            props.store(out, "Flash Mod");
        } catch (IOException ignored) {
            // Se não conseguir salvar, só perde a preferência.
        }
    }
}
