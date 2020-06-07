package com.swapnilsankla.loanfulfilmentservice.model

data class LoanOfferAvailable(val customerId: String, val offer: Offer) {
    data class Offer(val interestRate: Double,
                     val tenureInMonths: Int,
                     val approvedLoanAmount: Double,
                     val foreclosureCharges: Double,
                     val partPaymentCharges: Double)
}
