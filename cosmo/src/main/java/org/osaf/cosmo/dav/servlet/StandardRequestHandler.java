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
package org.osaf.cosmo.dav.servlet;

import java.io.IOException;
import java.text.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.util.EntityTag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceFactory;

import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavRequest;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.DavResponse;
import org.osaf.cosmo.dav.impl.StandardDavRequest;
import org.osaf.cosmo.dav.impl.StandardDavResponse;
import org.osaf.cosmo.dav.provider.DavProvider;
import org.osaf.cosmo.dav.provider.FileProvider;
import org.osaf.cosmo.http.IfMatch;
import org.osaf.cosmo.http.IfNoneMatch;

import org.springframework.web.HttpRequestHandler;

/**
 * <p>
 * An implementation of the Spring {@link HttpRequestHandler} that
 * services WebDAV requests. Finds the resource being acted upon, checks that
 * conditions are right for the request and resource, chooses a provider
 * based on the resource type, and then delegates to a specific provider
 * method based on the request method.
 * </p>
 */
public class StandardRequestHandler implements HttpRequestHandler {
    private static final Log log =
        LogFactory.getLog(StandardRequestHandler.class);

    private DavLocatorFactory locatorFactory;
    private DavResourceFactory resourceFactory;

    // RequestHandler methods
               
    /**
     * <p>
     * Processes the request and returns a response. Calls
     * {@link DavResourceFactory.createResource(DavResourceLocator, DavRequest, DavResponse)}
     * to find the targeted resource. Calls {@link #preconditions(DavRequest, DavResponse, DavResource)}
     * to verify preconditions. Calls {@link #process(DavRequest, DavResponse, DavResource)}
     * to execute the verified request.
     * </p>
     * <p>
     * Invalid preconditions and processing exceptions are handled by
     * sending a response with the appropriate error status and message and
     * an entity describing the error.
     * </p>
     */
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
        throws ServletException, IOException {
        DavRequest wreq = createDavRequest(request);
        DavResponse wres = createDavResponse(response);

        try {
            DavResource resource = (DavResource)
                resourceFactory.createResource(wreq.getRequestLocator(),
                                               wreq, wres);
            
            if (preconditions(wreq, wres, resource))
                process(wreq, wres, resource);
        } catch (Throwable e) {
            DavException de = e instanceof DavException ?
                (DavException) e : new DavException(e);
            wres.sendDavError(de);
        }
    }

    // our methods

    /**
     * <p>
     * Validates preconditions that must exist before the request may be
     * executed. If a precondition is specified but is not met, the
     * appropriate response is set and <code>false</code> is returned.
     * </p>
     * <p>
     * These preconditions are checked:
     * </p>
     * <ul>
     * <li>The <code>If-Match</code> request header</li>
     * <li>The <code>If-None-Match</code> request header</li>
     * <li>The <code>If-Modified-Since</code> request header</li>
     * <li>The <code>If-Unmodified-Since</code> request header</li>
     * </ul>
     */
    protected boolean preconditions(DavRequest request,
                                    DavResponse response,
                                    DavResource resource)
        throws IOException {
        if (! ifMatch(request, response, resource))
            return false;

        if (! ifNoneMatch(request, response, resource))
            return false;

        if (! ifModifiedSince(request, response, resource))
            return false;

        if (! ifUnmodifiedSince(request, response, resource))
            return false;

        return true;
    }

    /**
     * <p>
     * Hands the request off to a provider method for handling. The provider
     * is created by calling {@link #createProvider(DavResource)}. The
     * specific provider method is chosen by examining the request method.
     * </p>
     */
    protected void process(DavRequest request,
                           DavResponse response,
                           DavResource resource)
        throws IOException, DavException {
        DavProvider provider = createProvider(resource);

        if (request.getMethod().equals("OPTIONS"))
            options(request, response, resource);
        else if (request.getMethod().equals("GET"))
            provider.get(request, response, resource);
        else if (request.getMethod().equals("HEAD"))
            provider.head(request, response, resource);
        else if (request.getMethod().equals("PROPFIND"))
            provider.propfind(request, response, resource);
        else if (request.getMethod().equals("PROPPATCH"))
            provider.proppatch(request, response, resource);
        else if (request.getMethod().equals("PUT"))
            provider.put(request, response, resource);
        else if (request.getMethod().equals("DELETE"))
            provider.delete(request, response, resource);
        else if (request.getMethod().equals("COPY"))
            provider.copy(request, response, resource);
        else if (request.getMethod().equals("MOVE"))
            provider.move(request, response, resource);
        else if (request.getMethod().equals("MKCOL"))
            provider.mkcol(request, response, resource);
        else if (request.getMethod().equals("MKCALENDAR"))
            provider.mkcalendar(request, response, resource);
        else if (request.getMethod().equals("MKTICKET"))
            provider.mkticket(request, response, resource);
        else if (request.getMethod().equals("DELTICKET"))
            provider.delticket(request, response, resource);
        else
            response.setStatus(405);
    }

    /**
     * <p>
     * Creates an instance of @{link Provider}. The specific provider class
     * is chosen based on the type of resource:
     * </p>
     * <ul>
     * <li> home collection: {@link HomeCollectionProvider}</li>
     * <li> calendar collection: {@link CalendarCollectionProvider}</li>
     * <li> collection: {@link CollectionProvider}</li>
     * <li> calendar resource: {@link CalendarResourceProvider}</li>
     * <li> file resource: {@link FileProvider}</li>
     * </ul>
     */
    protected DavProvider createProvider(DavResource resource) {
        return new FileProvider();
    }

    /**
     * <p>
     * Creates an instance of <code>DavRequest</code> based on the
     * provided <code>HttpServletRequest</code>.
     * </p>
     */
    protected DavRequest createDavRequest(HttpServletRequest request) {
        // Create buffered request if method is PUT so we can retry
        // on concurrency exceptions
        if (request.getMethod().equals("PUT"))
            return new StandardDavRequest(request, locatorFactory, true);
        else
            return new StandardDavRequest(request, locatorFactory);   
    }

    /**
     * <p>
     * Creates an instance of <code>DavResponse</code> based on the
     * provided <code>HttpServletResponse</code>.
     * </p>
     */
    protected DavResponse createDavResponse(HttpServletResponse response) {
        return new StandardDavResponse(response);
    }

    public void init() {
        if (locatorFactory == null)
            throw new RuntimeException("locatorFactory must not be null");
        if (resourceFactory == null)
            throw new RuntimeException("resourceFactory must not be null");
    }

    public DavLocatorFactory getLocatorFactory() {
        return locatorFactory;
    }

    public void setLocatorFactory(DavLocatorFactory factory) {
        locatorFactory = factory;
    }

    public DavResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    public void setResourceFactory(DavResourceFactory factory) {
        resourceFactory = factory;
    }

    private boolean ifMatch(DavRequest request,
                            DavResponse response,
                            DavResource resource)
        throws IOException {
        EntityTag etag = etag(resource);
        try {
            if (IfMatch.allowMethod(request.getHeader("If-Match"), etag))
                return true;
        } catch (ParseException e) {
            response.sendError(400, e.getMessage());
            return false;
        }

        response.sendError(412, "If-Match disallows conditional request");
        if (etag != null)
            response.addHeader("ETag", etag.toString());

        return false;
    }

    private boolean ifNoneMatch(DavRequest request,
                                DavResponse response,
                                DavResource resource)
        throws IOException {
        EntityTag etag = etag(resource);
        try {
            if (IfNoneMatch.allowMethod(request.getHeader("If-None-Match"),
                                        etag))
                return true;
        } catch (ParseException e) {
            response.sendError(400, e.getMessage());
            return false;
        }

        if (deservesNotModified(request))
            response.sendError(304, "Not Modified");
        else
            response.sendError(412, "If-None-Match disallows conditional request");

        if (etag != null)
            response.addHeader("ETag", etag.toString());

        return false;
    }

    private boolean ifModifiedSince(DavRequest request,
                                    DavResponse response,
                                    DavResource resource)
        throws IOException {
        if (resource == null)
            return true;

        long mod = resource.getModificationTime();
        if (mod == -1)
            return true;
        mod = mod / 1000 * 1000;

        long since = request.getDateHeader("If-Modified-Since");
        if (since == -1)
            return true;

        if (mod > since)
            return true;

        response.sendError(304, "Not Modified");
        return false;
    }

    private boolean ifUnmodifiedSince(DavRequest request,
                                      DavResponse response,
                                      DavResource resource)
        throws IOException {
        if (resource == null)
            return true;

        long mod = resource.getModificationTime();
        if (mod == -1)
            return true;
        mod = mod / 1000 * 1000;

        long since = request.getDateHeader("If-Modified-Since");
        if (since == -1)
            return true;

        if (mod <= since)
            return true;

        response.sendError(412, "If-Unmodified-Since disallows conditional request");
        return false;
    }

    private EntityTag etag(DavResource resource) {
        if (resource == null)
            return null;
        String etag = resource.getETag();
        if (etag == null)
            return null;
        // resource etags have doublequotes wrapped around them
        if (etag.startsWith("\""))
            etag = etag.substring(1, etag.length()-1);
        return new EntityTag(etag);
    }

    private boolean deservesNotModified(DavRequest request) {
        return (request.getMethod().equals("GET") ||
                request.getMethod().equals("HEAD"));
    }

    private void options(DavRequest request,
                         DavResponse response,
                         DavResource resource) {
        response.setStatus(200);
        response.addHeader("Allow", resource.getSupportedMethods());
        response.addHeader("DAV", resource.getComplianceClass());
    }
}
