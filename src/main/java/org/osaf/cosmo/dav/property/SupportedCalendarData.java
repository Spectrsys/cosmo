/*
 * Copyright 2005 Open Source Applications Foundation
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
package org.osaf.cosmo.dav.property;

import java.util.HashSet;
import java.util.Set;

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;

import org.osaf.cosmo.dav.CosmoDavConstants;
import org.osaf.cosmo.dav.property.CosmoDavPropertyName;
import org.osaf.cosmo.icalendar.ICalendarConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

/**
 * Represents the CalDAV supported-calendar-data property.
 */
public class SupportedCalendarData extends AbstractDavProperty
    implements ICalendarConstants {

    /**
     */
    public SupportedCalendarData() {
        super(CosmoDavPropertyName.SUPPORTEDCALENDARDATA, true);
    }

    /**
     * (Returns a
     * <code>SupportedCalendarData.SupportedCalendarDataInfo</code>
     * for this property.
     */
    public Object getValue() {
        return new SupportedCalendarDataInfo();
    }

    /**
     */
    public class SupportedCalendarDataInfo implements XmlSerializable {
  
        /**
         */
        public Element toXml(Document document) {
            Element element = DomUtil.
                createElement(document,
                              CosmoDavConstants.ELEMENT_CALDAV_CALENDAR_DATA,
                              CosmoDavConstants.NAMESPACE_CALDAV);
            DomUtil.setAttribute(element,
                                 CosmoDavConstants.ATTR_CALDAV_CONTENT_TYPE,
                                 CosmoDavConstants.NAMESPACE_CALDAV,
                                 CONTENT_TYPE);
            DomUtil.setAttribute(element,
                                 CosmoDavConstants.ATTR_CALDAV_VERSION,
                                 CosmoDavConstants.NAMESPACE_CALDAV,
                                 VERSION);
            return element;
        }
    }
}
