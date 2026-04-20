package com.example.ecommerce_payment_service.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.ecommerce_payment_service.entities.Payment;
import com.example.ecommerce_payment_service.entities.ProcessedEvent;
import com.example.ecommerce_payment_service.repositories.PaymentRepository;
import com.example.ecommerce_payment_service.repositories.ProcessedEventRepository;

import com.example.events.StockAvroReservedEvent;
import com.example.events.PaymentAvroCompletedEvent;
import com.example.events.PaymentAvroFailedEvent;

import jakarta.transaction.Transactional;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentListener {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    // ✅ SIN @Qualifier → porque tu bean se llama "kafkaTemplate"
    public PaymentListener(
            PaymentRepository paymentRepository,
            ProcessedEventRepository processedEventRepository,
            KafkaTemplate<String, SpecificRecord> kafkaTemplate
    ) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "stock-reserved",
            groupId = "payment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(StockAvroReservedEvent event) {

        String eventId = event.getEventId().toString();

        // 🔥 1️⃣ Idempotencia
        if (processedEventRepository.existsById(eventId)) {
            return;
        }

        try {
            // 🔥 2️⃣ Crear pago
            Payment payment = new Payment(
                    event.getOrderId(),
                    calculateAmount(event),
                    "EUR",
                    "STRIPE"
            );

            // 🔥 3️⃣ Procesar pago
            boolean paymentOk = processPayment(event);

            if (paymentOk) {

                String transactionId = UUID.randomUUID().toString();

                // 🔥 4️⃣ Guardar
                payment.markCompleted(transactionId);
                paymentRepository.save(payment);

                processedEventRepository.save(
                        new ProcessedEvent(
                                eventId,
                                event.getClass().getSimpleName(),
                                String.valueOf(event.getOrderId())
                        )
                );

                // 🔥 5️⃣ EVENTO OK (AVRO)
                PaymentAvroCompletedEvent successEvent =
                        PaymentAvroCompletedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(event.getOrderId())
                                .setTransactionId(transactionId)
                                .setInstant(Instant.now().toString())
                                .build();

                kafkaTemplate.send("payment-completed", successEvent);

            } else {
                handleFailure(event, eventId);
            }

        } catch (Exception e) {
            handleFailure(event, eventId);
            throw e;
        }
    }

    // 🔥 método limpio reutilizable
    private void handleFailure(StockAvroReservedEvent event, String eventId) {

        processedEventRepository.save(
                new ProcessedEvent(
                        eventId,
                        event.getClass().getSimpleName(),
                        String.valueOf(event.getOrderId())
                )
        );

        PaymentAvroFailedEvent failedEvent =
                PaymentAvroFailedEvent.newBuilder()
                        .setEventId(UUID.randomUUID().toString())
                        .setOrderId(event.getOrderId())
                        .setReason("PAYMENT_FAILED")
                        .setInstant(Instant.now().toString())
                        .build();

        kafkaTemplate.send("payment-failed", failedEvent);
    }

    private boolean processPayment(StockAvroReservedEvent event) {
        return true;
    }

    private BigDecimal calculateAmount(StockAvroReservedEvent event) {
        return BigDecimal.valueOf(100);
    }
}