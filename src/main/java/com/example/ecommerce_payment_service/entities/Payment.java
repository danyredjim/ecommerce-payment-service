package com.example.ecommerce_payment_service.entities;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.ecommerce_payment_service.dto.PaymentStatus;
import jakarta.persistence.*;


@Entity
@Table(name = "payments"/*,
indexes = {
    @Index(name = "idx_order_id", columnList = "orderId"),
    @Index(name = "idx_transaction_id", columnList = "transactionId")
}*/)

public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // ID que devuelve el proveedor externo (Stripe, etc.)
    private String transactionId;

    // Proveedor (STRIPE, PAYPAL...)
    private String provider;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;

    protected Payment() {
    }

    public Payment(Long orderId, BigDecimal amount, String currency,String provider) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markCompleted(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
    
    
}
