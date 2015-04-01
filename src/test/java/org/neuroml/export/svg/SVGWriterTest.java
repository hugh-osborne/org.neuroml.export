package org.neuroml.export.svg;

import java.io.IOException;

import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.AppTest;
import org.neuroml.export.UtilsTest;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

public class SVGWriterTest extends TestCase
{

    public void testGetMainScript() throws LEMSException, IOException, GenerationException, NeuroMLException, ModelFeatureSupportException, NeuroMLException
    {

        String exampleFilename = "L23PyrRS.nml";

        String content = JUtil.getRelativeResource(this.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR + "/" + exampleFilename);
        NeuroMLConverter nmlc = new NeuroMLConverter();
        NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);

        SVGWriter sw = new SVGWriter(nmlDocument, AppTest.getTempDir(), exampleFilename.replaceAll("nml", "svg"));

        UtilsTest.checkConvertedFiles(sw.convert());

    }
}
