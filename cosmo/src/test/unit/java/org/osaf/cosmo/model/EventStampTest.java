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
package org.osaf.cosmo.model;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;

import org.osaf.cosmo.calendar.ICalDate;
import org.osaf.cosmo.eim.schema.EimValueConverter;

/**
 * Test EventStamp
 */
public class EventStampTest extends TestCase {
   
    public void testEventStampGetCalendar() throws Exception {
        TimeZoneRegistry registry =
            TimeZoneRegistryFactory.getInstance().createRegistry();
        NoteItem master = new NoteItem();
        EventStamp eventStamp = new EventStamp(master);
        eventStamp.createCalendar();
        Date date = new ICalDate(";VALUE=DATE-TIME:20070212T074500").getDate();
        eventStamp.setStartDate(date);
        
        Calendar cal = eventStamp.getCalendar();
        
        // date has no timezone, so there should be no timezones
        Assert.assertEquals(0, cal.getComponents(Component.VTIMEZONE).size());
        
        date = new ICalDate(";VALUE=DATE-TIME;TZID=America/Chicago:20070212T074500").getDate();
        eventStamp.setStartDate(date);
        
        cal = eventStamp.getCalendar();
        
        // date has timezone, so there should be a timezone
        Assert.assertEquals(1, cal.getComponents(Component.VTIMEZONE).size());
        
        date = new ICalDate(";VALUE=DATE-TIME;TZID=America/Los_Angeles:20070212T074500").getDate();
        eventStamp.setEndDate(date);
        
        cal = eventStamp.getCalendar();
        
        // dates have 2 different timezones, so there should be 2 timezones
        Assert.assertEquals(2, cal.getComponents(Component.VTIMEZONE).size());
        
        // add timezones to master event calendar
        eventStamp.getMasterCalendar().getComponents().add(registry.getTimeZone("America/Chicago").getVTimeZone());
        eventStamp.getMasterCalendar().getComponents().add(registry.getTimeZone("America/Los_Angeles").getVTimeZone());
        
        cal = eventStamp.getCalendar();
        Assert.assertEquals(2, cal.getComponents(Component.VTIMEZONE).size());
    }
    
    public void testInheritedAlarm() throws Exception {
        NoteItem master = new NoteItem();
        EventStamp eventStamp = new EventStamp(master);
        eventStamp.createCalendar();
        eventStamp.creatDisplayAlarm();
        Date date = new ICalDate(";VALUE=DATE-TIME:20070212T074500").getDate();
        eventStamp.setStartDate(date);
        eventStamp.setDisplayAlarmDescription("alarm");
        eventStamp.setDisplayAlarmTrigger(EimValueConverter.toIcalTrigger("-PT15M"));
        
        NoteItem mod = new NoteItem();
        mod.setModifies(master);
        master.getModifications().add(mod);
        EventExceptionStamp exceptionStamp = new EventExceptionStamp(mod);
        mod.addStamp(exceptionStamp);
        exceptionStamp.createCalendar();
        exceptionStamp.creatDisplayAlarm();
        exceptionStamp.setStartDate(date);
        exceptionStamp.setRecurrenceId(date);
        
        // first test inherited alarm
        Calendar cal = eventStamp.getCalendar();
        ComponentList comps = cal.getComponents(Component.VEVENT);
        Assert.assertEquals(2, comps.size());
        VEvent masterEvent = (VEvent) comps.get(0);
        VEvent modEvent = (VEvent) comps.get(1);
        
        VAlarm masterAlarm = (VAlarm) masterEvent.getAlarms().get(0);
        VAlarm modAlarm = (VAlarm) modEvent.getAlarms().get(0);
        
        Assert.assertEquals(masterAlarm, modAlarm);
        
        // next test not inherited
        exceptionStamp.setDisplayAlarmDescription("alarm2");
        exceptionStamp.setDisplayAlarmTrigger(EimValueConverter.toIcalTrigger("-PT30M"));
        cal = eventStamp.getCalendar();
        comps = cal.getComponents(Component.VEVENT);
        Assert.assertEquals(2, comps.size());
        masterEvent = (VEvent) comps.get(0);
        modEvent = (VEvent) comps.get(1);
        masterAlarm = (VAlarm) masterEvent.getAlarms().get(0);
        modAlarm = (VAlarm) modEvent.getAlarms().get(0);
        
        Assert.assertFalse(masterAlarm.equals(modAlarm));
        
        // finally test no alarm
        exceptionStamp.removeDisplayAlarm();
        
        cal = eventStamp.getCalendar();
        comps = cal.getComponents(Component.VEVENT);
        Assert.assertEquals(2, comps.size());
        masterEvent = (VEvent) comps.get(0);
        modEvent = (VEvent) comps.get(1);
        
        Assert.assertEquals(1, masterEvent.getAlarms().size());
        Assert.assertEquals(0, modEvent.getAlarms().size());
    }
    
    
}
