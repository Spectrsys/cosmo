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
package org.osaf.cosmo.dao.jcr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;

import org.apache.jackrabbit.util.Text;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.CosmoConstants;
import org.osaf.cosmo.TestHelper;
import org.osaf.cosmo.icalendar.ICalendarConstants;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.User;

/**
 */
public class JcrTestHelper extends TestHelper
    implements ICalendarConstants, JcrConstants {
    private static final Log log = LogFactory.getLog(JcrTestHelper.class);

    static int nseq = 0;
    static int pseq = 0;

    private Session session;

    public JcrTestHelper(Session session) {
        super();
        this.session = session;
    }

    /**
     */
    public Session getSession() {
        return session;
    }

    /**
     */
    private String newNodeName() {
        String serial = new Integer(++nseq).toString();
        return "dummy" + serial;
    }

    /**
     */
    public Node addNode()
        throws RepositoryException {
        return addNode(newNodeName());
    }

    /**
     */
    public Node addNode(String name)
        throws RepositoryException {
        return addNode(session.getRootNode(), name);
    }

    /**
     */
    public Node addNode(Node parent)
        throws RepositoryException {
        return addNode(parent, newNodeName());
    }

    /**
     */
    public Node addNode(Node parent,
                        String name)
        throws RepositoryException {
        String trimmed = name.indexOf("/") >= 0 ? Text.getName(name) : name;
        return parent.addNode(trimmed);
    }

    /**
     */
    public Node addFileNode(InputStream data,
                            String mimetype,
                            String charset)
        throws RepositoryException {
        return addFileNode(session.getRootNode(), data, mimetype, charset);
    }

    /**
     */
    public Node addFileNode(Node parent,
                            InputStream data,
                            String mimetype,
                            String charset)
        throws RepositoryException {
        return addFileNode(parent, data, mimetype, charset, newNodeName());
    }

    /**
     */
    public Node addFileNode(Node parent,
                            InputStream data,
                            String mimetype,
                            String charset,
                            String name)
        throws RepositoryException {
        String trimmed = name.indexOf("/") >= 0 ? Text.getName(name) : name;
        Node node = parent.addNode(trimmed, NT_FILE);

        Node content = node.addNode(NN_JCR_CONTENT, NT_RESOURCE);
        content.setProperty(NP_JCR_DATA, data);
        content.setProperty(NP_JCR_MIMETYPE, mimetype);
        content.setProperty(NP_JCR_ENCODING, charset);
        content.setProperty(NP_JCR_LASTMODIFIED,
                            java.util.Calendar.getInstance());

        return node;
    }

    /**
     */
    public javax.jcr.Property addProperty(Node node)
        throws RepositoryException {
        String serial = new Integer(++pseq).toString();
        String name = "dummy" + serial;

        return node.setProperty(name, name);
    }

    /**
     */
    public User makeAndStoreDummyUser()
        throws RepositoryException {
        User user = makeDummyUser();

        Node node = session.getRootNode().addNode(user.getUsername());
        node.addMixin(NT_USER);
        JcrUserMapper.userToNode(user, node);

        return user;
    }

    /**
     */
    public User findDummyUser(String username)
        throws RepositoryException {
        return session.getRootNode().hasNode(username) ?
            JcrUserMapper.nodeToUser(session.getRootNode().getNode(username)) :
            null;
    }

    /**
     */
    public void removeDummyUser(User user)
        throws RepositoryException {
        if (! session.getRootNode().hasNode(user.getUsername())) {
            return;
        }
        session.getRootNode().getNode(user.getUsername()).remove();
    }

    /**
     */
    public Node addTicketableNode()
        throws RepositoryException {
        Node node = addNode();

        node.addMixin(NT_TICKETABLE);

        return node;
    }

    /**
     */
    public Ticket makeAndStoreDummyTicket(Node node,
                                          User user)
        throws RepositoryException {
        Ticket ticket = makeDummyTicket(user);

        Node ticketNode = node.addNode(NN_TICKET, NT_TICKET);
        JcrTicketMapper.ticketToNode(ticket, ticketNode);

        return ticket;
    }

    /**
     */
    public Ticket findDummyTicket(Node node,
                                  String id)
        throws RepositoryException {
        for (NodeIterator i = node.getNodes(NN_TICKET); i.hasNext();) {
            Node child = i.nextNode();
            if (child.getProperty(NP_TICKET_ID).getString().equals(id)) {
                return JcrTicketMapper.nodeToTicket(child);
            }
        }
        return null;
    }

    /**
     */
    public Node addCalendarCollectionNode()
        throws RepositoryException {
        return addCalendarCollectionNode(newNodeName());
    }

    /**
     */
    public Node addCalendarCollectionNode(String name)
        throws RepositoryException {
        Node node = addNode(name);

        node.addMixin(NT_CALENDAR_COLLECTION);
        node.setProperty(NP_CALENDAR_DESCRIPTION, node.getName());
        node.setProperty(NP_XML_LANG, Locale.getDefault().toString());

        return node;
    }

    /**
     */
    public Node addDavResourceNode(InputStream data,
                                   String mimetype,
                                   String charset)
        throws RepositoryException {
        return addDavResourceNode(session.getRootNode(), data, mimetype,
                                  charset);
    }

    /**
     */
    public Node addDavResourceNode(Node parent,
                                   InputStream data,
                                   String mimetype,
                                   String charset)
        throws RepositoryException {
        return addDavResourceNode(parent, data, mimetype, charset,
                                  newNodeName());
    }

    /**
     */
    public Node addDavResourceNode(Node parent,
                                   InputStream data,
                                   String mimetype,
                                   String charset,
                                   String name)
        throws RepositoryException {
        Node node = addFileNode(parent, data, mimetype, charset, name);

        node.addMixin(NT_DAV_RESOURCE);
        node.addMixin(NT_TICKETABLE);
        node.setProperty(NP_DAV_DISPLAYNAME, node.getName());
        node.setProperty(NP_DAV_CONTENTLANGUAGE,
                         Locale.getDefault().getLanguage());

        return node;
    }

    /**
     */
    public Node addCalendarResourceNode(Calendar calendar)
        throws RepositoryException {
        return addCalendarResourceNode(session.getRootNode(), calendar);
    }

    /**
     */
    public Node addCalendarResourceNode(Node node,
                                        Calendar calendar)
        throws RepositoryException {
        return addCalendarResourceNode(node, calendar, newNodeName());
    }

    /**
     */
    public Node addCalendarResourceNode(Node node,
                                        String name)
        throws RepositoryException {
        return addCalendarResourceNode(node, loadCalendar(name), name);
    }

    public Node addCalendarResourceNode(Node node,
                                        Calendar calendar,
                                        String name)
        throws RepositoryException {
        InputStream data =
            new ByteArrayInputStream(calendar.toString().getBytes());

        Node resourceNode = addDavResourceNode(node, data, CONTENT_TYPE, "utf8",
                                               name);

        resourceNode.addMixin(NT_CALENDAR_RESOURCE);
        // XXX: assume the calendar represents an event
        resourceNode.addMixin(NT_EVENT_RESOURCE);

        // XXX: move into JcrResourceMapper!!@#$!@!@
        Component event = (Component) calendar.getComponents().
            getComponents(Component.VEVENT).
            get(0);
        Property uid = (Property)
            event.getProperties().getProperty(Property.UID);
        resourceNode.setProperty(NP_CALENDAR_UID, uid.getValue());

        if (! CosmoConstants.INDEX_VIRTUAL_PROPERTIES) {
            if (log.isDebugEnabled()) {
                log.debug("storing flattened properties");
            }
            // set flattened properties
            // XXX: if the node is being updated, find the
            // properties that previously existed but are not in
            // the new entity and nuke them
            JcrCalendarFlattener flattener = new JcrCalendarFlattener();
            Map flattened = flattener.flattenCalendarObject(calendar);
            for (Iterator i=flattened.entrySet().iterator();
                 i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                if (log.isDebugEnabled()) {
                    log.debug("setting flattened property " +
                              entry.getKey() +
                              " = " + entry.getValue());
                }
                resourceNode.setProperty(entry.getKey().toString(),
                                         entry.getValue().toString());
            }
        }
        
        return node;
    }

    /**
     */
    public Calendar loadCalendar(String name) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new IllegalStateException("resource " + name + " not found");
        }
        try {
            return new CalendarBuilder().build(in);
        } catch (Exception e) {
            throw new RuntimeException("error loading calendar", e);
        }
    }
}
