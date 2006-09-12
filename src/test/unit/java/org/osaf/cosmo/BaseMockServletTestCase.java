/*
 * Copyright 2005-2006 Open Source Applications Foundation
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
package org.osaf.cosmo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import org.osaf.cosmo.model.User;
import org.osaf.cosmo.security.mock.MockSecurityManager;
import org.osaf.cosmo.security.mock.MockUserPrincipal;

import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.w3c.dom.Document;

/**
 * Base class for executing servlet tests in a mock servlet container.
 */
public abstract class BaseMockServletTestCase extends TestCase {
    private static final Log log =
        LogFactory.getLog(BaseMockServletTestCase.class);
    protected static final DocumentBuilderFactory BUILDER_FACTORY =
        DocumentBuilderFactory.newInstance();

    private MockSecurityManager securityManager;
    private MockServletContext servletContext;
    private MockServletConfig servletConfig;

    /**
     */
    protected void setUp() throws Exception {
        securityManager = new MockSecurityManager();
        servletContext = new MockServletContext();
        servletConfig = new MockServletConfig(servletContext);
    }

    /**
     */
    protected MockHttpServletRequest createMockRequest(String method,
                                                       String cmpPath) {
        MockHttpServletRequest request =
            new MockHttpServletRequest(servletContext, method,
                                       getServletPath() + cmpPath);
        request.setServletPath(getServletPath());
        request.setPathInfo(cmpPath);
        return request;
    }

    /**
     */
    protected void logInUser(User user) {
        securityManager.setUpMockSecurityContext(new MockUserPrincipal(user));
    }

    /**
     */
    protected void sendXmlRequest(MockHttpServletRequest request,
                                  XmlSerializable thing)
        throws Exception {
        Document doc = BUILDER_FACTORY.newDocumentBuilder().newDocument();
        doc.appendChild(thing.toXml(doc));
        sendXmlRequest(request, doc);
    }

    /**
     */
    protected void sendXmlRequest(MockHttpServletRequest request,
                                  Document doc)
        throws Exception {
        OutputFormat format = new OutputFormat("xml", "UTF-8", true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.setNamespaces(true);
        serializer.asDOMSerializer().serialize(doc);
        request.setContentType("text/xml");
        request.setCharacterEncoding("UTF-8");
        request.setContent(out.toByteArray());;
    }

    /**
     */
    protected Document readXmlResponse(MockHttpServletResponse response)
        throws Exception {
        ByteArrayInputStream in =
            new ByteArrayInputStream(response.getContentAsByteArray());
        BUILDER_FACTORY.setNamespaceAware(true);
        return BUILDER_FACTORY.newDocumentBuilder().parse(in);
    }

    /**
     */
    public abstract String getServletPath();

    /**
     */
    public MockSecurityManager getSecurityManager() {
        return securityManager;
    }

    /**
     */
    public MockServletContext getServletContext() {
        return servletContext;
    }

    /**
     */
    public MockServletConfig getServletConfig() {
        return servletConfig;
    }
}
