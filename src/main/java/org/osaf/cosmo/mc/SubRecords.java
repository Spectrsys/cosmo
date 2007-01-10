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
package org.osaf.cosmo.mc;

import java.util.ArrayList;
import java.util.Iterator;

import org.osaf.cosmo.eim.EimRecordSet;
import org.osaf.cosmo.eim.schema.EimRecordTranslationIterator;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;

/**
 * Bean class that aggregates all of the EIM records for a subscribe
 * response and provides the corresponding synchronization token.
 *
 * @see EimRecord
 * @see SyncToken
 */
public class SubRecords {

    protected SyncToken token;
    private EimRecordTranslationIterator iterator;
    private CollectionItem collection;

    /** */
    public SubRecords(CollectionItem collection) {
        this.token = SyncToken.generate(collection);
        this.iterator = createIterator(collection);
        this.collection = collection;
    }

    /** */
    public Iterator<EimRecordSet> getRecordSets() {
        return iterator;
    }

    /** */
    public SyncToken getToken() {
        return token;
    }

    /** */
    public String getUid() {
        return collection.getUid();
    }

    /** */
    public String getName() {
        return collection.getDisplayName();
    }

    /** */
    protected EimRecordTranslationIterator createIterator(CollectionItem collection) {
        ArrayList<ContentItem> items = new ArrayList<ContentItem>();

        for (Item child : collection.getChildren()) {
            if (! (child instanceof ContentItem))
                continue;
            items.add((ContentItem)child);
        }

        return new EimRecordTranslationIterator(items);
    }
}
