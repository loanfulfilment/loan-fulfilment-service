package com.swapnilsankla.loanfulfilmentservice.listener.loanApplication

import com.fasterxml.jackson.databind.ObjectMapper
import com.swapnilsankla.loanfulfilmentservice.model.LoanApplication
import com.swapnilsankla.loanfulfilmentservice.repository.LoanApplicationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import java.util.logging.Logger
import javax.annotation.PostConstruct

@Component
class LoanApplicationEventListener(
        @Autowired val objectMapper: ObjectMapper,
        @Autowired val loanApplicationEventKafkaConsumer: KafkaReceiver<String, String>,
        @Autowired val loanApplicationRepository: LoanApplicationRepository
) {
    private val logger = Logger.getLogger(LoanApplicationEventListener::class.simpleName)

    @PostConstruct
    fun listen() {
        loanApplicationEventKafkaConsumer.receive().map {
            val loanApplication = objectMapper.readValue(it.value() as String, LoanApplication::class.java)
            logger.info("loan application event received for customer ${loanApplication.customerId}")
            loanApplicationRepository.upsert(loanApplication)
        }.subscribe()
    }
}