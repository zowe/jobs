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

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.utils.ResponseCache;
import org.zowe.jobs.model.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class SubmitJobStringZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<Job> {

    private String jcl;

    public SubmitJobStringZosmfRequestRunner(String jcl) {
        this.jcl = jcl;
    }

    @Override
    RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException, IOException {
        String urlPath = String.format("restjobs/jobs"); // $NON-NLS-1
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        StringEntity stringEntity = new StringEntity(jcl);
        RequestBuilder requestBuilder = RequestBuilder.put(requestUrl).setEntity(stringEntity);
        requestBuilder.addHeader("X-IBM-Intrdr-Class", "A");
        requestBuilder.addHeader("X-IBM-Intrdr-Recfm", "F");
        requestBuilder.addHeader("X-IBM-Intrdr-Lrecl", "80");
        requestBuilder.addHeader("X-IBM-Intrdr-Mode", "TEXT");
        requestBuilder.addHeader("Content-type", ContentType.TEXT_PLAIN.getMimeType());
        return requestBuilder;
    }

    @Override
    int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_CREATED };
    }

    @Override
    Job getResult(ResponseCache responseCache) throws IOException {
        return getJobFromJson(responseCache.getEntityAsJsonObject());
    }
}
