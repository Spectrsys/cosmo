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
package org.osaf.cosmo.acegisecurity;

import java.util.Properties;

import net.sf.acegisecurity.event.authentication.AuthenticationFailureServiceExceptionEvent;
import net.sf.acegisecurity.providers.ProviderManager;

import org.osaf.cosmo.acegisecurity.ticket.TicketTimeoutException;
import org.osaf.cosmo.acegisecurity.ticket.TicketedItemNotFoundException;

/**
 * Extends {@link net.sf.acegisecurity.providers.ProviderManager}
 * to add exception mappings for Cosmo-specific authentication
 * exceptions.
 */
public class CosmoProviderManager extends ProviderManager {

    /**
     */
    protected void
        doAddExtraDefaultExceptionMappings(Properties exceptionMappings) {
        exceptionMappings.put(TicketTimeoutException.class.getName(),
                              AuthenticationFailureServiceExceptionEvent.
                              class.getName());
        exceptionMappings.put(TicketedItemNotFoundException.class.getName(),
                              AuthenticationFailureServiceExceptionEvent.
                              class.getName());
        
    }
}
