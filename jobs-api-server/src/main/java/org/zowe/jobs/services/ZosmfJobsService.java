/**
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
import org.zowe.jobs.exceptions.BadRequestException;
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

        String urlPath = String.format("restjobs/jobs?owner=%s&prefix=%s", queryOwner, queryPrefix); //$NON-NLS-1$
        String requestUrl = zosmfconnector.getFullUrl(urlPath);
        List<Job> jobs = new ArrayList<>();
        try {
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_OK) {
                JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);

                for (JsonElement jsonElement : jsonResponse.getAsJsonArray()) {
                    Job job = getJobFromJson(jsonElement.getAsJsonObject());
                    if (status.matches(job.getStatus())) {
                        jobs.add(job);
                    }
                }
            } else {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    ContentType contentType = ContentType.get(entity);
                    String mimeType = contentType.getMimeType();
                    if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                        JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);
                        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if ("Value of prefix query parameter is not valid".equals(zosmfMessage)) {
                                    throw new InvalidPrefixException(queryPrefix);
                                } else if ("Value of owner query parameter is not valid".equals(zosmfMessage)) {
                                    throw new InvalidOwnerException(queryOwner);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                        } else {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                // TODO MAYBE - wrap these exceptions with our own?
                                throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                            }
                            // TODO LATER - improve this if we ever hit
                            throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode),
                                    jsonResponse.toString());
                        }
                    } else {
                        throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
                    }
                } else {
                    throw new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), urlPath);
                }
            }
        } catch (IOException e) {
            log.error("getJobs", e);
            throw new ServerErrorException(e);
        }
        return jobs;
    }

    @Override
    public Job getJob(String jobName, String jobId) {

        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        String requestUrl = zosmfconnector.getFullUrl(urlPath);
        Job job = null;
        try {
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_OK) {
                JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
                job = getJobFromJson(jsonResponse.getAsJsonObject());
            } else {
                HttpEntity entity = response.getEntity();
                // TODO - work out how to tidy when brain is sharper
                if (entity != null) {
                    ContentType contentType = ContentType.get(entity);
                    String mimeType = contentType.getMimeType();
                    if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                        JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);
                        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId)
                                        .equals(zosmfMessage)) {
                                    throw new JobNameNotFoundException(jobName, jobId);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("Failed to lookup job %s(%s)", jobName, jobId).equals(zosmfMessage)) {
                                    throw new JobIdNotFoundException(jobName, jobId);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                        } else {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                // TODO MAYBE - wrap these exceptions with our own?
                                throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                            }
                            // TODO LATER - improve this if we ever hit
                            throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode),
                                    jsonResponse.toString());
                        }
                    } else {
                        throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
                    }
                } else {
                    throw new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), urlPath);
                }
            }
        } catch (IOException e) {
            log.error("getJob", e);
            throw new ServerErrorException(e);
        }
        return job;
    }

    @Override
    public Job submitJobString(String jcl) {
        String urlPath = String.format("restjobs/jobs"); //$NON-NLS-1$

        String requestUrl = zosmfconnector.getFullUrl(urlPath);
        Job job = null;
        try {

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
                job = getJobFromJson(jsonResponse.getAsJsonObject());
            } else {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    ContentType contentType = ContentType.get(entity);
                    String mimeType = contentType.getMimeType();
                    if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                        JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);
                        // TODO MAYBE - wrap these exceptions with our own?
                        if (jsonResponse.has("message")) {
                            String zosmfMessage = jsonResponse.get("message").getAsString();
                            throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                        }
                        // TODO LATER - improve this if we ever hit
                        throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode),
                                jsonResponse.toString());
                    } else {
                        throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
                    }
                } else {
                    throw new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), urlPath);
                }
            }
        } catch (IOException e) {
            log.error("submitJobString", e);
            throw new ServerErrorException(e);
        }
        return job;
    }

    @Override
    public Job submitJobFile(String dataSet) {
        String urlPath = String.format("restjobs/jobs"); //$NON-NLS-1$

        String requestUrl = zosmfconnector.getFullUrl(urlPath);
        Job job = null;
        try {

            JsonObject body = new JsonObject();
            body.addProperty("file", "//'" + dataSet + "'");

            StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
            RequestBuilder requestBuilder = RequestBuilder.put(requestUrl).setEntity(requestEntity);

            HttpResponse response = zosmfconnector.request(requestBuilder);
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_CREATED) {
                JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);
                job = getJobFromJson(jsonResponse.getAsJsonObject());
            } else {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    ContentType contentType = ContentType.get(entity);
                    String mimeType = contentType.getMimeType();
                    if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                        JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);
                        // TODO MAYBE - wrap these exceptions with our own?
                        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("Data set not found: %s", dataSet).equals(zosmfMessage)) {
                                    throw new DataSetNotFoundException(dataSet);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("Error opening input data set: //'%s'", dataSet)
                                        .equals(zosmfMessage)) {
                                    throw new DataSetNotFoundException(dataSet);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                        } else {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                // TODO MAYBE - wrap these exceptions with our own?
                                throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                            }
                            // TODO LATER - improve this if we ever hit
                            throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode),
                                    jsonResponse.toString());
                        }
                    } else {
                        throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
                    }
                } else {
                    throw new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), urlPath);
                }
            }
        } catch (IOException e) {
            log.error("submitJobFile", e);
            throw new ServerErrorException(e);
        }
        return job;
    }

    @Override
    public void purgeJob(String jobName, String jobId) {
        String urlPath = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
        String requestUrl = zosmfconnector.getFullUrl(urlPath);
        try {
            HttpResponse response = zosmfconnector.request(RequestBuilder.delete(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode != HttpStatus.SC_ACCEPTED) {
                HttpEntity entity = response.getEntity();
                // TODO - work out how to tidy when brain is sharper
                if (entity != null) {
                    ContentType contentType = ContentType.get(entity);
                    String mimeType = contentType.getMimeType();
                    if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                        JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);
                        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId)
                                        .equals(zosmfMessage)) {
                                    throw new JobNameNotFoundException(jobName, jobId);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                        } else {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                // TODO MAYBE - wrap these exceptions with our own?
                                throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                            }
                            // TODO LATER - improve this if we ever hit
                            throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode),
                                    jsonResponse.toString());
                        }
                    } else {
                        throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
                    }
                } else {
                    throw new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), urlPath);
                }
            }
        } catch (IOException e) {
            log.error("purgeJob", e);
            throw new ServerErrorException(e);
        }
    }

    @Override
    public List<JobFile> getJobFiles(String jobName, String jobId) {
        String urlPath = String.format("restjobs/jobs/%s/%s/files", jobName, jobId); //$NON-NLS-1$
        String requestUrl = zosmfconnector.getFullUrl(urlPath);
        List<JobFile> jobFiles = new ArrayList<>();
        try {
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_OK) {
                JsonElement jsonResponse = ResponseUtils.getEntityAsJson(response);

                for (JsonElement jsonElement : jsonResponse.getAsJsonArray()) {
                    jobFiles.add(getJobFileFromJson(jsonElement.getAsJsonObject()));
                }
            } else {
                HttpEntity entity = response.getEntity();
                // TODO - work out how to tidy when brain is sharper
                if (entity != null) {
                    ContentType contentType = ContentType.get(entity);
                    String mimeType = contentType.getMimeType();
                    if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                        JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);
                        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId)
                                        .equals(zosmfMessage)) {
                                    throw new JobNameNotFoundException(jobName, jobId);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("Failed to lookup job %s(%s)", jobName, jobId).equals(zosmfMessage)) {
                                    throw new JobIdNotFoundException(jobName, jobId);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else {
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                            }
                        } else {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                // TODO MAYBE - wrap these exceptions with our own?
                                throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                            }
                            // TODO LATER - improve this if we ever hit
                            throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode),
                                    jsonResponse.toString());
                        }
                    } else {
                        throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
                    }
                } else {
                    throw new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), urlPath);
                }
            }
        } catch (IOException e) {
            log.error("getJob", e);
            throw new ServerErrorException(e);
        }
        return jobFiles;
    }

    @Override
    public JobFileContent getJobFileContent(String jobName, String jobId, String fileId) {
        String urlPath = String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId); //$NON-NLS-1$
        String requestUrl = zosmfconnector.getFullUrl(urlPath);
        JobFileContent jobFileContent;
        try {
            HttpResponse response = zosmfconnector.request(RequestBuilder.get(requestUrl));
            int statusCode = ResponseUtils.getStatus(response);
            if (statusCode == HttpStatus.SC_OK) {
                jobFileContent = new JobFileContent(ResponseUtils.getEntity(response));
            } else {
                HttpEntity entity = response.getEntity();
                // TODO - work out how to tidy when brain is sharper
                if (entity != null) {
                    ContentType contentType = ContentType.get(entity);
                    String mimeType = contentType.getMimeType();
                    if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                        JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);
                        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId)
                                        .equals(zosmfMessage)) {
                                    throw new JobNameNotFoundException(jobName, jobId);
                                } else if (String.format("Job '%s(%s)' does not contain spool file id %s", jobName,
                                        jobId, fileId).equals(zosmfMessage)) {
                                    throw new JobFileIdNotFoundException(jobName, jobId, fileId);
                                } else {
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                                }
                            } else {
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                            }
                        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                if (String.format("Failed to lookup job %s(%s)", jobName, jobId).equals(zosmfMessage)) {
                                    throw new JobIdNotFoundException(jobName, jobId);
                                } else
                                    // TODO LATER - improve this if we ever hit
                                    throw new BadRequestException(zosmfMessage);
                            } else
                                // TODO LATER - improve this if we ever hit
                                throw new BadRequestException(jsonResponse.toString());
                        } else {
                            if (jsonResponse.has("message")) {
                                String zosmfMessage = jsonResponse.get("message").getAsString();
                                // TODO MAYBE - wrap these exceptions with our own?
                                throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                            }
                            // TODO LATER - improve this if we ever hit
                            throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode),
                                    jsonResponse.toString());
                        }
                    } else {
                        throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
                    }
                } else {
                    throw new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), urlPath);
                }
            }
        } catch (IOException e) {
            log.error("getJobFileContent", e);
            throw new ServerErrorException(e);
        }
        return jobFileContent;
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
        return JobFile.builder().id(returned.get("id").getAsLong()).ddname(returned.get("ddname").getAsString())
                .recfm(returned.get("recfm").getAsString()).lrecl(returned.get("lrecl").getAsLong())
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
