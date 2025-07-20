package org._3rivers_ashtanga._2013.wos_ebird_summary;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class WebReader {

    private static final String USER_AGENT = "Mozilla/5.0";
    private final static String HOST_STRING = "https://www.faintlake.com/eBird/compiler/";
    private static final WebReader WEBREADER_INSTANCE = new WebReader();

    private static final URL HOST_URL;
    static {
	try {
	    HOST_URL = new URL(HOST_STRING);
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }

    public static WebReader getInstance() {
	return WEBREADER_INSTANCE;
    }

    public XSSFWorkbook fetchTripListResults(
	String tripLists)
	throws IOException {
	HttpsURLConnection con = null;
	XSSFWorkbook result = null;
	try {
//	    Logger.getLogger(getClass()).log(Level.INFO, "trip lists: " + tripLists);
	    con = (HttpsURLConnection) HOST_URL
		    .openConnection();
	    con.setRequestMethod("POST");
	    con.setRequestProperty("User-Agent",
		    USER_AGENT);
	    String parameters = "checklists=" + tripLists
		    + "&" + "fetchButton=Go!";
	    con.setRequestProperty("Content-Type",
		    "application/x-www-form-urlencoded");
	    con.setRequestProperty("Content-Length",
		    Integer.toString(
			    parameters.getBytes().length));
	    con.setDoOutput(true);
	    OutputStream os = con.getOutputStream();
	    os.write(parameters.getBytes());
	    os.flush();
	    os.close();
	    int responseCode = con.getResponseCode();
	    if (responseCode == HttpsURLConnection.HTTP_OK) { // success

		byte[] resultBytes = con.getInputStream()
			.readAllBytes();
		con.getInputStream().close();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(
			resultBytes);
		result = new XSSFWorkbook(byteStream);
	    } else {
		throw new RuntimeException(
			"POST request did not work.");
	    }
	} finally {
	    if (con != null)
		con.disconnect();
	}
	return result;
    }

}
