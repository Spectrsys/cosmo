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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Transient;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterFactoryImpl;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.osaf.cosmo.CosmoConstants;
import org.osaf.cosmo.icalendar.ICalendarConstants;


/**
 * Represents a calendar event.
 */
public abstract class BaseEventStamp extends Stamp
    implements java.io.Serializable, ICalendarConstants {

    public abstract VEvent getEvent();
    public abstract void setCalendar(Calendar calendar);
    
    
    /**
     * Return BaseEventStamp from Item
     * @param item
     * @return BaseEventStamp from Item
     */
    public static BaseEventStamp getStamp(Item item) {
        return (BaseEventStamp) item.getStamp(BaseEventStamp.class);
    }
    
    /**
     * Return BaseEventStamp from Item
     * @param item
     * @param activeOnly
     * @return BaseEventStamp from Item
     */
    public static BaseEventStamp getStamp(Item item,
                                          boolean activeOnly) {
        return (BaseEventStamp) item.getStamp(BaseEventStamp.class,
                                              activeOnly);
    }
    
    /**
     * Returns a copy of the the iCalendar UID property value of the
     * event .
     */
    @Transient
    public String getIcalUid() {
        return getEvent().getUid().getValue();
    }

    /**
     * Returns a copy of the the iCalendar SUMMARY property value of
     * the event (can be null).
     */
    @Transient
    public String getSummary() {
        Property p = getEvent().getProperties().
            getProperty(Property.SUMMARY);
        if (p == null)
            return null;
        return p.getValue();
    }

    /** 
     * Sets the iCalendar SUMMARY property of the event.
     *
     * @param text a text string
     */
    @Transient
    public void setSummary(String text) {
        setDirty(true);
        Summary summary = (Summary)
            getEvent().getProperties().getProperty(Property.SUMMARY);
        if (text == null) {
            if (summary != null)
                getEvent().getProperties().remove(summary);
            return;
        }                
        if (summary == null) {
            summary = new Summary();
            getEvent().getProperties().add(summary);
        }
        summary.setValue(text);
    }
    
    /**
     * Returns a copy of the the iCalendar DESCRIPTION property value of
     * the event (can be null).
     */
    @Transient
    public String getDescription() {
        Property p = getEvent().getProperties().
            getProperty(Property.DESCRIPTION);
        if (p == null)
            return null;
        return p.getValue();
    }

    /** 
     * Sets the iCalendar DESCRIPTION property of the event.
     *
     * @param text a text string
     */
    @Transient
    public void setDescription(String text) {
        setDirty(true);
        Description description = (Description)
            getEvent().getProperties().getProperty(Property.DESCRIPTION);
       
        if (text == null) {
            if (description != null)
                getEvent().getProperties().remove(description);
            return;
        }                
        if (description == null) {
            description = new Description();
            getEvent().getProperties().add(description);
        }
        description.setValue(text);
    }

    /**
     * Returns a copy of the the iCalendar DTSTART property value of
     * the event (never null).
     */
    @Transient
    public Date getStartDate() {
        DtStart dtStart = getEvent().getStartDate();
        if (dtStart == null)
            return null;
        return dtStart.getDate();
    }

    /** 
     * Sets the iCalendar DTSTART property of the event.
     *
     * @param date a <code>Date</code>
     */
    @Transient
    public void setStartDate(Date date) {
        DtStart dtStart = getEvent().getStartDate();
        if (dtStart != null)
            dtStart.setDate(date);
        else {
            dtStart = new DtStart(date);
            getEvent().getProperties().add(dtStart);
        }
        setDatePropertyValue(dtStart, date);
        setDirty(true);
    }

    /**
     * Returns the end date of the event as calculated from the
     * iCalendar DTEND property value or the the iCalendar DTSTART +
     * DURATION (never null).
     */
    @Transient
    public Date getEndDate() {
        DtEnd dtEnd = getEvent().getEndDate();
        if (dtEnd == null)
            return null;
        return dtEnd.getDate();
    }

    /** 
     * Sets the iCalendar DTEND property of the event.
     *
     * @param date a <code>Date</code>
     */
    @Transient
    public void setEndDate(Date date) {
        DtEnd dtEnd = getEvent().getEndDate();
        if (dtEnd != null)
            dtEnd.setDate(date);
        else {
            // remove the duration if there was one
            Duration duration = (Duration) getEvent().getProperties().
                getProperty(Property.DURATION);
            if (duration != null)
                getEvent().getProperties().remove(duration);
            dtEnd = new DtEnd(date);
            getEvent().getProperties().add(dtEnd);
        }
        setDatePropertyValue(dtEnd, date);
        setDirty(true);
    }

    @Transient
    protected void setDatePropertyValue(DateProperty prop,
                                        Date date) {
        if (prop == null)
            return;
        Value value = (Value)
            prop.getParameters().getParameter(Parameter.VALUE);
        if (value != null)
            prop.getParameters().remove(value);
        value = date instanceof DateTime ? Value.DATE_TIME : Value.DATE;
        prop.getParameters().add(value);
        setDirty(true);
    }

    /**
     * Returns a copy of the the iCalendar LOCATION property value of
     * the event (can be null).
     */
    @Transient
    public String getLocation() {
        Property p = getEvent().getProperties().
            getProperty(Property.LOCATION);
        if (p == null)
            return null;
        return p.getValue();
    }

    /** 
     * Sets the iCalendar LOCATION property of the event.
     *
     * @param text a text string
     */
    @Transient
    public void setLocation(String text) {
        setDirty(true);
        
        Location location = (Location)
            getEvent().getProperties().getProperty(Property.LOCATION);
        
        if (text == null) {
            if (location != null)
                getEvent().getProperties().remove(location);
            return;
        }                
        if (location == null) {
            location = new Location();
            getEvent().getProperties().add(location);
        }
        location.setValue(text);
    }
    
    /**
     * Returns a list of copies of the iCalendar RRULE property values
     * of the event (can be empty).
     */
    @Transient
    public List<Recur> getRecurrenceRules() {
        ArrayList<Recur> l = new ArrayList<Recur>();
        for (RRule rrule : (List<RRule>) getEvent().getProperties().
                 getProperties(Property.RRULE))
            l.add(rrule.getRecur());
        return l;
    }

    /** 
     * Sets the iCalendar RRULE properties of the event,
     * removing any RRULEs that were previously set.
     *
     * @param recurs a <code>List</code> of <code>Recur</code>s
     */
    @Transient
    public void setRecurrenceRules(List<Recur> recurs) {
        if (recurs == null)
            return;
        PropertyList pl = getEvent().getProperties();
        for (RRule rrule : (List<RRule>) pl.getProperties(Property.RRULE))
            pl.remove(rrule);
        for (Recur recur : recurs)
            pl.add(new RRule(recur));
        
        setDirty(true);
    }

    /**
     * Returns a list of copies of the iCalendar EXRULE property values
     * of the event (can be empty).
     */
    @Transient
    public List<Recur> getExceptionRules() {
        ArrayList<Recur> l = new ArrayList<Recur>();
        for (ExRule exrule : (List<ExRule>) getEvent().getProperties().
                 getProperties(Property.EXRULE))
            l.add(exrule.getRecur());
        return l;
    }

    /** 
     * Sets the iCalendar EXRULE properties of the event,
     * removing any EXRULEs that were previously set.
     *
     * @param recurs a <code>List</code> of <code>Recur</code>s
     */
    @Transient
    public void setExceptionRules(List<Recur> recurs) {
        if (recurs == null)
            return;
        PropertyList pl = getEvent().getProperties();
        for (ExRule exrule : (List<ExRule>) pl.getProperties(Property.EXRULE))
            pl.remove(exrule);
        for (Recur recur : recurs)
            pl.add(new ExRule(recur));
        setDirty(true);
    }

    /**
     * Returns a list of copies of the iCalendar RDATE property values
     * of the event (can be empty).
     */
    @Transient
    public DateList getRecurrenceDates() {
        DateList l = new DateList(Value.DATE_TIME);
        for (RDate rdate : (List<RDate>) getEvent().getProperties().
                 getProperties(Property.RDATE))
            l.addAll(rdate.getDates());
        return l;
    }

    /**
     * Sets a single iCalendar RDATE property of the event,
     * removing any RDATEs that were previously set.
     *
     * @param dates a <code>DateList</code>
     */
    @Transient
    public void setRecurrenceDates(DateList dates) {
        if (dates == null)
            return;
        
        setDirty(true);
        PropertyList pl = getEvent().getProperties();
        for (RDate rdate : (List<RDate>) pl.getProperties(Property.RDATE))
            pl.remove(rdate);
        if (dates.isEmpty())
            return;
        pl.add(new RDate(dates));
    }

    /**
     * Returns a list of copies of the values of all iCalendar EXDATE
     * properties of the event (can be empty).
     */
    @Transient
    public DateList getExceptionDates() {
        DateList l = new DateList(Value.DATE_TIME);
        for (ExDate exdate : (List<ExDate>) getEvent().getProperties().
                 getProperties(Property.EXDATE))
            l.addAll(exdate.getDates());
        return l;
    }
    
    /**
     * Return the first display alarm on the event
     * @return first display alarm on event
     */
    public VAlarm getDisplayAlarm() {
        VEvent event = getEvent();
       
        if(event==null)
            return null;
        
        for(Iterator it = event.getAlarms().iterator();it.hasNext();) {
            VAlarm alarm = (VAlarm) it.next();
            if (alarm.getProperties().getProperty(Property.ACTION).equals(
                    Action.DISPLAY))
                return alarm;
        }
        
        return null;
    }

    /**
     * Sets a single iCalendar EXDATE property of the event,
     * removing any EXDATEs that were previously set.
     *
     * @param dates a <code>DateList</code>
     */
    @Transient
    public void setExceptionDates(DateList dates) {
        if (dates == null)
            return;
        setDirty(true);
        PropertyList pl = getEvent().getProperties();
        for (ExDate exdate : (List<ExDate>) pl.getProperties(Property.EXDATE))
            pl.remove(exdate);
        if (dates.isEmpty())
            return;
        pl.add(new ExDate(dates));
    }

    /**
     * Returns a copy of the the iCalendar RECURRENCE_ID property
     * value of the event (can be null). 
     */
    @Transient
    public Date getRecurrenceId() {
        RecurrenceId rid = getEvent().getReccurrenceId();
        if (rid == null)
            return null;
        return rid.getDate();
    }

    /** 
     * Sets the iCalendar RECURRENCE_ID property of the event.
     *
     * @param date a <code>Date</code>
     */
    @Transient
    public void setRecurrenceId(Date date) {
        setDirty(true);
        RecurrenceId recurrenceId = (RecurrenceId)
            getEvent().getProperties().
            getProperty(Property.RECURRENCE_ID);
        if (date == null) {
            if (recurrenceId != null)
                getEvent().getProperties().remove(recurrenceId);
            return;
        }
        if (recurrenceId == null) {
            recurrenceId = new RecurrenceId();
            getEvent().getProperties().add(recurrenceId);
        }
        recurrenceId.setDate(date);
    }

    /**
     * Returns a copy of the the iCalendar STATUS property value of
     * the event (can be null).
     */
    @Transient
    public String getStatus() {
        Property p = getEvent().getProperties().
            getProperty(Property.STATUS);
        if (p == null)
            return null;
        return p.getValue();
    }

    /** 
     * Sets the iCalendar STATUS property of the event.
     *
     * @param text a text string
     */
    @Transient
    public void setStatus(String text) {
        // ical4j Status value is immutable, so if there's any change
        // at all, we have to remove the old status and add a new
        // one.
        setDirty(true);
        Status status = (Status)
            getEvent().getProperties().getProperty(Property.STATUS);
        if (status != null)
            getEvent().getProperties().remove(status);
        if (text == null)
            return;
        getEvent().getProperties().add(new Status(text));
    }
    
    
    /**
     * Is the event marked as anytime.
     * @return true if the event is an anytime event
     */
    @Transient
    public boolean isAnyTime() {
        DtStart dtStart = getEvent().getStartDate();
        if (dtStart == null)
            return false;
        Parameter parameter = dtStart.getParameters()
            .getParameter(PARAM_X_OSAF_ANYTIME);
        if (parameter == null) {
            return false;
        }

        return VALUE_TRUE.equals(parameter.getValue());
    }
    
    /**
     * Toggle the event anytime parameter.
     * @param isAnyTime true if the event occurs anytime
     */
    public void setAnyTime(boolean isAnyTime) {
        DtStart dtStart = getEvent().getStartDate();
        if (dtStart == null)
            throw new IllegalStateException("event has no start date");
        Parameter parameter = dtStart.getParameters().getParameter(
                PARAM_X_OSAF_ANYTIME);

        setDirty(true);
        
        // add X-OSAF-ANYTIME if it doesn't exist
        if (parameter == null && isAnyTime) {
            dtStart.getParameters().add(getAnyTimeXParam());
            return;
        }

        // if it exists, update based on isAnyTime
        if (parameter != null) {
            String value = parameter.getValue();
            boolean currIsAnyTime = VALUE_TRUE.equals(value);
            if (currIsAnyTime && !isAnyTime)
                dtStart.getParameters().remove(parameter);
            else if (!currIsAnyTime && isAnyTime) {
                dtStart.getParameters().remove(parameter);
                dtStart.getParameters().add(getAnyTimeXParam());
            }
        }
    }
    
    @Transient
    private Parameter getAnyTimeXParam() {
        return new XParameter(PARAM_X_OSAF_ANYTIME, VALUE_TRUE);
    }
    
    /**
     * Initializes the Calendar with a default master event.
     * Initializes the master event using the underlying item's
     * icalUid (if NoteItem) or uid, and if the item is a NoteItem,
     * initializes SUMMARY and DESCRIPTION with the NoteItem's 
     * displayName and body.
     */
    public void createCalendar() {
        Calendar cal = new Calendar();
        cal.getProperties().add(new ProdId(CosmoConstants.PRODUCT_ID));
        cal.getProperties().add(Version.VERSION_2_0);
        cal.getProperties().add(CalScale.GREGORIAN);
        
        VEvent vevent = new VEvent();
        Uid uid = new Uid();
        NoteItem note = null;
        if(getItem()!=null && getItem() instanceof NoteItem)
            note = (NoteItem) getItem();
        
        // VEVENT UID is the NoteItem's icalUid
        // if it exists, or just the Item's uid
        if(note!=null && note.getIcalUid() != null)
            uid.setValue(note.getIcalUid());
        else
            uid.setValue(getItem().getUid());
            
        vevent.getProperties().add(uid);
       
        cal.getComponents().add(vevent);
        setCalendar(cal);
        
        // SUMMARY is NoteItem.displayName and
        // DESCRIPTION is NoteItem.body
        if(note!=null) {
            setSummary(note.getDisplayName());
            setDescription(note.getBody());
        }
    }
}