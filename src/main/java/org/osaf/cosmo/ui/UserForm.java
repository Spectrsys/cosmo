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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.validator.ValidatorForm;
import org.apache.struts.action.ActionMapping;

/**
 * Action for managing users.
 */
public class UserForm extends ValidatorForm {

    private String id;
    private String username;
    private String email;
    private String password;
    private String confirm;
    private String[] role;

    /**
     */
    public UserForm() {
        initialize();
    }

    /**
     */
    public String getId() {
        return id;
    }

    /**
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     */
    public void setId(Long id) {
        this.id = id.toString();
    }

    /**
     */
    public String getUsername() {
        return username;
    }

    /**
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     */
    public String getEmail() {
        return email;
    }

    /**
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     */
    public String getPassword() {
        return password;
    }

    /**
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     */
    public String getConfirm() {
        return confirm;
    }

    /**
     */
    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    /**
     */
    public String[] getRole() {
        return role;
    }

    /**
     */
    public void setRole(String[] role) {
        this.role = role;
    }

    /**
     */
    public void reset(ActionMapping mapping,
                      HttpServletRequest request) {
        super.reset(mapping, request);
        initialize();
    }

    /**
     */
    private void initialize() {
        username = null;
        email = null;
        password = null;
        confirm = null;
        role = new String[0];
    }
}
