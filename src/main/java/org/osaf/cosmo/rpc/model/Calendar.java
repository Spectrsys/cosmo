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
package org.osaf.cosmo.rpc.model;

import java.util.Map;

import org.osaf.cosmo.model.Ticket;


/**
 * This class contains information about a user's calendar.
 *
 * @author bobbyrullo
 */
public class Calendar {

    private String displayName = null;
    private String uid = null;
    private String ticketKey;

    private Map<String, String> protocolUrls;

    public Map<String, String> getProtocolUrls() {
        return protocolUrls;
    }

    public void setProtocolUrls(Map<String, String> protocolUrls) {
        this.protocolUrls = protocolUrls;
    }
    /**
     * This is the name as displayed to the user
     */
    public String getName() {
        return displayName;
    }

    public void setName(String displayName) {
        this.displayName = displayName;
    }


    /**
     * This is the uid of this collection.
     */
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    
    /**
     * A ticket that provides access to this collection.
     * @return
     */
    public String getTicketKey() {
        return ticketKey;
    }

    public void setTicketKey(String ticket) {
        this.ticketKey = ticket;
    }




}
