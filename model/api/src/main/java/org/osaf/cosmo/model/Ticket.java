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
import java.util.Set;

/**
 * Represents a ticket that is used to grant access to
 * an Item.
 */
public interface Ticket extends AuditableObject {

    /** */
    String TIMEOUT_INFINITE = "Infinite";
    /** */
    String PRIVILEGE_READ = "read";
    /** */
    String PRIVILEGE_WRITE = "write";
    /** */
    String PRIVILEGE_FREEBUSY = "freebusy";
    
    String getKey();

    void setKey(String key);

    /**
     */
    String getTimeout();

    /**
     */
    void setTimeout(String timeout);

    /**
     */
    void setTimeout(Integer timeout);

    /**
     */
    Set<String> getPrivileges();

    /**
     */
    void setPrivileges(Set<String> privileges);

    /**
     * Returns the ticket type if the ticket's privileges match up
     * with one of the predefined types, or <code>null</code>
     * otherwise.
     */
    TicketType getType();

    /**
     */
    Date getCreated();

    /**
     */
    void setCreated(Date created);

    User getOwner();

    void setOwner(User owner);

    /**
     */
    boolean hasTimedOut();

    /**
     * Determines whether or not the ticket is granted on the given
     * item or one of its ancestors.
     */
    boolean isGranted(Item item);

    boolean isReadOnly();

    boolean isReadWrite();

    boolean isFreeBusy();

    int compareTo(Ticket t);

    Item getItem();

    void setItem(Item item);

}