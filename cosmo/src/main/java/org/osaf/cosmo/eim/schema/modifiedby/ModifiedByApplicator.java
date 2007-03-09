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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.schema.BaseItemApplicator;
import org.osaf.cosmo.eim.schema.EimFieldValidator;
import org.osaf.cosmo.eim.schema.EimSchemaException;
import org.osaf.cosmo.eim.schema.EimValidationException;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;

/**
 * Applies modifiedBy records to content items.
 *
 * Modifiedby is a special type of record that is not processed in the
 * standard field-per-attribute fashion. It represents the fact that a
 * user made a change to an item at some point in the past and is used
 * to update a content item's lastModifiedBy property if the record
 * represents a change that was made subsequent to the last stored
 * change. In other words, if a modifiedby record shows that a change
 * was made at point X in time, but we have already stored updates
 * made after X in time, then there is no need to apply the record.
 *
 * Deleted modifiedby records are used by the client and may be sent
 * to the server. They are not useful to the server and are ignored.
 * 
 * @see ContentItem
 */
public class ModifiedByApplicator extends BaseItemApplicator
    implements ModifiedByConstants {
    private static final Log log =
        LogFactory.getLog(ModifiedByApplicator.class);

    private Date timestamp;
    private String userid;

    /** */
    public ModifiedByApplicator(Item item) {
        super(PREFIX_MODIFIEDBY, NS_MODIFIEDBY, item);
        if (! (item instanceof ContentItem))
            throw new IllegalArgumentException("item " + item.getUid() + " not a content item");
    }

    /**
     * After all fields have been applied, updates the content item's
     * lastModifiedBy property with the record's userid if the
     * record's timestamp is more recent than the content item's last
     * modified timestamp.
     */
    public void applyRecord(EimRecord record)
        throws EimSchemaException {
        super.applyRecord(record);

        if (timestamp == null)
            throw new EimSchemaException("No timestamp provided");
        if (userid == null)
            throw new EimSchemaException("no userid provided");

        ContentItem contentItem = (ContentItem) getItem();
        if (timestamp.after(contentItem.getModifiedDate()))
            contentItem.setLastModifiedBy(userid);
    }

    /**
     * Stores the timestamp and userid from the record for later
     * processing.
     *
     * @throws EimValidationException if the field value is invalid
     * @throws EimSchemaException if the field is improperly
     * constructed or cannot otherwise be applied to the contentItem 
     */
    protected void applyField(EimRecordField field)
        throws EimSchemaException {
        ContentItem contentItem = (ContentItem) getItem();

        if (field.getName().equals(FIELD_TIMESTAMP))
            timestamp = EimFieldValidator.validateTimeStamp(field);
        else if (field.getName().equals(FIELD_USERID))
            userid = EimFieldValidator.validateText(field, MAXLEN_USERID);
        else
            throw new EimSchemaException("Unknown field " + field.getName());
    }

    /**
     * Handles deleted records by ignoring them.
     */
    protected void applyDeletion(EimRecord record)
        throws EimSchemaException {
        // do nothing for deleted records
    }
}
