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
package org.osaf.cosmo.dav.provider;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.InputContextImpl;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.io.OutputContextImpl;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.dav.BadRequestException;
import org.osaf.cosmo.dav.ContentLengthRequiredException;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavRequest;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.DavResponse;
import org.osaf.cosmo.dav.ForbiddenException;
import org.osaf.cosmo.dav.NotFoundException;
import org.osaf.cosmo.dav.PreconditionFailedException;
import org.osaf.cosmo.dav.caldav.report.FreeBusyReport;
import org.osaf.cosmo.model.Ticket;

/**
 * <p>
 * A base class for implementations of <code>DavProvider</code>.
 * </p>
 *
 * @see DavProvider
 */
public abstract class BaseProvider implements DavProvider, DavConstants {
    private static final Log log = LogFactory.getLog(BaseProvider.class);

    private DavResourceFactory resourceFactory;

    public BaseProvider(DavResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    // DavProvider methods

    public void get(DavRequest request,
                    DavResponse response,
                    DavResource resource)
        throws DavException, IOException {
        spool(request, response, resource, true);
    }

    public void head(DavRequest request,
                     DavResponse response,
                     DavResource resource)
        throws DavException, IOException {
        spool(request, response, resource, false);
    }

    public void propfind(DavRequest request,
                         DavResponse response,
                         DavResource resource)
        throws DavException, IOException {
        if (! resource.exists())
            throw new NotFoundException();

        try {
            int depth = request.getDepth(DEPTH_INFINITY);
            DavPropertyNameSet props = request.getPropFindProperties();
            int type = request.getPropFindType();

            MultiStatus ms = new MultiStatus();
            ms.addResourceProperties(resource, props, type, depth);

            response.sendMultiStatus(ms);
        } catch (org.apache.jackrabbit.webdav.DavException e) {
            throw new DavException(e);
        }
    }

    public void proppatch(DavRequest request,
                          DavResponse response,
                          DavResource resource)
        throws DavException, IOException {
        if (! resource.exists())
            throw new NotFoundException();

        try {
            DavPropertySet set = request.getPropPatchSetProperties();
            DavPropertyNameSet remove = request.getPropPatchRemoveProperties();
            if (set.isEmpty() && remove.isEmpty())
                throw new BadRequestException("No properties specified");

            MultiStatus ms = new MultiStatus();
            MultiStatusResponse msr = resource.alterProperties(set, remove);
            ms.addResponse(msr);

            response.sendMultiStatus(ms);
        } catch (org.apache.jackrabbit.webdav.DavException e) {
            throw new DavException(e);
        }
    }

    public void delete(DavRequest request,
                       DavResponse response,
                       DavResource resource)
        throws DavException, IOException {
        if (! resource.exists())
            throw new NotFoundException();

        int depth = request.getDepth(DEPTH_INFINITY);
        if (depth != DEPTH_INFINITY)
            throw new BadRequestException("Depth for DELETE must be Infinity");

        try {
            DavResource parent = (DavResource) resource.getCollection();
            parent.removeMember(resource);
            response.setStatus(204);
        } catch (org.apache.jackrabbit.webdav.DavException e) {
            throw new DavException(e);
        }
    }

    public void copy(DavRequest request,
                     DavResponse response,
                     DavResource resource)
        throws DavException, IOException {
        if (! resource.exists())
            throw new NotFoundException();

        int depth = request.getDepth(DEPTH_INFINITY);
        if (! (depth == DEPTH_0 || depth == DEPTH_INFINITY))
            throw new BadRequestException("Depth for COPY must be 0 or Infinity");

        DavResource destination =
            resourceFactory.resolve(request.getDestinationLocator(), request);
        validateDestination(request, destination);

        try {
            if (destination.exists() && request.isOverwrite())
                destination.getCollection().removeMember(destination);
            resource.copy(destination, depth == DEPTH_0);
            response.setStatus(destination.exists() ? 204 : 201);
        } catch (org.apache.jackrabbit.webdav.DavException e) {
            throw new DavException(e);
        }
    }

    public void move(DavRequest request,
                     DavResponse response,
                     DavResource resource)
        throws DavException, IOException {
        if (! resource.exists())
            throw new NotFoundException();

        DavResource destination =
            resourceFactory.resolve(request.getDestinationLocator(), request);
        validateDestination(request, destination);

        try {
            if (destination.exists() && request.isOverwrite())
                destination.getCollection().removeMember(destination);
            resource.move(destination);
            response.setStatus(destination.exists() ? 204 : 201);
        } catch (org.apache.jackrabbit.webdav.DavException e) {
            throw new DavException(e);
        }
    }

    public void report(DavRequest request,
                       DavResponse response,
                       DavResource resource)
        throws DavException, IOException {
        if (! resource.exists())
            throw new NotFoundException();

        try {
            ReportInfo info = request.getReportInfo();

            // Since the report type could not be determined in the security
            // filter in order to check ticket permissions on REPORT, the
            // check must be done manually here.
            checkReportAccess(info);

            resource.getReport(info).run(response);
        } catch (org.apache.jackrabbit.webdav.DavException e) {
            throw new DavException(e);
        }
    }

    public void mkticket(DavRequest request,
                         DavResponse response,
                         DavResource resource)
        throws DavException, IOException {
    }

    public void delticket(DavRequest request,
                          DavResponse response,
                          DavResource resource)
        throws DavException, IOException {
    }

    // our methods

    protected void spool(DavRequest request,
                         DavResponse response,
                         DavResource resource,
                         boolean withEntity)
        throws DavException, IOException {
        if (! resource.exists())
            throw new NotFoundException();

        if (log.isDebugEnabled())
            log.debug("spooling resource " + resource.getResourcePath());

        resource.spool(createOutputContext(response, withEntity));
        response.flushBuffer();
    }

    protected InputContext createInputContext(DavRequest request)
        throws DavException, IOException {
        String xfer = request.getHeader("Transfer-Encoding");
        boolean chunked = xfer != null && xfer.equals("chunked");
        if (xfer != null && ! chunked)
            throw new BadRequestException("Unknown Transfer-Encoding " + xfer);
        if (chunked && request.getContentLength() <= 0)
            throw new ContentLengthRequiredException();

        InputStream in = (request.getContentLength() > 0 || chunked) ?
            request.getInputStream() : null;
        return new InputContextImpl(request, in);
    }

    protected OutputContext createOutputContext(DavResponse response,
                                                boolean withEntity)
        throws IOException {
        OutputStream out = withEntity ? response.getOutputStream() : null;
        return new OutputContextImpl(response, out);
    }

    protected void validateDestination(DavRequest request,
                                       DavResource destination)
        throws DavException {
        String uri = request.getHeader(HEADER_DESTINATION);
        if (StringUtils.isBlank(uri))
            throw new BadRequestException("Destination header not provided");
        if (destination.getLocator().equals(request.getRequestLocator()))
            throw new ForbiddenException("Destination URI is the same as the original resource URI");
        if (destination.exists() && ! request.isOverwrite())
            throw new PreconditionFailedException("Overwrite header was not specified for existing destination");
    }

    protected void checkReportAccess(ReportInfo info)
        throws DavException {
        Ticket ticket = getResourceFactory().getSecurityManager().
            getSecurityContext().getTicket();
        if (ticket == null)
            return;

        if (isFreeBusyReport(info)) {
            if (ticket.getPrivileges().contains(Ticket.PRIVILEGE_FREEBUSY))
                return;
            if (ticket.getPrivileges().contains(Ticket.PRIVILEGE_READ))
                return;
            // Do not allow the client to know that this resource actually
            // exists, as per CalDAV report definition
            throw new NotFoundException();
        }

        if (ticket.getPrivileges().contains(Ticket.PRIVILEGE_READ))
            return;

       throw new ForbiddenException("Ticket privileges deny access");
    }

    public DavResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    private boolean isFreeBusyReport(ReportInfo info) {
        return FreeBusyReport.REPORT_TYPE_CALDAV_FREEBUSY.
            isRequestedReportType(info);
    }
}
