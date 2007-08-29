/*
 * Copyright 2007 Open Source Applications Foundation
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
package org.osaf.cosmo.dav.acl.report;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;

import org.osaf.cosmo.dav.BadRequestException;
import org.osaf.cosmo.dav.DavCollection;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.ForbiddenException;
import org.osaf.cosmo.dav.UnprocessableEntityException;
import org.osaf.cosmo.dav.acl.AclConstants;
import org.osaf.cosmo.dav.acl.resource.DavUserPrincipal;
import org.osaf.cosmo.dav.impl.DavCalendarCollection;
import org.osaf.cosmo.dav.property.DavProperty;
import org.osaf.cosmo.dav.report.MultiStatusReport;
import org.osaf.cosmo.model.User;

import org.w3c.dom.Element;

/**
 * <p>
 * Represents the <code>DAV:principal-property-search</code> report that
 * provides a mechanism for finding resources whose property values match
 * given input strings.
 * </p>
 * <p>
 * If the report includes the <code>DAV:self</code> element, it matches
 * any principal resource in the target collection that represents the
 * currently authenticated user. This form of the report is used to search
 * through a principal collection for any principal resources that match
 * the current user.
 * </p>
 * <p>
 * If the report includes the <code>DAV:principal-property</code> element,
 * that element's first child element is taken to be the name of a
 * property that identifies the principal associated with a resource. The
 * report matches any resource in the target collection that 1) has
 * the specified principal property, 2) the principal property contains at
 * least one child <code>DAV:href</code> element, and 3) at least one of the
 * hrefs matches the principal URL of the currently authenticated user. This 
 * form of the report is used to search through an item collection for any
 * resources that are associated with the current user via the specified
 * principal property (usually <code>DAV:owner</code>).
 * </p>
 * <p>
 * Both forms of the report may optionally include a <code>DAV:prop</code>
 * element specifying the names of properties that are to be included in the
 * response for each matching resource. If <code>DAV:prop</code> is not
 * included in the report, then only the href and response status are
 * provided for each resource.
 * </p>
 * <p>
 * As per RFC 3744, the report must be specified with depth 0. The report
 * must be targeted at a collection.
 * </p>
 */
public class PrincipalPropertySearchReport extends MultiStatusReport
    implements AclConstants {
    private static final Log log =
        LogFactory.getLog(PrincipalPropertySearchReport.class);

    public static final ReportType REPORT_TYPE_PRINCIPAL_PROPERTY_SEARCH =
        ReportType.register(ELEMENT_ACL_PRINCIPAL_PROPERTY_SEARCH, NAMESPACE,
                            PrincipalPropertySearchReport.class);

    private boolean self;

    // Report methods

    public ReportType getType() {
        return REPORT_TYPE_PRINCIPAL_PROPERTY_SEARCH;
    }

    // ReportBase methods

    /**
     * Parses the report info, extracting self, principal property and
     * return properties.
     */
    protected void parseReport(ReportInfo info)
        throws DavException {
        if (! getType().isRequestedReportType(info))
            throw new DavException("Report not of type " + getType());

        if (! getResource().isCollection())
            throw new BadRequestException(getType() + " report must target a collection");
        if (info.getDepth() != DEPTH_0)
            throw new BadRequestException(getType() + " report must be made with depth 0");

        setPropFindProps(info.getPropertyNameSet());
        setPropFindType(PROPFIND_BY_PROPERTY);
    }

    /**
     * <p>
     * Executes the report query and stores the result. Behaves like the
     * superclass method except that it does not check depth, since the
     * report by definition always uses depth 0.
     * </p>
     */
    protected void runQuery()
        throws DavException {
        DavCollection collection = (DavCollection) getResource();
        doQuerySelf(collection);
        doQueryChildren(collection);
        // don't use doQueryDescendents, because that would cause us to have to
        // iterate through the members twice. instead, we implement
        // doQueryChildren to call itself recursively.
        // XXX: refactor ReportBase.runQuery() to use a helper object rather
        // than specifying doQuerySelf etc interface methods.
    }

    protected void doQuerySelf(DavResource resource)
        throws DavException {
        if (log.isDebugEnabled())
            log.debug("Querying " + resource.getResourcePath());
        // XXX
    }

    protected void doQueryChildren(DavCollection collection)
        throws DavException {
        for (DavResourceIterator i = collection.getMembers(); i.hasNext();) {
            DavResource member = (DavResource) i.nextResource();
            if (member.isCollection()) {
                DavCollection dc = (DavCollection) member;
                doQuerySelf(dc);
                doQueryChildren(dc);
            } else
                doQuerySelf(member);
        }
    }

    // our methods
}
