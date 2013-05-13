/*
 * Copyright 2006 Open Source Applications Foundation
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
package org.osaf.cosmo.ui;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.osaf.cosmo.utils.CosmoConstants;

/**
 * A servlet filter that sets a response header containing the Cosmo
 * version.
 */
public class VersionHeaderFilter implements Filter {

    /**
     * The response header that contains the Cosmo version:
     * <code>X-Cosmo-Version</code>
     */
    public static final String HEADER_VERSION = "X-Cosmo-Version";

    /**
     */
    public void init(FilterConfig filterConfig)
        throws ServletException {
        // does nothing
    }

    /**
     */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
        throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            ((HttpServletResponse)response).
                setHeader(HEADER_VERSION, CosmoConstants.PRODUCT_VERSION);
        }
        chain.doFilter(request, response);
    }

    /**
     */
    public void destroy() {
        // does nothing
    }
}
