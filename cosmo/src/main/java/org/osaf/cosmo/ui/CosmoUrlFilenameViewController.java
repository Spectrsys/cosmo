/*
 * Copyright 2006-2008 Open Source Applications Foundation
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

import org.springframework.web.servlet.mvc.UrlFilenameViewController;

/**
 * Default controller that attempts to find a jsp at a path specified
 * by transforming _ characters in the view name to /
 * 
 * For example, the view x_y_z would resolve to a file at x/y/z
 * 
 * @author Travis Vachon travis@osafoundation.org
 *
 */
public class CosmoUrlFilenameViewController extends UrlFilenameViewController {
	
    private String COSMO_VIEW_PATH_SEPARATOR = "_";

	protected String extractViewNameFromUrlPath(String uri) {
        if (uri.startsWith("/")){
            uri = uri.substring(1);
        }
        
        int dotIndex = uri.lastIndexOf('.');
        if (dotIndex != -1) {
            uri = uri.substring(0, dotIndex);
        }
        
        uri = uri.replace("/", COSMO_VIEW_PATH_SEPARATOR );
        
        if (uri.endsWith(COSMO_VIEW_PATH_SEPARATOR)){
        	uri = uri.substring(0, uri.length() - 1);
        	
        }
        
        return uri;
    }
    
}
