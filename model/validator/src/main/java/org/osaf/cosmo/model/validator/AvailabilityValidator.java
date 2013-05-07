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
package org.osaf.cosmo.model.validator;

import java.io.IOException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.ValidationException;

import org.osaf.cosmo.api.ICalendarConstants;
import org.osaf.cosmo.utils.CalendarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check if a Calendar object contains a valid VAVAILABILITY
 */
public class AvailabilityValidator implements ConstraintValidator<Availability, Calendar> {

    private Logger logger = LoggerFactory.getLogger(AvailabilityValidator.class);

    public void initialize(Availability parameters) {
    }

    public boolean isValid(Calendar value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        Calendar calendar = null;
        try {
            calendar = (Calendar) value;
            
            // validate entire icalendar object
            calendar.validate(true);
            
            // additional check to prevent bad .ics
            CalendarUtils.parseCalendar(calendar.toString());
            
            // make sure we have a VAVAILABILITY
            ComponentList comps = calendar.getComponents();
            if (comps==null) {
                logger.warn("error validating availability: {}", calendar);
                return false;
            }
            
            comps = comps.getComponents(ICalendarConstants.COMPONENT_VAVAILABLITY);
            if (comps==null || comps.size()==0) {
                logger.warn("error validating availability: {}", calendar);
                return false;
            }
            
            return true;
            
        } catch (ValidationException ve) {
            logger.warn("availability validation error", ve);
            if (calendar!=null) {
                logger.warn("error validating availability: {}", calendar);
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch(ParserException e) {
            logger.warn("parse error", e);
            if (calendar!=null) {
                logger.warn("error parsing availability: {}", calendar);
            }
            return false;
        }
    }

}

