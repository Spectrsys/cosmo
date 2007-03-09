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
package org.osaf.cosmo.eim.schema.modifiedby;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.eim.DecimalField;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.TextField;
import org.osaf.cosmo.eim.schema.BaseApplicatorTestCase;
import org.osaf.cosmo.model.ContentItem;

/**
 * Test Case for {@link ModifiedByApplicator}.
 */
public class ModifiedByApplicatorTest extends BaseApplicatorTestCase
    implements ModifiedByConstants {
    private static final Log log =
        LogFactory.getLog(ModifiedByApplicatorTest.class);

    public void testApplyRecentModification() throws Exception {
        String origUuid = "deadbeef";
        String origLastModifiedBy = "bcm@osafoundation.org";
        Date origModifiedDate = makeDate("2007-01-01T12:00:00");
        ContentItem contentItem = makeTestItem(origUuid,
                                               origLastModifiedBy,
                                               origModifiedDate);

        Date updateDate = makeDate("2007-01-02T12:00:00");
        String updateLastModifiedBy = "bcm@maz.org";
        EimRecord record = makeTestRecord(contentItem.getUid(),
                                          updateDate,
                                          updateLastModifiedBy);

        ModifiedByApplicator applicator =
            new ModifiedByApplicator(contentItem);
        applicator.applyRecord(record);

        assertEquals("uid wrongly modified", origUuid, contentItem.getUid());
        assertEquals("modifiedDate wrongly modified", origModifiedDate,
                     contentItem.getModifiedDate());
        assertEquals("lastModifiedBy not modified", updateLastModifiedBy,
                     contentItem.getLastModifiedBy());
    }

    public void testApplyOldModification() throws Exception {
        String origUuid = "deadbeef";
        String origLastModifiedBy = "bcm@osafoundation.org";
        Date origModifiedDate = makeDate("2007-01-01T12:00:00");
        ContentItem contentItem = makeTestItem(origUuid,
                                               origLastModifiedBy,
                                               origModifiedDate);

        Date updateDate = makeDate("2006-01-02T12:00:00");
        String updateLastModifiedBy = "bcm@maz.org";
        EimRecord record = makeTestRecord(contentItem.getUid(),
                                          updateDate,
                                          updateLastModifiedBy);
        ModifiedByApplicator applicator =
            new ModifiedByApplicator(contentItem);
        applicator.applyRecord(record);

        assertEquals("uid wrongly modified", origUuid, contentItem.getUid());
        assertEquals("modifiedDate wrongly modified", origModifiedDate,
                     contentItem.getModifiedDate());
        assertEquals("lastModifiedBy wrongly modified", origLastModifiedBy,
                     contentItem.getLastModifiedBy());
    }

    public void testApplyDeleted() throws Exception {
        String origUuid = "deadbeef";
        String origLastModifiedBy = "bcm@osafoundation.org";
        Date origModifiedDate = makeDate("2007-01-01T12:00:00");
        ContentItem contentItem = makeTestItem(origUuid,
                                               origLastModifiedBy,
                                               origModifiedDate);

        Date updateDate = makeDate("2006-01-02T12:00:00");
        String updateLastModifiedBy = "bcm@maz.org";
        EimRecord record = makeTestDeletedRecord(origUuid);

        ModifiedByApplicator applicator =
            new ModifiedByApplicator(contentItem);
        applicator.applyRecord(record);

        assertEquals("uid wrongly modified", origUuid, contentItem.getUid());
        assertEquals("modifiedDate wrongly modified", origModifiedDate,
                     contentItem.getModifiedDate());
        assertEquals("lastModifiedBy wrongly modified", origLastModifiedBy,
                     contentItem.getLastModifiedBy());
    }

    private ContentItem makeTestItem(String uuid,
                                     String lastModifiedBy,
                                     Date modifiedDate) {
        ContentItem contentItem = new ContentItem();
        contentItem.setUid(uuid);
        contentItem.setLastModifiedBy(lastModifiedBy);
        contentItem.setModifiedDate(modifiedDate);
        return contentItem;
    }

    private EimRecord makeTestRecord(String uuid,
                                     Date date,
                                     String userid) {
        EimRecord record = new EimRecord(PREFIX_MODIFIEDBY, NS_MODIFIEDBY);
        record.addKeyField(new TextField(FIELD_UUID, uuid));
        record.addKeyField(new DecimalField(FIELD_TIMESTAMP,
                                            makeTimestamp(date),
                                            DEC_TIMESTAMP, DIGITS_TIMESTAMP));
        record.addKeyField(new TextField(FIELD_USERID, userid));
        return record;
    }

    private EimRecord makeTestDeletedRecord(String uuid) {
        EimRecord record = new EimRecord(PREFIX_MODIFIEDBY, NS_MODIFIEDBY);
        record.addKeyField(new TextField(FIELD_UUID, uuid));
        record.setDeleted(true);
        return record;
    }

    private Date makeDate(String formatted)
        throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(formatted);
    }

    private BigDecimal makeTimestamp(Date date) {
        return new BigDecimal(date.getTime() / 1000);
    }
}
