/*
 * Copyright 2005 Open Source Applications Foundation
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
package org.osaf.cosmo.ui.home;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.osaf.cosmo.dao.NoSuchResourceException;
import org.osaf.cosmo.model.CalendarCollectionResource;
import org.osaf.cosmo.model.CollectionResource;
import org.osaf.cosmo.model.EventResource;
import org.osaf.cosmo.model.FileResource;
import org.osaf.cosmo.model.Resource;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.service.HomeDirectoryService;
import org.osaf.cosmo.ui.CosmoAction;
import org.osaf.cosmo.ui.UIConstants;
import org.osaf.cosmo.ui.bean.EventBean;
import org.osaf.cosmo.ui.bean.CalendarBean;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Action for browsing a user's home directory.
 */
public class HomeDirectoryBrowserAction extends CosmoAction {
    private static final Log log =
        LogFactory.getLog(HomeDirectoryBrowserAction.class);

    private HomeDirectoryService homeDirectoryService;

    /**
     */
    public static final String PARAM_PATH = "path";
    /**
     */
    public static final String PARAM_TICKET = "ticket";
    /**
     */
    public static final String ATTR_COLLECTION = "Collection";
    /**
     */
    public static final String ATTR_RESOURCE = "Resource";
    /**
     */
    public static final String ATTR_CALENDAR = "Calendar";
    /**
     */
    public static final String ATTR_EVENT = "Event";
    /**
     */
    public static final String FWD_COLLECTION = "collection";
    /**
     */
    public static final String FWD_RESOURCE = "resource";
    /**
     */
    public static final String FWD_EVENT = "event";
    /**
     */
    public static final String FWD_CALENDAR = "calendar";

    /**
     * Retrieves the resource or collection specified by the
     * {@link #PARAM_PATH} parameter and sets it into the
     * {@link #ATTR_COLLECTION} or {@link #ATTR_RESOURCE} request
     * attribute as appropriate.
     */
    public ActionForward browse(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        String path = request.getParameter(PARAM_PATH);

        Resource resource = getResource(path);
        addTitleParam(request, resource.getPath());

        if (resource instanceof CollectionResource) {
            request.setAttribute(ATTR_COLLECTION, resource);
            return mapping.findForward(FWD_COLLECTION);
        }
 
        request.setAttribute(ATTR_RESOURCE, resource);
        return mapping.findForward(FWD_RESOURCE);
    }

    /**
     * Removes the resource or collection specified by the
     * {@link #PARAM_PATH} parameter.
     */
    public ActionForward remove(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        String path = request.getParameter(PARAM_PATH);

        if (log.isDebugEnabled()) {
            log.debug("removing resource at " + path);
        }

        homeDirectoryService.removeResource(path);

        String parentPath = path.substring(0, path.lastIndexOf('/'));

        return copyForward(mapping.findForward(UIConstants.FWD_SUCCESS),
                           parentPath);
    }

    /**
     * Retrieves the event resource or calendar collection specified
     * by the {@link #PARAM_PATH} parameter, extracts the calendar
     * content and sets it into the {@link #ATTR_CALENDAR} or
     * {@link #ATTR_EVENT} request attribute, and sets the collection
     * or resource into the {@link #ATTR_COLLECTION} or
     * {@link #ATTR_RESOURCE} request attribute as appropriate.
     */
    public ActionForward view(ActionMapping mapping,
                              ActionForm form,
                              HttpServletRequest request,
                              HttpServletResponse response)
        throws Exception {
        String path = request.getParameter(PARAM_PATH);

        Resource resource = getResource(path);
        addTitleParam(request, resource.getPath());

        if (resource instanceof EventResource) {
            EventResource event = (EventResource) resource;
            request.setAttribute(ATTR_RESOURCE, resource);
            request.setAttribute(ATTR_EVENT, new EventBean(event));
            return mapping.findForward(FWD_EVENT);
        }
        else if (resource instanceof CalendarCollectionResource) {
            CalendarCollectionResource calendar =
                (CalendarCollectionResource) resource;
            request.setAttribute(ATTR_COLLECTION, resource);
            request.setAttribute(ATTR_CALENDAR, new CalendarBean(calendar));
            return mapping.findForward(FWD_CALENDAR);
        }

        throw new ServletException("resource of type " +
                                   resource.getClass().getName() + " at " +
                                   path + " cannot be viewed" );
    }

    /**
     * Downloads the contents of the file specified by the
     * {@link #PARAM_PATH} parameter.
     */
    public ActionForward download(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response)
        throws Exception {
        String path = request.getParameter(PARAM_PATH);

        Resource resource = getResource(path);

        if (resource instanceof FileResource) {
            spoolFile((FileResource) resource, request, response);
            return null;
        }
        else if (resource instanceof CalendarCollectionResource) {
            spoolCalendar((CalendarCollectionResource) resource, request,
                          response);
            return null;
        }
        throw new ServletException("resource of type " +
                                   resource.getClass().getName() +
                                   " at " + path + " cannot be downloaded");
    }

    /**
     * Grants the ticket specified by the input form to the resource
     * specified by the form.
     */
    public ActionForward grantTicket(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
        throws Exception {
        TicketForm ticketForm = (TicketForm) form;

        if (isCancelled(request)) {
            String path = ticketForm.getPath();
            ticketForm.reset(mapping, request);
            return copyForward(mapping.findForward(UIConstants.FWD_CANCEL),
                               path);
        }

        if (log.isDebugEnabled()) {
            log.debug("granting ticket to resource at " +
                      ticketForm.getPath());
        }

        Ticket ticket = ticketForm.getTicket();
        ticket.setOwner(getSecurityManager().getSecurityContext().getUser().
                        getUsername());

        homeDirectoryService.grantTicket(ticketForm.getPath(), ticket);

        return copyForward(mapping.findForward(UIConstants.FWD_SUCCESS),
                           ticketForm.getPath());
    }

    /**
     * Revokes the ticket specified by the {@link #PARAM_TICKET}
     * parameter from the resource specified by the
     * {@link #PARAM_PATH} parameter.
     */
    public ActionForward revokeTicket(ActionMapping mapping,
                                      ActionForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
        throws Exception {
        String path = request.getParameter(PARAM_PATH);
        String id = request.getParameter(PARAM_TICKET);

        if (log.isDebugEnabled()) {
            log.debug("revoking ticket " + id + " from resource at " + path);
        }

        homeDirectoryService.revokeTicket(path, id);

        return copyForward(mapping.findForward(UIConstants.FWD_SUCCESS),
                           path);
    }

    /**
     */
    public HomeDirectoryService getHomeDirectoryService() {
        return homeDirectoryService;
    }

    /**
     */
    public void setHomeDirectoryService(HomeDirectoryService service) {
        this.homeDirectoryService = service;
    }

    private Resource getResource(String path) {
        try {
            return homeDirectoryService.getResource(path);
        } catch (InvalidDataAccessResourceUsageException e) {
            // this happens when an item exists in the repository at
            // that path, but it does not represent a dav resource or
            // collection, so pretend that there's no resource there
            // at all
            throw new NoSuchResourceException(path);
        }
    }

    private void spoolFile(FileResource file,
                           HttpServletRequest request,
                           HttpServletResponse response)
        throws IOException {
        // set headers
        response.setStatus(HttpServletResponse.SC_OK);
        if (file.getContentType() != null) {
            response.setContentType(file.getContentType());
        }
        else {
            response.setContentType("application/octet-stream");
        }
        if (file.getContentLength() != null) {
            response.setContentLength(file.getContentLength().intValue());
        }
        if (file.getContentEncoding() != null) {
            response.setCharacterEncoding(file.getContentEncoding());
        }
        if (file.getContentLanguage() != null) {
            response.setHeader("Content-Language",
                               file.getContentLanguage());
        }

        // spool data
        InputStream in = file.getContent();
        OutputStream out = response.getOutputStream();
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
        response.flushBuffer();
    }

    private void spoolCalendar(CalendarCollectionResource collection,
                               HttpServletRequest request,
                               HttpServletResponse response)
        throws IOException, ParserException {
        // set headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/calendar");
        response.setCharacterEncoding("UTF-8");
        // XXX we can't know content length unless we write to a temp
        // file and then spool that

        // spool data
        CalendarOutputter outputter = new CalendarOutputter();
        // since the content was validated when the event resource was
        // imported, there's no need to do it here
        outputter.setValidating(false);
        try {
            outputter.output(collection.getCalendar(),
                             response.getOutputStream());
        } catch (ValidationException e) {
            log.error("invalid output calendar?!", e);
        }
        response.flushBuffer();
    }
}
