package org._3rivers_ashtanga._2013.jaxb.jaxb_app;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

abstract public class JAXBAppFactoryClass<T,
	X extends JAXBApp<T>>
	implements
		JAXBAppFactory<T, X> {

	private final Class<T> parametersClass;
	private final JAXBContext jaxbContext;
	private final Unmarshaller unmarshaller;

	public JAXBAppFactoryClass(
		final Class<T> parametersClass,
		String packages)
		throws JAXBException {
		this(
			parametersClass,
			JAXBContext.newInstance(packages));
	}

	public JAXBAppFactoryClass(
		final Class<T> parametersClass)
		throws JAXBException {
		this(
			parametersClass,
			parametersClass.getPackage().getName());
	}

	public JAXBAppFactoryClass(
		Class<T> parametersClass,
		JAXBContext jaxbContext2)
		throws JAXBException,
			JAXBException {
		this.parametersClass = parametersClass;
		this.jaxbContext
			= jaxbContext2;
		this.unmarshaller
			= this.jaxbContext.createUnmarshaller();
	}

	public X createApp(final String uriOrPath)
		throws MalformedURLException,
		JAXBException,
		IOException,
		SAXException,
		ParserConfigurationException,
		JAXBException
	{
		URI uri;
		InputStream inputStream;

		// patrick 20230817 copied 'upto' processing from
		// org._3rivers-ashtanga._2013.xsl:xslt-processing

		// See if legal URi
		try {
			uri = new URI(
				uriOrPath);
		} catch (final URISyntaxException e) {
			// Maybe it's a file?
			uri = new File(
				uriOrPath).getAbsoluteFile().toURI();
		}
		// If uri is not absolute or opaque, it must be a relative file URI
		if (!uri.isAbsolute() && !uri.isOpaque()) {
			uri = new File(
				uriOrPath).getAbsoluteFile().toURI();
		}
		inputStream = uri.toURL().openStream();
		return createApp(uri,
			inputStream);
	}

	public X createApp(final URI uri)
		throws MalformedURLException,
		JAXBException,
		IOException,
		SAXException,
		ParserConfigurationException,
		JAXBException
	{
		return createApp(uri,
			uri.toURL().openStream());
	}

	@SuppressWarnings("unchecked")
	public X createApp(final URI uri,
		InputStream inputStream)
		throws MalformedURLException,
		JAXBException,
		IOException,
		SAXException,
		ParserConfigurationException,
		JAXBException
	{
		DocumentBuilderFactory dbf
			= DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setXIncludeAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(inputStream,
			uri.toString());
		NamedNodeMap attMap
			= doc.getDocumentElement().getAttributes();

		Marshaller marshaller
			= this.jaxbContext.createMarshaller();
		marshaller.setProperty(
			"jaxb.formatted.output",
			true);
		marshaller.setProperty(
			"com.sun.xml.bind.namespacePrefixMapper",
			new SourceDomNamespacePrefixMapper(
				doc));

		DOMSource src = new DOMSource(
			doc,
			uri.toString());
		final Object obj = this.getUnmarshaller()
			.unmarshal(src);// this.getParametersClass());//uri.toURL().openStream());
		T parameters;
		if (obj.getClass() == this.getParametersClass())
			parameters = (T) obj;
		else {
			JAXBElement<T> element = ((JAXBElement<T>) obj);
			parameters = element.getValue();
		}
		return createApp(uri,
			parameters,
			marshaller,
			attMap);
	}

	/**
	 * 
	 * @param uri
	 * @param parameters
	 * @throws ParserConfigurationException
	 * @throws MalformedURLException
	 * @throws SAXException
	 * @throws IOException
	 * @throws JAXBException
	 */
	public X createApp(URI uri,
		T parameters)
		throws ParserConfigurationException,
		MalformedURLException,
		SAXException,
		IOException,
		JAXBException
	{
		// We already have a set of parameters, but no source for it.
		// Marshal it to a dom.
		DocumentBuilderFactory dbf
			= DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setXIncludeAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.newDocument();
		Marshaller marshaller
			= this.jaxbContext.createMarshaller();
		marshaller.setProperty(
			"jaxb.formatted.output",
			true);
		marshaller.setProperty(
			"com.sun.xml.bind.namespacePrefixMapper",
			new SourceDomNamespacePrefixMapper(
				doc));
		NamedNodeMap attMap = null;
		if (doc != null
			&& doc.getDocumentElement() != null) {
			attMap
				= doc.getDocumentElement().getAttributes();
		}
		return createApp(uri,
			parameters,
			marshaller,
			attMap);

	}

	public JAXBContext getJAXBContext()
	{
		return this.jaxbContext;
	}

	private Class<T> getParametersClass()
	{
		return this.parametersClass;
	}

	public Unmarshaller getUnmarshaller()
	{
		return this.unmarshaller;
	}

	@Override
	public void main(final String[] args)
		throws MalformedURLException,
		JAXBException,
		IOException,
		SAXException,
		ParserConfigurationException,
		JAXBException
	{

		X app;
		if (!"-upto".equals(args[0])) {
			app = createApp(args[0]);
		} else {
			// patrick 20230817 copied 'upto' processing from
			// org._3rivers-ashtanga._2013.xsl:xslt-processing
			InputStream inputStream;
			URI systemId;
			// first value to 'concat args' will be the token signaling end of
			// input
			Iterator<String> argIter
				= Arrays.asList(args).iterator();
			argIter.next(); // throw away "-upto"
			String content = concatArgs(argIter);
			// System.err.println("xsl: " + argValue);
			inputStream
				= new ByteArrayInputStream(
					content.getBytes());
			// system id is current wd
			systemId = new File(
				"x").getAbsoluteFile().getParentFile()
					.toURI();
			app = createApp(systemId,
				inputStream);
		}
		app.run();
	}

	private String concatArgs(Iterator<String> argIter)
	{
		StringBuilder result = new StringBuilder();

		String delimiter = "";
		String stopToken = null;
		while (argIter.hasNext()) {
			if (stopToken == null) {
				stopToken = argIter.next();
			} else {
				String arg = argIter.next();
				if (stopToken.equals(arg))
					break;
				else {
					result.append(delimiter)
						.append(arg);
					delimiter = " ";
				}
			}
		}
		return result.toString();
	}

}
