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

/**
 * Represents a Stamp on an Item. A Stamp is a set of related
 * properties and apis that is associated to an item.
 */
public interface Stamp extends AuditableObject {

    /**
     * @return Item attribute belongs to
     */
    Item getItem();

    /**
     * @param item
     *            attribute belongs to
     */
    void setItem(Item item);

    /**
     * @return Stamp type
     */
    String getType();

    /**
     * Return a new instance of Stamp containing a copy of the Stamp
     * @return copy of Stamp
     */
    Stamp copy();

    /**
     * Update stamp's timestamp
     */
    void updateTimestamp();

}