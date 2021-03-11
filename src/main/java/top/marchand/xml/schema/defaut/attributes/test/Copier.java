package top.marchand.xml.schema.defaut.attributes.test;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.net.URL;

public class Copier {

    public static void main(String[] args)  throws Exception {
        XMLReader xmlReader = createNewXmlReader();
        Transformer transformer = createNewXsltTransformer();
        URL xmlUrl = Copier.class.getResource("/data.xml");
        transformer.transform(
                new SAXSource(xmlReader,getInputSource(xmlUrl)),
                new StreamResult(System.out)
        );
    }

    private static InputSource getInputSource(URL xmlUrl) throws IOException {
        InputSource source = new InputSource(xmlUrl.openStream());
        source.setSystemId(xmlUrl.toString());
        return source;
    }

    private static Transformer createNewXsltTransformer() throws TransformerConfigurationException {
        return TransformerFactory.newInstance(
                "net.sf.saxon.jaxp.SaxonTransformerFactory",
                Copier.class.getClassLoader()).newTransformer();
    }

    private static XMLReader createNewXmlReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance(
                "org.apache.xerces.jaxp.SAXParserFactoryImpl",
                Copier.class.getClassLoader());
        saxFactory.setValidating(true);
        saxFactory.setNamespaceAware(true);
        saxFactory.setSchema(getSchema());
        SAXParser parser = saxFactory.newSAXParser();
        return parser.getXMLReader();
    }

    private static Schema getSchema() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL schemaUrl = Copier.class.getResource("/schema.xsd");
        return factory.newSchema(schemaUrl);
    }
}