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
package org.osaf.cosmo.jcr;

/**
 * Provides constants for JCR items, node types,  etc. implemented by
 * Cosmo.
 */
public class CosmoJcrConstants {

    // node names
    public static final String NN_JCR_CONTENT = "jcr:content";
    public static final String NN_JCR_DATA = "jcr:data";
    public static final String NN_JCR_MIMETYPE = "jcr:mimeType";
    public static final String NN_JCR_ENCODING = "jcr:encoding";
    public static final String NN_JCR_LASTMODIFIED = "jcr:lastModified";

    public static final String NN_TICKET = "ticket:ticket";

    // node types

    public static final String NT_FOLDER = "nt:folder";
    public static final String NT_RESOURCE = "nt:resource";

    public static final String NT_DAV_COLLECTION = "dav:collection";
    public static final String NT_DAV_RESOURCE = "dav:resource";

    public static final String NT_TICKETABLE = "mix:ticketable";
    public static final String NT_TICKET = "ticket:ticket";

    public static final String NT_CALDAV_HOME = "caldav:home";
    public static final String NT_CALDAV_COLLECTION = "caldav:collection";
    public static final String NT_CALDAV_RESOURCE = "caldav:resource";

    // node properties

    public static final String NP_JCR_DATA = "jcr:data";
    public static final String NP_JCR_CREATED = "jcr:created";
    public static final String NP_JCR_LASTMODIFIED = "jcr:lastModified";

    public static final String NP_XML_LANG = "xml:lang";

    public static final String NP_DAV_DISPLAYNAME = "dav:displayname";
    public static final String NP_DAV_CONTENTLANGUAGE =
        "dav:contentlanguage";

    public static final String NP_CALDAV_CALENDARDESCRIPTION =
        "caldav:calendar-description";
    public static final String NP_CALDAV_UID =
        "caldav:uid";

    public static final String NP_ID = "ticket:id";
    public static final String NP_OWNER = "ticket:owner";
    public static final String NP_TIMEOUT = "ticket:timeout";
    public static final String NP_PRIVILEGES = "ticket:privileges";
    public static final String NP_CREATED = "ticket:created";
}
