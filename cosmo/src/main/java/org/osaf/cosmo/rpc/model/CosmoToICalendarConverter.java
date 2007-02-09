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
package org.osaf.cosmo.rpc.model;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.osaf.cosmo.icalendar.ICalendarConstants.PARAM_X_OSAF_ANYTIME;
import static org.osaf.cosmo.icalendar.ICalendarConstants.VALUE_TRUE;
import static org.osaf.cosmo.rpc.model.ICalendarToCosmoConverter.EVENT_ALLDAY;
import static org.osaf.cosmo.rpc.model.ICalendarToCosmoConverter.EVENT_DESCRIPTION;
import static org.osaf.cosmo.rpc.model.ICalendarToCosmoConverter.EVENT_END;
import static org.osaf.cosmo.rpc.model.ICalendarToCosmoConverter.EVENT_START;
import static org.osaf.cosmo.rpc.model.ICalendarToCosmoConverter.EVENT_STATUS;
import static org.osaf.cosmo.rpc.model.ICalendarToCosmoConverter.EVENT_TITLE;
import static org.osaf.cosmo.rpc.model.ICalendarToCosmoConverter.EVENT_LOCATION;
import static org.osaf.cosmo.util.CollectionUtils.createSetFromArray;
import static org.osaf.cosmo.util.ICalendarUtils.getMasterEvent;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterFactoryImpl;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DateListProperty;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.id.uuid.VersionFourGenerator;
import org.apache.commons.lang.StringUtils;
import org.osaf.cosmo.CosmoConstants;
import org.osaf.cosmo.util.ICalendarUtils;

/**
 * An instance of this class is used to convert Scooby Events into ical4j events
 * or ical4j events wrapped in ical4j Calendars.
 *
 *
 * @author bobbyrullo
 *
 */
public class CosmoToICalendarConverter {

    // TODO Have spring manage UUID generator
    public static VersionFourGenerator uidGenerator = new VersionFourGenerator();

    public static TimeZoneRegistry timeZoneRegistry = TimeZoneRegistryFactory
            .getInstance().createRegistry();

    /**
     * Creates a Calendar object which wraps an ical4j VEvent
     *
     * All timezone information is ignored at this point, so all dates are
     * "floating" dates
     *
     * @param event
     */
    public Calendar createWrappedVEvent(Event event) {
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId(CosmoConstants.PRODUCT_ID));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        VEvent vevent = createVEvent(event);
        calendar.getComponents().add(vevent);
        addModificationsToCalendar(calendar, vevent, event);
        updateVTimeZones(calendar);
        return calendar;
    }

    /**
     * Creates a VEvent from a scooby Event.
     *
     * All timezone information is ignored at this point, so all dates are
     * "floating" dates
     *
     * @param event
     */
    public VEvent createVEvent(Event event) {
        VEvent vevent = new VEvent();

        copyProperties(event, vevent);

        // New events need a new uid
        Uid uid = new Uid();
        uid.setValue(event.getId() != null ? event.getId() : getNextUID());
        vevent.getProperties().add(uid);

        return vevent;
    }

    /**
     * Updates an existing event within an ical4j calendar by finding the VEvent
     * within the calendar with the same UID and copying its properties to that
     * event. Any properties that Scooby doesn't support are preserved.
     *
     * Timezones of all dates will remain as they were originally.
     *
     * @param event
     *            the scooby event
     * @param calendar
     *            the calendar containing the VEvent to update
     */
    public void updateEvent(Event event, Calendar calendar) {
        VEvent vevent = getMasterEvent(calendar);
        copyProperties(event, vevent);
        addModificationsToCalendar(calendar, vevent, event);
    }

    /**
     * Sets a DateProperty with the Date (no time) represented by the
     * ScoobyDate.
     *
     * @param dateProp
     * @param scoobyDate
     */
    protected void setDate(DateProperty dateProp, CosmoDate scoobyDate) {
        String dateString = getDateTimeString(scoobyDate, false, false);
        Date date = null;
        try {
            date = new Date(dateString);
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
        dateProp.setDate(date);
        dateProp.getParameters().add(Value.DATE);
    }

    protected void setDateOrDatetime(DateProperty destProp,
            CosmoDate sourceDate, boolean dateTime) {
        if (dateTime){
            setDateTime(destProp, sourceDate);
        } else {
            setDate(destProp, sourceDate);
        }

    }

    /**
     * Sets a DateProperty with the DateTime represented by the ScoobyDate.
     *
     * @param dateProp
     * @param scoobyDate
     */
    protected void setDateTime(DateProperty dateProp, CosmoDate scoobyDate) {
        String dateString = getDateTimeString(scoobyDate, true, scoobyDate
                .isUtc());
        DateTime dateTime = null;
        try {
            String tzId = scoobyDate.getTzId();
            TimeZone timeZone = null;

            if (!StringUtils.isEmpty(tzId)){
                timeZone= timeZoneRegistry.getTimeZone(tzId);
            }

            if (timeZone != null){
                dateTime = new DateTime(dateString, timeZone);
            } else {
                dateTime = new DateTime(dateString);
            }
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
        dateProp.setDate(dateTime);
    }

    protected DateTime createDateTime(CosmoDate cosmoDate, DateProperty timeZoneSource){
        String dateString = getDateTimeString(cosmoDate, true, cosmoDate
                .isUtc());
        DateTime dateTime = null;
        try {
            dateTime = new DateTime(dateString);
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
        DateTime timeZoneSourceDateTime = (DateTime) timeZoneSource.getDate();
        TimeZone tz = timeZoneSourceDateTime.getTimeZone();
        if (tz != null){
            dateTime.setTimeZone(timeZoneSourceDateTime.getTimeZone());
        }
        return dateTime;
    }

    protected void copyProperties(Event event, VEvent vevent) {
        // if dtStart and end are dates with times, this is true
        boolean dateTime = true;
        DtStart dtStart = null;
        DtEnd dtEnd = null;

        if (event.isAllDay()) {
            dateTime = false;
            dtStart = new DtStart();
            setDate(dtStart, event.getStart());
            if (event.getEnd() != null) {
                dtEnd = new DtEnd();
                java.util.Calendar endCalendar = createCalendar(event.getEnd());
                endCalendar.add(java.util.Calendar.DATE, 1);
                Date endDate = null;
                try {
                    endDate = new Date(getDateTimeString(endCalendar, false,
                            false));
                } catch (ParseException pe) {
                    throw new RuntimeException(pe);
                }
                dtEnd.setDate(endDate);
                dtEnd.getParameters().add(Value.DATE);
            }

        } else if (event.isAnyTime()) {
            dateTime = false;
            Parameter anyTimeParam = null;
            try {
                anyTimeParam = ParameterFactoryImpl.getInstance()
                        .createParameter(PARAM_X_OSAF_ANYTIME, VALUE_TRUE);
            } catch (URISyntaxException urise) {
                throw new RuntimeException(urise);
            }
            dtStart = new DtStart();
            setDate(dtStart, event.getStart());
            dtStart.getParameters().add(anyTimeParam);
            removeProperty(vevent, Property.DTEND);

        } else if (event.isPointInTime()) {
            dtStart = new DtStart();
            setDateTime(dtStart, event.getStart());
            removeProperty(vevent, Property.DTEND);
        } else {
            dtStart = new DtStart();
            setDateTime(dtStart, event.getStart());
            dtEnd = new DtEnd();
            setDateTime(dtEnd, event.getEnd());
        }

        Summary summary = new Summary();
        summary.setValue(event.getTitle());
        
        Location location = new Location();
        location.setValue(event.getLocation());

        Description description = new Description();
        description.setValue(event.getDescription());

        Status status = null;
        if (!StringUtils.isEmpty(event.getStatus())) {
            status = new Status();
            status.setValue(event.getStatus());
        }

        addOrReplaceProperty(vevent, description);
        addOrReplaceProperty(vevent, summary);
        addOrReplaceProperty(vevent, location);
        if (status != null) {
            addOrReplaceProperty(vevent, status);
        } else {
            removeProperty(vevent, Property.STATUS);
        }
        addOrReplaceProperty(vevent, dtStart);
        if (dtEnd != null) {
            addOrReplaceProperty(vevent, dtEnd);
        }
        removeProperty(vevent, Property.DURATION);

        RecurrenceRule recurrenceRule = event.getRecurrenceRule();
        if (recurrenceRule != null && isEmpty(recurrenceRule.getCustomRule())) {
            String rule = getFrequency(recurrenceRule.getFrequency())
                    + ";";
            if (recurrenceRule.getEndDate() != null) {
                CosmoDate rruleEndDate = recurrenceRule.getEndDate();
                if (event.isAllDay() || event.isAnyTime()) {
                    java.util.Calendar rruleEndDateCalendar = createCalendar(recurrenceRule
                            .getEndDate());
                    String dateString = getDateTimeString(rruleEndDateCalendar,
                            false, false);
                    rule += "UNTIL=" + dateString;
                } else {
                    if (!StringUtils.isEmpty(event.getStart().getTzId())){
                        java.util.Calendar endDateCalendar = java.util.Calendar
                                .getInstance(((DateTime) dtStart.getDate())
                                        .getTimeZone());
                        endDateCalendar.set(java.util.Calendar.YEAR, rruleEndDate.getYear());
                        endDateCalendar.set(java.util.Calendar.MONTH, rruleEndDate.getMonth());
                        endDateCalendar.set(java.util.Calendar.DATE, rruleEndDate.getDate());
                        endDateCalendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
                        endDateCalendar.set(java.util.Calendar.MINUTE, 59);
                        endDateCalendar.set(java.util.Calendar.SECOND, 59);
                        long timeInMillis = endDateCalendar.getTimeInMillis();
                        endDateCalendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                        endDateCalendar.setTimeInMillis(timeInMillis);
                        String dateString = getDateTimeString(endDateCalendar, true, true);
                        rule += "UNTIL=" + dateString;
                    } else {
                        // a floating recurring event
                        java.util.Calendar endDateCalendar = java.util.Calendar.getInstance();
                        endDateCalendar.set(java.util.Calendar.YEAR,
                                rruleEndDate.getYear());
                        endDateCalendar.set(java.util.Calendar.MONTH,
                                rruleEndDate.getMonth());
                        endDateCalendar.set(java.util.Calendar.DATE,
                                rruleEndDate.getDate());
                        endDateCalendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
                        endDateCalendar.set(java.util.Calendar.MINUTE, 59);
                        endDateCalendar.set(java.util.Calendar.SECOND, 59);
                        String dateString = getDateTimeString(endDateCalendar, true,false);
                        rule += "UNTIL=" + dateString;
                    }
                }
            }
            try {
                RRule rrule = new RRule(new Recur(rule));
                addOrReplaceProperty(vevent,rrule);
            } catch (ParseException pe) {
                throw new RuntimeException(pe);
            }

            CosmoDate[] exceptionDates = recurrenceRule.getExceptionDates();
            if (exceptionDates != null && exceptionDates.length > 0){
                ExDate exDate = new ExDate();

                //just use the timezone of the DTSTART for now (maybe forever)
                boolean utc = false;
                if (dateTime){
                    utc = dtStart.isUtc();
                    if (utc){
                        exDate.setUtc(true);
                    } else {
                        copyTimeZone(dtStart, exDate);
                    }
                }

                for (int x = 0; x < exceptionDates.length; x++) {
                    try {
                        if (dateTime){
                        exDate.getDates().add(
                                new DateTime(getDateTimeString(exceptionDates[x],
                                        dateTime, utc)));
                        } else {
                            exDate.getDates().add(
                                    new Date(getDateTimeString(exceptionDates[x],
                                            dateTime, utc)));
                        }
                    } catch (ParseException pe) {
                        throw new RuntimeException(pe);
                    }
                }

                addOrReplaceProperty(vevent, exDate);
            }
        } else if (recurrenceRule == null) {
            removeProperty(vevent, Property.RRULE);
            removeProperty(vevent, Property.EXDATE);
        }

    }

    public void updateVTimeZones(Calendar calendar){
        //first get all tzids references
        Set<String> tzIds = getTzIdsUsed(calendar);
        Set<String> tzIdsWithVTimeZones = new HashSet<String>();

        //then get all VTimeZones, get rid of VTimezones
        ComponentList vtzs = calendar.getComponents().getComponents(Component.VTIMEZONE);
        for (Object o : vtzs){
            VTimeZone vtz = (VTimeZone) o;
            String tzid = ICalendarUtils.getTZId(vtz);
            if (!tzIds.contains(tzid)){
                calendar.getComponents().remove(vtz);
            } else {
                tzIdsWithVTimeZones.add(tzid);
            }
        }

        //now add VTimeZones for all tzids which don't already have one
        tzIds.remove(tzIdsWithVTimeZones);
        for (String tzId : tzIds){
            TimeZone tz = timeZoneRegistry.getTimeZone(tzId);
            calendar.getComponents().add(tz.getVTimeZone());
        }

    }

    private Set<String> getTzIdsUsed(Calendar calendar){
        Set<String> l = new HashSet<String>();

        ComponentList comps = calendar.getComponents();
        for (int x = 0; x < comps.size(); x++){
            Component c = (Component) comps.get(x);
            PropertyList props = c.getProperties();
            if (!(c instanceof VTimeZone)) {
                for (int y = 0; y < props.size(); y++) {
                    Property property = (Property) props.get(y);
                    if (property instanceof DateProperty) {
                        String tzid = ICalendarUtils.getTZId((DateProperty) property);
                        if (!StringUtils.isEmpty(tzid)){
                            l.add(tzid);
                        }
                    }
                    if (property instanceof DateListProperty) {
                        String tzid = ICalendarUtils.getTZId((DateListProperty) property);
                        if (!StringUtils.isEmpty(tzid)){
                            l.add(tzid);
                        }
                    }
                }
            }
        }

        return l;
    }

    private String getFrequency(String frequency) {
        String freq = "FREQ=";
        if (frequency.equals(RecurrenceRule.FREQUENCY_DAILY)){
            return freq + Recur.DAILY;
        } else if (frequency.equals(RecurrenceRule.FREQUENCY_WEEKLY)){
            return freq + Recur.WEEKLY;
        } else if (frequency.equals(RecurrenceRule.FREQUENCY_BIWEEKLY)){
            return freq + Recur.WEEKLY + ";INTERVAL=2";
        } else if (frequency.equals(RecurrenceRule.FREQUENCY_MONTHLY)){
            return freq + Recur.MONTHLY;
        } else if (frequency.equals(RecurrenceRule.FREQUENCY_YEARLY)){
            return freq + Recur.YEARLY;
        } else {
            return null;
        }
    }

    protected static void addOrReplaceProperty(VEvent destination,
            Property property) {

        PropertyList oldPropList = destination.getProperties().getProperties(
                property.getName());
        for (Object o : oldPropList) {
            Property oldProp = (Property) o;
            if (oldProp != null) {
                destination.getProperties().remove(oldProp);
            }
        }
        destination.getProperties().add(property);

    }

    protected static void removeProperty(Component component, String propName) {
        Property property = component.getProperties().getProperty(propName);
        component.getProperties().remove(property);
    }

    protected static String getDateTimeString(CosmoDate scoobyDate,
            boolean includeTime, boolean utc) {
        StringBuffer buf = new StringBuffer();
        buf.append(StringUtils.leftPad("" + scoobyDate.getYear(), 4, "0"));
        buf.append(StringUtils
                .leftPad("" + (scoobyDate.getMonth() + 1), 2, "0"));
        buf.append(StringUtils.leftPad("" + scoobyDate.getDate(), 2, "0"));
        if (includeTime) {
            buf.append("T");
            buf.append(StringUtils.leftPad("" + scoobyDate.getHours(), 2, "0"));
            buf.append(StringUtils
                    .leftPad("" + scoobyDate.getMinutes(), 2, "0"));
            buf.append(StringUtils
                    .leftPad("" + scoobyDate.getSeconds(), 2, "0"));
            if (utc) {
                buf.append("Z");
            }
        }
        return buf.toString();
    }

    protected static String getDateTimeString(java.util.Calendar calendar,
            boolean includeTime, boolean utc) {
        StringBuffer buf = new StringBuffer();
        buf.append(StringUtils.leftPad(""
                + calendar.get(java.util.Calendar.YEAR), 4, "0"));
        buf.append(StringUtils.leftPad(""
                + (calendar.get(java.util.Calendar.MONTH) + 1), 2, "0"));
        buf.append(StringUtils.leftPad(""
                + calendar.get(java.util.Calendar.DAY_OF_MONTH), 2, "0"));
        if (includeTime) {
            buf.append("T");
            buf.append(StringUtils.leftPad(""
                    + calendar.get(java.util.Calendar.HOUR_OF_DAY), 2, "0"));
            buf.append(StringUtils.leftPad(""
                    + calendar.get(java.util.Calendar.MINUTE), 2, "0"));
            buf.append(StringUtils.leftPad(""
                    + calendar.get(java.util.Calendar.SECOND), 2, "0"));
            if (utc) {
                buf.append("Z");
            }
        }
        return buf.toString();
    }

    protected static java.util.Calendar createCalendar(CosmoDate scoobyDate) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.clear();
        calendar.set(java.util.Calendar.YEAR, scoobyDate.getYear());
        calendar.set(java.util.Calendar.MONTH, scoobyDate.getMonth());
        calendar.set(java.util.Calendar.DAY_OF_MONTH, scoobyDate.getDate());
        calendar.set(java.util.Calendar.HOUR_OF_DAY, scoobyDate.getHours());
        calendar.set(java.util.Calendar.MINUTE, scoobyDate.getMinutes());
        calendar.set(java.util.Calendar.SECOND, scoobyDate.getDate());
        return calendar;
    }

    protected static String getNextUID() {
        return uidGenerator.nextIdentifier().toString();
    }

    /**
     * Copies the TZID from the source to the dest
     *
     * @param source
     * @param dest
     */
    protected void copyTimeZone(DateProperty source, DateProperty dest) {
        TzId sourceTz = (TzId) source.getParameters().getParameter(
                Parameter.TZID);
        if (sourceTz != null) {
            dest.getParameters().add(sourceTz);
        }
    }

    protected void copyTimeZone(DateProperty source, DateListProperty dest){
        TzId sourceTz = (TzId) source.getParameters().getParameter(
                Parameter.TZID);
        if (sourceTz != null) {
            dest.getParameters().add(sourceTz);
        }
    }

    private boolean hasProperty(Component c, String propName) {
        PropertyList l = c.getProperties().getProperties(propName);
        return l != null && l.size() > 0;
    }

    private void removeInstances(Calendar calendar){
        for (Object o : calendar.getComponents()
                .getComponents(Component.VEVENT)) {
            VEvent vevent = (VEvent) o;
            if (hasProperty(vevent, Property.RECURRENCE_ID)){
                calendar.getComponents().remove(vevent);
            }

        }
    }

    private void addModificationsToCalendar(Calendar calendar,
            VEvent masterVEvent, Event event) {

        boolean isCustom = event.getRecurrenceRule() != null
                && StringUtils.isNotBlank(event.getRecurrenceRule()
                        .getCustomRule());

        if (isCustom){
            return;
        }

        //no matter what we need to blow away modifications
        removeInstances(calendar);

        if (event.getRecurrenceRule() == null){
            return;
        }

        DtStart dtStart = masterVEvent.getStartDate();
        RecurrenceRule recurrenceRule = event.getRecurrenceRule();
        Modification[] modifications = recurrenceRule.getModifications();
        boolean dateTime = dtStart.getDate() instanceof DateTime;

        if (modifications != null && modifications.length > 0) {
            for (Modification modification : modifications) {
                VEvent modVEvent = new VEvent();
                Event modEvent = modification.getEvent();
                RecurrenceId recurrenceId = new RecurrenceId();
                setDateOrDatetime(recurrenceId,
                        modification.getInstanceDate(), dateTime);
                modVEvent.getProperties().add(recurrenceId);
                //UID is required
                DtStart modStart = new DtStart(recurrenceId.getDate());
                if (!(recurrenceId.getDate() instanceof DateTime)){
                    //ical4j doesn't automatically add this value.
                    modStart.getParameters().add(Value.DATE);
                }
                ICalendarUtils.addOrReplaceProperty(modVEvent, modStart);
                modVEvent.getProperties().add(masterVEvent.getProperties().getProperty(Property.UID));
                
                //convert properties into a set so we can handle one at a time.
                Set<String> modifiedProperties = createSetFromArray(modification.getModifiedProperties());
                
                
                if (modifiedProperties.contains(EVENT_ALLDAY)){
                    if (modEvent.isAllDay()){
                        //this means that the master event must NOT be all day, 
                        //but the instance is
                        
                        //strip the time from the start time
                        modStart = new DtStart();
                        //we can assume that the modification has a modified Start property
                        setDateOrDatetime(modStart, modification.getEvent().getStart(), false);
                        ICalendarUtils.addOrReplaceProperty(modVEvent, modStart);
                        
                        //strip the time from the end time, add one day
                        DtEnd modEnd = new DtEnd();

                        //we can also assume that the modification has a modified Start property
                        setDateOrDatetime(modEnd, modification.getEvent().getEnd(), false);
                        modEnd.getDate().setTime(modEnd.getDate().getTime() + (1000 * 60 * 60 * 24));
                        ICalendarUtils.addOrReplaceProperty(modVEvent, modEnd);
                    } else {
                        //so the master event USED to be all day but no longer is 
                        modStart = new DtStart();
                        setDateOrDatetime(modStart, modification.getEvent()
                                .getStart(), true);
                        ICalendarUtils.addOrReplaceProperty(modVEvent, modStart);

                        DtEnd modEnd = new DtEnd();
                        setDateOrDatetime(modEnd, modification.getEvent()
                                .getEnd(), true);
                        modVEvent.getProperties().add(modEnd);
                    }
                    
                } else {
                    if (modifiedProperties.contains(EVENT_START)) {
                        modStart = new DtStart();
                        setDateOrDatetime(modStart, modification.getEvent()
                                .getStart(), dateTime);
                        ICalendarUtils.addOrReplaceProperty(modVEvent, modStart);
                    }
                    
                    if (modifiedProperties.contains(EVENT_END)) {
                        DtEnd modEnd = new DtEnd();
                        setDateOrDatetime(modEnd, modification.getEvent()
                                .getEnd(), dateTime);
                        modVEvent.getProperties().add(modEnd);
                    }
                }
                
                if (modifiedProperties.contains(EVENT_DESCRIPTION)) {
                    Description description = new Description();
                    description.setValue(modification.getEvent()
                            .getDescription());
                    modVEvent.getProperties().add(description);
                } 
                
                if (modifiedProperties.contains(EVENT_TITLE)) {
                    Summary summary = new Summary();
                    summary.setValue(modification.getEvent().getTitle());
                    modVEvent.getProperties().add(summary);
                }
                
                if (modifiedProperties.contains(EVENT_LOCATION)) {
                    Location location = new Location();
                    location.setValue(modification.getEvent().getLocation());
                    modVEvent.getProperties().add(location);
                }
                
                if (modifiedProperties.contains(EVENT_STATUS)) {
                    Status status = new Status();
                    status.setValue(modification.getEvent().getStatus());
                    modVEvent.getProperties().add(status);
                }
                
                calendar.getComponents().add(modVEvent);
            }
        }
    }
    
}
