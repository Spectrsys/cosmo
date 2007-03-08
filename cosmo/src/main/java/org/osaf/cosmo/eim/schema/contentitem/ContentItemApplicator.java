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

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.schema.BaseItemApplicator;
import org.osaf.cosmo.eim.schema.EimFieldValidator;
import org.osaf.cosmo.eim.schema.EimSchemaException;
import org.osaf.cosmo.eim.schema.EimValidationException;
import org.osaf.cosmo.eim.schema.util.TriageStatusFormat;
import org.osaf.cosmo.model.BaseEventStamp;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.TriageStatus;

/**
 * Applies EIM records to content items.
 *
 * @see ContentItem
 */
public class ContentItemApplicator extends BaseItemApplicator
    implements ContentItemConstants {
    private static final Log log =
        LogFactory.getLog(ContentItemApplicator.class);

    /** */
    public ContentItemApplicator(Item item) {
        super(PREFIX_ITEM, NS_ITEM, item);
        if (! (item instanceof ContentItem))
            throw new IllegalArgumentException("item " + item.getUid() + " not a content item");
    }

    /**
     * Copies record field values to contentItem properties and
     * attributes.
     *
     * @throws EimValidationException if the field value is invalid
     * @throws EimSchemaException if the field is improperly
     * constructed or cannot otherwise be applied to the contentItem 
     */
    protected void applyField(EimRecordField field)
        throws EimSchemaException {
        ContentItem contentItem = (ContentItem) getItem();

        if (field.getName().equals(FIELD_TITLE)) {
            
            if(field.isMissing()) {
                handleMissingAttribute("displayName");
            }
            else {
                String value = EimFieldValidator.validateText(field, MAXLEN_TITLE);
                contentItem.setDisplayName(value);
            }
            
            // ContentItem.displayName == BaseEventStamp.getSummary()
            // For now, we have to keep the ContentItem and
            // EventStamp in sync, otherwise an update by morsecode
            // to ContentItem will not propogate to a CalDAV client.
            BaseEventStamp eventStamp = BaseEventStamp.getStamp(contentItem);
            if(eventStamp!=null) {
                eventStamp.setSummary(contentItem.getDisplayName());
            }
        } else if (field.getName().equals(FIELD_TRIAGE_STATUS)) {
            if(field.isMissing()) {
                handleMissingAttribute("triageStatus");
            } else {
                String value =
                    EimFieldValidator.validateText(field, MAXLEN_TRIAGE_STATUS);
                try {
                    TriageStatus ts =
                        TriageStatusFormat.getInstance().parse(value);
                    contentItem.setTriageStatus(ts);
                } catch (ParseException e) {
                    throw new EimValidationException("Illegal triage status", e);
                }
            }
        } else if (field.getName().equals(FIELD_LAST_MODIFIED_BY)) {
            if(field.isMissing()) {
                handleMissingAttribute("lastModifiedBy");
            }
            else {
                String value =
                    EimFieldValidator.validateText(field, MAXLEN_LAST_MODIFIED_BY);
                contentItem.setLastModifiedBy(value);
            }
        } else if (field.getName().equals(FIELD_CREATED_ON)) {
            if(field.isMissing()) {
                handleMissingAttribute("clientCreationDate");
            } else {
                Date value = EimFieldValidator.validateTimeStamp(field);
                contentItem.setClientCreationDate(value);
            }
        } else {
            applyUnknownField(field);
        }
    }
}
