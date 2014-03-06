/**
 * 
 */
package org.neuroml.export.base;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.neuroml.export.Utils;
import org.neuroml.export.neuron.NeuronWriter;
import org.neuroml.model.BiophysicalProperties;
import org.neuroml.model.Cell;
import org.neuroml.model.ChannelDensity;
import org.neuroml.model.ChannelDensityGHK;
import org.neuroml.model.ChannelDensityNernst;
import org.neuroml.model.InitMembPotential;
import org.neuroml.model.IntracellularProperties;
import org.neuroml.model.Member;
import org.neuroml.model.MembraneProperties;
import org.neuroml.model.Morphology;
import org.neuroml.model.Point3DWithDiam;
import org.neuroml.model.Resistivity;
import org.neuroml.model.Segment;
import org.neuroml.model.SegmentGroup;
import org.neuroml.model.Species;
import org.neuroml.model.SpecificCapacitance;
import org.neuroml.model.util.NeuroMLException;

/**
 * @author boris
 *
 */
public class JSONCellSerializer {

	public static String cellToJson(Cell cell, NeuronWriter.SupportedUnits units) throws ContentError, ParseError, IOException, NeuroMLException
	{
		JsonFactory f = new JsonFactory();
		StringWriter sw = new StringWriter();
		JsonGenerator g = f.createJsonGenerator(sw);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();  
	
		g.writeStringField("id", cell.getId());
		if (cell.getNotes()!=null && cell.getNotes().length()>0)
		{
			g.writeStringField("notes", cell.getNotes());
		}
	
		g.writeArrayFieldStart("sections");
		Morphology morph = cell.getMorphology();
		HashMap<Integer, String> idsVsNames = new HashMap<Integer, String>();
	
		for (Segment seg: morph.getSegment()) {
	
			g.writeStartObject();
			String name = NeuronWriter.getNrnSectionName(seg);
			idsVsNames.put(seg.getId(), name);
			//g.writeObjectFieldStart(name);
			g.writeStringField("name",name);
			g.writeStringField("id",seg.getId()+"");
			String parent = (seg.getParent()==null || seg.getParent().getSegment()==null) ? "-1" : idsVsNames.get(seg.getParent().getSegment());
			g.writeStringField("parent",parent);
			String comments = null;
			g.writeArrayFieldStart("points3d");
			Point3DWithDiam p0 = seg.getDistal();
			g.writeString(String.format("%g, %g, %g, %g", p0.getX(), p0.getY(), p0.getZ(), p0.getDiameter()));
			Point3DWithDiam p1 = seg.getProximal();
			if (p0.getX() == p1.getX() &&
					p0.getY() == p1.getY() &&
					p0.getZ() == p1.getZ() &&
					p0.getDiameter() == p1.getDiameter())
			{
				comments = "Section in NeuroML is spherical, so using cylindrical section along Y axis in NEURON";
				p1.setY(p0.getDiameter());
			}
			g.writeString(String.format("%g, %g, %g, %g", p1.getX(), p1.getY(), p1.getZ(), p1.getDiameter()));
			g.writeEndArray();
			if (comments!=null)
				g.writeStringField("comments",comments);
	
			//g.writeEndObject();
			g.writeEndObject();
		}
	
		g.writeEndArray();
	
		g.writeArrayFieldStart("groups");
		boolean foundAll = false;
		for (SegmentGroup grp: morph.getSegmentGroup()) {
			g.writeStartObject();
			g.writeStringField("name", grp.getId());
			foundAll = grp.getId().equals("all");
			if (!grp.getMember().isEmpty()) {
				g.writeArrayFieldStart("sections");
				for (Member m: grp.getMember()) {
					g.writeString(idsVsNames.get(m.getSegment()));
				}
				g.writeEndArray();
			}
			if (!grp.getInclude().isEmpty()) {
				g.writeArrayFieldStart("groups");
				for (org.neuroml.model.Include inc: grp.getInclude()) {
					g.writeString(inc.getSegmentGroup());
				}
				g.writeEndArray();
			}
			g.writeEndObject();
		}
		if (!foundAll) {
			g.writeStartObject();
			g.writeStringField("name", "all");
			g.writeArrayFieldStart("sections");
			for (Segment seg: morph.getSegment()) {
				String name = NeuronWriter.getNrnSectionName(seg);
				g.writeString(name);
			}
			g.writeEndArray();
			g.writeEndObject();
	
		}
		g.writeEndArray();
	
	
		BiophysicalProperties bp = cell.getBiophysicalProperties();
		g.writeArrayFieldStart("specificCapacitance");
		MembraneProperties mp = bp.getMembraneProperties();
		for (SpecificCapacitance sc: mp.getSpecificCapacitance()) {
			g.writeStartObject();
			String group = sc.getSegmentGroup()==null ? "all" : sc.getSegmentGroup();
			g.writeStringField("group",group);
			float value = Utils.getMagnitudeInSI(sc.getValue()) * units.specCapFactor;
			g.writeStringField("value",NeuronWriter.formatDefault(value));
			g.writeEndObject();
	
		}
		g.writeEndArray();
	
		g.writeArrayFieldStart("initMembPotential");
	
		for (InitMembPotential imp: mp.getInitMembPotential()) {
			g.writeStartObject();
			String group = imp.getSegmentGroup()==null ? "all" : imp.getSegmentGroup();
			g.writeStringField("group",group);
			float value = Utils.getMagnitudeInSI(imp.getValue()) * units.voltageFactor;
			g.writeStringField("value",NeuronWriter.formatDefault(value));
			g.writeEndObject();
	
		}
		g.writeEndArray();
	
		g.writeArrayFieldStart("resistivity");
		IntracellularProperties ip = bp.getIntracellularProperties();
		for (Resistivity res: ip.getResistivity()) {
			g.writeStartObject();
			String group = res.getSegmentGroup()==null ? "all" : res.getSegmentGroup();
			g.writeStringField("group",group);
	
			float value = Utils.getMagnitudeInSI(res.getValue()) * units.resistivityFactor;
			g.writeStringField("value",NeuronWriter.formatDefault(value));
			g.writeEndObject();
	
		}
		g.writeEndArray();
	
	
		g.writeArrayFieldStart("channelDensity");
		for (ChannelDensity cd: mp.getChannelDensity()) {
			g.writeStartObject();
			g.writeStringField("id",cd.getId());
			g.writeStringField("ionChannel",cd.getIonChannel());
	
			if (cd.getIon()!=null) {
				g.writeStringField("ion",cd.getIon());
			} else {
				g.writeStringField("ion","non_specific");
			}
	
			String group = cd.getSegmentGroup()==null ? "all" : cd.getSegmentGroup();
			g.writeStringField("group",group);
	
			float valueCondDens = Utils.getMagnitudeInSI(cd.getCondDensity())*units.condDensFactor;
			g.writeStringField("condDens",NeuronWriter.formatDefault(valueCondDens));
	
			float valueErev = Utils.getMagnitudeInSI(cd.getErev())*units.voltageFactor;
			g.writeStringField("erev",NeuronWriter.formatDefault(valueErev));
	
			g.writeEndObject();
		}
		for (ChannelDensityNernst cdn: mp.getChannelDensityNernst()) {
			g.writeStartObject();
			g.writeStringField("id",cdn.getId());
			g.writeStringField("ionChannel",cdn.getIonChannel());
	
			g.writeStringField("ion",cdn.getIon());
	
	
			String group = cdn.getSegmentGroup()==null ? "all" : cdn.getSegmentGroup();
			g.writeStringField("group",group);
	
			float valueCondDens = Utils.getMagnitudeInSI(cdn.getCondDensity())*units.condDensFactor;
			g.writeStringField("condDens",NeuronWriter.formatDefault(valueCondDens));
	
			g.writeStringField("erev","calculated_by_Nernst_equation");
	
			g.writeEndObject();
		}
		for (ChannelDensityGHK cdg: mp.getChannelDensityGHK()) {
			g.writeStartObject();
			g.writeStringField("id",cdg.getId());
			g.writeStringField("ionChannel",cdg.getIonChannel());
	
			g.writeStringField("ion",cdg.getIon());
	
	
			String group = cdg.getSegmentGroup()==null ? "all" : cdg.getSegmentGroup();
			g.writeStringField("group",group);
	
			float valuePermeab = Utils.getMagnitudeInSI(cdg.getPermeability())*units.permeabilityFactor;
			g.writeStringField("permeability",NeuronWriter.formatDefault(valuePermeab));
	
			g.writeStringField("erev","calculated_by_GHK_equation");
	
			g.writeEndObject();
		}
	
	
		g.writeEndArray();
	
	
		g.writeArrayFieldStart("species");
		for (Species sp: ip.getSpecies()) {
			g.writeStartObject();
			g.writeStringField("id",sp.getId());
			g.writeStringField("ion",sp.getIon());
			g.writeStringField("concentrationModel",sp.getConcentrationModel());
	
	
			String group = sp.getSegmentGroup()==null ? "all" : sp.getSegmentGroup();
			g.writeStringField("group",group);
	
			float initConc = Utils.getMagnitudeInSI(sp.getInitialConcentration())*units.concentrationFactor;
			g.writeStringField("initialConcentration",NeuronWriter.formatDefault(initConc));
			float initExtConc = Utils.getMagnitudeInSI(sp.getInitialExtConcentration())*units.concentrationFactor;
			g.writeStringField("initialExtConcentration",NeuronWriter.formatDefault(initExtConc));
	
	
			g.writeEndObject();
		}
		g.writeEndArray();
	
	
	
		g.writeEndObject();
		g.close();
		System.out.println(sw.toString());
	
		return sw.toString();
	
	}

}