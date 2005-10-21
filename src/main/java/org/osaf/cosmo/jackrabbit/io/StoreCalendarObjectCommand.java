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
package org.osaf.cosmo.jackrabbit.io;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

import org.apache.jackrabbit.server.io.AbstractCommand;
import org.apache.jackrabbit.server.io.AbstractContext;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.webdav.DavException;

import org.apache.log4j.Logger;

import org.osaf.cosmo.dao.jcr.JcrCalendarMapper;
import org.osaf.cosmo.dao.jcr.JcrConstants;
import org.osaf.cosmo.dao.UnsupportedCalendarObjectException;
import org.osaf.cosmo.dav.CosmoDavResponse;
import org.osaf.cosmo.icalendar.ICalendarConstants;
import org.osaf.cosmo.icalendar.RecurrenceException;

import org.springframework.dao.DataAccessException;

/**
 * An import command for storing the calendar object attached to a
 * dav resource.
 */
public class StoreCalendarObjectCommand extends AbstractCommand
    implements JcrConstants, ICalendarConstants {
    private static final Logger log =
        Logger.getLogger(StoreCalendarObjectCommand.class);

    /**
     */
    public boolean execute(AbstractContext context)
        throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        }
        else {
            return false;
        }
    }

    /**
     */
    public boolean execute(ImportContext context)
        throws Exception {
        Node resourceNode = context.getNode();
        if (resourceNode == null) {
            return false;
        }

        // if the node's parent is not a calendar collection, don't
        // bother storing, since we'll never query "webcal"
        // calendars.
        if (! resourceNode.getParent().isNodeType(NT_CALDAV_COLLECTION)) {
            return false;
        }

        // ensure that the resource is a dav resource and that either
        // it is of type text/calendar or its name ends with .ics
        if (! (resourceNode.isNodeType(NT_DAV_RESOURCE) &&
               (context.getContentType().startsWith(CONTENT_TYPE) ||
                resourceNode.getName().endsWith("." + FILE_EXTENSION)))) {
            return false;
        }

        // get a handle to the resource content
        Node content = resourceNode.getNode(NN_JCR_CONTENT);
        InputStream in = content.getProperty(NP_JCR_DATA).getStream();

        try {
            // parse the resource
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(in);

            // store the resource in the repository
            JcrCalendarMapper.calendarToNode(calendar, resourceNode);
        } catch (ParserException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error parsing calendar resource", e);
            }
            throw new DavException(CosmoDavResponse.SC_FORBIDDEN);
        } catch (UnsupportedCalendarObjectException e) {
            if (log.isDebugEnabled()) {
                log.debug("Calendar object contains no supported components",
                          e);
            }
            throw new DavException(CosmoDavResponse.SC_CONFLICT);
        } catch (RecurrenceException e) {
            if (log.isDebugEnabled()) {
                log.debug("Calendar object contains bad recurrence", e);
            }
            throw new DavException(CosmoDavResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error storing calendar object", e);
            }
            if (e instanceof DataAccessException &&
                e.getCause() instanceof RepositoryException) {
                throw (RepositoryException) e.getCause();
            }
            throw e;
        }

        return false;
    }
}
