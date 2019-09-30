package gca.in.xap.tools.operationtool.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class GsaGscXmlFileParser {

	public static final String expression = "/process/script/environment";

	public String extractXPath(File file, String xpathExpression) throws IOException {
		try (FileInputStream inputStream = new FileInputStream(file)) {
			return extractXPath(inputStream, xpathExpression);
		}
	}

	public String extractXPath(InputStream inputStream, String xpathExpression) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document xml = builder.parse(inputStream);

			Element root = xml.getDocumentElement();
			XPathFactory xpf = XPathFactory.newInstance();
			XPath path = xpf.newXPath();

			return path.evaluate(xpathExpression, root);
		} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

}
