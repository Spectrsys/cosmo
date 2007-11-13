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

import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.AbstractTarget;

import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.User;

/**
 * A target that identifies a particular user and home collection for which
 * a collection is to be creatd.
 */
public class NewCollectionTarget extends AbstractTarget {

    private User user;
    private HomeCollectionItem home;

    /**
     * Constructs a <code>NewCollectionTarget</code> of type
     * {@link TargetType.TYPE_SERVICE}.
     */
    public NewCollectionTarget(RequestContext request,
                               User user,
                               HomeCollectionItem home) {
        this(TargetType.TYPE_SERVICE, request, user, home);
    }

    public NewCollectionTarget(TargetType type,
                               RequestContext request,
                               User user,
                               HomeCollectionItem home) {
        super(type, request);
        this.user = user;
        this.home = home;
    }

    public User getUser() {
        return user;
    }

    public HomeCollectionItem getHomeCollection() {
        return home;
    }
}
