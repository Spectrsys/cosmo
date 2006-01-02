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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.dao.jcr.ContentStoreDAOJCR;

/**
 * A tool that reads a list of usernames from a supplied file and
 * creates a home directory for each one.
 */
public class HomeDirectoryRecoverer {
    private static final Log log =
        LogFactory.getLog(HomeDirectoryRecoverer.class);

    /**
     */
    public static void createHomeDirectoriesFromFile(String filename)
        throws FileNotFoundException, IOException {
        Set usernames = loadUsernames(filename);
    }

    /**
     */
    protected static Set loadUsernames(String filename)
        throws FileNotFoundException, IOException {
        HashSet usernames = new HashSet();
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
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            log.error("Usage: " + HomeDirectoryRecoverer.class.getName() +
                      " <input file>");
            System.exit(1);
        }

        try {
            createHomeDirectoriesFromFile(args[0]);
        } catch (FileNotFoundException e) {
            log.error("Can't open input file: " + e.getMessage());
            System.exit(2);
        } catch (IOException e) {
            log.error("Can't read input file: " + e.getMessage());
            System.exit(3);
        }
    }
}
