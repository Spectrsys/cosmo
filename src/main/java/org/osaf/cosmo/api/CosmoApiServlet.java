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
package org.osaf.cosmo.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.osaf.cosmo.manager.ProvisioningManager;
import org.osaf.cosmo.model.ModelValidationException;
import org.osaf.cosmo.model.DuplicateEmailException;
import org.osaf.cosmo.model.DuplicateUsernameException;
import org.osaf.cosmo.model.Role;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.CosmoSecurityManager;

import org.springframework.beans.BeansException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A servlet which implements a REST-style API for Cosmo.
 *
 * The API defines the following resources:
 *
 * <dl>
 * <dt><code>Users</code></dt>
 * <dd>A resource representing a collection of user resources</dd>
 * <dt><code>User</code></dt>
 * <dd>A resource representing an individual user</dd>
 * </dl>
 *
 * The API defines the following admin-only operations:
 *
 * <dl>
 * <dt><code>GET /users</code></dt>
 * <dd>Returns an XML representation of the <code>Users</code> resource collecting all Cosmo users as per {@link UsersResource}.</dd>
 * <dt><code>GET /user/&lgt;username&gt;</code></dt>
 * <dd>Returns an XML representation of a user as per {@link UserResource}.</dd>
 * <dt><code>PUT /user/&lgt;username&gt;</code></dt>
 * <dd>Includes an XML representation of a user as per {@link UserResource}, creating or modifying the user's properties within Cosmo, with all associated side effects including home directory creation.</dd>
 * <dt><code>DELETE /user/&lgt;username&gt;</code></dt>
 * <dd>Causes a user to be removed, with all associated side effects including home directory removal.</dd>
 * </dl>
 *
 * The API defines the following authenticated operations:
 *
 * <dl>
 * <dt><code>GET /account</code></td>
 * <dd>Returns an XML representation of the authenticated user as per {@link UserResource}.</dd>
 * <dt><code>PUT /account</code></dt>
 * <dd>Includes an XML representation of the authenticated user as per {@link UserResource}, modifying the user's properties within Cosmo.</dd>
 * </dl>
 *
 * The API defines the following anonymous (unauthenticated)
 * operations:
 *
 * <dl>
 * <dt><code>PUT /signup</code></dt>
 * <dd>Includes an XML representation of a user, creating a user
 * account and home directory within Cosmo.
 * </dl>
 */
public class CosmoApiServlet extends HttpServlet {
    private static final Log log = LogFactory.getLog(CosmoApiServlet.class);

    private static final String BEAN_PROVISIONING_MANAGER =
        "provisioningManager";
    private static final String BEAN_SECURITY_MANAGER =
        "securityManager";

    private WebApplicationContext wac;
    private ProvisioningManager provisioningManager;
    private CosmoSecurityManager securityManager;

    /**
     * Load the servlet context's
     * {@link org.springframework.web.context.WebApplicationContext}
     * and look up support objects.
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();

        wac = WebApplicationContextUtils.
            getRequiredWebApplicationContext(getServletContext());

        provisioningManager = (ProvisioningManager)
            getBean(BEAN_PROVISIONING_MANAGER, ProvisioningManager.class);
        securityManager = (CosmoSecurityManager)
            getBean(BEAN_SECURITY_MANAGER, CosmoSecurityManager.class);
    }

    // HttpServlet methods

    /**
     * Responds to the following operations:
     *
     * <ul>
     * <li><code>DELETE /user/&lgt;username&gt;</code></li>
     * </ul>
     */
    protected void doDelete(HttpServletRequest req,
                            HttpServletResponse resp)
        throws ServletException, IOException {
        if (req.getPathInfo().startsWith("/user/")) {
            processUserDelete(req, resp);
            return;
        }
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Responds to the following operations:
     *
     * <ul>
     * <li><code>GET /account</code></li>
     * <li><code>GET /users</code></li>
     * <li><code>GET /user/&lgt;username&gt;</code></li>
     * </ul>
     */
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
        throws ServletException, IOException {
        if (req.getPathInfo().equals("/account")) {
            processAccountGet(req, resp);
            return;
        }
        if (req.getPathInfo().equals("/users")) {
            processUsersGet(req, resp);
            return;
        }
        if (req.getPathInfo().startsWith("/user/")) {
            processUserGet(req, resp);
            return;
        }
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Responds to the following operations:
     *
     * <ul>
     * <li><code>PUT /signup</code></li>
     * <li><code>PUT /account</code></li>
     * <li><code>PUT /user/&lgt;username&gt;</code></li>
     * </ul>
     */
    protected void doPut(HttpServletRequest req,
                         HttpServletResponse resp)
        throws ServletException, IOException {
        if (! checkPutPreconditions(req, resp)) {
            return;
        }
        if (req.getPathInfo().equals("/signup")) {
            processSignup(req, resp);
            return;
        }
        if (req.getPathInfo().equals("/account")) {
            processAccountUpdate(req, resp);
            return;
        }
        if (req.getPathInfo().startsWith("/user/")) {
            String urlUsername = usernameFromPathInfo(req.getPathInfo());
            if (urlUsername == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            try {
                User user = provisioningManager.getUserByUsername(urlUsername);
                processUserUpdate(req, resp, user);
                return;
            } catch (ObjectRetrievalFailureException e) {
                processUserCreate(req, resp);
                return;
            }
        }
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    // our methods

    /**
     * Enforces preconditions on all PUT requests, including content
     * length and content type checks. Returns <code>true</code> if
     * all preconditions are met, otherwise sets the appropriate
     * error response code and returns <code>false</code>.
     */
    protected boolean checkPutPreconditions(HttpServletRequest req,
                                            HttpServletResponse resp) {
        if (req.getContentLength() <= 0) {
            resp.setStatus(HttpServletResponse.SC_LENGTH_REQUIRED);
            return false;
        }
        if (req.getContentType() == null ||
            ! req.getContentType().startsWith("text/xml")) {
            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return false;
        }
        if (req.getHeader("Content-Transfer-Encoding") != null ||
            req.getHeader("Content-Encoding") != null ||
            req.getHeader("Content-Base") != null ||
            req.getHeader("Content-Location") != null ||
            req.getHeader("Content-MD5") != null ||
            req.getHeader("Content-Range") != null) {
            resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return false;
        }
        return true;
    }

    /**
     * Delegated to by {@link #doDelete} to handle user DELETE
     * requests, removing the user and setting the response status and
     * headers.
     */
    protected void processUserDelete(HttpServletRequest req,
                                     HttpServletResponse resp)
        throws ServletException, IOException {
        String username = usernameFromPathInfo(req.getPathInfo());
        if (username == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (username.equals(CosmoSecurityManager.USER_ROOT)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        provisioningManager.removeUserByUsername(username);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /**
     * Delegated to by {@link #doGet} to handle account GET
     * requests, retrieving the account for the currently logged in
     * user, setting the response status and headers, and writing the
     * response content.
     */
    protected void processAccountGet(HttpServletRequest req,
                                     HttpServletResponse resp)
        throws ServletException, IOException {
        User user = getLoggedInUser();
        UserResource resource = new UserResource(user, getUrlBase(req));
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("ETag", resource.getEntityTag());
        sendXmlResponse(resp, resource);
    }

    /**
     * Delegated to by {@link #doGet} to handle users GET
     * requests, retrieving all user accounts, setting the response
     * status and headers, and writing the response content.
     */
    protected void processUsersGet(HttpServletRequest req,
                                   HttpServletResponse resp)
        throws ServletException, IOException {
        List users = provisioningManager.getUsers();
        resp.setStatus(HttpServletResponse.SC_OK);
        sendXmlResponse(resp, new UsersResource(users, getUrlBase(req)));
    }

    /**
     * Delegated to by {@link #doGet} to handle user GET
     * requests, retrieving the user account, setting the response
     * status and headers, and writing the response content.
     */
    protected void processUserGet(HttpServletRequest req,
                                  HttpServletResponse resp)
        throws ServletException, IOException {
        String username = usernameFromPathInfo(req.getPathInfo());
        if (username == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            User user = provisioningManager.getUserByUsername(username);
            UserResource resource = new UserResource(user, getUrlBase(req));
            resp.setHeader("ETag", resource.getEntityTag());
            sendXmlResponse(resp, resource);
        } catch (ObjectRetrievalFailureException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    /**
     * Delegated to by {@link #doPut} to handle signup
     * requests, creating the user account and setting the response
     * status and headers.
     */
    protected void processSignup(HttpServletRequest req,
                                 HttpServletResponse resp)
        throws ServletException, IOException {
        try {
            Document xmldoc = readXmlRequest(req);
            UserResource resource = new UserResource(getUrlBase(req), xmldoc);
            User user = resource.getUser();
            Role userRole = provisioningManager.
                getRoleByName(CosmoSecurityManager.ROLE_USER);
            user.addRole(userRole);
            provisioningManager.saveUser(user);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setHeader("Content-Location", resource.getHomedirUrl()); 
            resp.setHeader("ETag", resource.getEntityTag());
        } catch (JDOMException e) {
            log.warn("Error parsing request body: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (CosmoApiException e) {
            log.warn("bad request for signup: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           e.getMessage());
        } catch (ModelValidationException e) {
            handleModelValidationError(resp, e);
        }
   }

    /**
     * Delegated to by {@link #doPut} to handle account update
     * requests for the currently logged in user, saving the modified
     * account and setting the response status and headers.
     */
    protected void processAccountUpdate(HttpServletRequest req,
                                        HttpServletResponse resp)
        throws ServletException, IOException {
        try {
            Document xmldoc = readXmlRequest(req);
            String urlUsername = usernameFromPathInfo(req.getPathInfo());
            User user = getLoggedInUser();
            String oldUsername = user.getUsername();
            UserResource resource =
                new UserResource(user, getUrlBase(req), xmldoc);
            if (user.isUsernameChanged()) {
                // reset logged in user's username
                user.setUsername(oldUsername);
                log.warn("bad request for account update: " +
                         "Username may not be changed");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Username may not be changed");
                return;
            }
            provisioningManager.updateUser(user);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            resp.setHeader("ETag", resource.getEntityTag());
        } catch (JDOMException e) {
            log.warn("Error parsing request body: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (CosmoApiException e) {
            log.warn("bad request for account update: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           e.getMessage());
        } catch (ModelValidationException e) {
            handleModelValidationError(resp, e);
        }
    }

    /**
     * Delegated to by {@link #doPut} to handle user creation
     * requests, creating the user account and setting the response
     * status and headers.
     */
    protected void processUserCreate(HttpServletRequest req,
                                     HttpServletResponse resp)
        throws ServletException, IOException {
        try {
            Document xmldoc = readXmlRequest(req);
            String urlUsername = usernameFromPathInfo(req.getPathInfo());
            UserResource resource = new UserResource(getUrlBase(req), xmldoc);
            User user = resource.getUser();
            if (user.getUsername() != null &&
                ! user.getUsername().equals(urlUsername)) {
                log.warn("bad request for user update: " +
                         "Username does not match request URI");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Username does not match request URI");
                return;
            }
            Role userRole = provisioningManager.
                getRoleByName(CosmoSecurityManager.ROLE_USER);
            user.addRole(userRole);
            provisioningManager.saveUser(user);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setHeader("ETag", resource.getEntityTag());
        } catch (JDOMException e) {
            log.warn("Error parsing request body: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (CosmoApiException e) {
            log.warn("bad request for user create: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           e.getMessage());
        } catch (ModelValidationException e) {
            handleModelValidationError(resp, e);
        }
    }

    /**
     * Delegated to by {@link #doPut} to handle user update
     * requests, saving the modified account and setting the response
     * status and headers.
     */
    protected void processUserUpdate(HttpServletRequest req,
                                     HttpServletResponse resp,
                                     User user)
        throws ServletException, IOException {
        try {
            Document xmldoc = readXmlRequest(req);
            String urlUsername = usernameFromPathInfo(req.getPathInfo());
            UserResource resource =
                new UserResource(user, getUrlBase(req), xmldoc);
            provisioningManager.updateUser(user);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            resp.setHeader("ETag", resource.getEntityTag());
            if (! user.getUsername().equals(urlUsername)) {
                resp.setHeader("Content-Location", resource.getUserUrl());
            }
        } catch (JDOMException e) {
            log.warn("Error parsing request body: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (CosmoApiException e) {
            log.warn("bad request for user update: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           e.getMessage());
        } catch (ModelValidationException e) {
            handleModelValidationError(resp, e);
        }
    }

    /**
     */
    protected void handleModelValidationError(HttpServletResponse resp,
                                              ModelValidationException e)
        throws IOException {
        if (e instanceof DuplicateUsernameException) {
            sendApiError(resp, CosmoApiConstants.SC_USERNAME_IN_USE);
            return;
        }
        if (e instanceof DuplicateEmailException) {
            sendApiError(resp, CosmoApiConstants.SC_EMAIL_IN_USE);
            return;
        }
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                       e.getMessage());
    }

    /**
     */
    protected void sendApiError(HttpServletResponse resp,
                                int errorCode)
        throws IOException {
        resp.sendError(errorCode,
                       CosmoApiConstants.getReasonPhrase(errorCode));
    }

    /**
     * Looks up the bean with given name and class in the web
     * application context.
     *
     * @param name the bean's name
     * @param clazz the bean's class
     */
    protected Object getBean(String name, Class clazz)
        throws ServletException {
        try {
            return wac.getBean(name, clazz);
        } catch (BeansException e) {
            throw new ServletException("Error retrieving bean " + name +
                                       " of type " + clazz +
                                       " from web application context", e);
        }
    }

    /**
     */
    public WebApplicationContext getWebApplicationContext() {
        return wac;
    }

    /**
     */
    protected User getLoggedInUser() {
        return securityManager.getSecurityContext().getUser();
    }

    /**
     */
    protected String usernameFromPathInfo(String pathInfo) {
        if (pathInfo.startsWith("/user/")) {
            String username = pathInfo.substring(6);
            if (! (username.equals("") ||
                   username.indexOf("/") >= 0)) {
                return username;
            }
        }
        return null;
    }

    /**
     */
    protected Document readXmlRequest(HttpServletRequest req)
        throws JDOMException, IOException {
        InputStream in = req.getInputStream();
        if (in == null) {
            return null;
        }
        SAXBuilder builder = new SAXBuilder(false);
        return builder.build(in);
    }

    /**
     */
    protected void sendXmlResponse(HttpServletResponse resp,
                                   CosmoApiResource resource)
        throws IOException {
        // pretty format is easier for api scripters to read
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        // write xml into a byte array so we can calculate length
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        outputter.output(resource.toXml(), buf);
        byte[] bytes = buf.toByteArray();
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentLength(bytes.length);
        resp.getOutputStream().write(bytes);
    }

    /**
     */
    protected String getUrlBase(HttpServletRequest req) {
        // like response.encodeUrl() except does not include servlet
        // path or session id
        StringBuffer buf = new StringBuffer();
        buf.append(req.getScheme()).
            append("://").
            append(req.getServerName());
        if ((req.isSecure() && req.getServerPort() != 443) ||
            (req.getServerPort() != 80)) {
            buf.append(":").append(req.getServerPort());
        }
        if (! req.getContextPath().equals("/")) {
            buf.append(req.getContextPath());
        }
        return buf.toString();
    }
}
