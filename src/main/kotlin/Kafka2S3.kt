import Config.S3AWS.accessKey
import Config.S3AWS.clientRegion
import Config.S3AWS.localClient
import Config.S3AWS.region
import Config.S3AWS.secretKey
import Config.S3AWS.serviceEndpoint
import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.misc.Signal

val logger: Logger = LoggerFactory.getLogger("Kafka2S3")

suspend fun main() {
    val kafka = KafkaConsumer<ByteArray, ByteArray>(Config.Kafka.props)
    val job = shovelAsync(kafka, s3client(), Config.Kafka.pollTimeout)
    Signal.handle(Signal("INT")) { job.cancel() }
    Signal.handle(Signal("TERM")) { job.cancel() }
    job.await()
    kafka.close()
}

fun s3client(): AmazonS3 {
    return if (localClient) {
        logger.info("Creating localstack aws s3 client.")
        AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
            .withClientConfiguration(ClientConfiguration().withProtocol(Protocol.HTTP))
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey)))
            .withPathStyleAccessEnabled(true)
            .disableChunkedEncoding()
            .build()
    } else {
        logger.info("Creating cloud aws s3 client.")
        AmazonS3ClientBuilder.standard()
            .withCredentials(DefaultAWSCredentialsProviderChain())
            .withRegion(clientRegion)
            .build()
    }
}