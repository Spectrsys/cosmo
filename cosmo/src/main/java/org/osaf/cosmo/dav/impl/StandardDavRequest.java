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
import java.util.HashSet;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;

import org.osaf.cosmo.dav.BadRequestException;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavRequest;
import org.osaf.cosmo.dav.caldav.CaldavConstants;
import org.osaf.cosmo.dav.caldav.InvalidCalendarDataException;
import org.osaf.cosmo.dav.caldav.property.CalendarDescription;
import org.osaf.cosmo.dav.caldav.property.CalendarTimezone;
import org.osaf.cosmo.dav.caldav.property.SupportedCalendarComponentSet;
import org.osaf.cosmo.dav.ticket.TicketConstants;
import org.osaf.cosmo.model.Ticket;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 */
public class StandardDavRequest extends WebdavRequestImpl
    implements DavRequest, DavConstants, CaldavConstants, TicketConstants {
    private static final Log log =
        LogFactory.getLog(StandardDavRequest.class);

    private DavPropertySet mkcalendarSet;
    private Ticket ticket;
    private ReportInfo reportInfo;
    private boolean bufferRequestContent = false;
    private long bufferedContentLength = -1;

    /**
     */
    public StandardDavRequest(HttpServletRequest request,
                              DavLocatorFactory factory) {
        super(request, factory);
    }
    
    /**
     */
    public StandardDavRequest(HttpServletRequest request,
                              DavLocatorFactory factory,
                              boolean bufferRequestContent) {
        super(request, factory);
        this.bufferRequestContent = bufferRequestContent;
    }

    // CosmoDavRequest methods

    /**
     * Return the base URL for this request (including scheme, server
     * name, and port if not the scheme's default port).
     */
    public String getBaseUrl() {
        StringBuffer buf = new StringBuffer();
        buf.append(getScheme());
        buf.append("://");
        buf.append(getServerName());
        if ((isSecure() && getServerPort() != 443) ||
            getServerPort() != 80) {
            buf.append(":");
            buf.append(getServerPort());
        }
        if (! getContextPath().equals("/")) {
            buf.append(getContextPath());
        }
        return buf.toString();
    }

    // CaldavRequest methods

    /**
     * Return the list of 'set' entries in the MKCALENDAR request
     * body. The list is empty if the request body did not
     * contain any 'set' elements.
     * An <code>IllegalArgumentException</code> is raised if the request body can not
     * be parsed
     */
    public DavPropertySet getMkCalendarSetProperties()
        throws DavException {
        if (mkcalendarSet == null)
            mkcalendarSet = parseMkCalendarRequest();
        return mkcalendarSet;
    }

    // TicketDavRequest methods

    /**
     * Return a {@link Ticket} representing the information about a
     * ticket to be created by a <code>MKTICKET</code> request.
     *
     * @throws IllegalArgumentException if there is no ticket
     * information in the request or if the ticket information exists
     * but is invalid
     */
    public Ticket getTicketInfo() {
        if (ticket == null) {
            ticket = parseTicketRequest();
        }
        return ticket;
    }

    /**
     * Return the ticket id included in this request, if any. If
     * different ticket ids are included in the headers and URL, the
     * one from the URL is used.
     */
    public String getTicketId() {
        String ticketId = getParameter(PARAM_TICKET);
        if (ticketId == null) {
            ticketId = getHeader(HEADER_TICKET);
        }
        return ticketId;
    }

    /**
     * Return the report information, if any, included in the
     * request.
     *
     * @throws DavException if there is no report information in the
     * request or if the report information is invalid
     */
    public ReportInfo getReportInfo()
        throws DavException {
        if (reportInfo == null) {
            reportInfo = parseReportRequest();
        }
        return reportInfo;
    }
  
    // private methods

    private DavPropertySet parseMkCalendarRequest()
        throws DavException {
        DavPropertySet propertySet = new DavPropertySet();

        Document requestDocument = getRequestDocument();
        if (requestDocument == null)
            return propertySet;

        Element root = requestDocument.getDocumentElement();
        if (! DomUtil.matches(root, "mkcalendar", NAMESPACE_CALDAV))
            throw new BadRequestException("Expected CALDAV:mkcalendar root element");
        Element set = DomUtil.getChildElement(root, "set", NAMESPACE);
        if (set == null)
            throw new BadRequestException("Expected DAV:set child of CALDAV:mkcalendar");
        Element prop =DomUtil.getChildElement(set, "prop", NAMESPACE);
        if (prop == null)
            throw new BadRequestException("Expected DAV:prop child of DAV:set");
        ElementIterator i = DomUtil.getChildren(prop);
        while (i.hasNext()) {
            Element e = i.nextElement();
            if (DomUtil.matches(e, "calendar-timezone", NAMESPACE_CALDAV))
                parseCalendarTimezone(propertySet, e);
            else if (DomUtil.matches(e, "calendar-description",
                                     NAMESPACE_CALDAV))
                parseDescription(propertySet, e);
            else if (DomUtil.getNamespace(e).equals(NAMESPACE_CALDAV))
                throw new BadRequestException("CALDAV:" + e.getTagName() + " is a protected or unknown property");
            else if (DomUtil.matches(e, "displayname", NAMESPACE))
                propertySet.add(DefaultDavProperty.createFromXml(e));
            else if (DomUtil.getNamespace(e).equals(NAMESPACE))
                throw new BadRequestException("DAV:" + e.getTagName() + " is a protected or unknown property");
            else
                propertySet.add(DefaultDavProperty.createFromXml(e));
        }

        return propertySet;
    }

    private void parseDescription(DavPropertySet propertySet,
                                  Element e)
        throws DavException {
        DefaultDavProperty d = DefaultDavProperty.createFromXml(e);

        String value = (String) d.getValue();
        String lang = DomUtil.getAttribute(e, "lang", NAMESPACE_XML);

        propertySet.add(new CalendarDescription(value, lang));
    }

    private void parseCalendarTimezone(DavPropertySet propertySet,
                                       Element e)
        throws DavException {
        String ical = DomUtil.getTextTrim(e);
        if (StringUtils.isBlank(ical))
            throw new InvalidCalendarDataException("Expected calendar object in CALDAV:calendar-timezone value");

        propertySet.add(new CalendarTimezone(ical));
    }

    private Ticket parseTicketRequest() {
        Document requestDocument = getRequestDocument();
        if (requestDocument == null) {
            throw new IllegalArgumentException("ticket request missing body");
        }

        Element root = requestDocument.getDocumentElement();
        if (! DomUtil.matches(root, ELEMENT_TICKET_TICKETINFO,
                              NAMESPACE_TICKET)) {
            throw new IllegalArgumentException("ticket request has missing ticket:ticketinfo");
        }

        if (DomUtil.hasChildElement(root, ELEMENT_TICKET_ID,
                                    NAMESPACE_TICKET)) {
            throw new IllegalArgumentException("ticket request must not include ticket:id");
        }
        if (DomUtil.hasChildElement(root, XML_OWNER, NAMESPACE)) {
            throw new IllegalArgumentException("ticket request must not include ticket:owner");
        }

        String timeout =
            DomUtil.getChildTextTrim(root, ELEMENT_TICKET_TIMEOUT,
                                     NAMESPACE_TICKET);
        if (timeout != null &&
            ! timeout.equals(TIMEOUT_INFINITE)) {
            try {
                int seconds = Integer.parseInt(timeout.substring(7));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("malformed ticket:timeout value " + timeout);
            }
        } else {
            timeout = TIMEOUT_INFINITE;
        }

        // visit limits are not supported

        Element privilege =
            DomUtil.getChildElement(root, XML_PRIVILEGE, NAMESPACE);
        if (privilege == null) {
            throw new IllegalArgumentException("ticket request missing DAV:privileges");
        }
        Element read =
            DomUtil.getChildElement(privilege, XML_READ, NAMESPACE);
        Element write =
            DomUtil.getChildElement(privilege, XML_WRITE, NAMESPACE);
        Element freebusy =
            DomUtil.getChildElement(privilege, ELEMENT_TICKET_FREEBUSY,
                                    NAMESPACE);
        if (read == null && write == null && freebusy == null) {
            throw new IllegalArgumentException("ticket request contains empty or invalid DAV:privileges");
        }

        Ticket ticket = new Ticket();
        ticket.setTimeout(timeout);
        if (read != null) {
            ticket.getPrivileges().add(Ticket.PRIVILEGE_READ);
        }
        if (write != null) {
            ticket.getPrivileges().add(Ticket.PRIVILEGE_WRITE);
        }
        if (freebusy != null) {
            ticket.getPrivileges().add(Ticket.PRIVILEGE_FREEBUSY);
        }

        return ticket;
    }

    private ReportInfo parseReportRequest()
        throws DavException {
        Document requestDocument = getRequestDocument();
        if (requestDocument == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST,
                                   "Report content must not be empty");
        }
        try {
            return new ReportInfo(requestDocument.getDocumentElement(),
                                  getDepth(DEPTH_0));
        } catch (org.apache.jackrabbit.webdav.DavException e) {
            throw new DavException(e);
        }
    }

    @Override
    public ServletInputStream getInputStream()
        throws IOException {
        if (! bufferRequestContent)
            return super.getInputStream();
        
        BufferedServletInputStream is = 
            new BufferedServletInputStream(super.getInputStream());
        bufferedContentLength = is.getLength();

        long contentLength = getContentLength();
        if (contentLength != -1 && contentLength != bufferedContentLength)
            throw new IOException("Read only " + bufferedContentLength + " of " + contentLength + " bytes");
        
        return is;
    }
    
    public boolean isRequestContentBuffered() {
        return bufferRequestContent;
    }

    public long getBufferedContentLength() {
        return bufferedContentLength;
    }
}
