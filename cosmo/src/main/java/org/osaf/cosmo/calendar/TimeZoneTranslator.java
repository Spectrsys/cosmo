/*
 * Copyright 2005-2007 Open Source Applications Foundation
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
package org.osaf.cosmo.calendar;

import java.io.IOException;
import java.util.Properties;

import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;

/**
 * Provides methods for translating an arbitrary timezone into
 * an equivalent Olson timezone.  Translation relies on a set of
 * known timezone aliases.
 */
public class TimeZoneTranslator {
    
    // mapping of known timezone aliases to Olson tzids
    private Properties ALIASES = new Properties();

    private TimeZoneRegistry REGISTRY = TimeZoneRegistryFactory.getInstance()
            .createRegistry();
    
    private static TimeZoneTranslator instance = new TimeZoneTranslator();
    
    private TimeZoneTranslator() {
        try {
            ALIASES.load(TimeZoneTranslator.class
                    .getResourceAsStream("/timezone.alias"));
        } catch (IOException e) {
            throw new RuntimeException("Error parsing tz aliases");
        }
    }
    
    public static TimeZoneTranslator getInstance() {
        return instance;
    }
    
    /**
     * Given a timezone and date, return the equivalent Olson timezone.
     * @param timezone timezone
     * @return equivalent Olson timezone
     */
    public TimeZone translateToOlsonTz(TimeZone timezone) {
        
        // First use registry to find Olson tz
        TimeZone translatedTz = REGISTRY.getTimeZone(timezone.getID());
        if(translatedTz!=null)
            return translatedTz;
        
        // Next check for known aliases
        String aliasedTzId = ALIASES.getProperty(timezone.getID());
        
        // If an aliased id was found, return the Olson tz from the registry
        if(aliasedTzId!=null)
            return REGISTRY.getTimeZone(aliasedTzId);
        
        return null;
    }
    
    /**
     * Given a timezone id, return the equivalent Olson timezone.
     * @param tzId timezone id to translate
     * @return equivalent Olson timezone
     */
    public TimeZone translateToOlsonTz(String tzId) {
        
        // First use registry to find Olson tz
        TimeZone translatedTz = REGISTRY.getTimeZone(tzId);
        if(translatedTz!=null)
            return translatedTz;
        
        // Next check for known aliases
        String aliasedTzId = ALIASES.getProperty(tzId);
        
        // If an aliased id was found, return the Olson tz from the registry
        if(aliasedTzId!=null)
            return REGISTRY.getTimeZone(aliasedTzId);
        
        return null;
    }
}
