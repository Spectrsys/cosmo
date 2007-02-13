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

import javax.xml.namespace.QName;
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
        if (xmlReader.isEndElement() &&
            xmlReader.getName().equals(QN_COLLECTION))
            return false;
        if (xmlReader.isStartElement() &&
            xmlReader.getName().equals(QN_RECORDSET))
            return true;
        throw new EimmlStreamException("hasNext called at illegal cursor position " + xmlReader.getName());
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
        if (log.isDebugEnabled()) {
            if (xmlReader.isStartElement())
                log.debug("read start tag: " + xmlReader.getName());
            else
                log.debug("read end tag: " + xmlReader.getName());
        }
        return rc;
    }

    // leaves the cursor positioned at the first recordset element
    private void readCollection()
        throws XMLStreamException, EimmlStreamException {
        // move to <collection>
        nextTag();
        if (! xmlReader.isStartElement() &&
            xmlReader.getName().equals(QN_COLLECTION))
            throw new EimmlStreamException("Outermost element must be " + QN_COLLECTION);

        uuid = xmlReader.getAttributeValue(null, ATTR_UUID);

        name = xmlReader.getAttributeValue(null, ATTR_NAME);
        if (StringUtils.isBlank(name))
            name = null;

        // move to first <recordset> or </collection>
        nextTag();
    }

    // leaves the cursor positioned at either the next recordset
    // element or the collection end element
    private EimRecordSet readNextRecordSet()
        throws EimmlStreamException, XMLStreamException {
        // finish stream on </collection>
        if (xmlReader.isEndElement() &&
            xmlReader.getName().equals(QN_COLLECTION))
            return null;

        // begin at <recordset>
        if (! (xmlReader.isStartElement() &&
               xmlReader.getName().equals(QN_RECORDSET)))
            throw new EimmlStreamException("Expected start element " + QN_RECORDSET + " but got " + xmlReader.getName());

        EimRecordSet recordset = new EimRecordSet();

        for (int i=0; i<xmlReader.getAttributeCount(); i++) {
            QName name = xmlReader.getAttributeName(i);
            String value = xmlReader.getAttributeValue(i);
            if (name.equals(QN_UUID)) {
                if (StringUtils.isBlank(value))
                    throw new EimmlStreamException("Recordset element requires " + ATTR_UUID + " attribute");
                recordset.setUuid(value);
            } else if (name.equals(QN_DELETED)) {
                if (BooleanUtils.toBoolean(value))
                    recordset.setDeleted(true);
            } else {
                log.warn("skipped unrecognized recordset attribute " + name);
            }
        }

        // move to next <record> or </recordset>
        nextTag();

        while (xmlReader.hasNext()) {
            // complete on </recordset>
            if (xmlReader.isEndElement() &&
                xmlReader.getName().equals(QN_RECORDSET))
                break;

            EimRecord record = readNextRecord();
            if (record == null)
                throw new EimmlStreamException("Premature end of stream");

            recordset.addRecord(record);

            // move to next <record> or </recordset>
            nextTag();
        }

        // move to next <recordset> or </collection>
        nextTag();

        return recordset;
    }

    // leaves cursor on </record>
    private EimRecord readNextRecord()
        throws EimmlStreamException, XMLStreamException {
        // begin at <record>
        if (! (xmlReader.isStartElement() &&
               xmlReader.getLocalName().equals(EL_RECORD)))
            throw new EimmlStreamException("Expected start element " + EL_RECORD + " but got " + xmlReader.getName());

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

        // move to next field element or </record>
        nextTag();

        while (xmlReader.hasNext()) {
            // complete on </record>
            if (xmlReader.isEndElement() &&
                xmlReader.getLocalName().equals(EL_RECORD))
                break;

            if (! xmlReader.isStartElement())
                throw new EimmlStreamException("Expected field element but got " + xmlReader.getName());

            String name = xmlReader.getLocalName();

            boolean isKey = BooleanUtils.
                toBoolean(xmlReader.getAttributeValue(NS_CORE, ATTR_KEY));
            boolean isEmpty = BooleanUtils.
                toBoolean(xmlReader.getAttributeValue(null, ATTR_EMPTY));
            String type = xmlReader.getAttributeValue(NS_CORE, ATTR_TYPE);
            if (StringUtils.isBlank(type))
                throw new EimmlStreamException(xmlReader.getName() + " element requires " + ATTR_TYPE + " attribute");

            String text = xmlReader.getElementText();
            if (isEmpty) {
                if (! (type.equals(TYPE_TEXT) || type.equals(TYPE_CLOB) ||
                       type.equals(TYPE_BLOB)))
                    throw new EimmlStreamException("Invalid empty attribute on field element " + xmlReader.getName());
                if (text != null)
                    if (log.isDebugEnabled())
                        log.debug("emptying non-null text for field " + xmlReader.getName());
                text = "";
            } else if (text.equals(""))
                text = null;

            EimRecordField field = null;
            if (type.equals(TYPE_BYTES)) {
                byte[] value = EimmlTypeConverter.toBytes(text);
                field = new BytesField(name, value);
            } else if (type.equals(TYPE_TEXT)) {
                String value = EimmlTypeConverter.toText(text,
                                                         documentEncoding);
                field = new TextField(name, value);
            } else if (type.equals(TYPE_BLOB)) {
                InputStream value = EimmlTypeConverter.toBlob(text);
                field = new BlobField(name, value);
            } else if (type.equals(TYPE_CLOB)) {
                Reader value = EimmlTypeConverter.toClob(text);
                field = new ClobField(name, value);
            } else if (type.equals(TYPE_INTEGER)) {
                Integer value = EimmlTypeConverter.toInteger(text);
                field = new IntegerField(name, value);
            } else if (type.equals(TYPE_DATETIME)) {
                Calendar value = EimmlTypeConverter.toDateTime(text);
                field = new DateTimeField(name, value);
            } else if (type.equals(TYPE_DECIMAL)) {
                BigDecimal value = EimmlTypeConverter.toDecimal(text);
                field = new DecimalField(name, value);
            } else {
                throw new EimmlStreamException("Unrecognized field type");
            }

            if (isKey)
                record.addKeyField(field);
            else
                record.addField(field);

            // move to next field element or </record>
            nextTag();
        }

        return record;
    }
}
