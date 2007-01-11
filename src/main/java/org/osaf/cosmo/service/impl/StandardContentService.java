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
package org.osaf.cosmo.service.impl;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.calendar.query.CalendarFilter;
import org.osaf.cosmo.dao.CalendarDao;
import org.osaf.cosmo.dao.ContentDao;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.CollectionLockedException;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.service.ContentService;
import org.osaf.cosmo.service.lock.LockManager;

/**
 * Standard implementation of <code>ContentService</code>.
 *
 * @see ContentService
 * @see ContentDao
 */
public class StandardContentService implements ContentService {
    private static final Log log =
        LogFactory.getLog(StandardContentService.class);

    private CalendarDao calendarDao;
    private ContentDao contentDao;
    private LockManager lockManager;
    
    private long lockTimeout = 0;

    // ContentService methods

    /**
     * Get the root item for a user
     *
     * @param user
     */
    public HomeCollectionItem getRootItem(User user) {
        if (log.isDebugEnabled()) {
            log.debug("getting root item for " + user.getUsername());
        }
        return contentDao.getRootItem(user);
    }

    /**
     * Find an item with the specified uid. The return type will be one of
     * ContentItem, CollectionItem, CalendarCollectionItem, CalendarItem.
     *
     * @param uid
     *            uid of item to find
     * @return item represented by uid
     */
    public Item findItemByUid(String uid) {
        if (log.isDebugEnabled()) {
            log.debug("finding item with uid " + uid);
        }
        return contentDao.findItemByUid(uid);
    }

    /**
     * Find content item by path. Path is of the format:
     * /username/parent1/parent2/itemname.
     */
    public Item findItemByPath(String path) {
        if (log.isDebugEnabled()) {
            log.debug("finding item at path " + path);
        }
        return contentDao.findItemByPath(path);
    }
    
    /**
     * Find content item's parent by path. Path is of the format:
     * /username/parent1/parent2/itemname.  In this example,
     * the item at /username/parent1/parent2 would be returned.
     */
    public Item findItemParentByPath(String path) {
        if (log.isDebugEnabled()) {
            log.debug("finding item's parent at path " + path);
        }
        return contentDao.findItemParentByPath(path);
    }

    /**
     * Update an existing item.
     * 
     * @param item
     *            item to update
     * @return updated item
     */
    public Item updateItem(Item item) {
        if (log.isDebugEnabled()) {
            log.debug("updating item " + item.getName());
        }
        
        if (item instanceof CollectionItem)
            return updateCollection((CollectionItem) item);
        return updateContent((ContentItem) item);
    }

    /**
     * Copy an item to the given path
     * @param item item to copy
     * @param path path to copy item to
     * @param deepCopy true for deep copy, else shallow copy will
     *                 be performed
     * @throws org.osaf.cosmo.model.ItemNotFoundException
     *         if parent item specified by path does not exist
     * @throws org.osaf.cosmo.model.DuplicateItemNameException
     *         if path points to an item with the same path
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if Item is a ContentItem and destination CollectionItem
     *         is lockecd.
     */
    public void copyItem(Item item, String path, boolean deepCopy) {
        
        // handle case of copying ContentItem (need to sync on dest collection)
        if(item != null && item instanceof ContentItem) {
            
            // need to get exclusive lock to destination collection
            CollectionItem parent = 
                (CollectionItem) contentDao.findItemParentByPath(path);
            
            // only attempt to lock if destination exists
            if(parent!=null) {
                
                // if we can't get lock, then throw exception
                if (!lockManager.lockCollection(parent, lockTimeout))
                    throw new CollectionLockedException(
                            "unable to obtain collection lock");

                try {
                    contentDao.copyItem(item, path, deepCopy);
                } finally {
                    lockManager.unlockCollection(parent);
                }
                
            } else { 
                // let the dao handle throwing an error
                contentDao.copyItem(item, path, deepCopy);
            }
        }
        else { 
            // no need to synchronize if not ContentItem
            contentDao.copyItem(item, path, deepCopy);
        }
    }
  
    /**
     * Move item to the given path
     * @param item item to move
     * @param path path to move item to
     * @throws org.osaf.cosmo.model.ItemNotFoundException
     *         if parent item specified by path does not exist
     * @throws org.osaf.cosmo.model.DuplicateItemNameException
     *         if path points to an item with the same path
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if Item is a ContentItem and source or destination 
     *         CollectionItem is lockecd.
     */
    public void moveItem(Item item, String path) {
        
        // handle case of moving ContentItem 
        // (need to synch on src and dest collections)
        if(item != null && item instanceof ContentItem) {
            
            // need to get exclusive lock to source and destination parent
            CollectionItem destParent = 
                (CollectionItem) contentDao.findItemParentByPath(path);
            CollectionItem srcParent = item.getParent();
            
            // only attempt to lock if source and destination exist
            if(srcParent != null && destParent != null) {
                
                // lock destination first
                if (! lockManager.lockCollection(destParent, lockTimeout))
                    throw new CollectionLockedException("unable to obtain collection lock");
               
                // lock source
                try {
                    if (! lockManager.lockCollection(srcParent, lockTimeout))
                            throw new CollectionLockedException("unable to obtain collection lock");
                } catch (RuntimeException e) {
                    lockManager.unlockCollection(destParent);
                    throw e;
                } 
                
                // now we have exclusive locks to both collections
                try {
                    // copy Item to destination collection
                    contentDao.copyItem(item, path, false);
                    ContentItem copy = (ContentItem) contentDao.findItemByPath(path);
                    
                    // delete item from source collection
                    contentDao.removeContent((ContentItem) item);
                    
                    // update uid on copy, so that copy uid matches original
                    copy.setUid(item.getUid());
                    contentDao.updateContent(copy);
                    
                } finally {
                    lockManager.unlockCollection(srcParent);
                    lockManager.unlockCollection(destParent);
                }
                
            } else {
                // let dao throw errors
                contentDao.moveItem(item, path);
            }
        } else {
            // no need to sync
            contentDao.moveItem(item, path);
        }
    }
    
    /**
     * Remove an item.
     * 
     * @param item
     *            item to remove
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if Item is a ContentItem and parent CollectionItem
     *         is locked
     */
    public void removeItem(Item item) {
        if (log.isDebugEnabled()) {
            log.debug("removing item " + item.getName());
        }
        
        // Let service handle ContentItems (for sync purposes)
        if(item instanceof ContentItem)
            removeContent((ContentItem) item);
        else
            contentDao.removeItem(item);
    }

    /**
     * Remove an item.
     * 
     * @param path
     *            path of item to remove
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if Item is a ContentItem and parent CollectionItem
     *         is locked
     */
    public void removeItem(String path) {
        if (log.isDebugEnabled()) {
            log.debug("removing item at path " + path);
        }
        Item item = contentDao.findItemByPath(path);
        if (item == null)
            return;
        
        removeItem(item);
    }

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
                                           CollectionItem collection) {
        if (log.isDebugEnabled()) {
            log.debug("creating collection " + collection.getName() +
                      " in " + parent.getName());
        }
        return contentDao.createCollection(parent, collection);
    }

    /**
     * Create a new collection.
     * 
     * @param parent
     *            parent of collection.
     * @param collection
     *            collection to create
     * @param children
     *            collection children
     * @return newly created collection
     */
    public CollectionItem createCollection(CollectionItem parent,
            CollectionItem collection, Set<Item> children) {
        if (log.isDebugEnabled()) {
            log.debug("creating collection " + collection.getName() + " in "
                    + parent.getName());
        }

        CollectionItem newCollection = contentDao.createCollection(parent,
                collection);
        for (Item item : children) {
            if (item instanceof ContentItem) {
                ContentItem newItem = contentDao.createContent(collection,
                        (ContentItem) item);
                newCollection.getChildren().add(newItem);
            }
        }
        
        return contentDao.updateCollection(collection);
    }
    
    /**
     * Update collection item
     * 
     * @param collection
     *            collection item to update
     * @return updated collection
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if CollectionItem is locked
     */
    public CollectionItem updateCollection(CollectionItem collection) {

        if (! lockManager.lockCollection(collection, lockTimeout))
            throw new CollectionLockedException("unable to obtain collection lock");
        
        try {
            return contentDao.updateCollection(collection);
        } finally {
            lockManager.unlockCollection(collection);
        }
    }

    /**
     * Update a collection and set of children.  The set of
     * children to be updated can include updates to existing
     * children, new children, and removed children.  A removal
     * of a child Item is accomplished by setting Item.isActive
     * to false to an existing Item.
     * 
     * The collection is locked at the beginning of the update. Any
     * other update that begins before this update has completed, and
     * the collection unlocked, will fail immediately with a
     * <code>CollectionLockedException</code>.
     *
     * @param collection
     *             collection to update
     * @param children
     *             children to update
     * @return updated collection
     * @throws CollectionLockedException if the collection is
     *         currently locked for an update.
     */
    public CollectionItem updateCollection(CollectionItem collection,
                                           Set<Item> children) {
        if (log.isDebugEnabled()) {
            log.debug("updating collection " + collection.getName());
        }

        if (! lockManager.lockCollection(collection, lockTimeout))
            throw new CollectionLockedException("unable to obtain collection lock");

        try {
            for(Item item : children) {
                // for now, only process ContentItems
                if(item instanceof ContentItem) {
                    // deletion
                    if(item.getIsActive()==false)
                        contentDao.removeContent((ContentItem) item);
                    // addition
                    else  if(item.getId()==-1) {
                        ContentItem newItem = contentDao.createContent(collection,
                                                 (ContentItem) item);
                        collection.getChildren().add(newItem);
                    }
                    // update
                    else
                        contentDao.updateContent((ContentItem) item);
                }
            }

            // update collection
            collection = contentDao.updateCollection(collection);
            
            return collection;
        } finally {
            lockManager.unlockCollection(collection);
        }

    }

    /**
     * Find all children for collection. Children can consist of ContentItem and
     * CollectionItem objects.
     * 
     * @param collection
     *            collection to find children for
     * @return set of child objects for parent collection. Child objects
     *         can be either CollectionItem or ContentItem.
     */
    public Set findChildren(CollectionItem collection) {
        if (log.isDebugEnabled()) {
            log.debug("finding children of collection " +
                      collection.getName());
        }
        return contentDao.findChildren(collection);
    }

    /**
     * Remove collection item
     * 
     * @param collection
     *            collection item to remove
     */
    public void removeCollection(CollectionItem collection) {
        if (log.isDebugEnabled())
            log.debug("removing collection " + collection.getName());

        contentDao.removeCollection(collection);
    }

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
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if parent CollectionItem is locked
     */
    public ContentItem createContent(CollectionItem parent,
                                     ContentItem content) {
        if (log.isDebugEnabled()) {
            log.debug("creating content item " + content.getName() +
                      " in " + parent.getName());
        }
        
        if (! lockManager.lockCollection(parent, lockTimeout))
            throw new CollectionLockedException("unable to obtain collection lock");
        
        try {
            return contentDao.createContent(parent, content);
        } finally {
            lockManager.unlockCollection(parent);
        }   
    }

    /**
     * Update an existing content item.
     * 
     * @param content
     *            content item to update
     * @return updated content item
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if parent CollectionItem is locked
     */
    public ContentItem updateContent(ContentItem content) {
        if (log.isDebugEnabled()) {
            log.debug("updating content item " + content.getName());
        }
        
        CollectionItem parent = content.getParent();
        
        if (! lockManager.lockCollection(parent, lockTimeout))
            throw new CollectionLockedException("unable to obtain collection lock");
        
        try {
            return contentDao.updateContent(content);
        } finally {
            lockManager.unlockCollection(parent);
        }
    }

    /**
     * Remove content item
     * 
     * @param content
     *            content item to remove
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if parent CollectionItem is locked           
     */
    public void removeContent(ContentItem content) {
        if (log.isDebugEnabled()) {
            log.debug("removing content item " + content.getName());
        }
        
        CollectionItem parent = content.getParent();
        
        if (! lockManager.lockCollection(parent, lockTimeout))
            throw new CollectionLockedException("unable to obtain collection lock");
        
        try {
            contentDao.removeContent(content);
        } finally {
            lockManager.unlockCollection(parent);
        }
    }

    
    /**
     * Find calendar events by filter.
     *
     * @param calendar
     *            calendar collection to search
     * @param filter
     *            filter to use in search
     * @return set CalendarEventItem objects matching specified
     *         filter.
     */
    public Set<ContentItem> findEvents(CollectionItem calendar,
                                             CalendarFilter filter) {
        if (log.isDebugEnabled()) {
            log.debug("finding events in calendar " + calendar.getName() +
                      " by filter " + filter);
        }
        return calendarDao.findEvents(calendar, filter);
    }


    /**
     * Creates a ticket on an item.
     *
     * @param item the item to be ticketed
     * @param ticket the ticket to be saved
     */
    public void createTicket(Item item,
                             Ticket ticket) {
        if (log.isDebugEnabled()) {
            log.debug("creating ticket on item " + item.getName());
        }
        contentDao.createTicket(item, ticket);
    }

    /**
     * Creates a ticket on an item.
     *
     * @param path the path of the item to be ticketed
     * @param ticket the ticket to be saved
     */
    public void createTicket(String path,
                             Ticket ticket) {
        if (log.isDebugEnabled()) {
            log.debug("creating ticket on item at path " + path);
        }
        Item item = contentDao.findItemByPath(path);
        if (item == null)
            throw new IllegalArgumentException("item not found for path " + path);
        contentDao.createTicket(item, ticket);
    }

    /**
     * Returns all tickets on the given item.
     *
     * @param item the item to be ticketed
     */
    public Set getTickets(Item item) {
        if (log.isDebugEnabled()) {
            log.debug("getting tickets for item " + item.getName());
        }
        return contentDao.getTickets(item);
    }

    /**
     * Returns the identified ticket on the given item, or
     * <code>null</code> if the ticket does not exists. Tickets are
     * inherited, so if the specified item does not have the ticket
     * but an ancestor does, it will still be returned.
     *
     * @param item the ticketed item
     * @param key the ticket to return
     */
    public Ticket getTicket(Item item,
                            String key) {
        if (log.isDebugEnabled()) {
            log.debug("getting ticket " + key + " for item " + item.getName());
        }
        return contentDao.getTicket(item, key);
    }

    public Ticket getTicket(String itemId, String key){
        Item item = findItemByUid(itemId);
        return getTicket(item, key);
    }

    /**
     * Removes a ticket from an item.
     *
     * @param item the item to be de-ticketed
     * @param ticket the ticket to remove
     */
    public void removeTicket(Item item,
                             Ticket ticket) {
        if (log.isDebugEnabled()) {
            log.debug("removing ticket " + ticket.getKey() + " on item " +
                      item.getName());
        }
        contentDao.removeTicket(item, ticket);
    }

    /**
     * Removes a ticket from an item.
     *
     * @param path the path of the item to be de-ticketed
     * @param key the key of the ticket to remove
     */
    public void removeTicket(String path,
                             String key) {
        if (log.isDebugEnabled()) {
            log.debug("removing ticket " + key + " on item at path " + path);
        }
        Item item = contentDao.findItemByPath(path);
        if (item == null)
            throw new IllegalArgumentException("item not found for path " + path);
        Ticket ticket = contentDao.getTicket(item, key);
        if (ticket == null)
            return;
        contentDao.removeTicket(item, ticket);
    }

    // Service methods

    /**
     * Initializes the service, sanity checking required properties
     * and defaulting optional properties.
     */
    public void init() {
        if (calendarDao == null)
            throw new IllegalStateException("calendarDao must not be null");
        if (contentDao == null)
            throw new IllegalStateException("contentDao must not be null");
        if (lockManager == null)
            throw new IllegalStateException("lockManager must not be null");
    }

    /**
     * Readies the service for garbage collection, shutting down any
     * resources used.
     */
    public void destroy() {
        // does nothing
    }

    // our methods

    /** */
    public CalendarDao getCalendarDao() {
        return calendarDao;
    }

    /** */
    public void setCalendarDao(CalendarDao dao) {
        calendarDao = dao;
    }

    /** */
    public ContentDao getContentDao() {
        return contentDao;
    }

    /** */
    public void setContentDao(ContentDao dao) {
        contentDao = dao;
    }

    /** */
    public LockManager getLockManager() {
        return lockManager;
    }

    /** */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }
    
    
    /**
     * Sets the maximum ammount of time (in millisecondes) that the
     * service will wait on acquiring an exclusive lock on a CollectionItem.
     * @param lockTimeout
     */
    public void setLockTimeout(long lockTimeout) {
        this.lockTimeout = lockTimeout;
    }
}
