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
package org.osaf.cosmo.model;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.apache.commons.io.IOUtils;
import org.hibernate.annotations.Type;


/**
 * Represents an attribute with a text value.
 */
@Entity
@DiscriminatorValue("text")
public class TextAttribute extends Attribute implements
        java.io.Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 2417093506524504993L;
    private String value;

    // Constructors

    /** default constructor */
    public TextAttribute() {
    }

    public TextAttribute(QName qname, String value) {
        setQName(qname);
        this.value = value;
    }
    
    /**
     * Construct TextAttribute from Reader
     * @param qname
     * @param reader
     */
    public TextAttribute(QName qname, Reader reader) {
        this.value = read(reader);
    }

    // Property accessors
    @Column(name="textvalue", length=102400000)
    @Type(type="text")
    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * @return reader to value
     */
    @Transient
    public Reader getReader() {
        if(value!=null)
            return new StringReader(value);
        else
            return null;
    }
    
    /**
     * @return length of text
     */
    @Transient
    public int getLength() {
        if(value!=null)
            return value.length();
        else
            return 0;
    }
    
    public void setValue(Object value) {
        if (value != null && !(value instanceof String) &&
            !(value instanceof Reader))
            throw new ModelValidationException(
                    "attempted to set non String or Reader value on attribute");
        if (value instanceof Reader) {
            setValue(read((Reader) value));
        } else {
            setValue((String) value);
        }
    }
    
    public Attribute copy() {
        TextAttribute attr = new TextAttribute();
        attr.setQName(getQName().copy());
        attr.setValue(getValue());
        return attr;
    }

    private String read(Reader reader) {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(reader, writer);
        } catch (IOException e) {
            throw new RuntimeException("error reading stream");
        }
        return writer.toString();
    }
}
