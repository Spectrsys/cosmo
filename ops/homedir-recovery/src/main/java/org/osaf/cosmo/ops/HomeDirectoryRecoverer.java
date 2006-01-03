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
package org.osaf.cosmo.ops;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.commons.jackrabbit.RepositoryClient;

/**
 * A tool that reads a list of usernames either from a file or
 * directly from the user database and creates a home directory for
 * each one.
 */
public class HomeDirectoryRecoverer extends RepositoryClient {
    private static final Log log =
        LogFactory.getLog(HomeDirectoryRecoverer.class);

    private static final String DB_DRIVER = "org.hsqldb.jdbcDriver";
    private static final String DB_USERNAME = "sa";
    private static final String DB_PASSWORD = "";
    private static final String SQL_LOAD_USERNAMES =
        "SELECT LIMIT 0 0 username FROM user ORDER BY UPPER(username)";
    private static final String SQL_SHUTDOWN = "SHUTDOWN";

    /**
     * Returns a list of usernames from the named file in the order
     * they were specified in the file. Expects each line to contain a
     * single username. Terminates upon reaching a blank line.
     */
    public List loadUsernamesFromFile(String filename)
        throws FileNotFoundException, IOException {
        log.info("Loading user accounts from " + filename);
        ArrayList usernames = new ArrayList();
        BufferedReader in = new BufferedReader(new FileReader(filename));
        try {
            String username = in.readLine();
            while (username != null) {
                // hsqldb writes a blank line after the last row of data,
                // so we can stop reading at this point
                if (username.matches("^\\s*$")) {
                    break;
                }
                usernames.add(username);
                username = in.readLine();
            }
        } finally {
            in.close();
        }
        return usernames;
    }

    /**
     * Returns a list of usernames from the specified user database
     * ordered by username.
     */
    public List loadUsernamesFromDatabase(String url)
        throws ClassNotFoundException, SQLException {
        log.info("Loading user accounts from " + url);
        ArrayList usernames = new ArrayList();
        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(url, DB_USERNAME,
                                                      DB_PASSWORD);
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(SQL_LOAD_USERNAMES);
            for (; rs.next();) {
                usernames.add(rs.getString(1));
            }
            st.close();
        } finally {
            Statement st = conn.createStatement();
            st.execute(SQL_SHUTDOWN);
            conn.close();
        }
        return usernames;
    }

    /**
     * Creates a home directory node in the repository for each
     * specified username. Ignores <code>root</code> if that username
     * is in the list.
     */
    public void createHomeDirectories(List usernames)
        throws RepositoryException {
        log.info("Opening repository at " + getRepositoryHomedirPath() +
                 " with config " + getConfigFilePath());
        Repository repository = openRepository();
        try {
            Session session = repository.login(getCredentials());
            for (Iterator i=usernames.iterator(); i.hasNext();) {
                String username = (String) i.next();
                // root doesn't get a home directory
                if (username.equals("root")) {
                    continue;
                }
                createHomeDirectory(session, username);
            }
            session.logout();
        } finally {
            closeRepository(repository);
        }
    }

    private void createHomeDirectory(Session session,
                                     String username)
        throws RepositoryException {
        log.info("Creating home directory for " + username);
        Node rootNode = session.getRootNode();
        Node homedirNode = rootNode.addNode(hexEscape(username),
                                            "dav:collection");
        homedirNode.addMixin("caldav:home");
        homedirNode.addMixin("mix:ticketable");
        homedirNode.setProperty("dav:displayname", username);
        homedirNode.setProperty("caldav:calendar-description", username);
        homedirNode.setProperty("xml:lang", Locale.getDefault().toString());
        rootNode.save();
    }

    // copied from JCREscapist in order to avoid direct cosmo
    // dependency
    private static String hexEscape(String str) {
        StringBuffer buf = null;
        int length = str.length();
        int pos = 0;
        for (int i = 0; i < length; i++) {
            int ch = str.charAt(i);
            switch (ch) {
            case '.':
                if (i >= 2) {
                    // . only needs to be escaped if it's the first or
                    // second character
                    continue;
                }
            case '/':
            case ':':
            case '[':
            case ']':
            case '*':
            case '"':
            case '|':
            case '\'':
                if (buf == null) {
                    buf = new StringBuffer();
                }
                if (i > 0) {
                    buf.append(str.substring(pos, i));
                }
                pos = i + 1;
                break;
            default:
                continue;
            }
            buf.append("%").append(Integer.toHexString(ch));
        }
        
        if (buf == null) {
            return str;
        }

        if (pos < length) {
            buf.append(str.substring(pos));
        }
        return buf.toString();
    }

    /**
     */
    public static void main(String[] args) {
        if (args.length != 4 ||
            ! (args[0].equals("-file") ||
               args[0].equals("-db"))) {
            log.error("Usage: " + HomeDirectoryRecoverer.class.getName() +
                      " (-file <input file> | -db <jdbc url>) " +
                      "<repository config file path> " +
                      "<repository home dir path>");
            System.exit(1);
        }

        HomeDirectoryRecoverer recoverer = new HomeDirectoryRecoverer();
        recoverer.setConfigFilePath(args[2]);
        recoverer.setRepositoryHomedirPath(args[3]);
        recoverer.setCredentials("cosmo_repository", "");

        List usernames = null;
        if (args[0].equals("-file")) {
            try {
                usernames = recoverer.loadUsernamesFromFile(args[1]);
            } catch (FileNotFoundException e) {
                log.error("Can't open input file: " + e.getMessage());
                System.exit(2);
            } catch (IOException e) {
                log.error("Can't read input file: " + e.getMessage());
                System.exit(3);
            }
        }
        else {
            try {
                usernames = recoverer.loadUsernamesFromDatabase(args[1]);
            } catch (ClassNotFoundException e) {
                log.error("Can't find HSQL driver: " + e.getMessage());
                System.exit(4);
            } catch (SQLException e) {
                log.error("Can't load usernames from database: " +
                          e.getMessage());
                System.exit(5);
            }
        }

        try {
            recoverer.createHomeDirectories(usernames);
        } catch (RepositoryException e) {
            log.error("Repository error", e);
            System.exit(999);
        }
    }
}
