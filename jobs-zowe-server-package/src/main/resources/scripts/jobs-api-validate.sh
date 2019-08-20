################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2019
################################################################################

#Configure and start require:
# - ZOWE_PREFIX
# - JOBS_API_PORT - The port the data sets server will use
# - KEY_ALIAS
# - KEYSTORE - The keystore to use for SSL certificates
# - KEYSTORE_PASSWORD - The password to access the keystore supplied by KEYSTORE
# - KEY_ALIAS - The alias of the key within the keystore
# - ZOSMF_PORT - The SSL port z/OSMF is listening on.
# - ZOSMF_IP_ADDRESS - The IP Address z/OSMF can be reached
# - STATIC_DEF_CONFIG_DIR
# - ZOWE_EXPLORER_HOST
# - ZOWE_JAVA_HOME



#Make sure Java is available on the path - TODO needed at all, move to a all zowe setup/validate?
export JAVA_HOME=$ZOWE_JAVA_HOME
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH"
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..."
  export PATH=$PATH:$JAVA_HOME/bin
  echo "Done."
fi