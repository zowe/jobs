package org.zowe.jobs.model;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModifyMultipleJobsRequest extends ModifyJobRequest{
    private ArrayList<SimpleJob> jobs;
}
