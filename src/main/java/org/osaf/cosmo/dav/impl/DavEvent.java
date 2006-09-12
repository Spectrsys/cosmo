/*
 * Copyright 2006 Open Source Applications Foundation
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

import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;

import org.apache.log4j.Logger;

import org.osaf.cosmo.model.CalendarEventItem;

/**
 * Extends <code>DavFile</code> to adapt the Cosmo
 * <code>CalendarEventItem</code> to the DAV resource model.
 *
 * This class does not define any live properties.
 *
 * @see DavFile
 * @see CalendarEventItem
 */
public class DavEvent extends DavFile {
    private static final Logger log = Logger.getLogger(DavEvent.class);

    /** */
    public DavEvent(CalendarEventItem event,
                    DavResourceLocator locator,
                    DavResourceFactory factory,
                    DavSession session) {
        super(event, locator, factory, session);
    }

    /** */
    public DavEvent(DavResourceLocator locator,
                    DavResourceFactory factory,
                    DavSession session) {
        this(new CalendarEventItem(), locator, factory, session);
    }
}
