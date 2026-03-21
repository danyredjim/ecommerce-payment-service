package com.example.ecommerce_payment_service.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerce_payment_service.entities.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String>{

	//Optional<ProcessedEvent>existsById(Long id);
	
}
