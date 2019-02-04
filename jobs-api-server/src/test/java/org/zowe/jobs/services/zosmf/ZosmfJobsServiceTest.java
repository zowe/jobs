/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2019
 */
package org.zowe.jobs.services.zosmf;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.test.ZoweApiTest;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.JobFileContent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
//TODO - review prepares
@PrepareForTest({ ZosmfJobsService.class })
public class ZosmfJobsServiceTest extends ZoweApiTest {

    @Mock
    ZosmfConnector zosmfConnector;

    ZosmfJobsService jobsService;

    @Before
    public void setUp() throws Exception {
        jobsService = new ZosmfJobsService();
        jobsService.zosmfConnector = zosmfConnector;
    }

    @Test
    public void get_job_jcl_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21489";
        JobFileContent expected = new JobFileContent(
                "        1 //ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440               JOB21849\n"
                        + "          //*        TEST JOB\n        2 //UNIT     EXEC PGM=IEFBR14\n" + "");

        GetJobFileContentZosmfRequestRunner runner = mock(GetJobFileContentZosmfRequestRunner.class);
        PowerMockito.whenNew(GetJobFileContentZosmfRequestRunner.class).withArguments(jobName, jobId, "3")
            .thenReturn(runner);

        when(runner.run(zosmfConnector)).thenReturn(expected);

        assertEquals(expected, jobsService.getJobJcl(jobName, jobId));
    }

    @Test
    public void get_job_jcl_for_non_existing_jobname_should_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";

        Exception expectedException = new JobNameNotFoundException(jobName, jobId);

        getJobJclCheckException(jobName, jobId, expectedException, expectedException);
    }

    @Test
    public void get_job_jcl_for_non_existing_job_id_should_throw_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "z000000";

        Exception expectedException = new JobIdNotFoundException(jobName, jobId);

        getJobJclCheckException(jobName, jobId, expectedException, expectedException);
    }

    @Test
    public void get_job_jcl_for_non_existing_field_id_should_throw_different_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21849";

        Exception expectedException = new JobJesjclNotFoundException(jobName, jobId);
        JobFileIdNotFoundException subException = new JobFileIdNotFoundException(jobName, jobId, "3");

        getJobJclCheckException(jobName, jobId, expectedException, subException);
    }

    private void getJobJclCheckException(String jobName, String jobId, Exception expectedException,
            Exception subException) throws Exception {
        GetJobFileContentZosmfRequestRunner runner = mock(GetJobFileContentZosmfRequestRunner.class);
        PowerMockito.whenNew(GetJobFileContentZosmfRequestRunner.class).withArguments(jobName, jobId, "3")
            .thenReturn(runner);

        when(runner.run(zosmfConnector)).thenThrow(subException);

        shouldThrow(expectedException, () -> jobsService.getJobJcl(jobName, jobId));
    }
}
