package com.example.ecommerce_payment_service.config;

import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaServer;

    @Value("${spring.kafka.schema-registry}")
    private String schemaRegistry;

    // 🔥 PRODUCER AVRO GENÉRICO (LA CLAVE)
    @Bean
    public ProducerFactory<String, SpecificRecord> avroProducerFactory() {

        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);

        // Apicurio config
        config.put("apicurio.registry.url", schemaRegistry);
        config.put("apicurio.registry.auto-register", true);

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    // 🔥 ESTE ES EL QUE NECESITA TU LISTENER
    @Bean
    public KafkaTemplate<String, SpecificRecord> kafkaTemplate() {
        return new KafkaTemplate<>(avroProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, SpecificRecord> consumerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-group-v1");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class);

        // 🔥 Apicurio config
        props.put("apicurio.registry.url", schemaRegistry);
        props.put("apicurio.registry.use-specific-avro-reader", true);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SpecificRecord> kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, SpecificRecord> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        return factory;
    }
}