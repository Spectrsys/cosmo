/*
 * Copyright 2007 Open Source Applications Foundation
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
package org.osaf.cosmo.atom.provider;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for for {@link StandardProvider} tests.
 */
public class BaseProviderTestCase extends TestCase {
    private static final Log log =
        LogFactory.getLog(BaseProviderTestCase.class);

    protected StandardProvider provider;
    protected ProviderHelper helper;

    protected void setUp() throws Exception {
        helper = new ProviderHelper();
        helper.setUp();

        provider = new StandardProvider();
        provider.setGeneratorFactory(helper.getGeneratorFactory());
        provider.setProcessorFactory(helper.getProcessorFactory());
        provider.setContentService(helper.getContentService());
        provider.setServiceLocatorFactory(helper.getServiceLocatorFactory());
        provider.init();

        helper.logIn();
    }

    protected void tearDown() throws Exception {
    }
}
