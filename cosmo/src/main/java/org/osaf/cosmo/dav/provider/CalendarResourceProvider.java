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

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.dav.ConflictException;
import org.osaf.cosmo.dav.DavContent;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavRequest;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.DavResourceLocator;
import org.osaf.cosmo.dav.DavResponse;
import org.osaf.cosmo.dav.caldav.SupportedCalendarComponentException;
import org.osaf.cosmo.dav.impl.DavCalendarResource;
import org.osaf.cosmo.dav.impl.DavEvent;
import org.osaf.cosmo.dav.impl.DavJournal;
import org.osaf.cosmo.dav.impl.DavTask;
import org.osaf.cosmo.dav.io.DavInputContext;

/**
 * <p>
 * An implementation of <code>DavProvider</code> that implements
 * access to <code>DavCalendarResource</code> resources.
 * </p>
 *
 * @see DavProvider
 * @see DavFile
 */
public class CalendarResourceProvider extends FileProvider {
    private static final Log log =
        LogFactory.getLog(CalendarResourceProvider.class);

    public CalendarResourceProvider(DavResourceFactory resourceFactory) {
        super(resourceFactory);
    }
    
    // DavProvider methods

    public void put(DavRequest request,
                    DavResponse response,
                    DavContent content)
        throws DavException, IOException {
        if (! content.getParent().exists())
            throw new ConflictException("One or more intermediate collections must be created");

        int status = content.exists() ? 204 : 201;
        DavInputContext ctx = (DavInputContext) createInputContext(request);
        if (! content.exists())
            content = createCalendarResource(request, response,
                                             content.getResourceLocator(),
                                             ctx.getCalendar());
        content.getParent().addContent(content, ctx);
        response.setStatus(status);
        response.setHeader("ETag", content.getETag());
    }

    protected DavContent createCalendarResource(DavRequest request,
                                                DavResponse response,
                                                DavResourceLocator locator,
                                                Calendar calendar)
        throws DavException {
        if (! calendar.getComponents(Component.VEVENT).isEmpty())
            return new DavEvent(locator, getResourceFactory());
        if (! calendar.getComponents(Component.VTODO).isEmpty())
            return new DavTask(locator, getResourceFactory());
        if (! calendar.getComponents(Component.VJOURNAL).isEmpty())
            return new DavJournal(locator, getResourceFactory());
        throw new SupportedCalendarComponentException();
  }
}
