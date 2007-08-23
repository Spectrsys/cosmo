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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;

import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.ProtectedPropertyModificationException;
import org.osaf.cosmo.dav.property.DavProperty;
import org.osaf.cosmo.dav.property.Etag;
import org.osaf.cosmo.dav.property.LastModified;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.MessageStamp;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.TriageStatus;
import org.osaf.cosmo.model.User;

/**
 * Extends <code>DavItemResourceBase</code> to adapt the Cosmo
 * <code>ContentItem</code> to the DAV resource model.
 *
 * This class defines the following live properties:
 *
 * <ul>
 * <li><code>DAV:getetag</code> (protected)</li>
 * <li><code>DAV:getlastmodified</code> (protected)</li>
 * </ul>
 *
 * @see DavContent
 * @see DavResourceBase
 * @see ContentItem
 */
public abstract class DavContentBase extends DavItemResourceBase
    implements DavItemContent {
    private static final Log log = LogFactory.getLog(DavContentBase.class);
    private static final int[] RESOURCE_TYPES;
    private static final Set<String> DEAD_PROPERTY_FILTER =
        new HashSet<String>();

    static {
        registerLiveProperty(DavPropertyName.GETETAG);
        registerLiveProperty(DavPropertyName.GETLASTMODIFIED);

        RESOURCE_TYPES = new int[] { ResourceType.DEFAULT_RESOURCE };

        DEAD_PROPERTY_FILTER.add(NoteItem.class.getName());
        DEAD_PROPERTY_FILTER.add(MessageStamp.class.getName());
    }

    /** */
    public DavContentBase(ContentItem item,
                          DavResourceLocator locator,
                          DavResourceFactory factory)
        throws DavException {
        super(item, locator, factory);
    }


    // Jackrabbit DavResource

    /** */
    public boolean isCollection() {
        return false;
    }

    /** */
    public String getSupportedMethods() {
        return "OPTIONS, GET, HEAD, POST, TRACE, PROPFIND, PROPPATCH, COPY, PUT, DELETE, MOVE, MKTICKET, DELTICKET";
    }

    /** */
    public long getModificationTime() {
        if (getItem() == null)
            return -1;
        if (getItem().getModifiedDate() == null)
            return new Date().getTime();
        return getItem().getModifiedDate().getTime();
    }

    /** */
    public abstract void spool(OutputContext outputContext)
        throws IOException;

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

    // DavResource methods

    /** */
    public Report getReport(ReportInfo reportInfo)
        throws DavException {
        throw new UnsupportedOperationException();
    }

    // our methods

    /** */
    protected int[] getResourceTypes() {
        return RESOURCE_TYPES;
    }

    /** */
    protected void populateItem(InputContext inputContext)
        throws DavException {
        super.populateItem(inputContext);

        ContentItem content = (ContentItem) getItem();
        
        User user = getSecurityManager().getSecurityContext().getUser();
        content.setLastModifiedBy(user != null ? user.getEmail() : "");

        if (content.getUid() == null) {
            content.setTriageStatus(TriageStatus.createInitialized());
            content.setLastModification(ContentItem.Action.CREATED);
            content.setSent(Boolean.FALSE);
            content.setNeedsReply(Boolean.FALSE);
        } else {
            content.setLastModification(ContentItem.Action.EDITED);
        }
    }
    
    
    /** */
    protected void loadLiveProperties(DavPropertySet properties) {
        super.loadLiveProperties(properties);

        ContentItem content = (ContentItem) getItem();
        if (content == null)
            return;

        properties.add(new Etag(getETag()));
        properties.add(new LastModified(content.getModifiedDate()));
    }

    /** */
    protected void setLiveProperty(DavProperty property)
        throws DavException {
        super.setLiveProperty(property);

        ContentItem content = (ContentItem) getItem();
        if (content == null)
            return;

        DavPropertyName name = property.getName();
        if (name.equals(DavPropertyName.GETCONTENTLENGTH) ||
            name.equals(DavPropertyName.GETETAG) ||
            name.equals(DavPropertyName.GETLASTMODIFIED))
            throw new ProtectedPropertyModificationException(name);
    }

    /** */
    protected void removeLiveProperty(DavPropertyName name)
        throws DavException {
        super.removeLiveProperty(name);

        ContentItem content = (ContentItem) getItem();
        if (content == null)
            return;

        if (name.equals(DavPropertyName.GETCONTENTLENGTH) ||
            name.equals(DavPropertyName.GETETAG) ||
            name.equals(DavPropertyName.GETLASTMODIFIED) ||
            name.equals(DavPropertyName.GETCONTENTTYPE))
            throw new ProtectedPropertyModificationException(name);
    }

    /** */
    protected Set<String> getDeadPropertyFilter() {
        return DEAD_PROPERTY_FILTER;
    }
}
