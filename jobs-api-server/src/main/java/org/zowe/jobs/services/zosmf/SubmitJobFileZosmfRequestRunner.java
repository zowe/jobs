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
import org.zowe.api.common.connectors.zosmf.exceptions.DataSetNotFoundException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseCache;
import org.zowe.jobs.model.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class SubmitJobFileZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<Job> {

    private String fileName;

    public SubmitJobFileZosmfRequestRunner(String fileName, List<Header> headers) {
        super(headers);
        this.fileName = fileName;
    }

    @Override
    protected int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_CREATED };
    }

    @Override
    protected RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs"); //$NON-NLS-1$
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        JsonObject body = new JsonObject();
        body.addProperty("file", "//'" + fileName + "'");

        StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
        RequestBuilder requestBuilder = RequestBuilder.put(requestUrl).setEntity(requestEntity);
        return requestBuilder;
    }

    @Override
    protected Job getResult(ResponseCache responseCache) throws IOException {
        return getJobFromJson(responseCache.getEntityAsJsonObject());
    }

    @Override
    protected ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("Data set not found: %s", fileName).equals(zosmfMessage)) {
                    throw new DataSetNotFoundException(fileName);
                }
            }
        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("Error opening input data set: //'%s'", fileName).equals(zosmfMessage)) {
                    throw new DataSetNotFoundException(fileName);
                }
            }
        }
        return null;
    }
}
