
package melcoe.fedora.user;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import melcoe.fedora.util.DataUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class UserServlet
        extends HttpServlet {

    private static final long serialVersionUID = 8591611806356037463L;

    private static final Logger log = Logger.getLogger(UserServlet.class);

    private static final String FEDORA_ATTRS_KEY =
            "FEDORA_AUX_SUBJECT_ATTRIBUTES";

    //private static final String ROLE_ATTR_KEY = "fedoraRole";

    private DocumentBuilder documentBuilder = null;

    @Override
    public void init() throws ServletException {
        try {
            DocumentBuilderFactory documentBuilderFactory =
                    DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            log.error("Unable to initialise UserServlet: " + pce.getMessage(),
                      pce);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String userId = request.getRemoteUser();
        if (userId == null) {
            userId = "anonymous";
        }

        Map<String, Set<String>> subjectAttributes =
                getSubjectAttributes(request);

        Document doc = documentBuilder.newDocument();
        doc.setXmlVersion("1.0");

        Element root = doc.createElement("user");
        root.setAttribute("id", userId);

        doc.appendChild(root);

        for (String attr : subjectAttributes.keySet()) {
            Element attribute = doc.createElement("attribute");
            attribute.setAttribute("name", attr);
            root.appendChild(attribute);

            for (String value : subjectAttributes.get(attr)) {
                Element v = doc.createElement("value");
                v.appendChild(doc.createTextNode(value));
                attribute.appendChild(v);
            }
        }

        byte[] output = null;
        try {
            output = DataUtils.format(doc).getBytes();
        } catch (Exception e) {
            log.error("Error obtaining user information: " + e.getMessage(), e);
        }

        response.setContentType("text/xml");
        response.setContentLength(output.length);
        OutputStream out = response.getOutputStream();
        out.write(output);
        out.flush();
        out.close();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> getSubjectAttributes(HttpServletRequest request) {
        Map<String, Set<String>> subjectAttr = null;
        subjectAttr =
                (Map<String, Set<String>>) request
                        .getAttribute(FEDORA_ATTRS_KEY);

        if (subjectAttr == null) {
            subjectAttr = new HashMap<String, Set<String>>();
        }

        return subjectAttr;
    }
}
