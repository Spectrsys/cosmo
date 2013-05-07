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

/**
 * An entity representing a password change request.
 * 
 * There should be a single password change request corresponding
 * to each password recovery request in the system. 
 */
public interface PasswordRecovery {

    String getKey();

    void setKey(String key);

    /**
     */
    long getTimeout();

    void setTimeout(long timeout);

    /**
     */
    Date getCreated();

    /**
     */
    void setCreated(Date created);

    /**
     */
    User getUser();

    /**
     */
    void setUser(User user);

    /**
     */
    boolean hasExpired();

}