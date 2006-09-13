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
package org.osaf.cosmo.rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.CosmoConstants;
import org.osaf.cosmo.calendar.query.CalendarFilter;
import org.osaf.cosmo.calendar.query.ComponentFilter;
import org.osaf.cosmo.calendar.query.TimeRangeFilter;
import org.osaf.cosmo.model.CalendarCollectionItem;
import org.osaf.cosmo.model.CalendarItem;
import org.osaf.cosmo.model.CalendarEventItem;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.DuplicateItemNameException;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.rpc.model.Calendar;
import org.osaf.cosmo.rpc.model.CosmoToICalendarConverter;
import org.osaf.cosmo.rpc.model.Event;
import org.osaf.cosmo.rpc.model.ICalendarToCosmoConverter;
import org.osaf.cosmo.security.CosmoSecurityManager;
import org.osaf.cosmo.service.ContentService;
import org.osaf.cosmo.service.UserService;
import org.osaf.cosmo.util.ICalendarUtils;
import org.osaf.cosmo.util.PathUtil;

public class RPCServiceImpl implements RPCService {
    private static final Log log =
        LogFactory.getLog(RPCServiceImpl.class);

    private UserService userService = null;
    private ContentService contentService = null;
    private CosmoSecurityManager cosmoSecurityManager = null;
    private CalendarBuilder calendarBuilder = new CalendarBuilder();
    private ICalendarToCosmoConverter icalendarToCosmoConverter = new ICalendarToCosmoConverter();
    private CosmoToICalendarConverter cosmoToICalendarConverter = new CosmoToICalendarConverter();
    
    public CosmoSecurityManager getCosmoSecurityManager() {
        return cosmoSecurityManager;
    }

    public void setCosmoSecurityManager(CosmoSecurityManager cosmoSecurityManager) {
        this.cosmoSecurityManager = cosmoSecurityManager;
    }

    public ContentService getContentService() {
        return contentService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void createCalendar(String displayName, String path)
        throws RPCException {
        String absolutePath = getAbsolutePath(path);
        if (log.isDebugEnabled())
            log.debug("Creating calendar at " + absolutePath);

        String parentPath = PathUtil.getParentPath(absolutePath);
        CollectionItem collection = (CollectionItem)
            contentService.findItemByPath(parentPath);
        if (collection == null)
            throw new RPCException("No collection at " + parentPath);

        CalendarCollectionItem calendar = new CalendarCollectionItem();
        calendar.setName(PathUtil.getBasename(absolutePath));
        calendar.setOwner(getUser());
        // XXX: no Item.displayName yet

        try {
            contentService.createCalendar(collection, calendar);
        } catch (DuplicateItemNameException e) {
            throw new RPCException("Calendar at " + absolutePath + " already exists", e);
        } catch (Exception e) {
            throw new RPCException("Cannot create calendar: " + e.getMessage(), e);
        }
    }

    public Calendar[] getCalendars() throws RPCException {
        if (log.isDebugEnabled())
            log.debug("Getting calendars in home collection of " +
                      getUser().getUsername());
        // XXX no ContentService.findCalendars yet
        HomeCollectionItem home = contentService.getRootItem(getUser());
        List<Calendar> cals = new ArrayList<Calendar>();
        for (Iterator<Item> i=home.getChildren().iterator(); i.hasNext();) {
            Item child = i.next();
            if (! (child instanceof CalendarCollectionItem))
                continue;
            Calendar calendar = new Calendar();
            calendar.setName(child.getName()); // XXX no Item.displayName yet
            calendar.setPath("/" + child.getName());
            cals.add(calendar);
        }
        //TODO sort these
        return (Calendar[]) cals.toArray(new Calendar[cals.size()]);
    }

    public Event getEvent(String calendarPath, String id) throws RPCException {
        String absolutePath = getAbsolutePath(calendarPath);
        if (log.isDebugEnabled())
            log.debug("Getting event " + id + " in calendar at " +
                      absolutePath);
        // XXX: assumes that the given id is the event uid, which the
        // content service assumes to be globally unique, but which
        // should probably only be assumed to be unique within the
        // collection
        CalendarEventItem calItem = contentService.findEventByUid(id);
        try {
            return icalendarToCosmoConverter.createEvent(calItem.getUid(),
                    calItem.getMasterEvent(), calItem.getCalendar());
        } catch (Exception e) {
            log.error("Problem getting event: userName: " + getUsername() 
                    + " calendarName: " + absolutePath
                    + " id: " +id, e);
            throw new RPCException("Problem getting event", e);
        }   
    }

    public Event[] getEvents(String calendarPath, long utcStartTime,
                             long utcEndTime) throws RPCException {
        DateTime start = new DateTime(utcStartTime);
        start.setUtc(true);

        DateTime end = new DateTime(utcEndTime);
        end.setUtc(true);

        ComponentFilter eventFilter = new ComponentFilter(Component.VEVENT);
        eventFilter.setTimeRangeFilter(new TimeRangeFilter(start, end));

        ComponentFilter calFilter =
            new ComponentFilter(net.fortuna.ical4j.model.Calendar.VCALENDAR);
        calFilter.getComponentFilters().add(eventFilter);

        CalendarFilter filter = new CalendarFilter();
        filter.setFilter(calFilter);

        String absolutePath = getAbsolutePath(calendarPath);
        if (log.isDebugEnabled())
            log.debug("Getting events between " + start + " and " + end +
                      " in calendar " + absolutePath);

        // XXX: need ContentService.findEvents(path, filter)
        CalendarCollectionItem collection = (CalendarCollectionItem)
            contentService.findCalendarByPath(absolutePath);
        if (collection == null)
            throw new RPCException("Collection " + absolutePath + " does not exist");

        Set<CalendarEventItem> calendarItems = null;
        try {
            calendarItems = contentService.findEvents(collection, filter);
        } catch (Exception e) {
            log.error("cannot find events for calendar " + absolutePath, e);
            throw new RPCException("Cannot find events for calendar " + absolutePath + ": " + e.getMessage(), e);
        }

        Event[] events = null;
        try {
            DateTime beginDate = new DateTime();
            beginDate.setUtc(true);
            beginDate.setTime(utcStartTime);
            DateTime endDate = new DateTime();
            endDate.setUtc(true);
            endDate.setTime(utcEndTime);

            events = icalendarToCosmoConverter.
                createEventsFromCalendars(calendarItems, beginDate, endDate);
        } catch (Exception e) {
            log.error("Problem getting events: userName: " + getUsername() 
                      + " calendarName: " + absolutePath 
                      + " beginDate: " + utcStartTime
                      + " endDate: " + utcStartTime, e);
            throw new RPCException("Problem getting events", e);
        }
        return events;
    }

    public String getPreference(String preferenceName) throws RPCException {
        if (log.isDebugEnabled())
            log.debug("Getting preference " + preferenceName);
       return userService.getPreference(getUsername(), preferenceName);
    }

    public String getTestString() {
        return "Scooby";
    }

    public String getVersion() {
        return CosmoConstants.PRODUCT_VERSION;
    }

    public void moveEvent(String sourceCalendar, String id,
            String destinationCalendar) throws RPCException {
        //TODO Not implemented yet
    }

    public void removeCalendar(String calendarPath) throws RPCException {
        String absolutePath = getAbsolutePath(calendarPath);
        if (log.isDebugEnabled())
            log.debug("Removing calendar " + absolutePath);
        contentService.removeItem(absolutePath);
    }

    public void removeEvent(String calendarPath, String id)
        throws RPCException {
        String absolutePath = getAbsolutePath(calendarPath);
        if (log.isDebugEnabled())
            log.debug("Removing event " + id + " from calendar " +
                      absolutePath);
        // XXX: assumes that the given id is the event uid, which the
        // content service assumes to be globally unique, but which
        // should probably only be assumed to be unique within the
        // collection
        CalendarEventItem calItem = contentService.findEventByUid(id);
        contentService.removeEvent(calItem);
    }

    public void removePreference(String preferenceName) throws RPCException {
        if (log.isDebugEnabled())
            log.debug("Removing preference " + preferenceName);
        userService.removePreference(getUsername(), preferenceName);
    }

    public String saveEvent(String calendarPath, Event event)
            throws RPCException {

        CalendarEventItem calendarEventItem = null;
        CalendarCollectionItem calendarItem = getCalendarCollectionItem(calendarPath);
        
        //Check to see if this is a new event
        if (StringUtils.isEmpty(event.getId())) {
            //make an empty iCalendar
            net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
            calendar.getProperties().add(new ProdId(CosmoConstants.PRODUCT_ID));
            calendar.getProperties().add(Version.VERSION_2_0);
            calendar.getProperties().add(CalScale.GREGORIAN);
            
            calendarEventItem = new CalendarEventItem();
            VEvent vevent = cosmoToICalendarConverter.createVEvent(event);
            calendar.getComponents().add(vevent);
            calendarEventItem.setContent(calendar.toString().getBytes());

            String name = findAvailableName(calendarPath, vevent);

            calendarEventItem.setName(name);
            calendarEventItem.setOwner(getUser());

            calendarEventItem = contentService.addEvent(calendarItem,
                    calendarEventItem);
        } else {
            calendarEventItem = contentService.findEventByUid(event.getId());
            net.fortuna.ical4j.model.Calendar calendar = calendarEventItem.getCalendar();
            cosmoToICalendarConverter.updateEvent(event, calendar);
            calendarEventItem.setContent(calendar.toString().getBytes());
            contentService.updateEvent(calendarEventItem);
        }

        return calendarEventItem.getUid();
    }

    private String findAvailableName(String calendarPath, VEvent vevent) throws RPCException{
        char[] chars = {'0', '1', '2', '3', '4', '5', '6', '6', '8', '9'};
        StringBuffer baseName = new StringBuffer(vevent.getUid().getValue())
                .append(".ics");
        int index = baseName.length() - 4;
        int count = 0;
        boolean foundAGoodName = false;
        
        while (!foundAGoodName && count < 10) {
            if (count == 1){
                baseName.insert(index, '0');
            } else if (count > 1){
                baseName.setCharAt(index, chars[count]);
            }
            if (contentService.findItemByPath(getAbsolutePath(calendarPath
                    + "/" + baseName.toString())) == null) {
                foundAGoodName = true;
            }
            count++;
        }
        
        if (!foundAGoodName){
            throw new RPCException("Could not find a suitable name for event!");
        }
        
        return baseName.toString();
    }

    public void setPreference(String preferenceName, String value)
            throws RPCException {
        if (log.isDebugEnabled())
            log.debug("Setting preference " + preferenceName + " to " + value);
        userService.setPreference(getUsername(),preferenceName);
    }

    /**
     * Given a path relative to the current user's home directory, returns the 
     * absolute path
     * @param relativePath
     * @return
     */
    private String getAbsolutePath(String relativePath) {
        try {
            StringBuffer s = new StringBuffer("/");
            s.append(getUser().getUsername());
            if (relativePath != null){
                if (! relativePath.startsWith("/"))
                    s.append("/");
                s.append(relativePath);
            }
            return s.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }       
    }
    
    private String getUsername() {
        try {
            return getUser().getUsername();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private User getUser() {
        try {
            return cosmoSecurityManager.getSecurityContext().getUser();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private CalendarCollectionItem getCalendarCollectionItem(String calendarPath) {
        return (CalendarCollectionItem) contentService
                .findItemByPath(getAbsolutePath(calendarPath));
    }

}
