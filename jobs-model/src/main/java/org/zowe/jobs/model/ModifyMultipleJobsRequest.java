package org.zowe.jobs.model;

import java.util.ArrayList;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ModifyMultipleJobsRequest {
    @ApiModelProperty(value = "The modify command, e.g. cancel, hold, release", dataType = "string", required = true, example = "cancel")
    private String command;
    @ApiModelProperty(value = "The list of jobs to receive the modify command", dataType = "List", required = true, example = "[{jobId:job1234, jobName:TestJob}]")
    private ArrayList<SimpleJob> jobs;
}
