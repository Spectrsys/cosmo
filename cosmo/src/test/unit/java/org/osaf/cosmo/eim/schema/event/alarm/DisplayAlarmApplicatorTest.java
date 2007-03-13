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
package org.osaf.cosmo.eim.schema.event.alarm;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.IntegerField;
import org.osaf.cosmo.eim.TextField;
import org.osaf.cosmo.eim.schema.BaseApplicatorTestCase;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.NoteItem;

/**
 * Test Case for {@link DisplayAlarmApplicator}.
 */
public class DisplayAlarmApplicatorTest extends BaseApplicatorTestCase
    implements DisplayAlarmConstants {
    private static final Log log =
        LogFactory.getLog(DisplayAlarmApplicatorTest.class);

    public void testApplyField() throws Exception {
        NoteItem noteItem = new NoteItem();
        EventStamp eventStamp = new EventStamp(noteItem);
        eventStamp.createCalendar();
        noteItem.addStamp(eventStamp);

        EimRecord record = makeTestRecord();

        DisplayAlarmApplicator applicator =
            new DisplayAlarmApplicator(noteItem);
        applicator.applyRecord(record);

        Assert.assertEquals(eventStamp.getDisplayAlarmDescription(), "My alarm");
        Assert.assertEquals(eventStamp.getDisplayAlarmTrigger().getValue(), "PT15M");
        Assert.assertEquals(eventStamp.getDisplayAlarmDuration().toString(), "P1W");
        Assert.assertEquals(eventStamp.getDisplayAlarmRepeat(), new Integer(1));
    }
    
    private EimRecord makeTestRecord() {
        EimRecord record = new EimRecord(PREFIX_DISPLAY_ALARM, NS_DISPLAY_ALARM);

        record.addField(new TextField(FIELD_DESCRIPTION, "My alarm"));
        record.addField(new TextField(FIELD_TRIGGER, "PT15M"));
        record.addField(new TextField(FIELD_DURATION, "P1W"));
        record.addField(new IntegerField(FIELD_REPEAT, 1));

        return record;
    }
}
