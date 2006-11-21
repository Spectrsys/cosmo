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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.dao.UserDao;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.CollectionSubscription;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.User;

public class HibernateUserDaoSubscriptionTest
    extends AbstractHibernateDaoTestCase {
    private static final Log log =
        LogFactory.getLog(HibernateUserDaoSubscriptionTest.class);
    
    protected ContentDaoImpl contentDao = null;
    protected UserDaoImpl userDao = null;
    
    public HibernateUserDaoSubscriptionTest() {
        super();
    }
    
    public void testSubscribe() throws Exception {
        User user = getUser(userDao, "subuser1");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);
        CollectionItem collection = getCollection(root, "subcoll1");
        Ticket ticket = generateTicket("/subuser1/subcoll1", user);

        CollectionSubscription sub1 = new CollectionSubscription();
        sub1.setDisplayName("sub1");
        sub1.setCollection(collection);
        sub1.setTicket(ticket);
        user.addSubscription(sub1);
        userDao.updateUser(user);

        clearSession();
        
        user = getUser(userDao, "subuser1");
        
        assertFalse("no subscriptions saved",
                    user.getCollectionSubscriptions().isEmpty());

        CollectionSubscription querySub = user
                .getSubscription("sub1");
        assertNotNull("sub1 not found", querySub);
        assertEquals("sub1 not same subscriber", user.getId(), querySub
                .getOwner().getId());
        assertEquals("sub1 not same collection", collection.getUid(), querySub
                .getCollectionUid());
        assertEquals("sub1 not same ticket", ticket.getKey(), querySub
                .getTicketKey());

        querySub.setDisplayName("sub2");
        userDao.updateUser(user);
        
        clearSession();
        
        user = getUser(userDao, "subuser1");
        
        querySub = user.getSubscription("sub1");
        assertNull("sub1 mistakenly found", querySub);

        querySub = user.getSubscription("sub2");
        assertNotNull("sub2 not found", querySub);

        user.removeSubscription(querySub);
        userDao.updateUser(user);

        clearSession();
        
        user = getUser(userDao, "subuser1");
        
        querySub = user.getSubscription("sub1");
        assertNull("sub1 mistakenly found", querySub);

        querySub = user.getSubscription("sub2");
        assertNull("sub2 mistakenly found", querySub);
    }

    private User getUser(UserDao userDao, String username) {
        return helper.getUser(userDao, contentDao, username);
    }

    private CollectionItem getCollection(CollectionItem parent,
                                         String name)
        throws Exception {
        for (Item child : (Set<Item>) parent.getChildren()) {
            if (child.getName().equals(name))
                return (CollectionItem) child;
        }
        CollectionItem collection = new CollectionItem();
        collection.setName(name);
        collection.setDisplayName(name);
        collection.setOwner(parent.getOwner());
        return contentDao.createCollection(parent, collection);
    }

    private Ticket generateTicket(String path,
                                  User owner) {
        Ticket ticket = new Ticket();
        ticket.setOwner(owner);
        ticket.setTimeout(Ticket.TIMEOUT_INFINITE);
        contentDao.createTicket(path, ticket);
        return ticket;
    }
}
