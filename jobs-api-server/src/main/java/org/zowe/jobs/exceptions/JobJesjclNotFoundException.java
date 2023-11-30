/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */

package org.zowe.jobs.exceptions;

import org.springframework.http.HttpStatus;
import org.zowe.api.common.exceptions.ZoweApiRestException;

public class JobJesjclNotFoundException extends ZoweApiRestException {

    /**
     * 
     */
    private static final long serialVersionUID = -7387499876050718494L;

    public JobJesjclNotFoundException(String jobName, String jobId) {
        super(HttpStatus.NOT_FOUND, "No JESJCL spool file found for job with name ''{0}'' and id ''{1}''", jobName,
                jobId);
    }

}
