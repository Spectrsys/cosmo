/*
 * Copyright 2006-2007 Open Source Applications Foundation
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.apache.jackrabbit.webdav.xml.Namespace;

import org.osaf.cosmo.dav.BadRequestException;
import org.osaf.cosmo.dav.ConflictException;
import org.osaf.cosmo.dav.DavCollection;
import org.osaf.cosmo.dav.DavException;
import org.osaf.cosmo.dav.DavResource;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.ExistsException;
import org.osaf.cosmo.dav.ExtendedDavConstants;
import org.osaf.cosmo.dav.ForbiddenException;
import org.osaf.cosmo.dav.LockedException;
import org.osaf.cosmo.dav.NotFoundException;
import org.osaf.cosmo.dav.PreconditionFailedException;
import org.osaf.cosmo.dav.ProtectedPropertyModificationException;
import org.osaf.cosmo.dav.UnprocessableEntityException;
import org.osaf.cosmo.dav.ticket.TicketConstants;
import org.osaf.cosmo.dav.ticket.property.TicketDiscovery;
import org.osaf.cosmo.model.Attribute;
import org.osaf.cosmo.model.CalendarCollectionStamp;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.CollectionLockedException;
import org.osaf.cosmo.model.DataSizeException;
import org.osaf.cosmo.model.DuplicateItemNameException;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ItemNotFoundException;
import org.osaf.cosmo.model.QName;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.CosmoSecurityManager;
import org.osaf.cosmo.service.ContentService;
import org.osaf.cosmo.util.PathUtil;

/**
 * Base class for implementations of <code>DavResource</code>
 * which provides behavior common to all resources.
 *
 * This class defines the following live properties:
 *
 * <ul>
 * <li><code>DAV:getcreationdate</code> (protected)</li>
 * <li><code>DAV:displayname</code> (protected)</li>
 * <li><code>DAV:iscollection</code> (protected)</li>
 * <li><code>DAV:resourcetype</code> (protected)</li>
 * <li><code>ticket:ticketdiscovery</code> (protected)</li>
 * <li><code>cosmo:uuid</code> (protected)</li>
 * </ul>
 *
 * Note that all of these properties are protected and cannot be
 * modified or removed.
 *
 * This class does not define any resource types.
 *
 * @see org.apache.jackrabbit.webdav.DavResource
 * @see DavResource
 */
public abstract class DavResourceBase
    implements ExtendedDavConstants, DavResource, TicketConstants {
    private static final Log log =
        LogFactory.getLog(DavResourceBase.class);

    private static final Set<DavPropertyName> LIVE_PROPERTIES =
        new HashSet<DavPropertyName>();

    private DavResourceLocator locator;
    private DavResourceFactory factory;
    private Item item;
    private DavPropertySet properties;
    private DavCollection parent;

    static {
        registerLiveProperty(DavPropertyName.CREATIONDATE);
        registerLiveProperty(DavPropertyName.DISPLAYNAME);
        registerLiveProperty(DavPropertyName.ISCOLLECTION);
        registerLiveProperty(DavPropertyName.RESOURCETYPE);
        registerLiveProperty(UUID);
        registerLiveProperty(TICKETDISCOVERY);
    }

    /** */
    public DavResourceBase(Item item,
                           DavResourceLocator locator,
                           DavResourceFactory factory) {
        this.item = item;
        this.locator = locator;
        this.factory = factory;
        this.properties = new DavPropertySet();

        loadProperties();
    }

    // DavResource methods

    /** */
    public String getComplianceClass() {
        return DavResource.COMPLIANCE_CLASS;
    }

    /** */
    public boolean exists() {
        return item != null && item.getUid() != null;
    }

    /** */
    public boolean isCollection() {
        throw new UnsupportedOperationException();
    }

    /** */
    public String getDisplayName() {
        return item.getDisplayName();
    }

    /** */
    public String getETag() {
        if (getItem() == null)
            return null;
        return "\"" + getItem().getEntityTag() + "\"";
    }

    /** */
    public DavResourceLocator getLocator() {
        return locator;
    }

    /** */
    public String getResourcePath() {
        return locator.getResourcePath();
    }

    /** */
    public String getHref() {
        return locator.getHref(isCollection());
    }

    /** */
    public DavPropertyName[] getPropertyNames() {
        return properties.getPropertyNames();
    }

    /** */
    public DavProperty getProperty(DavPropertyName name) {
        return properties.get(name);
    }

    /** */
    public DavPropertySet getProperties() {
        return properties;
    }

    /**
     * Sets the given DAV property on the resource.
     *
     * Attempts to interpret the property as a live property. If that
     * fails, then sets the property as a dead property.
     *
     * @param property the property to set
     *
     * @see #setLiveProperty(DavProperty)
     */
    public void setProperty(DavProperty property)
        throws org.apache.jackrabbit.webdav.DavException {
        if (! exists())
            throw new NotFoundException();

        setResourceProperty(property);
    }

    /**
     * Removes the named DAV property from the resource.
     *
     * Attempts to interpret the property as a live property. If that
     * fails, then assumes the property is a dead property.
     *
     * @param propertyName the name of the property to set
     *
     * @see #removeLiveProperty(DavPropertyName)
     */
    public void removeProperty(DavPropertyName propertyName)
        throws org.apache.jackrabbit.webdav.DavException {
        if (! exists())
            throw new NotFoundException();

        removeResourceProperty(propertyName);

        try {
            getContentService().updateItem(item);
        } catch (CollectionLockedException e) {
            throw new LockedException();
        }
    }

    public MultiStatusResponse
        alterProperties(DavPropertySet setProperties,
                        DavPropertyNameSet removePropertyNames)
        throws org.apache.jackrabbit.webdav.DavException {
        throw new UnsupportedOperationException();
    }

    public MultiStatusResponse
        updateProperties(DavPropertySet setProperties,
                         DavPropertyNameSet removePropertyNames)
        throws DavException {
        if (! exists())
            throw new NotFoundException();

        MultiStatusResponse msr = new MultiStatusResponse(getHref(), null);

        ArrayList<DavPropertyName> df = new ArrayList<DavPropertyName>();
        DavException error = null;
        DavPropertyName failed = null;

        DavProperty property = null;
        for (DavPropertyIterator i=setProperties.iterator(); i.hasNext();) {
            try {
                property = i.nextProperty();
                setResourceProperty(property);
                df.add(property.getName());
                msr.add(property.getName(), 200);
            } catch (DavException e) {
                // we can only report one error message in the
                // responsedescription, so even if multiple properties would
                // fail, we return 424 for the second and subsequent failures
                // as well
                if (error == null) {
                    error = e;
                    failed = property.getName();
                } else {
                    df.add(property.getName());
                }
            }
        }

        DavPropertyName name = null;
        for (DavPropertyNameIterator i=removePropertyNames.iterator();
             i.hasNext();) {
            try {
                name = (DavPropertyName) i.next();
                removeResourceProperty(name);
                df.add(name);
                msr.add(name, 200);
            } catch (DavException e) {
                // we can only report one error message in the
                // responsedescription, so even if multiple properties would
                // fail, we return 424 for the second and subsequent failures
                // as well
                if (error == null) {
                    error = e;
                    failed = name;
                } else {
                    df.add(name);
                }
            }
        }

        if (error != null) {
            // replace the other response with a new one, since we have to
            // change the response code for each of the properties that would
            // have been set successfully
            msr = new MultiStatusResponse(getHref(), error.getMessage());
            for (DavPropertyName n : df)
                msr.add(n, 424);
            msr.add(failed, error.getErrorCode());
            return msr;
        }

        try {
            getContentService().updateItem(item);
        } catch (CollectionLockedException e) {
            throw new LockedException();
        }

        return msr;
    }

    /** */
    public DavResource getCollection() {
        return getParent();
    }

    /** */
    public void move(org.apache.jackrabbit.webdav.DavResource destination)
        throws org.apache.jackrabbit.webdav.DavException {
        if (! exists())
            throw new NotFoundException();
       
        if (log.isDebugEnabled())
            log.debug("moving resource " + getResourcePath() + " to " +
                      destination.getResourcePath());

        try {
            getContentService().moveItem(getResourcePath(), destination.getResourcePath());
        } catch (ItemNotFoundException e) {
            throw new ConflictException("One or more intermediate collections must be created");
        } catch (DuplicateItemNameException e) {
            throw new ExistsException();
        } catch (CollectionLockedException e) {
            throw new LockedException();
        }
    }

    /** */
    public void copy(org.apache.jackrabbit.webdav.DavResource destination,
                     boolean shallow)
        throws org.apache.jackrabbit.webdav.DavException {
        if (! exists())
            throw new NotFoundException();

        if (log.isDebugEnabled())
            log.debug("copying resource " + getResourcePath() + " to " +
                      destination.getResourcePath());

        try {
            getContentService().copyItem(item, destination.getResourcePath(),
                                         ! shallow);
        } catch (ItemNotFoundException e) {
            throw new ConflictException("One or more intermediate collections must be created");
        } catch (DuplicateItemNameException e) {
            throw new ExistsException();
        } catch (CollectionLockedException e) {
            throw new LockedException();
        }
    }

    /** */
    public boolean isLockable(Type type,
                              Scope scope) {
        // nothing is lockable at the moment
        return false;
    }

    /** */
    public boolean hasLock(Type type,
                           Scope scope) {
        // nothing is lockable at the moment
        throw new UnsupportedOperationException();
    }

    /** */
    public ActiveLock getLock(Type type,
                              Scope scope) {
        // nothing is lockable at the moment
        throw new UnsupportedOperationException();
    }

    /** */
    public ActiveLock[] getLocks() {
        // nothing is lockable at the moment
        throw new UnsupportedOperationException();
    }

    /** */
    public ActiveLock lock(LockInfo reqLockInfo)
        throws org.apache.jackrabbit.webdav.DavException {
        // nothing is lockable at the moment
        throw new PreconditionFailedException("Resource not lockable");
    }

    /** */
    public ActiveLock refreshLock(LockInfo reqLockInfo,
                                  String lockToken)
        throws org.apache.jackrabbit.webdav.DavException {
        // nothing is lockable at the moment
        throw new PreconditionFailedException("Resource not lockable");
    }

    /** */
    public void unlock(String lockToken)
        throws org.apache.jackrabbit.webdav.DavException {
        // nothing is lockable at the moment
        throw new PreconditionFailedException("Resource not lockable");
    }

    /** */
    public void addLockManager(LockManager lockmgr) {
        // nothing is lockable at the moment
        throw new UnsupportedOperationException();
    }

    /** */
    public org.apache.jackrabbit.webdav.DavResourceFactory getFactory() {
        return null;
    }

    /** */
    public DavSession getSession() {
        return null;
    }

    // DavResource methods

    public DavCollection getParent() {
        if (parent == null) {
            String parentPath = PathUtil.getParentPath(getResourcePath());
            DavResourceLocator parentLocator =
                getLocator().getFactory().
                    createResourceLocator(getLocator().getPrefix(),
                                          getLocator().getWorkspacePath(),
                                          parentPath);
            parent = (DavCollection) factory.resolve(parentLocator);
        }

        return parent;
    }

    public void saveTicket(Ticket ticket)
        throws DavException {
        if (log.isDebugEnabled())
            log.debug("adding ticket for " + item.getName());

        getContentService().createTicket(item, ticket);
    }

    public void removeTicket(Ticket ticket)
        throws DavException {
        if (log.isDebugEnabled())
            log.debug("removing ticket " + ticket.getKey() + " on " +
                      item.getName());

        getContentService().removeTicket(item, ticket);
    }

    public Ticket getTicket(String id) {
        for (Iterator i=item.getTickets().iterator(); i.hasNext();) {
            Ticket t = (Ticket) i.next();
            if (t.getKey().equals(id))
                return t;
        }
        return null;
    }

    public Set<Ticket> getTickets() {
        return getSecurityManager().getSecurityContext().
            findVisibleTickets(item);
    }

    public DavResourceFactory getResourceFactory() {
        return factory;
    }

    // our methods

    protected ContentService getContentService() {
        return factory.getContentService();
    }

    protected CosmoSecurityManager getSecurityManager() {
        return factory.getSecurityManager();
    }

    protected Item getItem() {
        return item;
    }

    protected void setItem(Item item) {
        this.item = item;
        loadProperties();
    }

    /**
     * Returns the DAV resource type codes for this resource.
     *
     * @see ResourceType
     */
    protected abstract int[] getResourceTypes();

    /**
     * Sets the properties of the item backing this resource from the
     * given input context. 
     */
    protected void populateItem(InputContext inputContext)
        throws DavException {
        if (log.isDebugEnabled())
            log.debug("populating item for " + getResourcePath());

        if (item.getUid() == null) {
            item.setName(PathUtil.getBasename(getResourcePath()));
            if (item.getDisplayName() == null)
                item.setDisplayName(item.getName());
        }

        // if we don't know specifically who the user is, then the
        // owner of the resource becomes the person who issued the
        // ticket
        User owner = getSecurityManager().getSecurityContext().getUser();
        if (owner == null) {
            Ticket ticket = getSecurityManager().getSecurityContext().
                getTicket();
            owner = ticket.getOwner();
        }
        item.setOwner(owner);

        if (item.getUid() == null) {
            item.setClientCreationDate(Calendar.getInstance().getTime());
            item.setClientModifiedDate(item.getClientCreationDate());
        }
    }

    /**
     * Sets the attributes the item backing this resource from the
     * given property set.
     */
    protected MultiStatusResponse populateAttributes(DavPropertySet properties) {
        if (log.isDebugEnabled())
            log.debug("populating attributes for " + getResourcePath());

        MultiStatusResponse msr = new MultiStatusResponse(getHref(), null);
        if (properties == null)
            return msr;

        DavProperty property = null;
        ArrayList<DavPropertyName> df = new ArrayList<DavPropertyName>();
        DavException error = null;
        DavPropertyName failed = null;
        for (DavPropertyIterator i=properties.iterator(); i.hasNext();) {
            try {
                property = i.nextProperty();
                setResourceProperty(property);
                df.add(property.getName());
                msr.add(property.getName(), 200);
            } catch (DavException e) {
                // we can only report one error message in the
                // responsedescription, so even if multiple properties would
                // fail, we return 424 for the second and subsequent failures
                // as well
                if (error == null) {
                    error = e;
                    failed = property.getName();
                } else {
                    df.add(property.getName());
                }
            }
        }

        if (error == null)
            return msr;

        // replace the other response with a new one, since we have to
        // change the response code for each of the properties that would
        // have been set successfully
        msr = new MultiStatusResponse(getHref(), error.getMessage());
        for (DavPropertyName n : df)
            msr.add(n, 424);
        msr.add(failed, error.getErrorCode());

        return msr;
    }

    /**
     * Registers the name of a live property.
     *
     * Typically used in subclass static initializers to add to the
     * list of live properties which are often exposed differently
     * in the model than dead properties.
     */
    protected static void registerLiveProperty(DavPropertyName name) {
        LIVE_PROPERTIES.add(name);
    }

    /**
     * Determines whether or not the given property name identifies a
     * live property.
     * 
     * If the server understands the semantic meaning of a property
     * (probably because the property is defined in a DAV-related
     * specification somewhere), then the property is defined as
     * "live". Live properties typically have their own
     * <code>Item</code> accessor methods with strong typing and often
     * particular semantics.
     *
     * If the server does not know anything specific about the
     * property (usually because it was defined by a particular
     * client), then it is known as a "dead" property. Dead properties
     * are stored as <code>Attribute</code>s with names of the form
     * <code>&lt;namespace prefix&gt;:&lt;namespace URI&gt;&lt;local name&gt;</code>.
     */
    protected boolean isLiveProperty(DavPropertyName name) {
        return LIVE_PROPERTIES.contains(name);
    }

    /**
     * Loads the live DAV properties for the resource.
     */
    protected void loadLiveProperties() {
        if (item == null)
            return;

        long creationTime = item.getCreationDate() != null ?
            item.getCreationDate().getTime() :
            new Date().getTime();
        properties.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE,
                                              IOUtil.getCreated(creationTime)));

        properties.add(new DefaultDavProperty(DavPropertyName.DISPLAYNAME,
                                              item.getDisplayName()));

        properties.add(new ResourceType(getResourceTypes()));

        // Windows XP support
        properties.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION,
                                              isCollection() ? "1" : "0"));

        properties.add(new TicketDiscovery(this));

        properties.add(new DefaultDavProperty(UUID, item.getUid()));
    }

    /**
     * Sets a live DAV property on the resource.
     *
     * If the given property is a live property, then the backing
     * <code>Item</code> is updated. If the property is dead, then the
     * <code>Item</code> is not updated. This method does not persist
     * the changes to the <code>Item</code>. That must be done by the
     * caller.
     *
     * @param property the property to set
     *
     * @throws DavException if the property is protected
     * or if a null value is specified for a property that does not
     * accept them
     */
    protected void setLiveProperty(DavProperty property)
        throws DavException {
        if (item == null)
            return;

        DavPropertyName name = property.getName();
        if (property.getValue() == null)
            throw new UnprocessableEntityException("Property " + name + " requires a value");
        String value = property.getValue().toString();

        if (name.equals(TICKETDISCOVERY) ||
            name.equals(UUID))
            throw new ProtectedPropertyModificationException(name);

        if (name.equals(DavPropertyName.DISPLAYNAME))
            item.setDisplayName(value);
    }

    /**
     * Removes a live DAV property from the resource.
     *
     * If the given property is a live property, then the backing
     * <code>Item</code> is updated. If the property is dead, then the
     * <code>Item</code> is not updated. This method does not persist
     * the changes to the <code>Item</code>. That must be done by the
     * caller.
     *
     * @param name the name of the property to remove
     *
     * @throws DavException if the property is protected
     */
    protected void removeLiveProperty(DavPropertyName name)
        throws DavException {
        if (item == null)
            return;

        if (name.equals(TICKETDISCOVERY) ||
            name.equals(UUID) ||
            name.equals(DavPropertyName.DISPLAYNAME))
            throw new ProtectedPropertyModificationException(name);
    }

    /**
     * Returns a list of names of <code>Attribute</code>s that should
     * not be exposed through DAV as dead properties.
     */
    protected abstract Set<String> getDeadPropertyFilter();

    private void loadProperties() {
        if (! exists())
            return;

        if (log.isDebugEnabled())
            log.debug("loading properties for " + getResourcePath());

        // load subclass live properties
        loadLiveProperties();
        
        // load dead properties
        for (Iterator<Map.Entry<QName,Attribute>>
                 i=item.getAttributes().entrySet().iterator(); i.hasNext();) {
            Map.Entry<QName,Attribute> entry = i.next();

            // skip attributes that are not meant to be shown as dead
            // properties
            if (getDeadPropertyFilter().contains(entry.getKey().getNamespace()))
                continue;

            DavPropertyName propName = qNameToPropName(entry.getKey());

            // ignore live properties, as they'll be loaded separately
            if (isLiveProperty(propName))
                continue;

            properties.add(new DefaultDavProperty(propName,
                                                  entry.getValue().getValue()));
        }
    }

    private void setResourceProperty(DavProperty property)
        throws DavException {
        String value = property.getValue() != null ?
            property.getValue().toString() :
            null;

        if (log.isDebugEnabled())
            log.debug("setting property " + property.getName() + " on " +
                      getResourcePath() + " to " + value);

        if (isLiveProperty(property.getName()))
            setLiveProperty(property);
        else {
            if (property.getValue() == null ||
                StringUtils.isBlank(property.getValue().toString()))
                throw new UnprocessableEntityException("Property " + property.getName() + " requires a value");
            try {
                item.setAttribute(propNameToQName(property.getName()), value);
            } catch (DataSizeException e) {
                throw new ForbiddenException(e.getMessage());
            }
        }
        
        properties.add(property);
    }

    private void removeResourceProperty(DavPropertyName name)
        throws DavException {
        if (log.isDebugEnabled())
            log.debug("removing property " + name + " on " +
                      getResourcePath());

        if (isLiveProperty(name))
            removeLiveProperty(name);
        else         
            item.removeAttribute(propNameToQName(name));
        
        properties.remove(name);
    }

    private QName propNameToQName(DavPropertyName name) {
       
        String uri = name.getNamespace() != null ?
            name.getNamespace().getURI() : "";
        return new QName(uri, name.getName());
    }

    private DavPropertyName qNameToPropName(QName qname) {
        // no namespace at all
        if ("".equals(qname.getNamespace()))
            return DavPropertyName.create(qname.getLocalName());

        Namespace ns =  Namespace.getNamespace(qname.getNamespace());
        
        return DavPropertyName.create(qname.getLocalName(), ns);
    }
}
