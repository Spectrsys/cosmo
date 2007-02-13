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
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.DecimalField;
import org.osaf.cosmo.eim.TextField;
import org.osaf.cosmo.eim.schema.BaseItemGenerator;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generates EIM records from content items.
 *
 * @see ContentItem
 */
public class ContentItemGenerator extends BaseItemGenerator
    implements ContentItemConstants {
    private static final Log log =
        LogFactory.getLog(ContentItemGenerator.class);

    /** */
    public ContentItemGenerator(Item item) {
        super(PREFIX_ITEM, NS_ITEM, item);
        if (! (item instanceof ContentItem))
            throw new IllegalArgumentException("item " + item.getUid() + " not a content item");
    }

    /**
     * Copies contentItem properties and attributes into a contentItem
     * record.
     */
    public List<EimRecord> generateRecords() {
        ContentItem contentItem = (ContentItem) getItem();

        EimRecord record = new EimRecord(getPrefix(), getNamespace());

        record.addKeyField(new TextField(FIELD_UUID, contentItem.getUid()));

        record.addField(new TextField(FIELD_TITLE,
                                      contentItem.getDisplayName()));
        String ts = contentItem.getTriageStatus();
        if (ts != null)
            ts = ts.toLowerCase();
        record.addField(new TextField(FIELD_TRIAGE_STATUS, ts));
        record.addField(new DecimalField(FIELD_TRIAGE_STATUS_CHANGED,
                                         contentItem.getTriageStatusUpdated(),
                                         DIGITS_TIMESTAMP, DEC_TIMESTAMP));
        record.addField(new TextField(FIELD_LAST_MODIFIED_BY,
                                      contentItem.getLastModifiedBy()));
        Date d = contentItem.getClientCreationDate();
        BigDecimal createdOn = d != null ?
            new BigDecimal(d.getTime()) :
            null;
        record.addField(new DecimalField(FIELD_CREATED_ON, createdOn,
                                         DIGITS_TIMESTAMP, DEC_TIMESTAMP));

        record.addFields(generateUnknownFields());

        ArrayList<EimRecord> records = new ArrayList<EimRecord>();
        records.add(record);

        return records;
    }
}
