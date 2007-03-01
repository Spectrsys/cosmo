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
package org.osaf.cosmo.eim.schema.event.modification;

import net.fortuna.ical4j.model.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.EimRecordKey;
import org.osaf.cosmo.eim.schema.BaseStampApplicator;
import org.osaf.cosmo.eim.schema.EimFieldValidator;
import org.osaf.cosmo.eim.schema.EimSchemaException;
import org.osaf.cosmo.eim.schema.EimValidationException;
import org.osaf.cosmo.eim.schema.EimValueConverter;
import org.osaf.cosmo.eim.schema.ICalDate;
import org.osaf.cosmo.eim.schema.event.EventConstants;
import org.osaf.cosmo.model.EventExceptionStamp;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.Stamp;

/**
 * Applies event modification EIM records to and EventStamp.
 *
 * @see EventStamp
 */
public class EventModificationApplicator extends BaseStampApplicator
    implements EventConstants {
    private static final Log log =
        LogFactory.getLog(EventModificationApplicator.class);

    /** */
    public EventModificationApplicator(Item item) {
        super(PREFIX_EVENT_MODIFICATION, NS_EVENT_MODIFICATION, item);
        setStamp(EventExceptionStamp.getStamp(item));
    }
    
    /**
     * Copies record field values to stamp properties and
     * attributes.
     *
     * @throws EimValidationException if the field value is invalid
     * @throws EimSchemaException if the field is improperly
     * constructed or cannot otherwise be applied to the event 
     */
    protected void applyField(EimRecordField field)
        throws EimSchemaException {
        EventExceptionStamp event = (EventExceptionStamp) getStamp();

        if (field.getName().equals(FIELD_DTSTART)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_DTSTART);
            ICalDate icd = EimValueConverter.toICalDate(value);
            event.setStartDate(icd.getDate());
            event.setAnyTime(icd.isAnyTime());
            event.setDirty(true);
        } else if (field.getName().equals(FIELD_DTEND)) {
            String value = EimFieldValidator.validateText(field, MAXLEN_DTEND);
            event.setEndDate(EimValueConverter.toICalDate(value).getDate());
            event.setDirty(true);
        } else if (field.getName().equals(FIELD_LOCATION)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_LOCATION);
            event.setLocation(value);
            event.setDirty(true);
        } else if (field.getName().equals(FIELD_STATUS)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_STATUS);
            event.setStatus(value);
            event.setDirty(true);
        } else {
            applyUnknownField(field);
            event.setDirty(true);
        }
    }
    
    @Override
    protected Stamp createStamp(EimRecord record) throws EimSchemaException {
        EventExceptionStamp eventStamp = new EventExceptionStamp(getItem());
        // initialize calendar on EventStamp
        eventStamp.createCalendar();
        eventStamp.setRecurrenceId(getRecurrenceId(record));
        return eventStamp;
    }

    private Date getRecurrenceId(EimRecord record) throws EimSchemaException {
        // recurrenceId is a key field
        EimRecordKey key = record.getKey();
        for(EimRecordField keyField: key.getFields()) {
            if(keyField.getName().equals(FIELD_RECURRENCE_ID)) {
                String value = EimFieldValidator.validateText(keyField,
                        MAXLEN_RECURRENCE_ID);
                return EimValueConverter.toICalDate(value).getDate();
            }
        }
        throw new EimSchemaException("key field " + FIELD_RECURRENCE_ID
                + " required");
    }
}
