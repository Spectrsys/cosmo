/*
 * Copyright 2005 Open Source Applications Foundation
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

import java.util.List;

import org.osaf.cosmo.BaseCoreTestCase;
import org.osaf.cosmo.TestHelper;
import org.osaf.cosmo.dao.RoleDAO;
import org.osaf.cosmo.dao.UserDAO;
import org.osaf.cosmo.model.Role;
import org.osaf.cosmo.model.User;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataRetrievalFailureException;

/**
 * DAO Test Case for Roles.
 *
 * @author Brian Moseley
 */
public class RoleDAOTest extends BaseCoreTestCase {
    private static final Log log = LogFactory.getLog(RoleDAOTest.class);

    private RoleDAO dao;
    private UserDAO userDao;

    public void testCRUDRole() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("BEGIN");
        }

        Role role = TestHelper.makeDummyRole();
        dao.saveRole(role);
        assertNotNull(role.getId());
        assertNotNull(role.getDateCreated());
        assertNotNull(role.getDateModified());

        // get by id
        Role role2 = dao.getRole(role.getId());
        assertTrue(role2.equals(role));
        assertEquals(role2.hashCode(), role.hashCode());

        // get by name
        Role role3 = dao.getRole(role.getName());
        assertTrue(role3.equals(role));
        assertEquals(role3.hashCode(), role.hashCode());

        // update doesn't do anything

        dao.removeRole(role);
        try {
            dao.getRole(role.getId());
            fail("role not removed");
        } catch (DataRetrievalFailureException e) {
            // expected
        }
    }

    public void testListRoles() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("BEGIN");
        }

        List roles = dao.getRoles();
    }

    public void testAddRemoveUser() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("BEGIN");
        }

        Role role = TestHelper.makeDummyRole();
        dao.saveRole(role);

        User user = TestHelper.makeDummyUser();
        userDao.saveUser(user);

        role.getUsers().add(user);
        dao.updateRole(role);

        Role role2 = dao.getRole(role.getId());
        assertTrue(role.getUsers().size() == 1);

        role.getUsers().remove(user);
        dao.updateRole(role);

        Role role3 = dao.getRole(role.getId());
        assertTrue(role.getUsers().isEmpty());

        dao.removeRole(role);
        userDao.removeUser(user);
    }

    public void setRoleDAO(RoleDAO roleDao) {
        dao = roleDao;
    }

    public void setUserDAO(UserDAO userDao) {
        this.userDao = userDao;
    }
}
