package org._3rivers_ashtanga._2013.wos_ebird_summary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org._3rivers_ashtanga._2013.wos_ebird_summary.cached_workbooks.CachedWorkbookType;
import org._3rivers_ashtanga._2013.wos_ebird_summary.cached_workbooks.CachedWorkbooksType;
import org._3rivers_ashtanga._2013.wos_ebird_summary.cached_workbooks.ObjectFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class TripSummaryDataSourceFactory {
    private static final String WORKBOOK_INDEX = "workook-index.xml";
    private final static ObjectFactory CACHED_WORKBOOK_OBJECT_FACTORY = new ObjectFactory();
    private static final JAXBContext CACHED_WORKBOOK_JAXB_CONTEXT;

    static {
	try {
	    CACHED_WORKBOOK_JAXB_CONTEXT = JAXBContext
		    .newInstance(
			    CACHED_WORKBOOK_OBJECT_FACTORY
				    .getClass().getPackage()
				    .getName());
	} catch (JAXBException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}

    }

    private WebReader webReader = WebReader.getInstance();
    private final Map<Pair<String, String>,
	    CachedWorkbookType> cachedWorkbookMap = new HashMap<>();
    private final File workBookDirectory;

    public File getWorkBookDirectory() {
	return workBookDirectory;
    }

    private final File cachedWorkbookIndexFile;

    public File getCachedWorkbookIndexFile() {
	return cachedWorkbookIndexFile;
    }

    @SuppressWarnings("unchecked")
    public TripSummaryDataSourceFactory(
	File cachedWorkbookDirectory)
	throws JAXBException {
	this.workBookDirectory = cachedWorkbookDirectory;
	this.workBookDirectory.mkdirs();
	cachedWorkbookIndexFile = new File(
		this.workBookDirectory, WORKBOOK_INDEX);
	if (cachedWorkbookIndexFile.exists()) {
	    Unmarshaller unmarshal = CACHED_WORKBOOK_JAXB_CONTEXT
		    .createUnmarshaller();
	    Object x = unmarshal
		    .unmarshal(cachedWorkbookIndexFile);
	    CachedWorkbooksType cachedBooks = null;
	    if (x instanceof CachedWorkbooksType) {
		cachedBooks = (CachedWorkbooksType) x;
	    } else {
		cachedBooks = ((JAXBElement<
			CachedWorkbooksType>) x).getValue();
	    }
	    for (CachedWorkbookType bk : cachedBooks
		    .getCachedWorkbook()) {
		this.getCachedWorkbookMap()
			.put(Pair.of(bk.getRouteName(),
				bk.getTripInstance()), bk);
	    }
	}
    }

    private Map<Pair<String, String>,
	    CachedWorkbookType> getCachedWorkbookMap() {
	return this.cachedWorkbookMap;
    }

    private WebReader getWebReader() {
	return this.webReader;
    }

    public void flushIndex() throws IOException {
	try {
	    JAXBElement<
		    CachedWorkbooksType> rootElement = CACHED_WORKBOOK_OBJECT_FACTORY
			    .createCachedWorkbooks(
				    CACHED_WORKBOOK_OBJECT_FACTORY
					    .createCachedWorkbooksType());
	    rootElement.getValue().getCachedWorkbook()
		    .addAll(this.getCachedWorkbookMap()
			    .values());
	    Marshaller marshal = CACHED_WORKBOOK_JAXB_CONTEXT
		    .createMarshaller();
	    marshal.marshal(rootElement,
		    this.getCachedWorkbookIndexFile());
	} catch (JAXBException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}

    }

    public TripSummaryDataSource getTripDataSource(
	String tripName,
	String tripDay,
	String tripLists)
	throws IOException {
	Pair<String, String> entryKey = Pair.of(tripName,
		tripDay);
	CachedWorkbookType entry = this
		.getCachedWorkbookMap().get(entryKey);
	XSSFWorkbook resultWbk = null;
	if (entry == null) {
	    entry = CACHED_WORKBOOK_OBJECT_FACTORY
		    .createCachedWorkbookType();
	    entry.setRouteName(tripName);
	    entry.setTripInstance(tripDay);
	    entry.setTripData(tripLists.trim());
	    getCachedWorkbookMap().put(entryKey, entry);
	}
	String entryFile = entry.getCachedWorkbookFile();
	String newTripListData = tripLists.trim().replaceAll("\\s+", " ");
	String entryTripListData = entry.getTripData().trim().replaceAll("\\s+", " ");
	if (entryFile == null || !entryTripListData.equals(newTripListData)) {
	    resultWbk = getWebReader()
		    .fetchTripListResults(tripLists.trim());
	    if (entryFile == null) {
		Path dataFile = Files.createTempFile(
			this.getWorkBookDirectory()
				.toPath(),
			"trip-data-", ".xlsx");
		entryFile = dataFile.getFileName()
			.toString();
		entry.setCachedWorkbookFile(entryFile);
	    }
	    resultWbk.write(new FileOutputStream(
		    new File(this.getWorkBookDirectory(),
			    entryFile)));
	    // flush index for each new file so we don't lose work
	    flushIndex();
	}
	else {
	    resultWbk = new XSSFWorkbook(
		    new FileInputStream(
			    	new File(this.getWorkBookDirectory(),entryFile.trim())));
	}
	return new TripSummaryDataSource(resultWbk);
    }

}
