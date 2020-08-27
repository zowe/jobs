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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseCache;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ModifyJobZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<Void> {

    private String jobName;
    private String jobId;
    private String command;

    public ModifyJobZosmfRequestRunner(String jobName, String jobId, String command, List<Header> headers) {
        super(headers);
        this.jobName = jobName;
        this.jobId = jobId;
        this.command = command;
    }

    @Override
    protected int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_OK, HttpStatus.SC_ACCEPTED };
    }

    @Override
    protected RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        
        JsonObject body = new JsonObject();
        body.addProperty("request", command); //$NON-NLS-1$
        
        StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
        return RequestBuilder.put(requestUrl).setEntity(requestEntity);
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
