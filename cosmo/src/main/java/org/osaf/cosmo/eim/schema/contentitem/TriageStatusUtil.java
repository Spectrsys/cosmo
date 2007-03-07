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
package org.osaf.cosmo.eim.schema.contentitem;

import java.util.Date;

import org.osaf.cosmo.model.TriageStatus;
import org.osaf.cosmo.eim.schema.EimValidationException;

/**
 * Provides utilities for parsing and formatting EIM triage statuse
 * values.
 *
 * @see TriageStatus
 */
public class TriageStatusUtil {

    public static String format(TriageStatus ts) {
        // triage status is of the form
        //  <number> <unix timestamp> <auto triage bit>

        Integer code = ts.getCode();
        String label = code != null ?
            TriageStatus.label(code) :
            "unknown";

        Date updated = ts.getUpdated();
        long millis = updated != null ?
            updated.getTime() :
            System.currentTimeMillis();

        int autoTriage =
            ts.isAutoTriage() != null && ts.isAutoTriage() ?  1 : 0;

        return label + " " + (millis / 1000) + " " + autoTriage;
    }

    public static TriageStatus parse(String value)
        throws EimValidationException {
        String[] chunks = value.split(" ", 3);
        if (chunks.length != 3)
            throw new EimValidationException("Malformed triage status value " + value);

        TriageStatus ts = new TriageStatus();

        try {
            ts.setCode(TriageStatus.code(chunks[0]));
        } catch (IllegalArgumentException e) {
            throw new EimValidationException("Illegal triage status code " + chunks[0]);
        }

        try {
            long millis = Long.valueOf(chunks[1]) * 1000;
            ts.setUpdated(new Date(millis));
        } catch (NumberFormatException e) {
            throw new EimValidationException("Illegal triage status updated " + chunks[1]);
        }

        if (chunks[2].equals("1"))
            ts.setAutoTriage(Boolean.TRUE);
        else if (chunks[2].equals("0"))
            ts.setAutoTriage(Boolean.FALSE);
        else
            throw new EimValidationException("Illegal auto triage value " + chunks[2]);

        return ts;
    }
}
