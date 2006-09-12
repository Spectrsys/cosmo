/*
 * Copyright 2005-2006 Open Source Applications Foundation
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
package org.osaf.cosmo.security.impl;

import org.osaf.cosmo.security.CosmoSecurityContext;
import org.osaf.cosmo.security.CosmoSecurityException;
import org.osaf.cosmo.security.CosmoSecurityManager;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The default implementation of the {@link CosmoSecurityManager}
 * interface that provides a {@link CosmoSecurityContext} from
 * security information contained in JAAS or Acegi Security.
 */
public class CosmoSecurityManagerImpl implements CosmoSecurityManager {
    private static final Log log =
        LogFactory.getLog(CosmoSecurityManagerImpl.class);

    private AuthenticationManager authenticationManager;

    /* ----- CosmoSecurityManager methods ----- */

    /**
     * Provide a <code>CosmoSecurityContext</code> representing a
     * Cosmo user previously authenticated by the Cosmo security
     * system.
     */
    public CosmoSecurityContext getSecurityContext()
        throws CosmoSecurityException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authen = context.getAuthentication();
        if (authen == null) {
            throw new CosmoSecurityException("no Authentication found in " +
                                             "SecurityContext");
        }

        return createSecurityContext(authen);
    }

    /**
     * Authenticate the given Cosmo credentials and register a
     * <code>CosmoSecurityContext</code> for them. This method is used
     * when Cosmo components need to programatically log in a user
     * rather than relying on a security context already being in
     * place.
     */
    public CosmoSecurityContext initiateSecurityContext(String username,
                                                        String password)
        throws CosmoSecurityException {
        try {
            UsernamePasswordAuthenticationToken credentials =
                new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication =
                authenticationManager.authenticate(credentials);
            SecurityContext sc = SecurityContextHolder.getContext();
            sc.setAuthentication(authentication);
            return createSecurityContext(authentication);
        } catch (AuthenticationException e) {
            throw new CosmoSecurityException("can't establish security context",
                                             e);
        }
    }

    /* ----- our methods ----- */

    /**
     */
    protected CosmoSecurityContext
        createSecurityContext(Authentication authen) {
        return new CosmoSecurityContextImpl(authen);
    }

    /**
     */
    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    /**
     */
    public void
        setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
}
