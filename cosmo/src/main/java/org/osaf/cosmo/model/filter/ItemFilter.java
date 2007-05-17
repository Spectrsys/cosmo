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
package org.osaf.cosmo.model.filter;

import java.util.ArrayList;
import java.util.List;

import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.QName;

/**
 * Represents a filter that matches a set of criteria to all items.
 * The set of criteria is essentially "ANDed" together.
 * For example if displayName and parent are set, then the filter
 * will match all items that match the displayName set AND belong to
 * the parent set. 
 * 
 * If there are multiple AttributeFilters or StampFilters, all filters
 * must match the item for the item to match the ItemFilter.
 */
public class ItemFilter {
    
    String displayName = null;
    CollectionItem parent = null;
    String uid = null;
    
    List<AttributeFilter> attributeFilters = new ArrayList<AttributeFilter>();
    List<StampFilter> stampFilters = new ArrayList<StampFilter>();
    
    public ItemFilter() {
    }
    
    /**
     * List of AttributeFilters.  If there are multiple attribute filters,
     * each filter must match for an item to match the ItemFilter.
     * @return list of attribute filters
     */
    public List<AttributeFilter> getAttributeFilters() {
        return attributeFilters;
    }

    public void setAttributeFilters(List<AttributeFilter> attributeFilters) {
        this.attributeFilters = attributeFilters;
    }
    
    /**
     * Return an AttributeFilter that matches a specific QName
     * @param qname qualified name 
     * @return attribute filter that matches the qualified name
     */
    public AttributeFilter getAttributeFilter(QName qname) {
        for(AttributeFilter af: attributeFilters) {
            if(af.getQname().equals(qname))
                return af;
        }
        return null;
    }
    
    /**
     * Return a specific StampFilter instance
     * @param clazz StampFilter class
     * @return StampFilter instance that matches the given class
     */
    public StampFilter getStampFilter(Class clazz) {
        for(StampFilter sf: stampFilters) {
            if(sf.getClass().equals(clazz))
                return sf;
        }
        return null;
    }
    
    /**
     * List of StampFilters.  If there are multiple stamp filters,
     * each filter must match for an item to match the ItemFilter.
     * @return list of stamp filters
     */
    public List<StampFilter> getStampFilters() {
        return stampFilters;
    }

    public void setStampFilters(List<StampFilter> stampFilters) {
        this.stampFilters = stampFilters;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Match items by item displayName
     * @param displayName displayName to match
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public CollectionItem getParent() {
        return parent;
    }

    /**
     * Match items by parent
     * @param parent parent to match
     */
    public void setParent(CollectionItem parent) {
        this.parent = parent;
    }

    public String getUid() {
        return uid;
    }

    /**
     * Match item by uid
     * @param uid uid to match
     */
    public void setUid(String uid) {
        this.uid = uid;
    }
}
