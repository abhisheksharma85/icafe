/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * XMP.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    03Jul2015  Added override method getData()
 * WY    05Mar2015  Revised getMergedDocument()
 * WY    27Feb2015  Added support for ExtendedXMP data
 * WY    19Feb2015  Removed showMetadata() and added getXmpMeta()
 * WY    19Feb2015  Renamed getXMLDocument() to getXmpDocument()
 * WY    11Feb2015  Added getXMLDocument() and showMetadata()
 * WY    19Jan2015  Initial creation
 */

package cafe.image.meta.adobe;

import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.string.XMLUtils;

public class XMP extends Metadata {
	// Fields
	private XMPReader reader;
	private Document xmpDocument;
	private Document extendedXmpDocument;
	private Document mergedXmpDocument;
	private boolean hasExtendedXmp;
	private byte[] extendedXmpData;
		
	public XMP(byte[] data) {
		super(MetadataType.XMP, data);
		reader = new XMPReader(data);
	}
	
	public XMP(String xmp) {
		super(MetadataType.XMP, null);
		reader = new XMPReader(xmp);
	}
	
	public byte[] getData() {
		byte[] data = super.getData();
		if(data != null && !hasExtendedXmp)
			return data;
		try {
			return XMLUtils.serializeToByteArray(getMergedDocument());
		} catch (IOException e) {
			return null;
		}
	}
	
	public byte[] getExtendedXmpData() {
		return extendedXmpData;
	}
	
	public Document getExtendedXmpDocument() {
		if(hasExtendedXmp && extendedXmpDocument == null)
			extendedXmpDocument = XMLUtils.createXML(extendedXmpData);

		return extendedXmpDocument;
	}
	
	public Document getXmpDocument() {
		if(xmpDocument != null){
			return xmpDocument;
		}
		
		return reader.getXmpDocument();		
	}
	
	@Override
	public XMPReader getReader() {
		return reader;
	}
	
	public boolean hasExtendedXmp() {
		return hasExtendedXmp;
	}
	
	/**
	 * Merge the standard XMP and the extended XMP DOM
	 * <p>
	 * This is a very expensive operation, avoid if possible
	 * 
	 * @return a merged Document for the entire XMP data with the GUID from the standard XMP document removed
	 */
	public Document getMergedDocument() {
		if(mergedXmpDocument != null)
			return mergedXmpDocument;
		else if(getExtendedXmpDocument() != null) { // Merge document
			mergedXmpDocument = XMLUtils.createDocumentNode();
			Document rootDoc = getXmpDocument();
			NodeList children = rootDoc.getChildNodes();
			for(int i = 0; i< children.getLength(); i++) {
				Node importedNode = mergedXmpDocument.importNode(children.item(i), true);
				mergedXmpDocument.appendChild(importedNode);
			}
			// Remove GUID from the standard XMP
			XMLUtils.removeAttribute(mergedXmpDocument, "rdf:Description", "xmpNote:HasExtendedXMP");
			// Copy all the children of rdf:RDF element
			NodeList list = extendedXmpDocument.getElementsByTagName("rdf:RDF").item(0).getChildNodes();
			Element rdf = (Element)(mergedXmpDocument.getElementsByTagName("rdf:RDF").item(0));
		  	for(int i = 0; i < list.getLength(); i++) {
	    		Node curr = list.item(i);
	    		Node newNode = mergedXmpDocument.importNode(curr, true);
    			rdf.appendChild(newNode);
	    	}
	    	return mergedXmpDocument;
		} else
			return getXmpDocument();
	}
	
	public void setExtendedXMPData(byte[] extendedXmpData) {
		this.extendedXmpData = extendedXmpData;
		hasExtendedXmp = true;
	}
	
	public void showMetadata() {
		XMLUtils.printNode(getMergedDocument(), "");
	}
}