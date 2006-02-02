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
package org.osaf.cosmo.dav;

import java.io.InputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.j2ee.SimpleWebdavServlet;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.WebdavResponseImpl;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.simple.LocatorFactoryImpl;

import org.apache.log4j.Logger;

import org.osaf.cosmo.dao.TicketDao;
import org.osaf.cosmo.dav.CosmoDavResource;
import org.osaf.cosmo.dav.impl.CosmoDavLocatorFactoryImpl;
import org.osaf.cosmo.dav.impl.CosmoDavRequestImpl;
import org.osaf.cosmo.dav.impl.CosmoDavResourceImpl;
import org.osaf.cosmo.dav.impl.CosmoDavResourceFactoryImpl;
import org.osaf.cosmo.dav.impl.CosmoDavResponseImpl;
import org.osaf.cosmo.dav.impl.CosmoDavSessionProviderImpl;
import org.osaf.cosmo.dav.report.Report;
import org.osaf.cosmo.dav.report.ReportInfo;
import org.osaf.cosmo.io.CosmoInputContext;
import org.osaf.cosmo.io.InvalidCalendarObjectException;
import org.osaf.cosmo.io.InvalidDataException;
import org.osaf.cosmo.io.UidConflictException;
import org.osaf.cosmo.io.UnsupportedCalendarComponentException;
import org.osaf.cosmo.io.UnsupportedMediaTypeException;
import org.osaf.cosmo.model.ModelConversionException;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.security.CosmoSecurityManager;

import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import org.springmodules.jcr.JcrSessionFactory;

/**
 * An extension of Jackrabbit's 
 * {@link org.apache.jackrabbit.server.simple.WebdavServlet} which
 * integrates the Spring Framework for configuring support objects.
 */
public class CosmoDavServlet extends SimpleWebdavServlet {
    private static final Logger log =
        Logger.getLogger(CosmoDavServlet.class);

    /**
     * The name of the Spring bean identifying the
     * {@link org.osaf.commons.spring.jcr.JCRSessionFactory} that
     * produces JCR sessions for this servlet.
     */
    public static final String BEAN_DAV_SESSION_FACTORY =
        "homedirSessionFactory";
    /**
     * The name of the Spring bean identifying the Cosmo security
     * manager
     */
    public static final String BEAN_SECURITY_MANAGER = "securityManager";
    /**
     * The name of the Spring bean identifying the Cosmo ticket DAO.
     */
    public static final String BEAN_TICKET_DAO = "ticketDao";

    private CosmoSecurityManager securityManager;
    private WebApplicationContext wac;

    /**
     * Load the servlet context's
     * {@link org.springframework.web.context.WebApplicationContext}
     * and look up support objects.
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();

        wac = WebApplicationContextUtils.
            getWebApplicationContext(getServletContext());

        if (wac != null) {
            if (securityManager == null) {
                securityManager = (CosmoSecurityManager)
                    getBean(BEAN_SECURITY_MANAGER, CosmoSecurityManager.class);
            }

            JcrSessionFactory sessionFactory = (JcrSessionFactory)
                getBean(BEAN_DAV_SESSION_FACTORY, JcrSessionFactory.class);
            TicketDao ticketDao = (TicketDao)
                getBean(BEAN_TICKET_DAO, TicketDao.class);

            if (getDavSessionProvider() == null) {
                CosmoDavSessionProviderImpl sessionProvider =
                    new CosmoDavSessionProviderImpl();
                sessionProvider.setSessionFactory(sessionFactory);
                setDavSessionProvider(sessionProvider);
            }

            if (getResourceFactory() == null) {
                CosmoDavResourceFactoryImpl resourceFactory =
                    new CosmoDavResourceFactoryImpl(getLockManager(),
                                                    getResourceConfig());
                resourceFactory.setSecurityManager(securityManager);
                resourceFactory.setTicketDao(ticketDao);
                setResourceFactory(resourceFactory);
            }

            if (getLocatorFactory() == null) {
                CosmoDavLocatorFactoryImpl locatorFactory =
                    new CosmoDavLocatorFactoryImpl(getPathPrefix());
                setLocatorFactory(locatorFactory);
            }
        }
    }


    /**
     * Dispatch dav methods that jcr-server does not know about.
     *
     * @throws ServletException
     * @throws IOException
     * @throws DavException
     */
    protected boolean execute(WebdavRequest request,
                              WebdavResponse response,
                              int method,
                              DavResource resource)
            throws ServletException, IOException, DavException {
        CosmoDavRequest cosmoRequest = (CosmoDavRequest) request;
        CosmoDavResponse cosmoResponse = (CosmoDavResponse) response;
        CosmoDavResourceImpl cosmoResource = (CosmoDavResourceImpl) resource;

        // TODO We handle REPORT ourselves for now. Eventually this will be
        // punted up into jackrabbit
        if ((method > 0) && (method != DavMethods.DAV_REPORT)) {
            return super.execute(request, response, method, resource);
        } else if (method != DavMethods.DAV_REPORT) {
            method = CosmoDavMethods.getMethodCode(request.getMethod());
        }

        switch (method) {
        // TODO We handle REPORT ourselves for now. Eventually this will be
        // punted up into jackrabbit
        case DavMethods.DAV_REPORT:
            doReport(cosmoRequest, cosmoResponse, cosmoResource);
            break;
        case CosmoDavMethods.DAV_MKCALENDAR:
            doMkCalendar(cosmoRequest, cosmoResponse, cosmoResource);
            break;
        case CosmoDavMethods.DAV_MKTICKET:
            doMkTicket(cosmoRequest, cosmoResponse, cosmoResource);
            break;
        case CosmoDavMethods.DAV_DELTICKET:
            doDelTicket(cosmoRequest, cosmoResponse, cosmoResource);
            break;
        default:
            // any other method
            return false;
        }

        return true;
    }

    /**
     */
    protected void doPut(WebdavRequest request,
                         WebdavResponse response,
                         DavResource resource)
        throws IOException, DavException {
        try {
            super.doPut(request, response, resource);
        } catch (UnsupportedMediaTypeException e) {
            // {CALDAV:supported-calendar-data}
            response.sendError(DavServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                               e.getMediaType());
            return;
        } catch (InvalidDataException e) {
            // {CALDAV:valid-calendar-data
            response.sendError(DavServletResponse.SC_BAD_REQUEST,
                               e.getMessage());
            return;
        } catch (InvalidCalendarObjectException e) {
            // {CALDAV:valid-calendar-object-resource
            response.sendError(DavServletResponse.SC_PRECONDITION_FAILED,
                               "Resource does not obey all calendar data " +
                               "restrictions: " + e.getMessage());
            return;
        } catch (UnsupportedCalendarComponentException e) {
            // {CALDAV:supported-calendar-component
            response.sendError(DavServletResponse.SC_PRECONDITION_FAILED,
                               "Resource does not contain at least one " +
                               "supported calendar component: " +
                               e.getMessage());
            return;
        } catch (UidConflictException e) {
            // {CALDAV:no-uid-conflict
            response.sendError(DavServletResponse.SC_CONFLICT,
                               "Uid conflict: " + e.getUid());
            return;
        }

        // caldav (section 4.6.2): return ETag header
        // since we can't force a resource to reload its properties,
        // we have to get a new copy of the resource which will
        // contain the etag
        DavResource newResource = getResourceFactory().
            createResource(request.getRequestLocator(), request, response);
        response.setHeader("ETag", newResource.getETag());
    }

    // TODO We handle REPORT ourselves for now. Eventually this will be
    // punted up into jackrabbit
    /**
     * The REPORT method
     * 
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doReport(CosmoDavRequest request,
                            CosmoDavResponse response,
                            CosmoDavResource resource)
        throws DavException, IOException {

        ReportInfo info = ((CosmoDavRequestImpl) request).getCosmoReportInfo();
        Report report = ((CosmoDavResourceImpl) resource).getReport(info);
        response.sendXmlResponse(report.toXml(),
                                 DavServletResponse.SC_MULTI_STATUS);
    }

    /**
     * Executes the MKCALENDARmethod
     *
     * @throws IOException
     * @throws DavException
     */
    protected void doMkCalendar(CosmoDavRequest request,
                                CosmoDavResponse response,
                                CosmoDavResource resource)
        throws IOException, DavException {
        // {DAV:resource-must-be-null}
        if (resource.exists()) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED,
                               "Resource exists");
            return;
        }

        // {DAV:calendar-collection-location-ok}
        CosmoDavResource parentResource =
            (CosmoDavResource) resource.getCollection();
        if (parentResource == null ||
            ! parentResource.exists()) {
            response.sendError(DavServletResponse.SC_CONFLICT,
                               "One or more intermediate collections must be created");
            return;
        }

        // {DAV:calendar-collection-location-ok}
        if (! parentResource.isCollection() ||
            parentResource.isCalendarCollection()) {
            response.sendError(DavServletResponse.SC_FORBIDDEN,
                               "Parent resource must be a regular collection");
            return;
        }

        // also could return INSUFFICIENT_STORAGE if we do not have
        // enough space for the collection, but how do we determine
        // that?

        CosmoInputContext ctx = (CosmoInputContext)
            getInputContext(request, null);
        try {
            DavPropertySet properties = request.getMkCalendarSetProperties();
            ctx.setCalendarCollectionProperties(properties);
        } catch (IllegalArgumentException e) {
            response.sendError(DavServletResponse.SC_BAD_REQUEST,
                               e.getMessage());
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("adding calendar collection at " +
                      resource.getResourcePath());
        }
        parentResource.addMember(resource, ctx);

        response.setStatus(DavServletResponse.SC_CREATED);
        return;
    }

    /**
     * Executes the MKTICKET method
     *
     * @throws IOException
     * @throws DavException
     */
    protected void doMkTicket(CosmoDavRequest request,
                              CosmoDavResponse response,
                              CosmoDavResource resource)
        throws IOException, DavException {
        if (!resource.exists()) {
            response.sendError(DavServletResponse.SC_NOT_FOUND);
            return;
        }

        Ticket ticket = null;
        try {
            ticket = request.getTicketInfo();
        } catch (IllegalArgumentException e) {
            response.sendError(DavServletResponse.SC_BAD_REQUEST,
                               e.getMessage());
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("saving ticket for resource " +
                      resource.getResourcePath());
        }
        resource.saveTicket(ticket);

        response.sendMkTicketResponse(resource, ticket.getId());
    }

    /**
     * Executes the DELTICKET method
     *
     * @throws IOException
     * @throws DavException
     */
    protected void doDelTicket(CosmoDavRequest request,
                               CosmoDavResponse response,
                               CosmoDavResource resource)
        throws IOException, DavException {
        if (!resource.exists()) {
            response.sendError(DavServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!resource.isTicketable()) {
            throw new DavException(CosmoDavResponse.SC_METHOD_NOT_ALLOWED);
        }

        String ticketId = request.getTicketId();
        if (ticketId == null) {
            response.sendError(DavServletResponse.SC_BAD_REQUEST,
                               "No ticket was specified.");
            return;
        }
        Ticket ticket = resource.getTicket(ticketId);
        if (ticket == null) {
            response.sendError(DavServletResponse.SC_PRECONDITION_FAILED,
                               "The ticket specified does not exist.");
            return;
        }

        // must either be an admin or the user that created the ticket
        String loggedInUsername =
            securityManager.getSecurityContext().getUser().getUsername();
        if (! (ticket.getOwner().equals(loggedInUsername) ||
               securityManager.getSecurityContext().isAdmin())) {
            response.sendError(DavServletResponse.SC_FORBIDDEN);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("removing ticket " + ticket.getId() + " for resource " +
                      resource.getResourcePath());
        }
        resource.removeTicket(ticket);

        response.sendDelTicketResponse(resource, ticket.getId());
    }

    /**
     */
    protected WebdavRequest createWebdavRequest(HttpServletRequest request) {
        return new CosmoDavRequestImpl(request, getLocatorFactory());
    }

    /**
     */
    protected WebdavResponse createWebdavResponse(HttpServletResponse response) {
        return new CosmoDavResponseImpl(response);
    }

    /**
     */
    public InputContext getInputContext(DavServletRequest request,
                                        InputStream in) {
        return new CosmoInputContext(request, in);
    }

    // our methods

    /**
     * Looks up the bean with given name and class in the web
     * application context.
     *
     * @param name the bean's name
     * @param clazz the bean's class
     */
    protected Object getBean(String name, Class clazz)
        throws ServletException {
        try {
            return wac.getBean(name, clazz);
        } catch (BeansException e) {
            throw new ServletException("Error retrieving bean " + name +
                                       " of type " + clazz +
                                       " from web application context", e);
        }
    }
}
