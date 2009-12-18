/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import fedora.common.Constants;

/**
 * Fedora server configuration.
 * 
 * @author Chris Wilper
 */
public class ServerConfiguration
        extends Configuration
        implements Constants {

    private String m_className;

    private final List<ModuleConfiguration> m_moduleConfigurations;

    private final List<DatastoreConfiguration> m_datastoreConfigurations;

    public ServerConfiguration(String className,
                               List<Parameter> parameters,
                               List<ModuleConfiguration> moduleConfigurations,
                               List<DatastoreConfiguration> datastoreConfigurations) {
        super(parameters);
        m_className = className;
        m_moduleConfigurations = moduleConfigurations;
        m_datastoreConfigurations = datastoreConfigurations;
    }

    /**
     * Make an exact copy of this ServerConfiguration.
     */
    public ServerConfiguration copy() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serialize(out);
        return new ServerConfigurationParser(new ByteArrayInputStream(out
                .toByteArray())).parse();
    }

    /**
     * Apply the given properties to this ServerConfiguration. Trims leading and
     * trailing spaces from the property values before applying them.
     */
    public void applyProperties(Properties props) {
        Iterator iter = props.keySet().iterator();
        while (iter.hasNext()) {
            String fullName = (String) iter.next();
            String value = props.getProperty(fullName).trim();
            if (fullName.indexOf(":") != -1 && value != null
                    && value.length() > 0) {
                String name = fullName.substring(fullName.lastIndexOf(":") + 1);
                if (fullName.startsWith("server:")) {
                    if (name.endsWith(".class")) {
                        m_className = value;
                    } else {
                        setParameterValue(name, value, true);
                    }
                } else if (fullName.startsWith("module.")) {
                    String role =
                            fullName.substring(7, fullName.lastIndexOf(":"));
                    ModuleConfiguration module = getModuleConfiguration(role);
                    if (module == null) {
                        module =
                                new ModuleConfiguration(new ArrayList<Parameter>(),
                                                        role,
                                                        null,
                                                        null);
                        m_moduleConfigurations.add(module);
                    }
                    if (name.endsWith(".class")) {
                        module.setClassName(value);
                    } else {
                        module.setParameterValue(name, value, true);
                    }
                } else if (fullName.startsWith("datastore.")) {
                    String id =
                            fullName.substring(10, fullName.lastIndexOf(":"));
                    DatastoreConfiguration datastore =
                            getDatastoreConfiguration(id);
                    if (datastore == null) {
                        datastore =
                                new DatastoreConfiguration(new ArrayList<Parameter>(),
                                                           id,
                                                           null);
                        m_datastoreConfigurations.add(datastore);
                    }
                    datastore.setParameterValue(name, value, true);
                }
            }
        }
    }

    public void serialize(OutputStream xmlStream) throws IOException {
        PrintStream out = new PrintStream(xmlStream);
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<server xmlns=\"" + FCFG.uri + "\" class=\"" + m_className
                + "\">");

        // do server parameters first
        serializeParameters(getParameters(), 2, out);
        // next, modules
        Iterator mIter = getModuleConfigurations().iterator();
        while (mIter.hasNext()) {
            ModuleConfiguration mc = (ModuleConfiguration) mIter.next();
            out.println("  <module role=\"" + mc.getRole() + "\" class=\""
                    + mc.getClassName() + "\">");
            String comment = strip(mc.getComment());
            if (comment != null) {
                out.println("    <comment>" + comment + "</comment>");
            }
            serializeParameters(mc.getParameters(), 4, out);
            out.println("  </module>");
        }
        // finally, datastores
        Iterator dIter = getDatastoreConfigurations().iterator();
        while (dIter.hasNext()) {
            DatastoreConfiguration dc = (DatastoreConfiguration) dIter.next();
            out.println("  <datastore id=\"" + dc.getId() + "\">");
            String comment = strip(dc.getComment());
            if (comment != null) {
                out.println("    <comment>" + comment + "</comment>");
            }
            serializeParameters(dc.getParameters(), 4, out);
            out.println("  </datastore>");
        }

        out.println("</server>");
        out.close();
    }

    private void serializeParameters(List params, int indentBy, PrintStream out) {
        Iterator paramIter = params.iterator();
        while (paramIter.hasNext()) {
            out.println(getParamXMLString((Parameter) paramIter.next(),
                                          indentBy));
        }
    }

    private String spaces(int num) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < num; i++) {
            out.append(' ');
        }
        return out.toString();
    }

    private String getParamXMLString(Parameter p, int indentBy) {
        StringBuffer out = new StringBuffer();
        out.append(spaces(indentBy) + "<param name=\"" + p.getName()
                + "\" value=\"" + enc(p.getValue()) + "\"");
        if (p.getIsFilePath() != false) {
            out.append(" isFilePath=\"true\"");
        }
        if (p.getProfileValues() != null) {
            Iterator iter = p.getProfileValues().keySet().iterator();
            while (iter.hasNext()) {
                String profileName = (String) iter.next();
                String profileVal =
                        (String) p.getProfileValues().get(profileName);
                out.append(" " + profileName + "value=\"" + enc(profileVal)
                        + "\"");
            }
        }
        String comment = strip(p.getComment());
        if (comment != null) {
            out.append(">\n" + spaces(indentBy + 2) + "<comment>"
                    + enc(comment) + "</comment>\n" + spaces(indentBy)
                    + "</param>");
        } else {
            out.append("/>");
        }
        return out.toString();
    }

    private String enc(String in) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '\'') {
                out.append("&apos;");
            } else if (c == '\"') {
                out.append("&quot;");
            } else if (c == '&') {
                out.append("&amp;");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // strip leading and trailing whitespace and \n, return null if
    // resulting string is empty in incoming string is null.
    private String strip(String in) {
        if (in == null) {
            return null;
        }
        String out = stripTrailing(stripLeading(in));
        if (out.length() == 0) {
            return null;
        } else {
            return out;
        }
    }

    private static String stripLeading(String in) {
        StringBuffer out = new StringBuffer();
        boolean foundNonWhitespace = false;
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (foundNonWhitespace) {
                out.append(c);
            } else {
                if (c != ' ' && c != '\t' && c != '\n') {
                    foundNonWhitespace = true;
                    out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static String stripTrailing(String in) {
        StringBuffer out = new StringBuffer();
        boolean foundNonWhitespace = false;
        for (int i = in.length() - 1; i >= 0; i--) {
            char c = in.charAt(i);
            if (foundNonWhitespace) {
                out.insert(0, c);
            } else {
                if (c != ' ' && c != '\t' && c != '\n') {
                    foundNonWhitespace = true;
                    out.insert(0, c);
                }
            }
        }
        return out.toString();
    }

    public String getClassName() {
        return m_className;
    }

    public List<ModuleConfiguration> getModuleConfigurations() {
        return m_moduleConfigurations;
    }

    public ModuleConfiguration getModuleConfiguration(String role) {
        for (int i = 0; i < m_moduleConfigurations.size(); i++) {
            ModuleConfiguration config = m_moduleConfigurations.get(i);
            if (config.getRole().equals(role)) {
                return config;
            }
        }
        return null;
    }

    public List<DatastoreConfiguration> getDatastoreConfigurations() {
        return m_datastoreConfigurations;
    }

    public DatastoreConfiguration getDatastoreConfiguration(String id) {
        for (int i = 0; i < m_datastoreConfigurations.size(); i++) {
            DatastoreConfiguration config = m_datastoreConfigurations.get(i);
            if (config.getId().equals(id)) {
                return config;
            }
        }
        return null;
    }

    /**
     * Deserialize, then output the given configuration. If two parameters are
     * given, the first one is the filename and the second is the properties
     * file to apply before re-serializing.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IOException("One or two arguments expected.");
        }
        ServerConfiguration config =
                new ServerConfigurationParser(new FileInputStream(new File(args[0])))
                        .parse();
        if (args.length == 2) {
            Properties props = new Properties();
            props.load(new FileInputStream(new File(args[1])));
            config.applyProperties(props);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        config.serialize(out);
        String content = new String(out.toByteArray(), "UTF-8");
        System.out.println(content);
    }

}
