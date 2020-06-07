package com.swapnilsankla.loanfulfilmentservice.listener.loanOfferAvailable

import com.fasterxml.jackson.databind.ObjectMapper
import com.swapnilsankla.loanfulfilmentservice.model.LoanApplication
import com.swapnilsankla.loanfulfilmentservice.model.LoanOfferAvailable
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import java.util.logging.Logger
import javax.annotation.PostConstruct

@Component
class LoanOfferAvailableEventListener(
        @Autowired val objectMapper: ObjectMapper,
        @Autowired val loanApplicationEventKafkaProducer: KafkaProducer<String, String>,
        @Autowired val loanOfferAvailableEventKafkaConsumer: KafkaReceiver<String, String>,
        @Value("\${loan-fulfilment-service.kafka.loan-application-event.topic}") val loanApplicationTopicName: String
) {
    private val logger = Logger.getLogger(LoanOfferAvailableEventListener::class.simpleName)

    @PostConstruct
    fun listen() {
        loanOfferAvailableEventKafkaConsumer.receive().map {
            val loanOfferAvailableEvent = objectMapper.readValue(it.value() as String, LoanOfferAvailable::class.java)
            logger.info("loanOfferAvailable event received for customer ${loanOfferAvailableEvent.customerId}")
            val loanApplicationEventString = objectMapper.writeValueAsString(LoanApplication(
                    customerId = loanOfferAvailableEvent.customerId,
                    offer = loanOfferAvailableEvent.offer
            ))
            loanApplicationEventKafkaProducer.send(ProducerRecord(loanApplicationTopicName, loanApplicationEventString))
        }.subscribe()
    }
}