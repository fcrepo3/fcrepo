/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.io.ByteArrayInputStream;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import fedora.server.MultiValueMap;
import fedora.server.journal.JournalException;
import fedora.server.journal.entry.CreatorJournalEntry;
import fedora.server.journal.entry.JournalEntryContext;

/**
 * <p>
 * <b>Title:</b> SampleJournalEntries.java
 * </p>
 * <p>
 * <b>Description:</b> Some sample CreatorJournalEntry objects for use in unit
 * tests.
 * </p>
 * <p>
 * KLUGE: add more sample entries from these files:
 * C:\FedoraArchiveFiles\fedoraJournal20070130.154626.282Z,
 * C:\FedoraArchiveFiles\fedoraJournal20070130.154628.282Z,
 * C:\FedoraArchiveFiles\fedoraJournal20070130.154627.892Z
 * </p>
 *
 * @author jblake
 * @version $Id: SampleJournalEntries.java,v 1.3 2007/06/01 17:21:32 jblake Exp $
 */
public class SampleJournalEntries {

    public static final CreatorJournalEntry ENTRY_1;

    public static final CreatorJournalEntry ENTRY_1A;

    public static final CreatorJournalEntry ENTRY_2;

    public static final CreatorJournalEntry ENTRY_3;

    public static final List<CreatorJournalEntry> ALL_ENTRIES;

    private static final String[][] EMPTY = new String[0][2];

    private static final Date DATE_1 = createDate("2007-02-18T08:55:07.951Z");

    private static final String CONTENT_1 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<nsdl_dc:nsdl_dc schemaVersion=\"1.02.000\"\n"
                    + "  xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                    + "  xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                    + "  xmlns:nsdl_dc=\"http://ns.nsdl.org/nsdl_dc_v1.02/\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://ns.nsdl.org/nsdl_dc_v1.02/ http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd\">\n"
                    + "  <dc:identifier xsi:type=\"dct:URI\">http://serc.carleton.edu/introgeo/roleplaying/examples/globwarm.html</dc:identifier>\n"
                    + "  <dc:type>Calculation or Conversion Tool</dc:type>\n"
                    + "  <dc:type>Scientific Visualization</dc:type>\n"
                    + "  <dc:type xsi:type=\"dct:DCMIType\">Image</dc:type>\n"
                    + "  <dc:type xsi:type=\"dct:DCMIType\">InteractiveResource</dc:type>\n"
                    + "  <dc:type xsi:type=\"dct:DCMIType\">Software</dc:type>\n"
                    + "  <dc:type xsi:type=\"dct:DCMIType\">Text</dc:type>\n"
                    + "  <dc:type>Lesson</dc:type>\n"
                    + "  <dc:type>Course</dc:type>\n"
                    + "  <dc:type>Project</dc:type>\n"
                    + "  <dc:type>Reference</dc:type>\n"
                    + "  <dc:type>Illustration</dc:type>\n"
                    + "  <dc:type>Map</dc:type>\n"
                    + "  <dc:type>Remotely Sensed Imagery</dc:type>\n"
                    + "  <dc:type>Simulation</dc:type>\n"
                    + "  <dc:format xsi:type=\"dct:IMT\">text/html</dc:format>\n"
                    + "  <dc:format>Adobe Acrobat reader</dc:format>\n"
                    + "  <dc:format xsi:type=\"dct:IMT\">video/quicktime</dc:format>\n"
                    + "  <dc:format>Microsoft Windows</dc:format>\n"
                    + "  <dc:format>Macintosh</dc:format>\n"
                    + "  <dc:language xsi:type=\"dct:RFC3066\">en</dc:language>\n"
                    + "  <dc:date xsi:type=\"dct:W3CDTF\">2001-01-01</dc:date>\n"
                    + "  <dc:title>The World Watcher Project: The Global Warming Project</dc:title>\n"
                    + "  <dc:subject>Climatology</dc:subject>\n"
                    + "  <dc:subject>Environmental science</dc:subject>\n"
                    + "  <dc:subject>Cryology</dc:subject>\n"
                    + "  <dc:subject>Physical geography</dc:subject>\n"
                    + "  <dc:subject>Atmospheric science</dc:subject>\n"
                    + "  <dc:subject>Remote Sensing</dc:subject>\n"
                    + "  <dc:subject>Systems</dc:subject>\n"
                    + "  <dc:subject>Heat &amp; Energy</dc:subject>\n"
                    + "  <dc:subject>Climate Change</dc:subject>\n"
                    + "  <dc:subject>Scientific Visualization</dc:subject>\n"
                    + "  <dc:subject>image processing</dc:subject>\n"
                    + "  <dc:subject>Atmosphere</dc:subject>\n"
                    + "  <dc:subject>Global Climate Systems</dc:subject>\n"
                    + "  <dc:subject>Environmental Science</dc:subject>\n"
                    + "  <dc:subject>Human geography</dc:subject>\n"
                    + "  <dc:subject>Topography/Physical Geography</dc:subject>\n"
                    + "  <dc:subject>Policy issues</dc:subject>\n"
                    + "  <dc:subject>Composition and Chemistry</dc:subject>\n"
                    + "  <dc:subject>Atmospheric Dynamics</dc:subject>\n"
                    + "  <dc:subject>Global Warming</dc:subject>\n"
                    + "  <dc:subject>Weather</dc:subject>\n"
                    + "  <dc:subject>Anthropogenic Activity</dc:subject>\n"
                    + "  <dc:subject>Systems, Interactions, Feedback Loops</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Science</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Earth science</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Physical sciences</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Meteorology</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Geology</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Geography</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Chemistry</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Physics</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Astronomy</dc:subject>\n"
                    + "  <dc:subject xsi:type=\"nsdl_dc:GEM\">Space sciences</dc:subject>\n"
                    + "  <dc:description>Global warming and its potential impact provide the context for this unit, in which students learn about the scientific factors contributing to the debate. Students act as advisors to the heads of state of several nations and explore the issues as they respond to the various questions and concerns of these leaders. Activities include a combination of physical labs and investigations using World Watcher software, a geographic data visualization tool developed by Northwestern University.</dc:description>\n"
                    + "  <dc:rights>This product is free and clear for general use.</dc:rights>\n"
                    + "  <dct:educationLevel xsi:type=\"nsdl_dc:NSDLEdLevel\">Middle School</dct:educationLevel>\n"
                    + "  <dct:educationLevel xsi:type=\"nsdl_dc:NSDLEdLevel\">High School</dct:educationLevel>\n"
                    + "</nsdl_dc:nsdl_dc>\n" + "\n";

    private static final String[][] ENVIRONMENT_1 =
            new String[][] {
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:authType",
                            "BASIC"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:security",
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:security-secure"},
                    {"urn:fedora:names:fedora:2.1:environment:currentDate",
                            "2007-02-18Z"},
                    {"urn:fedora:names:fedora:2.1:environment:currentTime",
                            "08:55:07.951Z"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:sessionStatus",
                            "invalid"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:scheme",
                            "https"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:clientIpAddress",
                            "127.0.0.1"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:contentLength",
                            "6730"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:clientFqdn",
                            "repo5.nsdl.org"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverIpAddress",
                            "127.0.0.1"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverPort",
                            "8843"},
                    {"urn:fedora:names:fedora:2.1:environment:currentDateTime",
                            "2007-02-18T08:55:07.951Z"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol",
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol-soap"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:contentType",
                            "text/xml; charset=utf-8"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:method",
                            "POST"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:protocol",
                            "HTTP/1.0"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverFqdn",
                            "repo5.nsdl.org"}};

    private static final String[][] SUBJECT_1 =
            new String[][] {
                    {"fedoraRole", "administrator"},
                    {"urn:fedora:names:fedora:2.1:subject:loginId",
                            "fedoraAdmin"}};

    private static final String[][] RESOURCE_1 =
            new String[][] {
                    {
                            "urn:fedora:names:fedora:2.1:resource:datastream:newState",
                            "A"},
                    {
                            "urn:fedora:names:fedora:2.1:resource:datastream:newFormatUri",
                            "unknown"},
                    {
                            "urn:fedora:names:fedora:2.1:resource:datastream:newMimeType",
                            "application/xml"},
                    {"urn:fedora:names:fedora:2.1:resource:datastream:id",
                            "format_nsdl_dc"}};

    private static final String CONTENT_2 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<!-- edited with XMLSPY v5 rel. 2 U (http://www.xmlspy.com) by Elly Cramer (Cornell University) -->\n"
                    + "<foxml:digitalObject PID=\"hdl:2200%2F20070216150029939T\"\n"
                    + "  xmlns:audit=\"info:fedora/fedora-system:def/audit#\"\n"
                    + "  xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-0.xsd\">\n"
                    + "  <foxml:objectProperties>\n"
                    + "    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"Active\"/>\n"
                    + "    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"MetadataProvider\"/>\n"
                    + "    <foxml:property\n"
                    + "      NAME=\"info:fedora/fedora-system:def/model#contentModel\" VALUE=\"nsdl:MetadataProvider\"/>\n"
                    + "  </foxml:objectProperties>\n"
                    + "  <foxml:datastream CONTROL_GROUP=\"X\" ID=\"DC\" STATE=\"A\" VERSIONABLE=\"true\">\n"
                    + "    <foxml:datastreamVersion ID=\"DC1.0\" LABEL=\"Dublin Core Metadata\"\n"
                    + "      MIMETYPE=\"text/xml\" SIZE=\"235\">\n"
                    + "      <foxml:xmlContent>\n"
                    + "        <oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">\n"
                    + "          <dc:title>NSDL Metadata Provider</dc:title>\n"
                    + "          <dc:identifier>hdl:2200%2F20070216150029939T</dc:identifier>\n"
                    + "        </oai_dc:dc>\n"
                    + "      </foxml:xmlContent>\n"
                    + "    </foxml:datastreamVersion>\n"
                    + "  </foxml:datastream>\n"
                    + "  <foxml:datastream CONTROL_GROUP=\"X\" ID=\"RELS-EXT\" STATE=\"A\">\n"
                    + "    <foxml:datastreamVersion ID=\"RELS-EXT.0\"\n"
                    + "      LABEL=\"Relationships to other objects\" MIMETYPE=\"application/rdf+xml\">\n"
                    + "      <foxml:xmlContent>\n"
                    + "        <rdf:RDF xmlns:auth=\"http://ns.nsdl.org/ndr/auth#\"\n"
                    + "          xmlns:crs=\"http://ns.nsdl.org/ndr/collections#\"\n"
                    + "          xmlns:nsdl=\"http://ns.nsdl.org/api/relationships#\"\n"
                    + "          xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/3001/01/rdf-schema#\">\n"
                    + "          <rdf:Description rdf:about=\"info:fedora/hdl:2200%2F20070216150029939T\">\n"
                    + "            <nsdl:objectType>MetadataProvider</nsdl:objectType>\n"
                    + "            <nsdl:hasHandle>2200/20070216150029939T</nsdl:hasHandle>\n"
                    + "            <nsdl:aggregatedBy rdf:resource=\"info:fedora/hdl:2200%2F20070202120016034T\"/>\n"
                    + "            <nsdl:metadataProviderFor rdf:resource=\"info:fedora/hdl:2200%2F20061002131218276T\"/>\n"
                    + "            <auth:authorizedToChange rdf:resource=\"info:fedora/nsdl:1004\"/>\n"
                    + "            <crs:collectionNA>2802851</crs:collectionNA>\n"
                    + "            <nsdl:setSpec>2802851</nsdl:setSpec>\n"
                    + "            <nsdl:setName>The Teaching Company: Science and Mathematics Courses</nsdl:setName>\n"
                    + "          </rdf:Description>\n"
                    + "        </rdf:RDF>\n"
                    + "      </foxml:xmlContent>\n"
                    + "    </foxml:datastreamVersion>\n"
                    + "  </foxml:datastream>\n"
                    + "  <foxml:datastream CONTROL_GROUP=\"M\" ID=\"serviceDescription\" STATE=\"A\"\n"
                    + "    VERSIONABLE=\"true\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\">\n"
                    + "    <foxml:datastreamVersion ID=\"serviceDescription.0\"\n"
                    + "      LABEL=\"serviceDescription data stream\" MIMETYPE=\"application/xml\">\n"
                    + "      <foxml:xmlContent>\n"
                    + "        <serviceDescription xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                    + "          xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                    + "          xmlns:ieee=\"http://purl.org/ieee/phony_namespace\" xmlns:nsdl_dc=\"http://ns.nsdl.org/nsdl_dc_v1.02/\">\n"
                    + "          <dc:title>The Teaching Company: Science and Mathematics Courses OAI Service</dc:title>\n"
                    + "          <dc:description>Provides The Teaching Company: Science and Mathematics Courses records</dc:description>\n"
                    + "          <dc:type>MetadataProvider</dc:type>\n"
                    + "          <image>\n"
                    + "            <brandURL>http://crs.nsdl.org/brands/2802851.jpg</brandURL>\n"
                    + "            <title>The Teaching Company: Science and Mathematics Courses</title>\n"
                    + "            <width>100</width>\n"
                    + "            <height>30</height>\n"
                    + "          </image>\n"
                    + "          <contacts>\n"
                    + "            <contact>\n"
                    + "              <name>none specified</name>\n"
                    + "              <email>ml1047@columbia.edu</email>\n"
                    + "              <info>OAI Admin</info>\n"
                    + "            </contact>\n"
                    + "          </contacts>\n"
                    + "        </serviceDescription>\n"
                    + "      </foxml:xmlContent>\n"
                    + "    </foxml:datastreamVersion>\n"
                    + "  </foxml:datastream>\n"
                    + "  <foxml:datastream CONTROL_GROUP=\"M\" ID=\"harvestInfo\" STATE=\"A\"\n"
                    + "    VERSIONABLE=\"true\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\">\n"
                    + "    <foxml:datastreamVersion ID=\"harvestInfo.0\"\n"
                    + "      LABEL=\"harvestInfo data stream\" MIMETYPE=\"application/xml\">\n"
                    + "      <foxml:xmlContent>\n"
                    + "        <harvestInfo xmlns=\"http://ns.nsdl.org/MRingest/harvest_v1.00/\"\n"
                    + "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://ns.nsdl.org/MRingest/harvest_v1.00/ http://ns.nsdl.org/schemas/ndr/ndr_ingest.xsd\">\n"
                    + "          <harvestRequest schemaVersion=\"1.00.000\"\n"
                    + "            xmlns=\"http://ns.nsdl.org/MRingest/harvest_v1.00/\"\n"
                    + "            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://ns.nsdl.org/MRingest/harvest_v1.00/ http://ns.nsdl.org/schemas/MRingest/harvest_v1.00.xsd\">\n"
                    + "            <baseURL>http://grackle.cc.columbia.edu/cwis/SPT--OAI.php</baseURL>\n"
                    + "            <collectionNA>2802851</collectionNA>\n"
                    + "            <runType>harvest</runType>\n"
                    + "            <providerEmail>mr-ingest@nsdl.org</providerEmail>\n"
                    + "            <requestedChecks/>\n"
                    + "            <fromDate>2007-02-08</fromDate>\n"
                    + "            <sets>\n"
                    + "              <set>Publisher:Scientific_American</set>\n"
                    + "              <set>Publisher:Cambridge_University_Press_UK</set>\n"
                    + "              <set>Publisher:Cambridge_University_Press_US</set>\n"
                    + "              <set>Publisher:Columbia_University_Press</set>\n"
                    + "              <set>Publisher:American_Mathematical_Society</set>\n"
                    + "              <set>Publisher:Tool_Factory_Inc.</set>\n"
                    + "              <set>Publisher:John_Wiley_and_Sons</set>\n"
                    + "              <set>Publisher:Houghton_Mifflin_Science_Education_Place</set>\n"
                    + "              <set>Publisher:Prentice_Hall</set>\n"
                    + "              <set>Publisher:The_Apprentice_Corporation</set>\n"
                    + "              <set>Publisher:test</set>\n"
                    + "              <set>Publisher:Elsevier_Science_and_Technology</set>\n"
                    + "              <set>Publisher:Elsevier_Science_and_Technology</set>\n"
                    + "              <set>Publisher:Elsevier Science and Technology</set>\n"
                    + "              <set>Publisher:McDougal_Littell</set>\n"
                    + "              <set>Publisher:McDougal_Littel</set>\n"
                    + "              <set>Publisher:The_Teaching_Company</set>\n"
                    + "              <set>Publisher:The_Apprentice_Corporation_Non_Profit</set>\n"
                    + "              <set>Publisher:American_Ed</set>\n"
                    + "              <set>Publisher:Aimstar</set>\n"
                    + "              <set>Publisher:Autodesk</set>\n"
                    + "              <set>Publisher:Riverdeep_The_Learning_Company</set>\n"
                    + "              <set>Publisher:Sunburst</set>\n"
                    + "              <set>Publisher:LCSI</set>\n"
                    + "              <set>Publisher:Lynda.com</set>\n"
                    + "              <set>Publisher:Clearvue</set>\n"
                    + "              <set>Publisher:APTE</set>\n"
                    + "              <set>Publisher:Homeworkhelp.com</set>\n"
                    + "              <set>Publisher:Atomic_Learning</set>\n"
                    + "              <set>Publisher:ATEEC</set>\n"
                    + "              <set>Publisher:Avid</set>\n"
                    + "              <set>Publisher:Barnum_Software</set>\n"
                    + "              <set>Publisher:Learning_Company</set>\n"
                    + "              <set>Publisher:PCI_Educational_Publishing</set>\n"
                    + "              <set>Publisher:ADAM_Software</set>\n"
                    + "              <set>Publisher:Bagatrix</set>\n"
                    + "              <set>Publisher:Block_Publishing</set>\n"
                    + "              <set>Publisher:Cabrilog</set>\n"
                    + "              <set>Publisher:CyberEd</set>\n"
                    + "              <set>Publisher:Centron</set>\n"
                    + "              <set>Publisher:Chariot_Software</set>\n"
                    + "              <set>Publisher:Canela_Software</set>\n"
                    + "              <set>Publisher:Cord_Communications</set>\n"
                    + "              <set>Publisher:Core_Learning</set>\n"
                    + "              <set>Publisher:MathRealm</set>\n"
                    + "              <set>Publisher:Course_Technology</set>\n"
                    + "              <set>Publisher:Chemware</set>\n"
                    + "              <set>Publisher:Knowledge_Adventure</set>\n"
                    + "              <set>Publisher:Discovery_Education</set>\n"
                    + "              <set>Publisher:DeMarque,_Inc</set>\n"
                    + "              <set>Publisher:Ingenuity_Works</set>\n"
                    + "              <set>Publisher:Gamco</set>\n"
                    + "              <set>Publisher:Encore_Software</set>\n"
                    + "              <set>Publisher:Essential_Skills</set>\n"
                    + "              <set>Publisher:Emanuel_Software</set>\n"
                    + "              <set>Publisher:EOA_Scientific</set>\n"
                    + "              <set>Publisher:Edusoft</set>\n"
                    + "              <set>Publisher:Edu2000_America</set>\n"
                    + "              <set>Publisher:EdVenture</set>\n"
                    + "              <set>Publisher:Dorling_Kindersley_by_GSP</set>\n"
                    + "              <set>Publisher:Facts_on_File</set>\n"
                    + "              <set>Publisher:Focus_Educational_Software_Ltd</set>\n"
                    + "              <set>Publisher:FSCreations,_Inc.</set>\n"
                    + "              <set>Publisher:FTC_Publishing</set>\n"
                    + "              <set>Publisher:Scholastic_Software</set>\n"
                    + "              <set>Publisher:GollyGee_Software</set>\n"
                    + "              <set>Publisher:Gallopade_International</set>\n"
                    + "              <set>Publisher:Hotmath.com</set>\n"
                    + "              <set>Publisher:Hopkins_Technology</set>\n"
                    + "              <set>Publisher:Heartsoft</set>\n"
                    + "              <set>Publisher:Cambridgesoft</set>\n"
                    + "              <set>Publisher:Interactive_Learning</set>\n"
                    + "              <set>Publisher:Ideas_Learning</set>\n"
                    + "              <set>Publisher:AIMS Multimedia</set>\n"
                    + "              <set>Publisher:Queue</set>\n"
                    + "              <set>Publisher:SYSTAT</set>\n"
                    + "              <set>Publisher:Jaguar_Educational</set>\n"
                    + "              <set>Publisher:Inspiration_Software</set>\n"
                    + "              <set>Publisher:Academic_Hallmarks</set>\n"
                    + "              <set>Publisher:Kognito</set>\n"
                    + "              <set>Publisher:Kutoka</set>\n"
                    + "              <set>Publisher:Learning_Zone_Express</set>\n"
                    + "              <set>Publisher:Learning_Multi-Systems</set>\n"
                    + "              <set>Publisher:Learning_Team</set>\n"
                    + "              <set>Publisher:Maestro_Learning</set>\n"
                    + "              <set>Publisher:MCH_Multimedia</set>\n"
                    + "              <set>Publisher:MegaSystems</set>\n"
                    + "              <set>Publisher:Micrograms</set>\n"
                    + "              <set>Publisher:Milliken_Software</set>\n"
                    + "              <set>Publisher:Adobe_Press</set>\n"
                    + "              <set>Publisher:SVE</set>\n"
                    + "              <set>Publisher:MathResources</set>\n"
                    + "              <set>Publisher:Microsoft</set>\n"
                    + "              <set>Publisher:Multimedia_Science</set>\n"
                    + "              <set>Publisher:Sleek_Software</set>\n"
                    + "              <set>Publisher:Attainment_Company</set>\n"
                    + "              <set>Publisher:Tom_Snyder_Productions</set>\n"
                    + "              <set>Publisher:Ventura</set>\n"
                    + "              <set>Publisher:Optimum_Resource</set>\n"
                    + "              <set>Publisher:MathSoft</set>\n"
                    + "              <set>Publisher:Critical_Thinking</set>\n"
                    + "              <set>Publisher:Nordic_Software</set>\n"
                    + "              <set>Publisher:NECTAR_Foundation</set>\n"
                    + "              <set>Publisher:NeoSci</set>\n"
                    + "              <set>Publisher:2Simple_Software</set>\n"
                    + "              <set>Publisher:Pintar_Learning</set>\n"
                    + "              <set>Publisher:Maplesoft</set>\n"
                    + "              <set>Publisher:4:20_Communications</set>\n"
                    + "              <set>Publisher:Broderbund</set>\n"
                    + "              <set>Publisher:QA-Kids</set>\n"
                    + "              <set>Publisher:REMedia</set>\n"
                    + "              <set>Publisher:ScienceWorks</set>\n"
                    + "              <set>Publisher:Seeds_Software</set>\n"
                    + "              <set>Publisher:sciPROOF</set>\n"
                    + "              <set>Publisher:Sing_'n'_Learn_Software</set>\n"
                    + "              <set>Publisher:SPSS</set>\n"
                    + "              <set>Publisher:Imaginova</set>\n"
                    + "              <set>Publisher:School_Zone</set>\n"
                    + "              <set>Publisher:Two-Can_Publishing</set>\n"
                    + "              <set>Publisher:Tool_Factory</set>\n"
                    + "              <set>Publisher:Bright_Science</set>\n"
                    + "              <set>Publisher:Digital_Frog</set>\n"
                    + "              <set>Publisher:SuperSchool_Software</set>\n"
                    + "              <set>Publisher:Vernier</set>\n"
                    + "              <set>Publisher:Visions_Technology</set>\n"
                    + "              <set>Publisher:Wildridge_Software</set>\n"
                    + "              <set>Publisher:Pre-Engineering_Software</set>\n"
                    + "              <set>Publisher:Brighter_Minds</set>\n"
                    + "              <set>Publisher:Texas_Instruments</set>\n"
                    + "              <set>Publisher:Wolfram_Research</set>\n"
                    + "            </sets>\n" + "            <formats>\n"
                    + "              <format>nsdl_dc</format>\n"
                    + "            </formats>\n"
                    + "            <firstHarvest>true</firstHarvest>\n"
                    + "            <uuid/>\n" + "          </harvestRequest>\n"
                    + "        </harvestInfo>\n"
                    + "      </foxml:xmlContent>\n"
                    + "    </foxml:datastreamVersion>\n"
                    + "  </foxml:datastream>\n" + "</foxml:digitalObject>\n"
                    + "\n";

    private static final String[][] ENVIRONMENT_2 =
            new String[][] {
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:authType",
                            "BASIC"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:security",
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:security-secure"},
                    {"urn:fedora:names:fedora:2.1:environment:currentDate",
                            "2007-02-16Z"},
                    {"urn:fedora:names:fedora:2.1:environment:currentTime",
                            "20:00:31.139Z"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:sessionStatus",
                            "invalid"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:scheme",
                            "https"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:clientIpAddress",
                            "127.0.0.1"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:contentLength",
                            "16904"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:clientFqdn",
                            "repo5.nsdl.org"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverIpAddress",
                            "127.0.0.1"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverPort",
                            "8843"},
                    {"urn:fedora:names:fedora:2.1:environment:currentDateTime",
                            "2007-02-16T20:00:31.139Z"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol",
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol-soap"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:contentType",
                            "text/xml; charset=utf-8"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:method",
                            "POST"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:protocol",
                            "HTTP/1.0"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverFqdn",
                            "repo5.nsdl.org"}};

    private static final String[][] SUBJECT_2 =
            new String[][] {
                    {"fedoraRole", "administrator"},
                    {"urn:fedora:names:fedora:2.1:subject:loginId",
                            "fedoraAdmin"}};

    private static final String[][] RESOURCE_2 =
            new String[][] {
                    {"urn:fedora:names:fedora:2.1:resource:object:encoding",
                            "UTF-8"},
                    {"urn:fedora:names:fedora:2.1:resource:object:formatUri",
                            "foxml1.0"}};

    private static final String[][] RECOVERY_2 =
            new String[][] {{"info:fedora/fedora-system:def/recovery#pid",
                    "hdl:2200%2F20070216150029939T"}};

    private static final String[][] ENVIRONMENT_3 =
            new String[][] {
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:authType",
                            "BASIC"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:security",
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:security-insecure"},
                    {"urn:fedora:names:fedora:2.1:environment:currentDate",
                            "2007-01-30Z"},
                    {"urn:fedora:names:fedora:2.1:environment:currentTime",
                            "15:46:36.470Z"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:sessionStatus",
                            "invalid"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:scheme",
                            "http"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:clientIpAddress",
                            "127.0.0.1"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:contentLength",
                            "1806"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:clientFqdn",
                            "localhost"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverIpAddress",
                            "127.0.0.1"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverPort",
                            "8080"},
                    {"urn:fedora:names:fedora:2.1:environment:currentDateTime",
                            "2007-01-30T15:46:36.470Z"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol",
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol-soap"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:contentType",
                            "text/xml; charset=utf-8"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:method",
                            "POST"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:protocol",
                            "HTTP/1.1"},
                    {
                            "urn:fedora:names:fedora:2.1:environment:httpRequest:serverFqdn",
                            "localhost"}};

    private static final String[][] SUBJECT_3 =
            new String[][] {
                    {"fedoraRole", "administrator"},
                    {"urn:fedora:names:fedora:2.1:subject:loginId",
                            "fedoraAdmin"}};

    private static final String[][] RESOURCE_3 =
            new String[][] {
                    {
                            "urn:fedora:names:fedora:2.1:resource:datastream:newState",
                            "A"},
                    {
                            "urn:fedora:names:fedora:2.1:resource:datastream:newFormatUri",
                            "\n"},
                    {
                            "urn:fedora:names:fedora:2.1:resource:datastream:newMimeType",
                            "text/html"},
                    {
                            "urn:fedora:names:fedora:2.1:resource:datastream:newLocation",
                            "http://myserver.edu/mydir/aDifferentFile.html"},
                    {"urn:fedora:names:fedora:2.1:resource:datastream:id",
                            "DS1"}};

    /**
     * Make sure that the other constants have been initialized before this
     * static block appears.
     */
    static {
        ENTRY_1 = createSampleEntry1();
        ENTRY_1A = createSampleEntry1a();
        ENTRY_2 = createSampleEntry2();
        ENTRY_3 = createSampleEntry3();
        ALL_ENTRIES =
                Collections.unmodifiableList(Arrays
                        .asList(new CreatorJournalEntry[] {ENTRY_1, ENTRY_1A,
                                ENTRY_2, ENTRY_3}));
    }

    private static CreatorJournalEntry createSampleEntry1() {
        try {
            JournalEntryContext context =
                    buildContext("somePassword",
                                 false,
                                 DATE_1,
                                 ENVIRONMENT_1,
                                 SUBJECT_1,
                                 EMPTY,
                                 RESOURCE_1,
                                 EMPTY);

            CreatorJournalEntry entry =
                    new CreatorJournalEntry("modifyDatastreamByValue", context);

            entry.addArgument("pid", "hdl:2200%2F20061003155524381T");
            entry.addArgument("dsId", "format_nsdl_dc");
            entry.addArgument("altIds", new String[0]);
            entry.addArgument("dsLabel", "format_nsdl_dc data stream");
            entry.addArgument("versionable", true);
            entry.addArgument("mimeType", "application/xml");
            entry.addArgument("formatUri", "unknown");
            entry.addArgument("dsContent", buildInputStream(CONTENT_1));
            entry.addArgument("dsState", "A");
            entry.addArgument("message", "Modified by NSDL API");
            entry.addArgument("force", true);
            return entry;
        } catch (JournalException e) {
            return null;
        }
    }

    /**
     * Try entry 1 again, with null input stream.
     */
    private static CreatorJournalEntry createSampleEntry1a() {
        try {
            JournalEntryContext context =
                    buildContext("somePassword",
                                 false,
                                 new Date(),
                                 ENVIRONMENT_1,
                                 SUBJECT_1,
                                 EMPTY,
                                 RESOURCE_1,
                                 EMPTY);

            CreatorJournalEntry entry =
                    new CreatorJournalEntry("modifyDatastreamByValue", context);

            entry.addArgument("pid", "hdl:2200%2F20061003155524381T");
            entry.addArgument("dsId", "format_nsdl_dc");
            entry.addArgument("altIds", new String[0]);
            entry.addArgument("dsLabel", "format_nsdl_dc data stream");
            entry.addArgument("versionable", true);
            entry.addArgument("mimeType", "application/xml");
            entry.addArgument("formatUri", "unknown");
            entry.addArgument("dsContent", null);
            entry.addArgument("dsState", "A");
            entry.addArgument("message", "Modified by NSDL API");
            entry.addArgument("force", true);
            return entry;
        } catch (JournalException e) {
            return null;
        }
    }

    private static CreatorJournalEntry createSampleEntry2() {
        try {
            JournalEntryContext context =
                    buildContext("bogusStuff",
                                 false,
                                 new Date(),
                                 ENVIRONMENT_2,
                                 SUBJECT_2,
                                 EMPTY,
                                 RESOURCE_2,
                                 RECOVERY_2);

            CreatorJournalEntry entry =
                    new CreatorJournalEntry("ingest", context);
            entry.addArgument("serialization", buildInputStream(CONTENT_2));
            entry.addArgument("message", "Metadata provider added by NSDL API");
            entry.addArgument("format", "foxml1.0");
            entry.addArgument("encoding", "UTF-8");
            entry.addArgument("newPid", true);

            return entry;
        } catch (JournalException e) {
            return null;
        }
    }

    private static CreatorJournalEntry createSampleEntry3() {
        JournalEntryContext context =
                buildContext("mySecretWord",
                             false,
                             new Date(),
                             ENVIRONMENT_3,
                             SUBJECT_3,
                             EMPTY,
                             RESOURCE_3,
                             EMPTY);

        CreatorJournalEntry entry =
                new CreatorJournalEntry("modifyDatastreamByReference", context);
        entry.addArgument("pid", "demo:19");
        entry.addArgument("dsId", "DS1");
        entry.addArgument("altIds", new String[] {"this", "that", "another"});
        entry.addArgument("dsLabel", "A different source and some AltIDs");
        entry.addArgument("versionable", true);
        entry.addArgument("mimeType", "text/html");
        entry.addArgument("formatUri", "\n");
        entry.addArgument("dsLocation",
                          "http://myserver.edu/mydir/aDifferentFile.html");
        entry.addArgument("dsState", "A");
        entry.addArgument("message", "Modify by Reference with AltIDs");
        entry.addArgument("force", false);

        return entry;
    }

    private static JournalEntryContext buildContext(String password,
                                                    boolean noop,
                                                    Date now,
                                                    String[][] environment,
                                                    String[][] subject,
                                                    String[][] action,
                                                    String[][] resource,
                                                    String[][] recovery) {
        JournalEntryContext context = new JournalEntryContext();
        context.setPassword(password);
        context.setNoOp(noop);
        context.setNow(now);
        context.setEnvironmentAttributes(buildMultiMap(environment));
        context.setSubjectAttributes(buildMultiMap(subject));
        context.setActionAttributes(buildMultiMap(action));
        context.setResourceAttributes(buildMultiMap(resource));
        context.setRecoveryAttributes(buildMultiMap(recovery));
        return context;
    }

    private static ByteArrayInputStream buildInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private static MultiValueMap buildMultiMap(String[][] pairs) {
        MultiValueMap map = new MultiValueMap();
        for (String[] pair : pairs) {
            try {
                map.set(pair[0], pair[1]);
            } catch (Exception e) {
                e.printStackTrace();
                // Just eat the stupid Exception!!
            }
        }
        return map;
    }

    private static Date createDate(String dateString) {
        try {
            SimpleDateFormat parser =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            return parser.parse(dateString);
        } catch (ParseException e) {
            // eat the exception
            e.printStackTrace();
            return null;
        }
    }

}
