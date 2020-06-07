package com.swapnilsankla.loanfulfilmentservice.model

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("LoanApplication")
class LoanApplication(
        @Indexed(unique = true)
        val customerId: String,
        val applicationNumber: String? = null,
        val offer: LoanOfferAvailable.Offer? = null,
        val fraudStatus: FraudStatus? = null
) {
    fun canGrantLoan(): Boolean {
        return containsAllDataToProcess() &&
                fraudStatus == FraudStatus.CLEAR &&
                (offer?.approvedLoanAmount!! > 0.0)
    }

    override fun toString(): String {
        return "LoanApplication(customerId=$customerId, applicationNumber=$applicationNumber, offer=$offer, fraudStatus=$fraudStatus)"
    }

    private fun containsAllDataToProcess() = applicationNumber != null && offer != null && fraudStatus != null
}
