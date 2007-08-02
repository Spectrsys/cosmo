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
package org.osaf.cosmo.dav;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * An unclassified WebDAV Exception.
 */
public class DavException extends org.apache.jackrabbit.webdav.DavException
    implements ExtendedDavConstants {
    private static final DavNamespaceContext DAV_NAMESPACE_CONTEXT =
        DavNamespaceContext.newInstance();

    public DavException(int code) {
        super(code, null, null, null);
    }

    public DavException(Throwable t) {
        super(500, t);
    }

    public void writeTo(XMLStreamWriter writer)
        throws XMLStreamException {
        writer.setNamespaceContext(DAV_NAMESPACE_CONTEXT);
        writer.writeStartElement("DAV:", "error");
        for (String uri : DAV_NAMESPACE_CONTEXT.getNamespaceURIs())
            writer.writeNamespace(DAV_NAMESPACE_CONTEXT.getPrefix(uri), uri);
        writeContent(writer);
        writer.writeEndElement();
    }

    protected void writeContent(XMLStreamWriter writer)
        throws XMLStreamException {
        writer.writeCharacters(getStatusPhrase());
    }

    private static class DavNamespaceContext implements NamespaceContext {
        private static final HashMap<String,String> uris =
            new HashMap<String,String>(2);
        private static final HashMap<String,HashSet<String>> prefixes =
            new HashMap<String,HashSet<String>>(2);

        static {
            uris.put("D", "DAV:");
            uris.put(PRE_COSMO, NS_COSMO);

            HashSet<String> dav = new HashSet<String>(1);
            dav.add("D");
            prefixes.put("DAV:", dav);

            HashSet<String> cosmo = new HashSet<String>(1);
            cosmo.add(PRE_COSMO);
            prefixes.put(NS_COSMO, cosmo);
        }

        public static final DavNamespaceContext newInstance() {
            return new DavNamespaceContext();
        }

        public String getNamespaceURI(String prefix) {
            return uris.get(prefix);
        }

        public String getPrefix(String namespaceURI) {
            return prefixes.get(namespaceURI).iterator().next();
        }

        public Iterator getPrefixes(String namespaceURI) {
            return prefixes.get(namespaceURI).iterator();
        }

        public Set<String> getNamespaceURIs() {
            return prefixes.keySet();
        }
    }
}
