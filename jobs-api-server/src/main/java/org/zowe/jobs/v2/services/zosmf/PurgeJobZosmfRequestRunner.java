/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */
package org.zowe.jobs.v2.services.zosmf;

import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnectorV2;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseCache;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class PurgeJobZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<Void> {

    private String jobName;
    private String jobId;

    public PurgeJobZosmfRequestRunner(String jobName, String jobId) {
        this.jobName = jobName;
        this.jobId = jobId;
    }

    @Override
    protected int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_ACCEPTED };
    }

    @Override
    protected RequestBuilder prepareQuery(ZosmfConnectorV2 zosmfConnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        return RequestBuilder.delete(requestUrl);
    }

    @Override
    protected Void getResult(ResponseCache responseCache) throws IOException {
        return null;
    }

    @Override
    protected ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        return createJobNotFoundExceptions(jsonResponse, statusCode, jobName, jobId);
    }
}
