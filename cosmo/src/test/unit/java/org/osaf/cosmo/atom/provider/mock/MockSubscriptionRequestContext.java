/*
 * Copyright 2007-2008 Open Source Applications Foundation
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
package org.osaf.cosmo.atom.provider.mock;

import java.net.URLEncoder;

import org.apache.abdera.protocol.server.ServiceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.atom.provider.SubscriptionTarget;
import org.osaf.cosmo.model.CollectionSubscription;
import org.osaf.cosmo.model.User;

/**
 * Mock implementation of {@link RequestContext} representing requests
 * to a user subscription entry.
 */
public class MockSubscriptionRequestContext extends BaseMockRequestContext {
    private static final Log log =
        LogFactory.getLog(MockSubscriptionRequestContext.class);

    public MockSubscriptionRequestContext(ServiceContext context,
                                          User user,
                                          CollectionSubscription sub) {
        this(context, user, sub, "GET");
    }

    public MockSubscriptionRequestContext(ServiceContext context,
                                          User user,
                                          CollectionSubscription sub,
                                          String method) {
        super(context, method, toRequestUri(user, sub));
        this.target = new SubscriptionTarget(this, user, sub);
    }

    private static String toRequestUri(User user,
                                       CollectionSubscription sub) {
        return TEMPLATE_SUBSCRIPTION.bind(user.getUsername(),
                                          sub.getDisplayName());
    }
}
