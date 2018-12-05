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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.zowe.api.common.utils.ZosUtils;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.model.SubmitJobStringRequest;
import org.zowe.jobs.services.JobsService;

import javax.validation.Valid;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@Api(value = "JES Jobs APIs", tags = "JES job APIs")
public class JobsController {

    private static final Logger log = LoggerFactory.getLogger(JobsController.class);

    @Autowired
    private JobsService jobsService;

    @GetMapping(value = "/username", produces = { "application/json" })
    @ApiOperation(value = "Get current userid", nickname = "getCurrentUserName", notes = "This API returns the caller's current TSO userid.", response = String.class, tags = {
            "System APIs", })
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok", response = String.class) })
    public String getCurrentUserName() {
        return ZosUtils.getUsername();
    }

    @GetMapping(value = "", produces = { "application/json" })
    @ApiOperation(value = "Get a list of jobs", nickname = "getJobs", notes = "This API returns the a list of jobs for a given prefix and owner.", response = Job.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok", response = Job.class, responseContainer = "List") })
    public List<Job> getJobs(
            @ApiParam(value = "Job name prefix. If omitted, defaults to '*'.", defaultValue = "*") @Valid @RequestParam(value = "prefix", required = false, defaultValue = "*") String prefix,
            @ApiParam(value = "Job owner. Defaults to requester's userid.") @Valid @RequestParam(value = "owner", required = false) String owner,
            @ApiParam(value = "Job status to filter on, defaults to ALL.", allowableValues = "ACTIVE, OUTPUT, INPUT, ALL") @Valid @RequestParam(value = "status", required = false) JobStatus status) {

        String ownerFilter = getOwnerFilterValue(owner);
        if (status == null) {
            status = JobStatus.ALL;
        }
        List<Job> jobs = jobsService.getJobs(prefix, ownerFilter, status);
        return jobs;
    }

    private String getOwnerFilterValue(String owner) {
        if (owner == null) {
            String username = ZosUtils.getUsername();
            owner = (username != null) ? username : "*";
        }
        return owner;
    }

//    @GetMapping(value = "/{jobName}/{jobId}", produces = { "application/json" })
//    @ApiOperation(value = "Get the details of a job for a given job name and identifier", nickname = "getJobByNameAndId", notes = "This API returns the details of a job for a given job name and identifier.", response = Job.class)
//    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok", response = Job.class) })
//    public Job getJobByNameAndId(
//            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
//            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {
//        return jobsService.getJob(jobName, jobId);
//    }

//    @DeleteMapping(value = "/{jobName}/{jobId}", produces = { "application/json" })
//    @ApiOperation(value = "Cancel a Job and Purge it's associated files", nickname = "purgeJob", notes = "This api purges a Job", tags = {
//            "JES job APIs", })
//    @ApiResponses(value = { @ApiResponse(code = 204, message = "Job purge succesfully requested") })
//    public ResponseEntity<Void> purgeJob(
//            @ApiParam(value = "Job name", required = true) @PathVariable("jobName") String jobName,
//            @ApiParam(value = "Job identifier", required = true) @PathVariable("jobId") String jobId) {
//        jobsService.purgeJob(jobName, jobId);
//        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
//    }

//    @PostMapping(value = "", produces = { "application/json" })
//    @ApiOperation(value = "Submit a job", nickname = "submitJob", notes = "This API submits a partitioned data set member or Unix file. For fully qualified data set members use 'MYJOBS.TEST.CNTL(TESTJOBX)'. For non fully qualified use TEST.CNTL(TESTJOBX). For Unix files use /u/myjobs/job1.", tags = {
//            "JES job APIs", })
//    @ApiResponses(value = { @ApiResponse(code = 201, message = "Job successfully created", response = Job.class) })
//    public ResponseEntity<?> submitJob(@ApiParam(value = "JSON format input body") SubmitJobFileRequest request) {
//
//        String file = request.getFile();
//        if (StringUtils.isEmpty(file)) {
//            // TODO - throw exception
//            // String error = Messages.getString("Jobs.InvalidSubmitData");
//            // throw createNotFoundException(error);
//        }
//        Job job = jobsService.submitJob(file);
//
//        URI location = createSubmitJobLocationHeader(job);
//        return ResponseEntity.created(location).build();
//    }

    // TODO - this isn't properly tested - just added for the get integration tests
    @PostMapping(value = "", produces = { "application/json" })
    @ApiOperation(value = "Submit a job", nickname = "submitJob", notes = "This API submits a job given jcl as a string", tags = {
            "JES job APIs", })
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Job successfully created", response = Job.class) })
    public ResponseEntity<?> submitJob(@Validated @RequestBody SubmitJobStringRequest request) {

        Job job = jobsService.submitJobString(request.getJcl());

        URI location = createSubmitJobLocationHeader(job);
        return ResponseEntity.created(location).body(job);
    }

    URI createSubmitJobLocationHeader(Job job) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{jobName}/{jobID}")
                .buildAndExpand(job.getJobName(), job.getJobId()).toUri();
    }
//
//    @GetMapping(value = "/{jobName}/ids/{jobId}/files", produces = { "application/json" })
//    @ApiOperation(value = "Get a list of output file names for a job", nickname = "getJobOutputFiles", notes = "This API returns the output file names for a given job.", response = JobFile.class, responseContainer = "List", tags = {
//            "JES job APIs", })
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Ok", response = JobFile.class, responseContainer = "List") })
//    public List<JobFile> getJobOutputFiles(
//            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
//            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {
//
//        return jobsService.getJobFiles(jobName, jobId);
//    }
//
//    // TODO - do we want to support start and end immediately?
//    @GetMapping(value = "/{jobName}/ids/{jobId}/files/{fileId}", produces = { "application/json" })
//    @ApiOperation(value = "Get content from a specific job output file", nickname = "getJobOutputFile", notes = "This API reads content from a specific job output file. The API can read all output, or a relative record range.", response = OutputFile.class, tags = {
//            "JES job APIs", })
//    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok", response = OutputFile.class) })
//    public OutputFile getJobOutputFile(
//            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
//            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId,
//            @ApiParam(value = "Job file id number.", required = true) @PathVariable("fileId") String fileId,
//            @ApiParam(value = "Optional starting relative record number to read.") @Valid @RequestParam(value = "start", required = false) Integer start,
//            @ApiParam(value = "Optional ending relative record number to read. If omitted, all records are returned.") @Valid @RequestParam(value = "end", required = false) Integer end) {
//
//        return jobsService.getJobFileRecordsByRange(jobName, jobId, fileId, start, end);
//    }
//
//    @GetMapping(value = "/{jobName}/ids/{jobId}/steps", produces = { "application/json" })
//    @ApiOperation(value = "Get job steps for a given job", nickname = "getJobSteps", notes = "This API returns the step name and executed program for each job step for a given job name and identifier.", response = Step.class, responseContainer = "List", tags = {
//            "JES job APIs", })
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Ok", response = Step.class, responseContainer = "List") })
//    public List<Step> getJobSteps(
//            @ApiParam(value = "Job name.", required = true) @PathVariable("jobName") String jobName,
//            @ApiParam(value = "Job identifier.", required = true) @PathVariable("jobId") String jobId) {
//
//        OutputFile jcl = jobsService.getJobJcl(jobName, jobId);
//        return findJobSteps(jcl.getContent());
//    }
//
//    // TODO - refactor out private methods in utils class?
//    private static final String JES_JCL_STEP_PATTERN = "^.*(\\/\\/|XX)([^*\\s][^\\s]{0,7}) .+?PGM=([^\\s,]{1,8})"; //$NON-NLS-1$
//
//    public static List<Step> findJobSteps(String JCL) {
//        List<Step> steps = new LinkedList<>();
//
//        Pattern pattern = Pattern.compile(JES_JCL_STEP_PATTERN);
//        Scanner scanner = new Scanner(JCL);
//        int stepCount = 1;
//        while (scanner.hasNextLine()) {
//            String line = scanner.nextLine();
//            Matcher matcher = pattern.matcher(line);
//            if (matcher.find() && matcher.groupCount() == 3) {
//                Step step = new Step(matcher.group(2), matcher.group(3), stepCount++);
//                steps.add(step);
//            }
//        }
//        scanner.close();
//
//        return steps;
//    }
}
