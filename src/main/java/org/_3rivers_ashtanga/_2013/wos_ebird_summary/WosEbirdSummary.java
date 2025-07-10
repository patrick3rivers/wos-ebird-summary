package org._3rivers_ashtanga._2013.wos_ebird_summary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;

import org._3rivers_ashtanga._2013.jaxb.jaxb_app.JAXBAppClass;
import org._3rivers_ashtanga._2013.jaxb.jaxb_app.JAXBAppFactoryClass;
import org._3rivers_ashtanga._2013.wos_ebird_summary.input_parameters.ParametersType;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

public class WosEbirdSummary
    extends
	JAXBAppClass<ParametersType>
{

    static class WosEbirdSummaryFactory
	extends
	    JAXBAppFactoryClass<ParametersType,
		    WosEbirdSummary>
    {

	public WosEbirdSummaryFactory()
	    throws JAXBException {
	    super(ParametersType.class);
	}

	@Override
	public WosEbirdSummary createApp(
	    URI parametersURI,
	    ParametersType parameters,
	    Marshaller marshaller,
	    NamedNodeMap attMap) {
	    try {
		return new WosEbirdSummary(parametersURI,
			parameters, marshaller, attMap);
	    } catch (IOException e) {
		e.printStackTrace();
		throw new RuntimeException(e);
	    }
	}

    }

    private static WosEbirdSummaryFactory AppFactory;
    static {
	try {
	    AppFactory = new WosEbirdSummaryFactory();
	} catch (JAXBException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }

    private final XSSFWorkbook ebirdDataWorkbook;
    private final XSSFWorkbook summaryResultWorkbook;
    private final Map<Pair<String, String>,
	    File> tripDataRowMap = new HashMap<>();
    private final Map<String,
	    Integer> speciesRowMap = new HashMap<>();

    public Map<Pair<String, String>,
	    File> getTripDataRowMap() {
	return tripDataRowMap;
    }

    public XSSFWorkbook getEbirdDataWorkbook() {
	return ebirdDataWorkbook;
    }

    public XSSFWorkbook getSummaryResultWorkbook() {
	return summaryResultWorkbook;
    }

    private WosEbirdSummary(
	URI baseUri,
	ParametersType parameters,
	Marshaller marshaller,
	NamedNodeMap attMap)
	throws IOException {
	super(baseUri, parameters, marshaller, attMap);
	File resultWorkbookFile = new File(
		this.getParametersURI()
			.resolve(this.getParameters()
				.getSummaryOutput()));
	File sourceWorkbookFile = new File(
		this.getParametersURI()
			.resolve(this.getParameters()
				.getWorkbookSource()));
	File ebirdDataWorkbookFile = new File(
		this.getParametersURI()
			.resolve(this.getParameters()
				.getEbirdDataWorkbook()));
	resultWorkbookFile.getParentFile().mkdirs();
	try {
	    Files.copy(sourceWorkbookFile.toPath(),
		    resultWorkbookFile.toPath(),
		    StandardCopyOption.REPLACE_EXISTING);
	    // We're going to copy source workbook to result to start
	    this.summaryResultWorkbook = new XSSFWorkbook(
		    new FileInputStream(
			    resultWorkbookFile));
	    this.ebirdDataWorkbook = new XSSFWorkbook(
		    new FileInputStream(
			    ebirdDataWorkbookFile));
	    initializeTripDataRows();
	    initializeSpeciesRows();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}

    }

    private void initializeSpeciesRows() {
	int i = 0;
	for (Row outputRow : this.getSummaryResultWorkbook()
		.getSheetAt(0)) {
	    // First two rows are heading rows
	    if (i > 1) {
		String value = outputRow.getCell(0)
			.getStringCellValue().trim();
		if (!value.isBlank())
		    this.getSpeciesRowMap()
			    .put(outputRow.getCell(0)
				    .getStringCellValue()
				    .trim(), i);
	    }
	    ++i;
	}
    }

    private void initializeTripDataRows() {
	File ebirdDataRootDir = new File(getParameters()
		.getEbirdCompilationRootDir());
	int tripIndex = -1;
	int dayIndex = -1;
	int dataWbkIdx = -1;

	for (Row row : getEbirdDataWorkbook()
		.getSheetAt(0)) {
	    if (tripIndex < 0) {
		// This is first row
		for (Cell cell : row) {
		    if ("Trip".equals(cell
			    .getStringCellValue().trim())) {
			tripIndex = cell.getColumnIndex();
			if (dayIndex >= 0
				&& dataWbkIdx >= 0)
			    break;
		    } else if ("Day".equals(cell
			    .getStringCellValue().trim())) {
			dayIndex = cell.getColumnIndex();
			if (tripIndex >= 0
				&& dataWbkIdx >= 0)
			    break;
		    } else if ("Trip Report Compilation Output"
			    .equals(cell
				    .getStringCellValue()
				    .trim())) {
			dataWbkIdx = cell.getColumnIndex();
			if (tripIndex >= 0 && dayIndex >= 0)
			    break;
		    }
		}
	    } else {
		File ebirdDataWbkFile = null;
		String dataWbkPath = row.getCell(dataWbkIdx)
			.getStringCellValue();
		if (!dataWbkPath.isBlank())
		    ebirdDataWbkFile = new File(
			    ebirdDataRootDir,
			    dataWbkPath.trim());
		this.getTripDataRowMap().put(Pair.of(
			row.getCell(tripIndex)
				.getStringCellValue()
				.trim(),
			row.getCell(dayIndex)
				.getStringCellValue()
				.trim()),
			ebirdDataWbkFile);

	    }
	}
    }

    static public void main(String[] args)
	throws JAXBException,
	    IOException,
	    SAXException,
	    ParserConfigurationException {
	WosEbirdSummary x = AppFactory.createApp(args[0]);
	x.run();
    }

    @Override
    public void run() {
	// We're going to copy source workbook to result to start
	XSSFSheet resultSheet = getSummaryResultWorkbook()
		.getSheetAt(0);
	XSSFRow tripRow = resultSheet.getRow(0);
	XSSFRow dayRow = resultSheet.getRow(1);
	String tripName = null;
	String tripDay = null;
	for (Cell tripCell : tripRow) {
	    int columnIndex = tripCell.getColumnIndex();
	    String tripCellValue = tripCell
		    .getStringCellValue();
	    if (columnIndex > 0
		    && !tripCellValue.isBlank()) {
		tripName = tripCellValue.trim();
	    }
	    if (tripName != null) {
		tripDay = dayRow.getCell(columnIndex)
			.getStringCellValue().trim();
		File dataWbkFile = getTripDataRowMap()
			.get(Pair.of(tripName, tripDay));
		String tripDesc = tripName + "-" + tripDay;
		System.out.println("Processing trip: "
			+ tripDesc + " ...");
		summarizeEbirdData(columnIndex,
			dataWbkFile);
	    }
	}
    }

    private void summarizeEbirdData(
	int tripColumnIndex,
	File dataWbkFile) {
	if (dataWbkFile == null) {
	    System.err.println("No trip data!");
	    return;
	}
	if (!dataWbkFile.exists()) {
	    System.err.println("No data file: "
		    + dataWbkFile.getAbsolutePath());
	    return;
	}
	try {
	    for (TripSummaryData speciesData : new TripSummaryDataSource(
		    dataWbkFile)) {
		String ebirdSpecies = speciesData
			.getSpecies();
		Integer speciesRow = getSpeciesRowMap()
			.get(ebirdSpecies);
		if (speciesRow == null) {
		    System.err.print(
			    "No Species corresponding to Ebird value: "
				    + ebirdSpecies);
		    String catVal = speciesData
			    .getCategory();
		    if (!catVal.isBlank())
			System.err.print(" (Category: "
				+ catVal.trim() + ")");
		    System.err.println();
		    return;
		}
		Cell outputCell = getOutputCell(speciesRow,tripColumnIndex);
		outputCell.setCellValue(speciesData.getCount());
	    }
	} catch (IOException e) {
	    System.err
		    .println("error processing data file : "
			    + dataWbkFile + "\n"
			    + e.getMessage());
	}
    }

    private Cell getOutputCell(
	Integer speciesRow,
	int tripColumnIndex) {
	return this.getSummaryResultWorkbook().getSheetAt(0)
		.getRow(speciesRow).getCell(tripColumnIndex);
    }

    private Map<String, Integer> getSpeciesRowMap() {
	return this.speciesRowMap;
    }
}
