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
 * See the License for the specific language governing permissions and!
 * limitations under the License.
 */
package org.osaf.cosmo.api;

import org.apache.jackrabbit.webdav.xml.Namespace;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An interface for Cosmo API resources.
 */
public interface CosmoApiResource extends XmlSerializable {

    /**
     */
    public static final Namespace NS_COSMO =
        Namespace.getNamespace("http://osafoundation.org/cosmo");

    /**
     * Returns the entity instance that backs this resource.
     */
    public Object getEntity();
}
