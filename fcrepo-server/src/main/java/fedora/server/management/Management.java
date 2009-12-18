/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.management;

import java.io.InputStream;

import java.util.Date;

import fedora.server.Context;
import fedora.server.errors.ServerException;
import fedora.server.messaging.PName;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.RelationshipTuple;

/**
 * The management subsystem interface.
 *
 * @author Chris Wilper
 * @version $Id$
 */
public interface Management {

    public String ingest(@PName("context")Context context,
                         @PName("serialization")InputStream serialization,
                         @PName("logMessage")String logMessage,
                         @PName("format")String format,
                         @PName("encoding")String encoding,
                         @PName("newPid")boolean newPid) throws ServerException;

    public Date modifyObject(@PName("context")Context context,
                             @PName("pid")String pid,
                             @PName("state")String state,
                             @PName("label")String label,
                             @PName("ownerID")String ownerID,
                             @PName("logMessage")String logMessage) throws ServerException;

    public InputStream getObjectXML(@PName("context")Context context, @PName("pid")String pid, @PName("encoding")String encoding)
            throws ServerException;

    public InputStream export(@PName("context")Context context,
                              @PName("pid")String pid,
                              @PName("format")String format,
                              @PName("exportContext")String exportContext,
                              @PName("encoding")String encoding) throws ServerException;

    public Date purgeObject(@PName("context")Context context,
                            @PName("pid")String pid,
                            @PName("logMessage")String logMessage,
                            @PName("force")boolean force) throws ServerException;

    public String addDatastream(@PName("context")Context context,
                                @PName("pid")String pid,
                                @PName("dsID")String dsID,
                                @PName("altIDs")String[] altIDs,
                                @PName("dsLabel")String dsLabel,
                                @PName("versionable")boolean versionable,
                                @PName("mimeType")String mimeType,
                                @PName("formatURI")String formatURI,
                                @PName("dsLocation")String dsLocation,
                                @PName("controlGroup")String controlGroup,
                                @PName("dsState")String dsState,
                                @PName("checksumType")String checksumType,
                                @PName("checksum")String checksum,
                                @PName("logMessage")String logMessage) throws ServerException;

    public Date modifyDatastreamByReference(@PName("context")Context context,
                                            @PName("pid")String pid,
                                            @PName("dsID")String dsID,
                                            @PName("altIDs")String[] altIDs,
                                            @PName("dsLabel")String dsLabel,
                                            @PName("mimeType")String mimeType,
                                            @PName("formatURI")String formatURI,
                                            @PName("dsLocation")String dsLocation,
                                            @PName("checksumType")String checksumType,
                                            @PName("checksum")String checksum,
                                            @PName("logMessage")String logMessage,
                                            @PName("force")boolean force)
            throws ServerException;

    public Date modifyDatastreamByValue(@PName("context")Context context,
                                        @PName("pid")String pid,
                                        @PName("dsID")String dsID,
                                        @PName("altIDs")String[] altIDs,
                                        @PName("dsLabel")String dsLabel,
                                        @PName("mimeType")String mimeType,
                                        @PName("formatURI")String formatURI,
                                        @PName("dsContent")InputStream dsContent,
                                        @PName("checksumType")String checksumType,
                                        @PName("checksum")String checksum,
                                        @PName("logMessage")String logMessage,
                                        @PName("force")boolean force) throws ServerException;

    public Date[] purgeDatastream(@PName("context")Context context,
                                  @PName("pid")String pid,
                                  @PName("dsID")String dsID,
                                  @PName("startDT")Date startDT,
                                  @PName("endDT")Date endDT,
                                  @PName("logMessage")String logMessage,
                                  @PName("force")boolean force) throws ServerException;

    public Datastream getDatastream(@PName("context")Context context,
                                    @PName("pid")String pid,
                                    @PName("dsID")String dsID,
                                    @PName("asOfDateTime")Date asOfDateTime) throws ServerException;

    public Datastream[] getDatastreams(@PName("context")Context context,
                                       @PName("pid")String pid,
                                       @PName("asOfDateTime")Date asOfDateTime,
                                       @PName("dsState")String dsState) throws ServerException;

    public Datastream[] getDatastreamHistory(@PName("context")Context context,
                                             @PName("pid")String pid,
                                             @PName("dsID")String dsID)
            throws ServerException;

    public String putTempStream(@PName("context")Context context, @PName("in")InputStream in)
            throws ServerException;

    public InputStream getTempStream(@PName("id")String id) throws ServerException;

    public Date setDatastreamState(@PName("context")Context context,
                                   @PName("pid")String pid,
                                   @PName("dsID")String dsID,
                                   @PName("dsState")String dsState,
                                   @PName("logMessage")String logMessage) throws ServerException;

    public Date setDatastreamVersionable(@PName("context")Context context,
                                         @PName("pid")String pid,
                                         @PName("dsID")String dsID,
                                         @PName("versionable")boolean versionable,
                                         @PName("logMessage")String logMessage)
            throws ServerException;

    public String compareDatastreamChecksum(@PName("context")Context context,
                                            @PName("pid")String pid,
                                            @PName("dsID")String dsID,
                                            @PName("asOfDateTime")Date asOfDateTime)
            throws ServerException;

    public String[] getNextPID(@PName("context")Context context, @PName("numPIDs")int numPIDs, @PName("namespace")String namespace)
            throws ServerException;

    public RelationshipTuple[] getRelationships(@PName("context")Context context,
                                                @PName("subject")String subject,
                                                @PName("relationship")String relationship)
            throws ServerException;

    public boolean addRelationship(@PName("context")Context context,
                                   @PName("subject")String subject,
                                   @PName("relationship")String relationship,
                                   @PName("object")String object,
                                   @PName("isLiteral")boolean isLiteral,
                                   @PName("datatype")String datatype) throws ServerException;

    public boolean purgeRelationship(@PName("context")Context context,
                                     @PName("subject")String subject,
                                     @PName("relationship")String relationship,
                                     @PName("object")String object,
                                     @PName("isLiteral")boolean isLiteral,
                                     @PName("datatype")String datatype) throws ServerException;

}
