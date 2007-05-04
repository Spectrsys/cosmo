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
package org.osaf.cosmo.mc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.server.ServiceLocator;

/**
 * Provides basic information about the collections in a user's home
 * directory. Represents the Morse Code <em>collection service
 * document</em>.
 *
 * @see CollectionItem
 */
public class CollectionService {
    private static final Log log = LogFactory.getLog(CollectionService.class);
    private static final XMLOutputFactory XML_OUTPUT_FACTORY =
        XMLOutputFactory.newInstance();

    private HashSet<CollectionItem> collections;
    private ServiceLocator locator;

    public CollectionService(HomeCollectionItem home,
                             ServiceLocator locator) {
        this.locator = locator;
        this.collections = new HashSet<CollectionItem>();

        for (Item child : home.getChildren()) {
            if (child instanceof CollectionItem)
                collections.add((CollectionItem)child);
        }
    }

    public Set<CollectionItem> getCollections() {
        return collections;
    }

    public void writeTo(OutputStream out)
        throws IOException, XMLStreamException {
        XMLStreamWriter writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(out);

        try {
            writer.writeStartDocument();
            writer.writeStartElement("service");

            for (CollectionItem collection : collections) {
                writer.writeStartElement("collection");
                writer.writeAttribute("href",
                                      locator.getMorseCodeUrl(collection));

                writer.writeStartElement("name");
                writer.writeCharacters(collection.getDisplayName());
                writer.writeEndElement();

                for (Ticket ticket : collection.getTickets()) {
                    writer.writeStartElement("ticket");
                    writer.writeAttribute("type", ticket.getType().toString());
                    writer.writeCharacters(ticket.getKey());
                    writer.writeEndElement();
                }
            }

            writer.writeEndElement();
            writer.writeEndDocument();
        } finally {
            writer.close();
        }
    }
}
