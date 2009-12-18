/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.utility.ingest;

import java.rmi.RemoteException;

import fedora.client.Administrator;

import fedora.common.Constants;

import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;

/**
 * Creates basic object xml for ingest.
 *
 * @author Bill Branan
 */
public class XMLBuilder {

    private FedoraAPIM apim = null;

    public static enum OBJECT_TYPE {
        dataObject, contentModel, serviceDefinition, serviceDeployment
    };

    public XMLBuilder(FedoraAPIM fedoraAPIM) {
        apim = fedoraAPIM;
    }

    public String createObjectXML(OBJECT_TYPE objectType, String pid, String label) throws RemoteException {
        StringBuffer xml = new StringBuffer();
        pid = encodePid(pid);

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<foxml:digitalObject xmlns:xsi=\"" + Constants.XSI.uri + "\"\n");
        xml.append("       xmlns:foxml=\"" + Constants.FOXML.uri + "\"\n");
        xml.append("       xsi:schemaLocation=\"" + Constants.FOXML.uri  + " " + Constants.FOXML1_1.xsdLocation + "\"");
        xml.append("       VERSION=\"1.1\" PID=\"" + pid + "\">\n");
        xml.append("  <foxml:objectProperties>\n");
        xml.append("    <foxml:property NAME=\"" + Constants.MODEL.LABEL.uri + "\" VALUE=\"" + StreamUtility.enc(label) + "\"/>\n");
        xml.append("    <foxml:property NAME=\"" + Constants.MODEL.OWNER.uri + "\" VALUE=\"" + Administrator.getUser() + "\"/>");
        xml.append("  </foxml:objectProperties>\n");

        if(OBJECT_TYPE.contentModel.equals(objectType)) {
            xml.append("  <foxml:datastream ID=\"RELS-EXT\" CONTROL_GROUP=\"X\" STATE=\"A\" VERSIONABLE=\"true\">\n");
            xml.append("    <foxml:datastreamVersion ID=\"RELS-EXT1.0\" MIMETYPE=\"application/rdf+xml\" FORMAT_URI=\"" + Constants.RELS_EXT1_0.uri + "\" LABEL=\"RDF Statements about this object\">\n");
            xml.append("      <foxml:xmlContent>\n");
            xml.append("        <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">\n");
            xml.append("          <rdf:Description rdf:about=\"info:fedora/" + pid + "\">\n");
            xml.append("            <fedora-model:hasModel rdf:resource=\"info:fedora/fedora-system:ContentModel-3.0\" />\n");
            xml.append("          </rdf:Description>\n");
            xml.append("        </rdf:RDF>\n");
            xml.append("      </foxml:xmlContent>\n");
            xml.append("    </foxml:datastreamVersion>\n");
            xml.append("  </foxml:datastream>\n");
            xml.append("  <foxml:datastream ID=\"DS-COMPOSITE-MODEL\" STATE=\"A\" CONTROL_GROUP=\"X\" VERSIONABLE=\"true\">\n");
            xml.append("    <foxml:datastreamVersion ID=\"DS-COMPOSITE-MODEL1.0\" MIMETYPE=\"text/xml\" FORMAT_URI=\"" + Constants.DS_COMPOSITE_MODEL1_0.uri + "\" LABEL=\"Datastream Composite Model\">\n");
            xml.append("      <foxml:xmlContent>\n");
            xml.append("        <dsCompositeModel xmlns=\"info:fedora/fedora-system:def/dsCompositeModel#\">\n");
            xml.append("          <comment xmlns=\"info:fedora/fedora-system:def/comment#\">\n");
            xml.append("            This DS-COMPOSITE-MODEL datastream is included as a starting point to\n");
            xml.append("              assist in the creation of a content model. The DS-COMPOSITE-MODEL\n");
            xml.append("              should define the datastreams that are required for any objects\n");
            xml.append("              conforming to this content model.\n");
            xml.append("            For more information about content models, see:\n");
            xml.append("              http://fedora-commons.org/confluence/x/dgBI.\n");
            xml.append("            For examples of completed content model objects, see the demonstration\n");
            xml.append("              objects included with your Fedora distribution, such as:\n");
            xml.append("              demo:CMImage, demo:UVA_STD_IMAGE, demo:DualResImageCollection,\n");
            xml.append("              demo:TEI_TO_PDFDOC, and demo:XML_TO_HTMLDOC.\n");
            xml.append("            For more information about the demonstration objects, see:\n");
            xml.append("              http://fedora-commons.org/confluence/x/AwFI.\n");
            xml.append("          </comment>\n");
            xml.append("          <dsTypeModel ID=\"DSID\">\n");
            xml.append("            <form MIME=\"text/xml\"/>\n");
            xml.append("          </dsTypeModel>\n");
            xml.append("        </dsCompositeModel>\n");
            xml.append("      </foxml:xmlContent>\n");
            xml.append("    </foxml:datastreamVersion>\n");
            xml.append("  </foxml:datastream>\n");
        }
        else if(OBJECT_TYPE.serviceDefinition.equals(objectType)) {
            xml.append("  <foxml:datastream ID=\"RELS-EXT\" CONTROL_GROUP=\"X\" STATE=\"A\" VERSIONABLE=\"true\">");
            xml.append("    <foxml:datastreamVersion ID=\"RELS-EXT1.0\" MIMETYPE=\"application/rdf+xml\" FORMAT_URI=\"" + Constants.RELS_EXT1_0.uri + "\" LABEL=\"RDF Statements about this object\">");
            xml.append("      <foxml:xmlContent>");
            xml.append("        <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">");
            xml.append("          <rdf:Description rdf:about=\"info:fedora/" + pid + "\">");
            xml.append("            <fedora-model:hasModel rdf:resource=\"info:fedora/fedora-system:ServiceDefinition-3.0\"/>");
            xml.append("          </rdf:Description>");
            xml.append("        </rdf:RDF>");
            xml.append("      </foxml:xmlContent>");
            xml.append("    </foxml:datastreamVersion>");
            xml.append("  </foxml:datastream>");
            xml.append("  <foxml:datastream ID=\"METHODMAP\" CONTROL_GROUP=\"X\" STATE=\"A\" VERSIONABLE=\"true\">");
            xml.append("    <foxml:datastreamVersion ID=\"METHODMAP1.0\" MIMETYPE=\"text/xml\" FORMAT_URI=\"" + Constants.SDEF_METHOD_MAP1_0.uri + "\" LABEL=\"Abstract Method Map\">");
            xml.append("      <foxml:xmlContent>");
            xml.append("        <fmm:MethodMap name=\"Fedora MethodMap for SDef\" xmlns:fmm=\"http://fedora.comm.nsdlib.org/service/methodmap\">");
            xml.append("          <comment xmlns=\"info:fedora/fedora-system:def/comment#\">\n");
            xml.append("            This METHODMAP datastream is included as a starting point to\n");
            xml.append("              assist in the creation of a service definition. The METHODMAP\n");
            xml.append("              should define the methods and method parameters for this\n");
            xml.append("              service definition.\n");
            xml.append("            For more information about service definitions, see:\n");
            xml.append("              http://fedora-commons.org/confluence/x/dgBI.\n");
            xml.append("            For examples of completed service definition objects, see the demonstration\n");
            xml.append("              objects included with your Fedora distribution, such as:\n");
            xml.append("              demo:1, demo:12, demo: 19, and demo:27.\n");
            xml.append("            For more information about the demonstration objects, see:\n");
            xml.append("              http://fedora-commons.org/confluence/x/AwFI.\n");
            xml.append("          </comment>\n");
            xml.append("          <fmm:Method operationName=\"changeme\"/>");
            xml.append("        </fmm:MethodMap>");
            xml.append("      </foxml:xmlContent>");
            xml.append("    </foxml:datastreamVersion>");
            xml.append("  </foxml:datastream>");
        }
        else if(OBJECT_TYPE.serviceDeployment.equals(objectType)) {
            xml.append("  <foxml:datastream ID=\"RELS-EXT\" CONTROL_GROUP=\"X\" STATE=\"A\" VERSIONABLE=\"true\">");
            xml.append("    <foxml:datastreamVersion ID=\"RELS-EXT1.0\" MIMETYPE=\"application/rdf+xml\" FORMAT_URI=\"" + Constants.RELS_EXT1_0.uri + "\" LABEL=\"RDF Statements about this object\">");
            xml.append("    <foxml:xmlContent>");
            xml.append("      <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">");
            xml.append("        <rdf:Description rdf:about=\"info:fedora/" + pid + "\">");
            xml.append("          <fedora-model:hasModel rdf:resource=\"info:fedora/fedora-system:ServiceDeployment-3.0\"/>");
            xml.append("          <fedora-model:isDeploymentOf rdf:resource=\"info:fedora/changeme-to-sDefPid\"/>");
            xml.append("          <fedora-model:isContractorOf rdf:resource=\"info:fedora/changeme-to-cModelPid\"/>");
            xml.append("        </rdf:Description>");
            xml.append("      </rdf:RDF>");
            xml.append("    </foxml:xmlContent>");
            xml.append("    </foxml:datastreamVersion>");
            xml.append("  </foxml:datastream>");
            xml.append("  <foxml:datastream ID=\"METHODMAP\" CONTROL_GROUP=\"X\" STATE=\"A\" VERSIONABLE=\"true\">");
            xml.append("    <foxml:datastreamVersion ID=\"METHODMAP1.0\" MIMETYPE=\"text/xml\" FORMAT_URI=\"" + Constants.SDEP_METHOD_MAP1_1.uri + "\" LABEL=\"Deployment Method Map\">");
            xml.append("      <foxml:xmlContent>");
            xml.append("        <comment xmlns=\"info:fedora/fedora-system:def/comment#\">\n");
            xml.append("          This METHODMAP datastream is included as a starting point to\n");
            xml.append("          assist in the creation of a service deployment. The METHODMAP\n");
            xml.append("          should define the the mapping of the WSDL to Fedora object methods.\n");
            xml.append("        </comment>\n");
            xml.append("      </foxml:xmlContent>");
            xml.append("    </foxml:datastreamVersion>");
            xml.append("  </foxml:datastream>");
            xml.append("  <foxml:datastream ID=\"DSINPUTSPEC\" CONTROL_GROUP=\"X\" STATE=\"A\" VERSIONABLE=\"true\">");
            xml.append("    <foxml:datastreamVersion ID=\"DSINPUTSPEC1.0\" MIMETYPE=\"text/xml\" FORMAT_URI=\"" + Constants.DS_INPUT_SPEC1_1.uri + "\" LABEL=\"Datastream Input Specification\">");
            xml.append("      <foxml:xmlContent>");
            xml.append("        <comment xmlns=\"info:fedora/fedora-system:def/comment#\">\n");
            xml.append("          This DSINPUTSPEC datastream is included as a starting point to\n");
            xml.append("          assist in the creation of a service deployment. The DSINPUTSPEC\n");
            xml.append("          should define the datastreams to be used by WSDL-defined methods.\n");
            xml.append("        </comment>\n");
            xml.append("      </foxml:xmlContent>");
            xml.append("    </foxml:datastreamVersion>");
            xml.append("  </foxml:datastream>");
            xml.append("  <foxml:datastream ID=\"WSDL\" CONTROL_GROUP=\"X\" STATE=\"A\" VERSIONABLE=\"true\">");
            xml.append("    <foxml:datastreamVersion ID=\"WSDL1.0\" MIMETYPE=\"text/xml\" FORMAT_URI=\"" + Constants.WSDL.uri + "\" LABEL=\"WSDL Bindings\">");
            xml.append("      <foxml:xmlContent>");
            xml.append("        <comment xmlns=\"info:fedora/fedora-system:def/comment#\">\n");
            xml.append("          This WSDL datastream is included as a starting point to\n");
            xml.append("            assist in the creation of a service deployment. The WSDL\n");
            xml.append("            should define the services provided by this\n");
            xml.append("            service deployment.\n");
            xml.append("          For more information about service deployments, see:\n");
            xml.append("            http://fedora-commons.org/confluence/x/dgBI.\n");
            xml.append("          For examples of completed service deployment objects, see the demonstration\n");
            xml.append("            objects included with your Fedora distribution, such as:\n");
            xml.append("            demo:2, demo:13, demo:20, and demo:28.\n");
            xml.append("          For more information about the demonstration objects, see:\n");
            xml.append("            http://fedora-commons.org/confluence/x/AwFI.\n");
            xml.append("        </comment>\n");
            xml.append("      </foxml:xmlContent>");
            xml.append("    </foxml:datastreamVersion>");
            xml.append("  </foxml:datastream>");
        }

        xml.append("</foxml:digitalObject>");

        return xml.toString();
    }

    private String encodePid(String pid) throws RemoteException {
        if(pid == null || pid.equals("")) {
            pid = apim.getNextPID(null, null)[0];
        }
        return StreamUtility.enc(pid);
    }
}