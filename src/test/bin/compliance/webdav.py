__revision__  = "$Revision$"
__date__      = "$Date$"
__copyright__ = "Copyright (c) 2003-2004 Open Source Applications Foundation"
__license__   = "http://osafoundation.org/Chandler_0.1_license_terms.htm"

import unittest
from osaf.framework.twisted import testreactor
from twisted.internet import reactor, defer
from zanshin.webdav import ServerHandle, PermissionsError, PropertyId
from zanshin.http import Request

#---------------
import threading
# threading seems to install an atexit() handler that
# can wedge if you've run the reactor. So, we import it
# first....

import atexit

def runAndStop():
    if not reactor.running:
        reactor.callLater(0, reactor.stop)
        reactor.run()

# ... then our handler will be called before threading's.
atexit.register(runAndStop)
#---------------

VERBOSE = True
ACCOUNT_CREATED = True

class CosmoTest(testreactor.ReactorTestCase):
    localServer = True
    
    def setUp(self):

        super(CosmoTest, self).setUp()
        
        reactor.useRealTime()
        
        self.useSSL = False
        #self.useSSL = True
        self.username = "sprint"
        self.password = "sudoku"
        #self.host = "localhost"
        #self.username = "sprint2"
        #self.password = "sudoku"
        #self.username = "grant"
        #self.password = "demoed"
        self.host = "cosmo-demo.osafoundation.org"
        self.port = 8080
        #self.port = 443
        self.path = "/home/%s" % (self.username,)
        
        self.server = ServerHandle(self.host, self.port, self.username,
                                   self.password, self.useSSL)
                                   
        global ACCOUNT_CREATED
        if not ACCOUNT_CREATED:
                
            ACCOUNT_CREATED = True

            testHandle = ServerHandle(self.host, self.port, "root", "cosmo", False)
            xmlCreateAccount = """<?xml version="1.0" encoding="utf-8" ?> 
        <user xmlns="http://osafoundation.org/cosmo">
          <username>%s</username>
          <password>%s</password>
          <firstName>Tommy</firstName>
          <lastName>Tester</lastName>
          <email>%s@nojunkmailplease.com</email>
        </user>
        """ % (self.username, self.password, self.username)

            body = xmlCreateAccount.encode("utf-8")
            
            headers = { 'content-type': 'text/xml', 'connection': 'close' }
            
            print "Creating user account %s" % self.username

            req = Request('PUT', '/api/user/%s' % self.username, headers, body)
            req.timeout = 45.0
            response = reactor.waitUntil(testHandle.factory.addRequest(req), 45.0)

            if response.status == 201:
                print "Created user account %s" % self.username
            elif response.status == 204:
                print "WARNING: Account %s already exists." % self.username
            elif response.status == 401:
                print "ERROR: Authorization error. Check username and password."
            else:
                print "ERROR %s. Unknown error" % response.status
                                   
        #self.sendRequest('MKCOL', self.path, None, None)
                                   

    def tearDown(self):
        reactor.waitUntil(defer.maybeDeferred(self.server.factory.stopFactory), 10)
        
        super(CosmoTest, self).tearDown()
        
    def sendRequest(self, method, path, extraHeaders, body):
        req = Request(method, path, extraHeaders, body)
        
        if VERBOSE: print '... Sending %s %s' % (method, path)
        
        result = reactor.waitUntil(self.server.factory.addRequest(req), 60.0)
        
        if VERBOSE: print '... Received %s %s' % (result.status, result.message)
        
        return result



class MkcolTestCase(CosmoTest):
    def setUp(self):
        super(MkcolTestCase, self).setUp()
        
        #self.sendRequest('MKCOL', self.path, None, None)
        self.collectionPath = self.path + "/" + 'mkcoltest'
        self.sendRequest('DELETE', self.collectionPath, None, None)

    def testMkColSuccess(self):
        
        mkcolResponse = self.sendRequest('MKCOL', self.collectionPath, None, None)
        
        self.failUnlessEqual(mkcolResponse.status, 201, "Expected a 201 response")
        

    def testMkColOverwrite(self):
        self.sendRequest('MKCOL', self.collectionPath, None, None)
        response = self.sendRequest('MKCOL', self.collectionPath, None, None)
        
        self.failUnlessEqual(response.status, 405, "Expected MKCOL to fail on existing collection")
        
    def testMkColNoParent(self):
        response = self.sendRequest('MKCOL', self.collectionPath + "/skdjfksjfd/asdas", None, None)
        
        self.failUnlessEqual(response.status, 409)

    def testMkColWithBody(self):
        response = self.sendRequest('MKCOL', self.collectionPath, None, 'Hi, Mom!')
        
        self.failUnlessEqual(response.status, 415)

class AccountTestCase(CosmoTest):

    def FAILED_testDeleteHome(self):
        #self.sendRequest('MKCOL', self.path, None, None)
        response = self.sendRequest('DELETE', self.path, None, None)

    def FAILED_testNoNewAccount(self):
        #self.sendRequest('MKCOL', self.path, None, None)
        response = self.sendRequest('MKCOL', '/home/aksjdka-ajshdjashdj', None, None)
        
        self.failUnlessEqual(response.status, 403)


if __name__ == '__main__':
    unittest.main()

