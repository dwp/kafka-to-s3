# Kafka2S3

Listens to a kafka topic and writes it unadulterated to s3.

## Makefile

A Makefile wraps some of the gradle and docker-compose commands to give a
more unified basic set of operations. These can be checked by running:

```
$ make help
```

## Build

Ensure a JVM is installed and run the gradle wrapper.

    make build

## Distribute

If a standard zip file is required, just use the assembleDist command.

    make dist

This produces a zip and a tarball of the latest version.

## Run full local stack

A full local stack can be run using the provided Dockerfile and Docker
Compose configuration. The Dockerfile uses a multi-stage build so no
pre-compilation is required.

    make up

The environment can be stopped without losing any data:

    make down

Or completely removed including all data volumes:

    make destroy

## Run integration tests

Integration tests can be executed inside a Docker container to make use of
the Kafka and AWS instances running in the local stack. The integration
tests are written in Kotlin and use the standard `kotlintest` testing framework.

    make integration

## Run in an IDE

Both Kafka2S3 and the integration tests can be run in an IDE to facilitate
quicker feedback then a containerized approach. This is useful during active development.

To do this first bring up the s3, kafka and zookeeper containers:

    make services

Then insert into your local hosts file the names, IP addresses of the kafka and
hbase containers:

    make hosts

The main class and the integration test class can now be run from an IDE.

## Getting logs

The services are listed in the `docker-compose.yaml` file and logs can be
retrieved for all services, or for a subset.

    docker-compose logs aws-s3

The logs can be followed so new lines are automatically shown.

    docker-compose logs -f aws-s3

## Configuration

There are a number of environment variables that can be used to configure
the system. Some of them are for configuring Kafka2S3 itself, and some
are for configuring the built-in ACM PCA client to perform mutual auth.

### Kafka2S3 Configuration

#### Kafka

By default Kafka2S3 will connect to Kafka at `kafka:9092` in the `k2hb`
consumer group. It will poll the `test-topic` topic with a poll timeout of
`10` days, and refresh the topics list every 10 seconds (`10000` ms).

* **K2S3_KAFKA_BOOTSTRAP_SERVERS**
    Comma separated list of Kafka servers and ports
* **K2S3_KAFKA_CONSUMER_GROUP**
    The name of the consumer group to join
* **K2S3_KAFKA_TOPIC_REGEX**
    A regex that will fetch a list of topics to listen to, e.g. `db.*`. Defaults to `test-topic.*`
* **K2S3_KAFKA_META_REFRESH_MS** (Optional)
    The frequency that the consumer will ask the broker for metadata updates, which also checks for new topics.
    Defaults to `10000` ms (10 seconds).
    Typically, should be an order of magnitude less than `K2S3_KAFKA_POLL_TIMEOUT`, else new topics will not be discovered within each polling interval.
* **K2S3_KAFKA_POLL_TIMEOUT**
    The maximum time to wait for messages in ISO-8601 duration format (e.g. `PT10S`).
    Defaults to 1 Hour.
    Should be greater than `K2S3_KAFKA_META_REFRESH_MS`, else new topics will not be discovered within each polling interval.
* **K2S3_KAFKA_INSECURE**
    Disable SSL entirely (useful for dev / test) with `K2S3_KAFKA_INSECURE=true`
* **K2S3_KAFKA_CERT_MODE**
    If SSL is enabled, either create certs in ACM-PCA with value `CERTGEN` or retrieve
    them from ACM with value `RETRIEVE`

#### S3

* **AWS_ENDPOINT_S3
    Localstack only - where to direct aws s3 api calls - default `http://aws-s3:4566`
* **AWS_LOCAL_CLIENT
    Configure the s3 client for localstack usage.
* **AWS_REGION
    The region in which the target s3 bucket resides (default `eu-west-2`).
* **AWS_S3_BUCKET
    The bucket in which to place incoming messages (default `kafka2s3`).
* **AWS_S3_PREFIX_BASE
    A value to prefix onto the prefix for the placed objects (default is empty string).
* **AWS_ACCESS_KEY and AWS_SECRET_KEY
    Valid tokens for aws access - not needed for localstack.

#### SSL Mutual Authentication (CERTGEN mode)

By default the SSL is enabled but has no defaults. These must either be
configured in full or disabled entirely via `K2S3_KAFKA_INSECURE=FALSE`
and `K2S3_KAFKA_CERT_MODE=CERTGEN`.

For an authoritative full list of arguments see the tool help; Arguments not listed here are
defaulted in the `entrypoint.sh` script.

* **CERTGEN_CA_ARN**
    The AWS CA ARN to use to generate the cert
* **CERTGEN_KEY_TYPE**
    The type of private key (`RSA` or `DSA`)
* **CERTGEN_KEY_LENGTH**
    The key length in bits (`1024`, `2048` or `4096`)
* **CERTGEN_KEY_DIGEST**
    The key digest algorithm (`sha256`, `sha384`, `sha512`)
* **CERTGEN_SUBJECT_C**
    The subject country
* **CERTGEN_SUBJECT_ST**
    The subject state/province/county
* **CERTGEN_SUBJECT_L**
    The subject locality
* **CERTGEN_SUBJECT_O**
    The subject organisation
* **CERTGEN_SUBJECT_OU**
    The subject organisational unit
* **CERTGEN_SUBJECT_EMAILADDRESS**
    The subject email address
* **CERTGEN_SIGNING_ALGORITHM**
    The certificate signing algorithm used by ACM PCA
    (`SHA256WITHECDSA`, `SHA384WITHECDSA`, `SHA512WITHECDSA`, `SHA256WITHRSA`, `SHA384WITHRSA`, `SHA512WITHRSA`)
* **CERTGEN_VALIDITY_PERIOD**
    The certificate validity period in Go style duration (e.g. `1y2m6d`)
* **CERTGEN_PRIVATE_KEY_ALIAS**
    Alias for the private key
* **CERTGEN_TRUSTSTORE_CERTS**
    Comma delimited list of S3 URIs pointing to certificates to be included in the trust store
* **CERTGEN_TRUSTSTORE_ALIASES**
    Comma delimited list of aliases for the certificate
* **CERTGEN_LOG_LEVEL**
    The log level of the certificate generator (`CRITICAL`, `ERROR`, `WARNING`, `INFO`, `DEBUG`)


#### SSL Mutual Authentication (RETRIEVE mode)

By default the SSL is enabled but has no defaults. These must either be
configured in full or disabled entirely via `K2S3_KAFKA_INSECURE=FALSE`
and `K2S3_KAFKA_CERT_MODE=RETRIEVE`.

For an authoritative full list of arguments see the tool help; Arguments not listed here are
defaulted in the `entrypoint.sh` script.

* **RETRIEVER_ACM_CERT_ARN**
    ARN in AWS ACM to use to fetch the required cert, cert chain, and key
* **RETRIEVER_ADD_DOWNLOADED_CHAIN**
    Whether or not to add the downloaded cert chain from the ARN to the trust store
    Allowed missing, `true`, `false`, `yes`, `no`, `1` or `0`
    If missing defaults to false
* **RETRIEVE_TRUSTSTORE_CERTS**
    Comma delimited list of S3 URIs pointing to certificates to be included in the trust store
* **RETRIEVE_TRUSTSTORE_ALIASES**
    Comma delimited list of aliases for the certificate
* **RETRIEVE_LOG_LEVEL**
    The log level of the certificate generator (`CRITICAL`, `ERROR`, `WARNING`, `INFO`, `DEBUG`)
