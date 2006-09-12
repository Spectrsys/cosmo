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
package org.osaf.cosmo.dao.hibernate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.osaf.cosmo.dao.ContentDao;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.User;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 * Implementation of ContentDao using hibernate persistence objects
 * 
 */
public class ContentDaoImpl extends ItemDaoImpl implements ContentDao {

    private static final Log log = LogFactory.getLog(ContentDaoImpl.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#createCollection(org.osaf.cosmo.model.CollectionItem,
     *      org.osaf.cosmo.model.CollectionItem)
     */
    public CollectionItem createCollection(CollectionItem parent,
            CollectionItem collection) {

        if(parent==null)
            throw new IllegalArgumentException("parent cannot be null");
        
        if (collection == null)
            throw new IllegalArgumentException("collection cannot be null");

        if (collection.getId()!=-1)
            throw new IllegalArgumentException("invalid collection id (expected -1)");
        
        if (collection.getName() == null)
            throw new IllegalArgumentException("collection must have name");
        
        try {
            if (collection.getOwner() == null)
                throw new IllegalArgumentException("collection must have owner");

            User owner = collection.getOwner();
            
            // We need to enforce a content hierarchy to support WebDAV
            // In a hierarchy, can't have two items with same name with
            // same owner and parent
            checkForDuplicateItemName(owner.getId(), parent.getId(), collection
                    .getName());
            
            setBaseItemProps(collection);
            collection.setParent(parent);
            getSession().save(collection);
            getSession().flush();
            
            return collection;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } 
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#createContent(org.osaf.cosmo.model.CollectionItem,
     *      org.osaf.cosmo.model.ContentItem)
     */
    public ContentItem createContent(CollectionItem parent, ContentItem content) {

        if(parent==null)
            throw new IllegalArgumentException("parent cannot be null");
        
        if (content == null)
            throw new IllegalArgumentException("content cannot be null");

        if (content.getId()!=-1)
            throw new IllegalArgumentException("invalid content id (expected -1)");
        
        if (content.getName() == null)
            throw new IllegalArgumentException("content must have name");

        try {
            User owner = content.getOwner();

            if (owner == null)
                throw new IllegalArgumentException("content must have owner");

          
            // Enforce hiearchy for WebDAV support
            // In a hierarchy, can't have two items with same name with
            // same parent
            checkForDuplicateItemName(owner.getId(), parent.getId(), content.getName());

            
            setBaseItemProps(content);
            content.setParent(parent);
            getSession().save(content);
            getSession().flush();
            
            return content;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } 
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#findChildren(org.osaf.cosmo.model.CollectionItem)
     */
    public Set<Item> findChildren(CollectionItem collection) {

        try {
            getSession().refresh(collection);
            return collection.getChildren();
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#findCollectionByUid(java.lang.String)
     */
    public CollectionItem findCollectionByUid(String uid) {
        try {
            List results = getSession().getNamedQuery("collectionItem.by.uid").setParameter("uid", uid).list();
            if(results.size()>0)
                return (CollectionItem) results.get(0);
            else
                return null;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#findCollectionByPath(java.lang.String)
     */
    public CollectionItem findCollectionByPath(String path) {
        try {
            Item item = getItemPathTranslator().findItemByPath(path);
            if (item == null || !(item instanceof CollectionItem) )
                return null;

            return (CollectionItem) item;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#findContentByPath(java.lang.String)
     */
    public ContentItem findContentByPath(String path) {
        try {
            Item item = getItemPathTranslator().findItemByPath(path);
            if (item == null || !(item instanceof ContentItem) )
                return null;

            return (ContentItem) item;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } 
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#findContentByUid(java.lang.String)
     */
    public ContentItem findContentByUid(String uid) {
        try {
            List results = getSession().getNamedQuery("contentItem.by.uid").setParameter("uid", uid).list();
            if(results.size()>0)
                return (ContentItem) results.get(0);
            else
                return null;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#updateCollection(org.osaf.cosmo.model.CollectionItem)
     */
    public CollectionItem updateCollection(CollectionItem collection) {
        try {
            
            if (collection == null)
                throw new IllegalArgumentException("collection cannot be null");
            
            if (collection.getName() == null || "".equals(collection.getName()))
                throw new IllegalArgumentException("collection must have name");
            
            // In a hierarchy, can't have two items with same name with
            // same parent
            if (collection.getParent() != null)
                checkForDuplicateItemNameMinusItem(collection.getOwner().getId(), 
                    collection.getParent().getId(), collection.getName(), collection.getId());
            
            getSession().update(collection);
            return collection;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } 
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#updateContent(org.osaf.cosmo.model.ContentItem)
     */
    public ContentItem updateContent(ContentItem content) {
        try {     
            
            if (content == null)
                throw new IllegalArgumentException("content cannot be null");
            
            if(content.getParent()==null)
                throw new IllegalArgumentException("parent cannot be null");
            
            if (content.getName() == null || "".equals(content.getName()))
                throw new IllegalArgumentException("content must have name");
            
            // In a hierarchy, can't have two items with same name with
            // same parent
            checkForDuplicateItemNameMinusItem(content.getOwner().getId(), 
                    content.getParent().getId(), content.getName(), content.getId());
            
            getSession().update(content);
            return content;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#removeCollection(org.osaf.cosmo.model.CollectionItem)
     */
    public void removeCollection(CollectionItem collection) {
        removeItem(collection);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#removeContent(org.osaf.cosmo.model.ContentItem)
     */
    public void removeContent(ContentItem content) {
        removeItem(content);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.hibernate.ItemDaoImpl#findItemByPath(java.lang.String)
     */
    public Item findItemByPath(String path) {

        try {
            Item item = getItemPathTranslator().findItemByPath(path);
            return item;
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#findChildren(org.osaf.cosmo.model.User)
     */
    public Set<Item> findChildren(User user) {
        try {
            CollectionItem rootItem = getRootItem(user);
            if(rootItem!=null)
                return rootItem.getChildren();
            else
                return new HashSet<Item>(0);
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osaf.cosmo.dao.ContentDao#moveContent(org.osaf.cosmo.model.CollectionItem,
     *      org.osaf.cosmo.model.Item)
     */
    public void moveContent(CollectionItem parent, Item content) {
        try {
            
            if (parent == null)
                throw new IllegalArgumentException("Invalid parent collection");

            if (content == null)
                throw new IllegalArgumentException("Invalid Item");

            Item item = findItemByUid(content.getUid());

            Item currentParent = item.getParent();

            // can't move root collection
            if (currentParent == null)
                throw new IllegalArgumentException("Invalid parent collection");

            // need to verify we aren't creating a loop
            verifyNotInLoop(item, parent);

            content.setParent(parent);
            getSession().update(content);
            getSession().flush();
        } catch (HibernateException e) {
            throw SessionFactoryUtils.convertHibernateAccessException(e);
        } 
    }
}
