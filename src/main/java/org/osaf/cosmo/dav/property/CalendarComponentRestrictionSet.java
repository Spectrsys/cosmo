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

import java.util.Set;
import java.util.HashSet;

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;

import org.osaf.cosmo.dav.CosmoDavConstants;
import org.osaf.cosmo.dav.property.CosmoDavPropertyName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

/**
 * Represents the CalDAV calendar-component-restriction-set
 * property. Valid component types are {@link #VEVENT},
 * {@link #VTODO}, {@link #VJOURNAL}, {@link #VFREEBUSY},
 * {@link #VTIMEZONE}.
 */
public class CalendarComponentRestrictionSet extends AbstractDavProperty {

    /**
     */
    public static final int VEVENT = 0;

    /**
     */
    public static final int VFREEBUSY = 1;

    /**
     */
    public static final int VTIMEZONE = 2;

    private int[] componentTypes;

    /**
     */
    public CalendarComponentRestrictionSet(int[] componentTypes) {
        super(CosmoDavPropertyName.CALENDARCOMPONENTRESTRICTIONSET, true);
        for (int i=0; i<componentTypes.length; i++) {
            if (! isValidComponentType(componentTypes[i])) {
                throw new IllegalArgumentException("Invalid component type '" +
                                                   componentTypes[i] + "'.");
            }
        }
        this.componentTypes = componentTypes;
    }

    /**
     * (Returns a <code>Set</code> of
     * <code>CalendarComponentRestrictionSet.CalendarComponentInfo</code>s
     * for this property.
     */
    public Object getValue() {
        Set infos = new HashSet();
        for (int i=0; i<componentTypes.length; i++) {
            String type = getComponentTypeName(componentTypes[i]);
            infos.add(new CalendarComponentInfo(type));
        }
        return infos;
    }

    /**
     * Returns the component types for this property.
     */
    public int[] getComponentTypes() {
        return componentTypes;
    }

    /**
     */
    public String getComponentTypeName(int componentType) {
        switch (componentType) {
        case VEVENT:
            return "VEVENT";
        case VFREEBUSY:
            return "VFREEBUSY";
        case VTIMEZONE:
            return "VTIMEZONE";
        }
        throw new IllegalArgumentException("Invalid component type '" +
                                           componentType + "'.");
    }

    /**
     * Validates the specified component type.
     */
    public boolean isValidComponentType(int componentType) {
        return componentType >= VEVENT && componentType <= VTIMEZONE;
    }

    /**
     */
    public class CalendarComponentInfo implements XmlSerializable {
        private String type;

        /**
         */
        public CalendarComponentInfo(String type) {
            this.type = type;
        }

        /**
         */
        public Element toXml(Document document) {
            Element elem =
                DomUtil.createElement(document,
                                      CosmoDavConstants.ELEMENT_CALDAV_COMP,
                                      CosmoDavConstants.NAMESPACE_CALDAV);
            DomUtil.setAttribute(elem, CosmoDavConstants.ATTR_CALDAV_NAME,
                                 CosmoDavConstants.NAMESPACE_CALDAV, type);
            return elem;
        }
    }
}
