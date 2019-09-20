import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import lib.sendRecord
import lib.timestamp
import lib.uniqueBytes
import org.apache.kafka.clients.producer.KafkaProducer

class Kafka2S3 : StringSpec({
    val producer = KafkaProducer<ByteArray, ByteArray>(Config.Kafka.props)
    "messages are written faithfully to s3." {
        val topic = Config.Kafka.deadLetterQueueTopic.toByteArray()
        val body = uniqueBytes()
        val timestamp = timestamp()
        val key = uniqueBytes()
        producer.sendRecord(topic, key, body, timestamp)
        Thread.sleep(10_000)
        val s3object = s3client().getObject(Config.S3AWS.bucket, objectKey(key))
        println("Got s3Object '$s3object'.")
        s3object shouldNotBe null
        val bytes = s3object.objectContent.readBytes()

        println("Bytes place on queue: '${String(body)}'.")
        println("Bytes read from s3: '${String(bytes)}'.")
        body shouldBe bytes
        assert(String(bytes) == String(body))

        val metadata = s3object.objectMetadata
        metadata.userMetadata["kafka-topic"] shouldBe Config.Kafka.deadLetterQueueTopic
        metadata.userMetadata["kafka-timestamp"] shouldBe timestamp.toString()
    }
})