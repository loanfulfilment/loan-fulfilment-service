package com.swapnilsankla.loanfulfilmentservice.listener.screeningResultAvailable

import com.fasterxml.jackson.databind.ObjectMapper
import com.swapnilsankla.loanfulfilmentservice.model.LoanApplication
import com.swapnilsankla.loanfulfilmentservice.model.ScreeningResult
import com.swapnilsankla.loanfulfilmentservice.repository.LoanApplicationRepository
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import java.time.Duration
import java.util.logging.Logger
import javax.annotation.PostConstruct

@Component
class ScreeningResultAvailableEventListener(
        @Autowired val objectMapper: ObjectMapper,
        @Autowired val screeningResultAvailableEventKafkaConsumer: KafkaReceiver<String, String>,
        @Autowired val loanApplicationEventKafkaProducer: KafkaProducer<String, String>,
        @Value("\${loan-fulfilment-service.kafka.loan-application-event.topic}") val loanApplicationTopicName: String
) {
    private val logger = Logger.getLogger(ScreeningResultAvailableEventListener::class.simpleName)

    @PostConstruct
    fun listen() {
        screeningResultAvailableEventKafkaConsumer.receive().map {
            val screeningResultAvailableEvent = objectMapper.readValue(it.value() as String, ScreeningResult::class.java)
            logger.info("screeningResultAvailable event received for customer ${screeningResultAvailableEvent.customerId}")
            val loanApplicationEventString = objectMapper.writeValueAsString(LoanApplication(
                    customerId = screeningResultAvailableEvent.customerId,
                    fraudStatus = screeningResultAvailableEvent.fraudStatus
            ))
            loanApplicationEventKafkaProducer.send(ProducerRecord(loanApplicationTopicName, loanApplicationEventString))
        }.subscribe()
    }
}