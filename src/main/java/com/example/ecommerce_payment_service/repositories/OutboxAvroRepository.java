package com.example.ecommerce_payment_service.repositories;

import com.example.ecommerce_payment_service.entities.OutboxAvroEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxAvroRepository extends JpaRepository<OutboxAvroEventEntity, String> {

    List<OutboxAvroEventEntity> findByPublishedFalse();
}
