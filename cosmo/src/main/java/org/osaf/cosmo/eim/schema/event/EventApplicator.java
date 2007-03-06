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
package org.osaf.cosmo.eim.schema.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.schema.BaseStampApplicator;
import org.osaf.cosmo.eim.schema.EimFieldValidator;
import org.osaf.cosmo.eim.schema.EimSchemaException;
import org.osaf.cosmo.eim.schema.EimValidationException;
import org.osaf.cosmo.eim.schema.EimValueConverter;
import org.osaf.cosmo.eim.schema.ICalDate;
import org.osaf.cosmo.model.BaseEventStamp;
import org.osaf.cosmo.model.EventExceptionStamp;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.Stamp;

/**
 * Applies EIM records to event stamps.
 *
 * @see EventStamp
 */
public class EventApplicator extends BaseStampApplicator
    implements EventConstants {
    private static final Log log =
        LogFactory.getLog(EventApplicator.class);

    /** */
    public EventApplicator(Item item) {
        super(PREFIX_EVENT, NS_EVENT, item);
        setStamp(EventStamp.getStamp(item));
    }

    /**
     * Creates and returns a stamp instance that can be added by
     * <code>BaseStampApplicator</code> to the item. Used when a
     * stamp record is applied to an item that does not already have
     * that stamp.
     */
    protected Stamp createStamp(EimRecord record) throws EimSchemaException {
        BaseEventStamp eventStamp = null;
        NoteItem note = (NoteItem) getItem();
        
        // Create master event stamp, or event exception stamp
        if(note.getModifies()==null) {
            eventStamp = new EventStamp(getItem());
            eventStamp.createCalendar();
        }
        else {
            eventStamp = new EventExceptionStamp(getItem());
            eventStamp.createCalendar();
            String recurrenceId = note.getUid().split(":")[1];
            eventStamp.setRecurrenceId(EimValueConverter.toICalDate(recurrenceId).getDate());
        }
        
        return eventStamp;
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
        BaseEventStamp event = (BaseEventStamp) getStamp();

        if (field.getName().equals(FIELD_DTSTART)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_DTSTART);
            ICalDate icd = EimValueConverter.toICalDate(value);
            event.setStartDate(icd.getDate());
            event.setAnyTime(icd.isAnyTime());
        } else if (field.getName().equals(FIELD_DTEND)) {
            String value = EimFieldValidator.validateText(field, MAXLEN_DTEND);
            event.setEndDate(EimValueConverter.toICalDate(value).getDate());
        } else if (field.getName().equals(FIELD_LOCATION)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_LOCATION);
            event.setLocation(value);
        } else if (field.getName().equals(FIELD_RRULE)) {
            String value = EimFieldValidator.validateText(field, MAXLEN_RRULE);
            event.setRecurrenceRules(EimValueConverter.toICalRecurs(value));
        } else if (field.getName().equals(FIELD_EXRULE)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_EXRULE);
            event.setExceptionRules(EimValueConverter.toICalRecurs(value));
        } else if (field.getName().equals(FIELD_RDATE)) {
            String value = EimFieldValidator.validateText(field, MAXLEN_RDATE);
            ICalDate icd = EimValueConverter.toICalDate(value);
            event.setRecurrenceDates(icd != null ? icd.getDateList() : null);
        } else if (field.getName().equals(FIELD_EXDATE)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_EXDATE);
            ICalDate icd = EimValueConverter.toICalDate(value);
            event.setExceptionDates(icd != null ? icd.getDateList() : null);
        } else if (field.getName().equals(FIELD_STATUS)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_STATUS);
            event.setStatus(value);
        } else {
            applyUnknownField(field);
        }
    }
}
