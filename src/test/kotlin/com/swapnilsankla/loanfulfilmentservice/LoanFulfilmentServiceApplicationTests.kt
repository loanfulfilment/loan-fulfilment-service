package com.swapnilsankla.loanfulfilmentservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.swapnilsankla.loanfulfilmentservice.model.*
import com.swapnilsankla.loanfulfilmentservice.repository.CorelationRepository
import com.swapnilsankla.loanfulfilmentservice.repository.LoanApplicationRepository
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.lang.Thread.sleep
import java.time.Duration

@RunWith(SpringRunner::class)
@SpringBootTest
@EmbeddedKafka(partitions = 1,
        controlledShutdown = false,
        brokerProperties = ["listeners=PLAINTEXT://localhost:3333", "port=3333"])
@TestPropertySource(properties = [
    "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"
])
class LoanFulfilmentServiceApplicationTests {

    @Value("\${spring.kafka.bootstrap-servers}")
    lateinit var bootstrapServer: String

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var repository: LoanApplicationRepository

    @Autowired
    lateinit var corelationRepository: CorelationRepository


    @Before
    fun setUp() {
        repository.clearAll()
        corelationRepository.deleteAll()
    }

    @Test
    fun `should save entry in database on receiving needLoanEvent`() {
        //1. Produce event on needLoanEvent topic
        val needLoanEventString = objectMapper.writeValueAsString(NeedLoanEvent(customerId = "1", applicationNumber = "1A"))
        kafkaTemplate.send("needLoanEvent", needLoanEventString)

        //2. Check whether db has the entry
        val loanApplication = waitTill(numberOfAttempts = 5, tillValue = null) { repository.findBy(customerId = "1") }
        loanApplication shouldNotBe null
        loanApplication?.applicationNumber shouldBe "1A"
    }

    @Test
    fun `should save entry in database on receiving loanOfferAvailable`() {
        //1. Produce event on loanOfferDataAvailableForLoanProcessing topic
        val offer = LoanOfferAvailable.Offer(
                interestRate = 8.5,
                tenureInMonths = 60,
                approvedLoanAmount = 500000.0,
                foreclosureCharges = 0.0,
                partPaymentCharges = 0.0
        )
        val loanOfferAvailable = LoanOfferAvailable(
                customerId = "1",
                offer = offer
        )
        val loanOfferAvailableEventString = objectMapper.writeValueAsString(loanOfferAvailable)
        kafkaTemplate.send("loanOfferDataAvailableForLoanProcessing", loanOfferAvailableEventString)

        //2. Check whether db has the entry
        val loanApplication = waitTill(numberOfAttempts = 5, tillValue = null) { repository.findBy(customerId = "1") }
        loanApplication shouldNotBe null
        loanApplication?.offer shouldBe offer
    }

    @Test
    fun `should save entry in database on receiving screeningDataAvailableForLoanProcessing`() {
        //1. Produce event on screeningDataAvailableForLoanProcessing topic
        val screeningResult = ScreeningResult("1", FraudStatus.CLEAR)
        val screeningEventString = objectMapper.writeValueAsString(screeningResult)
        kafkaTemplate.send("screeningDataAvailableForLoanProcessing", screeningEventString)

        //2. Check whether db has the entry
        val loanApplication = waitTill(numberOfAttempts = 5, tillValue = null) { repository.findBy(customerId = "1") }
        loanApplication shouldNotBe null
        loanApplication?.fraudStatus shouldBe screeningResult.fraudStatus
    }

    @Test
    fun `should receive loanGranted event on receiving all events meet criteria of loan approval`() {
        //1. Produce event on needLoanEvent topic
        val needLoanEventString = objectMapper.writeValueAsString(NeedLoanEvent(customerId = "1", applicationNumber = "1A"))
        kafkaTemplate.send("needLoanEvent", needLoanEventString)

        //2. Produce event on loanOfferDataAvailableForLoanProcessing topic
        val offer = LoanOfferAvailable.Offer(
                interestRate = 8.5,
                tenureInMonths = 60,
                approvedLoanAmount = 500000.0,
                foreclosureCharges = 0.0,
                partPaymentCharges = 0.0
        )
        val loanOfferAvailable = LoanOfferAvailable(
                customerId = "1",
                offer = offer
        )
        val loanOfferAvailableEventString = objectMapper.writeValueAsString(loanOfferAvailable)
        kafkaTemplate.send("loanOfferDataAvailableForLoanProcessing", loanOfferAvailableEventString)

        //3. Produce event on screeningDataAvailableForLoanProcessing topic
        val screeningResult = ScreeningResult("1", FraudStatus.CLEAR)
        val screeningEventString = objectMapper.writeValueAsString(screeningResult)
        kafkaTemplate.send("screeningDataAvailableForLoanProcessing", screeningEventString)

        //4. Check whether loan granted event is raised or not
        val loanGrantedConsumer = buildKafkaConsumer()
        val records = waitTill(numberOfAttempts = 5, tillValue = listOf()) {
            loanGrantedConsumer.poll(Duration.ofSeconds(2)).records("loanGranted").toList()
        }
        val loanApplication = objectMapper.readValue(records?.first()?.value(), LoanApplication::class.java)
        loanApplication shouldNotBe null
        loanApplication.canGrantLoan() shouldBe true
    }

    private fun <T> waitTill(numberOfAttempts: Int, tillValue: T, f: () -> T): T? {
        var remainingNumberOfAttempts = numberOfAttempts
        var t: T? = null
        while (remainingNumberOfAttempts > 0) {
            t = f()
            if (t == tillValue) {
                remainingNumberOfAttempts--
                sleep(1000)
            } else break
        }
        return t
    }

    private fun buildKafkaConsumer(): KafkaConsumer<String, String> {
        val consumerProperties = java.util.Properties()

        consumerProperties[org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG] = "testClientId"
        consumerProperties[org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        consumerProperties[org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG] = "testGroupId"
        consumerProperties[org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProperties[org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

        val kafkaConsumer = KafkaConsumer<String, String>(consumerProperties)
        kafkaConsumer.subscribe(listOf("loanGranted"))
        return kafkaConsumer
    }
}
