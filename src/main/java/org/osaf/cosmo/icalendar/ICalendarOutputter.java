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
package org.osaf.cosmo.icalendar;

import java.io.OutputStream;
import java.io.IOException;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.model.CalendarCollectionStamp;
import org.osaf.cosmo.model.CollectionItem;

/**
 * A class that writes Cosmo calendar model objects to output streams
 * formatted according to the iCalendar specification (RFC 2445).
 */
public class ICalendarOutputter {
    private static final Log log = LogFactory.getLog(ICalendarOutputter.class);

    /**
     * Writes an iCalendar string representing the calendar items
     * contained within the given calendar collection to the given
     * output stream.
     *
     * Since the calendar content stored with each calendar items
     * is parsed and validated when the item is created, these
     * errors should not reoccur when the calendar is being
     * outputted.
     *
     * @param collection the <code>CollectionItem</code> to format
     *
     * @throws IllegalArgumentException if the collection is not
     * stamped as a calendar collection
     * @throws IOException
     */
    public static void output(CollectionItem collection,
                              OutputStream out)
        throws IOException {
        CalendarCollectionStamp stamp =
            CalendarCollectionStamp.getStamp(collection);
        if (stamp == null)
            throw new IllegalArgumentException("non-calendar collection cannot be formatted as iCalendar");

        if (log.isDebugEnabled())
            log.debug("outputting " + collection.getUid() + " as iCalendar");

        CalendarOutputter outputter = new CalendarOutputter();
        outputter.setValidating(false);
        try {
            outputter.output(stamp.getCalendar(), out);
        } catch (ParserException e) {
            throw new IllegalStateException("unable to compose collection calendar from child items' calendars", e);
        } catch (ValidationException e) {
            throw new IllegalStateException("unable to validate collection calendar", e);
        }
    }
}
