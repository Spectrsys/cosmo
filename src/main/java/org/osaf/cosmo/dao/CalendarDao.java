/*
 * Copyright (c) 2006 SimDesk Technologies, Inc.  All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * SimDesk Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with SimDesk Technologies.
 *
 * SIMDESK TECHNOLOGIES MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT
 * THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT.  SIMDESK TECHNOLOGIES
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package org.osaf.cosmo.dao;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.osaf.cosmo.calendar.query.CalendarFilter;
import org.osaf.cosmo.model.CalendarCollectionItem;
import org.osaf.cosmo.model.CalendarEventItem;
import org.osaf.cosmo.model.CalendarItem;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.DuplicateEventUidException;

/**
 * Interface for DAO that provides base functionality for calendaring items.
 *
 * A calendaring item is either a CalendarItem representing a single calendar
 * item or a CalendarCollectionItem, representing a collection of calendar
 * items. Unlike content items, a CalendarCollection cannot contain other
 * CalendarCollection items.
 *
 */
public interface CalendarDao extends ItemDao {

    /**
     * Create new calendar collection. All calendar collections live under the
     * top-level user collection.
     *
     * @param calendar
     *            calendar collection to create
     * @return newly created calendar collection
     * @throws DuplicateItemNameException
     */
    public CalendarCollectionItem createCalendar(CalendarCollectionItem calendar);


    /**
     * Create a new calendar collection in the specified collection.
     * @param collection collection where calendar will live
     * @param calendar calendar collection to create
     * @return newly created calendar collection
     * @throws DuplicateItemNameException
     */
    public CalendarCollectionItem createCalendar(CollectionItem collection, CalendarCollectionItem calendar);

    /**
     * Update existing calendar collection.
     *
     * @param calendar
     *            calendar collection to update
     * @return updated calendar collection
     * @throws DuplicateItemNameException
     */
    public CalendarCollectionItem updateCalendar(CalendarCollectionItem calendar);

    /**
     * Create new calendar event item.
     *
     * @param calendar
     *            calendar collection that new item will belong to
     * @param event
     *            new calendar event item
     * @return newly created calendar event item
     * @throws ModelValidationException
     * @throws DuplicateItemNameException
     * @throws DuplicateEventUidException
     */
    public CalendarEventItem addEvent(CalendarCollectionItem calendar,
            CalendarEventItem event);


    /**
     * Create a new calendar event item from an existing content item
     * @param content
     * @return newly created calendar event item
     */
    public CalendarEventItem addEvent(ContentItem content);

    /**
     * Update existing calendar event item.
     *
     * @param event
     *            calendar event to update
     * @return updated calendar event
     * @throws ModelValidationException
     * @throws DuplicateItemNameException
     */
    public CalendarEventItem updateEvent(CalendarEventItem event);

    /**
     * Find calendar collection by path. Path is of the format:
     * /username/calendarname
     *
     * @param path
     *            path of calendar
     * @return calendar represented by path
     */
    public CalendarCollectionItem findCalendarByPath(String path);

    /**
     * Find calendar collection by uid.
     *
     * @param uid
     *            uid of calendar
     * @return calendar represented by uid
     */
    public CalendarCollectionItem findCalendarByUid(String uid);

    /**
     * Find calendar event item by path. Path is of the format:
     * /username/calendarname/eventname
     *
     * @param path
     *            path of calendar event to find
     * @return calendar event represented by path
     */
    public CalendarEventItem findEventByPath(String path);

    /**
     * Find calendar event by uid.
     *
     * @param uid
     *            uid of calendar event
     * @return calendar event represented by uid
     */
    public CalendarEventItem findEventByUid(String uid);

    /**
     * Find calendar events by criteria. Events can be searched based on a set
     * of item attribute criteria. Only events that contain attributes with
     * values equal to those specified in the criteria map will be returned.
     *
     * @param calendar
     *            calendar collection to search
     * @param criteria
     *            criteria to search on.
     * @return set of CalendarEventItem objects matching specified
     *         criteria.
     */
    public Set<CalendarEventItem> findEvents(CalendarCollectionItem calendar,
                                             Map criteria);

    /**
     * Find calendar events by filter.
     *
     * @param calendar
     *            calendar collection to search
     * @param filter
     *            filter to use in search
     * @return set CalendarEventItem objects matching specified
     *         filter.
     */
    public Set<CalendarEventItem> findEvents(CalendarCollectionItem calendar,
                                             CalendarFilter filter);

    /**
     * Remove calendar collection.
     *
     * @param calendar
     *            calendar collection to remove.
     */
    public void removeCalendar(CalendarCollectionItem calendar);

    /**
     * Remove calendar event
     *
     * @param event
     *            calendar event to remove
     */
    public void removeEvent(CalendarEventItem event);
}
