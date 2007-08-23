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
package org.osaf.cosmo.dav.acl.resource;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty;

import org.osaf.cosmo.dav.DavCollection;
import org.osaf.cosmo.dav.DavContent;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.UnprocessableEntityException;
import org.osaf.cosmo.dav.impl.DavResourceBase;
import org.osaf.cosmo.dav.property.DavProperty;

/**
 * <p>
 * Models a WebDAV principal collection (as described in RFC 3744) that
 * contains a principal resource for each user account in the server. The
 * principal collection itself is not backed by a persistent entity.
 * </p>
 *
 * @see DavResourceBase
 * @see DavCollection
 */
public class DavUserPrincipalCollection extends DavResourceBase
    implements DavCollection {
    private static final Log log = LogFactory.getLog(DavUserPrincipalCollection.class);
    private static final int[] RESOURCE_TYPES =
        new int[] { ResourceType.COLLECTION };
    private static final HashSet<ReportType> REPORT_TYPES =
        new HashSet<ReportType>();

    private ArrayList<DavUserPrincipal> principals;

    static {
        registerLiveProperty(DeltaVConstants.SUPPORTED_REPORT_SET);
    }

    public DavUserPrincipalCollection(DavResourceLocator locator,
                                      DavResourceFactory factory)
        throws DavException {
        super(locator, factory);
        principals = new ArrayList<DavUserPrincipal>();
    }

    // Jackrabbit DavResource

    public String getSupportedMethods() {
        return "OPTIONS, GET, HEAD, TRACE, REPORT";
    }

    public boolean isCollection() {
        return true;
    }

    public long getModificationTime() {
        return -1;
    }

    public boolean exists() {
        return true;
    }

    public String getDisplayName() {
        return "User Principals";
    }

    public String getETag() {
        return null;
    }

    public void spool(OutputContext outputContext)
        throws IOException {
        writeHtmlDirectoryIndex(outputContext);
    }

    public void addMember(org.apache.jackrabbit.webdav.DavResource member,
                          InputContext inputContext)
        throws org.apache.jackrabbit.webdav.DavException {
        throw new UnsupportedOperationException();
    }

    public DavResourceIterator getMembers() {
        throw new UnsupportedOperationException();
    }

    public void removeMember(org.apache.jackrabbit.webdav.DavResource member)
        throws org.apache.jackrabbit.webdav.DavException {
        throw new UnsupportedOperationException();
    }

    public DavResource getCollection() {
        throw new UnsupportedOperationException();
    }

    public void move(org.apache.jackrabbit.webdav.DavResource destination)
        throws org.apache.jackrabbit.webdav.DavException {
        throw new UnsupportedOperationException();
    }

    public void copy(org.apache.jackrabbit.webdav.DavResource destination,
                     boolean shallow)
        throws org.apache.jackrabbit.webdav.DavException {
        throw new UnsupportedOperationException();
    }

    // DavResource

    public DavCollection getParent()
        throws DavException {
        return null;
    }

    public Report getReport(ReportInfo reportInfo)
        throws DavException {
        if (! isSupportedReport(reportInfo))
            throw new UnprocessableEntityException("Unknown report " + reportInfo.getReportName());

        try {
            return ReportType.getType(reportInfo).createReport(this, reportInfo);
        } catch (org.apache.jackrabbit.webdav.DavException e){
            throw new DavException(e);
        }
    }

    // DavCollection

    public void addContent(DavContent content,
                           InputContext context)
        throws DavException {
        throw new UnsupportedOperationException();
    }

    public MultiStatusResponse addCollection(DavCollection collection,
                                             DavPropertySet properties)
        throws DavException {
        throw new UnsupportedOperationException();
    }

    public DavUserPrincipal findMember(String uri)
        throws DavException {
        if (uri.startsWith(getLocator().getPrefix()))
            uri = uri.substring(getLocator().getPrefix().length());
        return resolveMemberUri(uri);
    }

    // our methods

    protected int[] getResourceTypes() {
        return RESOURCE_TYPES;
    }

    protected void loadLiveProperties(DavPropertySet properties) {
        throw new UnsupportedOperationException();
    }

    protected void setLiveProperty(DavProperty property)
        throws DavException {
        throw new UnsupportedOperationException();
    }

    protected void removeLiveProperty(DavPropertyName name)
        throws DavException {
        throw new UnsupportedOperationException();
    }

    protected void loadDeadProperties(DavPropertySet properties) {
        throw new UnsupportedOperationException();
    }

    protected void setDeadProperty(DavProperty property)
        throws DavException {
        throw new UnsupportedOperationException();
    }

    protected void removeDeadProperty(DavPropertyName name)
        throws DavException {
        throw new UnsupportedOperationException();
    }

    protected boolean isSupportedReport(ReportInfo info) {
        for (Iterator<ReportType> i=REPORT_TYPES.iterator(); i.hasNext();) {
            if (i.next().isRequestedReportType(info))
                return true;
        }
        return false;
    }

    protected DavUserPrincipal resolveMemberUri(String uri)
        throws DavException {
        DavResourceLocator locator = getLocator().getFactory().
            createResourceLocator(getLocator().getPrefix(),
                                  getLocator().getWorkspacePath(), uri,
                                  false);
        return (DavUserPrincipal) getResourceFactory().resolve(locator);
    }

    private void writeHtmlDirectoryIndex(OutputContext context)
        throws IOException {
        context.setContentType(IOUtil.buildContentType("text/html", "UTF-8"));

        if (! context.hasStream())
            return;

        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(context.getOutputStream(),
                                                   "utf8"));

        writer.write("<html><head><title>");
        writer.write(StringEscapeUtils.escapeHtml(getDisplayName()));
        writer.write("</title></head>");
        writer.write("<body>");
        writer.write("<h1>");
        writer.write(StringEscapeUtils.escapeHtml(getDisplayName()));
        writer.write("</h1>");
        writer.write("<p>");
        writer.write("The following reports are supported on this collection:");
        writer.write("<ul>");
        for (ReportType rt : REPORT_TYPES)
            writer.write(StringEscapeUtils.escapeHtml(rt.getReportName()));
        writer.write("</ul>");
        writer.write("</body>");
        writer.write("</html>");
        writer.write("\n");
        writer.close();
    }
}
