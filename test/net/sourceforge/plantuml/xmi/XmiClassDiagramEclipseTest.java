/*
 * Copyright (c) 2022 Johnathon Harris <jmharris@gmail.com>
 */
package net.sourceforge.plantuml.xmi;

import static net.sourceforge.plantuml.test.PlantUmlTestUtils.exportDiagram;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.utils.UniqueSequence;


class XmiClassDiagramEclipseTest {

	@Test
	public void test_class_generalization() throws Exception {

		UniqueSequence.reset();
		final String output = exportDiagram("@startuml",
				"ParentA <|-- ChildA",
				"package api {",
				"  class Sample",
				"  ParentB <|-- ChildB",
				"}",
				"@enduml").asString(FileFormat.XMI_ECLIPSE);

		assertThat(output).isEqualToIgnoringNewLines("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<uml:Model xmlns:uml=\"http://www.eclipse.org/uml2/5.0.0/UML\" xmlns:id=\"model1\" xmlns:xmi=\"http://www.omg.org/spec/XMI/20131001\" xmi.id=\"model1\" xmi.version=\"20131001\">"
				+ "    <packagedElement name=\"ParentA\" xmi:id=\"cl0002\" xmi:type=\"uml:Class\"/>"
				+ "    <packagedElement name=\"ChildA\" xmi:id=\"cl0003\" xmi:type=\"uml:Class\">"
				+ "        <generalization general=\"cl0002\" name=\"\" xmi:id=\"LNK4\" xmi:type=\"uml:Generalization\"/>"
				+ "    </packagedElement>"
				+ "    <packagedElement name=\"api\" xmi:id=\"96794\" xmi:type=\"uml:Package\">"
				+ "        <packagedElement name=\"Sample\" xmi:id=\"cl0006\" xmi:type=\"uml:Class\"/>"
				+ "        <packagedElement name=\"ParentB\" xmi:id=\"cl0007\" xmi:type=\"uml:Class\"/>"
				+ "        <packagedElement name=\"ChildB\" xmi:id=\"cl0008\" xmi:type=\"uml:Class\">"
				+ "            <generalization general=\"cl0007\" name=\"\" xmi:id=\"LNK9\" xmi:type=\"uml:Generalization\"/>"
				+ "        </packagedElement>"
				+ "    </packagedElement>"
				+ "</uml:Model>");
	}
}
