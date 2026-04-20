package com.example.ecommerce_payment_service.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


import com.example.ecommerce_common_events.PaymentCompletedEvent;
import com.example.ecommerce_common_events.PaymentFailedEvent;
import com.example.ecommerce_common_events.StockReservedEvent;
import com.example.ecommerce_payment_service.dto.PaymentStatus;
import com.example.ecommerce_payment_service.entities.Payment;
import com.example.ecommerce_payment_service.entities.ProcessedEvent;
import com.example.ecommerce_payment_service.repositories.PaymentRepository;
import com.example.ecommerce_payment_service.repositories.ProcessedEventRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentListener {

    private final PaymentRepository paymentRepository;
    
    @Autowired
    ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentListener(PaymentRepository paymentRepository,KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentRepository = paymentRepository;        
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "stock-reserved", groupId = "payment-group")
    @Transactional
    public void handle(StockReservedEvent event) {

        // 🔥 1️⃣ Idempotencia
        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }

        try {
            // 🔥 2️⃣ Crear pago en estado PENDING
            Payment payment = new Payment(
                    event.getOrderId(),
                    calculateAmount(event),
                    "EUR",
                    "STRIPE" // proveedor simulado
            );
            // 🔥 3️⃣ Procesar pago externo
            boolean paymentOk = processPayment(event);

            if (paymentOk) {

                String transactionId = UUID.randomUUID().toString();

                // 🔥 4️⃣ Cambiar estado dentro del dominio
                payment.markCompleted(transactionId);

                paymentRepository.save(payment);

                // 🔥 5️⃣ Guardar evento procesado
                processedEventRepository.save(new ProcessedEvent(
                                event.getEventId(),
                                event.getClass().getSimpleName(),
                                String.valueOf(event.getOrderId())
                        )
                );

                // 🔥 6️⃣ Publicar éxito
                kafkaTemplate.send("payment-completed",new PaymentCompletedEvent(
                                UUID.randomUUID().toString(),
                                event.getOrderId(),
                                Instant.now()
                        )
                );

            } else {

                // 🔥 Pago fallido
                payment.markFailed();

                paymentRepository.save(payment);

                processedEventRepository.save(
                        new ProcessedEvent(
                                event.getEventId(),
                                event.getClass().getSimpleName(),
                                String.valueOf(event.getOrderId())
                        )
                );

                kafkaTemplate.send(
                        "payment-failed",
                        new PaymentFailedEvent(
                                UUID.randomUUID().toString(),
                                event.getOrderId(),
                                Instant.now()
                        )
                );
            }

        } catch (Exception e) {

            // ⚠️ Si algo inesperado ocurre
            kafkaTemplate.send("payment-failed",new PaymentFailedEvent(
                            UUID.randomUUID().toString(),
                            event.getOrderId(),
                            Instant.now()
                    )
            );

            throw e; // importante para rollback
        }
    }

    private boolean processPayment(StockReservedEvent event) {
        // Aquí iría integración con Stripe, PayPal, etc.
        return true; // simulación
    }

    private BigDecimal calculateAmount(StockReservedEvent event) {
        // Normalmente vendría del Order Service
        return BigDecimal.valueOf(100);
    }
}
