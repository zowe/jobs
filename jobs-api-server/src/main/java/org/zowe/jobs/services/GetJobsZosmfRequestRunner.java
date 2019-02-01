package org.zowe.jobs.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.InvalidPrefixException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GetJobsZosmfRequestRunner extends AbstractZosmfRequestRunner<List<Job>> {

    private JobStatus status;
    private String queryPrefix;
    private String queryOwner;

    public GetJobsZosmfRequestRunner(JobStatus status, String queryPrefix, String queryOwner) {
        this.status = status;
        this.queryPrefix = queryPrefix;
        this.queryOwner = queryOwner;
    }

    @Override
    public List<Job> getResult(HttpResponse response) throws IOException {
        List<Job> jobs = new ArrayList<>();
        JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
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
        return jobs;
    }

    @Override
    public ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if ("Value of prefix query parameter is not valid".equals(zosmfMessage)) {
                    return new InvalidPrefixException(queryPrefix);
                } else if ("Value of owner query parameter is not valid".equals(zosmfMessage)) {
                    return new InvalidOwnerException(queryOwner);
                }
            }
        }
        return null;
    }

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
