package com.swapnilsankla.loanfulfilmentservice.configuration

import com.mongodb.reactivestreams.client.MongoClient
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions

@ConfigurationProperties(prefix = "loan-fulfilment-service.kafka.loan-application-event")
@Configuration
class LoanApplicationEventConfig {
    lateinit var topic: String
    var consumer = ConsumerConfig()
    var producer = ProducerConfig()
}

@ConfigurationProperties(prefix = "loan-fulfilment-service.kafka.need-loan-event")
@Configuration
class NeedLoanEventConfig {
    lateinit var topic: String
    var consumer = ConsumerConfig()
}

@ConfigurationProperties(prefix = "loan-fulfilment-service.kafka.loan-offer-event")
@Configuration
class LoanOfferAvailableEventConfig {
    lateinit var topic: String
    var consumer = ConsumerConfig()
}

@ConfigurationProperties(prefix = "loan-fulfilment-service.kafka.screening-result-event")
@Configuration
class ScreeningResultAvailableEventConfig {
    lateinit var topic: String
    var consumer = ConsumerConfig()
}

class ConsumerConfig {
    lateinit var clientId: String
    lateinit var groupId: String
    lateinit var autoOffsetReset: String
    var properties = Properties()
}

class ProducerConfig {
    lateinit var clientId: String
    var properties = Properties()
}

class Properties {
    var interceptor = InterceptorProperty()
}

class InterceptorProperty {
    lateinit var classes: String
}

@Configuration
class Beans {
    @Autowired
    private lateinit var loanApplicationEventProperties: LoanApplicationEventConfig

    @Autowired
    private lateinit var needLoanEventProperties: NeedLoanEventConfig

    @Autowired
    private lateinit var loanOfferAvailableEventConfig: LoanOfferAvailableEventConfig

    @Autowired
    private lateinit var screeningResultAvailableEventConfig: ScreeningResultAvailableEventConfig

    @Value("\${loan-fulfilment-service.mongo.database-name}")
    private lateinit var databaseName: String

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun loanApplicationEventKafkaProducer(): KafkaProducer<String, String> {
        val properties = java.util.Properties()
        properties[ProducerConfig.CLIENT_ID_CONFIG] = loanApplicationEventProperties.producer.clientId
        properties[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        properties[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        properties[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        properties[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] = loanApplicationEventProperties.producer.properties.interceptor.classes

        return KafkaProducer(properties)
    }

    @Bean
    fun loanApplicationEventKafkaConsumer(): KafkaReceiver<String, String>? {
        return buildKafkaConsumer(
                consumerConfig = loanApplicationEventProperties.consumer,
                topicName = loanApplicationEventProperties.topic
        )
    }

    @Bean
    fun needLoanEventKafkaConsumer(): KafkaReceiver<String, String>? {
        return buildKafkaConsumer(
                consumerConfig = needLoanEventProperties.consumer,
                topicName = needLoanEventProperties.topic
        )
    }

    @Bean
    fun loanOfferAvailableEventKafkaConsumer(): KafkaReceiver<String, String>? {
        return buildKafkaConsumer(
                consumerConfig = loanOfferAvailableEventConfig.consumer,
                topicName = loanOfferAvailableEventConfig.topic
        )
    }

    @Bean
    fun screeningResultAvailableEventKafkaConsumer(): KafkaReceiver<String, String>? {
        return buildKafkaConsumer(
                consumerConfig = screeningResultAvailableEventConfig.consumer,
                topicName = screeningResultAvailableEventConfig.topic
        )
    }

//    @Bean
//    fun reactiveMongoTemplate(@Autowired mongoClient: MongoClient): ReactiveMongoTemplate? {
//        return ReactiveMongoTemplate(mongoClient, databaseName)
//    }

    private fun buildKafkaConsumer(
            consumerConfig: ConsumerConfig,
            topicName: String
    ): KafkaReceiver<String, String>? {
        val consumerProperties = java.util.Properties()

        consumerProperties[CLIENT_ID_CONFIG] = consumerConfig.clientId
        consumerProperties[BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        consumerProperties[GROUP_ID_CONFIG] = consumerConfig.groupId
        consumerProperties[AUTO_OFFSET_RESET_CONFIG] = consumerConfig.autoOffsetReset
        consumerProperties[INTERCEPTOR_CLASSES_CONFIG] = consumerConfig.properties.interceptor.classes

        consumerProperties[KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProperties[VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

        val receiverOptions = ReceiverOptions
                .create<String, String>(consumerProperties)
                .subscription(listOf(topicName))
        return KafkaReceiver.create(receiverOptions)
    }
}