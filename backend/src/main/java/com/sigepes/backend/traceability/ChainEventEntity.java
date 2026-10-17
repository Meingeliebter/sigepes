package com.sigepes.backend.traceability;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * AC-09.3 / AC-09.5 · Eslabón de la cadena de custodia. De solo inserción: la entidad es inmutable
 * para Hibernate y la API no expone ninguna operación de edición ni borrado.
 */
@Entity
@Immutable
@Table(name = "chain_events")
public class ChainEventEntity {

    public enum EventType { CREATED, TRANSFER }

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "lot_id", length = 40, nullable = false)
    private String lotId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20, nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", length = 20, nullable = false)
    private PartyType partyType;

    @Column(name = "party_name", length = 120, nullable = false)
    private String partyName;

    @Column(name = "party_tax_id", length = 30)
    private String partyTaxId;

    @Column(name = "location", length = 120, nullable = false)
    private String location;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "recorded_by", length = 40, nullable = false)
    private String recordedBy;

    @Column(name = "previous_hash", length = 64, nullable = false)
    private String previousHash;

    @Column(name = "hash", length = 64, nullable = false)
    private String hash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChainEventEntity() {
    }

    ChainEventEntity(String id, String lotId, int sequenceNo, EventType eventType, PartyType partyType,
            String partyName, String partyTaxId, String location, LocalDate occurredOn, String recordedBy,
            String previousHash, String hash) {
        this.id = id;
        this.lotId = lotId;
        this.sequenceNo = sequenceNo;
        this.eventType = eventType;
        this.partyType = partyType;
        this.partyName = partyName;
        this.partyTaxId = partyTaxId;
        this.location = location;
        this.occurredOn = occurredOn;
        this.recordedBy = recordedBy;
        this.previousHash = previousHash;
        this.hash = hash;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getLotId() {
        return lotId;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public EventType getEventType() {
        return eventType;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public String getPartyName() {
        return partyName;
    }

    public String getPartyTaxId() {
        return partyTaxId;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
