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

import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.atom.AtomConstants;
import org.osaf.cosmo.atom.provider.mock.MockCollectionRequestContext;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.NoteItem;

/**
 * Test class for {@link ItemProvider#createEntry()} tests.
 */
public class CreateItemTest extends BaseItemProviderTestCase
    implements AtomConstants {
    private static final Log log = LogFactory.getLog(CreateItemTest.class);

    public void testCreateEntry() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        NoteItem item = helper.makeDummyItem(collection.getOwner());
        RequestContext req = createRequestContext(collection, item);

        ResponseContext res = provider.createEntry(req);
        assertNotNull("Null response context", res);
        assertEquals("Incorrect response status", 201, res.getStatus());
        assertNotNull("Null etag", res.getEntityTag());
        assertNotNull("Null last modified", res.getLastModified());
        assertNotNull("Null Location header", res.getHeader("Location"));
        assertNotNull("Null Content-Location header",
                      res.getHeader("Content-Location"));
    }

    public void testUnsupportedContentType() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        NoteItem item = helper.makeDummyItem(collection.getOwner());
        RequestContext req = createRequestContext(collection, item);
        helper.forgetContentTypes();

        ResponseContext res = provider.createEntry(req);
        assertNotNull("Null response context", res);
        assertEquals("Incorrect response status", 400, res.getStatus());
    }

    public void testInvalidContent() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        NoteItem item = helper.makeDummyItem(collection.getOwner());
        RequestContext req = createRequestContext(collection, item);
        helper.enableProcessorValidationError();

        ResponseContext res = provider.createEntry(req);
        assertNotNull("Null response context", res);
        assertEquals("Incorrect response status", 400, res.getStatus());
    }

    public void testUidInUse() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        NoteItem item1 = helper.makeAndStoreDummyItem(collection);
        NoteItem item2 = helper.makeDummyItem(collection.getOwner());
        item2.setUid(item1.getUid());
        RequestContext req = createRequestContext(collection, item2);

        ResponseContext res = provider.createEntry(req);
        assertNotNull("Null response context", res);
        assertEquals("Incorrect response status", 409, res.getStatus());
    }

    public void testProcessingError() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        NoteItem item = helper.makeDummyItem(collection.getOwner());
        RequestContext req = createRequestContext(collection, item);
        helper.enableProcessorFailure();

        ResponseContext res = provider.createEntry(req);
        assertNotNull("Null response context", res);
        assertEquals("Incorrect response status", 500, res.getStatus());
    }

    public void DONTtestLockedError() throws Exception {
        // XXX re-enable when i figure out how to lock the collection
        // from another thread
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        NoteItem item = helper.makeDummyItem(collection.getOwner());
        RequestContext req = createRequestContext(collection, item);
        helper.lockCollection(collection);

        ResponseContext res = provider.createEntry(req);
        assertNotNull("Null response context", res);
        assertEquals("Incorrect response status", 423, res.getStatus());
    }

    public void testGenerationError() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        NoteItem item = helper.makeDummyItem(collection.getOwner());
        RequestContext req = createRequestContext(collection, item);
        helper.enableGeneratorFailure();

        ResponseContext res = provider.createEntry(req);
        assertNotNull("Null response context", res);
        assertEquals("Incorrect response status", 500, res.getStatus());
    }

    protected void setUp() throws Exception {
        super.setUp();

        helper.rememberProjection(PROJECTION_FULL);
        helper.rememberContentType(Content.Type.TEXT.toString());
    }

    private RequestContext createRequestContext(CollectionItem collection,
                                                NoteItem item)
        throws Exception {
        MockCollectionRequestContext rc =
            new MockCollectionRequestContext(helper.getServiceContext(),
                                             collection, "POST");
        rc.setPropertiesAsEntry(serialize(item));
        return rc;
    }
}
