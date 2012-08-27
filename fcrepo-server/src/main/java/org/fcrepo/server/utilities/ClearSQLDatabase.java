
package org.fcrepo.server.utilities;

import org.fcrepo.server.Server;
import org.fcrepo.server.utilities.rebuild.SQLRebuilder;

/**
 * Clears the SQL database post-install. Can be called by system test scripts to
 * make sure that the configured SQL database is empty after installing fedora,
 * but before running it for the first time.
 *
 * @author Aaron Birkland
 * @version $Id$
 */
public class ClearSQLDatabase {

    public static void main(String[] args) {
        try {

            SQLRebuilder sqlDb = new SQLRebuilder();
            sqlDb.setServerConfiguration(Server.getConfig());
            sqlDb.init();
            sqlDb.blankExistingTables();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
