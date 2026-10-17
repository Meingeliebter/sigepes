package com.sigepes.backend.common;

import java.security.SecureRandom;

/**
 * Códigos públicos legibles (seguimiento de denuncias, lotes): aleatorios con {@link SecureRandom}
 * para que no se puedan adivinar, y sin 0/O ni 1/I/L para poder dictarlos sin confusiones.
 */
public final class Codes {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Codes() {
    }

    public static String random(String prefix, int length) {
        StringBuilder code = new StringBuilder(prefix).append('-');
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }
}
