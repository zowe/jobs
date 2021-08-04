#######################################################################
# This program and the accompanying materials are made available
# under the terms of the Eclipse Public License v2.0 which
# accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright Contributors to the Zowe Project.
#######################################################################

# base image tag
ARG ZOWE_BASE_IMAGE=latest-ubuntu

FROM zowe-docker-release.jfrog.io/ompzowe/base-jdk:${ZOWE_BASE_IMAGE} AS builder

##################################
# labels
LABEL name="Jobs API" \
      maintainer="jack-tiefeng.jia@ibm.com" \
      vendor="Zowe" \
      version="1.0.10" \
      release="0" \
      summary="IBM z/OS Jobs API service" \
      description="This Zowe component provides RESTful APIs to interact with jobs running on z/OS"

##################################
# environments
ENV ZOWE_COMPONENT_ID=jobs-api

##################################
# switch context
USER zowe
WORKDIR /component

##################################
# copy files
COPY --chown=zowe:zowe component .
COPY --chown=zowe:zowe component/LICENSE /licenses

##################################
# start command
EXPOSE 8080
ENTRYPOINT [ "bin/start.sh" ]
