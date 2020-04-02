#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2019
################################################################################

# Add static definition for jobs-api
cat <<EOF >${STATIC_DEF_CONFIG_DIR}/jobs-api.ebcidic.yml
#
services:
  - serviceId: jobs
    title: IBM z/OS Jobs
    description: IBM z/OS Jobs REST API service
    catalogUiTileId: jobs
    instanceBaseUrls:
      - https://${ZOWE_EXPLORER_HOST}:${JOBS_API_PORT}/
    homePageRelativeUrl:
    routedServices:
      - gatewayUrl: api/v1
        serviceRelativeUrl: api/v1/jobs
      - gatewayUrl: api/v2
        serviceRelativeUrl: api/v2/jobs
    apiInfo:
      - apiId: com.ibm.jobs.v1
        gatewayUrl: api/v1
        version: 1.0.0
        swaggerUrl: https://${ZOWE_EXPLORER_HOST}:${JOBS_API_PORT}/v2/api-docs
        documentationUrl: https://${ZOWE_EXPLORER_HOST}:${JOBS_API_PORT}/swagger-ui.html
      - apiId: com.ibm.jobs.v2
        gatewayUrl: api/v2
        version: 2.0.0
        swaggerUrl: https://${ZOWE_EXPLORER_HOST}:${JOBS_API_PORT}/v2/api-docs
        documentationUrl: https://${ZOWE_EXPLORER_HOST}:${JOBS_API_PORT}/swagger-ui.html
catalogUiTiles:
  jobs:
    title: z/OS Jobs services
    description: IBM z/OS Jobs REST services
EOF
iconv -f IBM-1047 -t IBM-850 ${STATIC_DEF_CONFIG_DIR}/jobs-api.ebcidic.yml > $STATIC_DEF_CONFIG_DIR/jobs-api.yml
rm ${STATIC_DEF_CONFIG_DIR}/jobs-api.ebcidic.yml
chmod 770 $STATIC_DEF_CONFIG_DIR/jobs-api.yml