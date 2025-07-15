package org._3rivers_ashtanga._2013.wos_ebird_summary;

public class BadTemplateException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    public BadTemplateException(WosEbirdSummary x) {
	super("Invalid template at "
		+ x.getParametersURI()
			.resolve(x.getParameters()
				.getWorkbookSource())
			.toString()
		+ ", see documentation.");
    }
    public BadTemplateException(WosEbirdSummary x, String message) {
	super("Invalid template at "
		+ x.getParametersURI()
			.resolve(x.getParameters()
				.getWorkbookSource())
			.toString()
		+ ", see documentation.\n\t Additional information: " + message);
    }

}
