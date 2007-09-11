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
package org.osaf.cosmo.calendar.query;

import java.io.FileInputStream;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;

/**
 * Test CalendarFilterEvaluater
 */
public class CalendarFilterEvaluaterTest extends TestCase {
    protected String baseDir = "src/test/unit/resources/testdata/";
    
    public void testEvaluateFilterPropFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "cal1.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VEVENT");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        PropertyFilter propFilter = new PropertyFilter("SUMMARY");
        TextMatchFilter textFilter = new TextMatchFilter("Visible");
        propFilter.setTextMatchFilter(textFilter);
        eventFilter.getPropFilters().add(propFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        textFilter.setValue("ViSiBle");
        textFilter.setCollation(textFilter.COLLATION_OCTET);
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
        
        textFilter.setCollation(null);
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        textFilter.setValue("XXX");
        textFilter.setNegateCondition(true);
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        propFilter.setTextMatchFilter(null);
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        propFilter.setName("RRULE");
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
        
        propFilter.setIsNotDefinedFilter(new IsNotDefinedFilter());
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateFilterParamFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "cal1.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VEVENT");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        PropertyFilter propFilter = new PropertyFilter("DTSTART");
        ParamFilter paramFilter = new ParamFilter("VALUE");
        TextMatchFilter textFilter = new TextMatchFilter("DATE-TIME");
        paramFilter.setTextMatchFilter(textFilter);
        propFilter.getParamFilters().add(paramFilter);
        eventFilter.getPropFilters().add(propFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        textFilter.setValue("XXX");
        Assert.assertFalse(evaluater.evaluate(calendar, filter));

        textFilter.setNegateCondition(true);
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        paramFilter.setTextMatchFilter(null);
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        paramFilter.setName("BOGUS");
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
        
        paramFilter.setIsNotDefinedFilter(new IsNotDefinedFilter());
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateFilterEventTimeRangeFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "cal1.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VEVENT");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        
        DateTime start = new DateTime("20050816T115000Z");
        DateTime end = new DateTime("20050916T115000Z");
    
        Period period = new Period(start, end);
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(period);
       
        eventFilter.setTimeRangeFilter(timeRangeFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        start = new DateTime("20050818T115000Z");
        period = new Period(start, end);
        timeRangeFilter.setPeriod(period);
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateFilterRecurringEventTimeRangeFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "eventwithtimezone1.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VEVENT");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        
        DateTime start = new DateTime("20070514T115000Z");
        DateTime end = new DateTime("20070516T115000Z");
    
        Period period = new Period(start, end);
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(period);
       
        eventFilter.setTimeRangeFilter(timeRangeFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        start = new DateTime("20070515T205000Z");
        period = new Period(start, end);
        timeRangeFilter.setPeriod(period);
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
    }
       
    public void testEvaluateFilterPropertyTimeRangeFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "cal1.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VEVENT");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        PropertyFilter propFilter = new PropertyFilter("DTSTAMP");
        
        DateTime start = new DateTime("20060517T115000Z");
        DateTime end = new DateTime("20060717T115000Z");
    
        Period period = new Period(start, end);
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(period);
       
        propFilter.setTimeRangeFilter(timeRangeFilter);
        eventFilter.getPropFilters().add(propFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        start = new DateTime("20060717T115000Z");
        period = new Period(start, end);
        timeRangeFilter.setPeriod(period);
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateComplicated() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "cal1.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VEVENT");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        
        DateTime start = new DateTime("20050816T115000Z");
        DateTime end = new DateTime("20050916T115000Z");
    
        Period period = new Period(start, end);
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(period);
       
        eventFilter.setTimeRangeFilter(timeRangeFilter);
        
        PropertyFilter propFilter = new PropertyFilter("SUMMARY");
        TextMatchFilter textFilter = new TextMatchFilter("Visible");
        propFilter.setTextMatchFilter(textFilter);
        eventFilter.getPropFilters().add(propFilter);
        
        PropertyFilter propFilter2 = new PropertyFilter("DTSTART");
        ParamFilter paramFilter2 = new ParamFilter("VALUE");
        TextMatchFilter textFilter2 = new TextMatchFilter("DATE-TIME");
        paramFilter2.setTextMatchFilter(textFilter2);
        propFilter2.getParamFilters().add(paramFilter2);
        
        DateTime start2 = new DateTime("20060517T115000Z");
        DateTime end2 = new DateTime("20060717T115000Z");
    
        Period period2 = new Period(start, end);
        TimeRangeFilter timeRangeFilter2 = new TimeRangeFilter(period2);
        propFilter2.setTimeRangeFilter(timeRangeFilter2);
        
        eventFilter.getPropFilters().add(propFilter2);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        // change one thing
        paramFilter2.setName("XXX");
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateVAlarmFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "event_with_alarm.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        filter.setFilter(compFilter);
        
        ComponentFilter eventFilter = new ComponentFilter("VEVENT");
        ComponentFilter alarmFilter = new ComponentFilter("VALARM");
        PropertyFilter propFilter = new PropertyFilter("ACTION");
        TextMatchFilter textMatch = new TextMatchFilter("AUDIO");
        propFilter.setTextMatchFilter(textMatch);
        
        compFilter.getComponentFilters().add(eventFilter);
        eventFilter.getComponentFilters().add(alarmFilter);
        alarmFilter.getPropFilters().add(propFilter);
       
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        textMatch.setValue("EMAIL");
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
        
        alarmFilter.getPropFilters().clear();
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        // time-range filter on VALARM
        
        // find alarm relative to start
        DateTime start = new DateTime("20060101T220000Z");
        DateTime end = new DateTime("20060101T230000Z");
    
        Period period = new Period(start, end);
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(period);
        alarmFilter.setTimeRangeFilter(timeRangeFilter);
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        // find alarm relative to end
        start = new DateTime("20060101T050000Z");
        end = new DateTime("20060101T190000Z");
        period = new Period(start, end);
        timeRangeFilter.setPeriod(period);
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        // find absolute repeating alarm
        start = new DateTime("20051230T050000Z");
        end = new DateTime("20051230T080000Z");
        period = new Period(start, end);
        timeRangeFilter.setPeriod(period);
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        // find no alarms
        start = new DateTime("20060101T020000Z");
        end = new DateTime("20060101T030000Z");
        period = new Period(start, end);
        timeRangeFilter.setPeriod(period);
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
        
        alarmFilter.setTimeRangeFilter(null);
        alarmFilter.setIsNotDefinedFilter(new IsNotDefinedFilter());
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateFilterPropFilterAgainstException() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "event_with_exception.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VEVENT");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        PropertyFilter propFilter = new PropertyFilter("DESCRIPTION");
        eventFilter.getPropFilters().add(propFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateVJournalFilterPropFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "vjournal.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VJOURNAL");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        PropertyFilter propFilter = new PropertyFilter("SUMMARY");
        TextMatchFilter textFilter = new TextMatchFilter("Staff");
        propFilter.setTextMatchFilter(textFilter);
        eventFilter.getPropFilters().add(propFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        textFilter.setValue("bogus");
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateVToDoFilterPropFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "vtodo.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter eventFilter = new ComponentFilter("VTODO");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(eventFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        PropertyFilter propFilter = new PropertyFilter("SUMMARY");
        TextMatchFilter textFilter = new TextMatchFilter("Income");
        propFilter.setTextMatchFilter(textFilter);
        eventFilter.getPropFilters().add(propFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        textFilter.setValue("bogus");
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateVFreeBusyFilterFilter() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        FileInputStream fis = new FileInputStream(baseDir + "vfreebusy.ics");
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar = cb.build(fis);
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter vfbFilter = new ComponentFilter("VFREEBUSY");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(vfbFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        PropertyFilter propFilter = new PropertyFilter("ORGANIZER");
        TextMatchFilter textFilter = new TextMatchFilter("Joe");
        propFilter.setTextMatchFilter(textFilter);
        vfbFilter.getPropFilters().add(propFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar, filter));
        
        textFilter.setValue("bogus");
        Assert.assertFalse(evaluater.evaluate(calendar, filter));
    }
    
    public void testEvaluateVFreeBusyFilterFilterTimeRange() throws Exception {
        CalendarBuilder cb = new CalendarBuilder();
        CalendarFilterEvaluater evaluater = new CalendarFilterEvaluater();
        Calendar calendar1 = cb.build(new FileInputStream(baseDir + "vfreebusy.ics"));
        Calendar calendar2 = cb.build(new FileInputStream(baseDir + "vfreebusy_no_dtstart.ics"));
        
        CalendarFilter filter = new CalendarFilter();
        ComponentFilter compFilter = new ComponentFilter("VCALENDAR");
        ComponentFilter vfbFilter = new ComponentFilter("VFREEBUSY");
        filter.setFilter(compFilter);
        compFilter.getComponentFilters().add(vfbFilter);
        
        DateTime start = new DateTime("20060102T115000Z");
        DateTime end = new DateTime("20060109T115000Z");
    
        Period period = new Period(start, end);
        TimeRangeFilter timeRangeFilter = new TimeRangeFilter(period);
        vfbFilter.setTimeRangeFilter(timeRangeFilter);
        
        Assert.assertTrue(evaluater.evaluate(calendar1, filter));
        Assert.assertTrue(evaluater.evaluate(calendar2, filter));
        
        start = new DateTime("20070102T115000Z");
        end = new DateTime("20070109T115000Z");
    
        period = new Period(start, end);
        timeRangeFilter.setPeriod(period);
        
        Assert.assertFalse(evaluater.evaluate(calendar1, filter));
        Assert.assertFalse(evaluater.evaluate(calendar2, filter));
        
    }
}
