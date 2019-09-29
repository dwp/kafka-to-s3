
import com.amazonaws.regions.Regions
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.time.Duration
import java.util.*

fun getEnv(envVar: String): String? {
    val value = System.getenv(envVar)
    return if (value.isNullOrEmpty()) null else value
}

fun String.toDuration(): Duration {
    return Duration.parse(this)
}

object Config {

    object S3AWS {
        val serviceEndpoint = getEnv("AWS_ENDPOINT_S3") ?: "http://aws-s3:4572"
        val region = getEnv("AWS_REGION") ?: "eu-west-2"
        val accessKey = getEnv("AWS_ACCESS_KEY") ?: "aws-access-key"
        val secretKey = getEnv("AWS_SECRET_KEY") ?: "aws-secret-access-key"
        private val regionEnumName = region.toUpperCase().replace("-", "_")
        val clientRegion = Regions.valueOf(regionEnumName)
        val localClient = (getEnv("AWS_LOCAL_CLIENT") ?: "true").toBoolean()
        val bucket = getEnv("AWS_S3_BUCKET") ?: "kafka2s3"
        val prefixBase = getEnv("AWS_S3_PREFIX_BASE") ?: ""
    }

    object Kafka {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getEnv("K2S3_KAFKA_BOOTSTRAP_SERVERS") ?: "kafka:9092")
            put(ConsumerConfig.GROUP_ID_CONFIG, getEnv("K2S3_KAFKA_CONSUMER_GROUP") ?: "test")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, getEnv("K2S3_KAFKA_META_REFRESH_MS") ?: "10000")

            if (secure()) {
                put("security.protocol", "SSL")
                put("ssl.truststore.location", getEnv("K2S3_TRUSTSTORE_PATH"))
                put("ssl.truststore.password", getEnv("K2S3_TRUSTSTORE_PASSWORD"))
                put("ssl.keystore.location", getEnv("K2S3_KEYSTORE_PATH"))
                put("ssl.keystore.password", getEnv("K2S3_KEYSTORE_PASSWORD"))
                put("ssl.key.password", getEnv("K2S3_PRIVATE_KEY_PASSWORD"))
            }
        }

        private fun secure(): Boolean {
            val useSSL = getEnv("K2S3_KAFKA_INSECURE") ?: "true" != "true"
            return useSSL
        }

        val pollTimeout: Duration = getEnv("K2S3_KAFKA_POLL_TIMEOUT")?.toDuration() ?: Duration.ofHours(1)
        val deadLetterQueueTopic: String = getEnv("K2S3_DEAD_LETTER_QUEUE") ?: "dead-letter-queue"
        fun reportTopicSubscriptionDetails() =
            """Subscribing to topics '${deadLetterQueueTopic}' 
                |with poll timeout '${pollTimeout}' 
                |and metadata refresh every '${props.getProperty(ConsumerConfig.METADATA_MAX_AGE_CONFIG)} ms'""".trimMargin()
    }
}
