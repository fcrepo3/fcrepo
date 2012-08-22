/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import org.fcrepo.client.FedoraClient;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class RISearchUtil {

    private static final String RISEARCH_SPO_COUNT =
            "/risearch?type=triples&lang=spo&format=count&stream=on&"
                    + "flush=true&query=";

    private static final String RISEARCH_SPARQL_COUNT =
            "/risearch?type=tuples&lang=sparql&format=count&stream=on&"
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
    
    public static void checkSPARQLCount(FedoraClient client,
                                     String query,
                                     int expectedCount) {
        int actualCount = getSPARQLCount(client, query);
        assertEquals("Expected " + expectedCount + " results from SPARQL query"
                             + " " + query + ", but got " + actualCount,
                     expectedCount,
                     actualCount);
    }

    private static int getCount(FedoraClient client, String path, String query) {
        String response = null;
        try {
            path =  path + URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("Encoding error while querying resource index. See stack trace");
        }
        try {
            response = client.getResponseAsString(path,
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
    
    public static int getSPARQLCount(FedoraClient client, String query) {
        return getCount(client, RISEARCH_SPARQL_COUNT, query);
    }

    public static int getSPOCount(FedoraClient client, String query) {
        return getCount(client, RISEARCH_SPO_COUNT, query);
    }
}
