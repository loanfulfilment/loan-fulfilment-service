package com.swapnilsankla.loanfulfilmentservice.interceptor

import com.swapnilsankla.loanfulfilmentservice.model.LoanApplication
import com.swapnilsankla.loanfulfilmentservice.publisher.LoanGrantedEventPublisher
import com.swapnilsankla.loanfulfilmentservice.repository.CorelationRepository
import com.swapnilsankla.loanfulfilmentservice.repository.LoanApplicationRepository
import com.swapnilsankla.tracestarter.Trace
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Aspect
class LoanApplicationRepositoryInterceptor(
        @Autowired val loanApplicationRepository: LoanApplicationRepository,
        @Autowired val corelationRepository: CorelationRepository,
        @Autowired val loanGrantedEventPublisher: LoanGrantedEventPublisher
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    @Pointcut("execution(public * com.swapnilsankla.loanfulfilmentservice.repository.LoanApplicationRepository.upsert(..)) " +
     "&& args(loanApplication)")
    fun pointcut(loanApplication: LoanApplication) {}

    @After("pointcut(loanApplication)")
    fun afterUpsert(loanApplication: LoanApplication) {
        loanApplicationRepository.findBy(loanApplication.customerId)?.let {
            if(it.canGrantLoan()) {
                logger.info("Raising loan granted event")
                loanGrantedEventPublisher.publish(it, corelationRepository.findByCustomerId(loanApplication.customerId).trace)
            }
        }
    }
}