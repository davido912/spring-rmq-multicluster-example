package com.example.demo

import com.example.demo.MessageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/messages")
class MessageController(private val messageService: MessageService) {

    @PostMapping("/send")
    fun sendMessage(): ResponseEntity<String> {
        messageService.sendToLegacyCluster()
        return ResponseEntity.ok("Message sent to RMQ clusters")
    }
}