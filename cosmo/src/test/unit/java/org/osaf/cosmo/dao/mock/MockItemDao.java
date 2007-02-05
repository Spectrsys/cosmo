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
package org.osaf.cosmo.dao.mock;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.dao.ItemDao;
import org.osaf.cosmo.model.Attribute;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.DuplicateItemNameException;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ItemNotFoundException;
import org.osaf.cosmo.model.QName;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.util.PathUtil;

/**
 * Mock implementation of <code>ItemDao</code> useful for testing.
 *
 * @see ItemDao
 * @see Item
 */
public class MockItemDao implements ItemDao {
    private static final Log log = LogFactory.getLog(MockItemDao.class);

    private MockDaoStorage storage;

    /**
     */
    public MockItemDao(MockDaoStorage storage) {
        this.storage = storage;
    }

    // ItemDao methods

    /**
     * Find an item with the specified uid. The return type will be one of
     * Item, CollectionItem, CalendarCollectionItem, CalendarItem.
     * 
     * @param uid
     *            uid of item to find
     * @return item represented by uid
     */
    public Item findItemByUid(String uid) {
        return storage.getItemByUid(uid);
    }
    
    /**
     * Find any item (active or inactive) with the specified uid. 
     * The return type will be one of ContentItem, CollectionItem, 
     * CalendarCollectionItem, CalendarItem.
     *
     * @param uid
     *            uid of item to find
     * @return item represented by uid
     */
    public Item findAnyItemByUid(String uid) {
        return storage.getItemByUid(uid);
    }

    /**
     * Find an item with the specified path. The return type will be one of
     * Item, CollectionItem, CalendarCollectionItem, CalendarItem.
     * 
     * @param path
     *            path of item to find
     * @return item represented by path
     */
    public Item findItemByPath(String path) {
        return storage.getItemByPath(path);
    }
    
    /**
     * Find the parent item of the item with the specified path. 
     * The return type will be one of CollectionItem, CalendarCollectionItem.
     *
     * @param path
     *            path of item
     * @return parent item of item represented by path
     */
    public Item findItemParentByPath(String path) {
        return storage.getItemByPath(path).getParent();
    }

    /**
     * Return the path to an item. The path has the format:
     * /username/parent1/parent2/itemname
     * 
     * @param item
     *            the item to calculate the path for
     * @return hierarchical path to item
     */
    public String getItemPath(Item item) {
        return storage.getItemPath(item);
    }

    /**
     * Return the path to an item. The path has the format:
     * /username/parent1/parent2/itemname
     * 
     * @param uid
     *            the uid of the item to calculate the path for
     * @return hierarchical path to item
     */
    public String getItemPath(String uid) {
        return storage.getItemPath(uid);
    }

    /**
     * Get the root item for a user
     * 
     * @param user
     * @return
     */
    public HomeCollectionItem getRootItem(User user) {
        if (user == null)
            throw new IllegalArgumentException("null user");

        if (log.isDebugEnabled()) {
            log.debug("getting root item for user " + user.getUsername());
        }

        return getStorage().getRootItem(user.getId());
    }

    /**
     * Create the root item for a user.
     * @param user
     * @return
     */
    public HomeCollectionItem createRootItem(User user) {
        if (user == null)
            throw new IllegalArgumentException("null user");

        if (log.isDebugEnabled()) {
            log.debug("creating root item for user " + user.getUsername());
        }

        HomeCollectionItem rootCollection = storage.createRootItem(user);

        if (log.isDebugEnabled()) {
            log.debug("root item uid is " + rootCollection.getUid());
        }

        return rootCollection;
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
     */
    public void copyItem(Item item,
                         String path,
                         boolean deepCopy) {
        if (item == null)
            throw new IllegalArgumentException("item cannot be null");
        if (path == null)
            throw new IllegalArgumentException("path cannot be null");

        String parentPath = PathUtil.getParentPath(path);
        CollectionItem parent = (CollectionItem)
            storage.getItemByPath(PathUtil.getParentPath(path));
        if (parent == null)
            throw new ItemNotFoundException("parent collection not found");

        Item copy =
            copyItem(item, PathUtil.getBasename(path), parent, deepCopy);
    }

    private Item copyItem(Item item,
                          String copyName,
                          CollectionItem parent,
                          boolean deepCopy) {
        Item copy = null;
        try {
            copy = item.getClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("unable to construct new instance of " + item.getClass());
        }

        if (copyName == null)
            copyName = item.getName();
        copy.setName(copyName);
        copy.setParent(parent);
        copy.setOwner(item.getOwner());

        for(Map.Entry<QName, Attribute> entry :
                item.getAttributes().entrySet())
            copy.addAttribute(entry.getValue().copy());

        // XXX: ignoring calendar indexes

        storage.storeItem(copy);

        if (deepCopy && (item instanceof CollectionItem)) {
            CollectionItem collection = (CollectionItem) item;
            for (Item child: collection.getChildren())
                copyItem(child, null, (CollectionItem) copy, true);
        }

        return copy;
    }
    
  
    /**
     * Move item to the given path
     * @param item item to move
     * @param path path to move item to
     * @throws org.osaf.cosmo.model.ItemNotFoundException
     *         if parent item specified by path does not exist
     * @throws org.osaf.cosmo.model.DuplicateItemNameException
     *         if path points to an item with the same path
     */
    public void moveItem(Item item,
                         String path) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Remove an item.
     * 
     * @param item
     *            item to remove
     */
    public void removeItem(Item item) {
        item.getParent().getChildren().remove(item);

        storage.removeItemByUid(item.getUid());
        storage.removeItemByPath(getItemPath(item));
        if (storage.getRootUid(item.getOwner().getId()).
            equals(item.getUid())) {
            storage.removeRootUid(item.getOwner().getId());
        }
    }

    /**
     * Creates a ticket on an item.
     *
     * @param item the item to be ticketed
     * @param ticket the ticket to be saved
     */
    public void createTicket(Item item,
                             Ticket ticket) {
        item.getTickets().add(ticket);
        storage.createTicket(item, ticket);
    }

    /**
     * Returns all tickets on the given item.
     *
     * @param item the item to be ticketed
     */
    public Set getTickets(Item item) {
        return storage.findItemTickets(item);
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
        for(Ticket t : storage.findItemTickets(item)) {
            if (t.getKey().equals(key))
                return t;
        }
        // the ticket might be on an ancestor, so check the parent
        if (item.getParent() != null) {
            return getTicket(storage.getItemByUid(item.getParent().getUid()),
                             key);
        }
        // this is the root item; the ticket simply doesn't exist
        // anywhere in the given path
        return null;
    }

    /**
     * Removes a ticket from an item.
     *
     * @param item the item to be de-ticketed
     * @param ticket the ticket to remove
     */
    public void removeTicket(Item item,
                             Ticket ticket) {
        Set itemTickets = storage.findItemTickets(item);
        if (itemTickets.contains(ticket)) {
            item.getTickets().remove(ticket);
            storage.removeTicket(item, ticket);
            return;
        }
        // the ticket might be on an ancestor, so check the parent
        if (item.getParent() != null) {
            removeTicket(storage.getItemByUid(item.getParent().getUid()),
                         ticket);
        }
        // this is the root item; the ticket simply doesn't exist
        // anywhere in the given path
        return;
    }

    public void removeItemByPath(String path) {
        removeItem(findItemByPath(path));
    }

    public void removeItemByUid(String uid) {
        removeItem(findItemByUid(uid));
    }
    
    // Dao methods

    /**
     * Initializes the DAO, sanity checking required properties
     * and defaulting optional properties.
     */
    public void init() {
    }

    /**
     * Readies the DAO for garbage collection, shutting down any
     * resources used.
     */
    public void destroy() {
    }

    // our methods

    /** */
    public MockDaoStorage getStorage() {
        return storage;
    }

    /** */
    protected Set findRootChildren(User user) {
        return storage.findItemChildren(storage.getRootItem(user.getId()));
    }
}
