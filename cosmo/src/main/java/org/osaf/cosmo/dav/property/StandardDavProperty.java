/*
 * Copyright 2007 Open Source Applications Foundation
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;

import org.osaf.cosmo.dav.ExtendedDavConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Represents the Cosmo extended DAV cosmo:exclude-free-busy-rollup
 * property.
 */
public class StandardDavProperty
    implements DavProperty, XmlSerializable {
    private static final Log log =
        LogFactory.getLog(StandardDavProperty.class);

    private DavPropertyName name;
    private Object value;
    private String lang;
    private boolean isProtected;

    public StandardDavProperty(DavPropertyName name,
                               Object value) {
        this(name, value, null, false);
    }

    public StandardDavProperty(DavPropertyName name,
                               Object value,
                               String lang) {
        this(name, value, lang, false);
    }

    public StandardDavProperty(DavPropertyName name,
                               Object value,
                               boolean isProtected) {
        this(name, value, null, isProtected);
    }

    public StandardDavProperty(DavPropertyName name,
                               Object value,
                               String lang,
                               boolean isProtected) {
        this.name = name;
        this.value = value;
        this.isProtected = isProtected;
        if (! StringUtils.isBlank(lang))
            this.lang = lang;
    }

    // org.apache.jackrabbit.webdav.property.DavProperty methods

    public DavPropertyName getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public boolean isProtected() {
        return isProtected;
    }

    // DavProperty methods

    public String getLanguage() {
        return lang;
    }

    public String getValueText() {
        if (value == null)
            return null;
        if (value instanceof Element) {
            String text = DomUtil.getText((Element) value);
            if (text != null)
                return text;
        }
        return value.toString();
    }

    // XmlSerializable methods

    public Element toXml(Document document) {
        Element e = null;

        if (value != null && value instanceof Element)
            e = (Element) document.importNode((Element) value, true);
        else {
            e = getName().toXml(document);
            Object v = getValue();
            if (v != null) {
                if (v instanceof XmlSerializable)
                    e.appendChild(((XmlSerializable)v).toXml(document));
                else
                    DomUtil.setText(e, v.toString());
            }
        }

        if (lang != null)
            DomUtil.setAttribute(e, XML_LANG, NAMESPACE_XML, lang);

        return e;
    }

    // our methods

    public int hashCode() {
        int hashCode = getName().hashCode();
        if (getValue() != null)
            hashCode += getValue().hashCode();
        return hashCode % Integer.MAX_VALUE;
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof DavProperty))
            return false;
        DavProperty prop = (DavProperty) obj;
        if (! getName().equals(prop.getName()))
            return false;
        return getValue() == null ? prop.getValue() == null :
            value.equals(prop.getValue());
    }

    public static StandardDavProperty createFromXml(Element e) {
        DavPropertyName name = DavPropertyName.createFromXml(e);
        String lang = DomUtil.getAttribute(e, XML_LANG, NAMESPACE_XML);
        if (lang == null && e.getParentNode() != null &&
            e.getParentNode().getNodeType() == Node.ELEMENT_NODE)
            lang = DomUtil.getAttribute((Element)e.getParentNode(), XML_LANG,
                                        NAMESPACE_XML);
        return new StandardDavProperty(name, e, lang);
    }
}
