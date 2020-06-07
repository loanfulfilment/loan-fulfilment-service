package com.swapnilsankla.loanfulfilmentservice.repository

import com.swapnilsankla.loanfulfilmentservice.model.LoanApplication
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

@Repository
class LoanApplicationRepository(@Autowired private val mongoTemplate: MongoTemplate) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun upsert(loanApplication: LoanApplication) {
        val query = Query()
        query.addCriteria(Criteria.where("customerId").`is`(loanApplication.customerId))
        val update = Update()

        update["customerId"] = loanApplication.customerId
        update["loanGranted"] = loanApplication.canGrantLoan()
        if(loanApplication.applicationNumber != null)
            update["applicationNumber"] = loanApplication.applicationNumber
        if(loanApplication.offer != null)
            update["offer"] = loanApplication.offer
        if(loanApplication.fraudStatus != null)
            update["fraudStatus"] = loanApplication.fraudStatus

        mongoTemplate.upsert(query, update, LoanApplication::class.java)
    }

    fun findBy(customerId: String): LoanApplication? {
        val query = Query()
        query.addCriteria(Criteria.where("customerId").`is`(customerId))
        return mongoTemplate.findOne(query, LoanApplication::class.java)
    }

    internal fun clearAll() {
        mongoTemplate.dropCollection(LoanApplication::class.java)
    }
}
