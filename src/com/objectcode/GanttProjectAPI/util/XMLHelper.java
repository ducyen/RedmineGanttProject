package com.objectcode.GanttProjectAPI.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
//import org.w3c.dom.CDATASection;
//import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLHelper {
  private static final Logger LOGGER = Logger.getLogger(XMLHelper.class);

  /**
   * read XMLFile and parse it into dom, which is returned
   *    
   * license: LGPL v3
   * 
   * @param is
   * @return
   */
  public static org.w3c.dom.Document parseXmlFile(InputStream is) {
    org.w3c.dom.Document dom = null;

    // get the factory
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    try {
      // Using factory get an instance of document builder
      DocumentBuilder db = dbf.newDocumentBuilder();

      // parse using builder to get DOM representation of the XML file
      dom = db.parse(is);
    } catch (ParserConfigurationException pce) {
      pce.printStackTrace();
    } catch (SAXException se) {
      se.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return dom;
  }  
  
  /**
   * take dom and write it into XMLFile
   * 
   * @param is
   * @return
   */
  public static void writeXmlFile(OutputStream os, org.w3c.dom.Document dom) {
    try {
      DOMSource domSource = new DOMSource(dom);
      StreamResult result = new StreamResult(os);
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.transform(domSource, result);
    } catch (TransformerException te) {
      te.printStackTrace();
    }
  }  
  
  /**
   * I take a xml element and the tag name, look for the tag and get the text
   * content i.e for &lt;employee&gt;&lt;name&gt;John&lt;/name&gt;&lt;/employee&gt; xml snippet if
   * the Element points to employee node and tagName is name I will return John  
   * @param ele
   * @param tagName
   * @return
   */
  public static String getTextValueFromXmlElement(org.w3c.dom.Element ele, String tagName) {
    String textVal = null;
    org.w3c.dom.NodeList nl = ele.getElementsByTagName(tagName);
    if (nl != null && nl.getLength() > 0) {
      org.w3c.dom.Element el = (org.w3c.dom.Element) nl.item(0);
      if (el != null && el.getFirstChild() != null) 
        textVal = el.getFirstChild().getNodeValue();
    }
    return textVal;
  }


  /* 
   * print a nodes elements and children 
   */  
  public static void printNodeElements( Node node ){  
    if (node.getNodeValue() == null) {
      LOGGER.debug( "name='"+node.getNodeName()+"' ; value=null" );  
    } else {
      LOGGER.debug( "name='"+node.getNodeName()+"' ; value='"+node.getNodeValue()+"' ; value.trim.length="+node.getNodeValue().trim().length()+" ; value.trim='"+node.getNodeValue().trim()+"' " );  
    }
    NodeList children = node.getChildNodes();  
    for ( int i = 0; i < children.getLength(); i++ ){  
      Node child = children.item( i );  
      if( child.getNodeType() == Node.ELEMENT_NODE ){  
        printNodeElements( child );  
      }  
    }  
   }  
  
}
