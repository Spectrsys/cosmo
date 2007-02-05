/*
 * Copyright 2006 Open Source Applications Foundation
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
package org.osaf.cosmo.dav.caldav.report;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.osaf.cosmo.dav.BaseDavTestCase;
import org.osaf.cosmo.dav.caldav.report.mock.MockCaldavReport;
import org.osaf.cosmo.dav.impl.DavHomeCollection;
import org.osaf.cosmo.model.CalendarCollectionStamp;
import org.osaf.cosmo.model.CollectionItem;
import org.w3c.dom.Document;

/**
 * Test case for <code>CaldavReport</code>.
 * <p>
 */
public class CaldavReportTest extends BaseDavTestCase
    implements DavConstants {
    private static final Log log = LogFactory.getLog(CaldavReportTest.class);

    /** */
    public void testDepthInfinity() throws Exception {
        testHelper.getHomeCollection().setExcludeFreeBusyRollup(false);
        DavHomeCollection home = testHelper.initializeHomeResource();

        CollectionItem coll1 = testHelper.
        makeAndStoreDummyCollection(testHelper.getHomeCollection());
        coll1.addStamp(new CalendarCollectionStamp(coll1));
        coll1.setExcludeFreeBusyRollup(false);
        
        CollectionItem coll2 = testHelper.
        makeAndStoreDummyCollection(testHelper.getHomeCollection());
        coll2.addStamp(new CalendarCollectionStamp(coll2));
        coll2.setExcludeFreeBusyRollup(false);
        
        MockCaldavReport report = new MockCaldavReport();
        report.init(home, makeReportInfo("freebusy1.xml", DEPTH_INFINITY));

        report.runQuery();
        
        // Verify report is recursively called on all collections
        Assert.assertEquals(report.calls.size(), 3);
        Assert.assertTrue(report.calls.contains(testHelper.getHomeCollection()
                .getDisplayName()));
        Assert.assertTrue(report.calls.contains(coll1.getDisplayName()));
        Assert.assertTrue(report.calls.contains(coll2.getDisplayName()));
    }

    private ReportInfo makeReportInfo(String resource, int depth)
        throws Exception {
        Document doc = testHelper.loadXml(resource);
        return new ReportInfo(doc.getDocumentElement(), depth);
    }
}
