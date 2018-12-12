package org.zowe.jobs.exceptions;

import org.springframework.http.HttpStatus;
import org.zowe.api.common.exceptions.ZoweApiRestException;

public class JobNameNotFoundException extends ZoweApiRestException {

    /**
     * 
     */
    private static final long serialVersionUID = 6936887858320598970L;

    // TODO - bad request, or not found?
    public JobNameNotFoundException(String jobName, String jobId) {
        super(HttpStatus.NOT_FOUND, "No job with name ''{0}'' and id ''{1}'' was found", jobName, jobId);
    }

}
