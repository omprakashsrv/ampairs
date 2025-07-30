package com.ampairs.tally.service

import com.ampairs.core.config.ApplicationProperties
import com.ampairs.tally.client.TallyClient
import com.ampairs.tally.model.TallyXML
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.stream.StreamSource

@Service
@Transactional
class TallyService(
    private val applicationProperties: ApplicationProperties,
    private val tallyClient: TallyClient,
) {
    private val logger = LoggerFactory.getLogger(TallyService::class.java)
    private val jaxbContext = JAXBContext.newInstance(TallyXML::class.java)

    @Retryable(
        value = [TallyIntegrationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 5000, multiplier = 2.0)
    )
    fun importMasters(inputStream: InputStream): TallyXML? {
        return try {
            val unmarshaller = jaxbContext.createUnmarshaller()
            (unmarshaller.unmarshal(inputStream) as TallyXML?).also {
                logger.info("Successfully imported Tally masters XML")
            }
        } catch (e: JAXBException) {
            logger.error("Failed to parse Tally XML: {}", e.message, e)
            throw TallyParsingException("Failed to parse Tally XML", e)
        } catch (e: Exception) {
            logger.error("Unexpected error during Tally import: {}", e.message, e)
            throw TallyIntegrationException("Failed to import Tally masters", e)
        }
    }

    @Retryable(
        value = [TallyIntegrationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 5000, multiplier = 2.0)
    )
    fun exportToTally(tallyXML: TallyXML): String {
        return try {
            val marshaller = jaxbContext.createMarshaller()
            marshaller.setProperty("jaxb.formatted.output", true)
            marshaller.setProperty("jaxb.encoding", "UTF-8")

            val stringWriter = StringWriter()
            marshaller.marshal(tallyXML, stringWriter)

            stringWriter.toString().also {
                logger.info("Successfully exported data to Tally XML format")
            }
        } catch (e: JAXBException) {
            logger.error("Failed to marshal Tally XML: {}", e.message, e)
            throw TallyParsingException("Failed to create Tally XML", e)
        } catch (e: Exception) {
            logger.error("Unexpected error during Tally export: {}", e.message, e)
            throw TallyIntegrationException("Failed to export to Tally", e)
        }
    }

    @Retryable(
        value = [TallyIntegrationException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 5000, multiplier = 2.0)
    )
    fun syncMasters(): TallySyncResult {
        return try {
            logger.info("Starting Tally masters synchronization")

            val startTime = System.currentTimeMillis()
            val companies = tallyClient.getCompanies()
            val ledgers = tallyClient.getLedgers()
            val stockItems = tallyClient.getStockItems()
            val vouchers = tallyClient.getVouchers()

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            TallySyncResult(
                success = true,
                companiesCount = companies.size,
                ledgersCount = ledgers.size,
                stockItemsCount = stockItems.size,
                vouchersCount = vouchers.size,
                durationMs = duration,
                message = "Synchronization completed successfully"
            ).also {
                logger.info(
                    "Tally sync completed: {} companies, {} ledgers, {} stock items, {} vouchers in {}ms",
                    companies.size, ledgers.size, stockItems.size, vouchers.size, duration
                )
            }
        } catch (e: Exception) {
            logger.error("Tally synchronization failed: {}", e.message, e)
            TallySyncResult(
                success = false,
                message = "Synchronization failed: ${e.message}",
                error = e.javaClass.simpleName
            )
        }
    }

    fun validateXML(xmlContent: String): ValidationResult {
        return try {
            val unmarshaller = jaxbContext.createUnmarshaller()
            unmarshaller.unmarshal(StreamSource(StringReader(xmlContent)))
            ValidationResult(valid = true, message = "XML is valid")
        } catch (e: JAXBException) {
            logger.warn("XML validation failed: {}", e.message)
            ValidationResult(valid = false, message = "Invalid XML: ${e.message}")
        } catch (e: Exception) {
            logger.warn("Unexpected error during XML validation: {}", e.message)
            ValidationResult(valid = false, message = "Validation error: ${e.message}")
        }
    }
}

data class TallySyncResult(
    val success: Boolean,
    val companiesCount: Int = 0,
    val ledgersCount: Int = 0,
    val stockItemsCount: Int = 0,
    val vouchersCount: Int = 0,
    val durationMs: Long = 0,
    val message: String,
    val error: String? = null,
)

data class ValidationResult(
    val valid: Boolean,
    val message: String,
)

class TallyIntegrationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TallyParsingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)