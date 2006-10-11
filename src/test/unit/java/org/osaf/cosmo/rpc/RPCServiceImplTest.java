/*
 * Copyright 2006 Open Source Applications Foundation
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
package org.osaf.cosmo.rpc;

import junit.framework.TestCase;

import org.apache.commons.id.random.SessionIdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.TestHelper;
import org.osaf.cosmo.dao.mock.MockCalendarDao;
import org.osaf.cosmo.dao.mock.MockContentDao;
import org.osaf.cosmo.dao.mock.MockDaoStorage;
import org.osaf.cosmo.dao.mock.MockUserDao;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.rpc.model.Calendar;
import org.osaf.cosmo.rpc.model.CosmoDate;
import org.osaf.cosmo.rpc.model.Event;
import org.osaf.cosmo.security.mock.MockSecurityManager;
import org.osaf.cosmo.security.mock.MockUserPrincipal;
import org.osaf.cosmo.service.impl.StandardContentService;
import org.osaf.cosmo.service.impl.StandardUserService;

public class RPCServiceImplTest extends TestCase {
    private static final String TEST_CALENDAR_NAME = "RemoteCosmoService";
    private static final String TEST_CALENDAR_PATH = "RemoteCosmoServiceTest";
    private static final Log log = LogFactory.getLog(RPCServiceImplTest.class);

    private TestHelper testHelper;

    private User user;
    
    private MockDaoStorage storage;
    
    private MockContentDao contentDao;
    private MockCalendarDao calendarDao;
    private MockUserDao userDao;
    private MockSecurityManager securityManager;
    
    private StandardContentService contentService;
    private StandardUserService userService;
    
    private RPCServiceImpl rpcService;
    
    private org.osaf.cosmo.rpc.model.Calendar[] calendars;

    public void setUp(){
        testHelper = new TestHelper();
        
        user = testHelper.makeDummyUser();
        
        storage = new MockDaoStorage();
        
        contentDao = new MockContentDao(storage);
        calendarDao = new MockCalendarDao(storage);
        userDao = new MockUserDao();

        securityManager = new MockSecurityManager();
        securityManager.setUpMockSecurityContext(new MockUserPrincipal(user));

        contentService = new StandardContentService();
        contentService.setContentDao(contentDao);
        contentService.setCalendarDao(calendarDao);
        userService = new StandardUserService();
        userService.setContentDao(contentDao);
        userService.setUserDao(userDao);
        userService.setPasswordGenerator(new SessionIdGenerator());
        userService.init();
        userService.createUser(user);

        rpcService = new RPCServiceImpl();
        rpcService.setContentService(contentService);
        rpcService.setCosmoSecurityManager(securityManager);
        rpcService.setUserService(userService);
        
        try {
            rpcService.createCalendar(TEST_CALENDAR_NAME, TEST_CALENDAR_PATH );
            calendars = rpcService.getCalendars();
        } catch (RPCException e) {
            log.info(e);
        }
    }
    protected Event createOneHourEvent(String title, String description, int year, int month, int date, int hour) {
        Event evt = new Event();
        evt.setTitle(title);
        
        CosmoDate start = new CosmoDate();
        start.setYear(year);
        start.setMonth(month);
        start.setDate(date);
        start.setHours(hour);
        start.setMinutes(0);
        start.setSeconds(0);
        
        evt.setStart(start);
        
        CosmoDate end = new CosmoDate(); 
        end.setYear(year);
        end.setMonth(month);
        end.setDate(date);
        end.setHours(hour+1);
        end.setMinutes(0);
        end.setSeconds(0);
        
        evt.setEnd(end);
        evt.setDescription(description);

        return evt;
    }
    
    protected Event createTestEvent() {
        return createOneHourEvent("Test Event", "A sample event", 2006, CosmoDate.MONTH_JANUARY, 2, 10);
    }
    
    public void testGetEvents() throws Exception {
        Event evt0 = createOneHourEvent("Test Event 1", "event before range", 2006, CosmoDate.MONTH_JANUARY,2, 10);
        Event evt1 = createOneHourEvent("Test Event 2", "event2 in range", 2006, CosmoDate.MONTH_MARCH, 4, 11);
        Event evt2 = createOneHourEvent("Test Event 3", "event3 in range", 2006, CosmoDate.MONTH_APRIL, 5, 18);
        Event evt3 = createOneHourEvent("Test Event 4", "event4 after range", 2006, CosmoDate.MONTH_JUNE, 4, 9);
        
        rpcService.saveEvent(TEST_CALENDAR_PATH, evt0);
        rpcService.saveEvent(TEST_CALENDAR_PATH, evt1);
        rpcService.saveEvent(TEST_CALENDAR_PATH, evt2);
        rpcService.saveEvent(TEST_CALENDAR_PATH, evt3);
        long UTC_MAR_ONE = 1141200000000L;    // 3/1/2006 - 1141200000000
        long UTC_MAY_ONE = 1146466800000L;    // 5/1/2006 - 1146466800000
        Event events[] = rpcService.getEvents(TEST_CALENDAR_PATH, UTC_MAR_ONE, UTC_MAY_ONE);
        assertEquals(events.length, 2);
    }

    public void testSaveEvent() throws Exception {
        Event evt = createTestEvent();              
        String id = rpcService.saveEvent(TEST_CALENDAR_PATH, evt);
        assertNotNull(id);
    }

    public void testRemoveEvent() throws Exception {
        Event evt = createTestEvent();              
        String id = rpcService.saveEvent(TEST_CALENDAR_PATH, evt);
        Event evt1 = rpcService.getEvent(TEST_CALENDAR_PATH, id);
        assertNotNull(evt1);
        rpcService.removeEvent(TEST_CALENDAR_PATH, id);
        Event evt2 = rpcService.getEvent(TEST_CALENDAR_PATH, id);
        assertNull(evt2);
    }

    public void testGetEvent() throws Exception {
        Event evt = createTestEvent();              
        String id = rpcService.saveEvent(TEST_CALENDAR_PATH, evt);
        
        Event evt1 = rpcService.getEvent(TEST_CALENDAR_PATH, id);
        evt.setId(evt1.getId()); // to pass equality test
        assertEquals(evt, evt1);
    }

    public void testMoveEvent() throws Exception {
        log.info("testMoveEvent not implemented because MoveEvent is not implemented");
        assertTrue(true);
    }

    public void testGetCalendars() throws Exception {
        rpcService.createCalendar(TEST_CALENDAR_NAME+"1", TEST_CALENDAR_PATH+"1" );
        calendars = rpcService.getCalendars();
        assertEquals(calendars.length, 2);
        rpcService.removeCalendar(TEST_CALENDAR_PATH+"1");
        calendars = rpcService.getCalendars();
        assertEquals(calendars.length, 1);
    }

    public void testCreateCalendar() throws Exception {
        boolean found = false;
        for (Calendar c : calendars) {
            log.info(c.getPath());
            if (c.getPath().equals("RemoteCosmoServiceTest")) {
                found = true;
            }
        }
        if (found) {
            assertTrue(true);
        } else {
            fail("Calendar not created");
        }
    }
    
    public void testRemoveCalendar() throws Exception {
        int initialNumberOfCalendars = calendars.length;
        rpcService.removeCalendar(TEST_CALENDAR_PATH);
        calendars = rpcService.getCalendars();
        assertEquals(calendars.length, initialNumberOfCalendars - 1);
    }

    public void testGetPreference() throws Exception {
        rpcService.removePreference("testPreference");
        String result = rpcService.getPreference("testPreference");
        assertNull(result);
        rpcService.setPreference("testPreference", "value");
        result = rpcService.getPreference("testPreference");
        assertEquals(result,"value");
    }

    public void testSetPreference() throws Exception {
        rpcService.setPreference("testPreference", "value");
        String result = rpcService.getPreference("testPreference");
        assertEquals(result,"value");
    }
    
    public void testRemovePreference() throws Exception {
        rpcService.removePreference("testPreference");
    }
}
