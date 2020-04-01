package org.zowe.jobs.services.zosmf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.connectors.zosmf.ZosmfConnectorJWTAuth;

@Service("ZosmfJobsServiceV2")
public class ZosmfJobsServiceV2 extends AbstractZosmfJobsService {
    @Autowired
    ZosmfConnectorJWTAuth zosmfConnector;

    @Override
    ZosmfConnector getZosmfConnector() {
        return zosmfConnector;
    }
}
