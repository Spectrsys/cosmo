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

import org.osaf.cosmo.dao.UserDao;
import org.osaf.cosmo.security.CosmoSecurityManager;
import org.osaf.cosmo.security.impl.CosmoUserDetailsImpl;

import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;

/**
 * Implements Acegi Security's <code>UserDetailsService</code>
 * interface by retrieving user details with a <code>UserDao</code>.
 * 
 * @see UserDetailsService
 * @see UserDao
 */
public class CosmoUserDetailsService implements UserDetailsService {
    private static final Log log =
        LogFactory.getLog(CosmoUserDetailsService.class);

    private UserDao userDao;

    /**
     * Locates the user with the given username by retrieving it
     * with this service's <code>UserDao</code> and returns a
     * <code>UserDetails</code> representing the user.
     *
     * @param username the username presented to the 
     * {@link DaoAuthenticationProvider}
     * @return a fully populated {@link UserDetails} (never
     * <code>null</code>)
     * @throws UsernameNotFoundException if the user could not be
     * found
     * @see UserDetails
     */
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException, DataAccessException {
        try {
            return new
                CosmoUserDetailsImpl(userDao.getUser(username));
        } catch (DataRetrievalFailureException e) {
            throw new UsernameNotFoundException("user " + username +
                                                " not found", e);
        }
    }

    /** */
    public UserDao getUserDao() {
        return userDao;
    }

    /** */
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
