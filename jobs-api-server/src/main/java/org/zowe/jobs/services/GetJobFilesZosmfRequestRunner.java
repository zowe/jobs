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
import org.zowe.jobs.model.JobFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GetJobFilesZosmfRequestRunner extends AbstractZosmfRequestRunner<List<JobFile>> {

    private String jobName;
    private String jobId;

    public GetJobFilesZosmfRequestRunner(String jobName, String jobId) {
        this.jobName = jobName;
        this.jobId = jobId;
    }

    @Override
    public List<JobFile> getResult(HttpResponse response) throws IOException {
        JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
        List<JobFile> jobFiles = new ArrayList<>();
        for (JsonElement jsonElement : jsonResponse.getAsJsonArray()) {
            jobFiles.add(getJobFileFromJson(jsonElement.getAsJsonObject()));
        }
        return jobFiles;
    }

    @Override
    public ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId).equals(zosmfMessage)) {
                    return new JobNameNotFoundException(jobName, jobId);
                }
            }
        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            if (jsonResponse.has("message")) {
                String zosmfMessage = jsonResponse.get("message").getAsString();
                if (String.format("Failed to lookup job %s(%s)", jobName, jobId).equals(zosmfMessage)) {
                    return new JobIdNotFoundException(jobName, jobId);
                }
            }
        }
        return null;
    }

    private static JobFile getJobFileFromJson(JsonObject returned) {
        return JobFile.builder().id(returned.get("id").getAsLong()).ddName(returned.get("ddname").getAsString())
            .recordFormat(returned.get("recfm").getAsString()).recordLength(returned.get("lrecl").getAsLong())
            .byteCount(returned.get("byte-count").getAsLong()).recordCount(returned.get("record-count").getAsLong())
            .build();
    }
}
