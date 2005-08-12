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
package org.osaf.cosmo;

/**
 * Constant definitions for Cosmo
 */
public class CosmoConstants {
    /**
     * A string identifier for Cosmo used to distinguish it from other
     * software producs.
     */
    public static final String PRODUCT_ID =
        "-//Open Source Applications Foundation//NONSGML Cosmo Sharing Server//EN";

    /**
     * The version of iCalendar supported by Cosmo: 2.0, as
     * specified by RFC 2445
     */
    public static final String ICALENDAR_VERSION = "2.0";

    /**
     * The content type for iCalendar resources: text/calendar, as
     * specified by RFC 2445
     */
    public static final String ICALENDAR_CONTENT_TYPE = "text/calendar";

    /**
     * The servlet context attribute which contains the Cosmo version
     * number.
     */
    public static final String SC_ATTR_VERSION = "cosmo.version";

    /**
     * The servlet context attribute which contains the Cosmo server
     * administrator's email address.
     */
    public static final String SC_ATTR_SERVER_ADMIN = "cosmo.serverAdmin";
}
