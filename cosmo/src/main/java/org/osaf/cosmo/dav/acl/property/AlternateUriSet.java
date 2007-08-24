/*
 * Copyright 2005-2007 Open Source Applications Foundation
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
package org.osaf.cosmo.dav.acl.property;

import java.util.HashSet;

import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;

import org.osaf.cosmo.dav.DavResourceLocator;
import org.osaf.cosmo.dav.acl.AclConstants;
import org.osaf.cosmo.dav.property.StandardDavProperty;
import org.osaf.cosmo.model.User;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * Represents the DAV:alternate-URI-set property.
 *
 * This property is protected. The value contains three DAV:href
 * elements specifying the Atom, CMP, dav and web URLs for the principal.
 */
public class AlternateUriSet extends StandardDavProperty
    implements AclConstants {

    private DavResourceLocator locator;
    private User user;

    public AlternateUriSet(DavResourceLocator locator,
                           User user) {
        super(ALTERNATEURISET, null, true);
        this.locator = locator;
        this.user = user;
    }

    public Object getValue() {
        return new AlternateUriSetInfo();
    }

    public class AlternateUriSetInfo implements XmlSerializable {
  
        public Element toXml(Document document) {
            HashSet<String> uris = new HashSet<String>();
            for (String uri : locator.getServiceLocator().getUserUrls(user).
                             values())
                uris.add(uri);
    
            Element set =
                DomUtil.createElement(document,
                                      ELEMENT_ACL_ALTERNATE_URI_SET,
                                      NAMESPACE);

            for (String uri : uris) {
                Element e =
                    DomUtil.createElement(document, XML_HREF, NAMESPACE);
                DomUtil.setText(e, uri);
                set.appendChild(e);
            }

            return set;
        }
    }
}
