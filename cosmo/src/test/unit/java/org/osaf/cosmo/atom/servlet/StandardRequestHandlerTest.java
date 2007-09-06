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
package org.osaf.cosmo.atom.servlet;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.abdera.protocol.server.Provider;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.atom.AtomHelper;
import org.osaf.cosmo.atom.provider.mock.MockCollectionRequestContext;
import org.osaf.cosmo.atom.provider.mock.MockProvider;
import org.osaf.cosmo.model.CollectionItem;

import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test class for {@link StandardRequestHandler}.
 */
public class StandardRequestHandlerTest extends TestCase {
    private static final Log log =
        LogFactory.getLog(StandardRequestHandlerTest.class);

    private AtomHelper helper;
    private StandardRequestHandler handler;

    public void SKIPtestIfMatchAll() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        helper.setIfMatch(req, "*");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertTrue("Preconditions failed", rv);
        assertEquals("Incorrect response status", 200, res.getStatus());
    }

    public void testIfMatchOk() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        helper.setIfMatch(req, collection);
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertTrue("Preconditions failed", rv);
        assertEquals("Incorrect response status", 200, res.getStatus());
    }

    public void testIfMatchNotOk() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        helper.setIfMatch(req, "aeiou");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertFalse("Preconditions passed", rv);
        assertEquals("Incorrect response status", 412, res.getStatus());
        assertNotNull("Null ETag header", res.getHeader("ETag"));
    }

    public void SKIPtestIfNoneMatchAll() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        helper.setIfNoneMatch(req, "*");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertFalse("Preconditions passed", rv);
        assertEquals("Incorrect response status", 304, res.getStatus());
        assertNotNull("Null ETag header", res.getHeader("ETag"));
    }

    public void testIfNoneMatchNotOk() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        helper.setIfNoneMatch(req, collection);
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertFalse("Preconditions passed", rv);
        assertEquals("Incorrect response status", 304, res.getStatus());
        assertNotNull("Null ETag header", res.getHeader("ETag"));
    }

    public void testIfNoneMatchOk() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        helper.setIfNoneMatch(req, "aeiou");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertTrue("Preconditions failed", rv);
        assertEquals("Incorrect response status", 200, res.getStatus());
    }

    public void testIfModifiedSinceAfter() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        Date date = new Date(System.currentTimeMillis()-1000000);
        helper.setIfModifiedSince(req, date);
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertTrue("Preconditions failed", rv);
        assertEquals("Incorrect response status", 200, res.getStatus());
    }

    public void testIfModifiedSinceBefore() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        Date date = new Date(System.currentTimeMillis()+1000000);
        helper.setIfModifiedSince(req, date);
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertFalse("Preconditions failed", rv);
        assertEquals("Incorrect response status", 304, res.getStatus());
    }

    public void testIfUnmodifiedSinceAfter() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        Date date = new Date(System.currentTimeMillis()-1000000);
        helper.setIfUnmodifiedSince(req, date);
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertFalse("Preconditions failed", rv);
        assertEquals("Incorrect response status", 412, res.getStatus());
    }

    public void testIfUnmodifiedSinceBefore() throws Exception {
        CollectionItem collection = helper.makeAndStoreDummyCollection();
        RequestContext req = createRequestContext(collection);
        Date date = new Date(System.currentTimeMillis()+1000000);
        helper.setIfUnmodifiedSince(req, date);
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean rv = handler.preconditions(helper.getProvider(req), req, res);
        assertTrue("Preconditions failed", rv);
        assertEquals("Incorrect response status", 200, res.getStatus());
    }

    protected void setUp() throws Exception {
        helper = new AtomHelper();
        helper.setUp();

        handler = new StandardRequestHandler();
    }

    protected void tearDown() throws Exception {
        helper.tearDown();
    }

    public RequestContext createRequestContext(CollectionItem collection) {
        return new MockCollectionRequestContext(helper.getServiceContext(),
                                                collection, "GET", "yyz",
                                                "eff");
    }

    public RequestContext createPutRequestContext(CollectionItem collection) {
        return new MockCollectionRequestContext(helper.getServiceContext(),
                                                collection, "PUT");
    }
}
