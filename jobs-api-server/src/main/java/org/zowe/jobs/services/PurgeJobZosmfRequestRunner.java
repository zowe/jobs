package org.zowe.jobs.services;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;

import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
public class PurgeJobZosmfRequestRunner extends AbstractZosmfRequestRunner<Void> {

    private String jobName;
    private String jobId;

    public PurgeJobZosmfRequestRunner(String jobName, String jobId) {
        this.jobName = jobName;
        this.jobId = jobId;
    }

    @Override
    int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_ACCEPTED };
    }

    @Override
    RequestBuilder prepareQuery(ZosmfConnector zosmfconnector) throws URISyntaxException {
        return null;
    }

    @Override
    public Void getResult(HttpResponse response) throws IOException {
        return null;
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
        }
        return null;
    }
}
