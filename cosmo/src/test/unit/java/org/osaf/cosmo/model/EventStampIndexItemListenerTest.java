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

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;

import org.osaf.cosmo.eim.schema.EimValueConverter;

/**
 * Test EventStampHandler
 */
public class EventStampIndexItemListenerTest extends TestCase {
   
    EventStampIndexItemListener handler = new EventStampIndexItemListener();
    TimeZoneRegistry registry =
        TimeZoneRegistryFactory.getInstance().createRegistry();
    
    public void testEventStampHandler() throws Exception {
        
        NoteItem master = new NoteItem();
        EventStamp eventStamp = new EventStamp(master);
        eventStamp.createCalendar();
        eventStamp.setStartDate(new DateTime("20070212T074500"));
        eventStamp.setEndDate(new DateTime("20070212T094500"));
        master.addStamp(eventStamp);
        
        handler.onCreateItem(master);
        
        Assert.assertEquals("20070212T074500", eventStamp.getTimeRangeIndex().getStartDate());
        Assert.assertEquals("20070212T094500", eventStamp.getTimeRangeIndex().getEndDate());
        Assert.assertTrue(eventStamp.getTimeRangeIndex().getIsFloating().booleanValue());
        
        TimeZone ctz = registry.getTimeZone("America/Chicago");
        DateTime start = new DateTime("20070212T074500", ctz);
        eventStamp.setStartDate(start);
        
        DateTime end = new DateTime("20070212T094500", ctz);
        eventStamp.setEndDate(end);
        
        String recur1 = "FREQ=DAILY;";
        
        List recurs = EimValueConverter.toICalRecurs(recur1);
        eventStamp.setRecurrenceRules(recurs);
        
        handler.onUpdateItem(master);
        Assert.assertEquals("20070212T134500Z", eventStamp.getTimeRangeIndex().getStartDate());
        Assert.assertEquals(EventStamp.TIME_INFINITY, eventStamp.getTimeRangeIndex().getEndDate());
        Assert.assertFalse(eventStamp.getTimeRangeIndex().getIsFloating().booleanValue());
    }
    
   
    
    
}
