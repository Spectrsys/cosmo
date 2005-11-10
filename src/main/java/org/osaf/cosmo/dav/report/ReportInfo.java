/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osaf.cosmo.dav.report;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.jdom.Element;

/**
 * This class is copied pretty much verbatim from
 * org.apache.jackrabit.webdav.version.ReportInfo.
 */

/**
 * The <code>ReportInfo</code> class encapsulates the body of a REPORT
 * request. <a href="http://www.ietf.org/rfc/rfc3253.txt">RFC 3253</a> the top
 * Xml element being the name of the requested report. In addition a Depth
 * header may be present (default value: {@link DavConstants#DEPTH_0}).
 */
public class ReportInfo {

    private final Element reportElement;

    private final int depth;

    private final DavSession session;

    /**
     * Create a new <code>ReportInfo</code> object.
     * 
     * @param reportElement
     * @param depth
     *            Depth value as retrieved from the
     *            {@link DavConstants#HEADER_DEPTH}.
     * @param session
     *            DavSession that may be needed by reports
     */
    public ReportInfo(Element reportElement, int depth, DavSession session) {
        this.reportElement = reportElement;
        this.depth = depth;
        this.session = session;
    }

    /**
     * Returns the Xml element specifying the requested report.
     * 
     * @return reportElement
     */
    public Element getReportElement() {
        return reportElement;
    }

    /**
     * Returns the depth field. The request must be applied separately to the
     * collection itself and to all members of the collection that satisfy the
     * depth value.
     * 
     * @return depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Returns the session object that some reports may need access to
     * 
     * @return Returns the session.
     */
    public DavSession getSession() {
        return session;
    }

    /**
     * Returns a <code>DavPropertyNameSet</code> providing the property names
     * present in an eventual {@link DavConstants#XML_PROP} child element. If no
     * such child element is present an empty set is returned.
     * 
     * @return {@link DavPropertyNameSet} providing the property names present
     *         in an eventual {@link DavConstants#XML_PROP DAV:prop} child
     *         element or an empty set.
     */
    public DavPropertyNameSet getPropertyNameSet() {
        Element propElement = reportElement.getChild(DavConstants.XML_PROP,
                DavConstants.NAMESPACE);
        if (propElement != null) {
            return new DavPropertyNameSet(propElement);
        } else {
            return new DavPropertyNameSet();
        }
    }
}