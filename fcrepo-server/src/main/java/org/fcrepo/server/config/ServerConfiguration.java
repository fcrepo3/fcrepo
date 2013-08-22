/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.fcrepo.common.Constants;
import org.fcrepo.server.utilities.StreamUtility;


/**
 * Fedora server configuration.
 *
 * @author Chris Wilper
 */
public class ServerConfiguration
        extends Configuration
        implements Constants {

			private final String DEPRECATION_WARNING =
			"\n<!--\n\n" +
			"WARNING! ACHTUNG! ATTENZIONE!\n\n" +
			"This configuration file is considered a legacy service and will eventually be deprecated.\n" +
			"The recommended means of configuration is now Fedora's Spring configuration facility, as\n" +
			"documented here:\n\n" +
			"https://wiki.duraspace.org/display/FEDORA36/Spring+Configuration\n\n" +
			"and here:\n\n" +
			"https://wiki.duraspace.org/display/FEDORA36/Spring+Security\n\n" +
			"-->\n\n";		
			
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
		out.println(DEPRECATION_WARNING);
        out.println("<server xmlns=\"" + FCFG.uri + "\" class=\"" + m_className
                + "\">");

        // do server parameters first
        serializeParameters(getParameters(Parameter.class), 2, out);
        // next, modules
        Iterator<ModuleConfiguration> mIter = getModuleConfigurations().iterator();
        while (mIter.hasNext()) {
            ModuleConfiguration mc = mIter.next();
            out.println("  <module role=\"" + mc.getRole() + "\" class=\""
                    + mc.getClassName() + "\">");
            String comment = strip(mc.getComment());
            if (comment != null) {
                out.println("    <comment>" + comment + "</comment>");
            }
            serializeParameters(mc.getParameters(Parameter.class), 4, out);
            out.println("  </module>");
        }
        // finally, datastores
        Iterator<DatastoreConfiguration> dIter = getDatastoreConfigurations().iterator();
        while (dIter.hasNext()) {
            DatastoreConfiguration dc = dIter.next();
            out.println("  <datastore id=\"" + dc.getId() + "\">");
            String comment = strip(dc.getComment());
            if (comment != null) {
                out.println("    <comment>" + comment + "</comment>");
            }
            serializeParameters(dc.getParameters(Parameter.class), 4, out);
            out.println("  </datastore>");
        }

        out.println("</server>");
        out.close();
    }

    private void serializeParameters(Collection<Parameter> params, int indentBy, PrintStream out) {
        Iterator<Parameter> paramIter = params.iterator();
        while (paramIter.hasNext()) {
            getParamXMLString(paramIter.next(),
                                          indentBy, out);
            out.append('\n');
        }
    }

    private void spaces(int num, PrintStream out) {
        for (int i = 0; i < num; i++) {
            out.append(' ');
        }
    }

    private String getParamXMLString(Parameter p, int indentBy, PrintStream out) {
        spaces(indentBy, out);
        out.append("<param name=\"");
        out.append(p.getName());
        out.append("\" value=\"");
        StreamUtility.enc(p.getValue(), out);
        out.append('"');
        if (p.getIsFilePath() != false) {
            out.append(" isFilePath=\"true\"");
        }
        if (p.getProfileValues() != null) {
            Iterator<String> iter = p.getProfileValues().keySet().iterator();
            while (iter.hasNext()) {
                String profileName = iter.next();
                String profileVal =
                        p.getProfileValues().get(profileName);
                out.append(" " + profileName + "value=\"");
                StreamUtility.enc(profileVal, out);
                out.append('"');
            }
        }
        String comment = strip(p.getComment());
        if (comment != null) {
            out.append(">\n");
            spaces(indentBy + 2, out);
            out.append("<comment>");
            StreamUtility.enc(comment, out);
            out.append("</comment>\n");
            spaces(indentBy, out);
            out.append("</param>");
        } else {
            out.append("/>");
        }
        return out.toString();
    }

    // strip leading and trailing whitespace and \n, return null if
    // resulting string is empty in incoming string is null.
    private static String strip(String in) {
        if (in == null) {
            return null;
        }
        String out = in.trim();
        if (out.length() == 0) {
            return null;
        } else {
            return out;
        }
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
