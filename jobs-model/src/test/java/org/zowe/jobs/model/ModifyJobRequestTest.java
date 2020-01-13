/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2020
 */
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
