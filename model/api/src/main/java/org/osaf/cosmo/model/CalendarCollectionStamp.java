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
package org.osaf.cosmo.model;

import java.util.Set;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;

import org.osaf.cosmo.model.validator.Timezone;

/**
 * Stamp that can be added to CollectionItem that stores
 * CalendarCollection specific attributes.
 */
public interface CalendarCollectionStamp extends Stamp {

    String getDescription();

    void setDescription(String description);

    String getLanguage();

    void setLanguage(String language);

    /**
     * @return calendar object representing timezone
     */
    @Timezone
    Calendar getTimezoneCalendar();

    /**
     * @return timezone if present
     */
    TimeZone getTimezone();

    /**
     * @return name of timezone if one is set
     */
    String getTimezoneName();

    /**
     * Set timezone definition for calendar.
     * 
     * @param timezone
     *            timezone definition in ical format
     */
    void setTimezoneCalendar(Calendar timezone);
    
    /**
     * Return a set of all EventStamps for the collection's children.
     * @return set of EventStamps contained in children
     */
    Set<EventStamp> getEventStamps();

}
