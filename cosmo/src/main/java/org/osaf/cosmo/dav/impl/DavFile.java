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
package org.osaf.cosmo.dav.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;

import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.io.DavInputContext;
import org.osaf.cosmo.model.DataSizeException;
import org.osaf.cosmo.model.FileItem;
import org.osaf.cosmo.model.ModelValidationException;

/**
 * Extends <code>DavResourceBase</code> to adapt the Cosmo
 * <code>FileItem</code> to the DAV resource model.
 *
 * This class defines the following live properties:
 *
 * <ul>
 * <li><code>DAV:getcontentlanguage</code></li>
 * <li><code>DAV:getcontentlength</code> (protected)</li>
 * <li><code>DAV:getcontenttype</code></li>
 * </ul>
 *
 * @see DavContent
 * @see FileItem
 */
public class DavFile extends DavContentBase {
    private static final Log log = LogFactory.getLog(DavFile.class);

    static {
        registerLiveProperty(DavPropertyName.GETCONTENTLANGUAGE);
        registerLiveProperty(DavPropertyName.GETCONTENTLENGTH);
        registerLiveProperty(DavPropertyName.GETCONTENTTYPE);
    }

    /** */
    public DavFile(FileItem item,
                   DavResourceLocator locator,
                   DavResourceFactory factory) {
        super(item, locator, factory);
    }

    /** */
    public DavFile(DavResourceLocator locator,
                   DavResourceFactory factory) {
        this(new FileItem(), locator, factory);
    }

    // DavResource

   
    /** */
    public void spool(OutputContext outputContext)
        throws IOException {
        if (! exists())
            throw new IllegalStateException("cannot spool a nonexistent resource");

        if (log.isDebugEnabled())
            log.debug("spooling file " + getResourcePath());

        FileItem content = (FileItem) getItem();

        String contentType =
            IOUtil.buildContentType(content.getContentType(),
                                    content.getContentEncoding());
        outputContext.setContentType(contentType);

        if (content.getContentLanguage() != null)
            outputContext.setContentLanguage(content.getContentLanguage());

        outputContext.setContentLength(content.getContentLength().longValue());
        outputContext.setModificationTime(getModificationTime());
        outputContext.setETag(getETag());

        if (! outputContext.hasStream())
            return;

        IOUtil.spool(content.getContentInputStream(),
                     outputContext.getOutputStream());
    }

    
    /** */
    protected void populateItem(InputContext inputContext)
        throws DavException {
        super.populateItem(inputContext);

        FileItem file = (FileItem) getItem();

        try {
            InputStream content = inputContext.getInputStream();
            if (content != null)
                file.setContent(content);

            if (inputContext.getContentLanguage() != null)
                file.setContentLanguage(inputContext.getContentLanguage());

            String contentType = inputContext.getContentType();
            if (contentType != null)
                file.setContentType(IOUtil.getMimeType(contentType));
            else
                file.setContentType(IOUtil.MIME_RESOLVER.
                                    getMimeType(file.getName()));

            String contentEncoding = IOUtil.getEncoding(contentType);
            if (contentEncoding != null)
                file.setContentEncoding(contentEncoding);
        } catch (IOException e) {
           throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot save content: " + e.getMessage());
        } catch (DataSizeException e) {
           throw new DavException(DavServletResponse.SC_FORBIDDEN, "Cannot store resource attribute: " + e.getMessage());
        }
    }

    /** */
    protected void loadLiveProperties() {
        super.loadLiveProperties();

        FileItem content = (FileItem) getItem();
        if (content == null)
            return;

        DavPropertySet properties = getProperties();

        if (content.getContentLanguage() != null) {
            properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLANGUAGE,
                                                  content.getContentLanguage()));
        }

        properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH,
                                              content.getContentLength()));

        properties.add(new DefaultDavProperty(DavPropertyName.GETETAG,
                                              getETag()));

        String contentType =
            IOUtil.buildContentType(content.getContentType(),
                                    content.getContentEncoding());
        properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE,
                                              contentType));

        long modTime = getModificationTime();
        properties.add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED,
                                              IOUtil.getLastModified(modTime)));
    }

    /** */
    protected void setLiveProperty(DavProperty property) {
        super.setLiveProperty(property);

        FileItem content = (FileItem) getItem();
        if (content == null)
            return;

        DavPropertyName name = property.getName();
        String value = property.getValue().toString();

        if (name.equals(DavPropertyName.GETCONTENTLANGUAGE)) {
            content.setContentLanguage(value);
            return;
        }

        if (name.equals(DavPropertyName.GETCONTENTTYPE)) {
            String type = IOUtil.getMimeType(value);
            if (type == null)
                throw new ModelValidationException("null mime type for property " + name);
            content.setContentType(type);
            content.setContentEncoding(IOUtil.getEncoding(value));
        }
    }

    /** */
    protected void removeLiveProperty(DavPropertyName name) {
        super.removeLiveProperty(name);

        FileItem content = (FileItem) getItem();
        if (content == null)
            return;

        if (name.equals(DavPropertyName.GETCONTENTLANGUAGE)) {
            content.setContentLanguage(null);
            return;
        }

        if (name.equals(DavPropertyName.GETCONTENTTYPE))
            throw new ModelValidationException("cannot remove property " + name);
    }
}
