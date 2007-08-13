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
package org.osaf.cosmo.model;

import net.fortuna.ical4j.model.Date;


/**
 * Represents an occurrence of a recurring NoteItem.
 * A NoteOccurrence occurence consists of the master NoteItem 
 * and an occurrence date.  The uid of a NoteOccurence is
 * a combination of the master NoteItem's uid and the occurrence
 * date.
 */
public class NoteOccurrence extends NoteItem {

    Date occurrenceDate = null;
    NoteItem masterNote = null;
    ModificationUid modUid = null;
    
    public NoteOccurrence(Date occurrenceDate, NoteItem masterNote) {
        // uid is the same as a modification's uid
        this.modUid = new ModificationUid(masterNote, occurrenceDate);
        setUid(modUid.toString());
        
        // set base fields from master
        setModifiedDate(masterNote.getModifiedDate());
        setCreationDate(masterNote.getCreationDate());
        setDisplayName(masterNote.getDisplayName());
        setOwner(masterNote.getOwner());
        this.occurrenceDate = occurrenceDate;
        this.masterNote = masterNote;
    }
   
    /**
     * A NoteOccurrence is an occurrence of a recurring NoteItem.
     * @return the master NoteItem
     */
    public NoteItem getMasterNote() {
        return masterNote;
    }
    
    /**
     * @return date of the occurrence
     */
    public Date getOccurrenceDate() {
        return occurrenceDate;
    }

    /**
    * @return the ModificationUid
     */
    public ModificationUid getModificationUid() {
        return modUid;
    }

    @Override
    public Item copy() {
        return new NoteOccurrence(occurrenceDate, masterNote);
    }
}
