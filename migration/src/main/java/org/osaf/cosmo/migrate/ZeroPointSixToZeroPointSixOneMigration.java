/*
 * Copyright 2007 Open Source Applications Foundation
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
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Migration implementation that migrates Cosmo 0.6.0
 * database to 0.6.1  
 * 
 * Supports MySQL5 and Derby dialects only.
 *
 */
public class ZeroPointSixToZeroPointSixOneMigration extends AbstractMigration {
    
    private static final Log log = LogFactory.getLog(ZeroPointSixToZeroPointSixOneMigration.class);
    private HibernateHelper hibernateHelper = new HibernateHelper();
    
    @Override
    public String getFromVersion() {
        return "0.6.0";
    }

    @Override
    public String getToVersion() {
        return "0.6.1";
    }

    public void migrateData(Connection conn, String dialect) throws Exception {
        
        log.debug("starting migrateData()");
        
        if(!"MySQL5".equals(dialect) && !"Derby".equals(dialect))
            throw new UnsupportedDialectException("Unsupported dialect " + dialect);
        
        if("Derby".equals(dialect))
            removeDerbyIndexes(conn);
    }
     
    private void removeDerbyIndexes(Connection conn)  throws Exception {
        
        Statement stmt = null;
        log.debug("starting migrateDerbyIndexes()");
        
        try {
            
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select constraintname from sys.sysconstraints, sys.systables where sys.sysconstraints.tableid=sys.systables.tableid and sys.systables.tablename='STAMP' and sys.sysconstraints.constraintname like 'SQL%' order by sys.sysconstraints.constraintname asc");
            
            // should be 2, and the second one is the name we are looking for
            if(!rs.next())
                throw new RuntimeException("error migrating derby indexes, no indexes found");
            if(!rs.next())
                throw new RuntimeException("error migrating derby indexes, only 1 index found");
            
            String indexName = rs.getString(1);
            rs.close();
            
            
            log.debug("dropping index " + indexName);
            stmt.execute("alter table stamp drop unique " + indexName);
        } finally {
            if(stmt!=null)
                stmt.close();
        }
    }

}
