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
package org.osaf.cosmo.dav;

import org.apache.commons.id.random.SessionIdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.simple.LocatorFactoryImpl;

import org.osaf.cosmo.TestHelper;
import org.osaf.cosmo.dao.mock.MockCalendarDao;
import org.osaf.cosmo.dao.mock.MockContentDao;
import org.osaf.cosmo.dao.mock.MockDaoStorage;
import org.osaf.cosmo.dao.mock.MockUserDao;
import org.osaf.cosmo.dav.DavCollection;
import org.osaf.cosmo.dav.DavResourceFactory;
import org.osaf.cosmo.dav.StandardResourceFactory;
import org.osaf.cosmo.dav.impl.DavHomeCollection;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.CosmoSecurityManager;
import org.osaf.cosmo.security.mock.MockSecurityManager;
import org.osaf.cosmo.security.mock.MockTicketPrincipal;
import org.osaf.cosmo.security.mock.MockUserPrincipal;
import org.osaf.cosmo.service.ContentService;
import org.osaf.cosmo.service.UserService;
import org.osaf.cosmo.service.account.AutomaticAccountActivator;
import org.osaf.cosmo.service.impl.StandardContentService;
import org.osaf.cosmo.service.impl.StandardTriageStatusQueryProcessor;
import org.osaf.cosmo.service.impl.StandardUserService;
import org.osaf.cosmo.service.lock.SingleVMLockManager;
import org.osaf.cosmo.util.PathUtil;

/**
 */
public class DavTestHelper extends TestHelper {
    private static final Log log = LogFactory.getLog(DavTestHelper.class);

    // XXX: refactor to use MockHelper

    private MockSecurityManager securityManager;
    private StandardContentService contentService;
    private StandardUserService userService;
    private StandardResourceFactory resourceFactory;
    private LocatorFactoryImpl locatorFactory;
    private User user;
    private HomeCollectionItem homeCollection;
    private DavResourceLocator homeLocator;
    private DavHomeCollection homeResource;

    /** */
    public DavTestHelper() {
        this("");
    }

    /** */
    public DavTestHelper(String repositoryPrefix) {
        super();

        securityManager = new MockSecurityManager();

        MockDaoStorage storage = new MockDaoStorage();
        MockCalendarDao calendarDao = new MockCalendarDao(storage);
        MockContentDao contentDao = new MockContentDao(storage);
        MockUserDao userDao = new MockUserDao();
        SingleVMLockManager lockManager = new SingleVMLockManager();

        contentService = new StandardContentService();
        contentService.setCalendarDao(calendarDao);
        contentService.setContentDao(contentDao);
        contentService.setLockManager(lockManager);
        contentService.setTriageStatusQueryProcessor(new StandardTriageStatusQueryProcessor());
        contentService.init();

        userService = new StandardUserService();
        userService.setContentDao(contentDao);
        userService.setUserDao(userDao);
        userService.setPasswordGenerator(new SessionIdGenerator());
        userService.init();

        resourceFactory =
            new StandardResourceFactory(contentService, securityManager);

        locatorFactory = new LocatorFactoryImpl(repositoryPrefix);
    }

    /** */
    public void setUp() throws Exception {
        user = makeDummyUser();
        userService.createUser(user);

        homeCollection = contentService.getRootItem(user);
        homeLocator =
            locatorFactory.createResourceLocator("", "/" + user.getUsername());
    }

    /** */
    protected void tearDown() throws Exception {
        userService.removeUser(user);

        userService.destroy();
        contentService.destroy();
    }

    /**
     */
    public void logIn() {
        logInUser(user);
    }

    /**
     */
    public void logInUser(User u) {
        securityManager.setUpMockSecurityContext(new MockUserPrincipal(u));
    }

    /**
     */
    public void logInTicket(Ticket t) {
        securityManager.setUpMockSecurityContext(new MockTicketPrincipal(t));
    }

    /** */
    public CosmoSecurityManager getSecurityManager() {
        return securityManager;
    }

    /** */
    public ContentService getContentService() {
        return contentService;
    }

    /** */
    public UserService getUserService() {
        return userService;
    }

    /** */
    public DavResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    /** */
    public DavLocatorFactory getLocatorFactory() {
        return locatorFactory;
    }

    /** */
    public User getUser() {
        return user;
    }

    /** */
    public HomeCollectionItem getHomeCollection() {
        return homeCollection;
    }

    /** */
    public DavResourceLocator getHomeLocator() {
        return homeLocator;
    }

    /** */
    public DavHomeCollection initializeHomeResource()
        throws DavException {
        return new DavHomeCollection(homeCollection, homeLocator,
                                     resourceFactory);
    }

    /** */
    public CollectionItem makeAndStoreDummyCollection(CollectionItem parent)
        throws Exception {
        CollectionItem c = makeDummyCollection(user);
        return contentService.createCollection(parent, c);
    }

    /** */
    public DavResource getMember(DavCollection parent,
                                 String name)
        throws Exception {
        for (DavResourceIterator i = parent.getMembers(); i.hasNext();) {
            DavResource m = i.nextResource();
            if (PathUtil.getBasename(m.getResourcePath()).equals(name))
                return m;
        }
        return null;
    }
}
