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
package org.osaf.cosmo.ui;

import org.osaf.commons.struts.OSAFStrutsConstants;
import org.osaf.cosmo.manager.ProvisioningManager;
import org.osaf.cosmo.model.Role;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.CosmoSecurityManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Action for managing users.
 */
public class UserAction extends CosmoAction {
    private static final String MSG_ERROR_EXISTS = "User.Form.Exists";
    private static final String MSG_CONFIRM_CREATE = "User.Form.Created";
    private static final String MSG_CONFIRM_UPDATE = "User.Form.Updated";
    private static final String MSG_CONFIRM_REMOVE = "User.Form.Removed";
    private static final Log log = LogFactory.getLog(UserAction.class);

    /**
     * The request parameter that contains the username identifying a
     * user.
     */
    public static final String PARAM_USERNAME = "username";
    /**
     * The request parameter that contains the id identifying a
     * user.
     */
    public static final String PARAM_ID = "id";
    /**
     * The request attribute in which this action places an
     * identified User: <code>User</code>
     */
    public static final String ATTR_USER = "User";
    /**
     * The request attribute in which this action places a List of
     * Users: <code>Users</code>
     */
    public static final String ATTR_USERS = "Users";

    private ProvisioningManager mgr;

    /**
     */
    public void setProvisioningManager(ProvisioningManager mgr) {
        this.mgr = mgr;
    }

    /**
     * Retrieves the identified user.
     */
    public ActionForward view(ActionMapping mapping,
                              ActionForm form,
                              HttpServletRequest request,
                              HttpServletResponse response)
        throws Exception {
        UserForm userForm = (UserForm) form;

        // the User may have previously been set by a request
        // attribute. if not, look to see if the form has id info. if
        // not, we're viewing the user for the first time.
        User user = (User) request.getAttribute(ATTR_USER);
        if (user == null) {
            if (userForm.getId() != null && ! userForm.getId().equals("")) {
                user = mgr.getUser(userForm.getId());
            }
            else {
                String username = request.getParameter(PARAM_USERNAME);
                if (log.isDebugEnabled()) {
                    log.debug("viewing user " + username);
                }
                user = mgr.getUserByUsername(username);
                populateUpdateForm(userForm, user);
            }
        }

        request.setAttribute(ATTR_USER, user);

        addTitleParam(request, user.getUsername());

        return mapping.findForward(OSAFStrutsConstants.FWD_OK);
    }

    /**
     * Creates the specified user.
     */
    public ActionForward create(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        UserForm userForm = (UserForm) form;
        User formUser = new User();
        populateUser(formUser, userForm);

        try {
            if (log.isDebugEnabled()) {
                log.debug("creating user " + formUser.getUsername());
            }
            User user = mgr.saveUser(formUser);

            request.setAttribute(ATTR_USER, user);
            saveConfirmationMessage(request, MSG_CONFIRM_CREATE);
        } catch (DataIntegrityViolationException e) {
            saveErrorMessage(request, MSG_ERROR_EXISTS, PARAM_USERNAME);
            return mapping.findForward(OSAFStrutsConstants.FWD_FAILURE);
        }

        return mapping.findForward(OSAFStrutsConstants.FWD_SUCCESS);
    }

    /**
     * Updates the specified user.
     */
    public ActionForward update(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        UserForm userForm = (UserForm) form;

        if (isCancelled(request)) {
            userForm.reset(mapping, request);
            return mapping.findForward(OSAFStrutsConstants.FWD_CANCEL);
        }

        try {
            User formUser =  mgr.getUser(userForm.getId());
            populateUser(formUser, userForm);
            if (log.isDebugEnabled()) {
                log.debug("updating user " + formUser.getUsername());
            }
            User user = mgr.updateUser(formUser);

            request.setAttribute(ATTR_USER, user);
            saveConfirmationMessage(request, MSG_CONFIRM_UPDATE);
        } catch (DataIntegrityViolationException e) {
            saveErrorMessage(request, MSG_ERROR_EXISTS, PARAM_USERNAME);
            return mapping.findForward(OSAFStrutsConstants.FWD_FAILURE);
        }

        return mapping.findForward(OSAFStrutsConstants.FWD_SUCCESS);
    }

    /**
     * Removes the identified user.
     */
    public ActionForward remove(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        String id = request.getParameter(PARAM_ID);

        if (id != null) {
            if (log.isDebugEnabled()) {
                log.debug("removing user " + id);
            }
            mgr.removeUser(id);

            saveConfirmationMessage(request, MSG_CONFIRM_REMOVE);
        }

        return mapping.findForward(OSAFStrutsConstants.FWD_SUCCESS);
    }

    /**
     * Retrieves a list of all users.
     */
    public ActionForward list(ActionMapping mapping,
                              ActionForm form,
                              HttpServletRequest request,
                              HttpServletResponse response)
        throws Exception {
        UserForm userForm = (UserForm) form;

        request.setAttribute(ATTR_USERS, getSortedUsers());

        return mapping.findForward(OSAFStrutsConstants.FWD_OK);
    }

    private List getSortedUsers() {
        if (log.isDebugEnabled()) {
            log.debug("listing users");
        }
        List users = mgr.getUsers();
        Collections.sort(users, new Comparator() {
                public int compare(Object o1, Object o2) {
                    User u1 = (User) o1;
                    User u2 = (User) o2;
                    String name1 = u1.getLastName() + " " + u1.getFirstName();
                    String name2 = u2.getLastName() + " " + u2.getFirstName();
                    return name1.compareTo(name2);
                }
            });

        return users;
    }

    private void populateUser(User user, UserForm form) {
        user.setUsername(form.getUsername());
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        user.setEmail(form.getEmail());
        if (form.getPassword() != null && ! form.getPassword().equals("")) {
            user.setPassword(form.getPassword());
        }

        Role userRole = mgr.getRoleByName(CosmoSecurityManager.ROLE_USER);
        user.addRole(userRole);

        Role rootRole = mgr.getRoleByName(CosmoSecurityManager.ROLE_ROOT);
        if (form.isAdmin()) {
            user.addRole(rootRole);
        }
        else {
            user.removeRole(rootRole);
        }
    }

    private void populateUpdateForm(UserForm form, User user) {
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        // never set password in the form
        form.setAdmin(user.isInRole(CosmoSecurityManager.ROLE_ROOT));
    }
}
