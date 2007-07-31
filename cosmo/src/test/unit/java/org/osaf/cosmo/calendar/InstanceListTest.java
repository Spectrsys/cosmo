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
package org.osaf.cosmo.calendar;

import java.io.FileInputStream;
import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;

/**
 * Test expand InstanceList
 */
public class InstanceListTest extends TestCase {
    protected String baseDir = "src/test/unit/resources/testdata/";
    private static final TimeZoneRegistry TIMEZONE_REGISTRY =
                TimeZoneRegistryFactory.getInstance().createRegistry();
    
    public void testNonUTCInstanceList() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "floating_recurr_event.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        DateTime start = new DateTime("20060101T190000Z");
        DateTime end = new DateTime("20060108T190000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(5, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060102T140000", key);
        Assert.assertEquals("20060102T140000", instance.getStart().toString());
        Assert.assertEquals("20060102T150000", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060103T140000", key);
        Assert.assertEquals("20060103T140000", instance.getStart().toString());
        Assert.assertEquals("20060103T150000", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060104T140000", key);
        Assert.assertEquals("20060104T160000", instance.getStart().toString());
        Assert.assertEquals("20060104T170000", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060105T140000", key);
        Assert.assertEquals("20060105T160000", instance.getStart().toString());
        Assert.assertEquals("20060105T170000", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060106T140000", key);
        Assert.assertEquals("20060106T140000", instance.getStart().toString());
        Assert.assertEquals("20060106T150000", instance.getEnd().toString());
    }
    
    public void testUTCInstanceList() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "floating_recurr_event.ics");
        Calendar calendar = cb.build(fis);
        
        VTimeZone vtz = (VTimeZone) calendar.getComponents().getComponent("VTIMEZONE");
        TimeZone tz = new TimeZone(vtz);
        
        InstanceList instances = new InstanceList();
        instances.setUTC(true);
        instances.setTimezone(tz);
        
        DateTime start = new DateTime("20060101T190000Z");
        DateTime end = new DateTime("20060108T190000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(5, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060102T190000Z", key);
        Assert.assertEquals("20060102T190000Z", instance.getStart().toString());
        Assert.assertEquals("20060102T200000Z", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060103T190000Z", key);
        Assert.assertEquals("20060103T190000Z", instance.getStart().toString());
        Assert.assertEquals("20060103T200000Z", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060104T190000Z", key);
        Assert.assertEquals("20060104T210000Z", instance.getStart().toString());
        Assert.assertEquals("20060104T220000Z", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060105T190000Z", key);
        Assert.assertEquals("20060105T210000Z", instance.getStart().toString());
        Assert.assertEquals("20060105T220000Z", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060106T190000Z", key);
        Assert.assertEquals("20060106T190000Z", instance.getStart().toString());
        Assert.assertEquals("20060106T200000Z", instance.getEnd().toString());
    }
    
    public void testInstanceListInstanceBeforeStartRange() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "eventwithtimezone3.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        DateTime start = new DateTime("20070509T090000Z");
        DateTime end = new DateTime("20070511T090000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(3, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070509T081500Z", key);
        Assert.assertEquals("20070509T031500", instance.getStart().toString());
        Assert.assertEquals("20070509T041500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070510T081500Z", key);
        Assert.assertEquals("20070510T031500", instance.getStart().toString());
        Assert.assertEquals("20070510T041500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070511T081500Z", key);
        Assert.assertEquals("20070511T031500", instance.getStart().toString());
        Assert.assertEquals("20070511T041500", instance.getEnd().toString());
    }
    
    public void testFloatingWithSwitchingTimezoneInstanceList() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "floating_recurr_event.ics");
        Calendar calendar = cb.build(fis);
        
        TimeZone tz = TIMEZONE_REGISTRY.getTimeZone("America/Los_Angeles");
        InstanceList instances = new InstanceList();
        instances.setTimezone(tz);
        
        DateTime start = new DateTime("20060102T220000Z");
        DateTime end = new DateTime("20060108T190000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(5, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060102T220000Z", key);
        Assert.assertEquals("20060102T140000", instance.getStart().toString());
        Assert.assertEquals("20060102T150000", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060103T220000Z", key);
        Assert.assertEquals("20060103T140000", instance.getStart().toString());
        Assert.assertEquals("20060103T150000", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060104T220000Z", key);
        Assert.assertEquals("20060104T160000", instance.getStart().toString());
        Assert.assertEquals("20060104T170000", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060105T220000Z", key);
        Assert.assertEquals("20060105T160000", instance.getStart().toString());
        Assert.assertEquals("20060105T170000", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20060106T220000Z", key);
        Assert.assertEquals("20060106T140000", instance.getStart().toString());
        Assert.assertEquals("20060106T150000", instance.getEnd().toString());
    }
    
    public void testExdateWithTimezone() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "recurring_with_exdates.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        DateTime start = new DateTime("20070509T090000Z");
        DateTime end = new DateTime("20070609T090000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(2, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070529T101500Z", key);
        Assert.assertEquals("20070529T051500", instance.getStart().toString());
        Assert.assertEquals("20070529T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070605T101500Z", key);
        Assert.assertEquals("20070605T051500", instance.getStart().toString());
        Assert.assertEquals("20070605T061500", instance.getEnd().toString());
    }
    
    public void testExdateUtc() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "recurring_with_exdates_utc.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        DateTime start = new DateTime("20070509T090000Z");
        DateTime end = new DateTime("20070609T090000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(2, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070529T101500Z", key);
        Assert.assertEquals("20070529T051500", instance.getStart().toString());
        Assert.assertEquals("20070529T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070605T101500Z", key);
        Assert.assertEquals("20070605T051500", instance.getStart().toString());
        Assert.assertEquals("20070605T061500", instance.getEnd().toString());
    }
    
    public void testExdateNoTimezone() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "recurring_with_exdates_floating.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        DateTime start = new DateTime("20070509T090000Z");
        DateTime end = new DateTime("20070609T090000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(2, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070529T051500", key);
        Assert.assertEquals("20070529T051500", instance.getStart().toString());
        Assert.assertEquals("20070529T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070605T051500", key);
        Assert.assertEquals("20070605T051500", instance.getStart().toString());
        Assert.assertEquals("20070605T061500", instance.getEnd().toString());
    }
    
    public void testRdateWithTimezone() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "recurring_with_rdates.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        DateTime start = new DateTime("20070509T090000Z");
        DateTime end = new DateTime("20070609T090000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(7, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070515T101500Z", key);
        Assert.assertEquals("20070515T051500", instance.getStart().toString());
        Assert.assertEquals("20070515T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070516T101500Z", key);
        Assert.assertEquals("20070516T051500", instance.getStart().toString());
        Assert.assertEquals("20070516T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070517T101500Z", key);
        Assert.assertEquals("20070517T101500Z", instance.getStart().toString());
        Assert.assertEquals("20070517T131500Z", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070522T101500Z", key);
        Assert.assertEquals("20070522T051500", instance.getStart().toString());
        Assert.assertEquals("20070522T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070523T101500Z", key);
        Assert.assertEquals("20070523T051500", instance.getStart().toString());
        Assert.assertEquals("20070523T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070529T101500Z", key);
        Assert.assertEquals("20070529T051500", instance.getStart().toString());
        Assert.assertEquals("20070529T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070605T101500Z", key);
        Assert.assertEquals("20070605T051500", instance.getStart().toString());
        Assert.assertEquals("20070605T061500", instance.getEnd().toString());
    }
    
    public void testExruleWithTimezone() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "recurring_with_exrule.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        DateTime start = new DateTime("20070509T090000Z");
        DateTime end = new DateTime("20070609T090000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(2, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070515T101500Z", key);
        Assert.assertEquals("20070515T051500", instance.getStart().toString());
        Assert.assertEquals("20070515T061500", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070529T101500Z", key);
        Assert.assertEquals("20070529T051500", instance.getStart().toString());
        Assert.assertEquals("20070529T061500", instance.getEnd().toString());
    }
    
    public void testAllDayRecurring() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "allday_recurring.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        DateTime start = new DateTime("20070101T090000Z");
        DateTime end = new DateTime("20070103T090000Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(3, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070101", key);
        Assert.assertEquals("20070101", instance.getStart().toString());
        Assert.assertEquals("20070102", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070102", key);
        Assert.assertEquals("20070102", instance.getStart().toString());
        Assert.assertEquals("20070103", instance.getEnd().toString());
        
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070103", key);
        Assert.assertEquals("20070103", instance.getStart().toString());
        Assert.assertEquals("20070104", instance.getEnd().toString());
    }
    
    public void testInstanceStartBeforeRange() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "recurring_with_exdates.ics");
        Calendar calendar = cb.build(fis);
        
        InstanceList instances = new InstanceList();
        
        // make sure startRange is after the startDate of an occurrence,
        // in this case the occurrence is at 20070529T101500Z
        DateTime start = new DateTime("20070529T110000Z");
        DateTime end = new DateTime("20070530T051500Z");
        
        ComponentList comps = calendar.getComponents();
        Iterator<VEvent> it = comps.getComponents("VEVENT").iterator();
        boolean addedMaster = false;
        while(it.hasNext()) {
            VEvent event = it.next();
            if(event.getRecurrenceId()==null) {
                addedMaster = true;
                instances.addComponent(event, start, end);
            }
            else {
                Assert.assertTrue(addedMaster);
                instances.addOverride(event, start, end);
            }
        }
        
        Assert.assertEquals(1, instances.size() );
        
        Iterator<String> keys = instances.keySet().iterator();
        
        String key = null;
        Instance instance = null;
            
        key = keys.next();
        instance = (Instance) instances.get(key);
        
        Assert.assertEquals("20070529T101500Z", key);
        Assert.assertEquals("20070529T051500", instance.getStart().toString());
        Assert.assertEquals("20070529T061500", instance.getEnd().toString());
    }
    
}
