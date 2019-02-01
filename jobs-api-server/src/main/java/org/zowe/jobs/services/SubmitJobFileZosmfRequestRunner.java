package org.zowe.jobs.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.zowe.api.common.connectors.zosmf.exceptions.DataSetNotFoundException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;

@Slf4j
public class SubmitJobFileZosmfRequestRunner extends AbstractZosmfRequestRunner<Job> {

    private String fileName;

    public SubmitJobFileZosmfRequestRunner(String fileName) {
        this.fileName = fileName;
    }

    @Override
    // TODO - dup of getJob & submitstring
    public Job getResult(HttpResponse response) throws IOException {
        JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
        return getJobFromJson(jsonResponse.getAsJsonObject());
    }

    @Override
    public ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
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

    // TODO - dup from getJobs, getJob & submitstring
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
