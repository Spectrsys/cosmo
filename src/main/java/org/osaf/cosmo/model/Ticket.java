/*
 * Copyright 2005 Open Source Applications Foundation
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

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A bean encapsulating the information about a ticket used in the
 * bodies of ticket requests and responses.
 *
 * This class does not perform any validation on the ticket info,
 * leaving that responsibility to those objects which manipulate
 * ticket info.
 *
 * Similarly, the class does not know how to convert itself to or from
 * XML.
 */
public class Ticket {

    public static final String INFINITE = "Infinite";

    private String id;
    private String owner;
    private String timeout;
    private Set privileges;
    private Date created;

    /**
     */
    public Ticket() {
        privileges = new HashSet();
    }

    /**
     */
    public String getId() {
        return id;
    }

    /**
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     */
    public String getOwner() {
        return owner;
    }

    /**
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     */
    public String getTimeout() {
        return timeout;
    }

    /**
     */
    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    /**
     */
    public Set getPrivileges() {
        return privileges;
    }

    /**
     */
    public void setPrivileges(Set privileges) {
        this.privileges = privileges;
    }

    /**
     */
    public Date getCreated() {
        return created;
    }

    /**
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     */
    public boolean hasTimedOut() {
        if (timeout == null || timeout.equals(INFINITE)) {
            return false;
        }

        int seconds = Integer.parseInt(timeout.substring(7));

        Calendar expiry = Calendar.getInstance();
        expiry.setTime(created);
        expiry.add(Calendar.SECOND, seconds);

        return Calendar.getInstance().after(expiry);
    }

    /**
     */
    public boolean equals(Object o) {
        if (! (o instanceof Ticket)) {
            return false;
        }
        Ticket it = (Ticket) o;
        return new EqualsBuilder().
            append(id, it.id).
            append(owner, it.owner).
            append(timeout, it.timeout).
            append(privileges, it.privileges).
            isEquals();
    }

    /**
     */
    public int hashCode() {
        return new HashCodeBuilder(3, 5).
            append(id).
            append(owner).
            append(timeout).
            append(privileges).
            toHashCode();
    }

    /**
     */
    public String toString() {
        return new ToStringBuilder(this).
            append("id", id).
            append("owner", owner).
            append("timeout", timeout).
            append("privileges", privileges).
            append("created", created).
            toString();
    }
}
