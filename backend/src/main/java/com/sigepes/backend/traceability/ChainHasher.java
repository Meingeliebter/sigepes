package com.sigepes.backend.traceability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

/**
 * AC-09.5 · Encadenamiento por hash (SHA-256).
 *
 * <p>Cada evento firma su contenido junto con el hash del anterior. El primero (CREATED) incluye
 * además los datos del lote (especie, peso, captura, fecha), de modo que alterar el peso del lote,
 * un eslabón o el orden de la cadena hace que la verificación falle.
 */
final class ChainHasher {

    static final String GENESIS = "GENESIS";

    private ChainHasher() {
    }

    static String hash(String previousHash, LotEntity lot, int sequenceNo, ChainEventEntity.EventType type,
            PartyType partyType, String partyName, String partyTaxId, String location, LocalDate occurredOn) {
        StringBuilder canonical = new StringBuilder()
                .append(previousHash).append('|')
                .append(lot.getId()).append('|')
                .append(sequenceNo).append('|')
                .append(type).append('|')
                .append(partyType).append('|')
                .append(partyName).append('|')
                .append(partyTaxId == null ? "" : partyTaxId).append('|')
                .append(location).append('|')
                .append(occurredOn);
        if (type == ChainEventEntity.EventType.CREATED) {
            canonical.append('|').append(lot.getCode())
                    .append('|').append(lot.getSpecies().getId())
                    .append('|').append(lot.getWeightKg().stripTrailingZeros().toPlainString())
                    .append('|').append(lot.getCatchEntry().getId())
                    .append('|').append(lot.getLandingDate());
        }
        return sha256(canonical.toString());
    }

    /** Recalcula toda la cadena: true solo si cada eslabón coincide y apunta al anterior. */
    static boolean verify(LotEntity lot, List<ChainEventEntity> events) {
        String previous = GENESIS;
        for (int i = 0; i < events.size(); i++) {
            ChainEventEntity event = events.get(i);
            if (event.getSequenceNo() != i + 1 || !previous.equals(event.getPreviousHash())) {
                return false;
            }
            String expected = hash(previous, lot, event.getSequenceNo(), event.getEventType(), event.getPartyType(),
                    event.getPartyName(), event.getPartyTaxId(), event.getLocation(), event.getOccurredOn());
            if (!expected.equals(event.getHash())) {
                return false;
            }
            previous = event.getHash();
        }
        return !events.isEmpty();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
