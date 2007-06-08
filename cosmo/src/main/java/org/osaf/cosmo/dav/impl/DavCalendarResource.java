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
package org.osaf.cosmo.dav.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;

import org.osaf.cosmo.calendar.util.CalendarUtils;
import org.osaf.cosmo.dav.io.DavInputContext;
import org.osaf.cosmo.icalendar.ICalendarConstants;
import org.osaf.cosmo.model.CalendarCollectionStamp;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.DataSizeException;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.ModelValidationException;
import org.osaf.cosmo.model.NoteItem;

/**
 * Abstract calendar resource.
 */
public abstract class DavCalendarResource extends DavContent
    implements ICalendarConstants {
    
    private static final Log log =
        LogFactory.getLog(DavCalendarResource.class);
  
    static {
        registerLiveProperty(DavPropertyName.GETCONTENTLENGTH);
        registerLiveProperty(DavPropertyName.GETCONTENTTYPE);
    }

    public DavCalendarResource(NoteItem item, DavResourceLocator locator,
            DavResourceFactory factory, DavSession session) {
        super(item, locator, factory, session);
    }
       
    // DavResource methods

    /** */
    public void move(DavResource destination)
        throws DavException {
        validateDestination(destination);
        super.move(destination);
    }

    /** */
    public void copy(DavResource destination,
                     boolean shallow)
        throws DavException {
        validateDestination(destination);
        super.copy(destination, shallow);
    }

    @Override
    protected void populateItem(InputContext inputContext) throws DavException {
        super.populateItem(inputContext);
        
        Calendar calendar = null;
        
        // get mime type
        String contentType = inputContext.getContentType();
        if(contentType!=null)
            contentType = IOUtil.getMimeType(contentType);
        else
            contentType = IOUtil.MIME_RESOLVER.getMimeType(getItem().getName());
        
        // CALDAV:valid-calendar-data
        if (contentType == null)
            throw new DavException(DavServletResponse.SC_BAD_REQUEST,
                                   "No media type specified for calendar data");
        if (! contentType.equals(ICALENDAR_MEDIA_TYPE))
            throw new DavException(DavServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                                   "Content of type " + contentType + " not supported in calendar collection");
        
        if (inputContext.hasStream()) {  
            try {
                // make sure content length matches what was read
                long contentLength = inputContext.getContentLength();
                long bufferedLength = ((DavInputContext) inputContext)
                        .getBufferedContentLength();

                if (contentLength != IOUtil.UNDEFINED_LENGTH
                        && contentLength != bufferedLength)
                    throw new IOException("Read only " + bufferedLength
                            + " of " + contentLength + " bytes");
                calendar = CalendarUtils.parseCalendar(inputContext.getInputStream());
                calendar.validate(true);
                
            } catch (IOException e) {
                log.error("Cannot read resource content", e);
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot read resource content: " + e.getMessage());
            } catch (DataSizeException e) {
                throw new DavException(DavServletResponse.SC_FORBIDDEN, "Cannot store resource content: " + e.getMessage());
            } 
            // CALDAV:valid-calendar-data
            catch (ValidationException e) {
                throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid calendar object: " + e.getMessage());
            } catch (ParserException e) {
                throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid calendar object: " + e.getMessage());
            } 
        }
        
        setCalendar(calendar);
    }
    
    // our methods

    /**
     * Returns the calendar object associated with this resource.
     */
    public abstract Calendar getCalendar();
    
    /**
     * Set the calendar object associated with this resource.
     * @param calendar calendar object parsed from inputcontext
     */
    protected abstract void setCalendar(Calendar calendar);

    private void validateDestination(DavResource destination)
        throws DavException {
        DavResource destinationCollection = destination.getCollection();

        // XXX: we should allow items to be moved/copied out of
        // calendar collections into regular collections, but they
        // need to be stripped of their calendar-ness
        if (! (destinationCollection instanceof DavCalendarCollection))
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Destination collection not a calendar collection");

        CollectionItem collection = (CollectionItem)
            ((DavResourceBase) destinationCollection).getItem();
        CalendarCollectionStamp calendar = CalendarCollectionStamp.getStamp(collection);
        
        NoteItem item = (NoteItem) getItem();
        EventStamp event = EventStamp.getStamp(item);

        if (log.isDebugEnabled())
            log.debug("validating destination " +
                      destination.getResourcePath());

        // CALDAV:supported-calendar-data
        // had to be of appropriate media type to be stored in the
        // original calendar collection

        // CALDAV:valid-calendar-data
        // same

        // CALDAV:valid-calendar-object-resource
        // same

        // CALDAV:supported-calendar-component
        // destination collection may support different component set
        if (! calendar.supportsCalendar(event.getCalendar()))
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Calendar object does not contain at least one supported component");

        // CALDAV:calendar-collection-location-ok not required here

        // CALDAV:max-resource-size was already taken care of when
        // DavCollection.addMember called DavResourceBase.populateItem
        // on the item, though it returned a 409 rather than a 412

        // XXX CALDAV:min-date-time

        // XXX CALDAV:max-date-time

        // XXX CALDAV:max-instances

        // XXX CALDAV:max-attendees-per-instance
    }
    
    @Override
    /** */
    public void spool(OutputContext outputContext)
        throws IOException {
        if (! exists())
            throw new IllegalStateException("cannot spool a nonexistent resource");

        if (log.isDebugEnabled())
            log.debug("spooling file " + getResourcePath());

        String contentType =
            IOUtil.buildContentType(ICALENDAR_MEDIA_TYPE, "UTF-8");
        outputContext.setContentType(contentType);
  
        // convert Calendar object to String, then to bytes (UTF-8)
        byte[] calendarBytes = getCalendar().toString().getBytes("UTF-8");
        outputContext.setContentLength(calendarBytes.length);
        outputContext.setModificationTime(getModificationTime());
        outputContext.setETag(getETag());
        
        if (! outputContext.hasStream())
            return;

        // spool calendar bytes
        ByteArrayInputStream bois = new ByteArrayInputStream(calendarBytes);
        IOUtil.spool(bois, outputContext.getOutputStream());
    }

    /** */
    protected void loadLiveProperties() {
        super.loadLiveProperties();

        DavPropertySet properties = getProperties();

        try {
            byte[] calendarBytes = getCalendar().toString().getBytes("UTF-8");
            String contentLength = new Integer(calendarBytes.length).toString();
            properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH,
                                                  contentLength));
        } catch (Exception e) {
            throw new RuntimeException("Can't convert calendar", e);
        }

        String contentType =
            IOUtil.buildContentType(ICALENDAR_MEDIA_TYPE, "UTF-8");
        properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE,
                                              contentType));
    }

    /** */
    protected void setLiveProperty(DavProperty property) {
        super.setLiveProperty(property);

        DavPropertyName name = property.getName();
        String value = property.getValue().toString();

        if (name.equals(DavPropertyName.GETCONTENTLENGTH) ||
            name.equals(DavPropertyName.GETCONTENTTYPE))
            throw new ModelValidationException("cannot set property " + name);
    }

    /** */
    protected void removeLiveProperty(DavPropertyName name) {
        super.removeLiveProperty(name);

        if (name.equals(DavPropertyName.GETCONTENTLENGTH) ||
            name.equals(DavPropertyName.GETCONTENTTYPE))
            throw new ModelValidationException("cannot remove property " + name);
    }
}
