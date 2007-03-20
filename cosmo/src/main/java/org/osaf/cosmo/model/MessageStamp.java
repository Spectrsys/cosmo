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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;


/**
 * Represents a Message Stamp.
 */
@Entity
@DiscriminatorValue("message")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class MessageStamp extends Stamp implements
        java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6100568628972081120L;
    
    public static final QName ATTR_MESSAGE_SUBJECT = new QName(
            MessageStamp.class, "subject");
    
    public static final QName ATTR_MESSAGE_TO = new QName(
            MessageStamp.class, "to");
    
    public static final QName ATTR_MESSAGE_CC = new QName(
            MessageStamp.class, "cc");
    
    public static final QName ATTR_MESSAGE_BCC = new QName(
            MessageStamp.class, "bcc");
    
    /** default constructor */
    public MessageStamp() {
    }
    
    public MessageStamp(Item item) {
        setItem(item);
    }
    
    @Transient
    public String getType() {
        return "message";
    }
    
    // Property accessors
    @Transient
    public String getBcc() {
        // bcc stored as TextAttribute on Item
        TextAttribute attr = (TextAttribute) getItem().getAttribute(ATTR_MESSAGE_BCC);
        if(attr!=null)
            return attr.getValue();
        else
            return null;
    }

    public void setBcc(String bcc) {
        //bcc stored as TextAttribute on Item
        TextAttribute attr = (TextAttribute) getItem().getAttribute(ATTR_MESSAGE_BCC);
        if(attr==null && bcc!=null) {
            attr = new TextAttribute(ATTR_MESSAGE_BCC,bcc);
            getItem().addAttribute(attr);
            return;
        }
        if(bcc==null)
            getItem().removeAttribute(ATTR_MESSAGE_BCC);
        else
            attr.setValue(bcc);
    }

    @Transient
    public String getCc() {
        // cc stored as TextAttribute on Item
        TextAttribute attr = (TextAttribute) getItem().getAttribute(ATTR_MESSAGE_CC);
        if(attr!=null)
            return attr.getValue();
        else
            return null;
    }

    public void setCc(String cc) {
        // cc stored as TextAttribute on Item
        TextAttribute attr = (TextAttribute) getItem().getAttribute(ATTR_MESSAGE_CC);
        if(attr==null && cc!=null) {
            attr = new TextAttribute(ATTR_MESSAGE_CC,cc);
            getItem().addAttribute(attr);
            return;
        }
        if(cc==null)
            getItem().removeAttribute(ATTR_MESSAGE_CC);
        else
            attr.setValue(cc);
    }

    @Transient
    public String getSubject() {
        // subject stored as TextAttribute on Item
        TextAttribute attr = (TextAttribute) getItem().getAttribute(ATTR_MESSAGE_SUBJECT);
        if(attr!=null)
            return attr.getValue();
        else
            return null;
    }

    public void setSubject(String subject) {
        // subject stored as TextAttribute on Item
        TextAttribute attr = (TextAttribute) getItem().getAttribute(ATTR_MESSAGE_SUBJECT);
        if(attr==null && subject!=null) {
            attr = new TextAttribute(ATTR_MESSAGE_SUBJECT,subject);
            getItem().addAttribute(attr);
            return;
        }
        if(subject==null)
            getItem().removeAttribute(ATTR_MESSAGE_SUBJECT);
        else
            attr.setValue(subject);
    }

    @Transient
    public String getTo() {
        // to stored as TextAttribute on Item
        TextAttribute attr = (TextAttribute) getItem().getAttribute(ATTR_MESSAGE_TO);
        if(attr!=null)
            return attr.getValue();
        else
            return null;
    }

    public void setTo(String to) {
        // to stored as TextAttribute on Item
        TextAttribute attr = (TextAttribute) getItem().getAttribute(ATTR_MESSAGE_TO);
        if(attr==null && to!=null) {
            attr = new TextAttribute(ATTR_MESSAGE_TO,to);
            getItem().addAttribute(attr);
            return;
        }
        if(to==null)
            getItem().removeAttribute(ATTR_MESSAGE_TO);
        else
            attr.setValue(to);
    }

    /**
     * Return MessageStamp from Item
     * @param item
     * @return MessageStamp from Item
     */
    public static MessageStamp getStamp(Item item) {
        return (MessageStamp) item.getStamp(MessageStamp.class);
    }
    
    /**
     * Return MessageStamp from Item
     * @param item
     * @param activeOnly
     * @return MessageStamp from Item
     */
    public static MessageStamp getStamp(Item item,
                                        boolean activeOnly) {
        return (MessageStamp) item.getStamp(MessageStamp.class,
                                            activeOnly);
    }
    
    public Stamp copy(Item item) {
        MessageStamp stamp = new MessageStamp();
        stamp.setSubject(getSubject());
        stamp.setTo(getTo());
        stamp.setBcc(getBcc());
        stamp.setCc(getCc());
        return stamp;
    }
}
