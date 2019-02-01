package org.zowe.jobs.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class SubmitJobStringZosmfRequestRunner extends AbstractZosmfRequestRunner<Job> {

    private String jcl;

    public SubmitJobStringZosmfRequestRunner(String jcl) {
        this.jcl = jcl;
    }

    @Override
    RequestBuilder prepareQuery(ZosmfConnector zosmfconnector) throws URISyntaxException, IOException {
        String urlPath = String.format("restjobs/jobs"); // $NON-NLS-1
        URI requestUrl = zosmfconnector.getFullUrl(urlPath);
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
    // TODO - dup of getJob
    Job getResult(HttpResponse response) throws IOException {
        JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
        return getJobFromJson(jsonResponse.getAsJsonObject());
    }

    // TODO - dup from getJobs, getJob
    private static Job getJobFromJson(JsonObject returned) {
        return Job.builder().jobId(returned.get("jobid").getAsString()) //$NON-NLS-1$
            .jobName(returned.get("jobname").getAsString()) //$NON-NLS-1$
            .owner(returned.get("owner").getAsString()) //$NON-NLS-1$
            .type(returned.get("type").getAsString()) //$NON-NLS-1$
            .status(JobStatus.valueOf(returned.get("status").getAsString())) //$NON-NLS-1$
            .returnCode(getStringOrNull(returned, "retcode")) //$NON-NLS-1$
            .subsystem(returned.get("subsystem").getAsString()) //$NON-NLS-1$
            .executionClass(returned.get("class").getAsString()) //$NON-NLS-1$
            .phaseName(returned.get("phase-name").getAsString()) //$NON-NLS-1$
            .build();
    }
}
