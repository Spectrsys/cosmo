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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.jackrabbit.webdav.xml.DomUtil;

import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.CosmoSecurityManager;

/**
 * A resource view of a {@link User}.
 */
public class UserResource implements CosmoApiResource {
    /**
     */
    public static final String EL_USER = "user";
    /**
     */
    public static final String EL_USERNAME = "username";
    /**
     */
    public static final String EL_PASSWORD = "password";
    /**
     */
    public static final String EL_FIRSTNAME = "firstName";
    /**
     */
    public static final String EL_LASTNAME = "lastName";
    /**
     */
    public static final String EL_EMAIL = "email";
    /**
     */
    public static final String EL_URL = "url";
    /**
     */
    public static final String EL_HOMEDIRURL = "homedirUrl";

    private User user;
    private String urlBase;
    private String userUrl;
    private String homedirUrl;

    /**
     */
    public UserResource(User user, String urlBase) {
        this.user = user;
        this.urlBase = urlBase;
        calculateUserUrl();
        calculateHomedirUrl();
    }

    /**
     */
    public UserResource(User user, String urlBase, Document doc) {
        this.user = user;
        this.urlBase = urlBase;
        setUserProperties(doc);
        calculateUserUrl();
        calculateHomedirUrl();
    }

    /**
     */
    public UserResource(String urlBase, Document doc) {
        this.user = new User();
        this.urlBase = urlBase;
        setUserProperties(doc);
        calculateUserUrl();
        calculateHomedirUrl();
    }

    // CosmoApiResource methods

    /**
     * Returns the <code>User</code> that backs this resource.
     */
    public Object getEntity() {
        return user;
    }

    /**
     * Returns an XML representation of the resource in the form of a
     * {@link org.w3c.dom.Element}.
     *
     * The XML is structured like so:
     *
     * <pre>
     * <user>
     *   <username>bcm</username>
     *   <firstName>Brian</firstName>
     *   <lastName>Moseley</firstName>
     *   <email>bcm@osafoundation.org</email>
     *   <url>http://localhost:8080/api/user/bcm</url>
     *   <homedirUrl>http://localhost:8080/home/bcm</homedirUrl>
     * </user>
     * </pre>
     *
     * The user's password is not included in the XML representation.
     */
    public Element toXml(Document doc) {
        Element e =  DomUtil.createElement(doc, EL_USER, NS_COSMO);

        Element username = DomUtil.createElement(doc, EL_USERNAME, NS_COSMO);
        DomUtil.setText(username, user.getUsername());
        e.appendChild(username);

        Element firstName = DomUtil.createElement(doc, EL_FIRSTNAME, NS_COSMO);
        DomUtil.setText(firstName, user.getFirstName());
        e.appendChild(firstName);

        Element lastName = DomUtil.createElement(doc, EL_LASTNAME, NS_COSMO);
        DomUtil.setText(lastName, user.getLastName());
        e.appendChild(lastName);

        Element email = DomUtil.createElement(doc, EL_EMAIL, NS_COSMO);
        DomUtil.setText(email, user.getEmail());
        e.appendChild(email);

        Element url = DomUtil.createElement(doc, EL_URL, NS_COSMO);
        DomUtil.setText(url, userUrl);
        e.appendChild(url);

        if (! user.getUsername().equals(CosmoSecurityManager.USER_ROOT)) {
            Element hurl = DomUtil.createElement(doc, EL_HOMEDIRURL, NS_COSMO);
            DomUtil.setText(hurl, homedirUrl);
            e.appendChild(hurl);
        }

        return e;
    }

    // our methods

    /**
     * Returns an entity tag as defined in RFC 2616 that uniqely
     * identifies the state of the <code>User</code> backing this
     * resource.
     */
    public String getEntityTag() {
        if (user == null) {
            return "";
        }
        return "\"" + user.hashCode() + "-" +
            user.getDateModified().getTime() + "\"";
    }

    /**
     * Just as {@link #getEntity}, except the returned object is cast
     * to <code>User</code>.
     */
    public User getUser() {
        return (User) getEntity();
    }

    /**
     */
    public String getUserUrl() {
        return userUrl;
    }

    /**
     */
    public String getHomedirUrl() {
        return homedirUrl;
    }

    /**
     */
    protected void setUserProperties(Document doc) {
        if (doc == null) {
            return;
        }

        Element root = doc.getDocumentElement();
        if (! DomUtil.matches(root, EL_USER, NS_COSMO)) {
            throw new CosmoApiException("root element not user");
        }

        Element e = DomUtil.getChildElement(root, EL_USERNAME, NS_COSMO);
        if (e != null) {
            if (user.getUsername() != null &&
                user.getUsername().equals(CosmoSecurityManager.USER_ROOT)) {
                throw new CosmoApiException("root user's username may not " +
                                            "be changed");
            }
            user.setUsername(DomUtil.getTextTrim(e));
        }

        e = DomUtil.getChildElement(root, EL_PASSWORD, NS_COSMO);
        if (e != null) {
            user.setPassword(DomUtil.getTextTrim(e));
        }

        e = DomUtil.getChildElement(root, EL_FIRSTNAME, NS_COSMO);
        if (e != null) {
            if (user.getUsername() != null &&
                user.getUsername().equals(CosmoSecurityManager.USER_ROOT)) {
                throw new CosmoApiException("root user's first name may not " +
                                            "be changed");
            }
            user.setFirstName(DomUtil.getTextTrim(e));
        }

        e = DomUtil.getChildElement(root, EL_LASTNAME, NS_COSMO);
        if (e != null) {
            if (user.getUsername() != null &&
                user.getUsername().equals(CosmoSecurityManager.USER_ROOT)) {
                throw new CosmoApiException("root user's last name may not " +
                                            "be changed");
            }
            user.setLastName(DomUtil.getTextTrim(e));
        }

        e = DomUtil.getChildElement(root, EL_EMAIL, NS_COSMO);
        if (e != null) {
            user.setEmail(DomUtil.getTextTrim(e));
        }
    }

    /**
     */
    protected void calculateUserUrl() {
        userUrl = urlBase + "/api/user/" + user.getUsername();
    }

    /**
     */
    protected void calculateHomedirUrl() {
        homedirUrl = urlBase + "/home/" + user.getUsername();
    }
}
