/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */
package org.zowe.jobs.services.zosmf;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseCache;
import org.zowe.jobs.exceptions.JobNameNotFoundException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class PurgeJobZosmfRequestRunner extends AbstractZosmfRequestRunner<Void> {

    private String jobName;
    private String jobId;

    public PurgeJobZosmfRequestRunner(String jobName, String jobId) {
        this.jobName = jobName;
        this.jobId = jobId;
    }

    @Override
    int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_ACCEPTED };
    }

    @Override
    RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        return RequestBuilder.delete(requestUrl);
    }

    @Override
    Void getResult(ResponseCache responseCache) throws IOException {
        return null;
    }

    @Override
    ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId).equals(zosmfMessage)) {
                    return new JobNameNotFoundException(jobName, jobId);
                }
            }
        }
        return null;
    }
}