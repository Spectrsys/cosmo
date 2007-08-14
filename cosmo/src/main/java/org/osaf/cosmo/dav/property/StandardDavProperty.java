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

import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.xml.DomUtil;

import org.osaf.cosmo.dav.ExtendedDavConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * Represents the Cosmo extended DAV cosmo:exclude-free-busy-rollup
 * property.
 */
public class StandardDavProperty extends DefaultDavProperty
    implements ExtendedDavConstants {

    private String lang;

    public StandardDavProperty(DavPropertyName name,
                               Object value) {
        super(name, value);
    }

    public StandardDavProperty(DavPropertyName name,
                               Object value,
                               String lang) {
        this(name, value);
        if (! StringUtils.isBlank(lang))
            this.lang = lang;
    }

    public StandardDavProperty(DefaultDavProperty orig,
                               String lang) {
        super(orig.getName(), orig.getValue(), orig.isProtected());
        if (! StringUtils.isBlank(lang))
            this.lang = lang;
    }

    public String getLang() {
        return lang;
    }

    public Element toXml(Document document) {
        Element e = super.toXml(document);

        if (lang != null)
            DomUtil.setAttribute(e, XML_LANG, NAMESPACE_XML, lang);

        return e;
    }

    public static StandardDavProperty createFromXml(Element e) {
        DefaultDavProperty orig = DefaultDavProperty.createFromXml(e);
        String lang = DomUtil.getAttribute(e, XML_LANG, NAMESPACE_XML);
        return new StandardDavProperty(orig, lang);
    }
}
