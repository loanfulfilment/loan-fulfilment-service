package com.swapnilsankla.loanfulfilmentservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
class LoanFulfilmentServiceApplication

fun main(args: Array<String>) {
    runApplication<LoanFulfilmentServiceApplication>(*args)
}
