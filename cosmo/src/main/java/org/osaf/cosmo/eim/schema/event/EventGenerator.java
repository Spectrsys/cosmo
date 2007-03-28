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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.TextField;
import org.osaf.cosmo.eim.schema.BaseStampGenerator;
import org.osaf.cosmo.eim.schema.EimValueConverter;
import org.osaf.cosmo.eim.schema.text.DurationFormat;
import org.osaf.cosmo.model.BaseEventStamp;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.Stamp;

/**
 * Generates EIM records from event stamps.
 *
 * @see BaseEventStamp
 */
public class EventGenerator extends BaseStampGenerator
    implements EventConstants {
    private static final Log log =
        LogFactory.getLog(EventGenerator.class);

    private static final HashSet<String> STAMP_TYPES = new HashSet<String>(2);
    
    static {
        STAMP_TYPES.add("event");
        STAMP_TYPES.add("eventexception");
    }
   
    /** */
    public EventGenerator(Item item) {
        super(PREFIX_EVENT, NS_EVENT, item);
        setStamp(BaseEventStamp.getStamp(item));
    }
    
    @Override
    protected Set<String> getStampTypes() {
        return STAMP_TYPES;
    }

    /**
     * Adds records representing the master event and its display
     * alarm (if one exists).
     */
    protected void addRecords(List<EimRecord> records) {
        BaseEventStamp stamp = (BaseEventStamp) getStamp();
        if (stamp == null)
            return;

        EimRecord record = new EimRecord(getPrefix(), getNamespace());
        addKeyFields(record);
        addFields(record);
        records.add(record);
    }
    
    /**
     * Adds a key field for uuid.
     */
    protected void addKeyFields(EimRecord record) {
        record.addKeyField(new TextField(FIELD_UUID, getItem().getUid()));
    }

    private void addFields(EimRecord record) {
        BaseEventStamp stamp = (BaseEventStamp) getStamp();

        String value = null;

        if(isMissingAttribute("startDate") && isMissingAttribute("anyTime")) {
            record.addField(generateMissingField(new TextField(FIELD_DTSTART, null)));
        } else {
            value = EimValueConverter.fromICalDate(stamp.getStartDate(),
                                                   stamp.isAnyTime());
            record.addField(new TextField(FIELD_DTSTART, value));
        }
         
        if(isMissingAttribute("duration")) {
            record.addField(generateMissingField(new TextField(FIELD_DURATION, null)));
        } else {
            value = DurationFormat.getInstance().format(stamp.getDuration());
            record.addField(new TextField(FIELD_DURATION, value));
        }
        
        if(isMissingAttribute("location")) {
            record.addField(generateMissingField(new TextField(FIELD_LOCATION, null)));
        } else {
            record.addField(new TextField(FIELD_LOCATION, stamp.getLocation()));
        }
        
        value = EimValueConverter.fromICalRecurs(stamp.getRecurrenceRules());
        record.addField(new TextField(FIELD_RRULE, value));

        value = EimValueConverter.fromICalRecurs(stamp.getExceptionRules());
        record.addField(new TextField(FIELD_EXRULE, value));

        value = EimValueConverter.fromICalDates(stamp.getRecurrenceDates());
        record.addField(new TextField(FIELD_RDATE, value));

        value = EimValueConverter.fromICalDates(stamp.getExceptionDates());
        record.addField(new TextField(FIELD_EXDATE, value));

        if(isMissingAttribute("status")) {
            record.addField(generateMissingField(new TextField(FIELD_STATUS, null)));
        } else {
            record.addField(new TextField(FIELD_STATUS, stamp.getStatus()));
        }
        
        record.addFields(generateUnknownFields());
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
