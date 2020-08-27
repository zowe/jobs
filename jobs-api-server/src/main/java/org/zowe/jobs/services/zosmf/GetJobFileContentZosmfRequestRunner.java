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

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseCache;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.model.JobFileContent;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class GetJobFileContentZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<JobFileContent> {

    private String jobName;
    private String jobId;
    private String fileId;

    public GetJobFileContentZosmfRequestRunner(String jobName, String jobId, String fileId, List<Header> headers) {
        super(headers);
        this.jobName = jobName;
        this.jobId = jobId;
        this.fileId = fileId;
    }

    @Override
    protected int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_OK };
    }

    @Override
    protected RequestBuilder prepareQuery(ZosmfConnector zosmfconnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId); //$NON-NLS-1$
        URI requestUrl = zosmfconnector.getFullUrl(urlPath);
        return RequestBuilder.get(requestUrl);
    }

    @Override
    protected JobFileContent getResult(ResponseCache responseCache) throws IOException {
        return new JobFileContent(responseCache.getEntity());
    }

    @Override
    protected ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("Job '%s(%s)' does not contain spool file id %s", jobName, jobId, fileId)
                    .equals(zosmfMessage)) {
                    return new JobFileIdNotFoundException(jobName, jobId, fileId);
                }
            }
        }
        return createJobNotFoundExceptions(jsonResponse, statusCode, jobName, jobId);
    }
}
