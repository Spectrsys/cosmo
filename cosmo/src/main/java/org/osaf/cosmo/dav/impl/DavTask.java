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
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;

import org.osaf.cosmo.CosmoConstants;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.DavException;
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
    public DavTask(DavResourceLocator locator,
                  DavResourceFactory factory) {
        this(new NoteItem(), locator, factory);
        getItem().addStamp(new TaskStamp());
    }

    /** */
    public DavTask(NoteItem item,
                   DavResourceLocator locator,
                   DavResourceFactory factory) {
        super(item, locator, factory);
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

    /**
     * <p>
     * Imports a calendar object containing a VTODO. Sets the
     * following properties:
     * </p>
     * <ul>
     * <li>display name: the VTODO's SUMMARY (or the item's name, if the
     * SUMMARY is blank)</li>
     * <li>icalUid: the VTODO's UID</li>
     * <li>body: the VTODO's DESCRIPTION</li>
     * </ul>
     */
    public void setCalendar(Calendar cal)
        throws DavException {
        NoteItem note = (NoteItem) getItem();
        String val = null;

        ComponentList vtodos = cal.getComponents(Component.VTODO);
        if (vtodos.isEmpty())
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "VCALENDAR does not contain any VTODOS");

        VToDo vtodo = (VToDo) vtodos.get(0);

        val = null;
        if (vtodo.getSummary() != null)
            val = StringUtils.substring(vtodo.getSummary().getValue(), 0, 255);
        if (StringUtils.isBlank(val))
            val = note.getName();
        note.setDisplayName(val);

        val = null;
        if (vtodo.getUid() != null)
            val = vtodo.getUid().getValue();
        if (StringUtils.isBlank(val))
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "VTODO does not contain a UID");
        note.setIcalUid(val);

        val = null;
        if (vtodo.getDescription() != null)
            val = vtodo.getDescription().getValue();
        if (val == null)
            val = "";
        note.setBody(val);
    }
}
