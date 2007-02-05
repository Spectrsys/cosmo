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

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Represents attribute with an Boolean value.
 */
@Entity
@DiscriminatorValue("boolean")
public class BooleanAttribute extends Attribute implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -8393344132524216261L;
    private Boolean value;

    /** default constructor */
    public BooleanAttribute() {
    }

    public BooleanAttribute(QName qname, Boolean value) {
        setQName(qname);
        this.value = value;
    }

    // Property accessors
    @Column(name = "booleanvalue")
    public Boolean getValue() {
        return this.value;
    }

    public Attribute copy() {
        BooleanAttribute attr = new BooleanAttribute();
        attr.setQName(getQName().copy());
        attr.setValue(new Boolean(value));
        return attr;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    public void setValue(Object value) {
        if (value != null && !(value instanceof Boolean))
            throw new ModelValidationException(
                    "attempted to set non Boolean value on attribute");
        setValue((Boolean) value);
    }

}
