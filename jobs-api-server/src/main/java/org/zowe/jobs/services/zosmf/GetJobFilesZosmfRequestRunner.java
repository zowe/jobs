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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.api.common.utils.ResponseCache;
import org.zowe.jobs.model.JobFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GetJobFilesZosmfRequestRunner extends AbstractZosmfJobsRequestRunner<ItemsWrapper<JobFile>> {

    private String jobName;
    private String jobId;

    public GetJobFilesZosmfRequestRunner(String jobName, String jobId) {
        super(null);
        this.jobName = jobName;
        this.jobId = jobId;
    }

    @Override
    protected RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException {
        String urlPath = String.format("restjobs/jobs/%s/%s/files", jobName, jobId); //$NON-NLS-1$
        URI requestUrl = zosmfConnector.getFullUrl(urlPath);
        return RequestBuilder.get(requestUrl);
    }

    @Override
    protected int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_OK };
    }

    @Override
    protected ItemsWrapper<JobFile> getResult(ResponseCache responseCache) throws IOException {
        JsonElement jsonResponse = responseCache.getEntityAsJson();
        List<JobFile> jobFiles = new ArrayList<>();
        for (JsonElement jsonElement : jsonResponse.getAsJsonArray()) {
            jobFiles.add(getJobFileFromJson(jsonElement.getAsJsonObject()));
        }
        return new ItemsWrapper<JobFile>(jobFiles);
    }

    @Override
    protected ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        return createJobNotFoundExceptions(jsonResponse, statusCode, jobName, jobId);
    }

    private static JobFile getJobFileFromJson(JsonObject returned) {
        return JobFile.builder().id(returned.get("id").getAsLong()).ddName(returned.get("ddname").getAsString())
            .recordFormat(returned.get("recfm").getAsString()).recordLength(returned.get("lrecl").getAsLong())
            .byteCount(returned.get("byte-count").getAsLong()).recordCount(returned.get("record-count").getAsLong())
            .build();
    }
}
