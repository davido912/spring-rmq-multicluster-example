package com.example.demo.service

import com.example.demo.config.RabbitMQConfig
import com.example.demo.config.RabbitTemplateWrapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MessageService(
    private val rabbitTemplateWrapper: RabbitTemplateWrapper,
) {
    @Value("\${rabbitmq.exchange.name}")
    private lateinit var exchangeName: String

    @Value("\${rabbitmq.routing.key}")
    private lateinit var routingKey: String

    fun sendToLegacyCluster() {
        rabbitTemplateWrapper.convertAndSend(exchangeName, routingKey, "test", RabbitMQConfig.RabbitMqConnection.SHARED_RABBITMQ)
        rabbitTemplateWrapper.convertAndSend(exchangeName, routingKey, "test", RabbitMQConfig.RabbitMqConnection.LEGACY_RABBITMQ)
    }
}