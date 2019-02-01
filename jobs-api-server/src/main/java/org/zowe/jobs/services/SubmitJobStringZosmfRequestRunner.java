package org.zowe.jobs.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;

@Slf4j
public class SubmitJobStringZosmfRequestRunner extends AbstractZosmfRequestRunner<Job> {

    @Override
    // TODO - dup of getJob
    public Job getResult(HttpResponse response) throws IOException {
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
