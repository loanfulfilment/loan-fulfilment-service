package com.swapnilsankla.loanfulfilmentservice.listener.needLoan

import com.fasterxml.jackson.databind.ObjectMapper
import com.swapnilsankla.loanfulfilmentservice.model.LoanApplication
import com.swapnilsankla.loanfulfilmentservice.model.NeedLoanEvent
import com.swapnilsankla.loanfulfilmentservice.repository.Corelation
import com.swapnilsankla.loanfulfilmentservice.repository.CorelationRepository
import com.swapnilsankla.loanfulfilmentservice.repository.LoanApplicationRepository
import com.swapnilsankla.tracestarter.TraceIdExtractor
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import java.time.Duration
import java.util.logging.Logger
import javax.annotation.PostConstruct

@Component
class NeedLoanEventListener(
        @Autowired val objectMapper: ObjectMapper,
        @Autowired val needLoanEventKafkaConsumer: KafkaReceiver<String, String>,
        @Autowired val loanApplicationEventKafkaProducer: KafkaProducer<String, String>,
        @Autowired val corelationRepository: CorelationRepository,
        @Autowired val traceIdExtractor: TraceIdExtractor,
        @Value("\${loan-fulfilment-service.kafka.loan-application-event.topic}") val loanApplicationTopicName: String
) {
    private val logger = Logger.getLogger(NeedLoanEventListener::class.simpleName)

    @PostConstruct
    fun listen() {
        needLoanEventKafkaConsumer.receive().map {
            val needLoanEvent = objectMapper.readValue(it.value() as String, NeedLoanEvent::class.java)
            val trace = traceIdExtractor.fromKafkaHeaders(it.headers())
            corelationRepository.save(Corelation(needLoanEvent.customerId, trace))
            logger.info("needLoanEvent event received for customer ${needLoanEvent.customerId}")
            val loanApplicationEventString = objectMapper.writeValueAsString(LoanApplication(
                    customerId = needLoanEvent.customerId,
                    applicationNumber = needLoanEvent.applicationNumber
            ))
            loanApplicationEventKafkaProducer.send(ProducerRecord(loanApplicationTopicName, loanApplicationEventString))
        }.subscribe()
    }
}