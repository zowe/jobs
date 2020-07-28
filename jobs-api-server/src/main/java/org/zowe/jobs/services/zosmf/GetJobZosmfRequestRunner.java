/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019, 2020
 */
package org.zowe.jobs.services.zosmf;

import com.google.gson.JsonObject;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseCache;
import org.zowe.jobs.model.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class GetJobZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<Job> {

    private String jobName;
    private String jobId;

    public GetJobZosmfRequestRunner(List<Header> headers, String jobName, String jobId) {
        super(headers);
        this.jobName = jobName;
        this.jobId = jobId;
    }

    @Override
    protected RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        return RequestBuilder.get(requestUrl);
    }

    @Override
    protected int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_OK };
    }

    @Override
    protected Job getResult(ResponseCache responseCache) throws IOException {
        return getJobFromJson(responseCache.getEntityAsJsonObject());
    }

    @Override
    protected ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        return createJobNotFoundExceptions(jsonResponse, statusCode, jobName, jobId);
    }
}
