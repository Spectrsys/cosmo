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
package org.osaf.cosmo.eim.schema.note;

import java.io.Reader;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.schema.BaseItemApplicator;
import org.osaf.cosmo.eim.schema.EimFieldValidator;
import org.osaf.cosmo.eim.schema.EimSchemaException;
import org.osaf.cosmo.eim.schema.EimValidationException;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.NoteItem;

/**
 * Applies EIM records to note items.
 *
 * @see NoteItem
 */
public class NoteApplicator extends BaseItemApplicator
    implements NoteConstants {
    private static final Log log =
        LogFactory.getLog(NoteApplicator.class);

    /** */
    public NoteApplicator(Item item) {
        super(PREFIX_NOTE, NS_NOTE, item);
        if (! (item instanceof NoteItem))
            throw new IllegalArgumentException("item " + item.getUid() + " not a note item");
    }

    /**
     * Copies record field values to note properties and
     * attributes.
     *
     * @throws EimValidationException if the field value is invalid
     * @throws EimSchemaException if the field is improperly
     * constructed or cannot otherwise be applied to the note 
     */
    protected void applyField(EimRecordField field)
        throws EimSchemaException {
        NoteItem note = (NoteItem) getItem();

        if (field.getName().equals(FIELD_BODY)) {
            Reader value = EimFieldValidator.validateClob(field);
            note.setBody(value);

            // NoteItem.body == EventStamp.getDescription()
            // For now, we have to keep the NoteItem and
            // EventStamp in sync, otherwise an update by Chander
            // to NoteItem will not propogate to a CalDAV client.
            EventStamp eventStamp = EventStamp.getStamp(note);
            if(eventStamp!=null)
                eventStamp.setDescription(note.getBody());
        } else if (field.getName().equals(FIELD_ICALUID)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_ICALUID);
            note.setIcalUid(value);
        } else if (field.getName().equals(FIELD_PARENTUUID)) {
            String value =
                EimFieldValidator.validateText(field, MAXLEN_PARENTUUID);
            handleParentUuidField(value, note);
        } else if(field.getName().equals(FIELD_REMINDER_TIME)) {
            Date value = EimFieldValidator.validateTimeStamp(field);
            note.setReminderTime(value);
        } else {
            applyUnknownField(field);
        }
    }
    
    private void handleParentUuidField(String parentUuid, NoteItem note)
            throws EimSchemaException {
        if (parentUuid == null)
            return;

        // We don't support changing the modifies field.  Once its set, its set.
        // It will be present for existing items that are being added to a new
        // collection, so in that case just ignore.
        if(note.getModifies()!=null)
            return;
        
        // Find parent note item by looking through parent's children.
        // This assumes that there will only be a single parent because
        // parentUuid should only be set once, at cration time, thus
        // assuring only a single parent.
        for (Item child : note.getParent().getChildren()) {
            if (child.getUid().equals(parentUuid) && child instanceof NoteItem) {
                note.setModifies((NoteItem) child);
                return;
            }
        }

        // If we didn't find the parent in the collection's children, then we
        // don't know about it, so throw an exception
        throw new EimSchemaException("Unable to find parent for " + parentUuid);
    }
}
