package com.example.ecommerce_payment_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerce_payment_service.entities.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long>{

}
