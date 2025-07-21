package com.example.demo

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class MessageListener {
    @RabbitListener(queues = ["\${rabbitmq.queue.name}"], containerFactory = "legacyListenerFactory")
    fun receiveMessageLegacy(message: String) {
        println("Received message from Legacy cluster queue: $message")
    }

    @RabbitListener(queues = ["\${rabbitmq.queue.name}"], containerFactory = "sharedListenerFactory")
    fun receiveMessageShared(message: String) {
        println("Received message from Shared cluster queue: $message")
    }
}