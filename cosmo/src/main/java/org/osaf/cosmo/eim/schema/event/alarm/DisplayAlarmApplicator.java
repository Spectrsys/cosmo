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
package org.osaf.cosmo.eim.schema.event.alarm;

import java.text.ParseException;
import java.util.Iterator;

import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Trigger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.schema.BaseStampApplicator;
import org.osaf.cosmo.eim.schema.EimFieldValidator;
import org.osaf.cosmo.eim.schema.EimSchemaException;
import org.osaf.cosmo.eim.schema.EimValidationException;
import org.osaf.cosmo.eim.schema.EimValueConverter;
import org.osaf.cosmo.eim.schema.text.DurationFormat;
import org.osaf.cosmo.model.BaseEventStamp;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.Stamp;

/**
 * Applies display alarm EIM records to an EventStamp.
 *
 * @see EventStamp
 */
public class DisplayAlarmApplicator extends BaseStampApplicator
    implements DisplayAlarmConstants {
    private static final Log log =
        LogFactory.getLog(DisplayAlarmApplicator.class);

    /** */
    public DisplayAlarmApplicator(Item item) {
        super(PREFIX_DISPLAY_ALARM, NS_DISPLAY_ALARM, item);
        setStamp(BaseEventStamp.getStamp(item));
    }
    
    @Override
    protected void applyDeletion(EimRecord record) throws EimSchemaException {
        VEvent event = getEvent(record);
        
        if(event==null)
            throw new EimSchemaException("no event found for alarm");
        
        VAlarm alarm = getDisplayAlarm(event);
        
        // remove alarm
        if(alarm != null)
            event.getAlarms().remove(alarm);
    }

    @Override
    public void applyRecord(EimRecord record) throws EimSchemaException {
        VEvent event = getEvent(record);
        BaseEventStamp eventStamp = getEventStamp();
        
        if(event==null)
            throw new EimSchemaException("no event found for alarm");
        
        if (record.isDeleted()) {
            applyDeletion(record);
            return;
        }
        
        // create the alarm if its not already there
        getOrCreateDisplayAlarm(event);
            
        for (EimRecordField field : record.getFields()) {
            if(field.getName().equals(FIELD_DESCRIPTION)) {
                if(field.isMissing()) {
                    handleMissingAttribute("displayAlarmDescription");
                }
                else {
                    String value = EimFieldValidator.validateText(field, MAXLEN_DESCRIPTION);
                    eventStamp.setDisplayAlarmDescription(value);
                }
            }
            else if(field.getName().equals(FIELD_TRIGGER)) {
                if(field.isMissing()) {
                    handleMissingAttribute("displayAlarmTrigger");
                }
                else {
                    String value = EimFieldValidator.validateText(field, MAXLEN_TRIGGER);
                    Trigger newTrigger = EimValueConverter.toIcalTrigger(value);
                    eventStamp.setDisplayAlarmTrigger(newTrigger);
                }
            }
            else if (field.getName().equals(FIELD_DURATION)) {
                if(field.isMissing()) {
                    handleMissingAttribute("displayAlarmDuration");
                }
                else {
                    String value = EimFieldValidator.validateText(field, MAXLEN_DURATION);
                    try {
                        Dur dur = DurationFormat.getInstance().parse(value);
                        eventStamp.setDisplayAlarmDuration(dur);
                    } catch (ParseException e) {
                        throw new EimValidationException("Illegal duration", e);
                    }
                }
            }
            else if(field.getName().equals(FIELD_REPEAT)) {
                if(field.isMissing()) {
                    handleMissingAttribute("displayAlarmRepeat");
                }
                else {
                    Integer value = EimFieldValidator.validateInteger(field);
                    eventStamp.setDisplayAlarmRepeat(value);
                }
            }
            else
                log.warn("usupported eim field " + field.getName()
                        + " found in " + record.getNamespace());
        }
    }
    
    @Override
    protected void applyField(EimRecordField field) throws EimSchemaException {
        // do nothing beause we override applyRecord()
    }
    
    @Override
    protected Stamp createStamp(EimRecord record) throws EimSchemaException {
        // do nothing as the stamp should already be created
        return null;
    }
        
   
    /**
     * get the current display alarm, or create a new one
     */
    private VAlarm getOrCreateDisplayAlarm(VEvent event) {
        VAlarm alarm = getDisplayAlarm(event);
        if(alarm==null)
            alarm = creatDisplayAlarm(event);
        return alarm;
    }
    
    /**
     * create new display alarm and add to event
     */
    private VAlarm creatDisplayAlarm(VEvent event) {
        VAlarm alarm = new VAlarm();
        alarm.getProperties().add(Action.DISPLAY);
        event.getAlarms().add(alarm);
        return alarm;
    }
    
    /**
     * Get the first display alarm from an event, or create one
     */
    private VAlarm getDisplayAlarm(VEvent event) {
        VAlarm alarm = null;
        
        // Find the first display alarm
        for(Iterator it = event.getAlarms().iterator();it.hasNext();) {
            VAlarm currAlarm = (VAlarm) it.next();
            if (currAlarm.getProperties().getProperty(Property.ACTION).equals(
                    Action.DISPLAY))
                alarm = currAlarm;
        }
        
        return alarm;
    }
    
    /**
     * Get the event associated with the displayAlarm record.
     */
    private VEvent getEvent(EimRecord record) throws EimSchemaException {
        
        BaseEventStamp eventStamp = getEventStamp();
        
        if(eventStamp==null)
            throw new EimSchemaException("EventStamp required");
        
        return eventStamp.getEvent();
    }
    
    private BaseEventStamp getEventStamp() {
        return BaseEventStamp.getStamp(getItem());
    }
    
    @Override
    protected Stamp getParentStamp() {
        NoteItem noteMod = (NoteItem) getItem();
        NoteItem parentNote = noteMod.getModifies();
        
        if(parentNote!=null)
            return parentNote.getStamp(BaseEventStamp.class);
        else
            return null;
    }
}
