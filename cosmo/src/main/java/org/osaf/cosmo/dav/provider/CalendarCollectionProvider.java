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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavRequest;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.DavResponse;
import org.osaf.cosmo.dav.MethodNotAllowedException;
import org.osaf.cosmo.dav.impl.DavCalendarCollection;

/**
 * <p>
 * An implementation of <code>DavProvider</code> that implements
 * access to <code>DavCalendarCollection</code> resources.
 * </p>
 *
 * @see DavProvider
 * @see DavCalendarCollection
 */
public class CalendarCollectionProvider extends CollectionProvider {
    private static final Log log =
        LogFactory.getLog(CalendarCollectionProvider.class);

    public CalendarCollectionProvider(DavResourceFactory resourceFactory) {
        super(resourceFactory);
    }

    // DavProvider methods

    public void mkcalendar(DavRequest request,
                          DavResponse response,
                          DavResource resource)
        throws DavException, IOException {
        DavResource parent = (DavResource) resource.getCollection();
        if (parent.isCalendarCollection())
            throw new MethodNotAllowedException("MKCALENDAR not allowed within a calendar collection");
        super.mkcalendar(request, response, resource);
    }
}
