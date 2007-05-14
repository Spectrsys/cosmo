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
package org.osaf.cosmo.dao.hibernate;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.hibernate.validator.InvalidStateException;
import org.osaf.cosmo.dao.UserDao;
import org.osaf.cosmo.model.Attribute;
import org.osaf.cosmo.model.AttributeTombstone;
import org.osaf.cosmo.model.CalendarAttribute;
import org.osaf.cosmo.model.CollectionItem;
import org.osaf.cosmo.model.ContentItem;
import org.osaf.cosmo.model.DecimalAttribute;
import org.osaf.cosmo.model.DuplicateItemNameException;
import org.osaf.cosmo.model.HomeCollectionItem;
import org.osaf.cosmo.model.Item;
import org.osaf.cosmo.model.ItemNotFoundException;
import org.osaf.cosmo.model.ItemTombstone;
import org.osaf.cosmo.model.ModelValidationException;
import org.osaf.cosmo.model.QName;
import org.osaf.cosmo.model.StringAttribute;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.TimestampAttribute;
import org.osaf.cosmo.model.Tombstone;
import org.osaf.cosmo.model.TriageStatus;
import org.osaf.cosmo.model.UidInUseException;
import org.osaf.cosmo.model.User;
import org.springframework.dao.DataIntegrityViolationException;

public class HibernateContentDaoTest extends AbstractHibernateDaoTestCase {

    protected UserDaoImpl userDao = null;

    protected ContentDaoImpl contentDao = null;

    public HibernateContentDaoTest() {
        super();
    }

    public void testContentDaoCreateContent() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(root, item);

        Assert.assertTrue(newItem.getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());

        helper.verifyItem(newItem, queryItem);
    }
    
    public void testContentDaoLoadChildren() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(root, item);

        Assert.assertTrue(newItem.getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        Set<ContentItem> children = contentDao.loadChildren(root, null);
        Assert.assertEquals(1, children.size());
        
        children = contentDao.loadChildren(root, newItem.getModifiedDate());
        Assert.assertEquals(0, children.size());
        
        children = contentDao.loadChildren(root, new Date(newItem.getModifiedDate().getTime() -1));
        Assert.assertEquals(1, children.size());
    }
    
    public void testContentDaoCreateContentDuplicateUid() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item1 = generateTestContent();
        item1.setName("test");
        item1.setUid("uid");

        contentDao.createContent(root, item1);
        
        ContentItem item2 = generateTestContent();
        item2.setName("test2");
        item2.setUid("uid");

        try {
            contentDao.createContent(root, item2);
            clearSession();
            Assert.fail("able to create duplicate uid");
        } catch (UidInUseException e) {
        }
    }

   /* public void testContentDaoInvalidContentNullLength() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = new ContentItem();
        item.setName("test");
        item.setOwner(user);
        item.setContent(helper.getBytes(baseDir + "/testdata1.txt"));
        item.setContentLanguage("en");
        item.setContentEncoding("UTF8");
        item.setContentType("text/text");
        item.setContentLength(null);

        try {
            contentDao.createContent(root, item);
            clearSession();
            Assert.fail("able to create invalid content.");
        } catch (InvalidStateException e) {
        }
    }*/

   /* public void testContentDaoInvalidContentNegativeLength() throws Exception {
        
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = new ContentItem();
        item.setName("test");
        item.setOwner(user);
        item.setContent(helper.getBytes(baseDir + "/testdata1.txt"));
        item.setContentLanguage("en");
        item.setContentEncoding("UTF8");
        item.setContentType("text/text");
        item.setContentLength(new Long(-1));
        
        try {
            contentDao.createContent(root, item);
            clearSession();
            Assert.fail("able to create invalid content.");
        } catch (InvalidStateException e) {
        }
    }*/
    
   /* public void testContentDaoInvalidContentMismatchLength() throws Exception {
        
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = new ContentItem();
        item.setName("test");
        item.setOwner(user);
        item.setContent(helper.getBytes(baseDir + "/testdata1.txt"));
        item.setContentLanguage("en");
        item.setContentEncoding("UTF8");
        item.setContentType("text/text");
        item.setContentLength(new Long(1));

        try {
            contentDao.createContent(root, item);
            Assert.fail("able to create invalid content.");
        } catch (ModelValidationException e) {
        }

    }*/
  
    public void testContentDaoInvalidContentNullName() throws Exception {
      
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        item.setName(null);

        try {
            contentDao.createContent(root, item);
            Assert.fail("able to create invalid content.");
        } catch (DataIntegrityViolationException e) {
        }
    }

    public void testContentDaoInvalidContentEmptyName() throws Exception {
        
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);
        ContentItem item = generateTestContent();
        item.setName("");

        try {
            contentDao.createContent(root, item);
            Assert.fail("able to create invalid content.");
        } catch (InvalidStateException e) {
            Assert.assertEquals("name", e.getInvalidValues()[0].getPropertyName());
        }
    }

    public void testContentAttributes() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        item.addIntegerAttribute("intattribute", new Long(22));
        item.addBooleanAttribute("booleanattribute", Boolean.TRUE);
        
        DecimalAttribute decAttr = 
            new DecimalAttribute(new QName("decimalattribute"),new BigDecimal("1.234567"));
        item.addAttribute(decAttr);
        
        // TODO: figure out db date type is handled because i'm seeing
        // issues with accuracy
        // item.addAttribute(new DateAttribute("dateattribute", new Date()));

        HashSet<String> values = new HashSet<String>();
        values.add("value1");
        values.add("value2");
        item.addMultiValueStringAttribute("multistringattribute", values);

        HashMap<String, String> dictionary = new HashMap<String, String>();
        dictionary.put("key1", "value1");
        dictionary.put("key2", "value2");
        item.addDictionaryAttribute("dictionaryattribute", dictionary);

        ContentItem newItem = contentDao.createContent(root, item);

        Assert.assertTrue(newItem.getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());

        Attribute attr = queryItem.getAttribute(new QName("decimalattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof DecimalAttribute);
        Assert.assertEquals(attr.getValue().toString(),"1.234567");
        
        Set<String> querySet = (Set<String>) queryItem
                .getAttributeValue("multistringattribute");
        Assert.assertTrue(querySet.contains("value1"));
        Assert.assertTrue(querySet.contains("value2"));

        Map<String, String> queryDictionary = (Map<String, String>) queryItem
                .getAttributeValue("dictionaryattribute");
        Assert.assertEquals("value1", queryDictionary.get("key1"));
        Assert.assertEquals("value2", queryDictionary.get("key2"));

        Attribute custom = queryItem.getAttribute("customattribute");
        Assert.assertEquals("customattributevalue", custom.getValue());

        helper.verifyItem(newItem, queryItem);

        // set attribute value to null
        custom.setValue(null);

        querySet.add("value3");
        queryDictionary.put("key3", "value3");

        queryItem.removeAttribute("intattribute");

        contentDao.updateContent(queryItem);

        clearSession();

        queryItem = contentDao.findContentByUid(newItem.getUid());
        querySet = (Set) queryItem.getAttributeValue("multistringattribute");
        queryDictionary = (Map) queryItem
                .getAttributeValue("dictionaryattribute");
        Attribute queryAttribute = queryItem.getAttribute("customattribute");
       
        Assert.assertTrue(querySet.contains("value3"));
        Assert.assertEquals("value3", queryDictionary.get("key3"));
        Assert.assertNotNull(queryAttribute);
        Assert.assertNull(queryAttribute.getValue());
        Assert.assertNull(queryItem.getAttribute("intattribute"));
    }
    
    public void testCalendarAttribute() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        
        CalendarAttribute calAttr = 
            new CalendarAttribute(new QName("calendarattribute"), "2002-10-10T00:00:00+05:00"); 
        item.addAttribute(calAttr);
        
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());

        Attribute attr = queryItem.getAttribute(new QName("calendarattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof CalendarAttribute);
        
        Calendar cal = (Calendar) attr.getValue();
        Assert.assertEquals(cal.getTimeZone().getID(), "GMT+05:00");
        Assert.assertTrue(cal.equals(calAttr.getValue()));
        
        attr.setValue("2003-10-10T00:00:00+02:00");

        contentDao.updateContent(queryItem);

        clearSession();

        queryItem = contentDao.findContentByUid(newItem.getUid());
        Attribute queryAttr = queryItem.getAttribute(new QName("calendarattribute"));
        Assert.assertNotNull(queryAttr);
        Assert.assertTrue(queryAttr instanceof CalendarAttribute);
        
        cal = (Calendar) queryAttr.getValue();
        Assert.assertEquals(cal.getTimeZone().getID(), "GMT+02:00");
        Assert.assertTrue(cal.equals(attr.getValue()));
    }
    
    public void testTimestampAttribute() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        Date dateVal = new Date();
        TimestampAttribute tsAttr = 
            new TimestampAttribute(new QName("timestampattribute"), dateVal); 
        item.addAttribute(tsAttr);
        
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());

        Attribute attr = queryItem.getAttribute(new QName("timestampattribute"));
        Assert.assertNotNull(attr);
        Assert.assertTrue(attr instanceof TimestampAttribute);
        
        Date val = (Date) attr.getValue();
        Assert.assertTrue(dateVal.equals(val));
        
        dateVal.setTime(dateVal.getTime() + 101);
        attr.setValue(dateVal);

        contentDao.updateContent(queryItem);

        clearSession();

        queryItem = contentDao.findContentByUid(newItem.getUid());
        Attribute queryAttr = queryItem.getAttribute(new QName("timestampattribute"));
        Assert.assertNotNull(queryAttr);
        Assert.assertTrue(queryAttr instanceof TimestampAttribute);
        
        val = (Date) queryAttr.getValue();
        Assert.assertTrue(dateVal.equals(val));
    }

    public void testCreateDuplicateRootItem() throws Exception {
        User testuser = getUser(userDao, "testuser");
        try {
            contentDao.createRootItem(testuser);
            Assert.fail("able to create duplicate root item");
        } catch (RuntimeException re) {
        }
    }

    public void testFindItem() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");

        CollectionItem root = (CollectionItem) contentDao
                .getRootItem(testuser2);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(getUser(userDao, "testuser2"));

        a = contentDao.createCollection(root, a);

        clearSession();

        Item queryItem = contentDao.findItemByUid(a.getUid());
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof CollectionItem);

        queryItem = contentDao.findItemByPath("/testuser2/a");
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof CollectionItem);

        ContentItem item = generateTestContent();
        
        a = contentDao.findCollectionByUid(a.getUid());
        item = contentDao.createContent(a, item);

        clearSession();

        queryItem = contentDao.findItemByPath("/testuser2/a/test");
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof ContentItem);

        clearSession();

        queryItem = contentDao.findItemParentByPath("/testuser2/a/test");
        Assert.assertNotNull(queryItem);
        Assert.assertEquals(a.getUid(), queryItem.getUid());
    }

    public void testContentDaoUpdateContent() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);
        Date newItemModifyDate = newItem.getModifiedDate();
        
        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());

        helper.verifyItem(newItem, queryItem);
        Assert.assertEquals(0, queryItem.getVersion().intValue());

        queryItem.setName("test2");
        queryItem.setDisplayName("this is a test item2");
        queryItem.getAttributes().remove("customattribute");
        queryItem.setContentLanguage("es");
        queryItem.setContent(helper.getBytes(baseDir + "/testdata2.txt"));

        // Make sure modified date changes
        Thread.sleep(1000);
        queryItem = contentDao.updateContent(queryItem);

        clearSession();

        ContentItem queryItem2 = contentDao.findContentByUid(newItem.getUid());
        Assert.assertTrue(queryItem2.getVersion().intValue() > 0);
        helper.verifyItem(queryItem, queryItem2);

        Assert.assertTrue(newItemModifyDate.before(
                queryItem2.getModifiedDate()));
    }

    public void testContentDaoUpdateError() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item1 = generateTestContent("test", "testuser");
        ContentItem item2 = generateTestContent("test2", "testuser");

        ContentItem newItem1 = contentDao.createContent(root, item1);
        ContentItem newItem2 = contentDao.createContent(root, item2);

        clearSession();

        ContentItem queryItem2 = contentDao.findContentByUid(newItem2.getUid());

        queryItem2.setName("test");
        try {
            contentDao.updateContent(queryItem2);
            Assert.fail("able to update item with duplicate name");
        } catch (DuplicateItemNameException dine) {
        }

    }

    public void testContentDaoDeleteContent() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());
        helper.verifyItem(newItem, queryItem);

        contentDao.removeContent(queryItem);

        clearSession();

        queryItem = contentDao.findContentByUid(queryItem.getUid());
        Assert.assertNull(queryItem);
        
        clearSession();
        
        root = (CollectionItem) contentDao.getRootItem(user);
        Assert.assertTrue(root.getChildren().size()==0);
        
    }

    public void testDeleteContentByPath() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());
        helper.verifyItem(newItem, queryItem);

        contentDao.removeItemByPath("/testuser/test");

        clearSession();

        queryItem = contentDao.findContentByUid(queryItem.getUid());
        Assert.assertNull(queryItem);
    }

    public void testDeleteContentByUid() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());
        helper.verifyItem(newItem, queryItem);

        contentDao.removeItemByUid(queryItem.getUid());

        clearSession();

        queryItem = contentDao.findContentByUid(queryItem.getUid());
        Assert.assertNull(queryItem);
    }
    
    public void testTombstoneDeleteContent() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());
        helper.verifyItem(newItem, queryItem);
        
        Assert.assertTrue(queryItem.getVersion().equals(0));

        contentDao.removeContent(queryItem);

        clearSession();

        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertNull(queryItem);
        
        root = (CollectionItem) contentDao.getRootItem(user);
        Assert.assertEquals(root.getTombstones().size(), 1);
        
        Tombstone ts = root.getTombstones().iterator().next();
        
        Assert.assertTrue(ts instanceof ItemTombstone);
        Assert.assertEquals(((ItemTombstone) ts).getItemUid(), newItem.getUid());
        
        item = generateTestContent();
        item.setUid(newItem.getUid());
        
        contentDao.createContent(root, item);

        clearSession();
        
        queryItem = contentDao.findContentByUid(newItem.getUid());
        
        Assert.assertNotNull(queryItem);
        
        root = (CollectionItem) contentDao.getRootItem(user);
        Assert.assertEquals(root.getTombstones().size(), 0);
    }

    public void testContentDaoCreateCollection() throws Exception {
        User user = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);

        Assert.assertTrue(a.getId() > -1);
        Assert.assertNotNull(a.getUid());

        clearSession();

        CollectionItem queryItem = contentDao.findCollectionByUid(a.getUid());
        helper.verifyItem(a, queryItem);
    }

    public void testContentDaoUpdateCollection() throws Exception {
        User user = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);

        clearSession();

        Assert.assertTrue(a.getId() > -1);
        Assert.assertNotNull(a.getUid());

        CollectionItem queryItem = contentDao.findCollectionByUid(a.getUid());
        helper.verifyItem(a, queryItem);

        queryItem.setName("b");
        contentDao.updateCollection(queryItem);

        clearSession();

        queryItem = contentDao.findCollectionByUid(a.getUid());
        Assert.assertEquals("b", queryItem.getName());
    }
    
    public void testContentDaoUpdateCollectionTimestamp() throws Exception {
        User user = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);
        Integer ver = a.getVersion();
        Date timestamp = a.getModifiedDate();
        
        clearSession();
        Thread.sleep(1);
        
        a = contentDao.updateCollectionTimestamp(a);
        Assert.assertTrue(a.getVersion()==ver + 1);
        Assert.assertTrue(timestamp.before(a.getModifiedDate()));
    }

    public void testContentDaoDeleteCollection() throws Exception {
        User user = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);

        clearSession();

        CollectionItem queryItem = contentDao.findCollectionByUid(a.getUid());
        Assert.assertNotNull(queryItem);

        contentDao.removeCollection(queryItem);

        clearSession();

        queryItem = contentDao.findCollectionByUid(a.getUid());
        Assert.assertNull(queryItem);
    }

    public void testContentDaoAdvanced() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao
                .getRootItem(testuser2);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(getUser(userDao, "testuser2"));

        a = contentDao.createCollection(root, a);

        CollectionItem b = new CollectionItem();
        b.setName("b");
        b.setOwner(getUser(userDao, "testuser2"));

        b = contentDao.createCollection(a, b);

        ContentItem c = generateTestContent("c", "testuser2");

        c = contentDao.createContent(b, c);

        ContentItem d = generateTestContent("d", "testuser2");

        d = contentDao.createContent(a, d);

        clearSession();

        // test duplicates
        CollectionItem a2 = new CollectionItem();
        a2.setName("a");
        a2.setOwner(getUser(userDao, "testuser2"));

        ContentItem d2 = generateTestContent("d", "testuser2");

        try {
            contentDao.createCollection(root, a2);
            Assert.fail("Should not be able to create duplicate collection");
        } catch (DuplicateItemNameException dine) {
        }

        try {
            contentDao.createContent(a, d2);
            Assert.fail("Should not be able to create duplicate content");
        } catch (DuplicateItemNameException dine) {
        }

        a = contentDao.findCollectionByUid(a.getUid());
        b = contentDao.findCollectionByUid(b.getUid());
        c = contentDao.findContentByUid(c.getUid());
        d = contentDao.findContentByUid(d.getUid());
        root = contentDao.getRootItem(testuser2);

        Assert.assertNotNull(a);
        Assert.assertNotNull(b);
        Assert.assertNotNull(d);
        Assert.assertNotNull(root);

        // test children
        Collection children = a.getChildren();
        Assert.assertEquals(2, children.size());
        verifyContains(children, b);
        verifyContains(children, d);

        children = root.getChildren();
        Assert.assertEquals(1, children.size());
        verifyContains(children, a);

        // test get by path
        ContentItem queryC = contentDao.findContentByPath("/testuser2/a/b/c");
        Assert.assertNotNull(queryC);
        helper.verifyInputStream(
                new FileInputStream(baseDir + "/testdata1.txt"), queryC
                        .getContent());
        Assert.assertEquals("c", queryC.getName());

        // test get path/uid abstract
        Item queryItem = contentDao.findItemByPath("/testuser2/a/b/c");
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof ContentItem);

        queryItem = contentDao.findItemByUid(a.getUid());
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof CollectionItem);

        // test delete
        contentDao.removeContent(c);
        queryC = contentDao.findContentByUid(c.getUid());
        Assert.assertNull(queryC);

        contentDao.removeCollection(a);

        CollectionItem queryA = contentDao.findCollectionByUid(a.getUid());
        Assert.assertNull(queryA);

        ContentItem queryD = contentDao.findContentByUid(d.getUid());
        Assert.assertNull(queryD);
    }

  
    public void testHomeCollection() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");
        HomeCollectionItem root = contentDao.getRootItem(testuser2);

        Assert.assertNotNull(root);
        root.setName("alsfjal;skfjasd");
        Assert.assertEquals(root.getName(), "testuser2");

    }

    public void testItemDaoMove() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao
                .getRootItem(testuser2);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(getUser(userDao, "testuser2"));

        a = contentDao.createCollection(root, a);

        CollectionItem b = new CollectionItem();
        b.setName("b");
        b.setOwner(getUser(userDao, "testuser2"));

        b = contentDao.createCollection(a, b);

        CollectionItem c = new CollectionItem();
        c.setName("c");
        c.setOwner(getUser(userDao, "testuser2"));

        c = contentDao.createCollection(b, c);

        ContentItem d = generateTestContent("d", "testuser2");

        d = contentDao.createContent(c, d);

        CollectionItem e = new CollectionItem();
        e.setName("e");
        e.setOwner(getUser(userDao, "testuser2"));

        e = contentDao.createCollection(a, e);

        clearSession();

        root = (CollectionItem) contentDao.getRootItem(testuser2);
        e = contentDao.findCollectionByUid(e.getUid());
        b = contentDao.findCollectionByUid(b.getUid());

        // verify can't move root collection
        try {
            contentDao.moveItem("/testuser2", "/testuser2/a/blah");
            Assert.fail("able to move root collection");
        } catch (IllegalArgumentException iae) {
        }

        // verify can't move to root collection
        try {
            contentDao.moveItem("/testuser2/a/e", "/testuser2");
            Assert.fail("able to move to root collection");
        } catch (ItemNotFoundException infe) {
        }

        // verify can't create loop
        try {
            contentDao.moveItem("/testuser2/a/b", "/testuser2/a/b/c/new");
            Assert.fail("able to create loop");
        } catch (ModelValidationException iae) {
        }

        clearSession();

        // verify that move works
        b = contentDao.findCollectionByPath("/testuser2/a/b");

        contentDao.moveItem("/testuser2/a/b", "/testuser2/a/e/b");

        clearSession();

        CollectionItem queryCollection = contentDao
                .findCollectionByPath("/testuser2/a/e/b");
        Assert.assertNotNull(queryCollection);

        contentDao.moveItem("/testuser2/a/e/b", "/testuser2/a/e/bnew");

        clearSession();
        queryCollection = contentDao
                .findCollectionByPath("/testuser2/a/e/bnew");
        Assert.assertNotNull(queryCollection);

        Item queryItem = contentDao.findItemByPath("/testuser2/a/e/bnew/c/d");
        Assert.assertNotNull(queryItem);
        Assert.assertTrue(queryItem instanceof ContentItem);
    }

    public void testItemDaoCopy() throws Exception {
        User testuser2 = getUser(userDao, "testuser2");
        CollectionItem root = (CollectionItem) contentDao
                .getRootItem(testuser2);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(getUser(userDao, "testuser2"));

        a = contentDao.createCollection(root, a);

        CollectionItem b = new CollectionItem();
        b.setName("b");
        b.setOwner(getUser(userDao, "testuser2"));

        b = contentDao.createCollection(a, b);

        CollectionItem c = new CollectionItem();
        c.setName("c");
        c.setOwner(getUser(userDao, "testuser2"));

        c = contentDao.createCollection(b, c);

        ContentItem d = generateTestContent("d", "testuser2");

        d = contentDao.createContent(c, d);

        CollectionItem e = new CollectionItem();
        e.setName("e");
        e.setOwner(getUser(userDao, "testuser2"));

        e = contentDao.createCollection(a, e);

        clearSession();

        root = (CollectionItem) contentDao.getRootItem(testuser2);
        e = contentDao.findCollectionByUid(e.getUid());
        b = contentDao.findCollectionByUid(b.getUid());

        // verify can't copy root collection
        try {
            contentDao.copyItem(root, "/testuser2/a/blah", true);
            Assert.fail("able to copy root collection");
        } catch (IllegalArgumentException iae) {
        }

        // verify can't move to root collection
        try {
            contentDao.copyItem(e, "/testuser2", true);
            Assert.fail("able to move to root collection");
        } catch (ItemNotFoundException infe) {
        }

        // verify can't create loop
        try {
            contentDao.copyItem(b, "/testuser2/a/b/c/new", true);
            Assert.fail("able to create loop");
        } catch (ModelValidationException iae) {
        }

        clearSession();

        // verify that copy works
        b = contentDao.findCollectionByPath("/testuser2/a/b");

        contentDao.copyItem(b, "/testuser2/a/e/bcopy", true);

        clearSession();

        CollectionItem queryCollection = contentDao
                .findCollectionByPath("/testuser2/a/e/bcopy");
        Assert.assertNotNull(queryCollection);

        queryCollection = contentDao
                .findCollectionByPath("/testuser2/a/e/bcopy/c");
        Assert.assertNotNull(queryCollection);

        d = contentDao.findContentByUid(d.getUid());
        ContentItem dcopy = contentDao
                .findContentByPath("/testuser2/a/e/bcopy/c/d");
        Assert.assertNotNull(dcopy);
        Assert.assertEquals(d.getName(), dcopy.getName());
        Assert.assertNotSame(d.getUid(), dcopy.getUid());
        helper.verifyBytes(d.getContent(), dcopy.getContent());

        clearSession();

        b = contentDao.findCollectionByPath("/testuser2/a/b");

        contentDao.copyItem(b, "/testuser2/a/e/bcopyshallow", false);

        clearSession();

        queryCollection = contentDao
                .findCollectionByPath("/testuser2/a/e/bcopyshallow");
        Assert.assertNotNull(queryCollection);

        queryCollection = contentDao
                .findCollectionByPath("/testuser2/a/e/bcopyshallow/c");
        Assert.assertNull(queryCollection);

        clearSession();
        d = contentDao.findContentByUid(d.getUid());
        contentDao.copyItem(d, "/testuser2/dcopy", true);

        clearSession();

        dcopy = contentDao.findContentByPath("/testuser2/dcopy");
        Assert.assertNotNull(dcopy);
    }

    public void testTickets() throws Exception {
        User testuser = getUser(userDao, "testuser");
        String name = "ticketable:" + System.currentTimeMillis();
        ContentItem item = generateTestContent(name, "testuser");

        CollectionItem root = (CollectionItem) contentDao.getRootItem(testuser);
        ContentItem newItem = contentDao.createContent(root, item);

        clearSession();
        newItem = contentDao.findContentByUid(newItem.getUid());

        Ticket ticket1 = new Ticket();
        ticket1.setKey("ticket1");
        ticket1.setTimeout(10);
        ticket1.setOwner(testuser);
        HashSet privs = new HashSet();
        privs.add("priv1");
        privs.add("privs2");
        ticket1.setPrivileges(privs);

        contentDao.createTicket(newItem, ticket1);

        Ticket ticket2 = new Ticket();
        ticket2.setKey("ticket2");
        ticket2.setTimeout(100);
        ticket2.setOwner(testuser);
        privs = new HashSet();
        privs.add("priv3");
        privs.add("priv4");
        ticket2.setPrivileges(privs);

        contentDao.createTicket(newItem, ticket2);

        clearSession();

        newItem = contentDao.findContentByUid(newItem.getUid());
        
        Ticket queryTicket1 = contentDao.getTicket(newItem,"ticket1");
        Assert.assertNotNull(queryTicket1);
        verifyTicket(queryTicket1, ticket1);

        Collection tickets = contentDao.getTickets(newItem);
        Assert.assertEquals(2, tickets.size());
        verifyTicketInCollection(tickets, ticket1.getKey());
        verifyTicketInCollection(tickets, ticket2.getKey());

        contentDao.removeTicket(newItem, ticket1);
        clearSession();
        
        newItem = contentDao.findContentByUid(newItem.getUid());
        
        tickets = contentDao.getTickets(newItem);
        Assert.assertEquals(1, tickets.size());
        verifyTicketInCollection(tickets, ticket2.getKey());

        queryTicket1 = contentDao.getTicket(newItem, "ticket1");
        Assert.assertNull(queryTicket1);

        Ticket queryTicket2 = contentDao.getTicket(newItem, "ticket2");
        Assert.assertNotNull(queryTicket2);
        verifyTicket(queryTicket2, ticket2);

        contentDao.removeTicket(newItem, ticket2);
        
        clearSession();
        newItem = contentDao.findContentByUid(newItem.getUid());

        tickets = contentDao.getTickets(newItem);
        Assert.assertEquals(0, tickets.size());
    }
    
    public void testItemInMutipleCollections() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);
        
        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(a, item);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        CollectionItem b = new CollectionItem();
        b.setName("b");
        b.setOwner(user);
        
        b = contentDao.createCollection(root, b);
        
        contentDao.addItemToCollection(queryItem, b);
        
        clearSession();
        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 2);
        
        b = contentDao.findCollectionByUid(b.getUid());
        contentDao.removeItemFromCollection(queryItem, b);
        clearSession();
        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        a = contentDao.findCollectionByUid(a.getUid());
        contentDao.removeItemFromCollection(queryItem, a);
        clearSession();
        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertNull(queryItem);
    }
    
    public void testItemInMutipleCollectionsDeleteCollection() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        CollectionItem a = new CollectionItem();
        a.setName("a");
        a.setOwner(user);

        a = contentDao.createCollection(root, a);
        
        ContentItem item = generateTestContent();
        item.setName("test");

        ContentItem newItem = contentDao.createContent(a, item);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        CollectionItem b = new CollectionItem();
        b.setName("b");
        b.setOwner(user);
        
        b = contentDao.createCollection(root, b);
        
        contentDao.addItemToCollection(queryItem, b);
        
        clearSession();
        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertEquals(queryItem.getParents().size(), 2);
        
        b = contentDao.findCollectionByUid(b.getUid());
        contentDao.removeCollection(b);
        
        clearSession();
        b = contentDao.findCollectionByUid(b.getUid());
        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertNull(b);
        Assert.assertEquals(queryItem.getParents().size(), 1);
        
        a = contentDao.findCollectionByUid(a.getUid());
        contentDao.removeCollection(a);
        clearSession();
        
        a = contentDao.findCollectionByUid(a.getUid());
        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertNull(a);
        Assert.assertNull(queryItem);
    }
    
    public void testContentDaoTriageStatus() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();
        item.setName("test");
        TriageStatus initialTriageStatus = TriageStatus.createInitialized();
        item.setTriageStatus(initialTriageStatus);

        ContentItem newItem = contentDao.createContent(root, item);

        Assert.assertTrue(newItem.getId() > -1);
        Assert.assertTrue(newItem.getUid() != null);

        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());
        TriageStatus triageStatus = queryItem.getTriageStatus();
        Assert.assertEquals(initialTriageStatus, triageStatus);

        triageStatus.setCode(TriageStatus.CODE_LATER);
        triageStatus.setAutoTriage(false);
        BigDecimal rank = new BigDecimal("-98765.43");
        triageStatus.setRank(rank);
        
        contentDao.updateContent(queryItem);
        clearSession();
        
        queryItem = contentDao.findContentByUid(newItem.getUid());
        triageStatus = queryItem.getTriageStatus();
        Assert.assertEquals(triageStatus.getAutoTriage(), Boolean.FALSE);
        Assert.assertEquals(triageStatus.getCode(),
                            new Integer(TriageStatus.CODE_LATER));
        Assert.assertEquals(triageStatus.getRank(), rank);
        
        queryItem.setTriageStatus(null);
        contentDao.updateContent(queryItem);
        clearSession();
        // should be null triagestatus
        queryItem = contentDao.findContentByUid(newItem.getUid());
        triageStatus = queryItem.getTriageStatus();
        Assert.assertNull(triageStatus);
    }
    
    public void testContentDaoAttributeTombstones() throws Exception {
        User user = getUser(userDao, "testuser");
        CollectionItem root = (CollectionItem) contentDao.getRootItem(user);

        ContentItem item = generateTestContent();

        ContentItem newItem = contentDao.createContent(root, item);
        
        clearSession();

        ContentItem queryItem = contentDao.findContentByUid(newItem.getUid());

        queryItem.removeAttribute(new QName("customattribute"));

        queryItem = contentDao.updateContent(queryItem);

        clearSession();

        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertEquals(0, queryItem.getAttributes().size());
        Assert.assertEquals(1, queryItem.getTombstones().size());
        
        Tombstone ts = queryItem.getTombstones().iterator().next();
        Assert.assertTrue(ts instanceof AttributeTombstone);
        Assert.assertTrue(((AttributeTombstone) ts).getQName().equals(new QName("customattribute")));
        
        queryItem.addAttribute(new StringAttribute(new QName("customattribute"),"customattributevalue"));
        clearSession();

        queryItem = contentDao.findContentByUid(newItem.getUid());
        Assert.assertEquals(1, queryItem.getAttributes().size());
        Assert.assertEquals(0, queryItem.getTombstones().size());
    }
    
    private void verifyTicket(Ticket ticket1, Ticket ticket2) {
        Assert.assertEquals(ticket1.getKey(), ticket2.getKey());
        Assert.assertEquals(ticket1.getTimeout(), ticket2.getTimeout());
        Assert.assertEquals(ticket1.getOwner().getUsername(), ticket2
                .getOwner().getUsername());
        Iterator it1 = ticket1.getPrivileges().iterator();
        Iterator it2 = ticket2.getPrivileges().iterator();

        Assert.assertEquals(ticket1.getPrivileges().size(), ticket1
                .getPrivileges().size());

        while (it1.hasNext())
            Assert.assertEquals(it1.next(), it2.next());
    }

    private void verifyTicketInCollection(Collection tickets, String name) {
        for (Iterator it = tickets.iterator(); it.hasNext();) {
            Ticket ticket = (Ticket) it.next();
            if (ticket.getKey().equals(name))
                return;
        }

        Assert.fail("could not find ticket: " + name);
    }

    private void verifyContains(Collection items, CollectionItem collection) {
        for (Iterator it = items.iterator(); it.hasNext();) {
            Item item = (Item) it.next();
            if (item instanceof CollectionItem
                    && item.getName().equals(collection.getName()))
                return;
        }
        Assert.fail("collection not found");
    }

    private void verifyContains(Collection items, ContentItem content) {
        for (Iterator it = items.iterator(); it.hasNext();) {
            Item item = (Item) it.next();
            if (item instanceof ContentItem
                    && item.getName().equals(content.getName()))
                return;
        }
        Assert.fail("content not found");
    }

    private User getUser(UserDao userDao, String username) {
        return helper.getUser(userDao, contentDao, username);
    }

    private ContentItem generateTestContent() throws Exception {
        return generateTestContent("test", "testuser");
    }

    private ContentItem generateTestContent(String name, String owner)
            throws Exception {
        ContentItem content = new ContentItem();
        content.setName(name);
        content.setDisplayName(name);
        content.setContent(helper.getBytes(baseDir + "/testdata1.txt"));
        content.setContentLanguage("en");
        content.setContentEncoding("UTF8");
        content.setContentType("text/text");
        content.setOwner(getUser(userDao, owner));
        content.addAttribute(new StringAttribute(new QName("customattribute"),
                "customattributevalue"));
        return content;
    }

}
