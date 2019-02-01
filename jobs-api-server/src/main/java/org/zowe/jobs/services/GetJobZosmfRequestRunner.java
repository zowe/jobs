package org.zowe.jobs.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;

@Slf4j
public class GetJobZosmfRequestRunner extends AbstractZosmfRequestRunner<Job> {

    private String jobName;
    private String jobId;

    public GetJobZosmfRequestRunner(String jobName, String jobId) {
        this.jobName = jobName;
        this.jobId = jobId;
    }

    @Override
    public Job getResult(HttpResponse response) throws IOException {
        JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
        return getJobFromJson(jsonResponse.getAsJsonObject());
    }

    @Override
    public ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId).equals(zosmfMessage)) {
                    throw new JobNameNotFoundException(jobName, jobId);
                }
            }
        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("Failed to lookup job %s(%s)", jobName, jobId).equals(zosmfMessage)) {
                    throw new JobIdNotFoundException(jobName, jobId);
                }
            }
        }
        return null;
    }

    // TODO - dup in GetJobs
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
