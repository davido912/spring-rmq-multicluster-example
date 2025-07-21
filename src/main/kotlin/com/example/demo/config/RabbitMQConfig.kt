package com.example.demo.config

import org.springframework.amqp.rabbit.annotation.MultiRabbitListenerAnnotationBeanPostProcessor
import org.springframework.amqp.rabbit.annotation.RabbitListenerAnnotationBeanPostProcessor
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactoryContextWrapper
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    @Bean
    @ConfigurationProperties("spring.rabbitmq.legacy-cluster")
    fun legacyRabbitmqProps() = RabbitProperties()

    @Bean
    @ConfigurationProperties("spring.rabbitmq.shared-cluster")
    fun sharedRabbitmqProps() = RabbitProperties()

    @Bean
    fun legacyRabbitMqConnectionFactory(
        legacyRabbitmqProps: RabbitProperties,
    ): CachingConnectionFactory = CachingConnectionFactory(
        legacyRabbitmqProps.host,
        legacyRabbitmqProps.port,
    ).apply {
        username = legacyRabbitmqProps.username
        setPassword(legacyRabbitmqProps.password)
        setPublisherConfirmType(CachingConnectionFactory.ConfirmType.SIMPLE)
    }

    @Bean
    fun sharedRabbitMqConnectionFactory(
        sharedRabbitmqProps: RabbitProperties,
    ): CachingConnectionFactory = CachingConnectionFactory(
        sharedRabbitmqProps.host,
        sharedRabbitmqProps.port,
    ).apply {
        username = sharedRabbitmqProps.username
        setPassword(sharedRabbitmqProps.password)
        setPublisherConfirmType(CachingConnectionFactory.ConfirmType.SIMPLE)
    }

    @Bean
    fun rabbitRoutingConnectionFactory(
        legacyRabbitMqConnectionFactory: CachingConnectionFactory,
        sharedRabbitMqConnectionFactory: CachingConnectionFactory,
    ): SimpleRoutingConnectionFactory = SimpleRoutingConnectionFactory().apply {
        setTargetConnectionFactories(
            mapOf(
                RabbitMqConnection.LEGACY_RABBITMQ.name to legacyRabbitMqConnectionFactory,
                RabbitMqConnection.SHARED_RABBITMQ.name to sharedRabbitMqConnectionFactory,
            ),
        )
        setDefaultTargetConnectionFactory(sharedRabbitMqConnectionFactory)
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: SimpleRoutingConnectionFactory,
        rabbitRoutingConnectionFactory: SimpleRoutingConnectionFactory,
    ): RabbitTemplateWrapper = RabbitTemplateWrapper(
        connectionFactory = connectionFactory,
        connectionContextWrapper = ConnectionFactoryContextWrapper(rabbitRoutingConnectionFactory),
    )

    @Bean
    fun legacyListenerFactory(legacyRabbitMqConnectionFactory: CachingConnectionFactory): SimpleRabbitListenerContainerFactory =
        SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(legacyRabbitMqConnectionFactory)
        }

    @Bean
    fun sharedListenerFactory(sharedRabbitMqConnectionFactory: CachingConnectionFactory): SimpleRabbitListenerContainerFactory =
        SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(sharedRabbitMqConnectionFactory)
        }

    @Bean
    fun rabbitListenerEndpointRegistry(): RabbitListenerEndpointRegistry = RabbitListenerEndpointRegistry()

    @Bean
    fun multiRabbitListenerAnnotationBeanPostProcessor(
        registry: RabbitListenerEndpointRegistry,
    ): RabbitListenerAnnotationBeanPostProcessor = MultiRabbitListenerAnnotationBeanPostProcessor().apply {
        setEndpointRegistry(registry)
        setContainerFactoryBeanName("legacyListenerFactory")
    }

    @Bean("legacyListenerFactory-admin")
    fun amqpAdmin(
        legacyRabbitMqConnectionFactory: CachingConnectionFactory,
    ): RabbitAdmin = RabbitAdmin(legacyRabbitMqConnectionFactory)

    @Bean("sharedListenerFactory-admin")
    fun sharedRabbitMqAdminAmqpClient(
        sharedRabbitMqConnectionFactory: CachingConnectionFactory,
    ): RabbitAdmin = RabbitAdmin(sharedRabbitMqConnectionFactory)


    enum class RabbitMqConnection {
        LEGACY_RABBITMQ,
        SHARED_RABBITMQ;
    }
}

class RabbitTemplateWrapper(
    connectionFactory: ConnectionFactory,
    private val connectionContextWrapper: ConnectionFactoryContextWrapper,
) : RabbitTemplate(connectionFactory) {

    fun convertAndSend(
        exchange: String,
        routingKey: String,
        message: Any,
        rabbitMqConnection: RabbitMQConfig.RabbitMqConnection,
    ) {
        connectionContextWrapper.run(rabbitMqConnection.name) {
            this.invoke { rabbitOps ->
                rabbitOps.convertAndSend(exchange, routingKey, message)
                rabbitOps.waitForConfirmsOrDie(5000)
            }
        }
    }
}