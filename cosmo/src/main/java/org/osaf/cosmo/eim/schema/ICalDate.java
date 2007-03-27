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
package org.osaf.cosmo.eim.schema;

import java.text.ParseException;
import java.util.Map.Entry;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.parameter.Value;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.calendar.TimeZoneTranslator;
import org.osaf.cosmo.icalendar.ICalendarConstants;

/**
 * Represents an iCalendar date or datetime property value, or a list
 * of them, with associated parameters like timezone and anytime.x
 */
public class ICalDate implements ICalendarConstants {
    private static final Log log = LogFactory.getLog(ICalDate.class);

    private static final TimeZoneRegistry TIMEZONE_REGISTRY =
        TimeZoneRegistryFactory.getInstance().createRegistry();
    
    private static TimeZoneTranslator tzTranslator = TimeZoneTranslator.getInstance();

    private Value value;
    private TzId tzid;
    private boolean anytime;
    private String text;
    private TimeZone tz;
    private Date date;
    private DateList dates;

    /**
     * Constructs an <code>ICalDate</code> by parsing an EIM text
     * value containing a serialized iCalendar property value with
     * optional parameter list as described in
     * {@link * ICalValueParser}.
     * <p>
     * Only the following parameters are processed. Unknown parameters
     * are logged and ignored.
     * <ul>
     * <li><code>VALUE</code></li>
     * <li><code>TZID</code></li>
     * <li><code>X-OSAF-ANYTIME</code></li>
     *
     * @throws EimConversionException
     * </ul>
     */
    public ICalDate(String text)
        throws EimConversionException {
        ICalValueParser parser = new ICalValueParser(text);
        try {
            parser.parse();
        } catch (ParseException e) {
            throw new EimConversionException("Error parsing iCalendar property value", e);
        }

        text = parser.getValue();

        for (Entry<String, String> entry : parser.getParams().entrySet()) {
            if (entry.getKey().equals("VALUE"))
                parseValue(entry.getValue());
            else if (entry.getKey().equals("TZID"))
                parseTzId(entry.getValue());
            else if (entry.getKey().equals(PARAM_X_OSAF_ANYTIME))
                parseAnyTime(entry.getValue());
            else
                log.warn("Skipping unknown parameter " + entry.getKey());
        }

        if (value == null)
            value = Value.DATE_TIME;
        
        // requires parameters to be set
        parseDates(text);
    }

    /**
     * Constructs an <code>ICalDate</code> from an iCalendar date.
     */
    public ICalDate(Date date) {
        if (date instanceof DateTime) {
            value = Value.DATE_TIME;
            tz = ((DateTime) date).getTimeZone();
            
            // Make sure timezone is Olson.  If the translator can't
            // translate the timezon to an Olson timezone, then
            // the event will essentially be floating.
            if (tz != null) {
                String origId = tz.getID();
                tz = tzTranslator.translateToOlsonTz(tz);
                if(tz==null)
                    log.warn("no Olson timezone found for " + origId);
            }
            
            if(tz != null) {
                String id = tz.getVTimeZone().getProperties().
                    getProperty(Property.TZID).getValue();
                tzid = new TzId(id);
            }
        } else {
            value = Value.DATE;
        }
        this.anytime = false;
        text = date.toString();
        this.date = date;
    }

    /**
     * Constructs an <code>ICalDate</code> from an iCalendar date,
     * optionally setting the anytime property.
     */
    public ICalDate(Date date,
                    boolean anytime) {
        this(date);
        this.anytime = anytime;
    }

    /**
     * Constructs an <code>ICalDate</code> from an iCalendar date
     * list. Date lists cannot be anytime.
     */
    public ICalDate(DateList dates) {
        value = dates.getType();
        tz = dates.getTimeZone();
        
        // Make sure timezone is Olson.  If the translator can't
        // translate the timezon to an Olson timezone, then
        // the event will essentially be floating.
        if (tz != null) {
            String origId = tz.getID();
            tz = tzTranslator.translateToOlsonTz(tz);
            if(tz==null)
                log.warn("no Olson timezone found for " + origId);
        }
        
        if (tz != null) {
            String id = tz.getVTimeZone().getProperties().
                getProperty(Property.TZID).getValue();
            tzid = new TzId(id);
        }
        text = dates.toString();
        this.dates = dates;
    }

    public boolean isDateTime() {
        return value != null && value.equals(Value.DATE_TIME);
    }

    public boolean isDate() {
        return value != null && value.equals(Value.DATE);
    }

    public boolean isAnyTime() {
        return anytime;
    }

    public Value getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public TzId getTzId() {
        return tzid;
    }

    public TimeZone getTimeZone() {
        return tz;
    }

    public Date getDate() {
        return date;
    }

    public DateTime getDateTime() {
        if (! (date instanceof DateTime))
            return null;
        return (DateTime) date;
    }

    public DateList getDateList() {
        return dates;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(";");
        buf.append(value.toString());
        if (tzid != null)
            buf.append(";").append("TZID=").append(tzid.getValue());
        if (anytime)
            buf.append(";").append(PARAM_X_OSAF_ANYTIME).
                append("=").append(VALUE_TRUE);
        buf.append(":").append(text);
        return buf.toString();
    }

    private void parseValue(String str)
        throws EimConversionException {
        if (str.equals("DATE"))
            value = Value.DATE;
        else if (str.equals("DATE-TIME"))
            value = Value.DATE_TIME;
        else
            throw new EimConversionException("Bad value " + str);
    }

    private void parseTzId(String str)
        throws EimConversionException {
        tzid = new TzId(str);
        tz = TIMEZONE_REGISTRY.getTimeZone(str);
        if (tz == null)
            throw new EimConversionException("Unknown timezone " + str);
    }

    private void parseAnyTime(String str)
        throws EimConversionException {
        anytime = BooleanUtils.toBoolean(str);
    }

    private void parseDates(String str)
        throws EimConversionException {
        String[] strs = str.split(",");
        if (strs.length == 1) {
            try {
                date = isDate() ? new Date(str) : new DateTime(str, tz);
            } catch (ParseException e) {
                throw convertParseException(str, e);
            }
        }

        dates = isDate() ?
            new DateList(Value.DATE) :
            new DateList(Value.DATE_TIME, tz);
        for (String s : strs) {
            try {
                if (isDate())
                    dates.add(new Date(s));
                else
                    dates.add(new DateTime(s, tz));
            } catch (ParseException e) {
                throw convertParseException(s, e);
            }
        }
    }

    private EimConversionException convertParseException(String value,
                                                         Exception e) {
        String msg = isDate() ?
            "Invalid iCalendar date value " :
            "Invalid iCalendar date-time value ";
        return new EimConversionException(msg + value, e);
    }
}
