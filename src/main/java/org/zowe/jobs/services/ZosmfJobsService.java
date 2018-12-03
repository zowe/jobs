package org.zowe.jobs.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ServerErrorException;
import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.exceptions.BadRequestException;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.InvalidPrefixException;
import org.zowe.jobs.exceptions.NoZosmfResponseEntityException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ZosmfJobsService implements JobsService {

	@Autowired
	ZosmfConnector zosmfconnector;

	// TODO - review error handling, serviceability

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
									// TODO - improve this if we ever hit
									throw new BadRequestException(zosmfMessage);
							} else
								// TODO - improve this if we ever hit
								throw new BadRequestException(jsonResponse.toString());
						} else {
							if (jsonResponse.has("message")) {
								String zosmfMessage = jsonResponse.get("message").getAsString();
								// TODO - improve this if we ever hit
								throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
							}
							// TODO - improve this if we ever hit
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

//	@Override
//	public Job getJob(String jobName, String jobId) {
//		return null;
//	}
//

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
			requestBuilder.addHeader("Accept", ContentType.TEXT_PLAIN.getMimeType());
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
						/*
						 * if (statusCode == HttpStatus.SC_BAD_REQUEST) { if
						 * (jsonResponse.has("message")) { String zosmfMessage =
						 * jsonResponse.get("message").getAsString(); if
						 * ("Value of prefix query parameter is not valid".equals(zosmfMessage)) { throw
						 * new InvalidPrefixException(queryPrefix); } else if
						 * ("Value of owner query parameter is not valid".equals(zosmfMessage)) { throw
						 * new InvalidOwnerException(queryOwner); } else // TODO - improve this if we
						 * ever hit throw new BadRequestException(zosmfMessage); } else // TODO -
						 * improve this if we ever hit throw new
						 * BadRequestException(jsonResponse.toString()); } else {
						 */
						if (jsonResponse.has("message")) {
							String zosmfMessage = jsonResponse.get("message").getAsString();
							// TODO - improve this if we ever hit
							throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
						}
						// TODO - improve this if we ever hit
						throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode),
								jsonResponse.toString());
					} else {
						throw new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
					}
				} else {
					throw new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), urlPath);
				}
			}
		} catch (

		IOException e) {
			// TODO - error handle
			e.printStackTrace();
		}
		return job;
	}

//	@Override
//	public void purgeJob(String jobName, String jobId) {
//	}
//
//	@Override
//	public Job submitJob(String file) {
//		return null;
//	}
//
//	@Override
//	public List<JobFile> getJobFiles(String jobName, String jobId) {
//		return null;
//	}
//
//	@Override
//	public OutputFile getJobFileRecordsByRange(String jobName, String jobId, String fileId, Integer start,
//			Integer end) {
//		return null;
//	}
//
//	@Override
//	public OutputFile getJobJcl(String jobName, String jobId) {
//		return null;
//	}

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
