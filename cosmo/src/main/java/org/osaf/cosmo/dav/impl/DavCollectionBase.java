/*
 * Copyright 2006-2007 Open Source Applications Foundation
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
import org.osaf.cosmo.dav.LockedException;
import org.osaf.cosmo.dav.NotFoundException;
import org.osaf.cosmo.dav.ProtectedPropertyModificationException;
import org.osaf.cosmo.dav.UnprocessableEntityException;
import org.osaf.cosmo.dav.caldav.report.FreeBusyReport;
import org.osaf.cosmo.dav.caldav.report.MultigetReport;
import org.osaf.cosmo.dav.caldav.report.QueryReport;
import org.osaf.cosmo.dav.property.DavProperty;
import org.osaf.cosmo.dav.property.ExcludeFreeBusyRollup;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.CollectionLockedException;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;

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
public class DavCollectionBase extends DavItemResourceBase
    implements DavItemCollection {
    private static final Log log = LogFactory.getLog(DavCollectionBase.class);
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
    public DavCollectionBase(CollectionItem collection,
                             DavResourceLocator locator,
                             DavResourceFactory factory)
        throws DavException {
        super(collection, locator, factory);
        members = new ArrayList();
    }

    /** */
    public DavCollectionBase(DavResourceLocator locator,
                             DavResourceFactory factory)
        throws DavException {
        this(new CollectionItem(), locator, factory);
    }

    // Jackrabbit DavResource

    /** */
    public String getSupportedMethods() {
        return "OPTIONS, GET, HEAD, TRACE, PROPFIND, PROPPATCH, COPY, DELETE, MOVE, MKTICKET, DELTICKET, MKCOL, MKCALENDAR";
    }

    /** */
    public boolean isCollection() {
        return true;
    }

    /** */
    public long getModificationTime() {
        return -1;
    }

    /** */
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
        try {
            for (Item memberItem : ((CollectionItem)getItem()).getChildren())
                members.add(memberToResource(memberItem));
            return new DavResourceIteratorImpl(members);
        } catch (DavException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeMember(org.apache.jackrabbit.webdav.DavResource member)
        throws org.apache.jackrabbit.webdav.DavException {
        if (member instanceof DavItemCollection) {
            removeSubcollection((DavItemCollection)member);
        } else {
            removeContent((DavItemContent)member);
        }

        members.remove(member);
    }

    // DavResource

    public Report getReport(ReportInfo reportInfo)
        throws DavException {
        if (! exists())
            throw new NotFoundException();

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
        DavContentBase base = (DavContentBase) content;
        base.populateItem(context);
        saveContent(base);
        members.add(base);
    }

    public MultiStatusResponse addCollection(DavCollection collection,
                                             DavPropertySet properties)
        throws DavException {
        DavCollectionBase base = (DavCollectionBase) collection;
        base.populateItem(null);
        MultiStatusResponse msr = base.populateAttributes(properties);
        if (! msr.hasNonOk()) {
            saveSubcollection(base);
            members.add(base);
        }
        return msr;
    }

    public DavResource findMember(String href)
        throws DavException {
        if (href.startsWith(getLocator().getPrefix())) {
            // convert absolute href to relative
            href = href.substring(getLocator().getPrefix().length());
        }
        return memberToResource(href);
    }

    // DavItemCollection

    public boolean isCalendarCollection() {
        return false;
    }

    public boolean isHomeCollection() {
        return false;
    }

    public boolean isExcludedFromFreeBusyRollups() {
        return ((CollectionItem) getItem()).isExcludeFreeBusyRollup();
    }

    // our methods

    /** */
    protected int[] getResourceTypes() {
        return RESOURCE_TYPES;
    }

    /** */
    protected void loadLiveProperties(DavPropertySet properties) {
        super.loadLiveProperties(properties);

        CollectionItem cc = (CollectionItem) getItem();
        if (cc == null)
            return;

        log.debug("loading collection live properties");

        properties.add(new SupportedReportSetProperty((ReportType[])REPORT_TYPES.toArray(new ReportType[0])));
        properties.add(new ExcludeFreeBusyRollup(cc.isExcludeFreeBusyRollup()));
    }

    /** */
    protected void setLiveProperty(DavProperty property)
        throws DavException {
        super.setLiveProperty(property);

        CollectionItem cc = (CollectionItem) getItem();
        if (cc == null)
            return;

        DavPropertyName name = property.getName();
        if (property.getValue() == null)
            throw new UnprocessableEntityException("Property " + name + " requires a value");

        if (name.equals(DeltaVConstants.SUPPORTED_REPORT_SET))
            throw new ProtectedPropertyModificationException(name);

        if (name.equals(EXCLUDEFREEBUSYROLLUP)) {
            Boolean flag = Boolean.valueOf(property.getValueText());
            cc.setExcludeFreeBusyRollup(flag);
        }
    }

    /** */
    protected void removeLiveProperty(DavPropertyName name)
        throws DavException {
        super.removeLiveProperty(name);

        CollectionItem cc = (CollectionItem) getItem();
        if (cc == null)
            return;

        if (name.equals(DeltaVConstants.SUPPORTED_REPORT_SET))
            throw new ProtectedPropertyModificationException(name);

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
    protected void saveSubcollection(DavItemCollection member)
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
            throw new LockedException();
        }
    }

    /**
     * Saves the given content resource to storage.
     */
    protected void saveContent(DavItemContent member)
        throws DavException {
        CollectionItem collection = (CollectionItem) getItem();
        ContentItem content = (ContentItem) member.getItem();

        try {
            if (content.getId() != -1) {
                if (log.isDebugEnabled())
                    log.debug("updating member " + member.getResourcePath());

                content = getContentService().updateContent(content);
            } else {
                if (log.isDebugEnabled())
                    log.debug("creating member " + member.getResourcePath());

                content =
                    getContentService().createContent(collection, content);
            }
        } catch (CollectionLockedException e) {
            throw new LockedException();
        }

        member.setItem(content);
    }

    /**
     * Removes the given collection resource from storage.
     */
    protected void removeSubcollection(DavItemCollection member)
        throws DavException {
        CollectionItem collection = (CollectionItem) getItem();
        CollectionItem subcollection = (CollectionItem) member.getItem();

        if (log.isDebugEnabled())
            log.debug("removing collection " + subcollection.getName() +
                      " from " + collection.getName());

        try {
            getContentService().removeCollection(subcollection);
        } catch (CollectionLockedException e) {
            throw new LockedException();
        }
    }

    /**
     * Removes the given content resource from storage.
     */
    protected void removeContent(DavItemContent member)
        throws DavException {
        CollectionItem collection = (CollectionItem) getItem();
        ContentItem content = (ContentItem) member.getItem();

        if (log.isDebugEnabled())
            log.debug("removing content " + content.getName() +
                      " from " + collection.getName());

        try {
            getContentService().removeContent(content);
        } catch (CollectionLockedException e) {
            throw new LockedException();
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

    protected DavResource memberToResource(Item item)
        throws DavException {
        String path = getResourcePath() + "/" + item.getName();
        DavResourceLocator locator = getLocator().getFactory().
            createResourceLocator(getLocator().getPrefix(),
                                  getLocator().getWorkspacePath(), path,
                                  false);
        return getResourceFactory().createResource(locator, item);
    }    

    protected DavResource memberToResource(String path)
        throws DavException {
        DavResourceLocator locator = getLocator().getFactory().
            createResourceLocator(getLocator().getPrefix(),
                                  getLocator().getWorkspacePath(), path,
                                  false);
        return getResourceFactory().resolve(locator);
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
        org.apache.jackrabbit.webdav.DavResource parent = getCollection();
        if (parent != null) {
            writer.write("<li><a href=\"");
            writer.write(parent.getLocator().getHref(true));
            writer.write("\">..</a></li>");
        }
        for (DavResourceIterator i=getMembers(); i.hasNext();) {
            DavItemResourceBase child = (DavItemResourceBase) i.nextResource();
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
