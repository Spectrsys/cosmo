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
package org.osaf.cosmo.ui.account;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.dao.NoSuchResourceException;
import org.osaf.cosmo.model.CalendarCollectionStamp;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.CosmoSecurityManager;
import org.osaf.cosmo.security.Permission;
import org.osaf.cosmo.service.ContentService;
import org.osaf.cosmo.ui.bean.CalendarBean;
import org.osaf.cosmo.ui.bean.EventBean;
import org.osaf.cosmo.util.PathUtil;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * Controller for browsing the contents of a user's account.
 * <p>
 * Each controller method acts on the item specified by the
 * <code>path</code> request parameter.
 * <p>
 * Each controller method adds the item's display name (and optionally
 * other pieces of data) to a <code>List</code> in the
 * <code>TitleParam</code> request attribute which can be used by
 * views to render dynamic page titles.
 */
public class AccountBrowsingController extends MultiActionController {
    private static final Log log =
        LogFactory.getLog(AccountBrowsingController.class);

    private ContentService contentService;
    private CosmoSecurityManager securityManager;
    private String calendarView;
    private String eventView;
    private String browseCollectionView;
    private String browseItemView;
    private String removeViewBase;
    private String revokeTicketBaseView;

    /**
     * Retrieves the requested item so that its attributes, properties
     * and children may be browsed. Sets its path into the
     * <code>Path</code> request attribute.
     * <p>
     * If the item is a collection, sets it into the
     * <code>Collection</code> request attribute and returns the
     * {@link #browseCollectionView}. Otherwise, sets it into the
     * <code>Item</code> request attribute and returns the
     * {@link #browseItemView}.
     *
     * @throws ResourceNotFoundException if an item is not found at
     * the given path
     * @throws PermissionException if the logged-in user does not have
     * read permission on the item
     */
    public ModelAndView browse(HttpServletRequest request,
                               HttpServletResponse response)
        throws Exception {
        String path = getPath(request);
        Item item = getItem(path);
        securityManager.checkPermission(item, Permission.READ);

        if (log.isDebugEnabled())
            log.debug("browsing item " + item.getUid() + " at " + path);

        addTitleParam(request, item.getDisplayName());
        request.setAttribute("Path", path);

        if (item instanceof CollectionItem) {
            CollectionItem collection = (CollectionItem) item;
            request.setAttribute("Collection", item);
            return new ModelAndView(browseCollectionView);
        }

        request.setAttribute("Item", item);
        return new ModelAndView(browseItemView);
    }

    /**
     * Removes the requested item. Returns the
     * {@link #removeViewBase} with the item path appended.
     *
     * @throws ResourceNotFoundException if an item is not found at
     * the given path
     * @throws PermissionException if the logged-in user does not have
     * write permission on the item
     */
    public ModelAndView remove(HttpServletRequest request,
                               HttpServletResponse response)
        throws Exception {
        String path = getPath(request);
        Item item = getItem(path);
        securityManager.checkPermission(item, Permission.WRITE);

        if (log.isDebugEnabled())
            log.debug("removing item " + item.getUid() + " from " + path);

        contentService.removeItem(item);

        String parentPath = PathUtil.getParentPath(path);
        return new ModelAndView(removeViewBase + parentPath);
    }

    /**
     * Retrieves the requested item so that its full contents may be
     * viewed inline in the UI. Sets its path into the
     * <code>Path</code> request attribute.
     * <p>
     * If the item has a calendar collection stamp, sets the item into
     * the <code>Collection</code> request attribute, sets its
     * aggregate calendar content as a <code>CalendarBean</code> into
     * the <code>Calendar</code> request attribute, and returns the
     * {@link #calendarView}.
     * <p>
     * If the item has an event stamp, sets the item into the
     * <code>Item</code> request attribute, sets its calendar content
     * as an <code>EventBean</code> into the <code>Event</code> and
     * returns the  {@link #eventView}.
     * <p>
     *
     * @throws ResourceNotFoundException if an item is not found at
     * the given path
     * @throws PermissionException if the logged-in user does not have
     * read permission on the item
     * @throws ServletException if the item does not have a calendar
     * collection stamp or an event stamp
     */
    public ModelAndView view(HttpServletRequest request,
                             HttpServletResponse response)
        throws Exception {
        String path = getPath(request);
        Item item = getItem(path);
        securityManager.checkPermission(item, Permission.READ);

        if (log.isDebugEnabled())
            log.debug("viewing item " + item.getUid() + " at " + path);

        addTitleParam(request, item.getDisplayName());
        request.setAttribute("Path", path);

        if (item.getStamp(EventStamp.class)!=null) {
            EventStamp eventStamp = EventStamp.getStamp(item);
            request.setAttribute("Item", item);
            request.setAttribute("Event", new EventBean(eventStamp));

            return new ModelAndView(eventView);
        } else if (item.getStamp(CalendarCollectionStamp.class)!=null) {
            CalendarCollectionStamp calendar = 
                CalendarCollectionStamp.getStamp(item);
            request.setAttribute("Collection", item);
            request.setAttribute("Calendar", new CalendarBean(calendar));

            return new ModelAndView(calendarView);
        }

        throw new ServletException("item of type " + item.getClass().getName()
                + " at " + path + " cannot be viewed");
    }

    /**
     * Retrieves the requested item so that its content may be
     * downloaded.
     * <p>
     * If the item has a calendar collection stamp, sends a
     * <code>text/calendar</code> response containing the aggregate
     * calendar content and returns <code>null</code>.
     * <p>
     * If the item has an event stamp, sends a
     * <code>text/calendar</code> response containing the event
     * content and returns <code>null</code>.
     * <p>
     * If the item is a content item, sends a response according to
     * the item's content type containing the item's content.
     *
     * @throws ResourceNotFoundException if an item is not found at
     * the given path
     * @throws PermissionException if the logged-in user does not have
     * read permission on the item
     * @throws ServletException if the item does not have a calendar
     * collection stamp or an event stamp and is not a content item
     */
    public ModelAndView download(HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        String path = getPath(request);
        Item item = getItem(path);
        securityManager.checkPermission(item, Permission.READ);

        if (log.isDebugEnabled())
            log.debug("downloading item " + item.getUid() + " at " + path);

        if (item.getStamp(EventStamp.class) != null) {
            spoolEvent(EventStamp.getStamp(item), request,
                    response);
        } else if (item instanceof ContentItem) {
            spoolItem((ContentItem) item, request, response);
            return null;
        } else if (item.getStamp(CalendarCollectionStamp.class) != null) {
            spoolCalendar(CalendarCollectionStamp.getStamp(item), request, response);
            return null;
        }
        throw new ServletException("item of type " + item.getClass().getName()
                + " at " + path + " cannot be downloaded");
    }

    /**
     * Revokes the requested ticket from the requested item. Returns
     * the {@link #revokeTicketBaseView} with the item path appended.
     *
     * @throws ResourceNotFoundException if an item is not found at
     * the given path
     * @throws PermissionException if the logged-in user does not have
     * write permission on the item
     */
    public ModelAndView revokeTicket(HttpServletRequest request,
                                     HttpServletResponse response)
        throws Exception {
        String key = request.getParameter("ticket");
        String path = getPath(request);
        Item item = getItem(path);
        securityManager.checkPermission(item, Permission.WRITE);

        if (log.isDebugEnabled())
            log.debug("revoking ticket " + key + " from item " +
                      item.getUid() + " at " + path);

        contentService.removeTicket(path, key);

        return new ModelAndView(this.revokeTicketBaseView + path);
    }

    public ContentService getContentService() {
        return contentService;
    }

    public void setContentService(ContentService service) {
        this.contentService = service;
    }

    public CosmoSecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(CosmoSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setCalendarView(String calendarView) {
        this.calendarView = calendarView;
    }

    public void setEventView(String eventView) {
        this.eventView = eventView;
    }

    public void setBrowseCollectionView(String browseCollectionView) {
        this.browseCollectionView = browseCollectionView;
    }

    public void setBrowseItemView(String browseItemView) {
        this.browseItemView = browseItemView;
    }

    public void setRemoveViewBase(String removeViewBase) {
        this.removeViewBase = removeViewBase;
    }

    public String getRevokeTicketBaseView() {
        return revokeTicketBaseView;
    }

    public void setRevokeTicketBaseView(String revokeTicketBaseView) {
        this.revokeTicketBaseView = revokeTicketBaseView;
    }
    
    private String getPath(HttpServletRequest request){
        String path = request.getParameter("path");
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        return path;
    }

    private Item getItem(String path) {
        Item item = contentService.findItemByPath(path);
        if (item == null)
            throw new NoSuchResourceException(path);
        return item;
    }

    private void addTitleParam(HttpServletRequest request, String param) {
        List<String> params = (List) request.getAttribute("TitleParam");
        if (params == null) {
            params = new ArrayList<String>();
            request.setAttribute("TitleParam", params);
        }
        params.add(param);
    }

    private void spoolItem(ContentItem item, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // set headers
        response.setStatus(HttpServletResponse.SC_OK);
        if (item.getContentType() != null) {
            response.setContentType(item.getContentType());
        } else {
            response.setContentType("application/octet-stream");
        }
        if (item.getContentLength() != null) {
            response.setContentLength(item.getContentLength().intValue());
        }
        if (item.getContentEncoding() != null) {
            response.setCharacterEncoding(item.getContentEncoding());
        }
        if (item.getContentLanguage() != null) {
            response.setHeader("Content-Language", item.getContentLanguage());
        }

        // spool data
        OutputStream out = response.getOutputStream();
        // XXX no stram api on ContentItem
        out.write(item.getContent());
        // InputStream in = item.getContent();
        // try {
        // byte[] buffer = new byte[8192];
        // int read;
        // while ((read = in.read(buffer)) >= 0) {
        // out.write(buffer, 0, read);
        // }
        // } finally {
        // in.close();
        // }
        response.flushBuffer();
    }

    private void spoolEvent(EventStamp event, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ParserException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/calendar");
        response.setCharacterEncoding("UTF-8");
        
        // spool data
        CalendarOutputter outputter = new CalendarOutputter();
             
        // since the content was validated when the event item was
        // imported, there's no need to do it here
        outputter.setValidating(false);
        try {
            outputter.output(event.getCalendar(), response
                    .getOutputStream());
        } catch (ValidationException e) {
            log.error("invalid output calendar?!", e);
        }
        response.flushBuffer();
    }
    
    private void spoolCalendar(CalendarCollectionStamp calendar,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ParserException {
        // set headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/calendar");
        response.setCharacterEncoding("UTF-8");
        // XXX we can't know content length unless we write to a temp
        // item and then spool that

        // spool data
        CalendarOutputter outputter = new CalendarOutputter();
        
        // since the content was validated when the event item was
        // imported, there's no need to do it here
        outputter.setValidating(false);
        try {
            outputter.output(calendar.getCalendar(), response
                    .getOutputStream());
        } catch (ValidationException e) {
            log.error("invalid output calendar?!", e);
        }
        response.flushBuffer();
    }
}
