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
package org.osaf.cosmo.dav.caldav;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.osaf.cosmo.dav.PreconditionFailedException;
import org.osaf.cosmo.api.CaldavConstants;
import org.osaf.cosmo.api.ICalendarConstants;

/**
 * An exception indicating that an unsuppported collation 
 * was specified.
 */
public class SupportedCollationException extends PreconditionFailedException {
    
    public SupportedCollationException() {
        super("Collation must be one of " + StringUtils.join(ICalendarConstants.SUPPORTED_COLLATIONS, ", "));
        getNamespaceContext().addNamespace(CaldavConstants.PRE_CALDAV, CaldavConstants.NS_CALDAV);
    }

    protected void writeContent(XMLStreamWriter writer)
        throws XMLStreamException {
        writer.writeStartElement(CaldavConstants.NS_CALDAV, "supported-collation");
        writer.writeCharacters(getMessage());
        writer.writeEndElement();
    }
}
