/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import java.io.IOException;

import java.net.URLEncoder;

import fedora.client.FedoraClient;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class RISearchUtil {

    private static final String RISEARCH_COUNT =
            "/risearch?type=triples&lang=spo&format=count&stream=on&"
                    + "flush=true&query=";

    public static void checkSPOCount(FedoraClient client,
                                     String query,
                                     int expectedCount) {
        int actualCount = getSPOCount(client, query);
        assertEquals("Expected " + expectedCount + " results from SPO query"
                             + " " + query + ", but got " + actualCount,
                     expectedCount,
                     actualCount);
    }

    public static int getSPOCount(FedoraClient client, String query) {
        String response = null;
        try {
            response =
                    client
                            .getResponseAsString(RISEARCH_COUNT
                                                         + URLEncoder
                                                                 .encode(query,
                                                                         "UTF-8"),
                                                 true,
                                                 true).trim();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error while querying resource index (is it enabled?).  "
                    + "See stack trace");
        }
        int count = 0;
        try {
            count = Integer.parseInt(response);
        } catch (NumberFormatException e) {
            fail("Expected numeric plaintext response body from RI query, but "
                    + "got the following: " + response);
        }
        return count;
    }
}
