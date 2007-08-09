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

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.ParseException;
import org.apache.abdera.protocol.server.provider.RequestContext;
import org.apache.abdera.protocol.server.provider.ResponseContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.atom.generator.ItemFeedGenerator;
import org.osaf.cosmo.atom.generator.GeneratorException;
import org.osaf.cosmo.atom.generator.UnsupportedFormatException;
import org.osaf.cosmo.atom.generator.UnsupportedProjectionException;
import org.osaf.cosmo.atom.processor.ContentProcessor;
import org.osaf.cosmo.atom.processor.ProcessorException;
import org.osaf.cosmo.atom.processor.UnsupportedContentTypeException;
import org.osaf.cosmo.atom.processor.ValidationException;
import org.osaf.cosmo.model.CollectionLockedException;
import org.osaf.cosmo.model.NoteItem;
import org.osaf.cosmo.model.UidInUseException;
import org.osaf.cosmo.server.ServiceLocator;

public class DetachedItemProvider extends ItemProvider {
    private static final Log log =
        LogFactory.getLog(DetachedItemProvider.class);
    private static final String[] ALLOWED_COLL_METHODS =
        new String[] { "OPTIONS" };
    private static final String[] ALLOWED_ENTRY_METHODS =
        new String[] { "OPTIONS", "POST" };

    // Provider methods

    public ResponseContext createEntry(RequestContext request) {
        DetachedItemTarget target = (DetachedItemTarget) request.getTarget();
        NoteItem master = target.getMaster();
        NoteItem occurrence = target.getOccurrence();

        if (log.isDebugEnabled())
            log.debug("detaching occurrence " + occurrence.getUid() +
                      " from master " + master.getUid());

        ResponseContext frc = checkEntryWritePreconditions(request);
        if (frc != null)
            return frc;

        try {
            // XXX: does abdera automatically resolve external content?
            Entry entry = (Entry) request.getDocument().getRoot();
            ContentProcessor processor = createContentProcessor(entry);
            NoteItem detached = (NoteItem) master.copy();
            processor.processContent(entry.getContent(), detached);
            detached = detachOccurrence(master, detached, occurrence);

            ItemTarget itemTarget =
                new ItemTarget(request, detached, target.getProjection(),
                               target.getFormat());
            ServiceLocator locator = createServiceLocator(request);
            ItemFeedGenerator generator =
                createItemFeedGenerator(itemTarget, locator);
            entry = generator.generateEntry(detached);

            return created(entry, detached, locator);
        } catch (IOException e) {
            String reason = "Unable to read request content: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        } catch (UnsupportedContentTypeException e) {
            return badrequest(getAbdera(), request, "Entry content type must be one of " + StringUtils.join(getProcessorFactory().getSupportedContentTypes(), ", "));
        } catch (ParseException e) {
            String reason = "Unparseable content: ";
            if (e.getCause() != null)
                reason = reason + e.getCause().getMessage();
            else
                reason = reason + e.getMessage();
            return badrequest(getAbdera(), request, reason);
        } catch (ValidationException e) {
            String reason = "Invalid content: ";
            if (e.getCause() != null)
                reason = reason + e.getCause().getMessage();
            else
                reason = reason + e.getMessage();
            return badrequest(getAbdera(), request, reason);
        } catch (UidInUseException e) {
            return conflict(getAbdera(), request, "Uid already in use");
        } catch (ProcessorException e) {
            String reason = "Unknown content processing error: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        } catch (CollectionLockedException e) {
            return locked(getAbdera(), request);
        } catch (GeneratorException e) {
            String reason = "Unknown entry generation error: " + e.getMessage();
            log.error(reason, e);
            return servererror(getAbdera(), request, reason, e);
        }
    }

    public ResponseContext deleteEntry(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_ENTRY_METHODS);
    }
  
    public ResponseContext deleteMedia(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    public ResponseContext updateEntry(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_ENTRY_METHODS);
    }
  
    public ResponseContext updateMedia(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext getService(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    public ResponseContext getFeed(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_COLL_METHODS);
    }

    public ResponseContext getEntry(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_ENTRY_METHODS);
    }
  
    public ResponseContext getMedia(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext getCategories(RequestContext request) {
        throw new UnsupportedOperationException();
    }
  
    public ResponseContext entryPost(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_ENTRY_METHODS);
    }
  
    public ResponseContext mediaPost(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_ENTRY_METHODS);
    }

    // ExtendedProvider methods

    public ResponseContext createCollection(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_COLL_METHODS);
    }

    public ResponseContext updateCollection(RequestContext request) {
        return methodnotallowed(getAbdera(), request, ALLOWED_COLL_METHODS);
    }

    // our methods

    // detaches on occurrence from a recurrence master:
    // - master is the original recurring item
    // - copy is a copy of the master with changes applied
    // - occurrence is the "detachment point"; may be a modification (in which
    //   case it has its own changes to the original master which don't apply
    //   to the copy) or an occurrence
    // * all modifications of the original master that occur after the
    //   detachment point are moved to the copy
    // * the recurrence rule of the master is updated to end at the latest
    //   occurrence before the detachment point
    // * the copy's recurrence rule is updated to begin at the detachment
    //   point (or the next occurrence after the detachment point, if that is
    //   a modification)
    // * the copy is added to all of the master's collections
    private NoteItem detachOccurrence(NoteItem master,
                                      NoteItem copy,
                                      NoteItem occurrence) {
        // XXX
        return copy;
    }
}
