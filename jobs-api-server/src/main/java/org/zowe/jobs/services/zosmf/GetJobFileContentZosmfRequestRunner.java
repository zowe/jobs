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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class GetJobFileContentZosmfRequestRunner extends AbstractZosmfRequestRunner<JobFileContent> {

    private String jobName;
    private String jobId;
    private String fileId;

    public GetJobFileContentZosmfRequestRunner(String jobName, String jobId, String fileId) {
        this.jobName = jobName;
        this.jobId = jobId;
        this.fileId = fileId;
    }

    @Override
    int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_OK };
    }

    @Override
    RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId); //$NON-NLS-1$
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        return RequestBuilder.get(requestUrl);
    }

    @Override
    JobFileContent getResult(HttpResponse response) throws IOException {
        return new JobFileContent(ResponseUtils.getEntity(response));
    }

    // TODO NOW - review the createExceptions to look for common behaviour
    @Override
    ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId).equals(zosmfMessage)) {
                    return new JobNameNotFoundException(jobName, jobId);
                } else if (String.format("Job '%s(%s)' does not contain spool file id %s", jobName, jobId, fileId)
                    .equals(zosmfMessage)) {
                    return new JobFileIdNotFoundException(jobName, jobId, fileId);
                }
            }
        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("Failed to lookup job %s(%s)", jobName, jobId).equals(zosmfMessage)) {
                    return new JobIdNotFoundException(jobName, jobId);
                }
            }
        }
        return null;
    }

    private static JobFile getJobFileFromJson(JsonObject returned) {
        return JobFile.builder().id(returned.get("id").getAsLong()).ddName(returned.get("ddname").getAsString())
            .recordFormat(returned.get("recfm").getAsString()).recordLength(returned.get("lrecl").getAsLong())
            .byteCount(returned.get("byte-count").getAsLong()).recordCount(returned.get("record-count").getAsLong())
            .build();
    }
}
