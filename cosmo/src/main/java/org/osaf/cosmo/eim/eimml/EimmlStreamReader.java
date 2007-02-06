/*
 * Copyright 2006 Open Source Applications Foundation
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
package org.osaf.cosmo.eim.eimml;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.eim.BlobField;
import org.osaf.cosmo.eim.BytesField;
import org.osaf.cosmo.eim.ClobField;
import org.osaf.cosmo.eim.DateTimeField;
import org.osaf.cosmo.eim.DecimalField;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.EimRecordSet;
import org.osaf.cosmo.eim.IntegerField;
import org.osaf.cosmo.eim.TextField;

/**
 * Provides forward, read-only access to an EIMML stream.
 *
 * This class is designed to iterate over sets of related
 * records. Clients can easily and efficiently retrieve all records in
 * the next set without requiring the rest of the stream to be
 * processed.
 */
public class EimmlStreamReader implements EimmlConstants, XMLStreamConstants {
    private static final Log log = LogFactory.getLog(EimmlStreamReader.class);
    private static final XMLInputFactory XML_INPUT_FACTORY =
        XMLInputFactory.newInstance();

    private XMLStreamReader xmlReader;
    private String documentEncoding;
    private String uuid;
    private String name;

    /**
     * Reads the document header and root element, positioning the
     * cursor just before the first recordset.
     */
    public EimmlStreamReader(InputStream in)
        throws IOException, EimmlStreamException {
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(in);
            if (! xmlReader.hasNext())
                throw new EimmlStreamException("Input stream has no data");

            documentEncoding = xmlReader.getCharacterEncodingScheme();
            if (documentEncoding == null)
                documentEncoding = "UTF-8";

            readCollection();
        } catch (XMLStreamException e) {
            throw new EimmlStreamException("Unable to read EIM records", e);
        }
    }

    /**
     * Returns the uid of the collection.
     */
    public String getCollectionUuid() {
        return uuid;
    }

    /**
     * Returns the name of the collection, if any is specified.
     */
    public String getCollectionName() {
        return name;
    }

    /** */
    public boolean hasNext()
        throws EimmlStreamException {
        try {
            return xmlReader.hasNext();
        } catch (XMLStreamException e) { 
            close();
            throw new EimmlStreamException("Error checking hasNext", e);
        }
    }

    /**
     * Returns the next recordset in the stream. Returns null
     * if there are no more recordsets in the stream.
     */
    public EimRecordSet nextRecordSet()
        throws EimmlStreamException {
        try {
            return readNextRecordSet();
        } catch (XMLStreamException e) {
            close();
            throw new EimmlStreamException("Error reading next recordset", e);
        }
    }

    /** */
    public void close() {
        try {
            xmlReader.close();
        } catch (XMLStreamException e) {
            log.error("Unable to close XML stream", e);
        }
    }

    private int nextTag()
        throws XMLStreamException {
        int rc = xmlReader.nextTag();
        if (log.isDebugEnabled())
            log.debug("read tag: " + xmlReader.getName());
        return rc;
    }

    private void readCollection()
        throws XMLStreamException, EimmlStreamException {
        nextTag();
        if (! xmlReader.isStartElement() &&
            xmlReader.getName().equals(QN_COLLECTION))
            throw new EimmlStreamException("Outermost element must be " + QN_COLLECTION);

        uuid = xmlReader.getAttributeValue(null, ATTR_UUID);

        name = xmlReader.getAttributeValue(null, ATTR_NAME);
        if (StringUtils.isBlank(name))
            name = null;
    }

    private EimRecordSet readNextRecordSet()
        throws EimmlStreamException, XMLStreamException {
        if (! xmlReader.hasNext())
            return null;

        nextTag();
        if (! (xmlReader.isStartElement() &&
               xmlReader.getName().equals(QN_RECORDSET)))
            throw new EimmlStreamException("Expected element " + QN_RECORDSET);

        EimRecordSet recordset = new EimRecordSet();

        String uuid = xmlReader.getAttributeValue(null, ATTR_UUID);
        if (StringUtils.isBlank(uuid))
            throw new EimmlStreamException("Recordset element requires " + ATTR_UUID + " attribute");
        recordset.setUuid(uuid);

        while (xmlReader.hasNext()) {
            nextTag();

            if (xmlReader.isEndElement())
                break;

            if (! (xmlReader.isStartElement() &&
                   xmlReader.getLocalName().equals(EL_RECORD)))
                throw new EimmlStreamException("Expected element " + EL_RECORD);

            EimRecord record = readNextRecord();
            if (record == null)
                throw new EimmlStreamException("Premature end of stream");

            recordset.addRecord(record);
        }

        return recordset;
    }

    private EimRecord readNextRecord()
        throws EimmlStreamException, XMLStreamException {
        EimRecord record = new EimRecord();

        record.setPrefix(xmlReader.getPrefix());
        record.setNamespace(xmlReader.getNamespaceURI());

        for (int i=0; i<xmlReader.getAttributeCount(); i++) {
            if (xmlReader.getAttributeName(i).equals(QN_DELETED))
                record.setDeleted(true);
            else
                log.warn("skipped unrecognized record attribute " +
                         xmlReader.getAttributeName(i));
        }

        while (xmlReader.hasNext()) {
            nextTag();

            if (xmlReader.isEndElement())
                break;

            String name = xmlReader.getLocalName();
            EimRecordField field = null;

            boolean isKey = BooleanUtils.
                toBoolean(xmlReader.getAttributeValue(NS_CORE, ATTR_TYPE));

            String type = xmlReader.getAttributeValue(NS_CORE, ATTR_TYPE);
            if (StringUtils.isBlank(type))
                throw new EimmlStreamException(xmlReader.getName() + " element requires " + ATTR_TYPE + " attribute");
            if (type.equals(TYPE_BYTES)) {
                byte[] value = EimmlTypeConverter.
                    toBytes(xmlReader.getElementText());
                field = new BytesField(name, value);
            } else if (type.equals(TYPE_TEXT)) {
                String value = EimmlTypeConverter.
                    toText(xmlReader.getElementText(), documentEncoding);
                field = new TextField(name, value);
            } else if (type.equals(TYPE_BLOB)) {
                InputStream value = EimmlTypeConverter.
                    toBlob(xmlReader.getElementText());
                field = new BlobField(name, value);
            } else if (type.equals(TYPE_CLOB)) {
                Reader value = EimmlTypeConverter.
                    toClob(xmlReader.getElementText());
                field = new ClobField(name, value);
            } else if (type.equals(TYPE_INTEGER)) {
                Integer value = EimmlTypeConverter.
                    toInteger(xmlReader.getElementText());
                field = new IntegerField(name, value);
            } else if (type.equals(TYPE_DATETIME)) {
                Calendar value = EimmlTypeConverter.
                    toDateTime(xmlReader.getElementText());
                field = new DateTimeField(name, value);
            } else if (type.equals(TYPE_DECIMAL)) {
                BigDecimal value = EimmlTypeConverter.
                    toDecimal(xmlReader.getElementText());
                field = new DecimalField(name, value);
            } else {
                throw new EimmlStreamException("Unrecognized field type");
            }

            if (isKey)
                record.addKeyField(field);
            else
                record.addField(field);
        }

        return record;
    }
}
