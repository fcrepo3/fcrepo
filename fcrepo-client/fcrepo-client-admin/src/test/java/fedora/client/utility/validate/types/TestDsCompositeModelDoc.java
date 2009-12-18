/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.types;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import fedora.client.utility.validate.InvalidContentModelException;
import fedora.client.utility.validate.types.DsCompositeModelDoc.DsTypeModel;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Jim Blake
 */
public class TestDsCompositeModelDoc {

    public static final String TYPICAL_MODEL =
            "<dsCompositeModel xmlns=\"info:fedora/fedora-system:def/dsCompositeModel#\">\n"
                    + "   <dsTypeModel ID=\"DC\">\n"
                    + "       <form MIME=\"text/xml\">\n"
                    + "       </form>\n"
                    + "   </dsTypeModel>\n"
                    + "   <dsTypeModel ID=\"RELS-EXT\">\n"
                    + "       <form MIME=\"application/rdf+xml\">\n"
                    + "       </form>\n"
                    + "   </dsTypeModel>\n"
                    + "   <dsTypeModel ID=\"XML_SOURCE\">\n"
                    + "       <form MIME=\"text/xml\">\n"
                    + "       </form>\n"
                    + "   </dsTypeModel>\n"
                    + "   <dsTypeModel ID=\"XSL_STYLESHEET1\">\n"
                    + "       <form MIME=\"text/xml\">\n"
                    + "       </form>\n"
                    + "   </dsTypeModel>\n"
                    + "   <dsTypeModel ID=\"XSL_STYLESHEET2\">\n"
                    + "       <form MIME=\"text/xml\">\n"
                    + "       </form>\n"
                    + "   </dsTypeModel>\n" + "</dsCompositeModel>";

    public static final ExpectedTypeModel[] TYPICAL_RESULTS =
            new ExpectedTypeModel[] {
                    new ExpectedTypeModel("DC",
                                          new ExpectedForm[] {new ExpectedForm("text/xml",
                                                                               null)}),
                    new ExpectedTypeModel("RELS-EXT",
                                          new ExpectedForm[] {new ExpectedForm("application/rdf+xml",
                                                                               null)}),
                    new ExpectedTypeModel("XML_SOURCE",
                                          new ExpectedForm[] {new ExpectedForm("text/xml",
                                                                               null)}),
                    new ExpectedTypeModel("XSL_STYLESHEET1",
                                          new ExpectedForm[] {new ExpectedForm("text/xml",
                                                                               null)}),
                    new ExpectedTypeModel("XSL_STYLESHEET2",
                                          new ExpectedForm[] {new ExpectedForm("text/xml",
                                                                               null)}),};

    public static final String ASSORTED_FORMS =
            "<dsCompositeModel xmlns=\"info:fedora/fedora-system:def/dsCompositeModel#\">\n"
                    + "   <dsTypeModel ID=\"NoForms\">\n"
                    + "   </dsTypeModel>\n"
                    + "   <dsTypeModel ID=\"MultipleForms\">\n"
                    + "       <form MIME=\"application/rdf+xml\" FORMAT_URI=\"someUri\"/>\n"
                    + "       <form MIME=\"text/xml\"/>\n"
                    + "       <form FORMAT_URI=\"otherUri\"/>\n"
                    + "       <form/>\n"
                    + "   </dsTypeModel>\n"
                    + "</dsCompositeModel>";

    public static final ExpectedTypeModel[] ASSORTED_RESULTS =
            new ExpectedTypeModel[] {
                    new ExpectedTypeModel("MultipleForms", new ExpectedForm[] {
                            new ExpectedForm("application/rdf+xml", "someUri"),
                            new ExpectedForm("text/xml", null),
                            new ExpectedForm(null, "otherUri"),
                            new ExpectedForm(null, null)}),
                    new ExpectedTypeModel("NoForms", new ExpectedForm[] {})};

    public static final String EMPTY_MODEL =
            "<dsCompositeModel xmlns=\"info:fedora/fedora-system:def/dsCompositeModel#\">\n"
                    + "</dsCompositeModel>";

    public static final ExpectedTypeModel[] EMPTY_RESULTS =
            new ExpectedTypeModel[0];

    public static final String BROKEN_TYPE_MODEL =
            "<dsCompositeModel xmlns=\"info:fedora/fedora-system:def/dsCompositeModel#\">\n"
                    + "   <dsTypeModel >\n"
                    + "   </dsTypeModel>\n"
                    + "</dsCompositeModel>";

    @Test
    public void typicalModel() throws InvalidContentModelException {
        DsCompositeModelDoc doc =
                new DsCompositeModelDoc("junkPid", TYPICAL_MODEL.getBytes());
        DsTypeModel[] actualModels = getSortedTypeModelsArray(doc);
        assertExpectedResults(TYPICAL_RESULTS, actualModels);
    }

    @Test
    public void assortedForms() throws InvalidContentModelException {
        DsCompositeModelDoc doc =
                new DsCompositeModelDoc("junkPid", ASSORTED_FORMS.getBytes());
        DsTypeModel[] actualModels = getSortedTypeModelsArray(doc);
        assertExpectedResults(ASSORTED_RESULTS, actualModels);
    }

    @Test
    public void emptyModel() throws InvalidContentModelException {
        DsCompositeModelDoc doc =
                new DsCompositeModelDoc("junkPid", EMPTY_MODEL.getBytes());
        DsTypeModel[] actualModels = getSortedTypeModelsArray(doc);
        assertExpectedResults(EMPTY_RESULTS, actualModels);
    }

    @Test(expected = InvalidContentModelException.class)
    public void typeModelHasNoId() throws InvalidContentModelException {
        new DsCompositeModelDoc("junkPid", BROKEN_TYPE_MODEL.getBytes());
    }

    private void assertExpectedResults(ExpectedTypeModel[] expectedModels,
                                       DsTypeModel[] actualModels) {
        assertEquals("number of models",
                     expectedModels.length,
                     actualModels.length);
        for (int i = 0; i < actualModels.length; i++) {
            DsTypeModel actualModel = actualModels[i];
            String actualId = actualModel.getId();
            assertEquals("id", expectedModels[i].id, actualId);
            assertEqualForms(actualId, expectedModels[i].forms, actualModel
                    .getForms());
        }
    }

    // No type models
    // No forms in a type model
    // Multiple forms in a type model
    // Form with neither Mime nor FormatUri
    // Form with either or both Mime or FormatUri

    private DsTypeModel[] getSortedTypeModelsArray(DsCompositeModelDoc doc) {
        Set<ContentModelInfo.DsTypeModel> tmSet = doc.getTypeModels();
        DsTypeModel[] result = tmSet.toArray(new DsTypeModel[tmSet.size()]);
        Arrays.sort(result, new Comparator<DsTypeModel>() {

            public int compare(DsTypeModel obj1, DsTypeModel obj2) {
                return obj1.getId().compareTo(obj2.getId());
            }
        });
        return result;
    }

    private void assertEqualForms(String dsId,
                                  ExpectedForm[] expected,
                                  Set<ContentModelInfo.Form> actual) {
        Set<ContentModelInfo.Form> extras =
                new HashSet<ContentModelInfo.Form>(actual);
        Set<ExpectedForm> missing = new HashSet<ExpectedForm>();

        outer: for (int i = 0; i < expected.length; i++) {
            ExpectedForm expectedForm = expected[i];
            for (Iterator<ContentModelInfo.Form> actuals = extras.iterator(); actuals
                    .hasNext();) {
                ContentModelInfo.Form actualForm = actuals.next();
                if (equivalent(expectedForm.mime, actualForm.getMimeType())
                        && equivalent(expectedForm.formatUri, actualForm
                                .getFormatUri())) {
                    actuals.remove();
                    continue outer;
                }
            }
            missing.add(expectedForm);
        }
        assertTrue("dsId: " + dsId + ", unexpected forms: " + extras
                + "\nmissing forms: " + missing, missing.isEmpty()
                && extras.isEmpty());
    }

    private boolean equivalent(Object obj1, Object obj2) {
        return (obj1 == null) ? obj2 == null : obj1.equals(obj2);
    }

    private static class ExpectedTypeModel {

        public final String id;

        public final ExpectedForm[] forms;

        public ExpectedTypeModel(String id, ExpectedForm[] forms) {
            this.id = id;
            this.forms = forms;
        }

    }

    private static class ExpectedForm {

        public final String mime;

        public final String formatUri;

        public ExpectedForm(String mime, String formatUri) {
            this.mime = mime;
            this.formatUri = formatUri;
        }

        @Override
        public String toString() {
            return "ExpectedForm[mime=" + mime + ", formatUri=" + formatUri
                    + "]";
        }

    }
}
