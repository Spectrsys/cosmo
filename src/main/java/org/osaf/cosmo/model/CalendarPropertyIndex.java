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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Index;
import org.hibernate.validator.NotNull;

/**
 * Represents an index for an item that contains an EventStamp.
 * The index consists of a property name and value, which 
 * represents a flatened icalendar property/value.
 * Many CalendarPropertyIndexes can be associated with a 
 * single Item.
 */
@Entity
@Table(name="cal_property_index")
public class CalendarPropertyIndex extends BaseModelObject implements
        java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 5482828614822186436L;
    private String name;
    private String value;
    private Item item;
    private EventStamp eventStamp = null;

    // Constructors

    /** default constructor */
    public CalendarPropertyIndex() {
    }

    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="itemid")
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
        
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventstampid")
    public EventStamp getEventStamp() {
        return eventStamp;
    }


    public void setEventStamp(EventStamp eventStamp) {
        this.eventStamp = eventStamp;
    }


    @Column(name = "propertyname", nullable = false, length=255)
    @NotNull
    @Index(name="idx_calpropname")
    public String getName() {
        return name;
    }



    public void setName(String name) {
        this.name = name;
    }


    @Column(name = "propertyvalue", length=20000)
    public String getValue() {
        return value;
    }



    public void setValue(String value) {
        this.value = value;
    }



    public CalendarPropertyIndex copy() {
        CalendarPropertyIndex index = new CalendarPropertyIndex();
        index.setName(getName());
        index.setValue(getValue());
        return index;
    }

    /** */
    public String toString() {
        return new ToStringBuilder(this).
            append("name", getName()).
            append("value", getValue()).
            toString();
    }
}
