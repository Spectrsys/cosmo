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
package org.osaf.cosmo.dao.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.model.CollectionResource;
import org.osaf.cosmo.model.FileResource;
import org.osaf.cosmo.model.Resource;

/**
 * Utility class that converts between {@link Resource}s and
 * {@link javax.jcr.Node}s.
 *
 * Maps {@link FileResource} to a <code>nt:file</code> node with the
 * <code>dav:resource</code> and <code>ticket:ticketable</code> mixin
 * types and a <code>jcr:content</code> child node with type
 * <code>nt:resource</code>.
 *
 * Maps {@link CollectionResource} to a <code>nt:folder</code> node
 * with the <code>dav:collection</code> and
 * <code>ticket:ticketable</code> mixin types.
 */
public class JcrResourceMapper implements JcrConstants {
    private static final Log log = LogFactory.getLog(JcrResourceMapper.class);

    /**
     * Returns a new instance of <code>Resource</code> populated from a
     * resource node. If the resource is a collection, include its
     * child resources to the indicated depth (0 meaning no
     * children).
     */
    public static Resource nodeToResource(Node node, int depth)
        throws RepositoryException {
        if (node.getPath().equals("/") ||
            node.isNodeType(NT_DAV_COLLECTION)) {
            return nodeToCollection(node, depth);
        }
        return nodeToFile(node);
    }

    /**
     * Returns a new instance of <code>Resource</code> populated from a
     * resource node. If the resource is a collection, include its no
     * children but no further descendents.
     */
    public static Resource nodeToResource(Node node)
        throws RepositoryException {
        return nodeToResource(node, 1);
    }

    /**
     * Copies the properties of a <code>Resource</code> into a resource
     * node.
     */
    public static void resourceToNode(Resource resource,
                                      Node node)
        throws RepositoryException {
        node.setProperty(NP_DAV_DISPLAYNAME, resource.getDisplayName());
        // XXX: all other properties
    }

    private static void setCommonResourceAttributes(Resource resource,
                                                    Node node)
        throws RepositoryException {
        resource.setPath(JcrEscapist.hexUnescapeJcrPath(node.getPath()));
        resource.setDisplayName(node.getProperty(NP_DAV_DISPLAYNAME).
                                getString());
        resource.setDateCreated(node.getProperty(NP_JCR_CREATED).getDate().
                                getTime());
        // all other properties
    }

    private static FileResource nodeToFile(Node node)
        throws RepositoryException {
        FileResource resource = new FileResource();
        setCommonResourceAttributes(resource, node);

        Node contentNode = node.getNode(NN_JCR_CONTENT);
        resource.setDateModified(contentNode.getProperty(NP_JCR_LASTMODIFIED).
                                 getDate().getTime());
        resource.setContentType(contentNode.getProperty(NP_JCR_MIMETYPE).
                                getString());
        if (contentNode.hasProperty(NP_JCR_ENCODING)) {
            resource.setContentEncoding(contentNode.
                                        getProperty(NP_JCR_ENCODING).
                                        getString());
        }
        if (contentNode.hasProperty(NP_DAV_CONTENTLANGUAGE)) {
            resource.setContentLanguage(contentNode.
                                        getProperty(NP_DAV_CONTENTLANGUAGE).
                                        getString());
        }
        Property content = contentNode.getProperty(NP_JCR_DATA);
        resource.setContentLength(new Long(content.getLength()));
        resource.setContent(content.getStream());

        return resource;
    }

    private static CollectionResource nodeToCollection(Node node,
                                                       int depth)
        throws RepositoryException {
        CollectionResource collection = new CollectionResource();

        if (node.getPath().equals("/")) {
            collection.setDisplayName("/");
            collection.setPath("/");
            // JCR 1.0 does not define a standard node type for the
            // root node, so we have no way of knowing what it's
            // creation date was or whether it has extra properties
        }
        else {
            setCommonResourceAttributes(collection, node);
        }

        if (depth > 0) {
            for (NodeIterator i=node.getNodes(); i.hasNext();) {
                Node child = i.nextNode();
                if (child.isNodeType(NT_DAV_COLLECTION) ||
                    child.isNodeType(NT_DAV_RESOURCE)) {
                    collection.addResource(nodeToResource(child, depth-1));
                }
                else {
                    if (log.isDebugEnabled()) {
                        log.debug("skipping child node of type " +
                                  node.getPrimaryNodeType().getName());
                    }
                }
            }
        }

        return collection;
    }
}
