/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import java.math.BigInteger;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.search.Condition;
import org.fcrepo.server.types.mtom.gen.ArrayOfString;

import org.fcrepo.utilities.DateUtility;

/**
 * A utility class for converting back and forth from the internal Fedora type
 * classes in org.fcrepo.server.storage.types and the generated type classes
 * produced by the wsdl2java emitter in org.fcrepo.server.types.gen.gen.
 *
 * @author Ross Wayland
 */
public abstract class TypeUtility {

    private static final Logger logger = LoggerFactory
            .getLogger(TypeUtility.class);

    private static final int INITIAL_SIZE = 1024 * 1024;

    private static final int BUFFER_SIZE = 1024;

    public static org.fcrepo.server.types.mtom.gen.Datastream convertDatastreamToGenDatastreamMTOM(org.fcrepo.server.storage.types.Datastream in) {
        org.fcrepo.server.types.mtom.gen.Datastream out =
                new org.fcrepo.server.types.mtom.gen.Datastream();
        String group = in.DSControlGrp;
        out.setControlGroup(org.fcrepo.server.types.mtom.gen.DatastreamControlGroup
                .fromValue(group));
        if ("R".equals(group) || "E".equals(group)) {
            // only given location if it's a redirect or external datastream
            out.setLocation(in.DSLocation);
        }
        out.setCreateDate(DateUtility.convertDateToString(in.DSCreateDT));
        out.setID(in.DatastreamID);
        org.fcrepo.server.types.mtom.gen.ArrayOfString altIDs =
                new org.fcrepo.server.types.mtom.gen.ArrayOfString();
        if (in.DatastreamAltIDs != null)
            altIDs.getItem().addAll(Arrays.asList(in.DatastreamAltIDs));
        out.setAltIDs(altIDs);
        out.setLabel(in.DSLabel);
        out.setVersionable(in.DSVersionable);
        out.setMIMEType(in.DSMIME);
        out.setFormatURI(in.DSFormatURI);
        out.setSize(in.DSSize);
        out.setState(in.DSState);
        out.setVersionID(in.DSVersionID);
        out.setChecksum(in.DSChecksum);
        out.setChecksumType(in.DSChecksumType);
        return out;
    }

    public static org.fcrepo.server.types.gen.Datastream convertDatastreamToGenDatastream(org.fcrepo.server.storage.types.Datastream in) {
        org.fcrepo.server.types.gen.Datastream out =
                new org.fcrepo.server.types.gen.Datastream();
        String group = in.DSControlGrp;
        out.setControlGroup(org.fcrepo.server.types.gen.DatastreamControlGroup
                .fromValue(group));
        if ("R".equals(group) || "E".equals(group)) {
            // only given location if it's a redirect or external datastream
            out.setLocation(in.DSLocation);
        }
        out.setCreateDate(DateUtility.convertDateToString(in.DSCreateDT));
        out.setID(in.DatastreamID);
        org.fcrepo.server.types.gen.ArrayOfString altIDs =
                new org.fcrepo.server.types.gen.ArrayOfString();
        if (in.DatastreamAltIDs != null)
            altIDs.getItem().addAll(Arrays.asList(in.DatastreamAltIDs));
        out.setAltIDs(altIDs);
        out.setLabel(in.DSLabel);
        out.setVersionable(in.DSVersionable);
        out.setMIMEType(in.DSMIME);
        out.setFormatURI(in.DSFormatURI);
        out.setSize(in.DSSize);
        out.setState(in.DSState);
        out.setVersionID(in.DSVersionID);
        out.setChecksum(in.DSChecksum);
        out.setChecksumType(in.DSChecksumType);
        return out;
    }

    public static org.fcrepo.server.types.gen.FieldSearchResult convertFieldSearchResultToGenFieldSearchResult(org.fcrepo.server.search.FieldSearchResult result) {
        org.fcrepo.server.types.gen.FieldSearchResult ret =
                new org.fcrepo.server.types.gen.FieldSearchResult();
        ret.setResultList(convertSearchObjectFieldsListToGenObjectFieldsArray(result
                .objectFieldsList()));
        if (result.getToken() != null) {
            org.fcrepo.server.types.gen.ListSession sess =
                    new org.fcrepo.server.types.gen.ListSession();
            org.fcrepo.server.types.gen.ObjectFactory factory =
                    new org.fcrepo.server.types.gen.ObjectFactory();
            sess.setToken(result.getToken());
            if (result.getCursor() > -1) {
                sess.setCursor(new BigInteger("" + result.getCursor()));
            }
            if (result.getCompleteListSize() > -1) {
                sess.setCompleteListSize(new BigInteger(""
                        + result.getCompleteListSize()));
            }
            if (result.getExpirationDate() != null) {
                sess.setExpirationDate(factory.createListSessionExpirationDate(DateUtility
                        .convertDateToString(result.getExpirationDate())));
            }
            ret.setListSession(factory.createFieldSearchResultListSession(sess));
        }
        return ret;
    }

    public static org.fcrepo.server.types.mtom.gen.FieldSearchResult convertFieldSearchResultToGenFieldSearchResultMTOM(org.fcrepo.server.search.FieldSearchResult result) {
        org.fcrepo.server.types.mtom.gen.FieldSearchResult ret =
                new org.fcrepo.server.types.mtom.gen.FieldSearchResult();
        ret.setResultList(convertSearchObjectFieldsListToGenObjectFieldsArrayMTOM(result
                .objectFieldsList()));
        if (result.getToken() != null) {
            org.fcrepo.server.types.mtom.gen.ListSession sess =
                    new org.fcrepo.server.types.mtom.gen.ListSession();
            org.fcrepo.server.types.mtom.gen.ObjectFactory factory =
                    new org.fcrepo.server.types.mtom.gen.ObjectFactory();
            sess.setToken(result.getToken());
            if (result.getCursor() > -1) {
                sess.setCursor(new BigInteger("" + result.getCursor()));
            }
            if (result.getCompleteListSize() > -1) {
                sess.setCompleteListSize(new BigInteger(""
                        + result.getCompleteListSize()));
            }
            if (result.getExpirationDate() != null) {
                sess.setExpirationDate(factory.createListSessionExpirationDate(DateUtility
                        .convertDateToString(result.getExpirationDate())));
            }
            ret.setListSession(factory.createFieldSearchResultListSession(sess));
        }
        return ret;
    }

    public static org.fcrepo.server.search.FieldSearchQuery convertGenFieldSearchQueryToFieldSearchQuery(org.fcrepo.server.types.gen.FieldSearchQuery gen)
            throws org.fcrepo.server.errors.InvalidOperatorException,
            org.fcrepo.server.errors.QueryParseException {
        if (gen.getTerms() != null) {
            return new org.fcrepo.server.search.FieldSearchQuery(gen.getTerms()
                    .getValue());
        } else {
            return new org.fcrepo.server.search.FieldSearchQuery(convertGenConditionArrayToSearchConditionList(gen
                    .getConditions().getValue()));
        }
    }

    public static org.fcrepo.server.search.FieldSearchQuery convertGenFieldSearchQueryToFieldSearchQueryMTOM(org.fcrepo.server.types.mtom.gen.FieldSearchQuery gen)
            throws org.fcrepo.server.errors.InvalidOperatorException,
            org.fcrepo.server.errors.QueryParseException {
        if (gen.getTerms() != null) {
            return new org.fcrepo.server.search.FieldSearchQuery(gen.getTerms()
                    .getValue());
        } else {
            return new org.fcrepo.server.search.FieldSearchQuery(convertGenConditionArrayToSearchConditionListMTOM(gen
                    .getConditions().getValue()));
        }
    }

    public static List<Condition> convertGenConditionArrayToSearchConditionList(org.fcrepo.server.types.gen.FieldSearchQuery.Conditions genConditions)
            throws org.fcrepo.server.errors.InvalidOperatorException,
            org.fcrepo.server.errors.QueryParseException {
        ArrayList<Condition> list = new ArrayList<Condition>();
        if (genConditions != null && genConditions.getCondition() != null) {
            for (org.fcrepo.server.types.gen.Condition c : genConditions
                    .getCondition()) {
                list.add(new org.fcrepo.server.search.Condition(c.getProperty(),
                                                                c.getOperator()
                                                                        .value(),
                                                                c.getValue()));
            }
        }
        return list;
    }

    public static List<Condition> convertGenConditionArrayToSearchConditionListMTOM(org.fcrepo.server.types.mtom.gen.FieldSearchQuery.Conditions genConditions)
            throws org.fcrepo.server.errors.InvalidOperatorException,
            org.fcrepo.server.errors.QueryParseException {
        ArrayList<Condition> list = new ArrayList<Condition>();
        if (genConditions != null && genConditions.getCondition() != null) {
            for (org.fcrepo.server.types.mtom.gen.Condition c : genConditions
                    .getCondition()) {
                list.add(new org.fcrepo.server.search.Condition(c.getProperty(),
                                                                c.getOperator()
                                                                        .value(),
                                                                c.getValue()));
            }
        }
        return list;
    }

    private static org.fcrepo.server.types.gen.FieldSearchResult.ResultList convertSearchObjectFieldsListToGenObjectFieldsArray(List<org.fcrepo.server.search.ObjectFields> sfList) {
        org.fcrepo.server.types.gen.FieldSearchResult.ResultList genFields =
                new org.fcrepo.server.types.gen.FieldSearchResult.ResultList();
        for (int i = 0; i < sfList.size(); i++) {
            org.fcrepo.server.types.gen.ObjectFields gf =
                    new org.fcrepo.server.types.gen.ObjectFields();
            org.fcrepo.server.search.ObjectFields sf = sfList.get(i);
            org.fcrepo.server.types.gen.ObjectFactory factory =
                    new org.fcrepo.server.types.gen.ObjectFactory();
            // Repository key fields
            if (sf.getPid() != null) {
                gf.setPid(factory.createObjectFieldsPid(sf.getPid()));
            }
            if (sf.getLabel() != null) {
                gf.setLabel(factory.createObjectFieldsLabel(sf.getLabel()));
            }
            if (sf.getState() != null) {
                gf.setState(factory.createObjectFieldsState(sf.getState()));
            }
            if (sf.getOwnerId() != null) {
                gf.setOwnerId(factory.createObjectFieldsOwnerId(sf.getOwnerId()));
            }
            if (sf.getCDate() != null) {
                gf.setCDate(factory.createObjectFieldsCDate(DateUtility
                        .convertDateToString(sf.getCDate())));
            }
            if (sf.getMDate() != null) {
                gf.setMDate(factory.createObjectFieldsMDate(DateUtility
                        .convertDateToString(sf.getMDate())));
            }
            if (sf.getDCMDate() != null) {
                gf.setDcmDate(factory.createObjectFieldsDcmDate(DateUtility
                        .convertDateToString(sf.getDCMDate())));
            }
            // Dublin core fields
            if (sf.titles().size() != 0) {
                gf.getTitle().addAll(toStringList(sf.titles()));
            }
            if (sf.creators().size() != 0) {
                gf.getCreator().addAll(toStringList(sf.creators()));
            }
            if (sf.subjects().size() != 0) {
                gf.getSubject().addAll(toStringList(sf.subjects()));
            }
            if (sf.descriptions().size() != 0) {
                gf.getDescription().addAll(toStringList(sf.descriptions()));
            }
            if (sf.publishers().size() != 0) {
                gf.getPublisher().addAll(toStringList(sf.publishers()));
            }
            if (sf.contributors().size() != 0) {
                gf.getContributor().addAll(toStringList(sf.contributors()));
            }
            if (sf.dates().size() != 0) {
                gf.getDate().addAll(toStringList(sf.dates()));
            }
            if (sf.types().size() != 0) {
                gf.getType().addAll(toStringList(sf.types()));
            }
            if (sf.formats().size() != 0) {
                gf.getFormat().addAll(toStringList(sf.formats()));
            }
            if (sf.identifiers().size() != 0) {
                gf.getIdentifier().addAll(toStringList(sf.identifiers()));
            }
            if (sf.sources().size() != 0) {
                gf.getSource().addAll(toStringList(sf.sources()));
            }
            if (sf.languages().size() != 0) {
                gf.getLanguage().addAll(toStringList(sf.languages()));
            }
            if (sf.relations().size() != 0) {
                gf.getRelation().addAll(toStringList(sf.relations()));
            }
            if (sf.coverages().size() != 0) {
                gf.getCoverage().addAll(toStringList(sf.coverages()));
            }
            if (sf.rights().size() != 0) {
                gf.getRights().addAll(toStringList(sf.rights()));
            }
            genFields.getObjectFields().add(gf);
        }
        return genFields;
    }

    private static org.fcrepo.server.types.mtom.gen.FieldSearchResult.ResultList convertSearchObjectFieldsListToGenObjectFieldsArrayMTOM(List<org.fcrepo.server.search.ObjectFields> sfList) {
        org.fcrepo.server.types.mtom.gen.FieldSearchResult.ResultList genFields =
                new org.fcrepo.server.types.mtom.gen.FieldSearchResult.ResultList();
        for (int i = 0; i < sfList.size(); i++) {
            org.fcrepo.server.types.mtom.gen.ObjectFields gf =
                    new org.fcrepo.server.types.mtom.gen.ObjectFields();
            org.fcrepo.server.search.ObjectFields sf = sfList.get(i);
            org.fcrepo.server.types.gen.ObjectFactory factory =
                    new org.fcrepo.server.types.gen.ObjectFactory();
            // Repository key fields
            if (sf.getPid() != null) {
                gf.setPid(factory.createObjectFieldsPid(sf.getPid()));
            }
            if (sf.getLabel() != null) {
                gf.setLabel(factory.createObjectFieldsLabel(sf.getLabel()));
            }
            if (sf.getState() != null) {
                gf.setState(factory.createObjectFieldsState(sf.getState()));
            }
            if (sf.getOwnerId() != null) {
                gf.setOwnerId(factory.createObjectFieldsOwnerId(sf.getOwnerId()));
            }
            if (sf.getCDate() != null) {
                gf.setCDate(factory.createObjectFieldsCDate(DateUtility
                        .convertDateToString(sf.getCDate())));
            }
            if (sf.getMDate() != null) {
                gf.setMDate(factory.createObjectFieldsMDate(DateUtility
                        .convertDateToString(sf.getMDate())));
            }
            if (sf.getDCMDate() != null) {
                gf.setDcmDate(factory.createObjectFieldsDcmDate(DateUtility
                        .convertDateToString(sf.getDCMDate())));
            }
            // Dublin core fields
            if (sf.titles().size() != 0) {
                gf.getTitle().addAll(toStringList(sf.titles()));
            }
            if (sf.creators().size() != 0) {
                gf.getCreator().addAll(toStringList(sf.creators()));
            }
            if (sf.subjects().size() != 0) {
                gf.getSubject().addAll(toStringList(sf.subjects()));
            }
            if (sf.descriptions().size() != 0) {
                gf.getDescription().addAll(toStringList(sf.descriptions()));
            }
            if (sf.publishers().size() != 0) {
                gf.getPublisher().addAll(toStringList(sf.publishers()));
            }
            if (sf.contributors().size() != 0) {
                gf.getContributor().addAll(toStringList(sf.contributors()));
            }
            if (sf.dates().size() != 0) {
                gf.getDate().addAll(toStringList(sf.dates()));
            }
            if (sf.types().size() != 0) {
                gf.getType().addAll(toStringList(sf.types()));
            }
            if (sf.formats().size() != 0) {
                gf.getFormat().addAll(toStringList(sf.formats()));
            }
            if (sf.identifiers().size() != 0) {
                gf.getIdentifier().addAll(toStringList(sf.identifiers()));
            }
            if (sf.sources().size() != 0) {
                gf.getSource().addAll(toStringList(sf.sources()));
            }
            if (sf.languages().size() != 0) {
                gf.getLanguage().addAll(toStringList(sf.languages()));
            }
            if (sf.relations().size() != 0) {
                gf.getRelation().addAll(toStringList(sf.relations()));
            }
            if (sf.coverages().size() != 0) {
                gf.getCoverage().addAll(toStringList(sf.coverages()));
            }
            if (sf.rights().size() != 0) {
                gf.getRights().addAll(toStringList(sf.rights()));
            }
            genFields.getObjectFields().add(gf);
        }
        return genFields;
    }

    public static String[] toStringArray(List<DCField> l) {
        String[] ret = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
            ret[i] = l.get(i).getValue();
        }
        return ret;
    }

    private static List<String> toStringList(List<DCField> dcFields) {
        List<String> ret = new ArrayList<String>(dcFields.size());
        for (DCField dcField : dcFields) {
            ret.add(dcField.getValue());
        }
        return ret;
    }

    public static org.fcrepo.server.types.gen.MethodParmDef convertMethodParmDefToGenMethodParmDef(org.fcrepo.server.storage.types.MethodParmDef methodParmDef) {
        if (methodParmDef != null) {
            org.fcrepo.server.types.gen.MethodParmDef genMethodParmDef =
                    new org.fcrepo.server.types.gen.MethodParmDef();
            genMethodParmDef.setParmName(methodParmDef.parmName);
            genMethodParmDef.setParmLabel(methodParmDef.parmLabel);
            genMethodParmDef
                    .setParmDefaultValue(methodParmDef.parmDefaultValue);
            org.fcrepo.server.types.gen.ArrayOfString parmDomainVals =
                    new org.fcrepo.server.types.gen.ArrayOfString();
            if (methodParmDef.parmDomainValues != null)
                parmDomainVals.getItem()
                        .addAll(Arrays.asList(methodParmDef.parmDomainValues));
            genMethodParmDef.setParmDomainValues(parmDomainVals);
            genMethodParmDef.setParmRequired(methodParmDef.parmRequired);
            genMethodParmDef.setParmType(methodParmDef.parmType);
            genMethodParmDef.setParmPassBy(methodParmDef.parmPassBy);
            return genMethodParmDef;

        } else {
            return null;
        }
    }

    public static org.fcrepo.server.types.mtom.gen.MethodParmDef convertMethodParmDefToGenMethodParmDefMTOM(org.fcrepo.server.storage.types.MethodParmDef methodParmDef) {
        if (methodParmDef != null) {
            org.fcrepo.server.types.mtom.gen.MethodParmDef genMethodParmDef =
                    new org.fcrepo.server.types.mtom.gen.MethodParmDef();
            genMethodParmDef.setParmName(methodParmDef.parmName);
            genMethodParmDef.setParmLabel(methodParmDef.parmLabel);
            genMethodParmDef
                    .setParmDefaultValue(methodParmDef.parmDefaultValue);
            org.fcrepo.server.types.mtom.gen.ArrayOfString parmDomainVals =
                    new org.fcrepo.server.types.mtom.gen.ArrayOfString();
            if (methodParmDef.parmDomainValues != null)
                parmDomainVals.getItem()
                        .addAll(Arrays.asList(methodParmDef.parmDomainValues));
            genMethodParmDef.setParmDomainValues(parmDomainVals);
            genMethodParmDef.setParmRequired(methodParmDef.parmRequired);
            genMethodParmDef.setParmType(methodParmDef.parmType);
            genMethodParmDef.setParmPassBy(methodParmDef.parmPassBy);
            return genMethodParmDef;

        } else {
            return null;
        }
    }

    public static org.fcrepo.server.types.gen.MIMETypedStream convertMIMETypedStreamToGenMIMETypedStream(org.fcrepo.server.storage.types.MIMETypedStream mimeTypedStream) {
        if (mimeTypedStream != null) {
            org.fcrepo.server.types.gen.MIMETypedStream genMIMETypedStream =
                    new org.fcrepo.server.types.gen.MIMETypedStream();
            genMIMETypedStream.setMIMEType(mimeTypedStream.MIMEType);
            org.fcrepo.server.storage.types.Property[] header =
                    mimeTypedStream.header;
            org.fcrepo.server.types.gen.MIMETypedStream.Header head =
                    new org.fcrepo.server.types.gen.MIMETypedStream.Header();
            if (header != null) {
                for (org.fcrepo.server.storage.types.Property property : header) {
                    head.getProperty()
                            .add(convertPropertyToGenProperty(property));
                }
            }
            genMIMETypedStream.setHeader(head);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
            InputStream is = mimeTypedStream.getStream();
            int byteStream = 0;
            try {
                byte[] buffer = new byte[255];
                while ((byteStream = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, byteStream);
                }
            } catch (IOException ioe) {
                logger.error("Error converting types", ioe);
            }
            genMIMETypedStream.setStream(baos.toByteArray());
            mimeTypedStream.close();
            mimeTypedStream.setStream(new ByteArrayInputStream(baos
                    .toByteArray()));
            return genMIMETypedStream;

        } else {
            return null;
        }
    }

    public static org.fcrepo.server.types.mtom.gen.MIMETypedStream convertMIMETypedStreamToGenMIMETypedStreamMTOM(org.fcrepo.server.storage.types.MIMETypedStream mimeTypedStream) {
        if (mimeTypedStream != null) {
            org.fcrepo.server.types.mtom.gen.MIMETypedStream genMIMETypedStream =
                    new org.fcrepo.server.types.mtom.gen.MIMETypedStream();
            genMIMETypedStream.setMIMEType(mimeTypedStream.MIMEType);
            org.fcrepo.server.storage.types.Property[] header =
                    mimeTypedStream.header;
            org.fcrepo.server.types.mtom.gen.MIMETypedStream.Header head =
                    new org.fcrepo.server.types.mtom.gen.MIMETypedStream.Header();
            if (header != null) {
                for (org.fcrepo.server.storage.types.Property property : header) {
                    head.getProperty()
                            .add(convertPropertyToGenPropertyMTOM(property));
                }
            }
            genMIMETypedStream.setHeader(head);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
            InputStream is = mimeTypedStream.getStream();
            int byteStream = 0;
            try {
                byte[] buffer = new byte[255];
                while ((byteStream = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, byteStream);
                }
            } catch (IOException ioe) {
                logger.error("Error converting types", ioe);
            }
            genMIMETypedStream
                    .setStream(new DataHandler(new ByteArrayDataSource(baos
                            .toByteArray(), "text/html")));
            mimeTypedStream.close();
            mimeTypedStream.setStream(new ByteArrayInputStream(baos
                    .toByteArray()));
            return genMIMETypedStream;

        } else {
            return null;
        }
    }

    public static List<org.fcrepo.server.types.gen.ObjectMethodsDef> convertObjectMethodsDefArrayToGenObjectMethodsDefList(org.fcrepo.server.storage.types.ObjectMethodsDef[] objectMethodDefs) {
        if (objectMethodDefs != null) {
            List<org.fcrepo.server.types.gen.ObjectMethodsDef> genObjectMethodDefs =
                    new ArrayList<org.fcrepo.server.types.gen.ObjectMethodsDef>(objectMethodDefs.length);
            for (org.fcrepo.server.storage.types.ObjectMethodsDef objectMethodsDef : objectMethodDefs) {
                org.fcrepo.server.types.gen.ObjectMethodsDef genObjectMethodDef =
                        new org.fcrepo.server.types.gen.ObjectMethodsDef();
                genObjectMethodDef.setPID(objectMethodsDef.PID);
                genObjectMethodDef
                        .setServiceDefinitionPID(objectMethodsDef.sDefPID);
                genObjectMethodDef.setMethodName(objectMethodsDef.methodName);
                org.fcrepo.server.storage.types.MethodParmDef[] methodParmDefs =
                        objectMethodsDef.methodParmDefs;
                org.fcrepo.server.types.gen.ObjectMethodsDef.MethodParmDefs genMethodParmDefs =
                        new org.fcrepo.server.types.gen.ObjectMethodsDef.MethodParmDefs();
                if (methodParmDefs != null) {
                    for (org.fcrepo.server.storage.types.MethodParmDef methodParmDef : methodParmDefs) {
                        genMethodParmDefs
                                .getMethodParmDef()
                                .add(convertMethodParmDefToGenMethodParmDef(methodParmDef));
                    }
                }
                genObjectMethodDef.setMethodParmDefs(genMethodParmDefs);
                genObjectMethodDefs.add(genObjectMethodDef);
            }
            return genObjectMethodDefs;

        } else {
            return null;
        }
    }

    public static List<org.fcrepo.server.types.mtom.gen.ObjectMethodsDef> convertObjectMethodsDefArrayToGenObjectMethodsDefListMTOM(org.fcrepo.server.storage.types.ObjectMethodsDef[] objectMethodDefs) {
        if (objectMethodDefs != null) {
            List<org.fcrepo.server.types.mtom.gen.ObjectMethodsDef> genObjectMethodDefs =
                    new ArrayList<org.fcrepo.server.types.mtom.gen.ObjectMethodsDef>(objectMethodDefs.length);
            for (org.fcrepo.server.storage.types.ObjectMethodsDef objectMethodsDef : objectMethodDefs) {
                org.fcrepo.server.types.mtom.gen.ObjectMethodsDef genObjectMethodDef =
                        new org.fcrepo.server.types.mtom.gen.ObjectMethodsDef();
                genObjectMethodDef.setPID(objectMethodsDef.PID);
                genObjectMethodDef
                        .setServiceDefinitionPID(objectMethodsDef.sDefPID);
                genObjectMethodDef.setMethodName(objectMethodsDef.methodName);
                org.fcrepo.server.storage.types.MethodParmDef[] methodParmDefs =
                        objectMethodsDef.methodParmDefs;
                org.fcrepo.server.types.mtom.gen.ObjectMethodsDef.MethodParmDefs genMethodParmDefs =
                        new org.fcrepo.server.types.mtom.gen.ObjectMethodsDef.MethodParmDefs();
                if (methodParmDefs != null) {
                    for (org.fcrepo.server.storage.types.MethodParmDef methodParmDef : methodParmDefs) {
                        genMethodParmDefs
                                .getMethodParmDef()
                                .add(convertMethodParmDefToGenMethodParmDefMTOM(methodParmDef));
                    }
                }
                genObjectMethodDef.setMethodParmDefs(genMethodParmDefs);
                genObjectMethodDefs.add(genObjectMethodDef);
            }
            return genObjectMethodDefs;

        } else {
            return null;
        }
    }

    public static org.fcrepo.server.types.gen.ObjectProfile convertObjectProfileToGenObjectProfile(org.fcrepo.server.access.ObjectProfile objectProfile) {
        if (objectProfile != null) {
            org.fcrepo.server.types.gen.ObjectProfile genObjectProfile =
                    new org.fcrepo.server.types.gen.ObjectProfile();
            genObjectProfile.setPid(objectProfile.PID);
            genObjectProfile.setObjLabel(objectProfile.objectLabel);

            org.fcrepo.server.types.gen.ObjectProfile.ObjModels objModels =
                    new org.fcrepo.server.types.gen.ObjectProfile.ObjModels();
            if (objectProfile.objectModels != null) {
                objModels.getModel().addAll(objectProfile.objectModels);
            }
            genObjectProfile.setObjCreateDate(DateUtility
                    .convertDateToString(objectProfile.objectCreateDate));
            genObjectProfile.setObjLastModDate(DateUtility
                    .convertDateToString(objectProfile.objectLastModDate));
            genObjectProfile
                    .setObjDissIndexViewURL(objectProfile.dissIndexViewURL);
            genObjectProfile
                    .setObjItemIndexViewURL(objectProfile.itemIndexViewURL);
            return genObjectProfile;
        } else {
            return null;
        }
    }

    public static org.fcrepo.server.types.mtom.gen.ObjectProfile convertObjectProfileToGenObjectProfileMTOM(org.fcrepo.server.access.ObjectProfile objectProfile) {
        if (objectProfile != null) {
            org.fcrepo.server.types.mtom.gen.ObjectProfile genObjectProfile =
                    new org.fcrepo.server.types.mtom.gen.ObjectProfile();
            genObjectProfile.setPid(objectProfile.PID);
            genObjectProfile.setObjLabel(objectProfile.objectLabel);

            org.fcrepo.server.types.mtom.gen.ObjectProfile.ObjModels objModels =
                    new org.fcrepo.server.types.mtom.gen.ObjectProfile.ObjModels();
            if (objectProfile.objectModels != null) {
                objModels.getModel().addAll(objectProfile.objectModels);
            }
            genObjectProfile.setObjModels(objModels);
            genObjectProfile.setObjCreateDate(DateUtility
                    .convertDateToString(objectProfile.objectCreateDate));
            genObjectProfile.setObjLastModDate(DateUtility
                    .convertDateToString(objectProfile.objectLastModDate));
            genObjectProfile
                    .setObjDissIndexViewURL(objectProfile.dissIndexViewURL);
            genObjectProfile
                    .setObjItemIndexViewURL(objectProfile.itemIndexViewURL);
            return genObjectProfile;
        } else {
            return null;
        }
    }

    public static org.fcrepo.server.types.gen.RepositoryInfo convertReposInfoToGenReposInfo(org.fcrepo.server.access.RepositoryInfo repositoryInfo) {
        if (repositoryInfo != null) {
            org.fcrepo.server.types.gen.RepositoryInfo genRepositoryInfo =
                    new org.fcrepo.server.types.gen.RepositoryInfo();
            genRepositoryInfo.setRepositoryName(repositoryInfo.repositoryName);
            genRepositoryInfo
                    .setRepositoryBaseURL(repositoryInfo.repositoryBaseURL);
            genRepositoryInfo
                    .setRepositoryVersion(repositoryInfo.repositoryVersion);
            genRepositoryInfo
                    .setRepositoryPIDNamespace(repositoryInfo.repositoryPIDNamespace);
            genRepositoryInfo
                    .setDefaultExportFormat(repositoryInfo.defaultExportFormat);
            genRepositoryInfo.setOAINamespace(repositoryInfo.OAINamespace);
            if (repositoryInfo.adminEmailList != null) {
                org.fcrepo.server.types.gen.ArrayOfString val =
                        new org.fcrepo.server.types.gen.ArrayOfString();
                val.getItem()
                        .addAll(Arrays.asList(repositoryInfo.adminEmailList));
                genRepositoryInfo.setAdminEmailList(val);
            }
            genRepositoryInfo.setSamplePID(repositoryInfo.samplePID);
            genRepositoryInfo
                    .setSampleOAIIdentifier(repositoryInfo.sampleOAIIdentifer);
            genRepositoryInfo
                    .setSampleSearchURL(repositoryInfo.sampleSearchURL);
            genRepositoryInfo
                    .setSampleAccessURL(repositoryInfo.sampleAccessURL);
            genRepositoryInfo.setSampleOAIURL(repositoryInfo.sampleOAIURL);
            if (repositoryInfo.retainPIDs != null) {
                org.fcrepo.server.types.gen.ArrayOfString val =
                        new org.fcrepo.server.types.gen.ArrayOfString();
                val.getItem().addAll(Arrays.asList(repositoryInfo.retainPIDs));
                genRepositoryInfo.setRetainPIDs(val);
            }
            return genRepositoryInfo;
        } else {
            return null;
        }
    }

    public static org.fcrepo.server.types.mtom.gen.RepositoryInfo convertReposInfoToGenReposInfoMTOM(org.fcrepo.server.access.RepositoryInfo repositoryInfo) {
        if (repositoryInfo != null) {
            org.fcrepo.server.types.mtom.gen.RepositoryInfo genRepositoryInfo =
                    new org.fcrepo.server.types.mtom.gen.RepositoryInfo();
            genRepositoryInfo.setRepositoryName(repositoryInfo.repositoryName);
            genRepositoryInfo
                    .setRepositoryBaseURL(repositoryInfo.repositoryBaseURL);
            genRepositoryInfo
                    .setRepositoryVersion(repositoryInfo.repositoryVersion);
            genRepositoryInfo
                    .setRepositoryPIDNamespace(repositoryInfo.repositoryPIDNamespace);
            genRepositoryInfo
                    .setDefaultExportFormat(repositoryInfo.defaultExportFormat);
            genRepositoryInfo.setOAINamespace(repositoryInfo.OAINamespace);
            if (repositoryInfo.adminEmailList != null) {
                org.fcrepo.server.types.mtom.gen.ArrayOfString val =
                        new org.fcrepo.server.types.mtom.gen.ArrayOfString();
                val.getItem()
                        .addAll(Arrays.asList(repositoryInfo.adminEmailList));
                genRepositoryInfo.setAdminEmailList(val);
            }
            genRepositoryInfo.setSamplePID(repositoryInfo.samplePID);
            genRepositoryInfo
                    .setSampleOAIIdentifier(repositoryInfo.sampleOAIIdentifer);
            genRepositoryInfo
                    .setSampleSearchURL(repositoryInfo.sampleSearchURL);
            genRepositoryInfo
                    .setSampleAccessURL(repositoryInfo.sampleAccessURL);
            genRepositoryInfo.setSampleOAIURL(repositoryInfo.sampleOAIURL);
            if (repositoryInfo.retainPIDs != null) {
                org.fcrepo.server.types.mtom.gen.ArrayOfString val =
                        new org.fcrepo.server.types.mtom.gen.ArrayOfString();
                val.getItem().addAll(Arrays.asList(repositoryInfo.retainPIDs));
                genRepositoryInfo.setRetainPIDs(val);
            }
            return genRepositoryInfo;
        } else {
            return null;
        }
    }

    public static org.fcrepo.server.storage.types.Property[] convertGenPropertyArrayToPropertyArray(org.fcrepo.server.types.gen.GetDissemination.Parameters genProperties) {
        if (genProperties != null) {
            org.fcrepo.server.storage.types.Property[] properties =
                    new org.fcrepo.server.storage.types.Property[genProperties
                            .getParameter() == null ? 0 : genProperties
                            .getParameter().size()];
            int i = 0;
            for (org.fcrepo.server.types.gen.Property prop : genProperties
                    .getParameter()) {
                org.fcrepo.server.storage.types.Property property =
                        new org.fcrepo.server.storage.types.Property();
                property = convertGenPropertyToProperty(prop);
                properties[i++] = property;
            }
            return properties;

        } else {
            return null;
        }
    }

    public static org.fcrepo.server.storage.types.Property[] convertGenPropertyArrayToPropertyArrayMTOM(org.fcrepo.server.types.mtom.gen.GetDissemination.Parameters genProperties) {
        if (genProperties != null) {
            org.fcrepo.server.storage.types.Property[] properties =
                    new org.fcrepo.server.storage.types.Property[genProperties
                            .getParameter() == null ? 0 : genProperties
                            .getParameter().size()];
            int i = 0;
            for (org.fcrepo.server.types.mtom.gen.Property prop : genProperties
                    .getParameter()) {
                org.fcrepo.server.storage.types.Property property =
                        new org.fcrepo.server.storage.types.Property();
                property = convertGenPropertyToPropertyMTOM(prop);
                properties[i++] = property;
            }
            return properties;

        } else {
            return null;
        }
    }

    public static org.fcrepo.server.storage.types.Property convertGenPropertyToProperty(org.fcrepo.server.types.gen.Property genProperty) {
        org.fcrepo.server.storage.types.Property property =
                new org.fcrepo.server.storage.types.Property();
        if (genProperty != null) {
            property.name = genProperty.getName();
            property.value = genProperty.getValue();
        }
        return property;
    }

    public static org.fcrepo.server.storage.types.Property convertGenPropertyToPropertyMTOM(org.fcrepo.server.types.mtom.gen.Property genProperty) {
        org.fcrepo.server.storage.types.Property property =
                new org.fcrepo.server.storage.types.Property();
        if (genProperty != null) {
            property.name = genProperty.getName();
            property.value = genProperty.getValue();
        }
        return property;
    }

    public static org.fcrepo.server.types.gen.Property convertPropertyToGenProperty(org.fcrepo.server.storage.types.Property property) {
        org.fcrepo.server.types.gen.Property genProperty =
                new org.fcrepo.server.types.gen.Property();
        if (property != null) {
            genProperty.setName(property.name);
            genProperty.setValue(property.value);
        }
        return genProperty;
    }

    public static org.fcrepo.server.types.mtom.gen.Property convertPropertyToGenPropertyMTOM(org.fcrepo.server.storage.types.Property property) {
        org.fcrepo.server.types.mtom.gen.Property genProperty =
                new org.fcrepo.server.types.mtom.gen.Property();
        if (property != null) {
            genProperty.setName(property.name);
            genProperty.setValue(property.value);
        }
        return genProperty;
    }

    public static org.fcrepo.server.types.mtom.gen.RelationshipTuple convertRelsTupleToGenRelsTuple(org.fcrepo.server.storage.types.RelationshipTuple in) {
        if (in == null) {
            return null;
        }
        org.fcrepo.server.types.mtom.gen.RelationshipTuple out =
                new org.fcrepo.server.types.mtom.gen.RelationshipTuple();
        out.setSubject(in.subject);
        out.setPredicate(in.predicate);
        out.setObject(in.object);
        out.setIsLiteral(in.isLiteral);
        out.setDatatype(in.datatype);
        return out;
    }

    public static org.fcrepo.server.types.gen.RelationshipTuple convertRelsTupleToGenRelsTupleMTOM(org.fcrepo.server.storage.types.RelationshipTuple in) {
        if (in == null) {
            return null;
        }
        org.fcrepo.server.types.gen.RelationshipTuple out =
                new org.fcrepo.server.types.gen.RelationshipTuple();
        out.setSubject(in.subject);
        out.setPredicate(in.predicate);
        out.setObject(in.object);
        out.setIsLiteral(in.isLiteral);
        out.setDatatype(in.datatype);
        return out;
    }

    public static org.fcrepo.server.types.gen.DatastreamDef convertDatastreamDefToGenDatastreamDef(org.fcrepo.server.storage.types.DatastreamDef in) {
        org.fcrepo.server.types.gen.DatastreamDef out =
                new org.fcrepo.server.types.gen.DatastreamDef();
        out.setID(in.dsID);
        out.setLabel(in.dsLabel);
        out.setMIMEType(in.dsMIME);

        return out;
    }

    public static org.fcrepo.server.types.mtom.gen.DatastreamDef convertDatastreamDefToGenDatastreamDefMTOM(org.fcrepo.server.storage.types.DatastreamDef in) {
        org.fcrepo.server.types.mtom.gen.DatastreamDef out =
                new org.fcrepo.server.types.mtom.gen.DatastreamDef();
        out.setID(in.dsID);
        out.setLabel(in.dsLabel);
        out.setMIMEType(in.dsMIME);

        return out;
    }

    public static List<org.fcrepo.server.types.gen.DatastreamDef> convertDatastreamDefArrayToGenDatastreamDefList(org.fcrepo.server.storage.types.DatastreamDef[] dsDefs) {

        if (dsDefs != null) {
            List<org.fcrepo.server.types.gen.DatastreamDef> genDatastreamDefs =
                    new ArrayList<org.fcrepo.server.types.gen.DatastreamDef>(dsDefs.length);
            for (org.fcrepo.server.storage.types.DatastreamDef dsDef : dsDefs) {
                genDatastreamDefs
                        .add(convertDatastreamDefToGenDatastreamDef(dsDef));
            }
            return genDatastreamDefs;

        } else {
            return null;
        }
    }

    public static List<org.fcrepo.server.types.mtom.gen.DatastreamDef> convertDatastreamDefArrayToGenDatastreamDefListMTOM(org.fcrepo.server.storage.types.DatastreamDef[] dsDefs) {

        if (dsDefs != null) {
            List<org.fcrepo.server.types.mtom.gen.DatastreamDef> genDatastreamDefs =
                    new ArrayList<org.fcrepo.server.types.mtom.gen.DatastreamDef>(dsDefs.length);
            for (org.fcrepo.server.storage.types.DatastreamDef dsDef : dsDefs) {
                genDatastreamDefs
                        .add(convertDatastreamDefToGenDatastreamDefMTOM(dsDef));
            }
            return genDatastreamDefs;

        } else {
            return null;
        }
    }

    public static org.fcrepo.server.types.mtom.gen.Validation convertValidationToGenValidation(org.fcrepo.server.storage.types.Validation validation) {

        if (validation == null) {
            return null;
        }
        org.fcrepo.server.types.mtom.gen.Validation genvalid =
                new org.fcrepo.server.types.mtom.gen.Validation();
        genvalid.setValid(validation.isValid());
        genvalid.setPid(validation.getPid());
        org.fcrepo.server.types.mtom.gen.Validation.ObjModels objModels =
                new org.fcrepo.server.types.mtom.gen.Validation.ObjModels();
        objModels.getModel().addAll(validation.getContentModels());
        genvalid.setObjModels(objModels);
        org.fcrepo.server.types.mtom.gen.Validation.ObjProblems objProblems =
                new org.fcrepo.server.types.mtom.gen.Validation.ObjProblems();
        objProblems.getProblem().addAll(validation.getObjectProblems());
        genvalid.setObjProblems(objProblems);

        Map<String, List<String>> dsprobs = validation.getDatastreamProblems();
        org.fcrepo.server.types.mtom.gen.Validation.DatastreamProblems problems =
                new org.fcrepo.server.types.mtom.gen.Validation.DatastreamProblems();
        if (dsprobs != null) {
            for (String key : dsprobs.keySet()) {
                org.fcrepo.server.types.mtom.gen.DatastreamProblem dsProblem =
                        new org.fcrepo.server.types.mtom.gen.DatastreamProblem();
                dsProblem.setDatastreamID(key);
                dsProblem.getProblem().addAll(dsprobs.get(key));
                problems.getDatastream().add(dsProblem);
            }
        }
        genvalid.setDatastreamProblems(problems);
        return genvalid;
    }

    public static org.fcrepo.server.types.gen.Validation convertValidationToGenValidationMTOM(org.fcrepo.server.storage.types.Validation validation) {

        if (validation == null) {
            return null;
        }
        org.fcrepo.server.types.gen.Validation genvalid =
                new org.fcrepo.server.types.gen.Validation();
        genvalid.setValid(validation.isValid());
        genvalid.setPid(validation.getPid());
        org.fcrepo.server.types.gen.Validation.ObjModels objModels =
                new org.fcrepo.server.types.gen.Validation.ObjModels();
        objModels.getModel().addAll(validation.getContentModels());
        genvalid.setObjModels(objModels);
        org.fcrepo.server.types.gen.Validation.ObjProblems objProblems =
                new org.fcrepo.server.types.gen.Validation.ObjProblems();
        objProblems.getProblem().addAll(validation.getObjectProblems());
        genvalid.setObjProblems(objProblems);

        Map<String, List<String>> dsprobs = validation.getDatastreamProblems();
        org.fcrepo.server.types.gen.Validation.DatastreamProblems problems =
                new org.fcrepo.server.types.gen.Validation.DatastreamProblems();
        if (dsprobs != null) {
            for (String key : dsprobs.keySet()) {
                org.fcrepo.server.types.gen.DatastreamProblem dsProblem =
                        new org.fcrepo.server.types.gen.DatastreamProblem();
                dsProblem.setDatastreamID(key);
                dsProblem.getProblem().addAll(dsprobs.get(key));
                problems.getDatastream().add(dsProblem);
            }
        }
        genvalid.setDatastreamProblems(problems);
        return genvalid;
    }

    public static byte[] convertDataHandlerToBytes(DataHandler dh) {
        if (dh != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(INITIAL_SIZE);
            InputStream in;
            try {
                in = dh.getInputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) >= 0) {
                    bos.write(buffer, 0, bytesRead);
                }
                return bos.toByteArray();
            } catch (IOException e) {
                return null;
            }
        } else
            return null;
    }

    public static DataHandler convertBytesToDataHandler(byte[] content) {
        if (content != null) {
            return new DataHandler(new ByteArrayDataSource(content, "text/xml"));
        } else
            return null;
    }

    public static ArrayOfString convertStringtoAOS(String[] arr) {
        if (arr != null) {
            ArrayOfString arofs = new ArrayOfString();
            arofs.getItem().addAll(Arrays.asList(arr));
            return arofs;
        } else
            return null;
    }

}
