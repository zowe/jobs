/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2019
 */
package org.zowe.jobs.services;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ServerErrorException;
import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
@Service
public class ZosmfJobsService implements JobsService {

    @Autowired
    ZosmfConnector zosmfconnector;

    // TODO - review error handling, serviceability, refactor out error handling?
    // TODO - use the zomsf error categories to work out errors
    // https://www.ibm.com/support/knowledgecenter/SSLTBW_2.3.0/com.ibm.zos.v2r3.izua700/IZUHPINFO_API_Error_Categories.htm?

    @Override
    public List<Job> getJobs(String prefix, String owner, JobStatus status) throws ZoweApiException {
        GetJobsZosmfRequestRunner runner = new GetJobsZosmfRequestRunner(prefix, owner, status);
        return runner.run(zosmfconnector);
    }

    @Override
    public Job getJob(String jobName, String jobId) {
        GetJobZosmfRequestRunner runner = new GetJobZosmfRequestRunner(jobName, jobId);
        return runner.run(zosmfconnector);
    }

    @Override
    public Job submitJobString(String jcl) {
        SubmitJobStringZosmfRequestRunner runner = new SubmitJobStringZosmfRequestRunner(jcl);
        return runner.run(zosmfconnector);
    }

    @Override
    public Job submitJobFile(String dataSet) {
        String urlPath = String.format("restjobs/jobs"); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            JsonObject body = new JsonObject();
            body.addProperty("file", "//'" + dataSet + "'");

            StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
            RequestBuilder requestBuilder = RequestBuilder.put(requestUrl).setEntity(requestEntity);

            HttpResponse response = zosmfconnector.request(requestBuilder);
            SubmitJobFileZosmfRequestRunner runner = new SubmitJobFileZosmfRequestRunner(dataSet);
            return runner.processResponse(response, requestUrl);
        } catch (IOException | URISyntaxException e) {
            log.error("submitJobFile", e);
            throw new ServerErrorException(e);
        }
    }

    @Override
    public void purgeJob(String jobName, String jobId) {
        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            HttpResponse response = zosmfconnector.request(RequestBuilder.delete(requestUrl));
            PurgeJobZosmfRequestRunner runner = new PurgeJobZosmfRequestRunner(jobName, jobId);
            runner.processResponse(response, requestUrl);
        } catch (IOException | URISyntaxException e) {
            log.error("purgeJob", e);
            throw new ServerErrorException(e);
        }
    }

    @Override
    public List<JobFile> getJobFiles(String jobName, String jobId) {
        String urlPath = String.format("restjobs/jobs/%s/%s/files", jobName, jobId); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            GetJobFilesZosmfRequestRunner runner = new GetJobFilesZosmfRequestRunner(jobName, jobId);
            return runner.processResponse(response, requestUrl);
        } catch (IOException | URISyntaxException e) {
            log.error("getJobFiles", e);
            throw new ServerErrorException(e);
        }
    }

    @Override
    public JobFileContent getJobFileContent(String jobName, String jobId, String fileId) {
        String urlPath = String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            GetJobFileContentZosmfRequestRunner runner = new GetJobFileContentZosmfRequestRunner(jobName, jobId,
                    fileId);
            return runner.processResponse(response, requestUrl);
        } catch (IOException | URISyntaxException e) {
            log.error("getJobFileContent", e);
            throw new ServerErrorException(e);
        }
    }

    @Override
    public JobFileContent getJobJcl(String jobName, String jobId) {
        try {
            return getJobFileContent(jobName, jobId, "3");
        } catch (JobFileIdNotFoundException e) {
            log.error("getJobJcl", e);
            throw new JobJesjclNotFoundException(jobName, jobId);
        }
    }
}
