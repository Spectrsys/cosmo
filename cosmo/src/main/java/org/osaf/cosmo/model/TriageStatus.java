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

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Represents a compound triage status value.
 */
public class TriageStatus {

    /** */
    public static final String LABEL_NOW = "NOW";
    /** */
    public static final String LABEL_LATER = "LATER";
    /** */
    public static final String LABEL_DONE = "DONE";
    /** */
    public static final int CODE_NOW = 100;
    /** */
    public static final int CODE_LATER = 200;
    /** */
    public static final int CODE_DONE = 300;

    private Integer code = null;
    private Date updated = null;
    private Boolean autoTriage = null;
    
    public TriageStatus() {
    }
   
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Boolean isAutoTriage() {
        return autoTriage;
    }

    public void setAutoTriage(Boolean autoTriage) {
        this.autoTriage = autoTriage;
    }
        
    public TriageStatus copy() {
        TriageStatus copy = new TriageStatus();
        copy.setCode(code);
        copy.setUpdated(updated);
        copy.setAutoTriage(autoTriage);
        return copy;
    }

    public String toString() {
        return new ToStringBuilder(this).
            append("code").append(code).
            append("updated").append(updated).
            append("autoTriage").append(autoTriage).
            toString();
    }

    public static String label(Integer code) {
        if (code.equals(CODE_NOW))
            return LABEL_NOW;
        if (code.equals(CODE_LATER))
            return LABEL_LATER;
        if (code.equals(CODE_DONE))
            return LABEL_DONE;
        throw new IllegalStateException("Unknown code " + code);
    }

    public static Integer code(String label) {
        if (label.equals(LABEL_NOW))
            return new Integer(CODE_NOW);
        if (label.equals(LABEL_LATER))
            return new Integer(CODE_LATER);
        if (label.equals(LABEL_DONE))
            return new Integer(CODE_DONE);
        throw new IllegalStateException("Unknown label " + label);
    }
}
