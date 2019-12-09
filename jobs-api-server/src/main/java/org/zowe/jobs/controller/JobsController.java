/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */
package org.zowe.jobs.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.zowe.api.common.controller.AbstractApiController;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.api.common.utils.ZosUtils;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.exceptions.JobStepsNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.model.JobStep;
import org.zowe.jobs.model.SubmitJobFileRequest;
import org.zowe.jobs.model.SubmitJobStringRequest;
import org.zowe.jobs.services.JobsService;

import javax.validation.Valid;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@Api(value = "JES Jobs APIs", tags = "JES job APIs")
public class JobsController extends AbstractApiController {

    @Autowired
    private JobsService jobsService;

    @GetMapping(value = "", produces = { "application/json" })
    @ApiOperation(value = "Get a list of jobs", nickname = "getJobs", notes = "This API returns the a list of jobs for a given prefix and owner.", response = Job.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
    public ItemsWrapper<Job> getJobs(
            @ApiParam(value = "Job name prefix. If omitted, defaults to '*'.", defaultValue = "*") @Valid @RequestParam(value = "prefix", required = false, defaultValue = "*") String prefix,
            @ApiParam(value = "Job owner. Defaults to requester's userid.") @Valid @RequestParam(value = "owner", required = false) String owner,
            @ApiParam(value = "Job status to filter on, defaults to ALL.", allowableValues = "ACTIVE, OUTPUT, INPUT, ALL") @Valid @RequestParam(value = "status", required = false) JobStatus status) {

        String ownerFilter = getOwnerFilterValue(owner);
        if (status == null) {
            status = JobStatus.ALL;
        }
        return jobsService.getJobs(prefix, ownerFilter, status);
    }

    private String getOwnerFilterValue(String owner) {
        if (owner == null) {
            String username = ZosUtils.getUsername();
            owner = (username != null) ? username : "*";
        }
        return owner;
    }

    @GetMapping(value = "/{jobName}/{jobId}", produces = { "application/json" })
    @ApiOperation(value = "Get the details of a job for a given job name and identifier", nickname = "getJobByNameAndId", notes = "This API returns the details of a job for a given job name and identifier.", response = Job.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok", response = Job.class) })
    public Job getJobByNameAndId(
            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {
        return jobsService.getJob(jobName, jobId);
    }

    @DeleteMapping(value = "/{jobName}/{jobId}", produces = { "application/json" })
    @ApiOperation(value = "Cancel a Job and Purge it's associated files", nickname = "purgeJob", notes = "This api purges a Job", tags = {
            "JES job APIs", })
    @ApiResponses(value = { @ApiResponse(code = 204, message = "Job purge succesfully requested") })
    public ResponseEntity<Void> purgeJob(
            @ApiParam(value = "Job name", required = true) @PathVariable("jobName") String jobName,
            @ApiParam(value = "Job identifier", required = true) @PathVariable("jobId") String jobId) {
        jobsService.purgeJob(jobName, jobId);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "string", produces = { "application/json" })
    @ApiOperation(value = "Submit a job given a string of JCL", nickname = "submitJob", notes = "This API submits a job given jcl as a string", tags = {
            "JES job APIs", })
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Job successfully created", response = Job.class) })
    public ResponseEntity<?> submitJob(@Validated @RequestBody SubmitJobStringRequest request) {

        Job job = jobsService.submitJobString(request.getJcl());

        URI location = getJobUri(job);
        return ResponseEntity.created(location).body(job);
    }

    @PostMapping(value = "dataset", produces = { "application/json" })
    @ApiOperation(value = "Submit a job given a data set", nickname = "submitJob", notes = "This API submits a partitioned data set member or Unix file. For fully qualified data set members use 'MYJOBS.TEST.CNTL(TESTJOBX)'. For Unix files use /u/myjobs/job1.", tags = {
            "JES job APIs", })
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Job successfully created", response = Job.class) })
    public ResponseEntity<?> submitJob(@RequestBody SubmitJobFileRequest request) {

        String file = request.getFile();
        Job job = jobsService.submitJobFile(file);

        URI location = getJobUri(job);
        return ResponseEntity.created(location).body(job);
    }

    URI getJobUri(Job job) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/jobs/{jobName}/{jobID}")
            .buildAndExpand(job.getJobName(), job.getJobId()).toUri();
    }

    @GetMapping(value = "/{jobName}/{jobId}/files", produces = { "application/json" })
    @ApiOperation(value = "Get a list of output file names for a job", nickname = "getJobOutputFiles", notes = "This API returns the output file names for a given job.", response = JobFile.class, responseContainer = "List", tags = {
            "JES job APIs", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
    public ItemsWrapper<JobFile> getJobOutputFiles(
            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {

        return jobsService.getJobFiles(jobName, jobId);
    }

    @GetMapping(value = "/{jobName}/{jobId}/files/{fileId}/content", produces = { "application/json" })
    @ApiOperation(value = "Get content from a specific job output file", nickname = "getJobOutputFile", notes = "This API reads content from a specific job output file. The API can read all output, or a relative record range.", response = JobFileContent.class, tags = {
            "JES job APIs", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok", response = JobFileContent.class) })
    public JobFileContent getJobOutputFile(
            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId,
            @ApiParam(value = "Job file id.", required = true) @PathVariable("fileId") String fileId) {

        return jobsService.getJobFileContent(jobName, jobId, fileId);
    }
    
    @GetMapping(value = "/{jobName}/{jobId}/files/content", produces = { "application/json" })
    @ApiOperation(value = "Get the contents of all job output files for a given job", nickname = "getConcatenatedJobOutputFiles", notes = "This API reads the contents of all job files of a given job.", response = JobFileContent.class, tags = {
            "JES job APIs", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok", response = JobFileContent.class) })
    public JobFileContent getConcatenatedJobOutputFiles(
            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {
        ItemsWrapper<JobFile> jobFiles = getJobOutputFiles(jobName, jobId);
        StringBuffer outputBuffer = new StringBuffer();
        for (JobFile file : jobFiles.getItems()) {
            outputBuffer.append(getJobOutputFile(jobName, jobId, file.getId().toString()).getContent());
        }
        JobFileContent output = new JobFileContent();
        output.setContent(outputBuffer.toString());
        return output;
    }
    

    @GetMapping(value = "/{jobName}/{jobId}/steps", produces = { "application/json" })
    @ApiOperation(value = "Get job steps for a given job", nickname = "getJobSteps", notes = "This API returns the step name and executed program for each job step for a given job name and identifier.", response = JobStep.class, responseContainer = "List", tags = {
            "JES job APIs", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok", response = JobStep.class, responseContainer = "List") })
    public List<JobStep> getJobSteps(
            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {

        try {
            JobFileContent jcl = jobsService.getJobJcl(jobName, jobId);
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
