#!/bin/sh

set -e

# If a proxy is requested, set it up

if [ "${INTERNET_PROXY}" ]; then
  export http_proxy="http://${INTERNET_PROXY}:3128"
  export https_proxy="http://${INTERNET_PROXY}:3128"
  export no_proxy=169.254.169.254
  echo "Using proxy ${INTERNET_PROXY}"
fi

# Generate a cert for Kafka mutual auth

if [[ "${K2S3_KAFKA_INSECURE}" != "true" ]]
then

    SSL_DIR="$(mktemp -d)"
    export K2S3_PRIVATE_KEY_PASSWORD="$(uuidgen)"

    export K2S3_KEYSTORE_PATH="${SSL_DIR}/k2s3.keystore"
    export K2S3_KEYSTORE_PASSWORD="$(uuidgen)"

    export K2S3_TRUSTSTORE_PATH="${SSL_DIR}/ks23.truststore"
    export K2S3_TRUSTSTORE_PASSWORD="$(uuidgen)"

    if [[ "${K2S3_KAFKA_CERT_MODE}" == "CERTGEN" ]]; then

        echo "Generating cert for host ${HOSTNAME}"

        acm-pca-cert-generator \
            --subject-cn "${HOSTNAME}" \
            --keystore-path "${K2S3_KEYSTORE_PATH}" \
            --keystore-password "${K2S3_KEYSTORE_PASSWORD}" \
            --private-key-password "${K2S3_PRIVATE_KEY_PASSWORD}" \
            --truststore-path "${K2S3_TRUSTSTORE_PATH}" \
            --truststore-password "${K2S3_TRUSTSTORE_PASSWORD}"

        echo "Cert generation result is $? for ${HOSTNAME}"

    elif [[ "${K2S3_KAFKA_CERT_MODE}" == "RETRIEVE" ]]; then

        echo "Retrieving cert from ${RETRIEVER_ACM_CERT_ARN}"

        export RETRIEVER_ACM_KEY_PASSPHRASE="$(uuidgen)"

        acm-cert-retriever \
            --acm-key-passphrase "${RETRIEVER_ACM_KEY_PASSPHRASE}" \
            --keystore-path "${K2S3_KEYSTORE_PATH}" \
            --keystore-password "${K2S3_KEYSTORE_PASSWORD}" \
            --private-key-password "${K2S3_PRIVATE_KEY_PASSWORD}" \
            --truststore-path "${K2S3_TRUSTSTORE_PATH}" \
            --truststore-password "${K2S3_TRUSTSTORE_PASSWORD}"

        echo "Cert retrieve result is $? for ${RETRIEVER_ACM_CERT_ARN}"

    else
        echo "K2S3_KAFKA_CERT_MODE must be one of 'CERTGEN,RETRIEVE' but was ${K2S3_KAFKA_CERT_MODE}"
        exit 1
    fi
else
    echo "Skipping cert generation for host ${HOSTNAME}"
fi

exec "${@}"
