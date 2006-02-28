/*
 * Copyright 2005 Open Source Applications Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osaf.cosmo.dav;

import javax.jcr.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;

import org.osaf.cosmo.dav.CosmoDavResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test Case for Bug 5254.
 */
public class Bug5254Test extends BaseReportTestCase {
    private static final Log log = LogFactory.getLog(Bug5254Test.class);

    /**
     */
    protected void setUpDavData(Node node) throws Exception {
        Node resourceNode =
            testHelper.addCalendarResourceNode(node, "reports/bugs/5254.ics");
    }

    public void testBug5254() throws Exception {
        Document content = testHelper.loadXml("reports/bugs/5254.xml");

        MockHttpServletRequest request = createMockRequest("REPORT", testUri);
        sendXmlRequest(request, content);

        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertEquals(CosmoDavResponse.SC_MULTI_STATUS, response.getStatus());
        //        log.debug(new String(response.getContentAsByteArray()));

        MultiStatus ms = readMultiStatusResponse(response);
        assertEquals(1, ms.getResponses().size());
    }
}
