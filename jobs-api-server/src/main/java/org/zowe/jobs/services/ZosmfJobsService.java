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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.connectors.zosmf.exceptions.DataSetNotFoundException;
import org.zowe.api.common.exceptions.NoZosmfResponseEntityException;
import org.zowe.api.common.exceptions.ServerErrorException;
import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.InvalidPrefixException;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
        String queryPrefix = "*"; //$NON-NLS-1$
        String queryOwner = "*"; //$NON-NLS-1$

        if (prefix != null) {
            queryPrefix = prefix;
        }
        if (owner != null) {
            queryOwner = owner;
        }

        String query = String.format("owner=%s&prefix=%s", queryOwner, queryPrefix); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl("restjobs/jobs", query);
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_OK) {
                return createJobsList(status, response);
            } else {
                throw createGetJobsException(queryPrefix, queryOwner, requestUrl, response, statusCode);
            }
        } catch (IOException | URISyntaxException e) {
            log.error("getJobs", e);
            throw new ServerErrorException(e);
        }
    }

    private List<Job> createJobsList(JobStatus status, HttpResponse response) throws IOException {
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

    private ZoweApiRestException createGetJobsException(String queryPrefix, String queryOwner, URI requestUrl,
            HttpResponse response, int statusCode) throws IOException {

        ZoweApiRestExceptionReturner jobsException = new ZoweApiRestExceptionReturner() {
            @Override
            public ZoweApiRestException run(JsonObject jsonResponse) {
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
        };

        return createGeneralException(requestUrl, response, statusCode, jobsException);
    }

    @Override
    public Job getJob(String jobName, String jobId) {

        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_OK) {
                JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
                return getJobFromJson(jsonResponse.getAsJsonObject());
            } else {
                throw createGetJobException(jobName, jobId, requestUrl, response, statusCode);
            }
        } catch (IOException | URISyntaxException e) {
            log.error("getJob", e);
            throw new ServerErrorException(e);
        }
    }

    private ZoweApiRestException createGetJobException(String jobName, String jobId, URI requestUrl,
            HttpResponse response, int statusCode) throws IOException {

        ZoweApiRestExceptionReturner jobException = new ZoweApiRestExceptionReturner() {
            @Override
            public ZoweApiRestException run(JsonObject jsonResponse) {
                if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    if (jsonResponse.has("message")) {
                        String zosmfMessage = jsonResponse.get("message").getAsString();
                        if (String.format("No job found for reference: '%s(%s)'", jobName, jobId)
                            .equals(zosmfMessage)) {
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
        };
        return createGeneralException(requestUrl, response, statusCode, jobException);
    }

    @Override
    public Job submitJobString(String jcl) {
        String urlPath = String.format("restjobs/jobs"); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            RequestBuilder requestBuilder = RequestBuilder.put(requestUrl).setEntity(new StringEntity(jcl));
            requestBuilder.addHeader("X-IBM-Intrdr-Class", "A");
            requestBuilder.addHeader("X-IBM-Intrdr-Recfm", "F");
            requestBuilder.addHeader("X-IBM-Intrdr-Lrecl", "80");
            requestBuilder.addHeader("X-IBM-Intrdr-Mode", "TEXT");
            // requestBuilder.addHeader("Accept", ContentType.TEXT_PLAIN.getMimeType());
            requestBuilder.addHeader("Content-type", ContentType.TEXT_PLAIN.getMimeType());

            HttpResponse response = zosmfconnector.request(requestBuilder);
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_CREATED) {
                JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
                return getJobFromJson(jsonResponse.getAsJsonObject());
            } else {
                throw createGeneralException(requestUrl, response, statusCode, createEmptyExceptionReturner());
            }
        } catch (IOException | URISyntaxException e) {
            log.error("submitJobString", e);
            throw new ServerErrorException(e);
        }
    }

    private ZoweApiRestExceptionReturner createEmptyExceptionReturner() {
        return new ZoweApiRestExceptionReturner() {
            @Override
            public ZoweApiRestException run(JsonObject jsonResponse) {
                return null;
            }
        };
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
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_CREATED) {
                JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
                return getJobFromJson(jsonResponse.getAsJsonObject());
            } else {
                throw createSubmitJobDataSetException(dataSet, requestUrl, response, statusCode);
            }
        } catch (IOException | URISyntaxException e) {
            log.error("submitJobFile", e);
            throw new ServerErrorException(e);
        }
    }

    private ZoweApiRestException createSubmitJobDataSetException(String dataSet, URI requestUrl, HttpResponse response,
            int statusCode) throws IOException {

        ZoweApiRestExceptionReturner submitException = new ZoweApiRestExceptionReturner() {
            @Override
            public ZoweApiRestException run(JsonObject jsonResponse) {
                if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    if (jsonResponse.has("message")) {
                        String zosmfMessage = jsonResponse.get("message").getAsString();
                        if (String.format("Data set not found: %s", dataSet).equals(zosmfMessage)) {
                            throw new DataSetNotFoundException(dataSet);
                        }
                    }
                } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    if (jsonResponse.has("message")) {
                        String zosmfMessage = jsonResponse.get("message").getAsString();
                        if (String.format("Error opening input data set: //'%s'", dataSet).equals(zosmfMessage)) {
                            throw new DataSetNotFoundException(dataSet);
                        }
                    }
                }
                return null;
            }
        };
        return createGeneralException(requestUrl, response, statusCode, submitException);
    }

    @Override
    public void purgeJob(String jobName, String jobId) {
        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            HttpResponse response = zosmfconnector.request(RequestBuilder.delete(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode != HttpStatus.SC_ACCEPTED) {
                throw createPurgeJobException(jobName, jobId, requestUrl, response, statusCode);
            }
        } catch (IOException | URISyntaxException e) {
            log.error("purgeJob", e);
            throw new ServerErrorException(e);
        }
    }

    private ZoweApiRestException createPurgeJobException(String jobName, String jobId, URI requestUrl,
            HttpResponse response, int statusCode) throws IOException {

        ZoweApiRestExceptionReturner jobsException = new ZoweApiRestExceptionReturner() {
            @Override
            public ZoweApiRestException run(JsonObject jsonResponse) {
                if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    if (jsonResponse.has("message")) {
                        String zosmfMessage = jsonResponse.get("message").getAsString();
                        if (String.format("No job found for reference: '%s(%s)'", jobName, jobId)
                            .equals(zosmfMessage)) {
                            return new JobNameNotFoundException(jobName, jobId);
                        }
                    }
                }
                return null;
            }
        };
        return createGeneralException(requestUrl, response, statusCode, jobsException);
    }

    @Override
    public List<JobFile> getJobFiles(String jobName, String jobId) {
        String urlPath = String.format("restjobs/jobs/%s/%s/files", jobName, jobId); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_OK) {
                JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
                List<JobFile> jobFiles = new ArrayList<>();
                for (JsonElement jsonElement : jsonResponse.getAsJsonArray()) {
                    jobFiles.add(getJobFileFromJson(jsonElement.getAsJsonObject()));
                }
                return jobFiles;
            } else {
                throw createGetJobFilesException(jobName, jobId, requestUrl, response, statusCode);
            }
        } catch (IOException | URISyntaxException e) {
            log.error("getJob", e);
            throw new ServerErrorException(e);
        }
    }

    private ZoweApiRestException createGetJobFilesException(String jobName, String jobId, URI requestUrl,
            HttpResponse response, int statusCode) throws IOException {
        ZoweApiRestExceptionReturner jobsException = new ZoweApiRestExceptionReturner() {
            @Override
            public ZoweApiRestException run(JsonObject jsonResponse) {
                if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    if (jsonResponse.has("message")) {
                        String zosmfMessage = jsonResponse.get("message").getAsString();
                        if (String.format("No job found for reference: '%s(%s)'", jobName, jobId)
                            .equals(zosmfMessage)) {
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
        };

        return createGeneralException(requestUrl, response, statusCode, jobsException);
    }

    @Override
    public JobFileContent getJobFileContent(String jobName, String jobId, String fileId) {
        String urlPath = String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId); //$NON-NLS-1$
        try {
            URI requestUrl = zosmfconnector.getFullUrl(urlPath);
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_OK) {
                return new JobFileContent(ResponseUtils.getEntity(response));
            } else {
                throw createGetJobFileContentException(jobName, jobId, fileId, requestUrl, response, statusCode);
            }
        } catch (IOException | URISyntaxException e) {
            log.error("getJobFileContent", e);
            throw new ServerErrorException(e);
        }
    }

    private ZoweApiRestException createGetJobFileContentException(String jobName, String jobId, String fileId,
            URI requestUrl, HttpResponse response, int statusCode) throws IOException {

        ZoweApiRestExceptionReturner jobsException = new ZoweApiRestExceptionReturner() {
            @Override
            public ZoweApiRestException run(JsonObject jsonResponse) {
                if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    if (jsonResponse.has("message")) {
                        String zosmfMessage = jsonResponse.get("message").getAsString();
                        if (String.format("No job found for reference: '%s(%s)'", jobName, jobId)
                            .equals(zosmfMessage)) {
                            return new JobNameNotFoundException(jobName, jobId);
                        } else if (String
                            .format("Job '%s(%s)' does not contain spool file id %s", jobName, jobId, fileId)
                            .equals(zosmfMessage)) {
                            return new JobFileIdNotFoundException(jobName, jobId, fileId);
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
        };
        return createGeneralException(requestUrl, response, statusCode, jobsException);
    }

    private ZoweApiRestException createGeneralException(URI requestUrl, HttpResponse response, int statusCode,
            ZoweApiRestExceptionReturner returner) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            ContentType contentType = ContentType.get(entity);
            String mimeType = contentType.getMimeType();
            if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);
                ZoweApiRestException exception = returner.run(jsonResponse);
                if (exception != null) {
                    return exception;
                }
                if (jsonResponse.has("message")) {
                    String zosmfMessage = jsonResponse.get("message").getAsString();
                    return new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                }
                return new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), jsonResponse.toString());
            } else {
                return new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
            }
        } else {
            return new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), requestUrl.toString());
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

    private static JobFile getJobFileFromJson(JsonObject returned) {
        return JobFile.builder().id(returned.get("id").getAsLong()).ddName(returned.get("ddname").getAsString())
            .recordFormat(returned.get("recfm").getAsString()).recordLength(returned.get("lrecl").getAsLong())
            .byteCount(returned.get("byte-count").getAsLong()).recordCount(returned.get("record-count").getAsLong())
            .build();
    }

    private static String getStringOrNull(JsonObject json, String key) {
        String value = null;
        JsonElement jsonElement = json.get(key);
        if (!jsonElement.isJsonNull()) {
            value = jsonElement.getAsString();
        }
        return value;
    }

    // TODO LATER - push up into common once created
    private org.springframework.http.HttpStatus getSpringHttpStatusFromCode(int statusCode) {
        return org.springframework.http.HttpStatus.resolve(statusCode);
    }
}
