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
package org.osaf.cosmo.dao;

import java.util.Set;

import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.User;

/**
 * Interface for DAO that provides base operations for content items.
 * 
 * A content item is either a piece of content (or file) or a collection
 * containing content items or other collection items.
 * 
 */
public interface ContentDao extends ItemDao {

    /**
     * Create a new collection.
     * 
     * @param parent
     *            parent of collection.
     * @param collection
     *            collection to create
     * @return newly created collection
     */
    public CollectionItem createCollection(CollectionItem parent,
            CollectionItem collection);

    /**
     * Update an existing collection.
     * 
     * @param collection
     *            collection to update
     * @return updated collection
     */
    public CollectionItem updateCollection(CollectionItem collection);

    /**
     * Find collection by hierachical path. Path must be in the form:
     * /username/parent1/parent2/collectionname
     * 
     * @param path
     *            path of collection
     * @return collection represented by path
     */
    public CollectionItem findCollectionByPath(String path);

    /**
     * Find collection by uid.
     * 
     * @param uid
     *            uid of collection
     * @return collection represented by uid
     */
    public CollectionItem findCollectionByUid(String uid);

    /**
     * Create new content item. A content item represents a piece of content or
     * file.
     * 
     * @param parent
     *            parent collection of content. If null, content is assumed to
     *            live in the top-level user collection
     * @param content
     *            content to create
     * @return newly created content
     */
    public ContentItem createContent(CollectionItem parent, ContentItem content);
    
    /**
     * Create new content item and associate with multiple parent collections.
     * 
     * @param parents
     *            parent collections of content. 
     * @param content
     *            content to create
     * @return newly created content
     */
    public ContentItem createContent(Set<CollectionItem> parents, ContentItem content);

    /**
     * Update an existing content item.
     * 
     * @param content
     *            content item to update
     * @return updated content item
     */
    public ContentItem updateContent(ContentItem content);

   
    /**
     * Find content item by path. Path is of the format:
     * /username/parent1/parent2/itemname.
     * 
     * @param path
     *            path of content to find
     * @return content item represented by hierachical path
     */
    public ContentItem findContentByPath(String path);

    /**
     * Find content item by uid.
     * 
     * @param uid
     *            uid of content to find
     * @return content item represented by uid
     */
    public ContentItem findContentByUid(String uid);

    /**
     * Remove content item
     * 
     * @param content
     *            content item to remove
     */
    public void removeContent(ContentItem content);

    /**
     * Remove collection item
     * 
     * @param collection
     *            collection item to remove
     */
    public void removeCollection(CollectionItem collection);

}
