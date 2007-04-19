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
package org.osaf.cosmo.atom.provider.mock;

import org.apache.abdera.protocol.server.ServiceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.atom.provider.CollectionTarget;
import org.osaf.cosmo.model.CollectionItem;

/**
 * Mock implementation of {@link RequestContext}.
 */
public class MockCollectionRequestContext extends BaseMockRequestContext {
    private static final Log log =
        LogFactory.getLog(MockCollectionRequestContext.class);

    public MockCollectionRequestContext(ServiceContext context,
                                        CollectionItem collection,
                                        String method) {
        this(context, collection, method, null, null);
    }

    public MockCollectionRequestContext(ServiceContext context,
                                        CollectionItem collection,
                                        String method,
                                        String projection,
                                        String format) {
        super(context, method, toRequestUri(collection.getUid()));
        this.target = new CollectionTarget(this, collection, projection,
                                           format);
    }

    public MockCollectionRequestContext(ServiceContext context,
                                        String uid,
                                        String method) {
        this(context, uid, method, null, null);
    }

    public MockCollectionRequestContext(ServiceContext context,
                                        String uid,
                                        String method,
                                        String projection,
                                        String format) {
        this(context, makeCollection(uid), method, projection, format);
    }

    private static String toRequestUri(String uid) {
        return "/collection/" + uid;
    }

    private static CollectionItem makeCollection(String uid) {
        CollectionItem collection = new CollectionItem();
        collection.setUid(uid);
        return collection;
    }
}
