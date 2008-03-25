/*
 * Copyright 2007-2008 Open Source Applications Foundation
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
package org.osaf.cosmo.model.mock;

import java.io.IOException;
import java.io.InputStream;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

import org.osaf.cosmo.calendar.util.CalendarUtils;
import org.osaf.cosmo.model.Attribute;
import org.osaf.cosmo.model.ICalendarAttribute;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ModelValidationException;
import org.osaf.cosmo.model.QName;

/**
 * Represents an attribute with a net.fortuna.ical4j.model.Calendar value.
 */
public class MockICalendarAttribute extends MockAttribute implements
        java.io.Serializable, ICalendarAttribute {

    private Calendar value;

    /** default constructor */
    public MockICalendarAttribute() {
    }

    /**
     * @param qname qualified name
     * @param value initial value
     */
    public MockICalendarAttribute(QName qname, Calendar value) {
        setQName(qname);
        this.value = value;
    }
    
    /**
     * @param qname qualified name
     * @param value calendar
     */
    public MockICalendarAttribute(QName qname, String value) {
        setQName(qname);
        setValue(value);
    }

    /* (non-Javadoc)
     * @see org.osaf.cosmo.model.copy.InterfaceICalendarAttribute#getValue()
     */
    public Calendar getValue() {
        return this.value;
    }

    /* (non-Javadoc)
     * @see org.osaf.cosmo.model.copy.InterfaceICalendarAttribute#setValue(net.fortuna.ical4j.model.Calendar)
     */
    public void setValue(Calendar value) {
        this.value = value;
    }
    
    public void setValue(Object value) {
        if (value != null && !(value instanceof Calendar)
                && !(value instanceof String) 
                && !(value instanceof InputStream))
            throw new ModelValidationException(
                    "attempted to set non Calendar value on attribute");
        
        if(value instanceof Calendar)
            setValue((Calendar) value);
        else if(value instanceof InputStream)
            setValue((InputStream) value);
        else
            setValue((String) value);
    }
    
    /* (non-Javadoc)
     * @see org.osaf.cosmo.model.copy.InterfaceICalendarAttribute#setValue(java.lang.String)
     */
    public void setValue(String value) {
        try {
            this.value = CalendarUtils.parseCalendar(value);
        } catch (ParserException e) {
            throw new ModelValidationException("invalid calendar: " + value);
        } catch (IOException ioe) {
            throw new ModelValidationException("error parsing calendar");
        }
    }
    
    /* (non-Javadoc)
     * @see org.osaf.cosmo.model.copy.InterfaceICalendarAttribute#setValue(java.io.InputStream)
     */
    public void setValue(InputStream is) {
        try {
            this.value = CalendarUtils.parseCalendar(is);
        } catch (ParserException e) {
            throw new ModelValidationException("invalid calendar: "
                    + e.getMessage());
        } catch (IOException ioe) {
            throw new ModelValidationException("error parsing calendar: "
                    + ioe.getMessage());
        }
    }
    
    public Attribute copy() {
        ICalendarAttribute attr = new MockICalendarAttribute();
        attr.setQName(getQName().copy());
        if(attr!=null) {
            try {
                attr.setValue(new Calendar(value));
            } catch (Exception e) {
                throw new RuntimeException("Error copying ICalendar attribute");
            }
        }
        return attr;
    }
    
    /**
     * Convienence method for returning a Calendar value on 
     * a ICalendarAttribute with a given QName stored on the given item.
     * @param item item to fetch ICalendarAttribute from
     * @param qname QName of attribute
     * @return Date value of ICalendarAttribute
     */
    public static Calendar getValue(Item item, QName qname) {
        ICalendarAttribute attr = (ICalendarAttribute) item.getAttribute(qname);
        if(attr==null)
            return null;
        else
            return attr.getValue();
    }
    
    /**
     * Convienence method for setting a Calendar value on a 
     * ICalendarpAttribute with a given QName stored on the given item.
     * @param item item to fetch ICalendarpAttribute from
     * @param qname QName of attribute
     * @param value value to set on ICalendarpAttribute
     */
    public static void setValue(Item item, QName qname, Calendar value) {
        ICalendarAttribute attr = (ICalendarAttribute) item.getAttribute(qname);
        if(attr==null && value!=null) {
            attr = new MockICalendarAttribute(qname,value);
            item.addAttribute(attr);
            return;
        }
        if(value==null)
            item.removeAttribute(qname);
        else
            attr.setValue(value);
    }
}
