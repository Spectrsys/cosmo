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

import org.apache.abdera.protocol.EntityTag;
import org.apache.abdera.protocol.server.provider.RequestContext;
import org.apache.abdera.protocol.server.provider.TargetType;
import org.apache.abdera.protocol.server.provider.TargetType;

import org.osaf.cosmo.model.NoteItem;

public class ExpandedItemTarget extends BaseItemTarget {

    private NoteItem item;

    public ExpandedItemTarget(RequestContext request,
                              NoteItem item) {
        this(request, item, null, null);
    }

    public ExpandedItemTarget(RequestContext request,
                              NoteItem item,
                              String projection,
                              String format) {
        super(TargetType.TYPE_COLLECTION, request, projection, format);
        this.item = item;
    }

    public NoteItem getItem() {
        return item;
    }

    public EntityTag getEntityTag() {
        return item != null ? new EntityTag(item.getEntityTag()) : null;
    }
}