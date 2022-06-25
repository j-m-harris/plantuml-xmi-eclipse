/*
 * Copyright (c) 2022 Johnathon Harris <jmharris@gmail.com>
 */
package net.sourceforge.plantuml.xmi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sourceforge.plantuml.classdiagram.ClassDiagram;
import net.sourceforge.plantuml.cucadiagram.Display;
import net.sourceforge.plantuml.xml.XmlFactories;

/**
 * Common logic for Eclipse compatible XMI export (UML2).
 * 
 * @author Johnathon Harris
 */
abstract class AbstractXmiEclipseDiagram implements IXmiClassDiagram {

	/**
	 * Output XML document.
	 */
	protected final Document document;
	/**
	 * Root UML Model element.
	 */
	protected final Element model;

	/**
	 * Lookup elements by xmi:id.
	 */
	private Map<String, Element> xmiElements = new HashMap<>();

	public AbstractXmiEclipseDiagram(ClassDiagram classDiagram) throws ParserConfigurationException {
		DocumentBuilder builder = XmlFactories.newDocumentBuilder();
		document = builder.newDocument();
		document.setXmlVersion("1.0");
		document.setXmlStandalone(true);

		model = document.createElement("uml:Model");
		model.setAttribute("xmi.id", CucaDiagramXmiMaker.getModel(classDiagram));
		model.setAttribute("xmi.version", "20131001");
		model.setAttribute("xmlns:xmi", "http://www.omg.org/spec/XMI/20131001");
		model.setAttribute("xmlns:uml", "http://www.eclipse.org/uml2/5.0.0/UML");
		model.setAttribute("xmlns:id", CucaDiagramXmiMaker.getModel(classDiagram));
		document.appendChild(model);
	}

	protected Element getXmiElement(String xmiId) {
		return xmiElements.get(xmiId);
	}

	protected void putXmiElement(String xmiId, Element element) {
		element.setAttribute("xmi:id", xmiId);
		xmiElements.put(xmiId, element);
	}

	@Override
	public void transformerXml(OutputStream outputStream) throws TransformerException, ParserConfigurationException {
		Source source = new DOMSource(document);
		Result result = new StreamResult(outputStream);
		Transformer transformer = XmlFactories.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, UTF_8.name());
		transformer.transform(source, result);
	}

	protected String forXMI(Display s) {
		if (Display.isNull(s)) {
			return "";
		}
		return s.get(0).toString().replace(':', ' ');
	}

}
