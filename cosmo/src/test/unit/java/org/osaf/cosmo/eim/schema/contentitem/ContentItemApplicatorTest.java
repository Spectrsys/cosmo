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
package org.osaf.cosmo.eim.schema.contentitem;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.eim.DecimalField;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.TextField;
import org.osaf.cosmo.eim.schema.BaseApplicatorTestCase;
import org.osaf.cosmo.model.Attribute;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.QName;
import org.osaf.cosmo.model.TriageStatus;

/**
 * Test Case for {@link ContentItemApplicator}.
 */
public class ContentItemApplicatorTest extends BaseApplicatorTestCase
    implements ContentItemConstants {
    private static final Log log =
        LogFactory.getLog(ContentItemApplicatorTest.class);

    public void testApplyField() throws Exception {
        ContentItem contentItem = new ContentItem();

        EimRecord record = makeTestRecord();

        ContentItemApplicator applicator =
            new ContentItemApplicator(contentItem);
        applicator.applyRecord(record);

        checkTextValue(record.getFields().get(0),
                       contentItem.getDisplayName());
        checkTextValue(record.getFields().get(1),
                       TriageStatusUtil.format(contentItem.getTriageStatus()));
        checkTextValue(record.getFields().get(2),
                       contentItem.getLastModifiedBy());
        checkTimeStampValue(record.getFields().get(3),
                            contentItem.getClientCreationDate());
        checkUnknownValue(record.getFields().get(4), contentItem);
    }

    private EimRecord makeTestRecord() {
        EimRecord record = new EimRecord(PREFIX_ITEM, NS_ITEM);

        record.addField(new TextField(FIELD_TITLE, "The Bangs"));

        TriageStatus ts = new TriageStatus();
        ts.setCode(TriageStatus.CODE_DONE);
        ts.setUpdated(new Date(System.currentTimeMillis()));
        ts.setAutoTriage(Boolean.TRUE);
        record.addField(new TextField(FIELD_TRIAGE_STATUS,
                                      TriageStatusUtil.format(ts)));

        record.addField(new TextField(FIELD_LAST_MODIFIED_BY,
                                      "bcm@osafoundation.org"));
        BigDecimal createdOn =
            new BigDecimal(Calendar.getInstance().getTime().getTime());
        record.addField(new DecimalField(FIELD_CREATED_ON, createdOn));
        record.addField(new TextField("Phish", "The Lizzards"));

        return record;
    }
}
