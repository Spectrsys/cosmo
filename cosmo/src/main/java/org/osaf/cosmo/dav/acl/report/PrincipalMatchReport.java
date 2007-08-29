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

import org.osaf.cosmo.dav.BadRequestException;
import org.osaf.cosmo.dav.DavCollection;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.ForbiddenException;
import org.osaf.cosmo.dav.UnprocessableEntityException;
import org.osaf.cosmo.dav.acl.AclConstants;
import org.osaf.cosmo.dav.acl.resource.DavUserPrincipal;
import org.osaf.cosmo.dav.impl.DavCalendarCollection;
import org.osaf.cosmo.dav.report.MultiStatusReport;
import org.osaf.cosmo.model.User;

import org.w3c.dom.Element;

/**
 * <p>
 * Represents the <code>DAV:principal-match</code> report that
 * provides a mechanism for finding resources that match the
 * current user.
 * </p>
 */
public class PrincipalMatchReport extends MultiStatusReport
    implements AclConstants {
    private static final Log log =
        LogFactory.getLog(PrincipalMatchReport.class);

    public static final ReportType REPORT_TYPE_PRINCIPAL_MATCH =
        ReportType.register(ELEMENT_ACL_PRINCIPAL_MATCH, NAMESPACE,
                            PrincipalMatchReport.class);

    private boolean self;
    private DavPropertyName principalProperty;
    private User currentUser;

    // Report methods

    public ReportType getType() {
        return REPORT_TYPE_PRINCIPAL_MATCH;
    }

    // ReportBase methods

    /**
     * Parses the report info.
     */
    protected void parseReport(ReportInfo info)
        throws DavException {
        if (! getType().isRequestedReportType(info))
            throw new DavException("Report not of type " + getType());

        if (! (getResource() instanceof DavCollection))
            throw new BadRequestException(getType() + " report must target a collection");
        if (info.getDepth() != DEPTH_0)
            throw new BadRequestException(getType() + " report must be made with depth 0");

        setPropFindProps(info.getPropertyNameSet());
        setPropFindType(PROPFIND_BY_PROPERTY);

        self = findSelf(info);
        if (! self)
            principalProperty = findPrincipalProperty(info);
        if (! (self || principalProperty != null))
            throw new UnprocessableEntityException("Expected either " + QN_ACL_SELF + " or " + QN_ACL_PRINCIPAL_PROPERTY + " child of " + REPORT_TYPE_PRINCIPAL_MATCH);

        currentUser = getResource().getResourceFactory().getSecurityManager().
            getSecurityContext().getUser();
        if (currentUser == null)
            throw new ForbiddenException("Authenticated principal is not a user");
        if (log.isDebugEnabled())
            log.debug("Matching current user " + currentUser.getUsername());
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
    }

    protected void doQuerySelf(DavResource resource)
        throws DavException {
        if (log.isDebugEnabled())
            log.debug("Querying " + resource.getResourcePath());
        if (self && matchesUserPrincipal(resource))
            getResults().add(resource);
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

    public boolean isSelf() {
        return self;
    }

    public DavPropertyName getPrincipalProperty() {
        return principalProperty;
    }

    private boolean matchesUserPrincipal(DavResource resource)
        throws DavException {
        if (resource instanceof DavUserPrincipal) {
            User principal = ((DavUserPrincipal)resource).getUser();
            if (currentUser.equals(principal)) {
                log.debug("Matched user principal " +
                          resource.getResourcePath());
                return true;
            }
        }
        return false;
    }

    private static boolean findSelf(ReportInfo info)
        throws DavException {
        // XXX
        return true;
    }

    private static DavPropertyName findPrincipalProperty(ReportInfo info)
        throws DavException {
        // XXX
        return null;
    }
}
