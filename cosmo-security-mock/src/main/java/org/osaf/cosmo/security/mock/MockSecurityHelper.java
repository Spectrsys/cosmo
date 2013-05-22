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
package org.osaf.cosmo.security.mock;

import java.security.Principal;

import org.osaf.cosmo.model.User;
import org.osaf.cosmo.model.mock.TestHelper;

public class MockSecurityHelper {

    private TestHelper testHelper = new TestHelper();

    static int apseq = 0;

    /**
     */
    public Principal makeDummyUserPrincipal() {
        return new MockUserPrincipal(testHelper.makeDummyUser());
    }

    /**
     */
    public Principal makeDummyUserPrincipal(String name,
                                            String password) {
        return new MockUserPrincipal(testHelper.makeDummyUser(name, password));
    }
    
    /**
     */
    public Principal makeDummyUserPrincipal(User user) {
        return new MockUserPrincipal(user);
    }

    /**
     */
    public Principal makeDummyAnonymousPrincipal() {
        String serial = new Integer(++apseq).toString();
        return new MockAnonymousPrincipal("dummy" + serial);
    }

    /**
     */
    public Principal makeDummyRootPrincipal() {
        User user = testHelper.makeDummyUser();
        user.setAdmin(Boolean.TRUE);
        return new MockUserPrincipal(user);
    }
    
}
