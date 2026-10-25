package com.flashmod.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Estado do poder no cliente + números de balanceamento (fáceis de ajustar). */
@Environment(EnvType.CLIENT)
public final class FlashClientState {
    private FlashClientState() {}

    public static final int MAX_LEVEL = 20;
    /** A partir deste nível o jogador anda sobre a água. */
    public static final int WATER_LEVEL = 5;
    /** A partir deste nível aparecem as faíscas de velocidade. */
    public static final int SPARKS_LEVEL = 10;

    /** Velocidade em blocos por tick (20 ticks = 1 segundo) no nível 1 e quanto cada nível soma. */
    public static final double BASE_SPEED = 0.35;
    public static final double STEP_SPEED = 0.17;

    public static double speedFor(int level) {
        return BASE_SPEED + STEP_SPEED * (level - 1);
    }

    /** Velocidade de subida ao correr na parede. */
    public static double climbFor(int level) {
        return Math.min(0.95, 0.35 + level * 0.03);
    }

    /** 0 = desligado, 1..20 = nível ativo. */
    public static int level = 0;
    /** Nível escolhido para quando ligar. */
    public static int selectedLevel = 1;

    /** O jogador pediu para atravessar paredes. */
    public static boolean phaseWanted = false;
    /** Está atravessando de fato (continua ligado até sair de dentro dos blocos). */
    public static boolean phaseActive = false;

    static boolean waterWalking = false;

    // Controle de sincronização com o servidor.
    static int sentLevel = -1;
    static int sentColor = -1;
    static boolean sentRainbow = false;
    static boolean sentPhase = false;
    static int sentTimer = 0;

    public static void reset() {
        level = 0;
        phaseWanted = false;
        phaseActive = false;
        waterWalking = false;
        sentLevel = -1;
        sentTimer = 0;
    }
}
