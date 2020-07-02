package org.zowe.jobs.model;

import java.util.ArrayList;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ModifyMultipleJobsRequest extends ModifyJobRequest {
    private ArrayList<SimpleJob> jobs;
    
    public ModifyMultipleJobsRequest(ArrayList<SimpleJob> jobs, String command) {
        super(command);
        this.jobs = jobs;
    }
}
