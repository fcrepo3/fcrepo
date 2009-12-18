/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate;

import java.util.Collection;

import fedora.client.utility.validate.types.BasicObjectInfo;
import fedora.client.utility.validate.types.ContentModelInfo;
import fedora.client.utility.validate.types.DatastreamInfo;
import fedora.client.utility.validate.types.ObjectInfo;
import fedora.client.utility.validate.types.RelationshipInfo;
import fedora.client.utility.validate.types.ContentModelInfo.DsTypeModel;
import fedora.client.utility.validate.types.ContentModelInfo.Form;

import fedora.common.Constants;

/**
 * This is the actual validation engine, performing validation tests on an
 * {@link ObjectInfo}. The engine is provided with an {@link ObjectSource} at
 * instantiation time, in case additional objects are required to complete the
 * validation.
 * 
 * @author Jim Blake
 */
public class ObjectValidator {

    private final ObjectSource objectSource;

    public ObjectValidator(ObjectSource objectSource) {
        if (objectSource == null) {
            throw new NullPointerException("objectSource may not be null.");
        }
        this.objectSource = objectSource;
    }

    /**
     * If we only have a PID, we can get the object from the
     * {@link ObjectSource}. But if that fails, we need to mock up an object
     * for the result.
     */
    public ValidationResult validate(String pid) {
        if (pid == null) {
            throw new NullPointerException("pid may not be null.");
        }

        ObjectInfo object = null;

        try {
            object = objectSource.getValidationObject(pid);
        } catch (ObjectSourceException e) {
            // This falls into the case of object==null.
        }

        if (object == null) {
            ValidationResult result =
                    new ValidationResult(new BasicObjectInfo(pid));
            result.addNote(ValidationResultNotation.objectNotFound(pid));
            return result;
        } else {
            return validate(object);
        }
    }

    /**
     * Each object is expected to have at least one content model. Validate each
     * of the content models.
     */
    public ValidationResult validate(ObjectInfo object) {
        if (object == null) {
            throw new NullPointerException("object may not be null.");
        }

        ValidationResult result = new ValidationResult(object);

        Collection<String> contentmodels = object.getContentModels();
        if (contentmodels.size() == 0){
            result.addNote(ValidationResultNotation.noContentModel());
            return result;
        }

        for (String contentmodel:contentmodels){
            validateAgainstContentModel(result, contentmodel, object);
        }

        return result;
    }

    /**
     * <p>
     * Validate each content model relation.
     * </p>
     * <p>
     * If the content model relation doesn't point to an object PID, note that
     * and give up. Same if no object is found at that PID.
     * </p>
     * <p>
     * If we find an actual content model object, verify that the original
     * object satisfies the content model.
     * </p>
     */
    private void validateAgainstContentModel(ValidationResult result,
                                             String cm,
                                             ObjectInfo object) {
        String contentModelPid;
        if (cm.startsWith("info:fedora/")){
            contentModelPid = cm.substring(12);
        } else{
            result.addNote(ValidationResultNotation
                    .unrecognizedContentModelUri(cm));
            return;
        }

        try {
            ContentModelInfo contentModel =
                    objectSource.getContentModelInfo(contentModelPid);
            if (contentModel == null) {
                result.addNote(ValidationResultNotation
                        .contentModelNotFound(contentModelPid));
            } else {
                for (DsTypeModel typeModel : contentModel.getTypeModels()) {
                    confirmMatchForDsTypeModel(result,
                                               typeModel,
                                               contentModelPid,
                                               object);
                }
            }
        } catch (ObjectSourceException e) {
            result.addNote(ValidationResultNotation
                    .errorFetchingContentModel(contentModelPid, e));
            return;
        } catch (InvalidContentModelException e) {
            result.addNote(ValidationResultNotation.contentModelNotValid(e));
        }
    }

    /**
     * The object must have a datastream to match each dsTypeModel in the
     * content model. Matching a dsTypeModel means equal IDs and an acceptable
     * form.
     */
    private void confirmMatchForDsTypeModel(ValidationResult result,
                                            DsTypeModel typeModel,
                                            String contentModelPid,
                                            ObjectInfo object) {
        String id = typeModel.getId();
        DatastreamInfo dsInfo = object.getDatastreamInfo(id);
        if (dsInfo == null) {
            // If there is no datastream by that name, nothing to check.
            result.addNote(ValidationResultNotation
                    .noMatchingDatastreamId(contentModelPid, id));
            return;
        }

        Collection<Form> forms = typeModel.getForms();
        if (forms.isEmpty()) {
            // If the type model has no forms, it's an automatic match.
            return;
        }

        // Otherwise, the datastream must meet the constraints of at least one form.
        for (Form form : forms) {
            if (meetsConstraint(dsInfo.getMimeType(), form.getMimeType())
                    && meetsConstraint(dsInfo.getFormatUri(), form
                            .getFormatUri())) {
                return;
            }
        }
        result.addNote(ValidationResultNotation
                .datastreamDoesNotMatchForms(contentModelPid, id));
    }

    /**
     * If the constraint is not null, the value must match it.
     */
    private boolean meetsConstraint(String value, String constraint) {
        return (constraint == null) || constraint.equals(value);
    }
}
