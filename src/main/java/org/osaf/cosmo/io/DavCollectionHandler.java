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
package org.osaf.cosmo.io;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;

import org.osaf.cosmo.jcr.CosmoJcrConstants;
import org.osaf.cosmo.jcr.JCREscapist;

/**
 * Implements {@link org.apache.jackrabbit.server.io.IOHandler}
 * to provide custom logic for importing and exporting dav
 * collections.
 */
public class DavCollectionHandler implements IOHandler {
    private static final Log log =
        LogFactory.getLog(DavCollectionHandler.class);

    private IOManager manager;

    /**
     */
    public DavCollectionHandler(IOManager manager) {
        this.manager = manager;
    }

    // IOHandler methods

    /**
     */
    public IOManager getIOManager() {
        return manager;
    }

    /**
     */
    public String getName() {
        return getClass().getName();
    }

    /**
     * Returns true if the import root is a dav collection node.
     */
    public boolean canImport(ImportContext context,
                             boolean isCollection) {
        if (context == null || context.isCompleted() ||
            context.getSystemId() == null || ! isCollection) {
            return false;
        }
        Item importRoot = context.getImportRoot();
        if (importRoot == null || ! importRoot.isNode()) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if the resource represents a collection.
     */
    public boolean canImport(ImportContext context,
                             DavResource resource) {
        if (resource == null) {
            return false;
        }
        return canImport(context, resource.isCollection());
    }

    /**
     * Imports a dav collection node beneath the import context root.
     */
    public boolean importContent(ImportContext context,
                                 boolean isCollection)
        throws IOException {
        if (! canImport(context, isCollection)) {
            return false;
        }
        try {
            Node parentNode = (Node) context.getImportRoot();
            String name = context.getSystemId();
            if (parentNode.hasNode(name)) {
                return false;
            }
            Node collectionNode =
                parentNode.addNode(name, CosmoJcrConstants.NT_DAV_COLLECTION);
            collectionNode.addMixin(CosmoJcrConstants.NT_TICKETABLE);
            collectionNode.setProperty(CosmoJcrConstants.NP_DAV_DISPLAYNAME,
                                       JCREscapist.hexUnescapeJCRNames(name));
            return true;
        } catch (RepositoryException e) {
            log.error("unable to import dav collection", e);
            throw new IOException("unable to import dav collection: " +
                                  e.getMessage());
        }
    }

    /**
     * Imports a dav collection node corresponding to the given
     * resource.
     */
    public boolean importContent(ImportContext context,
                                 DavResource resource)
        throws IOException {
        if (resource == null) {
            return false;
        }
        return importContent(context, resource.isCollection());
    }

    /**
     * Returns true if the export root is a dav collection node or the
     * repository root node.
     */
    public boolean canExport(ExportContext context,
                             boolean isCollection) {
        if (context == null || context.isCompleted() || ! isCollection) {
            return false;
        }
        Item exportRoot = context.getExportRoot();
        if (exportRoot == null || ! exportRoot.isNode()) {
            return false;
        }
        try {
            Node exportNode = (Node) exportRoot;
            if (! (exportNode.isNodeType(CosmoJcrConstants.NT_DAV_COLLECTION) ||
                   exportRoot.getPath().equals("/"))) {
                return false;
            }
            return true;
        } catch (RepositoryException e) {
            log.error("undetermined repository exception", e);
            return false;
        }
    }

    /**
     * Returns true if the resource represents a collection.
     */
    public boolean canExport(ExportContext context,
                             DavResource resource) {
        if (resource == null) {
            return false;
        }
        return canExport(context, resource.isCollection());
    }

    /**
     * Generates an HTML listing of the contents of the collection
     * resource.
     */
    public boolean exportContent(ExportContext context,
                                 DavResource resource)
        throws IOException {
        if (resource == null) {
            return false;
        }
        if (! canExport(context, resource.isCollection())) {
            return false;
        }
        if (! context.hasStream()) {
            return true;
        }
        context.setContentType("text/html", "UTF-8");
        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(context.getOutputStream(),
                                                   "utf8"));
        String title = resource.getLocator().getResourcePath(); 
        writer.write("<html><head><title>");
        writer.write(title); // XXX: html escape
        writer.write("</title></head>");
        writer.write("<body>");
        writer.write("<h1>");
        writer.write(title); // XXX: html escape
        writer.write("</h1>");
        writer.write("<ul>");
        if (! resource.getLocator().getResourcePath().equals("/")) {
            writer.write("<li><a href=\"../\">..</a></li>");
        }
        for (DavResourceIterator i=resource.getMembers(); i.hasNext();) {
            DavResource child = i.nextResource();
            String name = getResourceName(child.getLocator().
                                          getResourcePath()); 
            writer.write("<li><a href=\"");
            try {
                writer.write(URLEncoder.encode(name, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.warn("UTF-8 not supported?!", e);
                writer.write(name);
            }
            if (child.isCollection()) {
                writer.write("/");
            }
            writer.write("\">");
            writer.write(name);
            writer.write("</a></li>");
        }
        writer.write("</ul>");
        writer.write("</body>");
        writer.write("</html>");
        writer.write("\n");
        writer.close();

        return true;
    }

    /**
     * Unimplemented.
     */
    public boolean exportContent(ExportContext context,
                                 boolean isCollection)
        throws IOException {
        throw new RuntimeException("unimplemented");
    }

    // private methods

    private String getResourceName(String path) {
        int pos = path.lastIndexOf('/');
        return pos >= 0 ? path.substring(pos + 1) : "";
    }

}
