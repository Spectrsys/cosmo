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
package org.osaf.cosmo.dav.impl;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.apache.jackrabbit.webdav.simple.ResourceFactoryImpl;

import org.osaf.cosmo.dav.CosmoDavMethods;
import org.osaf.cosmo.dav.CosmoDavResource;
import org.osaf.cosmo.dav.CosmoDavResourceFactory;
import org.osaf.cosmo.security.CosmoSecurityManager;

/**
 * Extends 
 * {@link org.apache.jackrabbit.webdav.simple.ResourceFactoryImpl} to
 * provides instances of {@link CosmoDavResource}.
 */
public class CosmoDavResourceFactoryImpl extends ResourceFactoryImpl
    implements CosmoDavResourceFactory {

    private CosmoSecurityManager securityManager;

    /**
     */
    public CosmoDavResourceFactoryImpl(LockManager lockMgr) {
        super(lockMgr);
    }

    /**
     */
    public CosmoDavResourceFactoryImpl(LockManager lockMgr,
                                       ResourceConfig resourceConfig) {
        super(lockMgr, resourceConfig);
    }

    // DavResourceFactoryImpl methods

    /**
     */
    public DavResource createResource(DavResourceLocator locator,
                                      DavSession session)
        throws DavException {
        try {
            CosmoDavResourceImpl resource =
                new CosmoDavResourceImpl(locator, this, session,
                                         getResourceConfig());
            resource.addLockManager(getLockManager());
            return resource;
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Augments superclass method to also return <code>true</code> for
     * <code>MKCALENDAR</code> requests.
     */
    protected boolean isCreateRequest(DavServletRequest request) {
        if (CosmoDavMethods.getMethodCode(request.getMethod()) ==
            CosmoDavMethods.DAV_MKCALENDAR) {
            return true;
        }
        return super.isCreateRequest(request);
    }

    /**
     * Augments superclass method to also return <code>true</code> for
     * <code>MKCALENDAR</code> requests.
     */
    protected boolean isCreateCollectionRequest(DavServletRequest request) {
        if (CosmoDavMethods.getMethodCode(request.getMethod()) ==
            CosmoDavMethods.DAV_MKCALENDAR) {
            return true;
        }
        return super.isCreateCollectionRequest(request);
    }

    // CosmoDavResourceFactory methods

    /**
     */
    public CosmoSecurityManager getSecurityManager() {
        return securityManager;
    }

    // our methods

    /**
     */
    public void setSecurityManager(CosmoSecurityManager securityManager) {
        this.securityManager = securityManager;
    }
}
