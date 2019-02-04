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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class GetJobZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<Job> {

    private String jobName;
    private String jobId;

    public GetJobZosmfRequestRunner(String jobName, String jobId) {
        this.jobName = jobName;
        this.jobId = jobId;
    }

    @Override
    RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        return RequestBuilder.get(requestUrl);
    }

    @Override
    int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_OK };
    }

    @Override
    Job getResult(HttpResponse response) throws IOException {
        JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
        return getJobFromJson(jsonResponse.getAsJsonObject());
    }

    @Override
    ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId).equals(zosmfMessage)) {
                    throw new JobNameNotFoundException(jobName, jobId);
                }
            }
        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("Failed to lookup job %s(%s)", jobName, jobId).equals(zosmfMessage)) {
                    throw new JobIdNotFoundException(jobName, jobId);
                }
            }
        }
        return null;
    }
}
