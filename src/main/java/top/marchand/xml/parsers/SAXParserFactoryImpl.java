package top.marchand.xml.parsers;

import org.xml.sax.*;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A SAXParserFactory that wraps a Xerces SAXParserFactoryImpl, and provides
 * XMLReader that is schema-aware.
 * Schema loading is based on attribute, <code>xsl:noNamespaceSchemaLocation</code> or
 * <code>xsi:schemaLocation</code>
 */
public class SAXParserFactoryImpl extends SAXParserFactory {
    private static final QName QN_NO_NS_SCHEMA_LOCATION = new QName(
            XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
            "noNamespaceSchemaLocation");
    private static final QName QN_SCHEMA_LOCATION = new QName(
            XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
            "schemaLocation"
    );
    private org.apache.xerces.jaxp.SAXParserFactoryImpl internalFactory;

    public SAXParserFactoryImpl() {
        internalFactory = new org.apache.xerces.jaxp.SAXParserFactoryImpl();
    }

    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        return new SAXParserWrapper(internalFactory.newSAXParser());
    }

    @Override
    public void setFeature(String name, boolean value) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        internalFactory.setFeature(name, value);
    }

    @Override
    public boolean getFeature(String name) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        return internalFactory.getFeature(name);
    }

    @Override
    public void setNamespaceAware(boolean awareness) {
        internalFactory.setNamespaceAware(awareness);
    }

    @Override
    public void setValidating(boolean validating) {
        internalFactory.setValidating(validating);
    }

    @Override
    public boolean isNamespaceAware() {
        return internalFactory.isNamespaceAware();
    }

    @Override
    public boolean isValidating() {
        return internalFactory.isValidating();
    }

    @Override
    public Schema getSchema() {
        return internalFactory.getSchema();
    }

    @Override
    public void setSchema(Schema schema) {
        internalFactory.setSchema(schema);
    }

    @Override
    public void setXIncludeAware(boolean state) {
        internalFactory.setXIncludeAware(state);
    }

    @Override
    public boolean isXIncludeAware() {
        return internalFactory.isXIncludeAware();
    }

    private class SAXParserWrapper extends SAXParser {
        private final SAXParser internalParser;
        private final SAXParserFactoryImpl factory = SAXParserFactoryImpl.this;

        public SAXParserWrapper(SAXParser saxParser) {
            this.internalParser=saxParser;
        }

        @Override
        public Parser getParser() throws SAXException {
            return internalParser.getParser();
        }

        @Override
        public XMLReader getXMLReader() throws SAXException {
            return new SAXParserFactoryImpl.XMLReaderWrapper(this, internalParser.getXMLReader());
        }

        @Override
        public boolean isNamespaceAware() {
            return internalParser.isNamespaceAware();
        }

        @Override
        public boolean isValidating() {
            return internalParser.isValidating();
        }

        @Override
        public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            internalParser.setProperty(name, value);
        }

        @Override
        public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return internalParser.getProperty(name);
        }

    }

    private class XMLReaderWrapper implements XMLReader {
        private SAXParserWrapper saxParserWrapper;
        private XMLReader innerReader;

        public XMLReaderWrapper(SAXParserWrapper saxParserWrapper, XMLReader xmlReader) {
            this.saxParserWrapper = saxParserWrapper;
            this.innerReader = xmlReader;
        }

        @Override
        public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return innerReader.getFeature(name);
        }

        @Override
        public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
            innerReader.setFeature(name, value);
        }

        @Override
        public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
            return innerReader.getProperty(name);
        }

        @Override
        public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            innerReader.setProperty(name, value);
        }

        @Override
        public void setEntityResolver(EntityResolver resolver) {
            innerReader.setEntityResolver(resolver);
        }

        @Override
        public EntityResolver getEntityResolver() {
            return innerReader.getEntityResolver();
        }

        @Override
        public void setDTDHandler(DTDHandler handler) {
            innerReader.setDTDHandler(handler);
        }

        @Override
        public DTDHandler getDTDHandler() {
            return innerReader.getDTDHandler();
        }

        @Override
        public void setContentHandler(ContentHandler handler) {
            innerReader.setContentHandler(handler);
        }

        @Override
        public ContentHandler getContentHandler() {
            return innerReader.getContentHandler();
        }

        @Override
        public void setErrorHandler(ErrorHandler handler) {
            innerReader.setErrorHandler(handler);
        }

        @Override
        public ErrorHandler getErrorHandler() {
            return innerReader.getErrorHandler();
        }

        @Override
        public void parse(InputSource input) throws IOException, SAXException {
            Schema schema = null;
            try {
                schema = getSchemaFromSourceFile(input);
            } catch (ParserConfigurationException e) {
                throw new SAXException(e);
            }
            if(schema!=null) {
                try {
                    replaceXmlReader(schema);
                } catch (ParserConfigurationException e) {
                    throw new SAXException(e);
                }
            }
            innerReader.parse(input);
        }

        @Override
        public void parse(String systemId) throws IOException, SAXException {
            Schema schema = null;
            try {
                schema = getSchemaFromSourceFile(systemId);
            } catch (ParserConfigurationException e) {
                throw new SAXException(e);
            }
            if(schema!=null) {
                try {
                    replaceXmlReader(schema);
                } catch (ParserConfigurationException e) {
                    throw new SAXException(e);
                }
            }
            innerReader.parse(systemId);
        }

        private void replaceXmlReader(Schema schema) throws ParserConfigurationException, SAXException {
            saxParserWrapper.factory.setSchema(schema);
            saxParserWrapper = new SAXParserWrapper(saxParserWrapper.factory.newSAXParser());
            XMLReader newReader = saxParserWrapper.getXMLReader();
            newReader.setErrorHandler(innerReader.getErrorHandler());
            newReader.setContentHandler(innerReader.getContentHandler());
            newReader.setEntityResolver(innerReader.getEntityResolver());
            newReader.setDTDHandler(innerReader.getDTDHandler());
            innerReader = newReader;
        }
    }

    private Schema getSchemaFromSourceFile(InputSource source) throws ParserConfigurationException, SAXException {
        SAXParser parser = newSAXParser();
        SchemaHandler schemaHandler = new SchemaHandler(source.getSystemId());
        try {
            parser.parse(source.getSystemId(), schemaHandler);
        } catch(Exception ex) { }
        if(schemaHandler.schemaResource!=null) {
            return loadSchema(schemaHandler.schemaResource);
        }
        return null;
    }
    private Schema getSchemaFromSourceFile(String systemId) throws ParserConfigurationException, SAXException {
        SAXParser parser = newSAXParser();
        SchemaHandler schemaHandler = new SchemaHandler(systemId);
        try {
            parser.parse(systemId, schemaHandler);
        } catch(Exception ex) { }
        if(schemaHandler.schemaResource!=null) {
            return loadSchema(schemaHandler.schemaResource);
        }
        return null;
    }

    private Schema loadSchema(Source schemaResource) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return factory.newSchema(schemaResource);
    }

    private class SchemaHandler extends DefaultHandler2 {
        private Source schemaResource;
        private Map<String, Deque<String>> prefixMapping = new ConcurrentHashMap<>();
        private final String documentBase;

        private SchemaHandler(String documentBase) {
            this.documentBase = documentBase;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String schemaRelativePath = null;
            for(int i=0; i<attributes.getLength();i++) {
                QName attName = getQName(attributes.getQName(i));
                if(QN_NO_NS_SCHEMA_LOCATION.equals(attName)) {
                    schemaRelativePath = attributes.getValue(i);
                } else if(QN_SCHEMA_LOCATION.equals(attName)) {
                    // TODO : enhance this for multiple namespaces
                    schemaRelativePath = attributes.getValue(i).split(" ")[1];
                }
            }
            if(schemaRelativePath!=null) {
                try {
                    schemaResource = TransformerFactory.newInstance().getURIResolver().resolve(schemaRelativePath, documentBase);
                } catch(TransformerException ex) {
                    throw new SAXException(ex);
                }
            }
        }

        private QName getQName(String qName) {
            String[] s = qName.split(":");
            if(s.length==1) return new QName(qName);
            else return new QName(prefixMapping.get(s[0]).peek(), s[1], s[0]);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            Deque<String> stack = prefixMapping.getOrDefault(prefix, new ArrayDeque<>(5));
            stack.push(uri);
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            prefixMapping.get(prefix).pop();
        }
    }
}
