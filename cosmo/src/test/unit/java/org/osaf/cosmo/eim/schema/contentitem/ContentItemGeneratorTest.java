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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.EimRecordKey;
import org.osaf.cosmo.eim.schema.BaseGeneratorTestCase;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.QName;
import org.osaf.cosmo.model.StringAttribute;
import org.osaf.cosmo.model.TriageStatus;

/**
 * Test Case for {@link ContentItemGenerator}.
 */
public class ContentItemGeneratorTest extends BaseGeneratorTestCase
    implements ContentItemConstants {
    private static final Log log =
        LogFactory.getLog(ContentItemGeneratorTest.class);

    public void testGenerateRecord() throws Exception {
        String uid = "deadbeef";
        String name = "3inchesofblood";
        String displayName = "3 Inches of Blood";
        String triageStatusLabel = TriageStatus.LABEL_DONE;
        int triageStatusCode = TriageStatus.CODE_DONE;
        long triageStatusMillis = System.currentTimeMillis();
        Date triageStatusUpdated = new Date(triageStatusMillis);
        Boolean autoTriage = Boolean.TRUE;
        String lastModifiedBy = "bcm@osafoundation.org";
        Date clientCreationDate = Calendar.getInstance().getTime();

        TriageStatus ts = new TriageStatus();
        ts.setCode(triageStatusCode);
        ts.setUpdated(triageStatusUpdated);
        ts.setAutoTriage(autoTriage);

        ContentItem contentItem = new ContentItem();
        contentItem.setUid(uid);
        contentItem.setName(name);
        contentItem.setDisplayName(displayName);
        contentItem.setTriageStatus(ts);
        contentItem.setLastModifiedBy(lastModifiedBy);
        contentItem.setClientCreationDate(clientCreationDate);

        StringAttribute unknownAttr = makeStringAttribute();
        contentItem.addAttribute(unknownAttr);

        ContentItemGenerator generator = new ContentItemGenerator(contentItem);

        List<EimRecord> records = generator.generateRecords();
        assertEquals("unexpected number of records generated", 1,
                     records.size());

        EimRecord record = records.get(0);
        checkNamespace(record, PREFIX_ITEM, NS_ITEM);
        checkUuidKey(record.getKey(), uid);

        List<EimRecordField> fields = record.getFields();
        assertEquals("unexpected number of fields", 5, fields.size());

        EimRecordField titleField = fields.get(0);
        checkTextField(titleField, FIELD_TITLE, displayName);

        EimRecordField triageStatusField = fields.get(1);
        checkTextField(triageStatusField, FIELD_TRIAGE_STATUS,
                       TriageStatusUtil.format(ts));

        EimRecordField lastModifiedByField = fields.get(2);
        checkTextField(lastModifiedByField, FIELD_LAST_MODIFIED_BY,
                       lastModifiedBy);

        EimRecordField createdOnField = fields.get(3);
        checkTimeStampField(createdOnField, FIELD_CREATED_ON,
                            clientCreationDate);

        EimRecordField unknownField = fields.get(4);
        checkTextField(unknownField, unknownAttr.getName(),
                       unknownAttr.getValue());
    }

    public void testInactiveNotDeleted() throws Exception {
        // inactive items are not deleted via item records but via
        // recordset
        ContentItem contentItem = new ContentItem();
        contentItem.setIsActive(false);

        ContentItemGenerator generator = new ContentItemGenerator(contentItem);

        checkNotDeleted(generator.generateRecords().get(0));
    }

    private StringAttribute makeStringAttribute() {
        StringAttribute attr = new StringAttribute();
        attr.setQName(new QName(NS_ITEM, "Blues Traveler"));
        attr.setValue("Sweet talkin' hippie");
        return attr;
    }
}
