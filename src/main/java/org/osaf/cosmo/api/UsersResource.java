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
package org.osaf.cosmo.api;

import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.jackrabbit.webdav.xml.DomUtil;

import org.osaf.cosmo.model.User;

/**
 * An interface for Cosmo API resources
 */
public class UsersResource implements CosmoApiResource {
    /**
     */
    public static final String EL_USERS = "users";

    private List users;
    private String urlBase;

    /**
     */
    public UsersResource(List users, String urlBase) {
        this.users = users;
        this.urlBase = urlBase;
    }

    /**
     * Returns the <code>List</code> of <code>User</code>s that backs
     * this resource.
     */
    public Object getEntity() {
        return users;
    }

    /**
     * Returns an XML representation of the resource in the form of a
     * {@link org.w3c.dom.Element}.
     *
     * The XML is structured like so:
     *
     * <pre>
     * <users>
     *   <user>
     *     ...
     *   </user>
     * </users>
     * </pre>
     *
     * where the structure of the <code>user</code> element is defined
     * by {@link UserResource}.
     */
    public Element toXml(Document doc) {
        Element e = DomUtil.createElement(doc, EL_USERS, NS_COSMO);
        for (Iterator i=users.iterator(); i.hasNext();) {
            User user = (User) i.next();
            UserResource ur = new UserResource(user, urlBase);
            e.appendChild(ur.toXml(doc));
        }
        return e;
    }
}
