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

import java.util.Iterator;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Transient;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.validator.NotNull;
import org.osaf.cosmo.CosmoConstants;
import org.osaf.cosmo.calendar.util.CalendarUtils;
import org.osaf.cosmo.hibernate.validator.EventException;
import org.osaf.cosmo.icalendar.ICalendarConstants;


/**
 * Represents a calendar event.
 */
@Entity
@DiscriminatorValue("eventexception")
@SecondaryTable(name="eventexception_stamp", pkJoinColumns={
        @PrimaryKeyJoinColumn(name="stampid", referencedColumnName="id")})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EventExceptionStamp extends Stamp implements
        java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3992468809776886156L;

    private Calendar calendar = null;
    
    /** default constructor */
    public EventExceptionStamp() {
    }
    
    public EventExceptionStamp(Item item) {
        setItem(item);
    }
    
    @Transient
    public String getType() {
        return "eventexception";
    }
    
    @Column(table="eventexception_stamp", name = "icaldata", length=102400000, nullable = false)
    @Type(type="calendar_clob")
    @NotNull
    @EventException
    public Calendar getExceptionCalendar() {
        return calendar;
    }
    
    public void setExceptionCalendar(Calendar calendar) {
        this.calendar = calendar;
    }
    
    /**
     * Returns the exception event extracted from the underlying
     * icalendar object. Changes to the exception event will be persisted
     * when the stamp is saved.
     */
    @Transient
    public VEvent getExceptionEvent() {
        return (VEvent) getExceptionCalendar().getComponents().getComponents(
                Component.VEVENT).get(0);
    }
    
    public void setExceptionEvent(VEvent event) {
        if(calendar==null)
            createCalendar();
        
        // remove all events
        calendar.getComponents().removeAll(
                calendar.getComponents().getComponents(Component.VEVENT));
        
        // add event exception
        calendar.getComponents().add(event);
    }

    /**
     * Returns a copy of the the iCalendar UID property value of the
     * exception event .
     */
    @Transient
    public String getIcalUid() {
        return getExceptionEvent().getUid().getValue();
    }

    /**
     * Returns a copy of the the iCalendar SUMMARY property value of
     * the exception event (can be null).
     */
    @Transient
    public String getSummary() {
        Property p = getExceptionEvent().getProperties().
            getProperty(Property.SUMMARY);
        if (p == null)
            return null;
        return p.getValue();
    }

    /** 
     * Sets the iCalendar SUMMARY property of the exception event.
     *
     * @param text a text string
     */
    @Transient
    public void setSummary(String text) {
        Summary summary = (Summary)
            getExceptionEvent().getProperties().getProperty(Property.SUMMARY);
        if (text == null) {
            if (summary != null)
                getExceptionEvent().getProperties().remove(summary);
            return;
        }                
        if (summary == null) {
            summary = new Summary();
            getExceptionEvent().getProperties().add(summary);
        }
        summary.setValue(text);
    }
    
    /**
     * Returns a copy of the the iCalendar DESCRIPTION property value of
     * the exception event (can be null).
     */
    @Transient
    public String getDescription() {
        Property p = getExceptionEvent().getProperties().
            getProperty(Property.DESCRIPTION);
        if (p == null)
            return null;
        return p.getValue();
    }

    /** 
     * Sets the iCalendar DESCRIPTION property of the exception event.
     *
     * @param text a text string
     */
    @Transient
    public void setDescription(String text) {
        Description description = (Description)
            getExceptionEvent().getProperties().getProperty(Property.DESCRIPTION);
        if (text == null) {
            if (description != null)
                getExceptionEvent().getProperties().remove(description);
            return;
        }                
        if (description == null) {
            description = new Description();
            getExceptionEvent().getProperties().add(description);
        }
        description.setValue(text);
    }

    /**
     * Returns a copy of the the iCalendar DTSTART property value of
     * the exception event (never null).
     */
    @Transient
    public Date getStartDate() {
        DtStart dtStart = getExceptionEvent().getStartDate();
        if (dtStart == null)
            return null;
        return dtStart.getDate();
    }

    /** 
     * Sets the iCalendar DTSTART property of the exception event.
     *
     * @param date a <code>Date</code>
     */
    @Transient
    public void setStartDate(Date date) {
        DtStart dtStart = getExceptionEvent().getStartDate();
        if (dtStart != null)
            dtStart.setDate(date);
        else
            getExceptionEvent().getProperties().add(new DtStart(date));
    }

    /**
     * Returns the end date of the exception event as calculated from the
     * iCalendar DTEND property value or the the iCalendar DTSTART +
     * DURATION (never null).
     */
    @Transient
    public Date getEndDate() {
        DtEnd dtEnd = getExceptionEvent().getEndDate();
        if (dtEnd == null)
            return null;
        return dtEnd.getDate();
    }

    /** 
     * Sets the iCalendar DTEND property of the exception event.
     *
     * @param date a <code>Date</code>
     */
    @Transient
    public void setEndDate(Date date) {
        DtEnd dtEnd = getExceptionEvent().getEndDate();
        if (dtEnd != null)
            dtEnd.setDate(date);
        else {
            // remove the duration if there was one
            Duration duration = (Duration) getExceptionEvent().getProperties().
                getProperty(Property.DURATION);
            if (duration != null)
                getExceptionEvent().getProperties().remove(duration);
            getExceptionEvent().getProperties().add(new DtEnd(date));
        }
    }

    /**
     * Returns a copy of the the iCalendar LOCATION property value of
     * the exception event (can be null).
     */
    @Transient
    public String getLocation() {
        Property p = getExceptionEvent().getProperties().
            getProperty(Property.LOCATION);
        if (p == null)
            return null;
        return p.getValue();
    }

    /** 
     * Sets the iCalendar LOCATION property of the exception event.
     *
     * @param text a text string
     */
    @Transient
    public void setLocation(String text) {
        Location location = (Location)
            getExceptionEvent().getProperties().getProperty(Property.LOCATION);
        if (text == null) {
            if (location != null)
                getExceptionEvent().getProperties().remove(location);
            return;
        }                
        if (location == null) {
            location = new Location();
            getExceptionEvent().getProperties().add(location);
        }
        location.setValue(text);
    }
    
  
    /**
     * Return the first display alarm on the event exception.
     * @return first display alarm on event
     */
    @Transient
    public VAlarm getDisplayAlarm() {
        
        for(Iterator it = getExceptionEvent().getAlarms().iterator();it.hasNext();) {
            VAlarm alarm = (VAlarm) it.next();
            if (alarm.getProperties().getProperty(Property.ACTION).equals(
                    Action.DISPLAY))
                return alarm;
        }
        
        return null;
    }

  
    /**
     * Returns a copy of the the iCalendar RECURRENCE_ID property
     * value of the master event (can be null). 
     */
    @Transient
    public Date getRecurrenceId() {
        RecurrenceId rid = getExceptionEvent().getReccurrenceId();
        if (rid == null)
            return null;
        return rid.getDate();
    }

    /** 
     * Sets the iCalendar RECURRENCE_ID property of the exception event.
     *
     * @param date a <code>Date</code>
     */
    @Transient
    public void setRecurrenceId(Date date) {
        RecurrenceId recurrenceId = (RecurrenceId)
            getExceptionEvent().getProperties().
            getProperty(Property.RECURRENCE_ID);
        if (date == null) {
            if (recurrenceId != null)
                getExceptionEvent().getProperties().remove(recurrenceId);
            return;
        }
        if (recurrenceId == null) {
            recurrenceId = new RecurrenceId();
            getExceptionEvent().getProperties().add(recurrenceId);
        }
        recurrenceId.setDate(date);
    }

    /**
     * Returns a copy of the the iCalendar STATUS property value of
     * the exception event (can be null).
     */
    @Transient
    public String getStatus() {
        Property p = getExceptionEvent().getProperties().
            getProperty(Property.STATUS);
        if (p == null)
            return null;
        return p.getValue();
    }

    /** 
     * Sets the iCalendar STATUS property of the exception event.
     *
     * @param text a text string
     */
    @Transient
    public void setStatus(String text) {
        // ical4j Status value is immutable, so if there's any change
        // at all, we have to remove the old status and add a new
        // one.
        Status status = (Status)
            getExceptionEvent().getProperties().getProperty(Property.STATUS);
        if (status != null)
            getExceptionEvent().getProperties().remove(status);
        if (text == null)
            return;
        getExceptionEvent().getProperties().add(new Status(text));
    }
    
    
    /**
     * Is the event marked as anytime.
     * @return true if the event is an anytime event
     */
    @Transient
    public boolean isAnyTime() {
        DtStart dtStart = getExceptionEvent().getStartDate();
        if (dtStart == null)
            return false;
        Parameter parameter = dtStart.getParameters()
            .getParameter(ICalendarConstants.PARAM_X_OSAF_ANYTIME);
        if (parameter == null) {
            return false;
        }

        return ICalendarConstants.VALUE_TRUE.equals(parameter.getValue());
    }
    
    /**
     * Toggle the event anytime parameter.
     * @param isAnyTime true if the event occurs anytime
     */
    public void setAnyTime(boolean isAnyTime) {
        DtStart dtStart = getExceptionEvent().getStartDate();
        if (dtStart == null)
            throw new IllegalStateException("event has no start date");
        Parameter parameter = dtStart.getParameters().getParameter(
                ICalendarConstants.PARAM_X_OSAF_ANYTIME);

        // add X-OSAF-ANYTIME if it doesn't exist
        if (parameter == null && isAnyTime) {
            dtStart.getParameters().add(getAnyTimeXParam());
            return;
        }

        // if it exists, update based on isAnyTime
        if (parameter != null) {
            String value = parameter.getValue();
            boolean currIsAnyTime = ICalendarConstants.VALUE_TRUE.equals(value);
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
        return new XParameter(ICalendarConstants.PARAM_X_OSAF_ANYTIME,
                ICalendarConstants.VALUE_TRUE);
    }
    
    /**
     * Initializes the Calendar with a default exception event.
     * Initializes the exception event using the underlying item's
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
        setExceptionCalendar(cal);
        
        // SUMMARY is NoteItem.displayName and
        // DESCRIPTION is NoteItem.body
        if(note!=null) {
            setSummary(note.getDisplayName());
            setDescription(note.getBody());
        }
    }
    
    /**
     * Return EventExceptionStamp from Item
     * @param item
     * @return EventExceptionStamp from Item
     */
    public static EventExceptionStamp getStamp(Item item) {
        return (EventExceptionStamp) item.getStamp(EventExceptionStamp.class);
    }
    
    /* (non-Javadoc)
     * @see org.osaf.cosmo.model.Stamp#copy()
     */
    public Stamp copy(Item item) {
        EventExceptionStamp stamp = new EventExceptionStamp(item);
        
        // Need to copy Calendar
        stamp.setExceptionCalendar(CalendarUtils.copyCalendar(calendar));
        
        return stamp;
    }

    @Override
    public void remove() {
        super.remove();
    }
}
