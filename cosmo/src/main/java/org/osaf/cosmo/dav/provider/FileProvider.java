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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.io.OutputContextImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.dav.DavRequest;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.DavResponse;
import org.osaf.cosmo.dav.impl.DavFile;

/**
 * <p>
 * An implementation of <code>DavProvider</code> that implements
 * access to <code>DavFile</code> resources.
 * </p>
 *
 * @see DavProvider
 * @see DavFile
 */
public class FileProvider implements DavProvider {
    private static final Log log = LogFactory.getLog(FileProvider.class);

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
    }
    
    public void proppatch(DavRequest request,
                          DavResponse response,
                          DavResource resource)
        throws DavException, IOException {
    }

    public void put(DavRequest request,
                    DavResponse response,
                    DavResource resource)
        throws DavException, IOException {
    }

    public void delete(DavRequest request,
                       DavResponse response,
                       DavResource resource)
        throws DavException, IOException {
    }

    public void copy(DavRequest request,
                     DavResponse response,
                     DavResource resource)
        throws DavException, IOException {
    }

    public void move(DavRequest request,
                     DavResponse response,
                     DavResource resource)
        throws DavException, IOException {
    }

    public void mkcol(DavRequest request,
                      DavResponse response,
                      DavResource resource)
        throws DavException, IOException {
    }

    public void mkcalendar(DavRequest request,
                           DavResponse response,
                           DavResource resource)
        throws DavException, IOException {
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
        if (resource == null) {
            response.sendError(404);
            return;
        }

        if (log.isDebugEnabled())
            log.debug("spooling resource " + resource.getResourcePath());

        OutputStream out = withEntity ? response.getOutputStream() : null;
        resource.spool(createOutputContext(response, out));
        response.flushBuffer();
    }

    protected OutputContext createOutputContext(DavResponse response,
                                                OutputStream out) {
        return new OutputContextImpl(response, out);
    }
}
