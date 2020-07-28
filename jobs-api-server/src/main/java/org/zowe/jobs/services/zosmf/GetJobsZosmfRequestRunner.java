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

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.api.common.utils.ResponseCache;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.InvalidPrefixException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GetJobsZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<ItemsWrapper<Job>> {

    private JobStatus status;
    private String prefix;
    private String owner;

    public GetJobsZosmfRequestRunner(List<Header> headers, String prefix, String owner, JobStatus status) {
        super(headers);
        this.status = status;
        this.prefix = prefix;
        this.owner = owner;
    }

    @Override
    protected int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_OK };
    }

    @Override
    protected RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException {
        if (prefix == null) {
            prefix = "*";
        }
        String query;
        if (owner == null) {
            query = String.format("prefix=%s", prefix); //$NON-NLS-1$
        } else {
            query = String.format("owner=%s&prefix=%s", owner, prefix); //$NON-NLS-1$
        }
        URI requestUrl = zosmfConnector.getFullUrl("restjobs/jobs", query); //$NON-NLS-1$
        return RequestBuilder.get(requestUrl);
    }

    @Override
    protected ItemsWrapper<Job> getResult(ResponseCache responseCache) throws IOException {
        JsonElement jsonResponse = responseCache.getEntityAsJson();
        List<Job> jobs = new ArrayList<>();
        for (JsonElement jsonElement : jsonResponse.getAsJsonArray()) {
            try {
                Job job = getJobFromJson(jsonElement.getAsJsonObject());
                if (status.matches(job.getStatus())) {
                    jobs.add(job);
                }
            } catch (IllegalArgumentException e) {
                log.error("getJobs", e);
            }
        }
        return new ItemsWrapper<Job>(jobs);
    }

    @Override
    protected ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) throws IOException {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if ("Value of prefix query parameter is not valid".equals(zosmfMessage)) {
                    return new InvalidPrefixException(prefix);
                } else if ("Value of owner query parameter is not valid".equals(zosmfMessage)) {
                    return new InvalidOwnerException(owner);
                }
            }
        }
        return null;
    }
}