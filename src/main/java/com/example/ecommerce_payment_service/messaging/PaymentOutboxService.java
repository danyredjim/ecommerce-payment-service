package com.example.ecommerce_payment_service.messaging;

import com.example.ecommerce_payment_service.entities.OutboxAvroEventEntity;
import com.example.ecommerce_payment_service.entities.Payment;
import com.example.ecommerce_payment_service.repositories.OutboxAvroRepository;
import com.example.events.StockAvroReservedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentOutboxService {

    private final OutboxAvroRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentOutboxService(
            OutboxAvroRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void saveOutboxEvent(
            Payment payment,
            StockAvroReservedEvent event,
            boolean success,
            String transactionId
    ) {

        try {

            Map<String, Object> payloadMap = new HashMap<>();

            payloadMap.put("eventId", UUID.randomUUID().toString());
            payloadMap.put("orderId", event.getOrderId());
            payloadMap.put("instant", Instant.now().toString());

            if (success) {
                payloadMap.put("transactionId", transactionId);
            } else {
                payloadMap.put("reason", "PAYMENT_FAILED");
            }

            String payload = objectMapper.writeValueAsString(payloadMap);

            String eventType = success
                    ? "PaymentAvroCompletedEvent"
                    : "PaymentAvroFailedEvent";

            OutboxAvroEventEntity outbox = new OutboxAvroEventEntity(
                    UUID.randomUUID().toString(),
                    "PAYMENT",
                    payment.getOrderId().toString(),
                    eventType,
                    payload
            );

            outboxRepository.save(outbox);

        } catch (Exception e) {
            throw new RuntimeException("Error creando outbox event", e);
        }
    }
}