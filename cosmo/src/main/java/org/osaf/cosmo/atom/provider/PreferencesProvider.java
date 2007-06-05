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
package org.osaf.cosmo.atom.provider;

import java.io.IOException;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.EntityTag;
import org.apache.abdera.protocol.server.provider.AbstractResponseContext;
import org.apache.abdera.protocol.server.provider.RequestContext;
import org.apache.abdera.protocol.server.provider.ResponseContext;
import org.apache.abdera.util.Constants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.atom.AtomConstants;
import org.osaf.cosmo.atom.generator.GeneratorException;
import org.osaf.cosmo.atom.generator.PreferencesFeedGenerator;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.server.ServiceLocator;
import org.osaf.cosmo.service.UserService;

public class PreferencesProvider extends BaseProvider
    implements AtomConstants {
    private static final Log log = LogFactory.getLog(PreferencesProvider.class);

    private UserService userService;

    // Provider methods

    public ResponseContext createEntry(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    public ResponseContext deleteEntry(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext deleteMedia(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    public ResponseContext updateEntry(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext updateMedia(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext getService(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    public ResponseContext getFeed(RequestContext request) {
        PreferencesTarget target = (PreferencesTarget) request.getTarget();
        User user = target.getUser();
        if (log.isDebugEnabled())
            log.debug("getting preferences feed for user " +
                      user.getUsername());

        try {
            ServiceLocator locator = createServiceLocator(request);
            PreferencesFeedGenerator generator =
                createPreferencesFeedGenerator(target, locator);
            Feed feed = generator.generateFeed(user);

            // no entity tag for this synthetic feed
            return createResponseContext(feed.getDocument());
        } catch (GeneratorException e) {
            String reason = "Unknown feed generation error: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        }
    }

    public ResponseContext getEntry(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext getMedia(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext getCategories(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext entryPost(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext mediaPost(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    // ExtendedProvider methods

    public ResponseContext updateCollection(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    // our methods

    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void init() {
        super.init();
        if (userService == null)
            throw new IllegalStateException("userService is required");
    }

    protected PreferencesFeedGenerator
        createPreferencesFeedGenerator(PreferencesTarget target,
                                       ServiceLocator locator) {
        return getGeneratorFactory().
            createPreferencesFeedGenerator(locator);
    }
}
