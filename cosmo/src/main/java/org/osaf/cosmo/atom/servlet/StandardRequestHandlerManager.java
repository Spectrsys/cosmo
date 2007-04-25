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

import org.apache.abdera.protocol.server.servlet.RequestHandler;
import org.apache.abdera.protocol.server.servlet.RequestHandlerManager;

public class StandardRequestHandlerManager implements RequestHandlerManager {

    private RequestHandler requestHandler;

    // RequestHandlerManager methods

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public void release(RequestHandler requestHandler) {}

    // our methods

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public void init() {
        if (requestHandler == null)
            throw new IllegalStateException("requestHandler is required");
    }
}
