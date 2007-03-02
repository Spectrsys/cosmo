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
package org.osaf.cosmo.dav.impl;

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
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty;
import org.osaf.cosmo.dav.ExtendedDavConstants;
import org.osaf.cosmo.dav.caldav.report.FreeBusyReport;
import org.osaf.cosmo.dav.caldav.report.MultigetReport;
import org.osaf.cosmo.dav.caldav.report.QueryReport;
import org.osaf.cosmo.dav.property.ExcludeFreeBusyRollup;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.CollectionLockedException;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ModelValidationException;

/**
 * Extends <code>DavResourceBase</code> to adapt the Cosmo
 * <code>CollectionItem</code> to the DAV resource model.
 *
 * This class defines the following live properties:
 *
 * <ul>
 * <li><code>DAV:supported-report-set</code> (protected)</li>
 * <li><code>cosmo:exclude-free-busy-rollup</code></li>
 * </ul>
 *
 * @see DavResourceBase
 * @see CollectionItem
 */
public class DavCollection extends DavResourceBase
    implements ExtendedDavConstants {
    private static final Log log = LogFactory.getLog(DavCollection.class);
    private static final int[] RESOURCE_TYPES;
    private static final Set<String> DEAD_PROPERTY_FILTER =
        new HashSet<String>();
    private static final Set REPORT_TYPES = new HashSet();

    private ArrayList members;

    static {
        registerLiveProperty(DeltaVConstants.SUPPORTED_REPORT_SET);
        registerLiveProperty(EXCLUDEFREEBUSYROLLUP);

        RESOURCE_TYPES = new int[] { ResourceType.COLLECTION };

        REPORT_TYPES.add(QueryReport.REPORT_TYPE_CALDAV_QUERY);
        REPORT_TYPES.add(MultigetReport.REPORT_TYPE_CALDAV_MULTIGET);
        REPORT_TYPES.add(FreeBusyReport.REPORT_TYPE_CALDAV_FREEBUSY);

        DEAD_PROPERTY_FILTER.add(CollectionItem.class.getName());
    }

    /** */
    public DavCollection(CollectionItem collection,
                         DavResourceLocator locator,
                         DavResourceFactory factory,
                         DavSession session) {
        super(collection, locator, factory, session);
        members = new ArrayList();
    }

    /** */
    public DavCollection(DavResourceLocator locator,
                         DavResourceFactory factory,
                         DavSession session) {
        this(new CollectionItem(), locator, factory, session);
    }

    // DavResource

    /** */
    public String getSupportedMethods() {
        return "OPTIONS, GET, HEAD, TRACE, PROPFIND, PROPPATCH, COPY, DELETE, MOVE, MKTICKET, DELTICKET, MKCOL, MKCALENDAR";
    }

    /** */
    public long getModificationTime() {
        return -1;
    }

    /** */
    public String getETag() {
        return "";
    }

    /** */
    public void spool(OutputContext outputContext)
        throws IOException {
        writeHtmlDirectoryIndex(outputContext);
    }

    /**
     * Adds the given member resource to the collection (or updates it
     * if it is an existing file resource).
     *
     * Calls the following methods:
     *
     * <ol>
     * <li> {@link #populateItem(InputContext)} on the member to
     * populate its backing item from the input context</li>
     * <li> {@link #saveSubcollection(DavCollection)} or
     * {@link #saveFile(DavFile)} to actually save the
     * member into storage</li>
     * </ol>
     *
     */
    public void addMember(DavResource member,
                          InputContext inputContext)
        throws DavException {
        ((DavResourceBase)member).populateItem(inputContext);

        if (member instanceof DavCollection) {
            saveSubcollection((DavCollection)member);
        } else {
            saveFile((DavFile)member);
        }

        members.add(member);
    }

    /** */
    public MultiStatusResponse addMember(DavResource member,
                                         InputContext inputContext,
                                         DavPropertySet properties)
        throws DavException {
        MultiStatusResponse msr =
            ((DavResourceBase)member).populateAttributes(properties);

        addMember(member, inputContext);

        return msr;
    }

    /** */
    public DavResourceIterator getMembers() {
        loadMembers();
        return new DavResourceIteratorImpl(members);
    }

    /** */
    public DavResource findMember(String href)
        throws DavException {
        if (href.startsWith(getLocator().getPrefix())) {
            // convert absolute href to relative
            href = href.substring(getLocator().getPrefix().length());
        }

        DavResourceLocator memberLocator =
            getLocator().getFactory().
            createResourceLocator(getLocator().getPrefix(),
                                  getLocator().getWorkspacePath(),
                                  href, false);
        return ((StandardDavResourceFactory)getFactory()).
            createResource(memberLocator, getSession());
    }

    /**
     * Removes the given member resource from the collection.
     *
     * Calls {@link #removeSubcollection(DavCollection)} or
     * {@link #removeFile(DavFile)} to actually remove the
     * member from storage.
     */
    public void removeMember(DavResource member)
        throws DavException {
        if (member instanceof DavCollection) {
            removeSubcollection((DavCollection)member);
        } else {
            removeFile((DavFile)member);
        }

        members.remove(member);
    }

    /** */
    public Report getReport(ReportInfo reportInfo)
        throws DavException {
        if (! exists())
            throw new DavException(DavServletResponse.SC_NOT_FOUND);

        if (! isSupportedReport(reportInfo))
            throw new DavException(DavServletResponse.SC_UNPROCESSABLE_ENTITY, "Unknown report " + reportInfo.getReportName());

        return ReportType.getType(reportInfo).createReport(this, reportInfo);
    }

    // our methods

    /** */
    public boolean isExcludedFromFreeBusyRollups() {
        return ((CollectionItem) getItem()).isExcludeFreeBusyRollup();
    }

    /** */
    protected int[] getResourceTypes() {
        return RESOURCE_TYPES;
    }

    /** */
    protected void loadLiveProperties() {
        super.loadLiveProperties();

        CollectionItem cc = (CollectionItem) getItem();
        if (cc == null)
            return;

        DavPropertySet properties = getProperties();

        properties.add(new SupportedReportSetProperty((ReportType[])REPORT_TYPES.toArray(new ReportType[0])));
        properties.add(new ExcludeFreeBusyRollup(cc.isExcludeFreeBusyRollup()));
    }

    /** */
    protected void setLiveProperty(DavProperty property) {
        super.setLiveProperty(property);

        CollectionItem cc = (CollectionItem) getItem();
        if (cc == null)
            return;

        DavPropertyName name = property.getName();
        if (property.getValue() == null)
            throw new ModelValidationException("null value for property " + name);
        String value = property.getValue().toString();

        if (name.equals(DeltaVConstants.SUPPORTED_REPORT_SET))
            throw new ModelValidationException("cannot set protected property " + name);

        if (name.equals(EXCLUDEFREEBUSYROLLUP)) {
            cc.setExcludeFreeBusyRollup(Boolean.valueOf(value));
        }
    }

    /** */
    protected void removeLiveProperty(DavPropertyName name) {
        super.removeLiveProperty(name);

        CollectionItem cc = (CollectionItem) getItem();
        if (cc == null)
            return;

        if (name.equals(DeltaVConstants.SUPPORTED_REPORT_SET))
            throw new ModelValidationException("cannot remove protected property " + name);

        if (name.equals(EXCLUDEFREEBUSYROLLUP))
            cc.setExcludeFreeBusyRollup(false);
    }

    /** */
    protected Set<String> getDeadPropertyFilter() {
        return DEAD_PROPERTY_FILTER;
    }

    /**
     * Saves the given collection resource to storage.
     */
    protected void saveSubcollection(DavCollection member)
        throws DavException {
        CollectionItem collection = (CollectionItem) getItem();

        CollectionItem subcollection = (CollectionItem) member.getItem();

        if (log.isDebugEnabled())
            log.debug("creating collection " + member.getResourcePath());

        try {
            subcollection = getContentService().
                createCollection(collection, subcollection);
            member.setItem(subcollection);
        } catch (CollectionLockedException e) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
    }

    /**
     * Saves the given file resource to storage.
     */
    protected void saveFile(DavFile member)
        throws DavException {
        CollectionItem collection = (CollectionItem) getItem();
        ContentItem content = (ContentItem) member.getItem();

        try {
            if (content.getId() != -1) {
                if (log.isDebugEnabled())
                    log.debug("updating file " + member.getResourcePath());

                content = getContentService().updateContent(content);
            } else {
                if (log.isDebugEnabled())
                    log.debug("creating file " + member.getResourcePath());

                content =
                    getContentService().createContent(collection, content);
            }
        } catch (CollectionLockedException e) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }

        member.setItem(content);
    }

    /**
     * Removes the given collection resource from storage.
     */
    protected void removeSubcollection(DavCollection member)
        throws DavException {
        CollectionItem collection = (CollectionItem) getItem();

        CollectionItem subcollection = (CollectionItem) member.getItem();

        if (log.isDebugEnabled())
            log.debug("removing collection " + subcollection.getName() +
                      " from " + collection.getName());

        try {
            getContentService().removeCollection(subcollection);
        } catch (CollectionLockedException e) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
    }

    /**
     * Removes the given file resource from storage.
     */
    protected void removeFile(DavFile member)
        throws DavException {
        CollectionItem collection = (CollectionItem) getItem();
        ContentItem content = (ContentItem) member.getItem();

        if (log.isDebugEnabled())
            log.debug("removing content " + content.getName() +
                      " from " + collection.getName());

        try {
            getContentService().removeContent(content);
        } catch (CollectionLockedException e) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
    }

    /**
     * Determines whether or not the report indicated by the given
     * report info is supported by this collection.
     */
    protected boolean isSupportedReport(ReportInfo info) {
        for (Iterator<ReportType> i=REPORT_TYPES.iterator(); i.hasNext();) {
            if (i.next().isRequestedReportType(info))
                return true;
        }
        return false;
    }

    private void loadMembers() {
        for (Iterator i=((CollectionItem)getItem()).getChildren().iterator();
             i.hasNext();) {
            Item memberItem = (Item) i.next();
            
            String memberPath = getResourcePath() + "/" + memberItem.getName();
            try {
                DavResourceLocator memberLocator =
                    getLocator().getFactory().
                    createResourceLocator(getLocator().getPrefix(),
                                          getLocator().getWorkspacePath(),
                                          memberPath, false);
                DavResource member =
                    ((StandardDavResourceFactory)getFactory()).
                    createResource(memberLocator, getSession(), memberItem);
                members.add(member);
            } catch (DavException e) {
                // should never happen
                log.error("error loading member resource for item " +
                          memberItem.getName() + " in collection " +
                          getResourcePath(), e);
            }
        }
    }

    // creates a DavResource wrapping the given member item and adds
    // it to the internal members list
    private void stashMember(Item memberItem) {
        if (log.isDebugEnabled())
            log.debug("stashing member " + memberItem.getName());

        String memberPath = getResourcePath() + "/" + memberItem.getName();
        try {
            DavResourceLocator memberLocator =
                getLocator().getFactory().
                createResourceLocator(getLocator().getPrefix(),
                                      getLocator().getWorkspacePath(),
                                      memberPath, false);
            DavResource member =
                ((StandardDavResourceFactory)getFactory()).
                createResource(memberLocator, getSession(), memberItem);

            members.add(member);
        } catch (DavException e) {
            // should never happen
            log.error("error stashing member resource for item " +
                      memberItem.getName() + " in collection " +
                      getResourcePath(), e);
        }
    }

    private void writeHtmlDirectoryIndex(OutputContext context)
        throws IOException {
        if (log.isDebugEnabled())
            log.debug("writing html directory index for  " +
                      getItem().getName());

        context.setContentType(IOUtil.buildContentType("text/html", "UTF-8"));
        // XXX content length unknown unless we write a temp file
        // modification time and etag are undefined for a collection

        if (! context.hasStream()) {
            return;
        }

        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(context.getOutputStream(),
                                                   "utf8"));

        String title = getItem().getDisplayName();
        if (title == null)
            title = getItem().getUid();

        writer.write("<html><head><title>");
        writer.write(StringEscapeUtils.escapeHtml(title));
        writer.write("</title></head>");
        writer.write("<body>");
        writer.write("<h1>");
        writer.write(StringEscapeUtils.escapeHtml(title));
        writer.write("</h1>");
        writer.write("<ul>");
        if (! isHomeCollection()) {
            DavResource parent = getCollection();
            writer.write("<li><a href=\"");
            writer.write(parent.getLocator().getHref(true));
            writer.write("\">..</a></li>");
        };
        for (DavResourceIterator i=getMembers(); i.hasNext();) {
            DavResourceBase child = (DavResourceBase) i.nextResource();
            String displayName = child.getItem().getDisplayName();
            writer.write("<li><a href=\"");
            writer.write(child.getLocator().getHref(child.isCollection()));
            writer.write("\">");
            writer.write(StringEscapeUtils.escapeHtml(displayName));
            writer.write("</a></li>");
        }
        writer.write("</ul>");
        writer.write("</body>");
        writer.write("</html>");
        writer.write("\n");
        writer.close();
    }
}
