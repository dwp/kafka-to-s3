
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import lib.sendRecord
import lib.timestamp
import lib.uniqueBytes
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.slf4j.LoggerFactory
import java.util.*

class Kafka2S3Spec : StringSpec() {
    private val logger = LoggerFactory.getLogger(Kafka2S3Spec::class.toString())

    init {
        "messages are written faithfully to s3." {
            val producer = KafkaProducer<ByteArray, ByteArray>(producerProps())
            val topic = Config.Kafka.deadLetterQueueTopic.toByteArray()
            val body = uniqueBytes()
            val timestamp = timestamp()
            val key = uniqueBytes()
            logger.info("Placing '${String(key)}' on the queue.")
            producer.sendRecord(topic, key, body, timestamp)
            logger.info("Placed '${String(key)}' on the queue.")
            Thread.sleep(10_000)
            logger.info("Fetching object from s3.")
            val s3object = s3client().getObject(Config.S3AWS.bucket, objectKey(key))
            logger.info("Got s3Object '$s3object'.")
            s3object shouldNotBe null
            val bytes = s3object.objectContent.readBytes()
            logger.info("Bytes place on queue: '${String(body)}'.")
            logger.info("Bytes read from s3: '${String(bytes)}'.")
            body shouldBe bytes
            assert(String(bytes) == String(body))
            val metadata = s3object.objectMetadata
            metadata.userMetadata["kafka-topic"] shouldBe Config.Kafka.deadLetterQueueTopic
            metadata.userMetadata["kafka-timestamp"] shouldBe timestamp.toString()
        }

    }

    private fun producerProps() = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getEnv("K2S3_KAFKA_BOOTSTRAP_SERVERS") ?: "kafka:9092")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java)
        put(ProducerConfig.METADATA_MAX_AGE_CONFIG, getEnv("K2S3_KAFKA_META_REFRESH_MS") ?: "10000")
    }

}
