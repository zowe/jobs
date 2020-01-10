package org.zowe.jobs.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModifyJobRequestTest {
    
    @Test
    public void testModifyJobRequest() throws Exception {
        String modifyRequest = "cancel";
        
        ModifyJobRequest modifyJobRequest = new ModifyJobRequest(modifyRequest);
        assertEquals(modifyRequest, modifyJobRequest.getCommand());
    }

}
