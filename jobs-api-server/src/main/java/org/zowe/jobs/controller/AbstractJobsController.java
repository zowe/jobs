/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation "201"6, 2020
 */
package org.zowe.jobs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.exceptions.JobStepsNotFoundException;
import org.zowe.jobs.model.*;
import org.zowe.jobs.services.JobsService;

import javax.validation.Valid;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class AbstractJobsController {

    abstract JobsService getJobsService();

    @GetMapping(value = "/", produces = {"application/json"})
    @Operation(summary = "Get a list of jobs", operationId = "getJobs", description = "This API returns the a list of jobs for a given prefix and owner.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Ok")})
    public ItemsWrapper<Job> getJobs(
            @Parameter(description = "Job name prefix. If omitted, defaults to '*'.", schema = @Schema(defaultValue = "*")) @Valid @RequestParam(value = "prefix", required = false, defaultValue = "*") String prefix,
            @Parameter(description = "Job owner. Defaults to requester's userid.") @Valid @RequestParam(value = "owner", required = false) String owner,
            @Parameter(description = "Job status to filter on, defaults to ALL.", schema = @Schema(allowableValues = "ACTIVE, OUTPUT, INPUT, ALL")) @Valid @RequestParam(value = "status", required = false) JobStatus status) {

        if (status == null) {
            status = JobStatus.ALL;
        }
        return getJobsService().getJobs(prefix, owner, status);
    }


    @GetMapping(value = "/{jobName}/{jobId}", produces = {"application/json"})
    @Operation(summary = "Get the details of a job for a given job name and identifier", operationId = "getJobByNameAndId", description = "This API returns the details of a job for a given job name and identifier.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Ok")})
    public Job getJobByNameAndId(
            @Parameter(description = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @Parameter(description = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {
        return getJobsService().getJob(jobName, jobId);
    }

    @DeleteMapping(value = "/{jobName}/{jobId}", produces = {"application/json"})
    @Operation(summary = "Cancel a Job and Purge it's associated files", operationId = "purgeJob", description = "This API purges a Job")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Job purge succesfully requested")})
    public ResponseEntity<Void> purgeJob(
            @Parameter(description = "Job name", required = true) @PathVariable("jobName") String jobName,
            @Parameter(description = "Job identifier", required = true) @PathVariable("jobId") String jobId) {
        getJobsService().purgeJob(jobName, jobId);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(value = "/", produces = {"application/json"})
    @Operation(summary = "Given a list of jobs Cancel and Purge them all", operationId = "purgeJobs", description = "This API purges all jobs provided")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Job purges succesfully requested")})
    public ResponseEntity<Void> purgeJobs(@RequestBody List<SimpleJob> jobList) {
        jobList.forEach(job -> getJobsService().purgeJob(job.getJobName(), job.getJobId()));
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(value = "/{jobName}/{jobId}", produces = {"application/json"})
    @Operation(summary = "Modify a job", operationId = "modifyJob", description = "This API modifies a job (cancel, hold, release)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Job modify requested"),
            @ApiResponse(responseCode = "200", description = "Job modified")})
    public ResponseEntity<Void> modifyJob(
            @Parameter(description = "Job name", required = true) @PathVariable("jobName") String jobName,
            @Parameter(description = "Job identifier", required = true) @PathVariable("jobId") String jobId,
            @RequestBody ModifyJobRequest request) {
        getJobsService().modifyJob(jobName, jobId, request.getCommand());
        return ResponseEntity.accepted().build();
    }

    @PutMapping(value = "/", produces = {"application/json"})
    @Operation(summary = "Given a list of jobs issue a Modify command for each", operationId = "modifyJobs", description = "This API modifies all jobs provided")
    @ApiResponses(value = {@ApiResponse(responseCode = "202", description = "Job modifies requested")})
    public ResponseEntity<Void> modifyJobs(@RequestBody ModifyMultipleJobsRequest request) {
        request.getJobs().forEach(job -> getJobsService().modifyJob(job.getJobName(), job.getJobId(), request.getCommand()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "string", produces = {"application/json"})
    @Operation(summary = "Submit a job given a string of JCL", operationId = "submitJob", description = "This API submits a job given jcl as a string")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Job successfully created")})
    public ResponseEntity<?> submitJob(@Validated @RequestBody SubmitJobStringRequest request) {

        Job job = getJobsService().submitJobString(request.getJcl());

        URI location = getJobUri(job);
        return ResponseEntity.created(location).body(job);
    }

    @PostMapping(value = "dataset", produces = {"application/json"})
    @Operation(summary = "Submit a job given a data set", operationId = "submitJob",
            description = "This API submits a partitioned data set member or Unix file. For fully qualified data set members use 'MYJOBS.TEST.CNTL(TESTJOBX)'. For Unix files use /u/myjobs/job1.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Job successfully created")})
    public ResponseEntity<?> submitJob(@RequestBody SubmitJobFileRequest request) {

        String file = request.getFile();
        Job job = getJobsService().submitJobFile(file);

        URI location = getJobUri(job);
        return ResponseEntity.created(location).body(job);
    }

    URI getJobUri(Job job) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v2/jobs/{jobName}/{jobID}")
                .buildAndExpand(job.getJobName(), job.getJobId()).toUri();
    }

    @GetMapping(value = "/{jobName}/{jobId}/files", produces = {"application/json"})
    @Operation(summary = "Get a list of output file names for a job", operationId = "getJobOutputFiles",
            description = "This API returns the output file names for a given job.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Ok")})
    public ItemsWrapper<JobFile> getJobOutputFiles(
            @Parameter(description = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @Parameter(description = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {

        return getJobsService().getJobFiles(jobName, jobId);
    }

    @GetMapping(value = "/{jobName}/{jobId}/files/{fileId}/content", produces = {"application/json"})
    @Operation(summary = "Get content from a specific job output file", operationId = "getJobOutputFile",
            description = "This API reads content from a specific job output file. The API can read all output, or a relative record range.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Ok")})
    public JobFileContent getJobOutputFile(
            @Parameter(description = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @Parameter(description = "Job identifier.", required = true) @PathVariable("jobId") String jobId,
            @Parameter(description = "Job file id.", required = true) @PathVariable("fileId") String fileId) {

        return getJobsService().getJobFileContent(jobName, jobId, fileId);
    }

    @GetMapping(value = "/{jobName}/{jobId}/files/content", produces = {"application/json"})
    @Operation(summary = "Get the contents of all job output files for a given job", operationId = "getConcatenatedJobOutputFiles",
            description = "This API reads the contents of all job files of a given job.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Ok")})
    public JobFileContent getConcatenatedJobOutputFiles(
            @Parameter(description = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @Parameter(description = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {
        ItemsWrapper<JobFile> jobFiles = getJobOutputFiles(jobName, jobId);
        StringBuffer outputBuffer = new StringBuffer();
        for (JobFile file : jobFiles.getItems()) {
            outputBuffer.append(getJobOutputFile(jobName, jobId, file.getId().toString()).getContent());
        }
        JobFileContent output = new JobFileContent();
        output.setContent(outputBuffer.toString());
        return output;
    }


    @GetMapping(value = "/{jobName}/{jobId}/steps", produces = {"application/json"})
    @Operation(summary = "Get job steps for a given job", operationId = "getJobSteps",
            description = "This API returns the step name and executed program for each job step for a given job name and identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok")})
    public List<JobStep> getJobSteps(
            @Parameter(description = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @Parameter(description = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {

        try {
            JobFileContent jcl = getJobsService().getJobJcl(jobName, jobId);
            return findJobSteps(jcl.getContent());
        } catch (JobJesjclNotFoundException e) {
            log.error("getJobSteps", e);
            throw new JobStepsNotFoundException(jobName, jobId);
        }
    }

    private static final String JES_JCL_STEP_PATTERN = "^.*(\\/\\/|XX)([^*\\s][^\\s]{0,7}) .+?PGM=([^\\s,]{1,8})"; //$NON-NLS-1$

    private static List<JobStep> findJobSteps(String JCL) {
        List<JobStep> steps = new LinkedList<>();

        Pattern pattern = Pattern.compile(JES_JCL_STEP_PATTERN);
        Scanner scanner = new Scanner(JCL);
        int stepCount = 1;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher matcher = pattern.matcher(line);
            if (matcher.find() && matcher.groupCount() == 3) {
                JobStep step = new JobStep(matcher.group(2), matcher.group(3), stepCount++);
                steps.add(step);
            }
        }
        scanner.close();

        return steps;
    }
}
