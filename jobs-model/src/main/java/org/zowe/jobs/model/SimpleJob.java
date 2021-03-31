package org.zowe.jobs.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleJob {
    @ApiModelProperty(value = "The name of a job", dataType = "string", required = true, example = "TESTJOB")
    private String jobName;
    @ApiModelProperty(value = "The id of a job", dataType = "string", required = true, example = "JOB00001")
    private String jobId;
}
