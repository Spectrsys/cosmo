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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;

import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.NotFoundException;
import org.osaf.cosmo.model.CalendarCollectionStamp;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.FileItem;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.TaskStamp;
import org.osaf.cosmo.security.CosmoSecurityManager;
import org.osaf.cosmo.server.CollectionPath;
import org.osaf.cosmo.server.ItemPath;
import org.osaf.cosmo.service.ContentService;

/**
 * Implementation of <code>DavResourceFactory</code> that constructs
 * instances of <code>DavResource</code> which adapt the Cosmo model
 * to the jcr-server model.
 *
 * @see org.apache.jackrabbit.webdav.DavResourceFactory
 * @see org.apache.jackrabbit.webdav.DavResource
 */
public class StandardDavResourceFactory
    implements DavResourceFactory {
    private static final Log log =
        LogFactory.getLog(StandardDavResourceFactory.class);

    private ContentService contentService;
    private CosmoSecurityManager securityManager;

    // DavResourceFactory

    /** */
    public DavResource createResource(DavResourceLocator locator,
                                      DavServletRequest request,
                                      DavServletResponse response)
        throws org.apache.jackrabbit.webdav.DavException {
        DavResource resource = createResource(locator, null);
        if (resource != null)
            return resource;

        // we didn't find an item in storage for the resource, so either
        // the request is creating a resource or the request is targeting a
        // nonexistent item.
        if (request.getMethod().equals("MKCALENDAR"))
            return new DavCalendarCollection(locator, this);
        if (request.getMethod().equals("MKCOL"))
            return new DavCollection(locator, this);
        if (request.getMethod().equals("PUT"))
            // will be replaced by the provider if a different resource
            // type is required
            return new DavFile(locator, this);
        throw new NotFoundException();
    }

    /** */
    public DavResource createResource(DavResourceLocator locator,
                                      DavSession session)
        throws org.apache.jackrabbit.webdav.DavException {
        String path = locator.getResourcePath();
        Item item = null;

        CollectionPath cp = CollectionPath.parse(path, true);
        if (cp != null)
            item = cp.getPathInfo() != null ?
                contentService.findItemByPath(cp.getPathInfo(), cp.getUid()) :
                contentService.findItemByUid(cp.getUid());

        if (item == null) {
            ItemPath ip = ItemPath.parse(path, true);
            if (ip != null)
                item = ip.getPathInfo() != null ?
                    contentService.findItemByPath(ip.getPathInfo(),
                                                  ip.getUid()) :
                    contentService.findItemByUid(ip.getUid());
        }

        if (item == null)
            item = contentService.findItemByPath(path);

        if (item == null)
            return null;

        return itemToResource(locator, item);
    }

    // our methods

    /** */
    public DavResource itemToResource(DavResourceLocator locator,
                                      Item item) {
        if (item == null)
            throw new IllegalArgumentException("item cannot be null");

        if (item instanceof HomeCollectionItem)
            return new DavHomeCollection((HomeCollectionItem) item, locator,
                                         this);

        if (item instanceof CollectionItem) {
            if (item.getStamp(CalendarCollectionStamp.class) != null)
                return new DavCalendarCollection((CollectionItem) item,
                        locator, this);
            else
                return new DavCollection((CollectionItem) item, locator, this);
        }

        if (item instanceof NoteItem) {
            if (item.getStamp(EventStamp.class) != null)
                return new DavEvent((NoteItem) item, locator, this);
            else if (item.getStamp(TaskStamp.class) != null)
                return new DavTask((NoteItem) item, locator, this);
            else 
                return new DavJournal((NoteItem) item, locator, this);
        } 
            
        return new DavFile((FileItem) item, locator, this);
    }

    /** */
    public ContentService getContentService() {
        return contentService;
    }

    /** */
    public void setContentService(ContentService service) {
        contentService = service;
    }

    public CosmoSecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(CosmoSecurityManager securityManager) {
        this.securityManager = securityManager;
    }
}
