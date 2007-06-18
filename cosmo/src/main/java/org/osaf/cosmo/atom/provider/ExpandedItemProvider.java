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

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.provider.AbstractResponseContext;
import org.apache.abdera.protocol.server.provider.RequestContext;
import org.apache.abdera.protocol.server.provider.ResponseContext;
import org.apache.abdera.util.EntityTag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.atom.generator.ItemFeedGenerator;
import org.osaf.cosmo.atom.generator.GeneratorException;
import org.osaf.cosmo.atom.generator.UnsupportedFormatException;
import org.osaf.cosmo.atom.generator.UnsupportedProjectionException;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.server.ServiceLocator;

public class ExpandedItemProvider extends ItemProvider {
    private static final Log log =
        LogFactory.getLog(ExpandedItemProvider.class);
    private static final String[] ALLOWED_METHODS =
        new String[] { "GET", "HEAD" };

    // Provider methods

    public ResponseContext createEntry(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }

    public ResponseContext deleteEntry(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }
  
    public ResponseContext deleteMedia(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }

    public ResponseContext updateEntry(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }
  
    public ResponseContext updateMedia(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }
  
    public ResponseContext getService(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }

    public ResponseContext getFeed(RequestContext request) {
        ExpandedItemTarget target = (ExpandedItemTarget) request.getTarget();
        NoteItem item = target.getItem();
        if (log.isDebugEnabled())
            log.debug("getting expanded feed for item " + item.getUid());

        try {
            ServiceLocator locator = createServiceLocator(request);
            ItemFeedGenerator generator =
                createItemFeedGenerator(target, locator);
            generator.setFilter(createQueryFilter(request));

            Feed feed = generator.generateFeed(item);

            AbstractResponseContext rc =
                createResponseContext(feed.getDocument());
            rc.setEntityTag(new EntityTag(item.getEntityTag()));
            rc.setLastModified(item.getModifiedDate());
            return rc;
        } catch (InvalidQueryException e) {
            return badrequest(getAbdera(), request, e.getMessage());
        } catch (UnsupportedProjectionException e) {
            String reason = "Projection " + target.getProjection() + " not supported";
            return badrequest(getAbdera(), request, reason);
        } catch (UnsupportedFormatException e) {
            String reason = "Format " + target.getFormat() + " not supported";
            return badrequest(getAbdera(), request, reason);
        } catch (GeneratorException e) {
            String reason = "Unknown feed generation error: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        }
    }

    public ResponseContext getEntry(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }
  
    public ResponseContext getMedia(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }
  
    public ResponseContext getCategories(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }
  
    public ResponseContext entryPost(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }
  
    public ResponseContext mediaPost(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }

    // ExtendedProvider methods

    public ResponseContext updateCollection(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_METHODS);
    }
}
