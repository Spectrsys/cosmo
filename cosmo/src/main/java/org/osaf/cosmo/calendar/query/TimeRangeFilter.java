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
package org.osaf.cosmo.calendar.query;

import java.text.ParseException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.osaf.cosmo.dav.caldav.CaldavConstants;
import org.w3c.dom.Element;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.util.Dates;

/**
 * Represents the CALDAV:time-range element. From sec 9.8:
 * 
 * Name: time-range
 * 
 * Namespace: urn:ietf:params:xml:ns:caldav
 * 
 * Purpose: Specifies a time range to limit the set of calendar components
 * returned by the server.
 * 
 * Definition:
 * 
 * <!ELEMENT time-range EMPTY>
 * 
 * <!ATTLIST time-range start CDATA #IMPLIED end CDATA #IMPLIED> 
 * start value: an iCalendar "date with UTC time" 
 * end value: an iCalendar "date with UTC time"
 */
public class TimeRangeFilter implements CaldavConstants {

    private Period period = null;

    private VTimeZone timezone = null;

    private DateTime dstart, dend, fstart, fend;

    public TimeRangeFilter(Period period) {
        setPeriod(period);
    }
    
    /**
     * Construct a TimeRangeFilter object from a DOM Element
     * @param element
     * @throws ParseException
     */
    public TimeRangeFilter(Element element, VTimeZone timezone) throws ParseException {
        // Get start (must be present)
        String start =
            DomUtil.getAttribute(element, ATTR_CALDAV_START, null);
        if (start == null) {
            throw new ParseException("CALDAV:comp-filter time-range requires a start time", -1);
        }

        DateTime trstart = new DateTime(start);
        if (! trstart.isUtc()) {
            throw new ParseException("CALDAV:param-filter timerange start must be UTC", -1);
        }

        // Get end (must be present)
        String end =
            DomUtil.getAttribute(element, ATTR_CALDAV_END, null);
        if (end == null) {
            throw new ParseException("CALDAV:comp-filter time-range requires an end time", -1); 
        }

        DateTime trend = new DateTime(end);
        if (! trend.isUtc()) {
            throw new ParseException("CALDAV:param-filter timerange end must be UTC", -1);
        }

        setPeriod(new Period(trstart, trend));
        setTimezone(timezone);
    }

    public TimeRangeFilter(DateTime dtStart, DateTime dtEnd) {
        if (!dtStart.isUtc()) {
            throw new IllegalArgumentException("timerange start must be UTC");
        }

        if (!dtEnd.isUtc()) {
            throw new IllegalArgumentException("timerange start must be UTC");
        }

        Period period = new Period(dtStart, dtEnd);
        setPeriod(period);
    }

    public TimeRangeFilter(String start, String end)
        throws ParseException {
        this(new DateTime(start), new DateTime(end));
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
        // Get fixed start/end time
        dstart = period.getStart();
        dend = period.getEnd();
        
        // set timezone on floating times
        updateFloatingTimes();
    }

    public String getUTCStart() {
        return dstart.toString();
    }

    public String getUTCEnd() {
        return dend.toString();
    }

    public String getFloatStart() {
        return fstart.toString();
    }

    public String getFloatEnd() {
        return fend.toString();
    }

    public VTimeZone getTimezone() {
        return timezone;
    }

    public void setTimezone(VTimeZone timezone) {
        this.timezone = timezone;
        // update timezone on floating times
        updateFloatingTimes();
    }

    /** */
    public String toString() {
        return new ToStringBuilder(this).
            append("dstart", dstart).
            append("dend", dend).
            append("fstart", fstart).
            append("fend", fend).
            toString();
    }
    
    private void updateFloatingTimes() {
        if(dstart!=null) {
            Value v = dstart instanceof DateTime ?
                Value.DATE_TIME : Value.DATE;
            fstart = (DateTime) Dates.getInstance(dstart, v);
            // if the timezone is null then default system timezone is used
            fstart.setTimeZone((timezone != null) ? new TimeZone(timezone) : null);
        }
        if(dend!=null) {
            Value v = dend instanceof DateTime ?
                Value.DATE_TIME : Value.DATE;
            fend = (DateTime) Dates.getInstance(dend, v);
            // if the timezone is null then default system timezone is used
            fend.setTimeZone((timezone != null) ? new TimeZone(timezone) : null);
        }
    }
}
