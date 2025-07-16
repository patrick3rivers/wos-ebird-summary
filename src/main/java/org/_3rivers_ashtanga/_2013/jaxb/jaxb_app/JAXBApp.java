package org._3rivers_ashtanga._2013.jaxb.jaxb_app;

import java.net.URI;

/**
 * patrick_paulson@acm.org 20161021 Based on unfunded work done at PNNL
 *
 */
public interface JAXBApp<P>
		extends
			Runnable {

	P
			getParameters();
	URI getParametersURI();
}
