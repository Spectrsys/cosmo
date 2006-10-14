#   Copyright (c) 2006 Open Source Applications Foundation
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

from pyselenium import seleniumunittest
import time, re

class CosmoAboutBox(seleniumunittest.SeleniumTestCase):
    """Cosmo About Box Test Case"""
    def test_cosmo(self):
        """Main test method for cosmo about box testing"""
        sel = self.selenium
        sel.click("link=About Cosmo")
        time.sleep(2)

    