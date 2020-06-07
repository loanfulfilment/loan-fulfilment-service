package com.swapnilsankla.loanfulfilmentservice.repository

import com.swapnilsankla.tracestarter.Trace
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository

interface CorelationRepository: MongoRepository<Corelation, String> {
    fun findByCustomerId(customerId: String): Corelation
}

@Document("Corelation")
data class Corelation(val customerId: String, val trace: Trace)