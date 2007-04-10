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
package org.osaf.cosmo.dao.hibernate;

import java.util.Date;
import java.util.Set;

import org.apache.commons.id.IdentifierGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;
import org.osaf.cosmo.dao.ItemDao;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.DuplicateItemNameException;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ItemNotFoundException;
import org.osaf.cosmo.model.ItemTombstone;
import org.osaf.cosmo.model.ModelValidationException;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.UidInUseException;
import org.osaf.cosmo.model.User;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Implementation of ItemDao using Hibernate persistent objects.
 *
 */
public abstract class ItemDaoImpl extends HibernateDaoSupport implements ItemDao {

    private static final Log log = LogFactory.getLog(ItemDaoImpl.class);

    private IdentifierGenerator idGenerator = null;
    private IdentifierGenerator ticketKeyGenerator = null;
    private ItemPathTranslator itemPathTranslator = null;

    /*
     * (non-Javadoc)
     *
     * @see org.osaf.cosmo.dao.ItemDao#findItemByPath(java.lang.String)
     */
    public Item findItemByPath(String path) {
        try {
            Item dbItem = itemPathTranslator.findItemByPath(path);
            return dbItem;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }
    
  
    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#findItemByPath(java.lang.String, java.lang.String)
     */
    public Item findItemByPath(String path, String parentUid) {
        try {
            Item parent = findItemByUid(parentUid);
            if(parent==null)
                throw new ItemNotFoundException("parent with uid " + parentUid + " not found");
            Item item = itemPathTranslator.findItemByPath(path, (CollectionItem) parent);
            return item;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }


    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#findItemParentByPath(java.lang.String)
     */
    public Item findItemParentByPath(String path) {
        try {
            Item dbItem = itemPathTranslator.findItemParent(path);
            return dbItem;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see org.osaf.cosmo.dao.ItemDao#findItemByUid(java.lang.String)
     */
    public Item findItemByUid(String uid) {
        try {
            // prevent auto flushing when looking up item by uid
            getSession().setFlushMode(FlushMode.MANUAL);
            Query hibQuery = getSession().getNamedQuery("item.by.uid")
                    .setParameter("uid", uid);
            hibQuery.setCacheable(true);
            hibQuery.setFlushMode(FlushMode.MANUAL);
            return (Item) hibQuery.uniqueResult();
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#findAnyItemByUid(java.lang.String)
     */
    public Item findAnyItemByUid(String uid) {
        try {
            Query hibQuery = getSession().getNamedQuery("item.any.by.uid")
                    .setParameter("uid", uid);
            hibQuery.setCacheable(true);
            return (Item) hibQuery.uniqueResult();
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osaf.cosmo.dao.ItemDao#removeItem(org.osaf.cosmo.model.Item)
     */
    public void removeItem(Item item) {
        try {

            if(item==null)
                throw new IllegalArgumentException("item cannot be null");
            
            if(item instanceof HomeCollectionItem)
                throw new IllegalArgumentException("cannot remove root item");

            getSession().refresh(item);
            getSession().delete(item);
            getSession().flush();
            
        } catch(ObjectNotFoundException onfe) {
            throw new ItemNotFoundException("item not found");
        } catch(ObjectDeletedException ode) {
            throw new ItemNotFoundException("item not found");
        } catch(UnresolvableObjectException uoe) {
            throw new ItemNotFoundException("item not found");
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    
    /*
     * (non-Javadoc)
     *
     * @see org.osaf.cosmo.dao.ItemDao#getRootItem(org.osaf.cosmo.model.User)
     */
    public HomeCollectionItem getRootItem(User user) {
        try {
            return findRootItem(user.getId());
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }


    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#createRootItem(org.osaf.cosmo.model.User)
     */
    public HomeCollectionItem createRootItem(User user) {
        try {

            if(user==null)
                throw new IllegalArgumentException("invalid user");

            if(findRootItem(user.getId())!=null)
                throw new RuntimeException("user already has root item");

            HomeCollectionItem newItem = new HomeCollectionItem();

            newItem.setOwner(user);
            newItem.setName(user.getUsername());
            newItem.setDisplayName(newItem.getName());
            setBaseItemProps(newItem);
            getSession().save(newItem);
            getSession().flush();
            return newItem;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } catch (InvalidStateException ise) {
            logInvalidStateException(ise);
            throw ise;
        }
    }
    
    public void addItemToCollection(Item item, CollectionItem collection) {
        try {
            getSession().update(item);
            getSession().update(collection);
            collection.removeTombstone(item);
            item.getParents().add(collection);
            getSession().flush();
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }
    
    public void removeItemFromCollection(Item item, CollectionItem collection) {
        try {
            
            getSession().update(collection);
            getSession().update(item);
            
            collection.addTombstone(new ItemTombstone(collection, item));
            item.getParents().remove(collection);
            
            // If the item belongs to no collection, then it should
            // be purged.
            if(item.getParents().size()==0)
                getSession().delete(item);
            
            getSession().flush();
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } 
    }
    
    public Set<Ticket> getTickets(Item item) {
        if(item==null)
            throw new IllegalArgumentException("item cannot be null");

        try {
            getSession().refresh(item);
            return item.getTickets();
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    public void createTicket(Item item, Ticket ticket) {
        try {
            if(ticket==null)
                throw new IllegalArgumentException("ticket cannot be null");
            
            if(item==null)
                throw new IllegalArgumentException("item cannot be null");

            User owner = ticket.getOwner();
            if (owner == null)
                throw new IllegalArgumentException("ticket must have owner");

            if (ticket.getKey() == null)
                ticket.setKey(ticketKeyGenerator.nextIdentifier().toString());

            ticket.setCreated(new Date());
            getSession().update(item);
            item.addTicket(ticket);
            getSession().flush();
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } catch (InvalidStateException ise) {
            logInvalidStateException(ise);
            throw ise;
        }
    }

    public Ticket getTicket(Item item, String key) {
        try {
            getSession().refresh(item);
            return getTicketRecursive(item, key);
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    public void removeTicket(Item item, Ticket ticket) {
        try {
            getSession().update(item);
            getSession().refresh(item);
            item.getTickets().remove(ticket);
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }

    }

    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#removeItemByPath(java.lang.String)
     */
    public void removeItemByPath(String path) {
        try {
            Item item = itemPathTranslator.findItemByPath(path);
            if(item==null)
                throw new ItemNotFoundException("item at " + path
                        + " not found");
            removeItem(item);
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }

    }

    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#removeItemByUid(java.lang.String)
     */
    public void removeItemByUid(String uid) {
        try {
            Item item = findItemByUid(uid);
            if (item == null)
                throw new ItemNotFoundException("item with uid " + uid
                        + " not found");
            removeItem(item);
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }
    

    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#copyItem(org.osaf.cosmo.model.Item, java.lang.String, boolean)
     */
    public void copyItem(Item item, String path, boolean deepCopy) {
        try {
            String copyName = itemPathTranslator.getItemName(path);
            
            if(copyName==null || "".equals(copyName))
                throw new IllegalArgumentException("path must include name");
            
            if(item instanceof HomeCollectionItem)
                throw new IllegalArgumentException("cannot copy root collection");
            
            CollectionItem parent = (CollectionItem) itemPathTranslator.findItemParent(path);
            
            if(parent==null)
                throw new ItemNotFoundException("parent collection not found");
            
            checkForDuplicateItemName(item.getOwner().getId(), 
                    parent.getId(), copyName);
            
            verifyNotInLoop(item, parent);
            
            Item newItem = copyItem(item,parent,deepCopy);
            newItem.setName(copyName);
            getSession().flush();
            
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } catch (InvalidStateException ise) {
            logInvalidStateException(ise);
            throw ise;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#moveItem(java.lang.String, java.lang.String)
     */
    public void moveItem(String fromPath, String toPath) {
        try {
            
            // Get current item
            Item item = itemPathTranslator.findItemByPath(fromPath);
            
            if(item==null)
                throw new ItemNotFoundException("item " + fromPath + " not found");
            
            if(item instanceof HomeCollectionItem)
                throw new IllegalArgumentException("cannot move root collection");
            
            // Name of moved item
            String moveName = itemPathTranslator.getItemName(toPath);
            
            if(moveName==null || "".equals(moveName))
                throw new IllegalArgumentException("path must include name");
            
            // Parent of moved item
            CollectionItem parent = (CollectionItem) itemPathTranslator.findItemParent(toPath);
            
            if(parent==null)
                throw new ItemNotFoundException("parent collecion not found");
            
            // Current parent
            CollectionItem oldParent = (CollectionItem) itemPathTranslator.findItemParent(fromPath);
            
            checkForDuplicateItemName(item.getOwner().getId(), 
                    parent.getId(), moveName);
            
            verifyNotInLoop(item, parent);
            
            item.setName(moveName);
            if(!parent.getUid().equals(oldParent.getUid())) {
                parent.removeTombstone(item);
                item.getParents().add(parent);
                oldParent.addTombstone(new ItemTombstone(oldParent, item));
                item.getParents().remove(oldParent);
            }
            
            getSession().flush();
            
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } catch (InvalidStateException ise) {
            logInvalidStateException(ise);
            throw ise;
        }
    }

    
    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#refreshItem(org.osaf.cosmo.model.Item)
     */
    public void refreshItem(Item item) {
        try {
           getSession().refresh(item);
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.osaf.cosmo.dao.ItemDao#initializeItem(org.osaf.cosmo.model.Item)
     */
    public void initializeItem(Item item) {
        try {
            // initialize all the proxied-associations, to prevent
            // lazy-loading of this data
            Hibernate.initialize(item.getAttributes());
            Hibernate.initialize(item.getStamps());
            Hibernate.initialize(item.getTombstones());
         } catch (HibernateException e) {
             throw SessionFactoryUtils.convertHibernateAccessException(e);
         }
    }
    

    /**
     * Set the unique ID generator for new items
     *
     * @param idGenerator
     */
    public void setIdGenerator(IdentifierGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public IdentifierGenerator getIdGenerator() {
        return idGenerator;
    }
    
    /**
     * Set the unique key generator for new tickets
     *
     * @param idGenerator
     */
    public void setTicketKeyGenerator(IdentifierGenerator ticketKeyGenerator) {
        this.ticketKeyGenerator = ticketKeyGenerator;
    }

    public IdentifierGenerator getTicketKeyGenerator() {
        return ticketKeyGenerator;
    }

    public ItemPathTranslator getItemPathTranslator() {
        return itemPathTranslator;
    }

    /**
     * Set the path translator. The path translator is responsible for
     * translating a path to an item in the database.
     *
     * @param itemPathTranslator
     */
    public void setItemPathTranslator(ItemPathTranslator itemPathTranslator) {
        this.itemPathTranslator = itemPathTranslator;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osaf.cosmo.dao.Dao#destroy()
     */
    public void destroy() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.osaf.cosmo.dao.Dao#init()
     */
    public void init() {
        if (idGenerator == null) {
            throw new IllegalStateException("idGenerator is required");
        }
        
        if (ticketKeyGenerator == null) {
            throw new IllegalStateException("ticketKeyGenerator is required");
        }

        if (itemPathTranslator == null) {
            throw new IllegalStateException("itemPathTranslator is required");
        }
    }

    protected Item copyItem(Item item, CollectionItem parent, boolean deepCopy) {
        
        Item item2 = item.copy();
        
        // copy base Item fields
        item2.getParents().add(parent);
        setBaseItemProps(item2);
        
        // save Item before attempting deep copy
        getSession().save(item2);
        getSession().flush();
        
        // copy children if collection and deepCopy = true
        if(deepCopy==true && (item instanceof CollectionItem) ) {
            CollectionItem collection = (CollectionItem) item;
            for(Item child: collection.getChildren())
                copyItem(child,(CollectionItem) item2,true);       
        }
        
        return item2;
    }
    
    /**
     * Checks to see if a parent Item is currently a child of a target item. If
     * so, then this would put the hierarchy into a loop and is not allowed.
     *
     * @param item
     * @param newParent
     * @throws ModelValidationException
     *             if newParent is child of item
     */
    protected void verifyNotInLoop(Item item, CollectionItem newParent) {
        // need to verify that the new parent is not a child
        // of the item, otherwise we get a loop
        if (item.getId().equals(newParent.getId()))
            throw new ModelValidationException(
                    "Invalid parent - will cause loop");
        
        // If item is not a collection then all is good
        if(!(item instanceof CollectionItem ))
            return;
        
        CollectionItem collection = (CollectionItem) item;
        getSession().refresh(collection);

        for (Item nextItem: collection.getChildren())
            verifyNotInLoop(nextItem, newParent);
    }

    /**
     * Find the DbItem with the specified dbId
     *
     * @param dbId
     *            dbId of DbItem to find
     * @return DbItem with specified dbId
     */
    protected Item findItemByDbId(Long dbId) {
        return (Item) getSession().get(Item.class, dbId);
    }


    // Set server generated item properties
    protected void setBaseItemProps(Item item) {
        if (item.getUid() == null)
            item.setUid(idGenerator.nextIdentifier().toString());
        for (Ticket ticket : item.getTickets()) {
            if (ticket.getOwner() == null)
                ticket.setOwner(item.getOwner());
            if (ticket.getKey() == null)
                ticket.setKey(ticketKeyGenerator.nextIdentifier().toString());
            if (ticket.getTimeout() == null)
                ticket.setTimeout(Ticket.TIMEOUT_INFINITE);
            ticket.setCreated(new Date());
        }
    }

    protected Item findItemByParentAndName(Long userDbId, Long parentDbId,
            String name) {
        Query hibQuery = null;
        if (parentDbId != null) {
            hibQuery = getSession().getNamedQuery(
                    "item.by.ownerId.parentId.name").setParameter("ownerid",
                    userDbId).setParameter("parentid", parentDbId)
                    .setParameter("name", name);
            
        } else {
            hibQuery = getSession().getNamedQuery(
                    "item.by.ownerId.nullParent.name").setParameter("ownerid",
                    userDbId).setParameter("name", name);
        }

        return (Item) hibQuery.uniqueResult();
    }
    
    protected Item findItemByParentAndNameMinusItem(Long userDbId, Long parentDbId,
            String name, Long itemId) {
        Query hibQuery = null;
        if (parentDbId != null) {
            hibQuery = getSession().getNamedQuery(
                    "item.by.ownerId.parentId.name.minusItem").setParameter("itemid", itemId)
                    .setParameter("ownerid",
                    userDbId).setParameter("parentid", parentDbId)
                    .setParameter("name", name);      
        } else {
            hibQuery = getSession().getNamedQuery(
                    "item.by.ownerId.nullParent.name.minusItem").setParameter("itemid", itemId)
                    .setParameter("ownerid",
                    userDbId).setParameter("name", name);
        }

        return (Item) hibQuery.uniqueResult();
    }
    
    protected HomeCollectionItem findRootItem(Long dbUserId) {
        Query hibQuery = getSession().getNamedQuery(
                "homeCollection.by.ownerId").setParameter("ownerid",
                dbUserId);
        hibQuery.setCacheable(true);
        

        return (HomeCollectionItem) hibQuery.uniqueResult();
    }

    protected void checkForDuplicateUid(Item item) {
        // verify uid not in use
        if (item.getUid() != null) {
            Item duplicate  = findAnyItemByUid(item.getUid());
            
            // if uid is in use and item is active, throw exception
            if(duplicate!=null && duplicate.getIsActive()) {
                throw new UidInUseException("uid " + item.getUid()
                        + " already in use");
            }
            else if(duplicate!=null){
                // Otherwise remove deleted item to make way for new one.
                // Initialize new Item with old Item's version to generate
                // correct CollectionItem hash codes.
                item.setVersion(duplicate.getVersion()+1);
                getSession().delete(duplicate);
            }
        }
    }
    
    protected void checkForDuplicateItemName(Long userDbId, Long parentDbId,
            String name) {
        if (findItemByParentAndName(userDbId, parentDbId, name) != null)
            throw new DuplicateItemNameException(name);
    }
    

    protected void checkForDuplicateItemNameMinusItem(Long userDbId,
            Set<CollectionItem> parents, String name, Long itemId) {

        for (CollectionItem parent : parents) {
            if (findItemByParentAndNameMinusItem(userDbId, parent.getId(),
                    name, itemId) != null)
                throw new DuplicateItemNameException(name);
        }
    }
    
    protected void checkForDuplicateItemNameMinusItem(Long userDbId, Long parentDbId,
            String name, Long itemId) {
        if (findItemByParentAndNameMinusItem(userDbId, parentDbId, name, itemId) != null)
            throw new DuplicateItemNameException(name);
    }
    
    protected Ticket getTicketRecursive(Item item, String key) {
        if(item==null)
            return null;
        
        for (Ticket ticket : item.getTickets()) {
            if (ticket.getKey().equals(key))
                return ticket;
        }
        
        for(Item parent: item.getParents()) {
            Ticket ticket = getTicketRecursive(parent, key);
            if(ticket!=null)
                return ticket;
        }
        
        return null;
    }
    
    protected void attachToSession(Item item) {
        if(getSession().contains(item))
            return;
        getSession().lock(item, LockMode.NONE);
    }
    
    protected void logInvalidStateException(InvalidStateException ise) {
        // log more info about the invalid state
        if(log.isDebugEnabled()) {
            log.debug(ise.getLocalizedMessage());
            for (InvalidValue iv : ise.getInvalidValues())
                log.debug("property name: " + iv.getPropertyName() + " value: "
                        + iv.getValue());
        }
    }
    
}
