import Config.S3AWS.bucket
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.io.BufferedInputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*

val log: JsonLoggerWrapper = JsonLoggerWrapper.getLogger("shovelAsync")

fun shovelAsync(kafka: KafkaConsumer<ByteArray, ByteArray>, s3client: AmazonS3, pollTimeout: Duration) =
    GlobalScope.async {
        log.info(Config.Kafka.reportTopicSubscriptionDetails())
        while (isActive) {
            log.info("Subscribing", "dead_letter_queue_topic", Config.Kafka.deadLetterQueueTopic)
            kafka.subscribe(setOf(Config.Kafka.deadLetterQueueTopic))
            log.info("Polling", "polling_timeout", "$pollTimeout")
            val records = kafka.poll(pollTimeout)
            log.info("Got records", "count", "${records.count()}")
            for (record in records) {
                log.info("Processing record", "record", "${record.key()}")
                val newKey: ByteArray = record.key() ?: ByteArray(0)
                if (newKey.isEmpty()) {
                    log.warn(
                        "Empty key was skipped",
                            "topic", record.topic() ?: "null",
                            "partition", record.partition().toString(),
                            "offset", record.offset().toString())
                    continue
                }

                try {
                    putObject(s3client, record)
                    log.info("Written new key", "key", String(newKey),
                            "topic", record.topic() ?: "null",
                            "partition", "${record.partition()}",
                            "offset", "${record.offset()}")
                } catch (e: Exception) {
                    log.error("Error putting object", "message", e.message ?: "")
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
    logger.info("Put object", "result", "$putObjectResult")
}

fun objectKey(key: ByteArray): String {
    return "${Config.S3AWS.prefixBase}/${Config.Kafka.deadLetterQueueTopic}/${SimpleDateFormat("YYYY-MM-dd").format(Date())}/${String(key)}"
}
