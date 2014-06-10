package org.fcrepo.server.storage;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import javax.ws.rs.core.HttpHeaders;

import org.apache.http.Header;
import org.fcrepo.common.Constants;
import org.fcrepo.common.http.HttpInputStream;
import org.fcrepo.common.http.WebClient;
import org.fcrepo.common.http.WebClientConfiguration;
import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.HttpServiceNotFoundException;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.storage.translation.DOTranslationUtility;
import org.fcrepo.server.utilities.ServerUtility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
    "org.xml.sax.*", "javax.management.*", "javax.net.ssl.*"})
@PrepareForTest({DOTranslationUtility.class, ServerUtility.class})
public class DefaultExternalContentManagerTest {

    private DefaultExternalContentManager testObj;
    
    private static Field webClientField;
    
    private static boolean webClientFieldAccessible;
    
    private static final String TEST_URL =
            "http://foo.com/bar";
    
    private static final String FILE = "file";
    private static final String HTTP = "http";
    
    @Mock
    private Server mockServer;
    
    @Mock
    private ContentManagerParams mockParams;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private WebClient mockClient;
    
    @Mock
    private WebClientConfiguration mockClientConfig;
    
    @Mock
    private HttpInputStream mockResponse;
    
    @BeforeClass
    public static void bootstrap() throws NoSuchFieldException, SecurityException {
        webClientField = DefaultExternalContentManager.class.getDeclaredField("m_http");
        webClientFieldAccessible = webClientField.isAccessible();
        webClientField.setAccessible(true);
    }
    
    @AfterClass
    public static void cleanUp() {
        webClientField.setAccessible(webClientFieldAccessible);
    }
    
    @Before
    public void setUp()
            throws ModuleInitializationException, IllegalArgumentException, IllegalAccessException {
        when(mockServer.getWebClientConfig()).thenReturn(mockClientConfig);
        testObj = new DefaultExternalContentManager(new HashMap<String, String>(), mockServer,
                ExternalContentManager.class.getName());
        webClientField.set(testObj, mockClient);
        when(mockParams.getContext()).thenReturn(mockContext);
    }
    
    @Test
    public void testInit() throws ModuleInitializationException {
        when(mockClientConfig.getMaxConnPerHost()).thenReturn(5);
        when(mockClientConfig.getMaxTotalConn()).thenReturn(5);
        testObj.initModule();
    }
    
    private void mockResponseFor(String httpMethod) {
        when(mockParams.getUrl()).thenReturn(TEST_URL);
        when(mockParams.getProtocol()).thenReturn(HTTP);
        when(mockContext.getEnvironmentValue(Constants.HTTP_REQUEST.METHOD.attributeId)).thenReturn(httpMethod);
        String cLen = Long.toString(System.currentTimeMillis());
        when(mockResponse.getResponseHeaderValue(HttpHeaders.CONTENT_LENGTH, "-1")).thenReturn(cLen);
        when(mockResponse.getResponseHeaders()).thenReturn(new Header[0]);
        mockStatic(DOTranslationUtility.class);
        when(DOTranslationUtility.makeAbsoluteURLs(TEST_URL)).thenReturn(TEST_URL);
        mockStatic(ServerUtility.class);
        when(ServerUtility.isURLFedoraServer(TEST_URL)).thenReturn(false);
    }
    
    @Test
    public void testPassthroughHeadMethod() throws HttpServiceNotFoundException, GeneralException, IOException {
        mockResponseFor("HEAD");
        when(mockClient.head(TEST_URL, true, null, null))
        .thenReturn(mockResponse);
        testObj.getExternalContent(mockParams);
        verify(mockClient).head(TEST_URL, true, null, null);
    }

    @Test
    public void testDefaultToGetMethod() throws HttpServiceNotFoundException, GeneralException, IOException {
        mockResponseFor(null);
        when(mockClient.get(TEST_URL, true, null, null, null, null, null))
        .thenReturn(mockResponse);
        testObj.getExternalContent(mockParams);
        verify(mockClient).get(TEST_URL, true, null, null, null, null, null);
    }
}
