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
package org.osaf.cosmo.eim;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.model.CollectionItem;

/**
 * Models an EIM collection record.
 *
 * Note that collection records do not currently have any fields.
 */
public class CollectionRecord extends EimRecord {
    private static final Log log = LogFactory.getLog(CollectionRecord.class);

    /** */
    public CollectionRecord() {
    }

    /** */
    public CollectionRecord(CollectionItem collection) {
        setUuid(collection.getUid());
    }

    /** */
    public void applyTo(CollectionItem collection) {
        if (! collection.getUid().equals(getUuid()))
            throw new IllegalArgumentException("cannot apply record to item with non-matching uuid");
    }
}
