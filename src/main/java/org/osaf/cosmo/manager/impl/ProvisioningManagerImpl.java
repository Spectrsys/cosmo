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
package org.osaf.cosmo.manager.impl;

import org.osaf.cosmo.dao.RoleDAO;
import org.osaf.cosmo.dao.ShareDAO;
import org.osaf.cosmo.dao.UserDAO;
import org.osaf.cosmo.manager.ProvisioningManager;
import org.osaf.cosmo.model.Role;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.CosmoSecurityManager;

import java.security.MessageDigest;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.id.StringIdentifierGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataRetrievalFailureException;

/**
 * Basic implementation of ProvisioningManager.
 *
 * @author Brian Moseley
 */
public class ProvisioningManagerImpl
    implements InitializingBean, ProvisioningManager {
    private static final Log log =
        LogFactory.getLog(ProvisioningManagerImpl.class);
    private RoleDAO roleDao;
    private ShareDAO shareDao;
    private UserDAO userDao;
    private MessageDigest digest;
    private StringIdentifierGenerator passwordGenerator;

    /**
     */
    public void setRoleDAO(RoleDAO roleDao) {
        this.roleDao = roleDao;
    }

    /**
     */
    public void setShareDAO(ShareDAO shareDao) {
        this.shareDao = shareDao;
    }

    /**
     */
    public void setUserDAO(UserDAO userDao) {
        this.userDao = userDao;
    }

    /**
     */
    public void setDigest(String algorithm) {
        if (algorithm != null) {
            try {
                digest = MessageDigest.getInstance(algorithm);
            } catch (Exception e) {
                throw new RuntimeException("cannot get message digest for algorithm " + algorithm, e);
            }
        }
    }

    // ProvisioningManager methods

    /**
     */
    public List getRoles() {
        return roleDao.getRoles();
    }

    /**
     */
    public Role getRole(String id) {
        return roleDao.getRole(new Long(id));
    }

    /**
     */
    public Role getRoleByName(String name) {
        return roleDao.getRole(name);
    }

    /**
     */
    public List getUsers() {
        return userDao.getUsers();
    }

    /**
     */
    public User getUser(String id) {
        return userDao.getUser(new Long(id));
    }

    /**
     */
    public User getUserByUsername(String username) {
        return userDao.getUserByUsername(username);
    }

    /**
     */
    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    /**
     */
    public User saveUser(User user) {
        user.validateRawPassword();
        user.setPassword(digestPassword(user.getPassword()));

        userDao.saveUser(user);

        if (! user.getUsername().equals(CosmoSecurityManager.USER_ROOT)) {
            shareDao.createHomedir(user.getUsername());
        }

        return userDao.getUserByUsername(user.getUsername());
    }

    /**
     */
    public User updateUser(User user) {
        if (user.getPassword().length() < 32) {
            user.validateRawPassword();
            user.setPassword(digestPassword(user.getPassword()));
        }

        userDao.updateUser(user);

        // if the username was changed, rename the home directory. do
        // this after the database update, because we can roll back
        // the database if the homedir rename fails, but not vice
        // versa.
        if (user.isUsernameChanged()) {
            shareDao.renameHomedir(user.getOldUsername(), user.getUsername());
        }

        return userDao.getUserByUsername(user.getUsername());
    }

    /**
     */
    public void removeUser(String id) {
        User user = getUser(id);
        if (! user.getUsername().equals(CosmoSecurityManager.USER_ROOT)) {
            try {
                shareDao.deleteHomedir(user.getUsername());
            } catch (DataRetrievalFailureException e) {
                // the account may be in an inconsistent state where
                // the homedir doesn't exist but the user record does;
                // we'll go ahead and ignore this exception and let
                // userDao throw an exception if the user record
                // doesn't exist either
            }
        }
        userDao.removeUser(user);
    }

    /**
     */
    public void removeUserByUsername(String username) {
        User user = getUserByUsername(username);
        if (! user.getUsername().equals(CosmoSecurityManager.USER_ROOT)) {
            try {
                shareDao.deleteHomedir(user.getUsername());
            } catch (DataRetrievalFailureException e) {
                // the account may be in an inconsistent state where
                // the homedir doesn't exist but the user record does;
                // we'll go ahead and ignore this exception and let
                // userDao throw an exception if the user record
                // doesn't exist either
            }
        }
        userDao.removeUser(user);
    }

    /**
     */
    public String generatePassword() {
        String password = passwordGenerator.nextStringIdentifier();
        return password.length() <= 16 ? password : password.substring(0, 15);
    }

    // InitializingBean methods

    /**
     * Sanity check the object's properties.
     */
    public void afterPropertiesSet() throws Exception {
        if (roleDao == null) {
            throw new IllegalArgumentException("roleDAO is required");
        }
        if (shareDao == null) {
            throw new IllegalArgumentException("shareDAO is required");
        }
        if (userDao == null) {
            throw new IllegalArgumentException("userDAO is required");
        }
    }

    // our methods

    /**
     */
    public void setPasswordGenerator(StringIdentifierGenerator generator) {
        passwordGenerator = generator;
    }

    // private methods

    private String digestPassword(String password) {
        if (digest == null || password == null) {
            return password;
        }
        return new String(Hex.encodeHex(digest.digest(password.getBytes())));
    }
}
