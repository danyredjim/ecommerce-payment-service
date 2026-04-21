package com.example.ecommerce_payment_service.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_avro_event")
public class OutboxAvroEventEntity {

    @Id
    private String id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Instant createdAt = Instant.now();
    private boolean published = false;

    public OutboxAvroEventEntity() {}

    public OutboxAvroEventEntity(String id, String aggregateType, String aggregateId,
                                 String eventType, String payload) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    // getters/setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}