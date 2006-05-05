/*
 * Copyright 2005 Open Source Applications Foundation
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
package org.osaf.cosmo.cmp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.xml.Namespace;

import org.w3c.dom.Element;

import org.osaf.cosmo.model.User;

/**
 * Extends {@link UserContent} to use the {@link UserResource.NS_CMP0}
 * XML namespace.
 */
public class UserContent0 extends UserContent {
    private static final Log log = LogFactory.getLog(UserContent0.class);

    /**
     */
    public UserContent0(User user) {
        super(user);
    }

    /**
     */
    public Namespace getNamespace() {
        return UserResource.NS_CMP0;
    }
}
