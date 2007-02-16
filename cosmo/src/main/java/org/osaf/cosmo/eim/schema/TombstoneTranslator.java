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
package org.osaf.cosmo.eim.schema;

import org.apache.commons.lang.BooleanUtils;

import org.osaf.cosmo.eim.EimRecordSet;
import org.osaf.cosmo.model.Tombstone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles the translation of EIM recordsets from a
 * <code>Tombstone</code>.
 *
 * @see EimRecordSet
 * @see Tombstone
 */
public class TombstoneTranslator implements EimSchemaConstants {
    private static final Log log =
        LogFactory.getLog(TombstoneTranslator.class);

    private Tombstone tombstone;

    /** */
    public TombstoneTranslator(Tombstone tombstone) {
        this.tombstone = tombstone;
    }

    /**
     * Generates a deleted recordset from the tombstone.
     */
    public EimRecordSet generateRecordSet() {
        EimRecordSet recordset = new EimRecordSet();
        recordset.setUuid(tombstone.getItemUid());
        recordset.setDeleted(true);

        return recordset;
    }

    /** */
    public Tombstone getTombstone() {
        return tombstone;
    }
}
