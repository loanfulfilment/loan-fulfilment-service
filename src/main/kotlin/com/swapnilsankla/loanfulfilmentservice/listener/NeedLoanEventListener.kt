package com.swapnilsankla.loanfulfilmentservice.listener

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class NeedLoanEventListener(@Autowired val objectMapper: ObjectMapper) {

    @KafkaListener(topics = ["needLoanEvent"])
    fun listen(needLoanEventString: String) {
        val needLoanEvent = objectMapper.readValue(needLoanEventString, NeedLoanEvent::class.java)
        Logger.getLogger(NeedLoanEventListener::class.simpleName)
                .info("needLoanEvent event received for customer ${needLoanEvent.customerId}")
    }
}