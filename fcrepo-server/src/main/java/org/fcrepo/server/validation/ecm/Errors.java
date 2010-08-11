package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.storage.types.RelationshipTuple;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Aug 11, 2010
 * Time: 10:05:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class Errors {


    public static String unableToParseSchema(String schemaID, String datastreamID, String contentModel, Exception e) {
        return "Content Model error: Unable to parse the validation "
               + "schema '" + schemaID + "'for datastream '" + datastreamID + "' from content model '" +
               contentModel + "'." +
               "The following error was encountered '" + e.getMessage() + "'";

    }

    public static String invalidContentInDatastream(String datastreamID, String schemaID, String contentModel,
                                                    Exception e) {
        return "Data error: Invalid content in datastream '" + datastreamID + "', in regards to " +
               "schema '" + schemaID + "' the content model '" + contentModel + "'"
               + "'. " + e.getMessage();

    }

    public static String unableToReadDatastream(String datastreamID, Exception e) {
        return "Data error: Unable to read datastream '" +
               datastreamID + "'. " + e.getMessage();

    }

    public static String schemaValidationWarning(String datastreamID, String schemaID, String contentModel,
                                                 SAXParseException exception) {
        return "Encountered schema validation warning while parsing datastream '" + datastreamID +
               "' with the schema '" + schemaID + "' from content model '" + contentModel +
               "'. The warning was '" + exception.getLocalizedMessage() + "'";
    }

    public static String schemaValidationError(String datastreamID, String schemaID, String contentModel,
                                               SAXParseException exception) {
        return "Encountered schema validation error while parsing datastream '" + datastreamID +
               "' with the schema '" + schemaID + "' from content model '" + contentModel +
               "'. The error was '" + exception.getLocalizedMessage() + "'";
    }


    public static String schemaValidationFatalError(String datastreamID, String schemaID, String contentModel,
                                                    SAXParseException exception) {
        return "Encountered schema validation fatal error while parsing datastream '" + datastreamID +
               "' with the schema '" + schemaID + "' from content model '" + contentModel +
               "'. The fatal error was '" + exception.getLocalizedMessage() + "'";
    }

    public static String invalidFormatURIorMimeType(String dsid, String contentmodel) {
        return "Datastream '" + dsid + "' is does not have the FORMAT_URI and MIME_TYPE attributes required by '" +
               contentmodel + "'";

    }

    public static String missingRequiredDatastream(String dsid, String contentmodel) {
        return "Datastream '" + dsid + "' is required by the content model '" + contentmodel + "'";
    }

    public static String allValuesFromViolation(String subject, String relation, String requiredTarget) {
        return "The relation '" + relation + "' in '"+subject+"'  is restricted to values from " +
               "class '" + requiredTarget + "'";
    }

    public static String someValuesFromViolationNoSuchRelation(String subject, String ontologyrelation,
                                                               String requiredTarget) {
        return "The relation '" + ontologyrelation + "' in '"+subject+"'  should have at least one" +
               "value from the from the" +
               "class '" + requiredTarget + "' and exist at least once";
    }

    public static String someValuesFromViolationWrongClassOfTarget(String subject, String relation,
                                                                   String requiredTarget) {
        return "The relation '" + relation + "' in '"+subject+"' is restricted to values from " +
               "class '" + requiredTarget + "'";

    }

    public static String minCardinalityViolation(String subject, String ontologyrelation,int min) {
        return "The relation '" + ontologyrelation + "' in '"+subject+"' should at least exist '" + min + "' times.";
    }

    public static String maxCardinalityViolation(String subject, String ontologyrelation,int max) {
        return "The relation '" + ontologyrelation + "' in '"+subject+"' should exist at most '" + max + "' times.";
    }

    public static String exactCardinalityViolation(String subject, String ontologyrelation,int exact) {
        return "The relation '" + ontologyrelation + "' in '"+subject+"'  should exist exactly '" + exact + "' times.";
    }
}
