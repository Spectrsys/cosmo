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
package org.osaf.cosmo.dav.io;

/**
 * An exception indicating that a resource submitted for import was
 * not of a media type supported by the server in the specified
 * location.
 */
public class UnsupportedMediaTypeException extends RuntimeException {

    private String mediaType;

    /**
     */
    public UnsupportedMediaTypeException(String mediaType) {
        super(mediaType);
        this.mediaType = mediaType;
    }

    /**
     */
    public String getMediaType() {
        return mediaType;
    }
}
