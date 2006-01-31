#!/usr/bin/env python
#
#   Copyright 2006 Open Source Applications Foundation
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
#  
# silmut?
#   1) It's an anagram of litmus, which is a WebDAV test suite.
#   2) It's Finnish for buds.

import sys, getopt, httplib, base64

# Defaults
host = "localhost"
port = 8080

def caldav():
    '''
    TODO
    '''

def tickets():
    # Tests (from cosmo 0.2 spec):
    # -make sure MKTICKET request only shows tickets for current account
    # -make sure ticket timeouts followed
    # -make sure PROPFIND on ticketdiscovery property not supported
    # -make sure that visits elemnt always returns infinity
    # -make sure that http://www.xythos.com/namespaces/StorageServer is used for 
    #  the ticketdiscovery, ticketinfo, id and timeout elements
    # -valid values for timeout element are infinity and Seconds-xxxx
    # -if different ticket in headers and URL, URL is used
    # -DELTICKET on unowned ticket results in 403
    '''
    Initialization
        
    >>> global host, port
    >>> auth = 'Basic %s' % base64.encodestring('test1:test1').strip()
    >>> authHeaders = {'Authorization': auth}
    >>> hourTicket = """<?xml version="1.0" encoding="UTF-8"?>
    ... <X:ticketinfo xmlns:D="DAV:" 
    ...               xmlns:X="http://www.xythos.com/namespaces/StorageServer">
    ... <D:privilege><D:read/></D:privilege>
    ... <X:timeout>Second-3600</X:timeout>
    ... <D:visits>infinity</D:visits>
    ... </X:ticketinfo>"""
    >>> badNSTicket = """<?xml version="1.0" encoding="UTF-8"?>
    ... <D:ticketinfo xmlns:D="DAV:">
    ... <D:privilege><D:read/></D:privilege>
    ... <D:timeout>Second-3600</D:timeout>
    ... <D:visits>infinity</D:visits>
    ... </D:ticketinfo>"""
    
    MKTICKET
    
    Status codes

    OK
    
    >>> r = request('MKTICKET', '/home/test1', body=hourTicket,
    ...             headers=authHeaders)
    >>> print r.status # MKTICKET OK
    200

    Bad XML
    
    >>> r = request('MKTICKET', '/home/test1', body=badNSTicket,
    ...             headers=authHeaders)
    >>> print r.status # MKTICKET bad XML
    400

    No XML body
    
    >>> r = request('MKTICKET', '/home/test1', headers=authHeaders)
    >>> print r.status # MKTICKET no body
    400
    
    No access privileges
    
    >>> r = request('MKTICKET', '/home/test2', body=hourTicket,
    ...             headers=authHeaders)
    >>> print r.status # MKTICKET no access
    403

    No access privileges, no body
        
    >>> r = request('MKTICKET', '/home/test2', headers=authHeaders)
    >>> print r.status # MKTICKET no access, no body
    403

    No such resource
    
    >>> r = request('MKTICKET', '/home/test1/doesnotexist', headers=authHeaders)
    >>> print r.status # MKTICKET no such resource
    404

    No such resource, no body
    
    >>> r = request('MKTICKET', '/home/test1/doesnotexist', body=hourTicket,
    ...             headers=authHeaders)
    >>> print r.status # MKTICKET no such resource, no body
    404
    
    
    DELTICKET
    
    Status Codes
    
    Ticket does not exist
    
    >>> t = authHeaders.copy()
    >>> t['Ticket'] = 'nosuchticket5dfe45210787'
    >>> r = request('DELTICKET', '/home/test1?ticket=nosuchticket5dfe45210787',
    ...             headers=t)
    >>> print r.status # DELTICKET no such ticket
    412
    
    Ticket does not exist, body
    
    >>> t = authHeaders.copy()
    >>> t['Ticket'] = 'nosuchticket5dfe45210787'
    >>> r = request('DELTICKET', '/home/test1?ticket=nosuchticket5dfe45210787',
    ...             body=hourTicket, headers=t)
    >>> print r.status # DELTICKET no such ticket, body
    412
    
    Ticket does not exist, resource does not exist
    
    >>> t = authHeaders.copy()
    >>> t['Ticket'] = 'nosuchticket5dfe45210787'
    >>> r = request('DELTICKET', '/home/test1/doesnotexist?ticket=nosuchticket5dfe45210787',
    ...             headers=t)
    >>> print r.status # DELTICKET no such ticket or resource
    404
    
    Ticket does not exist, resource does not exist, body
    
    >>> t = authHeaders.copy()
    >>> t['Ticket'] = 'nosuchticket5dfe45210787'
    >>> r = request('DELTICKET', '/home/test1/doesnotexist?ticket=nosuchticket5dfe45210787',
    ...             body=hourTicket, headers=t)
    >>> print r.status # DELTICKET no such ticket or resource, body
    404    
    '''


def request(*args, **kw):
    """
    Helper function to make requests easier to make.
    """
    c = httplib.HTTPConnection(host, port)
    c.request(*args, **kw)
    return c.getresponse()


def usage():
    """
    Silmut is a CalDAV and TICKET compliance testsuite for Cosmo.

    Usage: python silmut.py [options]
    
    Options:
      -s      server (default is localhost)
      -p      port (default is 8080)
      -h      display this help text
    
    Assumes test1 and test2 are valid users on the server, and the passwords
    to be test1 and test2, respectively. Use createaccounts.py to set up 
    these accounts, for example.
    """
    print usage.__doc__


def main(argv):
    global host, port
    
    try:
        opts, args = getopt.getopt(argv, "s:p:h",)
    except getopt.GetoptError:
        usage()
        sys.exit(1)

    for (opt, arg) in opts:
        if   opt == "-s": host = arg
        elif opt == "-p": port = int(arg)
        elif opt == "-h":
            usage()
            sys.exit()

    import doctest
    doctest.testmod()
        

if __name__ == "__main__":
    main(sys.argv[1:])    
