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
import org.osaf.cosmo.atom.generator.SubscriptionFeedGenerator;
import org.osaf.cosmo.model.CollectionSubscription;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.server.ServiceLocator;
import org.osaf.cosmo.service.UserService;

public class SubscriptionProvider extends BaseProvider
    implements AtomConstants {
    private static final Log log =
        LogFactory.getLog(SubscriptionProvider.class);

    private UserService userService;

    // Provider methods

    public ResponseContext createEntry(RequestContext request) {
        SubscribedTarget target = (SubscribedTarget) request.getTarget();
        User user = target.getUser();

        // XXX: check write preconditions?

        try {
            Entry entry = (Entry) request.getDocument().getRoot();
            CollectionSubscription sub = readSubscription(entry);

            if (user.getSubscription(sub.getDisplayName()) != null)
                return conflict(getAbdera(), request,
                                "Named subscription exists");

            if (log.isDebugEnabled())
                log.debug("creating subscription " + sub.getDisplayName() +
                          " for user " + user.getUsername());

            user.addSubscription(sub);
            user = userService.updateUser(user);

            ServiceLocator locator = createServiceLocator(request);
            SubscriptionFeedGenerator generator =
                createSubscriptionFeedGenerator(locator);
            entry = generator.generateEntry(sub);

            AbstractResponseContext rc =
                createResponseContext(entry.getDocument(), 201, "Created");
            rc.setContentType(Constants.ATOM_MEDIA_TYPE);
            rc.setEntityTag(new EntityTag(sub.getEntityTag()));

            try {
                rc.setLocation(entry.getSelfLink().getHref().toString());
                rc.setContentLocation(entry.getSelfLink().getHref().toString());
            } catch (Exception e) {
                throw new RuntimeException("Error parsing self link href", e);
            }

            return rc;
        } catch (IOException e) {
            String reason = "Unable to read request content: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        } catch (GeneratorException e) {
            String reason = "Unknown entry generation error: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        }
    }

    public ResponseContext deleteEntry(RequestContext request) {
        SubscriptionTarget target = (SubscriptionTarget) request.getTarget();
        User user = target.getUser();
        CollectionSubscription sub = target.getSubscription();
        if (log.isDebugEnabled())
            log.debug("deleting entry for subscription " +
                      sub.getDisplayName() + " for user " + user.getUsername());

        user.removeSubscription(sub);
        userService.updateUser(user);

        return createResponseContext(204);
    }
  
    public ResponseContext deleteMedia(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    public ResponseContext updateEntry(RequestContext request) {
        SubscriptionTarget target = (SubscriptionTarget) request.getTarget();
        User user = target.getUser();
        CollectionSubscription sub = target.getSubscription();
        if (log.isDebugEnabled())
            log.debug("upudating subscription " + sub.getDisplayName() +
                      " for user " + user.getUsername());

        // XXX: check write preconditions?

        try {
            Entry entry = (Entry) request.getDocument().getRoot();
            CollectionSubscription newsub = readSubscription(entry);

            if (user.getSubscription(newsub.getDisplayName()) != null &&
                ! user.getSubscription(newsub.getDisplayName()).equals(sub))
                return conflict(getAbdera(), request,
                                "Named subscription exists");

            sub.setDisplayName(newsub.getDisplayName());
            sub.setCollectionUid(newsub.getCollectionUid());
            sub.setTicketKey(newsub.getTicketKey());

            user = userService.updateUser(user);

            ServiceLocator locator = createServiceLocator(request);
            SubscriptionFeedGenerator generator =
                createSubscriptionFeedGenerator(locator);
            entry = generator.generateEntry(sub);

            AbstractResponseContext rc =
                createResponseContext(entry.getDocument());
            rc.setContentType(Constants.ATOM_MEDIA_TYPE);
            rc.setEntityTag(new EntityTag(sub.getEntityTag()));

            return rc;
        } catch (IOException e) {
            String reason = "Unable to read request content: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        } catch (GeneratorException e) {
            String reason = "Unknown entry generation error: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        }
    }
  
    public ResponseContext updateMedia(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext getService(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    public ResponseContext getFeed(RequestContext request) {
        SubscribedTarget target = (SubscribedTarget) request.getTarget();
        User user = target.getUser();
        if (log.isDebugEnabled())
            log.debug("getting subscribed feed for user " + user.getUsername());

        try {
            ServiceLocator locator = createServiceLocator(request);
            SubscriptionFeedGenerator generator =
                createSubscriptionFeedGenerator(locator);
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
        SubscriptionTarget target = (SubscriptionTarget) request.getTarget();
        User user = target.getUser();
        CollectionSubscription sub = target.getSubscription();
        if (log.isDebugEnabled())
            log.debug("getting entry for subscription " +
                      sub.getDisplayName() + " for user " + user.getUsername());

        try {
            ServiceLocator locator = createServiceLocator(request);
            SubscriptionFeedGenerator generator =
                createSubscriptionFeedGenerator(locator);
            Entry entry = generator.generateEntry(sub);

            AbstractResponseContext rc =
                createResponseContext(entry.getDocument());
            rc.setEntityTag(new EntityTag(sub.getEntityTag()));
            // override Abdera which sets content type to include the
            // type attribute because IE chokes on it
            rc.setContentType(Constants.ATOM_MEDIA_TYPE);
            return rc;
        } catch (GeneratorException e) {
            String reason = "Unknown entry generation error: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        }
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

    protected SubscriptionFeedGenerator
        createSubscriptionFeedGenerator(ServiceLocator locator) {
        return getGeneratorFactory().
            createSubscriptionFeedGenerator(locator);
    }

    private CollectionSubscription readSubscription(Entry entry) {
        CollectionSubscription sub = new CollectionSubscription();

        sub.setDisplayName(entry.getTitle());
        sub.setTicketKey(entry.getSimpleExtension(QN_TICKET));
        sub.setCollectionUid(entry.getSimpleExtension(QN_COLLECTION));

        return sub;
    }
}
