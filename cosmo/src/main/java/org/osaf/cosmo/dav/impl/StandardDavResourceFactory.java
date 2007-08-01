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

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;

import org.osaf.cosmo.dav.CosmoDavMethods;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.icalendar.ICalendarConstants;
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
import org.osaf.cosmo.util.PathUtil;

/**
 * Implementation of <code>DavResourceFactory</code> that constructs
 * instances of <code>DavResource</code> which adapt the Cosmo model
 * to the jcr-server model.
 *
 * @see org.apache.jackrabbit.webdav.DavResourceFactory
 * @see org.apache.jackrabbit.webdav.DavResource
 */
public class StandardDavResourceFactory
    implements DavResourceFactory, ICalendarConstants {
    private static final Log log =
        LogFactory.getLog(StandardDavResourceFactory.class);

    private ContentService contentService;
    private CosmoSecurityManager securityManager;

    // DavResourceFactory

    /** */
    public DavResource createResource(DavResourceLocator locator,
                                      DavServletRequest request,
                                      DavServletResponse response)
        throws DavException {
        return createResource(locator, request.getDavSession());

//        DavResource resource =
//            createResource(locator, request.getDavSession());
//        if (resource != null)
//            return resource;

//        int methodCode = DavMethods.getMethodCode(request.getMethod());
//        if (methodCode == 0)
//            methodCode = CosmoDavMethods.getMethodCode(request.getMethod());
//        if (methodCode == 0)
             // we don't understand the method presented to us
//            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);

        // for creation methods, return a "blank" resource
//        if (CosmoDavMethods.DAV_MKCALENDAR == methodCode)
//            return new DavCalendarCollection(locator, this,
//                                             request.getDavSession());

//        if (DavMethods.DAV_MKCOL == methodCode)
//            return new DavCollection(locator, this, request.getDavSession());

//        if (DavMethods.DAV_PUT == methodCode) {
//            return new DavFile(locator, this, request.getDavSession());
//       }

//        throw new DavException(DavServletResponse.SC_NOT_FOUND);
    }

    /** */
    public DavResource createResource(DavResourceLocator locator,
                                      DavSession session)
        throws DavException {
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
            // this means the item doesn't exist in storage -
            // either it's about to be created, or the request will
            // result in a not found error
            return null;

        return createResource(locator, session, item);
    }

    // our methods

    /** */
    public DavResource createResource(DavResourceLocator locator,
                                      DavSession session,
                                      Item item)
        throws DavException {
        if (item == null)
            throw new IllegalArgumentException("item cannot be null");

        if (log.isDebugEnabled())
            log.debug("instantiating dav resource for item " + item.getUid() +
                      " at path " + locator.getResourcePath());

        if (item instanceof HomeCollectionItem)
            return new DavHomeCollection((HomeCollectionItem) item, locator,
                                         this, session);

        if (item instanceof CollectionItem) {
            if(item.getStamp(CalendarCollectionStamp.class) != null) {
                return new DavCalendarCollection((CollectionItem) item,
                        locator, this, session);
            } else {
                return new DavCollection((CollectionItem) item, locator, this,
                    session);
            }
        }

        if (item instanceof NoteItem) {
            if(item.getStamp(EventStamp.class) != null)
                return new DavEvent((NoteItem) item, locator, this,
                        session);
            else if (item.getStamp(TaskStamp.class) != null)
                return new DavTask((NoteItem) item, locator, this, session);
            else 
                return new DavJournal((NoteItem) item, locator, this,
                                      session);
        } 
            
        return new DavFile((FileItem) item, locator, this, session);
    }

    public DavResource createCalendarResource(DavResourceLocator locator,
                                              DavServletRequest request,
                                              DavServletResponse response,
                                              Calendar calendar)
        throws DavException {
        if (! calendar.getComponents(Component.VEVENT).isEmpty())
            return new DavEvent(locator, this, request.getDavSession());
        if (! calendar.getComponents(Component.VTODO).isEmpty())
            return new DavTask(locator, this, request.getDavSession());
        if (! calendar.getComponents(Component.VJOURNAL).isEmpty())
            return new DavJournal(locator, this, request.getDavSession());
        // CALDAV:supported-calendar-component
        throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Calendar object must contain at least one of " + StringUtils.join(SUPPORTED_COMPONENT_TYPES, ", "));
    }

    /** */
    public ContentService getContentService() {
        return contentService;
    }

    /** */
    public void setContentService(ContentService service) {
        contentService = service;
    }

    /** */
    public CosmoSecurityManager getSecurityManager() {
        return securityManager;
    }

    /** */
    public void setSecurityManager(CosmoSecurityManager manager) {
        securityManager = manager;
    }
}
