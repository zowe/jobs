#!/bin/bash -e

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2018, 2019
################################################################################

################################################################################
# Prepare workspace for integration test
#
# This script requires these environment variables:
# - FVT_ZOSMF_HOST
# - FVT_ZOSMF_PORT
# - FVT_SERVER_SSH_HOST
# - FVT_SERVER_SSH_PORT
# - FVT_SERVER_SSH_USERNAME
# - FVT_SERVER_SSH_PASSWORD
# - FVT_SERVER_DIRECTORY_ROOT
# - FVT_UID
################################################################################

################################################################################
# contants
SCRIPT_NAME=$(basename "$0")
SCRIPT_PWD=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_PWD" && cd .. && pwd)
FVT_APIML_ARTIFACT=$1
FVT_WORKSPACE="${ROOT_DIR}/.fvt"
FVT_APIML_DIR=api-layer
FVT_JOBS_DIR=jobs
FVT_KEYSTORE_DIR=keystore
FVT_CONFIG_DIR=configs
FVT_LOGS_DIR=logs
FVT_DISCOVERY_PORT=7552
FVT_GATEWAY_PORT=7554
FVT_API_PORT=8443

################################################################################
# validate parameters
# set default values
echo "[${SCRIPT_NAME}] validate parameters"
if [ -z "${FVT_ZOSMF_HOST}" ]; then
  echo "[${SCRIPT_NAME}][error] environment variable FVT_ZOSMF_HOST is required."
  exit 1
fi
if [ -z "${FVT_ZOSMF_PORT}" ]; then
  echo "[${SCRIPT_NAME}][error] environment variable FVT_ZOSMF_PORT is required."
  exit 1
fi
if [ -z "${FVT_SERVER_SSH_HOST}" ]; then
  echo "[${SCRIPT_NAME}][error] environment variable FVT_SERVER_SSH_HOST is required."
  exit 1
fi
if [ -z "${FVT_SERVER_SSH_PORT}" ]; then
  echo "[${SCRIPT_NAME}][error] environment variable FVT_SERVER_SSH_PORT is required."
  exit 1
fi
if [ -z "${FVT_SERVER_SSH_USERNAME}" ]; then
  echo "[${SCRIPT_NAME}][error] environment variable FVT_SERVER_SSH_USERNAME is required."
  exit 1
fi
if [ -z "${FVT_SERVER_SSH_PASSWORD}" ]; then
  echo "[${SCRIPT_NAME}][error] environment variable FVT_SERVER_SSH_PASSWORD is required."
  exit 1
fi
if [ -z "${FVT_SERVER_DIRECTORY_ROOT}" ]; then
  echo "[${SCRIPT_NAME}][error] environment variable FVT_SERVER_DIRECTORY_ROOT is required."
  exit 1
fi
if [ -z "${FVT_UID}" ]; then
  echo "[${SCRIPT_NAME}][error] environment variable FVT_UID is required."
  exit 1
fi
if [ -z "$(which ssh-agent)" ]; then
  echo "[${SCRIPT_NAME}][error] ssh-agent is required."
  exit 1
fi
if [ -z "$FVT_APIML_ARTIFACT" ]; then
  FVT_APIML_ARTIFACT="libs-release-local/org/zowe/apiml/sdk/zowe-install/*/zowe-install-*.zip"
  echo "[${SCRIPT_NAME}][warn] API-ML artifact is not defined, using default value."
fi
echo

################################################################################
echo "[${SCRIPT_NAME}] find jobs api jar"
cd "${ROOT_DIR}"
JOBS_API_JAR=$(find jobs-api-server -type f -name 'jobs-api-server-*-boot.jar')
if [ -z "${JOBS_API_JAR}" ]; then
  echo "[${SCRIPT_NAME}] build jobs api"
  ./bootstrap_gradlew.sh
  ./gradlew assemble
  JOBS_API_JAR=$(find jobs-api-server -type f -name 'jobs-api-server-*-boot.jar')
fi
if [ -z "${JOBS_API_JAR}" ]; then
  echo "[${SCRIPT_NAME}][error] failed to find jobs api jar."
  exit 1
else
  echo "[${SCRIPT_NAME}] jobs api jar found: ${JOBS_API_JAR}"
fi
echo

################################################################################
echo "[${SCRIPT_NAME}] prepare FVT workspace"
rm -fr "${FVT_WORKSPACE}"
mkdir -p "${FVT_WORKSPACE}/${FVT_APIML_DIR}"
mkdir -p "${FVT_WORKSPACE}/${FVT_JOBS_DIR}"
mkdir -p "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}"
mkdir -p "${FVT_WORKSPACE}/${FVT_CONFIG_DIR}"
mkdir -p "${FVT_WORKSPACE}/${FVT_LOGS_DIR}"
echo

################################################################################
echo "[${SCRIPT_NAME}] prepare certificates"
echo "[${SCRIPT_NAME}] - generate CA in PKCS12 format"
keytool -genkeypair -v \
  -alias localca \
  -keyalg RSA -keysize 2048 \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localca.keystore.p12" \
  -dname "CN=Zowe Development Instances Certificate Authority, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
  -keypass local_ca_password \
  -storepass local_ca_password \
  -storetype PKCS12 \
  -validity 3650 \
  -ext KeyUsage=keyCertSign -ext BasicConstraints:critical=ca:true
echo "[${SCRIPT_NAME}] - export CA public key"
keytool -export -v \
  -alias localca \
  -file "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localca.cer" \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localca.keystore.p12" \
  -rfc \
  -keypass local_ca_password \
  -storepass local_ca_password \
  -storetype PKCS12
echo "[${SCRIPT_NAME}] - generate server certificate in PKCS12 format"
keytool -genkeypair -v \
  -alias localhost \
  -keyalg RSA -keysize 2048 \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.p12" \
  -keypass password \
  -storepass password \
  -storetype PKCS12 \
  -dname "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
  -validity 3650
echo "[${SCRIPT_NAME}] - generate CSR"
keytool -certreq -v \
  -alias localhost \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.p12" \
  -storepass password \
  -file "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.csr" \
  -keyalg RSA -storetype PKCS12 \
  -dname "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
  -validity 3650
echo "[${SCRIPT_NAME}] - sign CSR"
keytool -gencert -v \
  -infile "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.csr" \
  -outfile "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore_signed.cer" \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localca.keystore.p12" \
  -alias localca \
  -keypass local_ca_password \
  -storepass local_ca_password \
  -storetype PKCS12 \
  -ext "SAN=dns:localhost,ip:127.0.0.1" \
  -ext "KeyUsage:critical=keyEncipherment,digitalSignature,nonRepudiation,dataEncipherment" \
  -ext "ExtendedKeyUsage=clientAuth,serverAuth" \
  -rfc \
  -validity 3650
echo "[${SCRIPT_NAME}] - import CA to server keystore"
keytool -importcert -v \
  -trustcacerts -noprompt \
  -file "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localca.cer" \
  -alias localca \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.p12" \
  -storepass password \
  -storetype PKCS12
echo "[${SCRIPT_NAME}] - import signed CSR to server keystore"
keytool -importcert -v \
  -trustcacerts -noprompt \
  -file "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore_signed.cer" \
  -alias localhost \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.p12" \
  -storepass password \
  -storetype PKCS12
echo "[${SCRIPT_NAME}] - import CA as truststore"
keytool -importcert -v \
  -trustcacerts -noprompt \
  -file "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localca.cer" \
  -alias localca \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.truststore.p12" \
  -storepass password \
  -storetype PKCS12
echo "[${SCRIPT_NAME}] - Generates key pair for JWT token secret and exports the public key"
keytool -genkeypair -v \
  -alias jwtsecret \
  -keyalg RSA -keysize 2048 \
  -keystore "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.p12" \
  -dname "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ" \
  -keypass password \
  -storepass password \
  -storetype PKCS12 \
  -validity 3650
echo "[${SCRIPT_NAME}] - certificates prepared:"
ls -l "${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}"
echo

################################################################################
# prepare and download APIML
echo "[${SCRIPT_NAME}] prepare APIML download spec"
sed -e "s#{ARTIFACT}#${FVT_APIML_ARTIFACT}#g" \
    -e "s#{TARGET}#${FVT_WORKSPACE}/${FVT_APIML_DIR}/#g" \
    -e "s#{EXPLODE}#true#g" \
    scripts/fvt/artifactory-download-spec.json.template > ${FVT_WORKSPACE}/artifactory-download-spec.json
cat ${FVT_WORKSPACE}/artifactory-download-spec.json
echo "[${SCRIPT_NAME}] download APIML to target test folder"
jfrog rt dl --spec=${FVT_WORKSPACE}/artifactory-download-spec.json
cd "${FVT_WORKSPACE}"
APIML_JAR=$(find . -name 'gateway-service.jar')
if [ -z "${APIML_JAR}" ]; then
  echo "[${SCRIPT_NAME}][error] failed to find APIML jar."
  exit 1
fi
echo

################################################################################
echo "[${SCRIPT_NAME}] copy jobs api"
cd "${ROOT_DIR}"
cp "${JOBS_API_JAR}" "${FVT_WORKSPACE}/${FVT_JOBS_DIR}"
echo

################################################################################
echo "[${SCRIPT_NAME}] copy configs"
cd "${ROOT_DIR}"
sed -e "s|{ZOSMF_HOST}|${FVT_ZOSMF_HOST}|g" \
  -e "s|{ZOSMF_PORT}|${FVT_ZOSMF_PORT}|g" \
  "scripts/fvt/zosmf.yml.template" > "${FVT_WORKSPACE}/${FVT_CONFIG_DIR}/zosmf.yml"
sed -e "s|{API_PORT}|${FVT_API_PORT}|g" \
  "scripts/fvt/jobs-api.yml.template" > "${FVT_WORKSPACE}/${FVT_CONFIG_DIR}/jobs-api.yml.yml"
echo

################################################################################
echo "[${SCRIPT_NAME}] start jobs api"
# -Xquickstart \
java -Xms16m -Xmx512m \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dserver.port=${FVT_API_PORT} \
    -Dcom.ibm.jsse2.overrideDefaultTLS=true \
    -Dserver.ssl.keyAlias=localhost \
    -Dserver.ssl.keyStore=${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.p12 \
    -Dserver.ssl.keyStorePassword=password \
    -Dserver.ssl.keyStoreType=PKCS12 \
    -Dserver.compression.enabled=true \
    -Dgateway.httpsPort=${FVT_GATEWAY_PORT} \
    -Dgateway.ipAddress=localhost \
    -Dspring.main.banner-mode=off \
    -jar "${JOBS_API_JAR}" \
    > "${FVT_WORKSPACE}/${FVT_LOGS_DIR}/jobs-api.log" &
echo 

################################################################################
echo "[${SCRIPT_NAME}] start APIML discovery server"
# -Xquickstart \
java -Xms32m -Xmx256m \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.active=https \
    -Dserver.address=0.0.0.0 \
    -Dapiml.discovery.userid=eureka \
    -Dapiml.discovery.password=password \
    -Dapiml.discovery.allPeersUrls="https://localhost:${FVT_DISCOVERY_PORT}/eureka/" \
    -Dapiml.service.hostname=localhost \
    -Dapiml.service.port=${FVT_DISCOVERY_PORT} \
    -Dapiml.service.ipAddress=127.0.0.1 \
    -Dapiml.service.preferIpAddress=true \
    -Dapiml.service.allowEncodedSlashes=true \
    -Dapiml.discovery.staticApiDefinitionsDirectories="${FVT_WORKSPACE}/${FVT_CONFIG_DIR}" \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=false \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.keyStore="${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.p12" \
    -Dserver.ssl.keyStoreType=PKCS12 \
    -Dserver.ssl.keyStorePassword=password \
    -Dserver.ssl.keyAlias=localhost \
    -Dserver.ssl.keyPassword=password \
    -Dserver.ssl.trustStore="${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.truststore.p12" \
    -Dserver.ssl.trustStoreType=PKCS12 \
    -Dserver.ssl.trustStorePassword=password \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -jar "${FVT_WORKSPACE}/api-layer/discovery-service.jar" \
    > "${FVT_WORKSPACE}/${FVT_LOGS_DIR}/discovery-service.log" &
echo

################################################################################
echo "[${SCRIPT_NAME}] start APIML gateway server"
# -Xquickstart \
java -Xms32m -Xmx256m \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.include=debug \
    -Dapiml.service.hostname=localhost \
    -Dapiml.service.port=${FVT_GATEWAY_PORT} \
    -Dapiml.service.discoveryServiceUrls="https://localhost:${FVT_DISCOVERY_PORT}/eureka/" \
    -Dapiml.service.preferIpAddress=true \
    -Dapiml.service.allowEncodedSlashes=true \
    -Denvironment.ipAddress=127.0.0.1 \
    -Dapiml.gateway.timeoutMillis=30000 \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=false \
    -Dapiml.security.auth.zosmfServiceId=zosmf \
    -Dserver.address=0.0.0.0 \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.keyStore="${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.keystore.p12" \
    -Dserver.ssl.keyStoreType=PKCS12 \
    -Dserver.ssl.keyStorePassword=password \
    -Dserver.ssl.keyAlias=localhost \
    -Dserver.ssl.keyPassword=password \
    -Dserver.ssl.trustStore="${FVT_WORKSPACE}/${FVT_KEYSTORE_DIR}/localhost.truststore.p12" \
    -Dserver.ssl.trustStoreType=PKCS12 \
    -Dserver.ssl.trustStorePassword=password \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -jar "${FVT_WORKSPACE}/api-layer/gateway-service.jar" \
    > "${FVT_WORKSPACE}/${FVT_LOGS_DIR}/gateway-service.log"  &
echo

################################################################################
echo "[${SCRIPT_NAME}] done."
exit 0
