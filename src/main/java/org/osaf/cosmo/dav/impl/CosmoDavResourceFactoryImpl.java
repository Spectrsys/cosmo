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
import org.apache.jackrabbit.webdav.simple.ResourceFilter;

import org.osaf.cosmo.dav.CosmoDavResource;
import org.osaf.cosmo.dav.CosmoDavResourceFactory;
import org.osaf.cosmo.security.CosmoSecurityManager;

/**
 * An implementation of 
 * {@link org.apache.jackrabbit.webdav.DavResourceFactory} that
 * provides instances of {@link CosmoDavResource}.
 */
public class CosmoDavResourceFactoryImpl implements CosmoDavResourceFactory {

    private LockManager lockManager;
    private ResourceFilter resourceFilter;
    private CosmoSecurityManager securityManager;

    // DavResourceFactory methods

    /**
     */
    public DavResource createResource(DavResourceLocator locator,
                                      DavServletRequest request,
                                      DavServletResponse response)
        throws DavException {
        return createResource(locator, request.getDavSession());
    }

    /**
     */
    public DavResource createResource(DavResourceLocator locator,
                                      DavSession session)
        throws DavException {
        try {
            CosmoDavResourceImpl resource =
                new CosmoDavResourceImpl(locator, this, session,
                                         resourceFilter);
            resource.addLockManager(lockManager);
            return resource;
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    // CosmoDavResourceFactory methods

    /**
     */
    public LockManager getLockManager() {
        return lockManager;
    }

    /**
     */
    public ResourceFilter getResourceFilter() {
        return resourceFilter;
    }

    /**
     */
    public CosmoSecurityManager getSecurityManager() {
        return securityManager;
    }

    // our methods

    /**
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    /**
     */
    public void setResourceFilter(ResourceFilter resourceFilter) {
        this.resourceFilter = resourceFilter;
    }

    /**
     */
    public void setSecurityManager(CosmoSecurityManager securityManager) {
        this.securityManager = securityManager;
    }
}
