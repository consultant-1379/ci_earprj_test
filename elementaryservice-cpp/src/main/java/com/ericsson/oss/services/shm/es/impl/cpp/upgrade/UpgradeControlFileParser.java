package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This file facilitates parsing of UCF File for retrieving Product Number and Product Revision.
 * 
 * @author tcsrohc
 * 
 */
public class UpgradeControlFileParser extends DefaultHandler {
    private static final String CLASSNAME = UpgradeControlFileParser.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeControlFileParser.class);

    // This stack keeps track of where we are in the XML element tree.
    private final Stack<String> elementStack = new Stack<String>();

    private String productNumber = null;
    private String productRevision = null;

    /**
     * This method parses a upgrade control file.
     * 
     * @param fileName
     *            the upgrade control file to parse.
     * @throws IOException
     *             if the file could not be opened.
     * @throws SAXException
     *             if there was a parse error.
     */
    public void parse(final String fileName) throws IOException, SAXException {
        try {
            final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

            LOGGER.info(CLASSNAME, "parse(" + fileName + ") -->");
            final FileReader fileReader = new FileReader(fileName);
            final InputSource input = new InputSource(fileReader);
            try {
                saxParser.parse(input, this);
            } catch (final SAXException e) {
                throw fixCause(e);
            }

        } catch (final ParserConfigurationException pce) {
            throw new SAXException("Reading/parsing of xml-file: " + fileName + " failed, exception from SAX parser: " + pce.getMessage());
        } finally {
            LOGGER.info(CLASSNAME, "parse() <--");
        }
    }

    /**
     * This method returns product number or null if the parsing failed.
     * 
     * @return A string with the product number or null
     */
    public String getProductNumber() {
        LOGGER.debug("Inside UpgradeControlFileParser.getProductNumber() with  Product number:{} ", productNumber);
        return productNumber;
    }

    /**
     * This method returns product revision or null if the parsing failed.
     * 
     * @return A string with the product revision or null
     */
    public String getProductRevision() {
        return productRevision;
    }

    /**
     * This function keeps the element stack updated. If an element ends, it should no longer be on the stack.
     */
    @Override
    public void endElement(final String namespaceURI, final String simpleName, final String qualifiedName) throws SAXException {
        if (!elementStack.empty()) {
            elementStack.pop();
        }
    }

    /**
     * This function extracts the information we need from the upgrade control file. This function is called whenever a new element is encountered by the SAX parser.
     * 
     * @see DefaultHandler#startElement(String, String, String, Attributes)
     * 
     * @param namespaceURI
     *            The namespace URI. Ignored since namespaces are not used.
     * @param simpleName
     *            The local name. Ignored since namespaces are not used.
     * @param qualifiedName
     *            The qualified name. This will be the element name since name spaces are not used.
     * @param attrs
     *            The attributes for the element that has been encountered.
     * 
     * @throws SAXException
     *             -
     */
    @Override
    public void startElement(final String namespaceURI, final String simpleName, final String qualifiedName, final Attributes attrs) throws SAXException {
        String parentElement = null;

        if (!elementStack.empty()) {
            parentElement = elementStack.peek();
        }

        if (parentElement != null && parentElement.equals("UpgradePackage") && qualifiedName.equals("ProductData")) {
            productNumber = attrs.getValue("number");
            productRevision = attrs.getValue("revision");
        }

        elementStack.push(qualifiedName);
    }

    /**
     * This method just calls initCause() on the given SAXException so that any cause exception can be retrieved through the getCause() method.
     */
    private SAXException fixCause(final SAXException e) {
        if (e.getCause() == null && e.getException() != null) {
            e.initCause(e.getException());
        }
        return e;
    }
}
