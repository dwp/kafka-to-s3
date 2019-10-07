#!/bin/sh

set -e

if [ -n "${HTTP_PROXY_HOST}" ]; then
    export GRADLE_OPTS="$GRADLE_OPTS -Dhttp.proxyHost=${HTTP_PROXY_HOST} -Dhttp.proxyPort=${HTTP_PROXY_HOST}"
fi

if [ -n "${HTTPS_PROXY_HOST}" ]; then
    export GRADLE_OPTS="$GRADLE_OPTS -Dhttps.proxyHost=${HTTPS_PROXY_HOST} -Dhttps.proxyPort=${HTTPS_PROXY_HOST}"
fi
