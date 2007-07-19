/*
 * Copyright 2007Task Open Source Applications Foundation
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
package org.osaf.cosmo.dav.impl;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;

import org.osaf.cosmo.CosmoConstants;
import org.osaf.cosmo.model.TaskStamp;
import org.osaf.cosmo.model.NoteItem;

/**
 * Extends <code>DavCalendarResource</code> to adapt the Cosmo
 * <code>ContentItem</code> with an <code>TaskStamp</code> to 
 * the DAV resource model.
 *
 * This class does not define any live properties.
 *
 * @see DavFile
 */
public class DavTask extends DavCalendarResource {
    private static final Log log = LogFactory.getLog(DavTask.class);

    /** */
    public DavTask(NoteItem item,
                   DavResourceLocator locator,
                   DavResourceFactory factory,
                   DavSession session) {
        super(item, locator, factory, session);
    }
    
    // our methods

    /**
     * <p>
     * Exports the stamp as a calendar object containing a single VTODO.
     * Sets the following properties:
     * </p>
     * <ul>
     * <li>UID: item's icalUid or uid</li>
     * <li>SUMMARY: item's displayName</li>
     * <li>DESCRIPTION: item's body</li>
     * </ul>
     */
    public Calendar getCalendar() {
        NoteItem note = (NoteItem) getItem();

        Calendar cal = new Calendar();
        cal.getProperties().add(new ProdId(CosmoConstants.PRODUCT_ID));
        cal.getProperties().add(Version.VERSION_2_0);
        cal.getProperties().add(CalScale.GREGORIAN);

        VToDo vtodo = new VToDo();

        Uid uid = new Uid();
        if (note.getIcalUid() != null ) {
            uid.setValue(note.getIcalUid());
        } else if (note.getModifies() != null) {
            if (note.getModifies().getIcalUid() != null)
                uid.setValue(note.getModifies().getIcalUid());
            else
                uid.setValue(note.getModifies().getUid());
        }
        if (uid.getValue() == null)
            uid.setValue(note.getUid());
        vtodo.getProperties().add(uid);

        vtodo.getProperties().add(new Summary(note.getDisplayName()));
        vtodo.getProperties().add(new Description(note.getBody()));

        cal.getComponents().add(vtodo);

        return cal;
    }
    
    public TaskStamp getTaskStamp() {
        return TaskStamp.getStamp(getItem());
    }

    public void setCalendar(Calendar cal) {
        throw new UnsupportedOperationException();
    }
}
