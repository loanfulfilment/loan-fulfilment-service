package com.swapnilsankla.loanfulfilmentservice.publisher

import com.swapnilsankla.loanfulfilmentservice.model.LoanApplication
import com.swapnilsankla.tracestarter.CustomKafkaTemplate
import com.swapnilsankla.tracestarter.Trace
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LoanGrantedEventPublisher(@Autowired val kafkaTemplate: CustomKafkaTemplate) {

    private val logger = LoggerFactory.getLogger(LoanGrantedEventPublisher::class.simpleName)

    fun publish(loanApplication: LoanApplication, trace: Trace) {
        logger.info("raising event $loanApplication")

        kafkaTemplate.publish(topic = "loanGranted",
                data = loanApplication,
                trace = trace)
    }
}