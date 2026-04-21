package com.example.ecommerce_payment_service.messaging;

import com.example.ecommerce_payment_service.entities.OutboxAvroEventEntity;
import com.example.ecommerce_payment_service.repositories.OutboxAvroRepository;
import com.example.events.PaymentAvroCompletedEvent;
import com.example.events.PaymentAvroFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OutboxPublisher {

    private final OutboxAvroRepository outboxRepository;
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxAvroRepository outboxRepository,
            KafkaTemplate<String, SpecificRecord> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishAvro() {

        System.out.println("⏱ PAYMENT OutboxPublisher ejecutándose...");

        List<OutboxAvroEventEntity> events =
                outboxRepository.findByPublishedFalse();

        for (OutboxAvroEventEntity e : events) {

            try {

                Map<String, Object> payload =
                        objectMapper.readValue(e.getPayload(), Map.class);

                if (e.getEventType().equals("PaymentAvroCompletedEvent")) {

                    PaymentAvroCompletedEvent event =
                            PaymentAvroCompletedEvent.newBuilder()
                                    .setEventId((String) payload.get("eventId"))
                                    .setOrderId(Long.parseLong(payload.get("orderId").toString()))
                                    .setTransactionId((String) payload.get("transactionId"))
                                    .setInstant((String) payload.get("instant"))
                                    .build();

                    kafkaTemplate.send("payment-completed", event).get();

                } else {

                    PaymentAvroFailedEvent event =
                            PaymentAvroFailedEvent.newBuilder()
                                    .setEventId((String) payload.get("eventId"))
                                    .setOrderId(Long.parseLong(payload.get("orderId").toString()))
                                    .setReason((String) payload.get("reason"))
                                    .setInstant((String) payload.get("instant"))
                                    .build();

                    kafkaTemplate.send("payment-failed", event).get();
                }

                e.setPublished(true);
                outboxRepository.save(e);

            } catch (Exception ex) {

                System.err.println("❌ Error en outbox PAYMENT: " + e.getId());
                ex.printStackTrace();
            }
        }
    }
}