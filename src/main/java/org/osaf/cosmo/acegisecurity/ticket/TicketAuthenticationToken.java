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
package org.osaf.cosmo.acegisecurity.ticket;

import java.io.Serializable;
import java.util.Set;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.providers.AbstractAuthenticationToken;

import org.osaf.cosmo.model.Ticket;

/**
 * Represents a ticket-based
 * {@link org.acegisecurity.Authentication}.
 *
 * Before being authenticated, the token contains the ticket id and
 * the path of the ticketed resource. After authentication, the
 * token's principal is the {@link Ticket} itself.
 */
public class TicketAuthenticationToken extends AbstractAuthenticationToken
    implements Serializable {

    private static final GrantedAuthority[] AUTHORITIES = {};

    private boolean authenticated;
    private String path;
    private Set ids;
    private Ticket ticket;

    /**
     * @param path the absolute URI path to the ticketed resource
     * @param id all ticket ids provided for the resource
     */
    public TicketAuthenticationToken(String path, Set ids) {
        super(AUTHORITIES);
        if (path == null || path.equals("")) {
            throw new IllegalArgumentException("path may not be null or empty");
        }
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids may not be null or empty");
        }
        this.path = path;
        this.ids = ids;
        authenticated = false;
    }

    // Authentication methods

    /**
     */
    public void setAuthenticated(boolean isAuthenticated) {
        authenticated = isAuthenticated;
    }

    /**
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Always returns an empty <code>String</code>.
     */
    public Object getCredentials() {
        return "";
    }

    /**
     * Returns the ticket.
     */
    public Object getPrincipal() {
        return ticket;
    }

    // our methods

    /**
     */
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    /**
     */
    public String getPath() {
        return path;
    }

    /**
     */
    public Set getIds() {
        return ids;
    }

    /**
     */
    public boolean equals(Object obj) {
        if (! super.equals(obj)) {
            return false;
        }
        if (! (obj instanceof TicketAuthenticationToken)) {
            return false;
        }
        TicketAuthenticationToken test = (TicketAuthenticationToken) obj;
        return ticket.equals(test.getPrincipal());
    }
}
