package org._3rivers_ashtanga._2013.jaxb.jaxb_app;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;


public interface JAXBAppFactory<T, X extends JAXBApp<T>> {
	/**
	 * Create an app to process parameters
	 * 
	 * @param parametersURI
	 *            URI of parameter set
	 * @param parameters
	 * @param marshaller
	 * @param attMap
	 * @return an application that processes parameters
	 */
	public X createApp(URI parametersURI,
		T parameters,
		Marshaller marshaller,
		NamedNodeMap attMap);

	void main(String[] args) throws MalformedURLException,
		JAXBException,
		IOException,
		SAXException,
		ParserConfigurationException, JAXBException;
}
