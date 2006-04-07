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
package org.osaf.cosmo.migrate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.model.HomeCollectionResource;
import org.osaf.cosmo.model.ResourceProperty;
import org.osaf.cosmo.model.Ticket;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.repository.HexEscaper;
import org.osaf.cosmo.repository.ResourceMapper;
import org.osaf.cosmo.repository.TicketMapper;
import org.osaf.cosmo.repository.UserMapper;

/**
 * Migrates the 0.2 schema to 0.3 and vice versa.
 */
public class Migration03 extends CopyBasedMigration {
    private static final Log log = LogFactory.getLog(Migration03.class);

    /**
     * <code>cosmo.migrate.03.userdb.url</code>
     */
    public static final String SYSPROP_USERDB_URL =
        "cosmo.migrate.03.userdb.url";

    private static final String VERSION = "0.3";
    private static final String DB_DRIVER = "org.hsqldb.jdbcDriver";
    private static final String DB_USERNAME = "sa";
    private static final String DB_PASSWORD = "";
    private static final String SQL_LOAD_OVERLORD =
        "SELECT username, password, firstName, lastName, email, dateCreated, dateModified FROM user WHERE username = 'root'";
    private static final String SQL_LOAD_USERS =
        "SELECT LIMIT 0 0 username, password, firstName, lastName, email, dateCreated, dateModified, id FROM user WHERE username != 'root'";
    private static final String SQL_LOAD_ROOT_IDS =
        "SELECT userid FROM userrole WHERE roleid = 1 AND userid != 1";
    private static final String SQL_SHUTDOWN = "SHUTDOWN";

    private Connection connection;

    // CopyBasedMigration methods

    /**
     * Connects to the 0.2 user database using the JDBC URL supplied
     * by the {@link #SYSPROP_USERDB_URL} system property, unless a
     * connection has previously been set with
     * {@link #setConnection(Connection)}.
     */
    public void init()
        throws MigrationException {
        if (connection != null) {
            return;
        }

        String url = System.getProperty(SYSPROP_USERDB_URL);
        if (url == null) {
            throw new MigrationException("System property " + SYSPROP_USERDB_URL + " not found");
        }

        log.info("Connecting to " + url);
        try {
            Class.forName(DB_DRIVER);
            connection = DriverManager.getConnection(url, DB_USERNAME,
                                                     DB_PASSWORD);
        } catch (Exception e) {
            throw new MigrationException("Cannot connect to userdb at " +
                                         url, e);
        }
    }

    /**
     */
    public void up(Session previous,
                   Session current)
        throws MigrationException {
        try {
            registerCurrentNamespaces(previous, current);
        } catch (RepositoryException e) {
            throw new MigrationException("unable to register namespaces into current repository", e);
        }

        User overlord = loadOverlord();
        Map users = loadUsers();

        for (Iterator i=users.values().iterator(); i.hasNext();) {
            User user = (User) i.next();
            try {
                HomeCollectionResource currentHome =
                    createCurrentHome(user, previous, current);
            } catch (MigrationException e) {
                log.error("SKIPPING " + user.getUsername(), e);
                continue;
            }
        }
    }

    /**
     */
    public void down(Session current,
                     Session previous)
        throws MigrationException {
    }

    /**
     */
    public void release()
        throws MigrationException {
        try {
            if (connection != null) {
                Statement st = connection.createStatement();
                st.execute(SQL_SHUTDOWN);
                connection.close();
            }
        } catch (Exception e) {
            throw new MigrationException("Cannot shut down user db", e);
        }
    }

    /**
     */
    public String getVersion() {
        return VERSION;
    }

    // our methods

    /**
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     */
    public Connection getConnection() {
        return connection;
    }

    // package protected methods, for individuable testability

    void registerCurrentNamespaces(Session previous,
                                   Session current)
        throws RepositoryException {
        NamespaceRegistry prevNsReg =
            previous.getWorkspace().getNamespaceRegistry();
        NamespaceRegistry curNsReg =
            current.getWorkspace().getNamespaceRegistry();

        String[] prevPrefixes = prevNsReg.getPrefixes();
        for (int i=0; i<prevPrefixes.length; i++) {
            String prefix = prevPrefixes[i];
            try {
                curNsReg.getURI(prefix);
            } catch (NamespaceException e) {
                // namespace prefix is not registered in the current
                // repository, so register it
                curNsReg.registerNamespace(prefix, prevNsReg.getURI(prefix));
            }
        }
    }

    User loadOverlord()
        throws MigrationException {
        User overlord = null;

        log.info("Loading overlord");
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(SQL_LOAD_OVERLORD);
            for (; rs.next();) {
                overlord = resultSetToUser(rs);
                overlord.setAdmin(Boolean.TRUE);
            }
            st.close();
        } catch (Exception e) {
            throw new MigrationException("Cannot load overlord", e);
        }

        return overlord;
    }

    Map loadUsers()
        throws MigrationException {
        HashMap users = new HashMap();

        log.info("Loading users");
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(SQL_LOAD_USERS);
            for (; rs.next();) {
                User user = resultSetToUser(rs);
                Integer id = rs.getInt("id");
                log.info(user.getUsername());
                users.put(id, user);
            }
            st.close();
        } catch (Exception e) {
            throw new MigrationException("Cannot load users", e);
        }

        log.info("Loading root roles");
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(SQL_LOAD_ROOT_IDS);
            for (; rs.next();) {
                Integer id = rs.getInt("userid");
                User user = (User) users.get(id);
                if (user == null) {
                    log.warn("Nonexistent user with id " + id + " marked as having root role in userdb... skipping:");
                    continue;
                }
                user.setAdmin(Boolean.TRUE);
            }
            st.close();
        } catch (Exception e) {
            throw new MigrationException("Cannot load users", e);
        }

        return users;
    }

    HomeCollectionResource createCurrentHome(User user,
                                             Session previous,
                                             Session current)
        throws MigrationException {
        try {
            // find old home node
            String oldHomePath = "/" + HexEscaper.escape(user.getUsername());
            Node oldHomeNode = (Node) previous.getItem(oldHomePath);

            // create new home node
            HomeCollectionResource newHome = oldNodeToHome(oldHomeNode);
            Node newHomeNode =
                ResourceMapper.createHomeCollection(newHome,
                                                    user.getUsername(),
                                                    current);
            UserMapper.userToNode(user, newHomeNode);

            return newHome;
        } catch (RepositoryException e) {
            throw new MigrationException("Failed to create home collection", e);
        }
    }

    private User resultSetToUser(ResultSet rs)
        throws SQLException {
        User user = new User();

        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFirstName(rs.getString("firstName"));
        user.setLastName(rs.getString("lastName"));
        user.setEmail(rs.getString("email"));
        user.setAdmin(Boolean.FALSE);
        user.setDateCreated(rs.getDate("dateCreated"));
        user.setDateModified(rs.getDate("dateModified"));

        return user;
    }

    private HomeCollectionResource oldNodeToHome(Node node) 
        throws RepositoryException {
        HomeCollectionResource home = new HomeCollectionResource();

        // set cosmo properties
        home.setDisplayName(HexEscaper.unescape(node.getName()));
        home.setDateCreated(node.getProperty("jcr:created").getDate().
                            getTime());

        // find old home custom properties
        for (PropertyIterator i=node.getProperties(); i.hasNext();) {
            Property p = i.nextProperty();
            if (p.getName().startsWith("cosmo:") ||
                p.getName().startsWith("jcr:") ||
                p.getName().startsWith("dav:") ||
                p.getName().startsWith("caldav:") ||
                p.getName().startsWith("icalendar:") ||
                p.getName().startsWith("xml")) {
                continue;
            }
            ResourceProperty rp = new ResourceProperty();
            rp.setName(p.getName());
            rp.setValue(p.getString());
            home.getProperties().add(rp);
        }

        // set old home tickets
        for (NodeIterator i=node.getNodes("ticket:ticket"); i.hasNext();) {
            Node child = i.nextNode();
            // we can use TicketMapper since the ticket node type did
            // not change between versions
            Ticket ticket = TicketMapper.nodeToTicket(child);
            // ignore timed out tickets
            if (ticket.hasTimedOut()) {
                continue;
            }
            home.getTickets().add(ticket);
        }

        return home;
    }
}
