package org._3rivers_ashtanga._2013.jaxb.jaxb_app;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
/**
 * patrick_paulson@acm.org 20161021 Based on unfunded work done at PNNL
 *
 */
public abstract class JAXBAppClass<P>
	implements
		JAXBApp<P> {

	private final P parameters;
	private final URI parametersURI;
	private final Marshaller marshaller;
	private final NamedNodeMap attMap;

	public JAXBAppClass(
		final URI baseUri,
		final P parameters,
		Marshaller marshaller2,
		NamedNodeMap attMap) {
		this.parametersURI = baseUri;
		this.parameters = parameters;
		this.marshaller = marshaller2;
		this.attMap = attMap;
	}

	protected File resolvePathSpec(String paramValue) {
	    return new File(new File(this.getParametersURI()).getParentFile().toURI().resolve(URI.create(paramValue.trim())));
	}
	@Override
	public P getParameters()
	{
		return this.parameters;
	}

	@Override
	public URI getParametersURI()
	{
		return this.parametersURI;
	}

	protected Marshaller getMarshaller()
	{
		return this.marshaller;
	}

	protected NamedNodeMap getAttMap()
	{
		return this.attMap;
	}

	/**
	 * marshal with prefixes, namespaces, from parameter document
	 * 
	 * @param file
	 *            where to write
	 * @param element
	 *            an element that our marshaller knows how to handle
	 * @param otherAttributes
	 *            other attributes from the top element, jaxb doesn't do good
	 *            job on these.
	 */
	protected void marshall(File file,
		JAXBElement<?> element,
		Map<QName, String> otherAttributes)
	{
		try {
			initOtherAttributes(otherAttributes);
			getMarshaller()
				.marshal(
					element,
					file);
		} catch (final JAXBException e) {
			e.printStackTrace();
			throw new RuntimeException(
				e);
		}
	}

	protected Map<QName, String> initOtherAttributes()
	{
		return initOtherAttributes(
			new HashMap<QName, String>());
	}

	private Map<QName, String>
		initOtherAttributes(
			Map<QName, String> otherAttributes)
	{
		for (int i = 0; i < getAttMap()
			.getLength(); ++i) {
			Attr att = (Attr) getAttMap().item(i);
			// use namespace mapper for namespace prefixes, but add other
			// global atts
			if (att.getNamespaceURI() == null ||
				!XMLConstants.XMLNS_ATTRIBUTE_NS_URI
					.equals(att.getNamespaceURI())) {
				QName qn = new QName(
					att.getNamespaceURI(),
					att.getLocalName(),
					att.getPrefix());
				if (!otherAttributes
					.containsKey(qn)) {
					otherAttributes.put(qn,
						att.getValue());
				}
			}
		}
		return otherAttributes;
	}

}
