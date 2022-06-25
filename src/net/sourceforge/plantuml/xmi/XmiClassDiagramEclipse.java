/*
 * Copyright (c) 2022 Johnathon Harris <jmharris@gmail.com>
 */
package net.sourceforge.plantuml.xmi;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;

import net.sourceforge.plantuml.classdiagram.ClassDiagram;
import net.sourceforge.plantuml.cucadiagram.IEntity;
import net.sourceforge.plantuml.cucadiagram.LeafType;
import net.sourceforge.plantuml.cucadiagram.Link;
import net.sourceforge.plantuml.cucadiagram.LinkDecor;

/**
 * Converts class diagram to Eclipse compatible XMI export (UML2).
 * 
 * @author Johnathon Harris
 */
public class XmiClassDiagramEclipse extends AbstractXmiEclipseDiagram implements IXmiClassDiagram {

	public XmiClassDiagramEclipse(ClassDiagram classDiagram) throws ParserConfigurationException {
		super(classDiagram);
		try {
			for (IEntity leafEntity : classDiagram.getLeafsvalues()) {
				Element element = processLeaf(leafEntity);
				if (element != null) {
					model.appendChild(element);
				}
			}
			for (Link link : classDiagram.getLinks()) {
				processLink(link);
			}
		} catch (Exception e) {
			System.out.print("Failed to convert to XMI for eclipse: " + e.getMessage());
			e.printStackTrace(System.out);
			throw e;
		}
	}

	private Element processLeaf(IEntity entity) {
		if (entity.getLeafType() == LeafType.NOTE) {
			throw new UnsupportedOperationException("TODO implement note");

		} else {
			Element classElement = document.createElement("packagedElement");
			classElement.setAttribute("xmi:type", "uml:Class");
			classElement.setAttribute("name", entity.getDisplay().get(0).toString());
			putXmiElement(entity.getUid(), classElement);

			String parentCode = entity.getIdent().parent().forXmi();
			if (parentCode.length() > 0) {
				Element packageElement = getPackageElement(Integer.toString(parentCode.hashCode()), parentCode);
				packageElement.appendChild(classElement);
				return packageElement;
			} else {
				return classElement;
			}
		}
	}

	private Element getPackageElement(String xmiId, String name) {
		Element packageElement = getXmiElement(xmiId);
		if (packageElement == null) {
			packageElement = document.createElement("packagedElement");
			packageElement.setAttribute("xmi:type", "uml:Package");
			packageElement.setAttribute("name", name);
			putXmiElement(xmiId, packageElement);
		}
		return packageElement;
	}

	private void processLink(Link link) {
		if (link.getType().getDecor1() == LinkDecor.EXTENDS || link.getType().getDecor2() == LinkDecor.EXTENDS) {
			processGeneralization(link);
		}
	}

	private void processGeneralization(Link link) {
		Element subElement;
		Element superElement;
		if (link.getType().getDecor1() == LinkDecor.EXTENDS) {
			subElement = getXmiElement(link.getEntity1().getUid());
			superElement = getXmiElement(link.getEntity2().getUid());
		} else if (link.getType().getDecor2() == LinkDecor.EXTENDS) {
			subElement = getXmiElement(link.getEntity2().getUid());
			superElement = getXmiElement(link.getEntity1().getUid());
		} else {
			throw new IllegalStateException();
		}
		if (subElement == null) {
			throw new IllegalArgumentException("Require sub element for extends link: " + link);
		}
		if (superElement == null) {
			throw new IllegalArgumentException("Require super element for extends link: " + link);
		}

		Element generalizationElement = document.createElement("generalization");
		generalizationElement.setAttribute("xmi:type", "uml:Generalization");
		putXmiElement(link.getUid(), generalizationElement);
		if (link.getLabel() != null) {
			generalizationElement.setAttribute("name", forXMI(link.getLabel()));
		}
		generalizationElement.setAttribute("general", superElement.getAttribute("xmi:id"));
		subElement.appendChild(generalizationElement);
	}

}
