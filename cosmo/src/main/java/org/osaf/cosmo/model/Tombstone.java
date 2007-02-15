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
package org.osaf.cosmo.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * When an Item is removed from a collection, a tombstone is attached
 * to the collection to track when this removal ocurred.
 */
@Entity
@Table(name="tombstones")
public class Tombstone extends BaseModelObject {
    
    private String itemUid = null;
    private Date timestamp = null;
    private CollectionItem collection = null;

    public Tombstone() {
    }
    
    public Tombstone(CollectionItem collection, Item item) {
        this.itemUid = item.getUid();
        this.timestamp = new Date(System.currentTimeMillis());
        this.collection = collection;
    }
    
    @Column(name = "itemuid", nullable = false, length=255)
    public String getItemUid() {
        return itemUid;
    }

    public void setItemUid(String itemUid) {
        this.itemUid = itemUid;
    }

    @Column(name = "removedate", nullable = false)
    @Type(type="long_timestamp")
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collectionid", nullable = false)
    public CollectionItem getCollection() {
        return collection;
    }

    public void setCollection(CollectionItem collection) {
        this.collection = collection;
    }
}
