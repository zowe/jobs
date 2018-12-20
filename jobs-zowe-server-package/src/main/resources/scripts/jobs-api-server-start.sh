# Variables to be replaced:
# - JAVA_SETUP -- sets JAVA_HOME by ZOWE_JAVA_HOME
# - IPADDRESS   -  The IP Address of the system running API Mediation
# - HOSTNAME   -  The hostname of the system running API Mediation (defaults to localhost)
# - DISCOVERY_PORT - The port the discovery service will use
# - CATALOG_PORT - The port the catalog service will use
# - GATEWAY_PORT - The port the gateway service will use
# - VERIFY_CERTIFICATES - true/false - Validation of TLS/SSL certitificates for services

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH"
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..."
  export PATH=$PATH:$JAVA_HOME/bin
  echo "Done."
fi

DIR=`dirname $0`

java -Xms16m -Xmx512m -Dibm.serversocket.recover=true -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp -Xquickstart  -Dserver.port=8443   -Dserver.ssl.keyAlias=locahost   
    -Dserver.ssl.keyStore=**KEYSTORE**   
    -Dserver.ssl.keyStorePassword=**KEYSTORE_PASSWROD**   -Dserver.ssl.keyStoreType=PKCS12   
    -Dzosmf.httpsPort=**ZOSMF_HTTPS_PORT**   
    -Dzosmf.ipAddress=**ZOSMF_IP**
    -jar ../jobs-api-server-0.0.1-SNAPSHOT.jar
