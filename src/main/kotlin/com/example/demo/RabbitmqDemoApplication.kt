package com.example.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration::class])
class RabbitmqDemoApplication

fun main(args: Array<String>) {
	runApplication<RabbitmqDemoApplication>(*args)
}
