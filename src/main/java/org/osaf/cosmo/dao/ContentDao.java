/*
 * Copyright (c) 2006 SimDesk Technologies, Inc.  All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * SimDesk Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with SimDesk Technologies.
 *
 * SIMDESK TECHNOLOGIES MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT
 * THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT.  SIMDESK TECHNOLOGIES
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package org.osaf.cosmo.dao;

import java.util.Set;

import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ModelValidationException;
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
     * Find all children for collection. Children can consist of ContentItem and
     * CollectionItem objects.
     * 
     * @param collection
     *            collection to find children for
     * @return collection of child objects for parent collection. Child objects
     *         can be either CollectionItem or ContentItem.
     */
    public Set<Item> findChildren(CollectionItem collection);

    /**
     * Find all top level children for user. Children can consist of ContentItem
     * and CollectionItem objects.
     * 
     * @param collection
     *            collection to find children for
     * @return collection of child objects for parent collection. Child objects
     *         can be either CollectionItem or ContentItem.
     */
    public Set<Item> findChildren(User user);

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
     * Update an existing content item.
     * 
     * @param content
     *            content item to update
     * @return updated content item
     */
    public ContentItem updateContent(ContentItem content);

    /**
     * Move an Item to a different collection
     * 
     * @param parent
     *            collection to add item to
     * @param item
     *            item to move
     * @throws ModelValidationException
     *             if parent is invalid
     */
    public void moveContent(CollectionItem parent, Item item);

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

    /**
     * Find content or collection item by path. Path is of the format:
     * /username/parent1/parent2/itemname
     * 
     * @param path
     *            path of item to find
     * @return item represented by path. Will be either a CollectionItem or
     *         ContentItem.
     */
    public Item findItemByPath(String path);

    /**
     * Find content or collection item by uid.
     * 
     * @param uid
     *            uid of item to find
     * @return item represented by uid. Will be either a CollectionItem or
     *         ContentItem.
     */
    public Item findItemByUid(String uid);
}
