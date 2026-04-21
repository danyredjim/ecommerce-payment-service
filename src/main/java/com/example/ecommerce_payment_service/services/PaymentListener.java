package com.example.ecommerce_payment_service.services;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.ecommerce_payment_service.entities.Payment;
import com.example.ecommerce_payment_service.entities.ProcessedEvent;
import com.example.ecommerce_payment_service.messaging.PaymentOutboxService;
import com.example.ecommerce_payment_service.repositories.PaymentRepository;
import com.example.ecommerce_payment_service.repositories.ProcessedEventRepository;
import com.example.events.StockAvroReservedEvent;

import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentListener {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentOutboxService paymentOutboxService;

    public PaymentListener(
            PaymentRepository paymentRepository,
            ProcessedEventRepository processedEventRepository,
            PaymentOutboxService paymentOutboxService
    ) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.paymentOutboxService = paymentOutboxService;
    }

    @KafkaListener(
            topics = "stock-reserved",
            groupId = "payment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handle(StockAvroReservedEvent event) {

        String eventId = event.getEventId().toString();

        // 🔥 1️⃣ IDEMPOTENCIA
        if (processedEventRepository.existsById(eventId)) {
            return;
        }

        try {

            // 🔥 2️⃣ CREAR PAYMENT (PENDING por defecto)
            Payment payment = new Payment(
                    event.getOrderId(),
                    calculateAmount(event),
                    "EUR",
                    "STRIPE"
            );

            paymentRepository.save(payment);

            // 🔥 3️⃣ PROCESAR PAGO (fake o real)
            boolean paymentOk = processPayment(event);

            if (paymentOk) {

                String transactionId = UUID.randomUUID().toString();

                // 🔥 4️⃣ ACTUALIZAR ESTADO
                payment.markCompleted(transactionId);
                paymentRepository.save(payment);

                // 🔥 5️⃣ GUARDAR EN OUTBOX (NO KAFKA)
                paymentOutboxService.saveOutboxEvent(
                        payment,
                        event,
                        true,
                        transactionId
                );

            } else {

                payment.markFailed();
                paymentRepository.save(payment);

                paymentOutboxService.saveOutboxEvent(
                        payment,
                        event,
                        false,
                        null
                );
            }

            // 🔥 6️⃣ MARCAR EVENTO COMO PROCESADO
            processedEventRepository.save(
                    new ProcessedEvent(
                            eventId,
                            event.getClass().getSimpleName(),
                            String.valueOf(event.getOrderId())
                    )
            );

        } catch (Exception e) {

            // ⚠️ IMPORTANTE: también registras como procesado
            processedEventRepository.save(
                    new ProcessedEvent(
                            eventId,
                            event.getClass().getSimpleName(),
                            String.valueOf(event.getOrderId())
                    )
            );

            throw e;
        }
    }

    // 🔧 Simulación (luego aquí irá Stripe)
    private boolean processPayment(StockAvroReservedEvent event) {
        return Math.random() > 0.2; // 80% OK
    }

    private BigDecimal calculateAmount(StockAvroReservedEvent event) {
        return BigDecimal.valueOf(100);
    }
}