package org._3rivers_ashtanga._2013.jaxb.jaxb_app;

import java.util.HashMap;
import java.util.Map;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;


class SourceDomNamespacePrefixMapper extends NamespacePrefixMapper{

	private final Map<String,String> prefixMap = new HashMap<>();
	SourceDomNamespacePrefixMapper(Document doc) {
		NamedNodeMap attMap = null;
		if (doc != null && doc.getDocumentElement() != null )
			attMap = doc.getDocumentElement().getAttributes();
		if (attMap != null) {
			for (int i = 0; i < attMap.getLength(); ++ i) {
				Attr att = (Attr)attMap.item(i);
				String uri = att.getNamespaceURI();
				if ("http://www.w3.org/2000/xmlns/".equals(uri)) {
					String v = att.getValue();
					String nm = att.getLocalName();
					this.getPrefixMap().put(v, nm);
				}
			}
		}
	}

	@Override
	public String getPreferredPrefix(String uri,
		String suggestion,
		boolean arg2)
	{
		String result = getPrefixMap().get(uri);
		if (result == null)
			result = suggestion;
		return result;
	}

	private Map<String,String> getPrefixMap()
	{
		return this.prefixMap;
	}

}
