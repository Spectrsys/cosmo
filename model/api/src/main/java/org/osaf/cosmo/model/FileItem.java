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
package org.osaf.cosmo.model;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extends {@link Item} to represent an item containing binary content.
 */
public interface FileItem extends ContentItem{

    // max content size is smaller than binary attribute value max
    // size
    long MAX_CONTENT_SIZE = 10 * 1024 * 1024;

    
    /**
     * Get content data as byte[]
     */
    byte[] getContent();

    /**
     * Sets content data using byte[]
     * @param content
     */
    void setContent(byte[] content);

    void clearContent();

    /**
     * Set ContentItem's data using InputStream.  The provided InputStream
     * is not closed.
     * @param is data
     * @throws IOException
     */
    void setContent(InputStream is) throws IOException;

    InputStream getContentInputStream();

    String getContentEncoding();

    void setContentEncoding(String contentEncoding);

    String getContentLanguage();

    void setContentLanguage(String contentLanguage);

    Long getContentLength();

    void setContentLength(Long contentLength);

    String getContentType();

    void setContentType(String contentType);

}