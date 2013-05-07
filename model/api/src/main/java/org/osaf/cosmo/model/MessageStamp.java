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

import java.io.Reader;

/**
 * Stamp that associates message-specific attributes to an item.
 */
public interface MessageStamp extends Stamp {

    String FORMAT_DATE_SENT = "EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z";
    
    // Property accessors
    String getMessageId();

    void setMessageId(String id);

    String getHeaders();

    void setHeaders(String headers);

    void setHeaders(Reader headers);

    String getFrom();

    void setFrom(String from);

    String getTo();

    void setTo(String to);

    String getBcc();

    void setBcc(String bcc);

    String getCc();

    void setCc(String cc);

    String getOriginators();

    void setOriginators(String originators);

    String getDateSent();

    void setDateSent(String dateSent);

    String getInReplyTo();

    void setInReplyTo(String inReplyTo);

    String getReferences();

    void setReferences(String references);

    void setReferences(Reader references);

}