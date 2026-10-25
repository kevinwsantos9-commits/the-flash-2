package com.flashmod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quem está atravessando paredes.
 * SERVER_PHASING: lado do servidor (libera a checagem de movimento do servidor).
 * CLIENT_NOCLIP: lado do cliente (só no tick em que o jogador realmente precisa atravessar um bloco).
 */
public final class FlashPhase {
    private FlashPhase() {}

    public static final Set<UUID> SERVER_PHASING = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> CLIENT_NOCLIP = ConcurrentHashMap.newKeySet();
}
