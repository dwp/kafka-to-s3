import Config.S3AWS.bucket
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*

val log: Logger = LoggerFactory.getLogger("shovelAsync")

fun shovelAsync(kafka: KafkaConsumer<ByteArray, ByteArray>, s3client: AmazonS3, pollTimeout: Duration) =
    GlobalScope.async {
        log.info(Config.Kafka.reportTopicSubscriptionDetails())
        while (isActive) {
            kafka.subscribe(setOf(Config.Kafka.deadLetterQueueTopic))
            val records = kafka.poll(pollTimeout)
            for (record in records) {

                val newKey: ByteArray = record.key() ?: ByteArray(0)
                if (newKey.isEmpty()) {
                    log.warn(
                        "Empty key was skipped for %s:%d:%d".format(
                            record.topic() ?: "null",
                            record.partition(),
                            record.offset()
                        ))
                    continue
                }

                try {
                    putObject(s3client, record)
                    log.info("""Written new key '$newKey', 
                        |topic '${record.topic()}', 
                        |partition: '${record.partition()}', 
                        |offset: '${record.offset()}'.""".trimMargin())
                } catch (e: Exception) {
                    log.error(e.message, e)
                }
            }
        }
    }


fun putObject(s3client: AmazonS3, record: ConsumerRecord<ByteArray, ByteArray>) {
    val key: ByteArray = record.key()
    val metadata = ObjectMetadata()
    metadata.contentType = "binary/octetstream"
    metadata.contentLength = record.value().size.toLong()
    metadata.addUserMetadata("kafka-topic", record.topic())
    metadata.addUserMetadata("kafka-partition", "${record.partition()}")
    metadata.addUserMetadata("kafka-offset", "${record.offset()}")
    metadata.addUserMetadata("kafka-timestamp", "${record.timestamp()}")
    val headers = record.headers()
    headers.forEach { metadata.addUserMetadata("kafka-header-${it.key()}", String(it.value())) }
    val path = objectKey(key)
    logger.info("path: '$path'.")
    val putObjectRequest: PutObjectRequest = PutObjectRequest(bucket,
        path,
        BufferedInputStream(record.value().inputStream()),
        metadata)
    val putObjectResult = s3client.putObject(putObjectRequest)
    //putObjectResult.metadata.
    logger.info("putObjectResult: '$putObjectResult'.")
}

fun objectKey(key: ByteArray): String {
    return "${Config.S3AWS.prefixBase}/${Config.Kafka.deadLetterQueueTopic}/${SimpleDateFormat("YYYY-MM-dd").format(Date())}/${String(key)}"
}