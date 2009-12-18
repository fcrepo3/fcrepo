/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import mock.fedora.client.utility.validate.MockObjectSource;

import org.junit.Before;
import org.junit.Test;

import fedora.client.utility.validate.types.BasicObjectInfo;
import fedora.client.utility.validate.types.ContentModelInfo;
import fedora.client.utility.validate.types.DatastreamInfo;
import fedora.client.utility.validate.types.ObjectInfo;
import fedora.client.utility.validate.types.RelationshipInfo;
import fedora.client.utility.validate.types.ContentModelInfo.DsTypeModel;
import fedora.client.utility.validate.types.ContentModelInfo.Form;

import fedora.common.Constants;

import static junit.framework.Assert.assertEquals;

/**
 * Testing to see that the {@link ObjectValidator} actually validates.
 * 
 * @author Jim Blake
 */
public class TestObjectValidator {

    /*
     * Some useful shorthand constants.
     */
    private static final String HAS_MODEL = Constants.MODEL.HAS_MODEL.uri;

    private static final DatastreamInfo[] NO_DATASTREAMS =
            new DatastreamInfo[0];

    private static final RelationshipInfo[] NO_RELATIONS =
            new RelationshipInfo[0];

    private static final DsTypeModel[] NO_TYPE_MODELS = new DsTypeModel[0];

    // Every content model must have this datastream.
    private static final DatastreamInfo[] CONTENT_MODEL_DATASTREAM =
            new DatastreamInfo[] {new DatastreamInfo(ContentModelInfo.DS_COMPOSITE_MODEL,
                                                     null,
                                                     ContentModelInfo.DS_COMPOSITE_MODEL_FORMAT)};

    /*
     * 
     */
    private static final String SAMPLE_PID = "throwIt";

    private static final String NON_PID_URI = "not_a_pid";

    /*
     * A simple object and content model for use in several tests.
     */
    private static final TestContentModelInfo CONTENT_MODEL_EMPTY =
            contentModel("emptyContentModel", NO_TYPE_MODELS);

    private static final BasicObjectInfo OBJECT_SIMPLE_SAMPLE =
            basicObject("objectSimpleSample",
                        contentModelRelations(CONTENT_MODEL_EMPTY),
                        NO_DATASTREAMS);

    private MockObjectSource objectSource;

    private ObjectValidator validator;

    /**
     * Create the object source and the validator. Add some simple objects to
     * the source for use in several tests.
     */
    @Before
    public void initializeSourceAndValidator() {
        objectSource = new MockObjectSource();

        addSeedsToObjectSource(CONTENT_MODEL_EMPTY, OBJECT_SIMPLE_SAMPLE);

        validator = new ObjectValidator(objectSource);
    }

    @Test(expected = NullPointerException.class)
    public void nullArgumentToConstructor() {
        new ObjectValidator(null);
    }

    @Test(expected = NullPointerException.class)
    public void nullArgumentToValidatePid() {
        validator.validate((String) null);
    }

    @Test
    public void gettingFromPidThrowsException() {
        objectSource.throwObjectSourceExceptionOnPid(SAMPLE_PID);

        ValidationResult expected =
                expectedResult(new BasicObjectInfo(SAMPLE_PID),
                               ValidationResultNotation
                                       .objectNotFound(SAMPLE_PID));

        ValidationResult actual = validator.validate(SAMPLE_PID);
        assertEquals("result", expected, actual);
    }

    @Test
    public void pidReturnsNullObject() {
        ValidationResult expected =
                expectedResult(new BasicObjectInfo(SAMPLE_PID),
                               ValidationResultNotation
                                       .objectNotFound(SAMPLE_PID));

        ValidationResult actual = validator.validate(SAMPLE_PID);
        assertEquals("result", expected, actual);
    }

    @Test
    public void simpleSuccessFromPid() {
        ValidationResult expected = expectedResult(OBJECT_SIMPLE_SAMPLE);
        ValidationResult actual =
                validator.validate(OBJECT_SIMPLE_SAMPLE.getPid());
        assertEquals("result", expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void nullArgumentToValidateObject() {
        validator.validate((ObjectInfo) null);
    }

    @Test
    public void simpleSuccessFromObject() {
        validateObject(OBJECT_SIMPLE_SAMPLE);
    }

    @Test
    public void noContentModel() {
        BasicObjectInfo object =
                basicObject("noContentModel", NO_RELATIONS, NO_DATASTREAMS);
        validateObject(object, ValidationResultNotation.noContentModel());
    }

    @Test
    public void contentModelUriIsNotPid() {
        BasicObjectInfo object =
                basicObject("unknownContentModel",
                            unknownContentModelRelation(),
                            NO_DATASTREAMS);
        validateObject(object, ValidationResultNotation
                .unrecognizedContentModelUri(NON_PID_URI));
    }

    @Test
    public void gettingContentModelThrowsException() {
        objectSource.throwObjectSourceException(CONTENT_MODEL_EMPTY);
        validateObject(OBJECT_SIMPLE_SAMPLE,
                       noteErrorFetchingContentModel(CONTENT_MODEL_EMPTY));
    }

    @Test
    public void contentModelIsInvalid() {
        objectSource.throwInvalidContentModelException(CONTENT_MODEL_EMPTY);
        validateObject(OBJECT_SIMPLE_SAMPLE,
                       noteInvalidContentModel(CONTENT_MODEL_EMPTY));
    }

    @Test
    public void contentModelDoesntExist() {
        objectSource.removeSeedModel(CONTENT_MODEL_EMPTY);
        validateObject(OBJECT_SIMPLE_SAMPLE, ValidationResultNotation
                .contentModelNotFound(CONTENT_MODEL_EMPTY.getPid()));
    }

    /**
     * Content model requires a datastream, but the object doesn't have it.
     */
    @Test
    public void noDsToMatchTypeModel() {
        TypeModel typeNoForms = new TypeModel(new HashSet<Form>(), "dsNoForms");
        TestContentModelInfo model =
                contentModel("oneTypeContentModel",
                             new DsTypeModel[] {typeNoForms});
        BasicObjectInfo object =
                basicObject("objectNoDsForModel",
                            contentModelRelations(model),
                            NO_DATASTREAMS);

        addSeedsToObjectSource(model, object);
        validateObject(object, ValidationResultNotation
                .noMatchingDatastreamId(model.getPid(), typeNoForms.getId()));
    }

    /**
     * Match to a content model with four datastreams, illustrating matches
     * against an assortment of types.
     */
    @Test
    public void matchAnAssortmentOfTypeModels() {
        TestForm formNeither = new TestForm(null, null);
        TestForm formMime = new TestForm(null, "mime");
        TestForm formFormat = new TestForm("format_uri", null);
        TestForm formBoth = new TestForm("both_format_uri", "both_mime");
        TypeModel typeNeither = typeModel("neither", formNeither);
        TypeModel typeMime = typeModel("mime only", formMime);
        TypeModel typeFormat = typeModel("format_uri only", formFormat);
        TypeModel typeBoth = typeModel("both", formBoth);
        TestContentModelInfo model =
                contentModel("model", typeModels(typeNeither,
                                                 typeMime,
                                                 typeFormat,
                                                 typeBoth));

        DatastreamInfo dsNeither = new DatastreamInfo("neither", null, null);
        DatastreamInfo dsMime = new DatastreamInfo("mime only", "mime", null);
        DatastreamInfo dsFormat =
                new DatastreamInfo("format_uri only", null, "format_uri");
        DatastreamInfo dsBoth =
                new DatastreamInfo("both", "both_mime", "both_format_uri");
        BasicObjectInfo matcher =
                basicObject("severalDatastreamAllMatch",
                            contentModelRelations(model),
                            datastreams(dsNeither, dsMime, dsFormat, dsBoth));

        addSeedsToObjectSource(model, matcher);
        validateObject(matcher);
    }

    @Test
    public void matchAgainstTypeWithNeither() {
        // This model has one type, with neither mime nor format URI specified.
        TypeModel typeNeither = typeModel("neither", new TestForm(null, null));
        TestContentModelInfo model =
                contentModel("model", typeModels(typeNeither));

        BasicObjectInfo neither =
                basicObject("neitherMatchesNeither",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("neither",
                                                           null,
                                                           null)));

        BasicObjectInfo mime =
                basicObject("anyMimeMatchesNeither",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("neither",
                                                           "wrongMime",
                                                           null)));

        BasicObjectInfo formatUri =
                basicObject("anyFormatMatchesNeither",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("neither",
                                                           null,
                                                           "wrongFormat")));

        addSeedsToObjectSource(model, neither, mime, formatUri);
        validateObject(neither);
        validateObject(mime);
        validateObject(formatUri);
    }

    @Test
    public void matchAgainstTypeWithMime() {
        // This model has one type, with mime specified but not format uri.
        TypeModel typeMime = typeModel("mime", new TestForm(null, "mimeType"));
        TestContentModelInfo model =
                contentModel("model", typeModels(typeMime));

        BasicObjectInfo neither =
                basicObject("neitherFailsOnMime",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("mime", null, null)));

        BasicObjectInfo mime =
                basicObject("mimeMatch",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("mime",
                                                           "mimeType",
                                                           null)));

        BasicObjectInfo wrongMime =
                basicObject("mimeMisMatch",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("mime",
                                                           "wrongMime",
                                                           null)));

        ValidationResultNotation note =
                ValidationResultNotation.datastreamDoesNotMatchForms(model
                        .getPid(), "mime");

        addSeedsToObjectSource(model, neither, mime, wrongMime);
        validateObject(neither, note);
        validateObject(mime);
        validateObject(wrongMime, note);
    }

    @Test
    public void matchAgainstTypeWithBoth() {
        // This model has one type, with both mime and format uri specified.
        TypeModel typeBoth =
                typeModel("both", new TestForm("formatUri", "mimeType"));
        TestContentModelInfo model =
                contentModel("model", typeModels(typeBoth));

        BasicObjectInfo neither =
                basicObject("neitherFails",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("both", null, null)));

        BasicObjectInfo mimeOnly =
                basicObject("mimeFails",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("both",
                                                           "mimeType",
                                                           null)));

        BasicObjectInfo fuOnly =
                basicObject("fuFails",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("both",
                                                           null,
                                                           "formatUri")));

        BasicObjectInfo both =
                basicObject("bothMatch",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("both",
                                                           "mimeType",
                                                           "formatUri")));

        BasicObjectInfo wrongMime =
                basicObject("mimeMismatch",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("both",
                                                           "wrongMime",
                                                           "formatUri")));

        BasicObjectInfo wrongFu =
                basicObject("fuMismatch",
                            contentModelRelations(model),
                            datastreams(new DatastreamInfo("both",
                                                           "mime",
                                                           "wrongFormatUri")));

        ValidationResultNotation note =
                ValidationResultNotation.datastreamDoesNotMatchForms(model
                        .getPid(), "both");

        addSeedsToObjectSource(model,
                               neither,
                               mimeOnly,
                               fuOnly,
                               both,
                               wrongMime,
                               wrongFu);
        validateObject(neither, note);
        validateObject(mimeOnly, note);
        validateObject(fuOnly, note);
        validateObject(both);
        validateObject(wrongMime, note);
        validateObject(wrongFu, note);
    }

    /**
     * Match against two content models - they both require one datastream, and
     * they each require another.
     */
    @Test
    public void matchTwoContentModels() {
        TestContentModelInfo model1 =
                contentModel("model1",
                             typeModels(typeModel("dsBoth",
                                                  new TestForm(null, "mimeA"),
                                                  new TestForm(null, "mimeB")),
                                        typeModel("ds1",
                                                  new TestForm("formatA", null))));

        TestContentModelInfo model2 =
                contentModel("model2",
                             typeModels(typeModel("ds2",
                                                  new TestForm("formatY", null)),
                                        typeModel("dsBoth",
                                                  new TestForm("formatX", null))));

        DatastreamInfo ds1Pass = new DatastreamInfo("ds1", null, "formatA");
        DatastreamInfo ds1Fail = new DatastreamInfo("ds1", null, null);
        DatastreamInfo ds2Pass = new DatastreamInfo("ds2", "mimeK", "formatY");
        DatastreamInfo ds2Fail = new DatastreamInfo("ds2", "mimeK", "formatZ");
        DatastreamInfo dsBothPass =
                new DatastreamInfo("dsBoth", "mimeB", "formatX");
        DatastreamInfo dsBothFail = new DatastreamInfo("dsBoth", "mimeB", null);

        BasicObjectInfo success =
                basicObject("success",
                            contentModelRelations(model1, model2),
                            datastreams(ds1Pass, ds2Pass, dsBothPass));

        BasicObjectInfo failDs1 =
                basicObject("failDs1",
                            contentModelRelations(model1, model2),
                            datastreams(ds1Fail, ds2Pass, dsBothPass));

        BasicObjectInfo failDs2 =
                basicObject("failDs2",
                            contentModelRelations(model1, model2),
                            datastreams(ds1Pass, ds2Fail, dsBothPass));

        BasicObjectInfo failDsBoth =
                basicObject("failDsBoth",
                            contentModelRelations(model1, model2),
                            datastreams(ds1Pass, ds2Pass, dsBothFail));

        ValidationResultNotation note1 =
                ValidationResultNotation.datastreamDoesNotMatchForms(model1
                        .getPid(), ds1Fail.getId());
        ValidationResultNotation note2 =
                ValidationResultNotation.datastreamDoesNotMatchForms(model2
                        .getPid(), ds2Fail.getId());
        ValidationResultNotation note3 =
                ValidationResultNotation.datastreamDoesNotMatchForms(model2
                        .getPid(), dsBothFail.getId());

        addSeedsToObjectSource(model1, success, failDs1, failDs2, failDsBoth);
        addSeedsToObjectSource(model2, success, failDs1, failDs2, failDsBoth);
        validateObject(success);
        validateObject(failDs1, note1);
        validateObject(failDs2, note2);
        validateObject(failDsBoth, note3);
    }

    /*
     * ------------------------------------------------------------------------
     * Helper methods
     * ------------------------------------------------------------------------
     */

    /**
     * Create a basic object from these specifications.
     */
    private static BasicObjectInfo basicObject(String pid,
                                               RelationshipInfo[] relations,
                                               DatastreamInfo[] datastreams) {
        return new BasicObjectInfo(pid, Arrays.asList(relations), Arrays
                .asList(datastreams));
    }

    /**
     * Create a content model from these specifications.
     */
    private static TestContentModelInfo contentModel(String pid,
                                                     DsTypeModel[] typeModels) {
        BasicObjectInfo base =
                basicObject(pid, NO_RELATIONS, CONTENT_MODEL_DATASTREAM);
        return new TestContentModelInfo(base, typeModels);
    }

    /**
     * Create relationships to these content models.
     */
    private static RelationshipInfo[] contentModelRelations(ContentModelInfo... models) {
        RelationshipInfo[] relations = new RelationshipInfo[models.length];
        for (int i = 0; i < models.length; i++) {
            String objectUri = "info:fedora/" + models[i].getPid();
            relations[i] = new RelationshipInfo(HAS_MODEL, objectUri);
        }
        return relations;
    }

    /**
     * Create a type model from these specifications.
     */
    private static TypeModel typeModel(String id, Form... forms) {
        return new TypeModel(Arrays.asList(forms), id);
    }

    /**
     * Create a relationship to a content model whose URI is not recognized as a
     * PID.
     */
    private static RelationshipInfo[] unknownContentModelRelation() {
        return new RelationshipInfo[] {new RelationshipInfo(HAS_MODEL,
                                                            NON_PID_URI)};
    }

    private static DatastreamInfo[] datastreams(DatastreamInfo... dsInfos) {
        return dsInfos;
    }

    private static DsTypeModel[] typeModels(DsTypeModel... types) {
        return types;
    }

    /**
     * Put the content model and the objects that comply to it into the mock
     * object source.
     */
    private void addSeedsToObjectSource(TestContentModelInfo model,
                                        BasicObjectInfo... objects) {
        for (BasicObjectInfo object : objects) {
            objectSource.addSeedObject(object);
        }
        objectSource.addSeedModel(model.getBasicObject(), model);
    }

    /**
     * If I validate this object, I should get these notes.
     */
    private void validateObject(BasicObjectInfo object,
                                ValidationResultNotation... expectedNotes) {
        ValidationResult expected = expectedResult(object, expectedNotes);
        ValidationResult actual = validator.validate(object);
        assertEquals("result", expected, actual);
    }

    /**
     * Assemble the expected {@link ValidationResult}.
     */
    private ValidationResult expectedResult(BasicObjectInfo object,
                                            ValidationResultNotation... notes) {
        ValidationResult result = new ValidationResult(object);
        for (ValidationResultNotation note : notes) {
            result.addNote(note);
        }
        return result;
    }

    /**
     * Convenience method: create a notation saying that we couldn't fetch this
     * content model.
     */
    private ValidationResultNotation noteErrorFetchingContentModel(ContentModelInfo model) {
        String pid = model.getPid();
        ObjectSourceException e = objectSource.createObjectSourceException(pid);
        return ValidationResultNotation.errorFetchingContentModel(pid, e);
    }

    /**
     * Convenience method: create a notation saying that the content model is
     * invalid.
     */
    private ValidationResultNotation noteInvalidContentModel(ContentModelInfo model) {
        String pid = model.getPid();
        InvalidContentModelException e =
                objectSource.createInvalidContentModelException(pid);
        return ValidationResultNotation.contentModelNotValid(e);
    }

    /*
     * ------------------------------------------------------------------------
     * Helper classes - simple implementations of the interfaces we use.
     * ------------------------------------------------------------------------
     */

    private static class TypeModel
            implements DsTypeModel {

        private final Collection<Form> forms;

        private final String id;

        public TypeModel(Collection<Form> forms, String id) {
            this.forms = forms;
            this.id = id;
        }

        public Collection<Form> getForms() {
            return forms;
        }

        public String getId() {
            return id;
        }
    }

    private static class TestContentModelInfo
            implements ContentModelInfo {

        private final BasicObjectInfo basicObject;

        private final List<DsTypeModel> typeModels;

        public TestContentModelInfo(BasicObjectInfo basicObject,
                                    DsTypeModel[] typeModels) {
            this.basicObject = basicObject;
            this.typeModels =
                    new ArrayList<DsTypeModel>(Arrays.asList(typeModels));
        }

        public String getPid() {
            return basicObject.getPid();
        }

        public Collection<DsTypeModel> getTypeModels() {
            return new HashSet<DsTypeModel>(typeModels);
        }

        public BasicObjectInfo getBasicObject() {
            return basicObject;
        }

    }

    private static class TestForm
            implements Form {

        private final String formatUri;

        private final String mimeType;

        public TestForm(String formatUri, String mimeType) {
            super();
            this.formatUri = formatUri;
            this.mimeType = mimeType;
        }

        public String getFormatUri() {
            return formatUri;
        }

        public String getMimeType() {
            return mimeType;
        }

    }
}
