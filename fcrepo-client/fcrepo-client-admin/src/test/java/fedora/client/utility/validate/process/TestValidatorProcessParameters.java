/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.process;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import fedora.client.utility.validate.process.ValidatorProcessParameters.IteratorType;

import fedora.server.errors.QueryParseException;
import fedora.server.search.Condition;
import fedora.server.search.FieldSearchQuery;

import static junit.framework.Assert.assertEquals;

import static fedora.client.utility.validate.process.ValidatorProcessParameters.PARAMETER_PASSWORD;
import static fedora.client.utility.validate.process.ValidatorProcessParameters.PARAMETER_PIDFILE;
import static fedora.client.utility.validate.process.ValidatorProcessParameters.PARAMETER_QUERY;
import static fedora.client.utility.validate.process.ValidatorProcessParameters.PARAMETER_SERVER_URL;
import static fedora.client.utility.validate.process.ValidatorProcessParameters.PARAMETER_TERMS;
import static fedora.client.utility.validate.process.ValidatorProcessParameters.PARAMETER_USERNAME;

/**
 * @author Jim Blake
 */
public class TestValidatorProcessParameters {

    private static final String PIDFILE_PATH = "/my/file/path";

    @Test
    public void simpleTermsSuccess() throws MalformedURLException {
        ValidatorProcessParameters parms =
                createParms(PARAMETER_USERNAME,
                            "username",
                            PARAMETER_PASSWORD,
                            "password",
                            PARAMETER_SERVER_URL,
                            "http://some.url/",
                            PARAMETER_TERMS,
                            "terms");
        assertEquals("username", "username", parms.getServiceInfo()
                .getUsername());
        assertEquals("password", "password", parms.getServiceInfo()
                .getPassword());
        assertEquals("serverurl", new URL("http://some.url/"), parms
                .getServiceInfo().getBaseUrl());
        assertEquals("iteratorType", IteratorType.FS_QUERY, parms
                .getIteratorType());
        FieldSearchQuery fsq = parms.getQuery();
        assertEquals("queryType", FieldSearchQuery.TERMS_TYPE, fsq.getType());
        assertEquals("terms", "terms", fsq.getTerms());
    }

    @Test
    public void simpleQuerySuccess() throws QueryParseException,
            MalformedURLException {
        ValidatorProcessParameters parms =
                createParms(PARAMETER_USERNAME,
                            "username",
                            PARAMETER_PASSWORD,
                            "password",
                            PARAMETER_SERVER_URL,
                            "http://some.url/",
                            PARAMETER_QUERY,
                            "pid=fred");
        assertEquals("username", "username", parms.getServiceInfo()
                .getUsername());
        assertEquals("password", "password", parms.getServiceInfo()
                .getPassword());
        assertEquals("serverurl", new URL("http://some.url/"), parms
                .getServiceInfo().getBaseUrl());
        assertEquals("iteratorType", IteratorType.FS_QUERY, parms
                .getIteratorType());
        FieldSearchQuery fsq = parms.getQuery();
        assertEquals("queryType", FieldSearchQuery.CONDITIONS_TYPE, fsq
                .getType());
        assertEquals("conditions", Condition.getConditions("pid=fred"), fsq
                .getConditions());
    }

    @Test
    public void simplePidfileSuccess() throws QueryParseException, IOException {
        File dummyFile = null;
        try {
            dummyFile =
                    File.createTempFile("TestValidatorProcessParameter",
                                        "dummyFile");
            dummyFile.deleteOnExit();
            ValidatorProcessParameters parms =
                    createParms(PARAMETER_USERNAME,
                                "username",
                                PARAMETER_PASSWORD,
                                "password",
                                PARAMETER_SERVER_URL,
                                "http://some.url/",
                                PARAMETER_PIDFILE,
                                dummyFile.getPath());
            assertEquals("username", "username", parms.getServiceInfo()
                    .getUsername());
            assertEquals("password", "password", parms.getServiceInfo()
                    .getPassword());
            assertEquals("serverurl", new URL("http://some.url/"), parms
                    .getServiceInfo().getBaseUrl());
            assertEquals("iteratorType", IteratorType.PIDFILE, parms
                    .getIteratorType());
            File pidfile = parms.getPidfile();
            assertEquals("pidfile", dummyFile, pidfile);
        } finally {
            if (dummyFile != null || dummyFile.exists()) {
                dummyFile.delete();
            }
        }
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void unrecognizedKeyword() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_TERMS,
                    "terms",
                    "-junk",
                    "junk");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void valueWithoutKeyword() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    "garbage",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_TERMS,
                    "terms");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void noUsername() {
        createParms(PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_TERMS,
                    "terms");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void nullUsername() {
        createParms(PARAMETER_USERNAME,
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_TERMS,
                    "terms");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void noPassword() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_TERMS,
                    "terms");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void noServerUrl() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_TERMS,
                    "terms");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void invalidServerUrl() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "",
                    PARAMETER_TERMS,
                    "terms");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void noTermsOrQueryOrPidfile() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void bothTermsAndQuery() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_TERMS,
                    "terms",
                    PARAMETER_QUERY,
                    "pid=fred");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void bothTermsAndPidfile() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_TERMS,
                    "terms",
                    PARAMETER_PIDFILE,
                    PIDFILE_PATH);
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void bothQueryAndPidfile() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_QUERY,
                    "pid=fred",
                    PARAMETER_PIDFILE,
                    PIDFILE_PATH);
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void invalidQueryString() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_QUERY,
                    "pid&fred");
    }

    @Test(expected = ValidatorProcessUsageException.class)
    public void pidfileDoesNotExist() {
        createParms(PARAMETER_USERNAME,
                    "username",
                    PARAMETER_PASSWORD,
                    "password",
                    PARAMETER_SERVER_URL,
                    "http://some.url/",
                    PARAMETER_PIDFILE,
                    "/bogus/pidfile/path");
    }

    private ValidatorProcessParameters createParms(String... args) {
        return new ValidatorProcessParameters(args);
    }
}
