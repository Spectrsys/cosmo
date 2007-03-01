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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.calendar.query.CalendarFilter;
import org.osaf.cosmo.calendar.util.CalendarUtils;
import org.osaf.cosmo.dao.CalendarDao;
import org.osaf.cosmo.dao.ContentDao;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.CollectionLockedException;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.EventExceptionStamp;
import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ItemNotFoundException;
import org.osaf.cosmo.model.NoteItem;
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
     * Find content item by path relative to the identified parent
     * item.
     *
     * @throws NoSuchItemException if a item does not exist at
     * the specified path
     */
    public Item findItemByPath(String path,
                               String parentUid) {
        if (log.isDebugEnabled())
            log.debug("finding item at path " + path + " below parent " +
                      parentUid);
        return contentDao.findItemByPath(path, parentUid);
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
     * @param fromPath path of item to move
     * @param toPath path of item to move
     * @throws org.osaf.cosmo.model.ItemNotFoundException
     *         if parent item specified by path does not exist
     * @throws org.osaf.cosmo.model.DuplicateItemNameException
     *         if path points to an item with the same path
     * @throws org.osaf.cosmo.model.CollectionLockedException
     *         if Item is a ContentItem and source or destination 
     *         CollectionItem is lockecd.
     */
    public void moveItem(String fromPath, String toPath) {
        
        CollectionItem oldParent = (CollectionItem) contentDao
                .findItemParentByPath(fromPath);
        
        if(oldParent==null | !(oldParent instanceof CollectionItem))
            throw new ItemNotFoundException("no item found for " + fromPath);
        
        CollectionItem newParent = (CollectionItem) contentDao
                .findItemParentByPath(toPath);

        if(newParent==null || !(newParent instanceof CollectionItem) )
            throw new ItemNotFoundException("no collection found for " + toPath + " found");
        
        Item fromItem = contentDao.findItemByPath(fromPath);
        
        if(fromItem==null)
            throw new ItemNotFoundException("no item found for " + fromPath);
        
        // Only need locking for ContentItem for now
        if(fromItem instanceof ContentItem) {
            Set<CollectionItem> locks = acquireLocks(newParent, fromItem);
            try {
                contentDao.moveItem(fromPath, toPath);
                // update collections involved
                for(CollectionItem parent : locks)
                    contentDao.updateCollection(parent);
                
            } finally {
                releaseLocks(locks);
            }
        } else {
            contentDao.moveItem(fromPath, toPath);
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

        // Obtain locks to all collections involved.  A collection is involved
        // if it is the parent of one of the children.  If all children are new
        // items, then no locks are obtained.
        Set<CollectionItem> locks = acquireLocks(collection, children);
        
        try {
            // Create the new collection
            collection = contentDao.createCollection(parent, collection);
            
            // Keep track of event items (NoteItem with EventStamp) that need
            // to be indexed
            HashMap<String, NoteItem> events = new HashMap<String, NoteItem>();
            
            // Keep track of NoteItem modifications that need to be processed
            // after the master NoteItem.
            ArrayList<NoteItem> modifications = new ArrayList<NoteItem>(); 
            
            // Either create or update each item
            for (Item item : children) {
                if (item instanceof NoteItem) {
                    
                    NoteItem note = (NoteItem) item;
                    
                    // If item is a modification and the master note
                    // hasn't been created, then we need to process
                    // the master first.
                    if(note.getModifies()!=null) {
                        if(note.getModifies().getId()==-1) {
                            modifications.add(note);
                            continue;
                        }
                    }
                    
                    // keep track of events so we can index
                    if(note.getModifies()!=null && (EventStamp.getStamp(note.getModifies())!=null))
                        events.put(note.getModifies().getUid(), note.getModifies());
                    else if(EventStamp.getStamp(note)!=null)
                        events.put(note.getUid(), note);
                    
                    // create item
                    if(item.getId()==-1) {
                        item = contentDao.createContent(collection,
                            (ContentItem) item);
                    } 
                    // update item
                    else {
                        contentDao.addItemToCollection(item, collection);
                        contentDao.updateContent((ContentItem) item);
                    }
                }
            }
            
            // process modifications that came before master
            for(NoteItem mod : modifications) {
                if(mod.getModifies().getId()==-1)
                    throw new RuntimeException("note modifies unknown item");
                mod = (NoteItem) contentDao.createContent(collection, mod);
            }
            
            // index events
            for(Entry<String, NoteItem> entry : events.entrySet()) {
                NoteItem note = entry.getValue();
                contentDao.refreshItem(note);
                calendarDao.indexEvent(EventStamp.getStamp(note));
            }
                
            // update collections involved
            for(CollectionItem lockedCollection : locks)
                contentDao.updateCollection(lockedCollection);
            
            contentDao.refreshItem(collection);
            return collection;
            
        } finally {
           releaseLocks(locks);
        }
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
                                           Set<Item> updates) {
        if (log.isDebugEnabled()) {
            log.debug("updating collection " + collection.getName());
        }

        // Obtain locks to all collections involved.  A collection is involved
        // if it is the parent of one of updated items.
        Set<CollectionItem> locks = acquireLocks(collection, updates);
        
        try {
            
            // Keep track of event items (NoteItem with EventStamp) that need
            // to be indexed
            HashMap<String, NoteItem> events = new HashMap<String, NoteItem>();
            
            // Keep track of NoteItem modifications that need to be processed
            // after the master NoteItem.
            ArrayList<NoteItem> modifications = new ArrayList<NoteItem>(); 
            
            for(Item item : updates) {
                // for now, only process NoteItem
                if(item instanceof NoteItem) {
                    
                    NoteItem  note = (NoteItem) item;
                    
                    // If item is a modification and the master note
                    // hasn't been created, then we need to process
                    // the master first.
                    if(note.getModifies()!=null) {
                        if(note.getModifies().getId()==-1) {
                            modifications.add(note);
                            continue;
                        }
                    }
                    
                    // keep track of events so we can index
                    if(note.getModifies()!=null) {
                        EventStamp masterEvent = EventStamp.getStamp(note.getModifies());
                        EventExceptionStamp exEvent = EventExceptionStamp.getStamp(note);
                        // only index event if the eventstamp is dirty
                        if(masterEvent!=null && exEvent !=null && exEvent.isDirty())
                            events.put(note.getModifies().getUid(), note.getModifies());
                    } else if(note.getIsActive()==true && EventStamp.getStamp(note)!=null) {
                        // only index event if the eventstamp is dirty
                        if(EventStamp.getStamp(note).isDirty())
                            events.put(note.getUid(), note);
                    }
                     
                    // addition
                    if(item.getId()==-1) {
                        note = (NoteItem) contentDao.createContent(collection,
                                                 note);
                    }
                    // deletion
                    else if(item.getIsActive()==false) {
                        item.setIsActive(true);
                        contentDao.removeItemFromCollection(item, collection);
                    }
                    // update
                    else {
                        
                        // Add item to collection if necessary
                        if(!item.getParents().contains(collection))
                            contentDao.addItemToCollection(item, collection);
                        contentDao.updateContent((ContentItem) item);
                    }
                }
            }
            
            // process modifications that came before master
            for(NoteItem mod : modifications) {
                if(mod.getModifies().getId()==-1)
                    throw new RuntimeException("note modifies unknown item");
                mod = (NoteItem) contentDao.createContent(collection, mod);
            }
            
            // index events
            for(Entry<String, NoteItem> entry : events.entrySet()) {
                NoteItem note = entry.getValue();
                contentDao.refreshItem(note);
                calendarDao.indexEvent(EventStamp.getStamp(note));
            }
            
            // update collections
            for(CollectionItem parent : locks)
                contentDao.updateCollection(parent);
            
            contentDao.refreshItem(collection);
            return collection;
        } finally {
            releaseLocks(locks);
        }
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
            content = contentDao.createContent(parent, content);
            contentDao.updateCollection(parent);
            return content;
        } finally {
            lockManager.unlockCollection(parent);
        }   
    }
    
    public NoteItem updateEvent(NoteItem note, Calendar calendar) {
        if (log.isDebugEnabled()) {
            log.debug("updating event " + note.getName());
        }
        
        Set<CollectionItem> locks = acquireLocks(note);
        
        try {
            updateEventInternal(note, calendar);
            
            // update collections
            for(CollectionItem parent : locks)
                contentDao.updateCollection(parent);
            
            return note;
        } finally {
            releaseLocks(locks);
        }
    }
    
    public NoteItem createEvent(CollectionItem parent, NoteItem masterNote, Calendar calendar) {
        
        if (log.isDebugEnabled()) {
            log.debug("creating event " + masterNote.getName() +
                      " in " + parent.getName());
        }
        
        if (! lockManager.lockCollection(parent, lockTimeout))
            throw new CollectionLockedException("unable to obtain collection lock");
        
        try {
            EventStamp eventStamp = EventStamp.getStamp(masterNote);
            masterNote.setIcalUid(eventStamp.getIcalUid());
            masterNote.setBody(eventStamp.getDescription());
            // create master note
            masterNote = (NoteItem) contentDao.createContent(parent, masterNote);
           
            // create modification notes if needed
            updateEventInternal(masterNote, calendar);
            contentDao.updateCollection(parent);
            return masterNote;
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
        
        Set<CollectionItem> locks = acquireLocks(content);
        
        try {
            content = contentDao.updateContent(content);
            
            // update collections
            for(CollectionItem parent : locks)
                contentDao.updateCollection(parent);
            
            return content;
        } finally {
            releaseLocks(locks);
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
        
        Set<CollectionItem> locks = acquireLocks(content);
        
        try {
            contentDao.removeContent(content);
            // update collections
            for(CollectionItem parent : locks)
                contentDao.updateCollection(parent);
        } finally {
            releaseLocks(locks);
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
    
    /**
     * Given a collection and a set of items, aquire a lock on the collection and
     * all 
     */
    private Set<CollectionItem> acquireLocks(CollectionItem collection, Set<Item> children) {
        
        HashSet<CollectionItem> locks = new HashSet<CollectionItem>();
        
        // Get locks for all collections involved
        try {

            if (! lockManager.lockCollection(collection, lockTimeout))
                throw new CollectionLockedException("unable to obtain collection lock");
            
            locks.add(collection);
            
            for(Item child : children)
                acquireLocks(locks, child);
           
            return locks;
        } catch (RuntimeException e) {
            releaseLocks(locks);
            throw e;
        }
    }
    
    private Set<CollectionItem> acquireLocks(CollectionItem collection, Item item) {
        HashSet<Item> items = new HashSet<Item>();
        items.add(item);
        
        return acquireLocks(collection, items);
    }
    
    private Set<CollectionItem> acquireLocks(Item item) {
        HashSet<CollectionItem> locks = new HashSet<CollectionItem>();
        try {
            acquireLocks(locks,item);
            return locks;
        } catch (RuntimeException e) {
            releaseLocks(locks);
            throw e;
        }
    }
    
    private void acquireLocks(Set<CollectionItem> locks, Item item) {
        for(CollectionItem parent: item.getParents()) {
            if(locks.contains(parent))
                continue;
            if (! lockManager.lockCollection(parent, lockTimeout))
                throw new CollectionLockedException("unable to obtain collection lock");
            locks.add(parent);
        }
    }
    
    private void releaseLocks(Set<CollectionItem> locks) {
        for(CollectionItem lock : locks)
            lockManager.unlockCollection(lock);
    }
    
    private void updateEventInternal(NoteItem masterNote, Calendar calendar) {
        HashMap<Date, VEvent> exceptions = new HashMap<Date, VEvent>();
        
        // Clone Calendar as it will be the basis for the master event's calendar
        Calendar masterCalendar = CalendarUtils.copyCalendar(calendar);
        
        ComponentList vevents = masterCalendar.getComponents().getComponents(
                Component.VEVENT);
        EventStamp eventStamp = EventStamp.getStamp(masterNote);

        // get list of exceptions (VEVENT with RECURRENCEID)
        for (Iterator<VEvent> i = vevents.iterator(); i.hasNext();) {
            VEvent event = i.next();
            if (event.getReccurrenceId() != null)
                exceptions.put(event.getReccurrenceId().getDate(), event);
        }
        
        // Remove all exceptions from master calendar as these
        // will be stored in each NoteItem modification's EventExceptionStamp
        for (Entry<Date, VEvent> entry : exceptions.entrySet())
            masterCalendar.getComponents().remove(entry.getValue());

        // Master calendar includes everything in the original calendar minus
        // any exception events (VEVENT with RECURRENCEID)
        eventStamp.setMasterCalendar(masterCalendar);
        
        // set content length of master NoteItem (length of entire Calendar)
        try {
            masterNote.setContentLength((long) calendar
                    .toString().getBytes("UTF-8").length);
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException("unsupported UTF-8 encoding");
        } 
        
        // synchronize exceptions with master NoteItem modifications
        syncExceptions(exceptions, masterNote);
                
        // index entire calendar
        calendarDao.indexEvent(eventStamp);
        
        // update master note
        contentDao.updateContent(masterNote);
    }

    private void syncExceptions(Map<Date, VEvent> exceptions,
            NoteItem masterNote) {
        for (Entry<Date, VEvent> entry : exceptions.entrySet())
            syncException(entry.getValue(), masterNote);

        // remove old exceptions
        List<NoteItem> toRemove = new ArrayList<NoteItem>();
        for (NoteItem noteItem : masterNote.getModifications()) {
            EventExceptionStamp eventException = EventExceptionStamp
                    .getStamp(noteItem);
            if (!exceptions.containsKey(eventException.getRecurrenceId()))
                toRemove.add(noteItem);
        }
        
        for(NoteItem noteItem : toRemove) {
            contentDao.removeContent(noteItem);
            masterNote.getModifications().remove(noteItem);
        }
    }

    private void syncException(VEvent event, NoteItem masterNote) {
        NoteItem mod = getModification(masterNote, event.getReccurrenceId()
                .getDate());

        if (mod == null) {
            // create if not present
            createNoteModification(masterNote, event);
        } else {
            // update existing mod
            updateNoteModification(mod, event);
        }
    }

    private NoteItem getModification(NoteItem masterNote,
            Date recurrenceId) {
        for (NoteItem mod : masterNote.getModifications()) {
            EventExceptionStamp exceptionStamp = EventExceptionStamp
                    .getStamp(mod);
            if (exceptionStamp.getRecurrenceId().equals(recurrenceId))
                return mod;
        }

        return null;
    }

    private void createNoteModification(NoteItem masterNote, VEvent event) {
        NoteItem noteMod = new NoteItem();
        EventExceptionStamp exceptionStamp = new EventExceptionStamp(noteMod);
        exceptionStamp.setExceptionEvent(event);
        noteMod.addStamp(exceptionStamp);

        noteMod.setOwner(masterNote.getOwner());
        noteMod.setName(event.getUid().getValue() + ":"
                + event.getReccurrenceId().getValue());
        noteMod.setDisplayName(exceptionStamp.getDescription());
        noteMod.setBody(exceptionStamp.getSummary());
        noteMod.setIcalUid(masterNote.getIcalUid());
        noteMod.setModifies(masterNote);
        noteMod = (NoteItem) contentDao.createContent(masterNote.getParents(), noteMod);
        noteMod.getModifications().add(noteMod);
    }

    private void updateNoteModification(NoteItem noteMod, VEvent event) {
        EventExceptionStamp exceptionStamp = EventExceptionStamp
                .getStamp(noteMod);
        exceptionStamp.setExceptionEvent(event);
        noteMod.setDisplayName(exceptionStamp.getDescription());
        noteMod.setBody(exceptionStamp.getSummary());
        contentDao.updateContent(noteMod);
    }
}
