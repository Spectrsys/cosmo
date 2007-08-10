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
package org.osaf.cosmo.dav.caldav.report;

import java.text.ParseException;

import net.fortuna.ical4j.model.component.VTimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.xml.DomUtil;

import org.osaf.cosmo.calendar.query.CalendarFilter;
import org.osaf.cosmo.dav.BadRequestException;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.caldav.TimeZoneExtractor;
import org.osaf.cosmo.dav.impl.DavCalendarCollection;

import org.w3c.dom.Element;

/**
 * Represents the <code>CALDAV:calendar-query</code> report that
 * provides a mechanism for finding calendar resources matching
 * specified criteria. It should be supported by all CalDAV
 * resources. <p/> CalDAV specifies the following required format for
 * the request body:
 *
 * <pre>
 *     &lt;!ELEMENT calendar-query (DAV:allprop | DAV:propname | DAV:prop)?
 *                   filter&gt;
 * </pre>
 */
public class QueryReport extends CaldavMultiStatusReport {
    private static final Log log = LogFactory.getLog(QueryReport.class);

    private VTimeZone tz;

    /** */
    public static final ReportType REPORT_TYPE_CALDAV_QUERY =
        ReportType.register(ELEMENT_CALDAV_CALENDAR_QUERY,
                            NAMESPACE_CALDAV, QueryReport.class);

    // Report methods

    /** */
    public ReportType getType() {
        return REPORT_TYPE_CALDAV_QUERY;
    }

    // CaldavReport methods

    /**
     * Parse property, timezone and filter information from the given
     * report info. Set query filter if the
     * <code>CALDAV:filter</code> property is included.
     */
    protected void parseReport(ReportInfo info)
        throws DavException {
        if (! getType().isRequestedReportType(info))
            throw new BadRequestException("Report not of type " + getType());

        setPropFindProps(info.getPropertyNameSet());
        if (info.containsContentElement(XML_ALLPROP, NAMESPACE)) {
            setPropFindType(PROPFIND_ALL_PROP);
        } else if (info.containsContentElement(XML_PROPNAME, NAMESPACE)) {
            setPropFindType(PROPFIND_PROPERTY_NAMES);
        } else {
            setPropFindType(PROPFIND_BY_PROPERTY);
            setOutputFilter(findOutputFilter(info));
        }

        tz = findTimeZone(info);
        setQueryFilter(findQueryFilter(info));
    }

    private VTimeZone findTimeZone(ReportInfo info)
        throws DavException {
        Element propdata = DomUtil.getChildElement(info.getReportElement(),
                                                   XML_PROP, NAMESPACE);
        if (propdata == null) {
            return null;
        }
        Element tzdata = DomUtil.
            getChildElement(propdata, ELEMENT_CALDAV_TIMEZONE,
                            NAMESPACE_CALDAV);
        if (tzdata == null) {
            return null;
        }
        String icaltz = DomUtil.getTextTrim(tzdata);

        return TimeZoneExtractor.extract(icaltz);
    }

    private CalendarFilter findQueryFilter(ReportInfo info)
        throws DavException {
        Element filterdata =
            DomUtil.getChildElement(info.getReportElement(),
                                    ELEMENT_CALDAV_FILTER, NAMESPACE_CALDAV);
        if (filterdata == null) {
            return null;
        }

        if (tz == null) {
            if (getResource() instanceof DavCalendarCollection) {
                // if no timezone was specified in the report info,
                // fall back to one stored with the calendar
                // collection, if any
                tz = ((DavCalendarCollection) getResource()).getTimeZone();
            }
        }

        try {
            return new CalendarFilter(filterdata, tz);
        } catch (ParseException e) {
            throw new BadRequestException("Calendar filter not parseable: " + e.getMessage());
        }
    }
}
