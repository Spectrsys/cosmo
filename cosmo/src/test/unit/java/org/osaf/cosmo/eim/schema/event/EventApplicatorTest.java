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

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.TextField;
import org.osaf.cosmo.eim.schema.BaseApplicatorTestCase;
import org.osaf.cosmo.eim.schema.EimValueConverter;
import org.osaf.cosmo.model.EventExceptionStamp;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.NoteItem;

/**
 * Test Case for {@link EventApplicator}.
 */
public class EventApplicatorTest extends BaseApplicatorTestCase
    implements EventConstants {
    private static final Log log =
        LogFactory.getLog(EventApplicatorTest.class);

    public void testApplyField() throws Exception {
        NoteItem noteItem = new NoteItem();
       
        EimRecord record = makeTestRecord();

        EventApplicator applicator =
            new EventApplicator(noteItem);
        applicator.applyRecord(record);

        EventStamp eventStamp = EventStamp.getStamp(noteItem);
        
        Assert.assertEquals(eventStamp.getLocation(), "here");
        Assert.assertEquals(eventStamp.getStartDate(), EimValueConverter.toICalDate(";VALUE=DATE-TIME:20070212T074500").getDate());
        Assert.assertEquals(eventStamp.getEndDate(), EimValueConverter.toICalDate(";VALUE=DATE-TIME:20070212T084500").getDate());
        Assert.assertEquals(eventStamp.getStatus(), "CONFIRMED");
        Assert.assertEquals(eventStamp.getRecurrenceRules().get(0).toString(), "FREQ=DAILY;UNTIL=20070306T055959Z");
    }
    
    public void testApplyMissingField() throws Exception {
        NoteItem masterNote = new NoteItem();
        EventStamp masterEvent = new EventStamp(masterNote);
        masterEvent.createCalendar();
        masterEvent.setLocation("here");
        masterEvent.setStatus("CONFIRMED");
        masterEvent.setStartDate(EimValueConverter.toICalDate(";VALUE=DATE-TIME:20070210T074500").getDate());
        masterEvent.setEndDate(EimValueConverter.toICalDate(";VALUE=DATE-TIME:20070214T084500").getDate());
        
        masterNote.addStamp(masterEvent);
        
        NoteItem modNote = new NoteItem();
        EventExceptionStamp modEvent = new EventExceptionStamp(modNote);
        modEvent.createCalendar();
        modEvent.setRecurrenceId(EimValueConverter.toICalDate(";VALUE=DATE-TIME:20070212T074500").getDate());
        modEvent.setLocation("blah");
        modEvent.setStatus("blah");
        modNote.setModifies(masterNote);
        modNote.addStamp(modEvent);
       
        EimRecord record = makeTestMissingRecord();

        EventApplicator applicator =
            new EventApplicator(modNote);
        applicator.applyRecord(record);

        Assert.assertEquals("20070212T074500", modEvent.getStartDate().toString());
        Assert.assertNull(modEvent.getDuration());
        Assert.assertNull(modEvent.getAnyTime());
        Assert.assertNull(modEvent.getLocation());
        Assert.assertNull(modEvent.getStatus());
        
        record = makeTestModificationRecord();

        applicator.applyRecord(record);

        Assert.assertEquals("20070213T074500", modEvent.getStartDate().toString());
        Assert.assertFalse(modEvent.getAnyTime());
    }
    
    private EimRecord makeTestRecord() {
        EimRecord record = new EimRecord(PREFIX_EVENT, NS_EVENT);

        record.addField(new TextField(FIELD_DTSTART, ";VALUE=DATE-TIME:20070212T074500"));
        record.addField(new TextField(FIELD_DURATION, "PT1H"));
        record.addField(new TextField(FIELD_LOCATION, "here"));
        record.addField(new TextField(FIELD_RRULE, "FREQ=DAILY;UNTIL=20070306T055959Z"));
        record.addField(new TextField(FIELD_STATUS, "CONFIRMED"));

        return record;
    }
    
    private EimRecord makeTestModificationRecord() {
        EimRecord record = new EimRecord(PREFIX_EVENT, NS_EVENT);

        record.addField(new TextField(FIELD_DTSTART, ";VALUE=DATE-TIME:20070213T074500"));
        record.addField(new TextField(FIELD_DURATION, "PT1H"));
        record.addField(new TextField(FIELD_LOCATION, "here"));
        record.addField(new TextField(FIELD_STATUS, "CONFIRMED"));

        return record;
    }
    
    private EimRecord makeTestMissingRecord() {
        EimRecord record = new EimRecord(PREFIX_EVENT, NS_EVENT);
        addMissingTextField(FIELD_DTSTART, record);
        addMissingTextField(FIELD_DURATION, record);
        addMissingTextField(FIELD_LOCATION, record);
        addMissingIntegerField(FIELD_STATUS, record);
        return record;
    }
    
    
}
