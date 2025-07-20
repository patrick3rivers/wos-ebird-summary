package org._3rivers_ashtanga._2013.wos_ebird_summary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;

import org._3rivers_ashtanga._2013.jaxb.jaxb_app.JAXBAppClass;
import org._3rivers_ashtanga._2013.jaxb.jaxb_app.JAXBAppFactoryClass;
import org._3rivers_ashtanga._2013.wos_ebird_summary.additional_species.ObjectFactory;
import org._3rivers_ashtanga._2013.wos_ebird_summary.additional_species.ObservedByType;
import org._3rivers_ashtanga._2013.wos_ebird_summary.additional_species.OtherSpeciesType;
import org._3rivers_ashtanga._2013.wos_ebird_summary.input_parameters.ParametersType;
import org._3rivers_ashtanga._2013.wos_ebird_summary.species_aliases.AliasesForType;
import org._3rivers_ashtanga._2013.wos_ebird_summary.species_aliases.AliasesType;
import org._3rivers_ashtanga._2013.wos_ebird_summary.taxonomy_order.TaxonType;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
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
	    } catch (IOException | JAXBException e) {
		e.printStackTrace();
		throw new RuntimeException(e);
	    }
	}

    }

    static final ObjectFactory OTHER_SPECIES_OBJ_FACTORY = new ObjectFactory();

    static final int COMMON_NAME_HEADING_ROW_NUM = 1;
    // This means the template must have at least one row
    static final int FIRST_SPECIES_ROW = 2;
    static final String EBIRD_TAXON_ORDER_HEADING = "TAXON_ORDER";
    static final String EBIRD_CATEGORY_HEADER = "CATEGORY";
    static final String EBIRD_COMMON_NAME_HEADING = "PRIMARY_COM_NAME";
    static final String EBIRD_SPECIES_CATEGORY = "species";

    private static WosEbirdSummaryFactory AppFactory;
    static {
	try {
	    AppFactory = new WosEbirdSummaryFactory();
	} catch (JAXBException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }

    static private final Pattern SIMPLE_SPECIES_PATTERN = Pattern
	    .compile("([^(]*).*\\(.*");

    static public void main(String[] args)
	throws JAXBException,
	    IOException,
	    SAXException,
	    ParserConfigurationException {
	WosEbirdSummary x = AppFactory.createApp(args[0]);
	x.run();
    }

    private final XSSFWorkbook summaryResultWorkbook;
    private final Map<Pair<String, String>,
	    String> tripDataRowMap = new HashMap<>();

    private final Map<String,
	    Integer> speciesRowMap = new HashMap<>();

    private final Map<String,
	    OtherSpeciesType> otherSpeciesMap = new HashMap<>();

    private final Map<String,
	    TaxonType> taxonMap = new HashMap<>();

    /**
     * Filled in by main line, don't use until after that...
     */
    private final Map<Pair<String, String>,
	    Integer> tripColumnMap = new HashMap<>();

    private Integer firstTripColumn;

    private final TripSummaryDataSourceFactory dataSourceFactory;

    private WosEbirdSummary(
	URI baseUri,
	ParametersType parameters,
	Marshaller marshaller,
	NamedNodeMap attMap)
	throws IOException,
	    JAXBException {
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
	this.dataSourceFactory = new TripSummaryDataSourceFactory(
		new File(this.getParametersURI().resolve(
			this.getParameters().getEbirdCompilationRootDir())));
	try {
	    Files.copy(sourceWorkbookFile.toPath(),
		    resultWorkbookFile.toPath(),
		    StandardCopyOption.REPLACE_EXISTING);
	    // We're going to copy source workbook to result to start
	    this.summaryResultWorkbook = new XSSFWorkbook(
		    new FileInputStream(
			    sourceWorkbookFile));
	    XSSFWorkbook ebirdDataWbk = new XSSFWorkbook(
		    new FileInputStream(
			    ebirdDataWorkbookFile));
	    initializeTripDataRows(ebirdDataWbk);
	    initializeSpeciesRows();
	    initializeTaxonMap();
	    addAliasData();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}

    }

    public Integer getFirstTripColumn() {
	return firstTripColumn;
    }

    public Map<Pair<String, String>,
	    Integer> getTripColumnMap() {
	return tripColumnMap;
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
	Integer firstTripColumn = null;
	for (Cell tripCell : tripRow) {
	    int columnIndex = tripCell.getColumnIndex();
	    String tripCellValue = tripCell
		    .getStringCellValue();
	    if (columnIndex > 0
		    && !tripCellValue.isBlank()) {
		tripName = tripCellValue.trim();
	    }
	    if (tripName != null) {
		if (firstTripColumn == null) {
		    firstTripColumn = tripCell
			    .getColumnIndex();
		    // This is used to format additional species rows.
		    // pretty messy.
		    this.setFirstTripColumn(
			    firstTripColumn);
		}
		XSSFCell tripDayCell = dayRow
			.getCell(columnIndex);
		XSSFCellStyle style = tripDayCell
			.getCellStyle();
		tripDay = tripDayCell.getStringCellValue()
			.trim();
		this.getTripColumnMap().put(
			Pair.of(tripName, tripDay),
			tripDayCell.getColumnIndex());
		String tripLists = getTripDataRowMap()
			.get(Pair.of(tripName, tripDay));
		if (tripLists == null)
		    tripLists = "";
		if (!tripLists.isBlank() && tripLists.charAt(0) == '"') {
		    tripLists = tripLists.substring(1,tripLists.length()-2);
		}
		String tripDesc = tripName + "-" + tripDay;
		System.out.println("Processing trip: "
			+ tripDesc + " ...");
		if (tripDesc.equals("Naneum-Colockum-Saturday")) {
		    System.out.println("Our problem trip.");
		}
		if (style.getFont().getStrikeout()) {
		    System.out
			    .println("   Trip cancelled.");
		} else {
		    try {
			summarizeEbirdData(columnIndex,
			    tripLists, tripName, tripDay);
		    } catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		    }
		}
		System.out.flush();
	    }
	}
	try {
	    addTotalsColumn();
	    // Sort other species values by taxonomy
	    SortedSet<
		    OtherSpeciesType> speciesInOrder = new TreeSet<>(
			    new Comparator<
				    OtherSpeciesType>() {

				@Override
				public int compare(
				    OtherSpeciesType o1,
				    OtherSpeciesType o2) {
				    return o1
					    .getTaxonOrder()
					    .compareTo(o2
						    .getTaxonOrder());
				}

			    });
	    speciesInOrder.addAll(
		    this.getOtherSpeciesMap().values());
	    addOtherSpecies(speciesInOrder);
	    addTotalCountRow("Total Species");
	    int lastCountRow = this
		    .getSummaryResultWorkbook()
		    .getSheetAt(0).getLastRowNum();
	    addOtherTaxa(speciesInOrder);
	    addTaxaTotalCountRow(lastCountRow);
	    saveData();
	} catch (IOException | JAXBException
		| BadTemplateException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }

    private void addTaxaTotalCountRow(
	final int lastCountRow)
	throws BadTemplateException {
	Function<Cell,
		String> sumAndCountFunction = new Function<>() {

		    @Override
		    public String apply(Cell cell) {
			return CellReference
				.convertNumToColString(cell
					.getColumnIndex())
				+ Integer.toString(
					lastCountRow + 1)
				+ " + COUNTIF("
				+ CellReference
					.convertNumToColString(
						cell.getColumnIndex())
				+ Integer.toString(
					lastCountRow + 2)
				+ ":"
				+ CellReference
					.convertNumToColString(
						cell.getColumnIndex())
				+ (cell.getRowIndex())
				+ ",\">0\")";
		    }

		};
	addTotalCountRow("Total Taxa", sumAndCountFunction);

    }

    private void addTotalCountRow(String heading)
	throws BadTemplateException {
	Function<Cell,
		String> countifFunction = new Function<>() {

		    @Override
		    public String apply(Cell cell) {
			return "COUNTIF(" + CellReference
				.convertNumToColString(cell
					.getColumnIndex())
				+ Integer.toString(
					FIRST_SPECIES_ROW
						+ 1)
				+ ":"
				+ CellReference
					.convertNumToColString(
						cell.getColumnIndex())
				+ (cell.getRowIndex())
				+ ",\">0\")";
		    }

		};
	addTotalCountRow(heading, countifFunction);
    }

    private void addAdditionalData(
	String heading,
	SortedSet<OtherSpeciesType> speciesInOrder,
	Predicate<OtherSpeciesType> predicate)
	throws BadTemplateException {
	addCommonNameHeadingRow(heading);
	Iterator<OtherSpeciesType> i = speciesInOrder
		.stream().filter(predicate).iterator();
	while (i.hasNext())
	    addSpeciesRow(i.next());
    }

    @SuppressWarnings("unchecked")
    private void addAliasData() throws JAXBException {
	String aliasDb = getParameters().getAliasDb();
	if (aliasDb != null && !aliasDb.isBlank()) {
	    AliasesType aliasLists = null;
	    File aliasDbFile = new File(
		    getParametersURI().resolve(aliasDb));
	    JAXBContext context = JAXBContext.newInstance(
		    AliasesType.class.getPackageName());
	    Object dObj = context.createUnmarshaller()
		    .unmarshal(aliasDbFile);
	    if (dObj instanceof AliasesType)
		aliasLists = (AliasesType) dObj;
	    else {
		aliasLists = ((JAXBElement<
			AliasesType>) dObj).getValue();
	    }
	    for (AliasesForType wosTermInfo : aliasLists
		    .getAliasesFor()) {
		String wosTerm = wosTermInfo.getTerm()
			.trim();
		Integer termRowIdx = this.getSpeciesRowMap()
			.get(wosTerm);
		if (termRowIdx == null) {
		    System.out.println(
			    "Unknown WOS species in alias db: "
				    + wosTerm);
		} else {
		    for (String alias : wosTermInfo
			    .getAlias()) {
			this.getSpeciesRowMap().put(
				alias.trim(), termRowIdx);
		    }
		}
	    }
	}
    }

    private void addCommonNameHeadingRow(String headingText)
	throws BadTemplateException {
	XSSFSheet sheet = this.getSummaryResultWorkbook()
		.getSheetAt(0);
	// Need at least one species row to use as model.
	if (sheet.getLastRowNum() < FIRST_SPECIES_ROW) {
	    throw new BadTemplateException(this,
		    "need one species row");
	}
	sheet.copyRows(COMMON_NAME_HEADING_ROW_NUM,
		COMMON_NAME_HEADING_ROW_NUM,
		sheet.getLastRowNum() + 1,
		new CellCopyPolicy.Builder().build());
	XSSFRow row = sheet.getRow(sheet.getLastRowNum());
	for (Cell cell : row) {
	    cell.setBlank();
	}
	XSSFCell cell = row.getCell(0);
	cell.setCellValue(headingText);
	XSSFCellStyle cellStyle = cell.getCellStyle();
	XSSFFont cellFont = cellStyle.getFont();
	cellFont.setItalic(true);
	row.setHeight(sheet.getRow(FIRST_SPECIES_ROW)
		.getHeight());
	CellRangeAddress mergedHeadingRegion = new CellRangeAddress(
		row.getRowNum(), row.getRowNum(), 0,
		row.getLastCellNum() - 1);
	sheet.addMergedRegion(mergedHeadingRegion);
    }

    private void addOtherSpecies(
	SortedSet<OtherSpeciesType> speciesInOrder)
	throws BadTemplateException {

	Predicate<
		OtherSpeciesType> predicate = new Predicate<
			OtherSpeciesType>() {

		    @Override
		    public boolean test(
			OtherSpeciesType t) {
			return t.getEbirdCategory().trim()
				.equals(EBIRD_SPECIES_CATEGORY);
		    }

		};
	addAdditionalData("Additional Species",
		speciesInOrder, predicate);
    }

    private void addOtherTaxa(
	SortedSet<OtherSpeciesType> speciesList)
	throws BadTemplateException {
	Predicate<
		OtherSpeciesType> predicate = new Predicate<
			OtherSpeciesType>() {

		    @Override
		    public boolean test(
			OtherSpeciesType t) {
			return !t.getEbirdCategory().trim()
				.equals(EBIRD_SPECIES_CATEGORY);
		    }

		};
	addAdditionalData("Additional Taxa", speciesList,
		predicate);

    }

    private void addSpeciesRow(OtherSpeciesType species)
	throws BadTemplateException {
	XSSFSheet sheet = this.getSummaryResultWorkbook()
		.getSheetAt(0);
	if (sheet.getLastRowNum() < FIRST_SPECIES_ROW)
	    throw new BadTemplateException(this,
		    "No species row to use as template");

	sheet.copyRows(FIRST_SPECIES_ROW, FIRST_SPECIES_ROW,
		sheet.getLastRowNum() + 1,
		new CellCopyPolicy.Builder().build());
	XSSFRow row = sheet.getRow(sheet.getLastRowNum());
	String cellValue = species.getSpecies();
	if (!species.getEbirdCategory()
		.equals(EBIRD_SPECIES_CATEGORY)) {
	    cellValue = cellValue + " ("
		    + species.getEbirdCategory() + ")";
	}
	row.getCell(0).setCellValue(cellValue);
	XSSFCellStyle fillerCellStyle = sheet
		.getRow(COMMON_NAME_HEADING_ROW_NUM)
		.getCell(1).getCellStyle();
	for (int i = 1; i < this
		.getFirstTripColumn(); ++i) {
	    XSSFCell cell = row.getCell(i);
	    cell.setCellStyle(fillerCellStyle);
	}
	int i = row.getLastCellNum() - 1;
	Cell summaryCell = row.getCell(i);
	summaryCell.setCellFormula("sum("
		+ CellReference.convertNumToColString(0)
		+ row.getRowNum() + ":"
		+ CellReference.convertNumToColString(
			row.getLastCellNum() - 1)
		+ row.getRowNum() + ")");
	for (ObservedByType tripInfo : species
		.getObservedBy()) {
	    Integer tripColumn = this.getTripColumnMap()
		    .get(Pair.of(
			    tripInfo.getTripName().trim(),
			    tripInfo.getTripDay().trim()));
	    row.getCell(tripColumn)
		    .setCellValue(tripColumn);
	}

    }

    private void addTotalCountRow(
	String heading,
	Function<Cell, String> formulaGenerator)
	throws BadTemplateException {
	XSSFSheet sheet = this.getSummaryResultWorkbook()
		.getSheetAt(0);
	if (sheet.getLastRowNum() < FIRST_SPECIES_ROW)
	    throw new BadTemplateException(this,
		    "No species row to use as template");
	sheet.copyRows(FIRST_SPECIES_ROW, FIRST_SPECIES_ROW,
		sheet.getLastRowNum() + 1,
		new CellCopyPolicy.Builder().build());
	XSSFRow row = sheet.getRow(sheet.getLastRowNum());
	String cellValue = heading;
	XSSFCell cell = row.getCell(0);
	cell.setCellValue(cellValue);
	XSSFCellStyle cellStyle = cell.getCellStyle();
	XSSFFont cellFont = cellStyle.getFont();
	cellFont.setItalic(true);
	XSSFCellStyle fillerCellStyle = sheet
		.getRow(COMMON_NAME_HEADING_ROW_NUM)
		.getCell(1).getCellStyle();
	for (int i = 1; i < this
		.getFirstTripColumn(); ++i) {
	    cell = row.getCell(i);
	    cell.setCellStyle(fillerCellStyle);
	}
	for (int i = this.getFirstTripColumn(); i < row
		.getLastCellNum(); ++i) {
	    cell = row.getCell(i);
	    cell.setCellFormula(
		    formulaGenerator.apply(cell));
	}
    }

    private void addTotalsColumn()
	throws BadTemplateException {
	XSSFSheet sheet = this.getSummaryResultWorkbook()
		.getSheetAt(0);
	Integer previousLastCellNum = null;
	if (sheet.getLastRowNum() < FIRST_SPECIES_ROW)
	    throw new BadTemplateException(this);
	for (Row row : sheet) {
	    int currentLastCell = row.getLastCellNum();
	    if (previousLastCellNum != null
		    && previousLastCellNum != currentLastCell)
		throw new BadTemplateException(this,
			"all rows must have same number of columns");
	    Cell cell = row.createCell(currentLastCell);
	    cell.setCellStyle(
		    row.getCell(currentLastCell - 1)
			    .getCellStyle());
	    switch (row.getRowNum()) {
	    case 0:
		cell.setCellValue("Totals");
		break;
	    case COMMON_NAME_HEADING_ROW_NUM:
		break;
	    default:
		cell.setCellFormula("sum("
			+ CellReference
				.convertNumToColString(0)
			+ row.getRowNum() + 1 + ":"
			+ CellReference
				.convertNumToColString(
					currentLastCell - 1)
			+ row.getRowNum() + 1 + ")");

	    }
	}
    }

    private Map<String, OtherSpeciesType>

	    getOtherSpeciesMap() {
	return this.otherSpeciesMap;
    }

    private Cell getOutputCell(
	Integer speciesRow,
	int tripColumnIndex) {

	return this.getSummaryResultWorkbook().getSheetAt(0)
		.getRow(speciesRow)
		.getCell(tripColumnIndex);
    }

    private Map<String, Integer> getSpeciesRowMap() {
	return this.speciesRowMap;
    }

    private XSSFWorkbook getSummaryResultWorkbook() {
	return summaryResultWorkbook;
    }

    private Map<String, TaxonType> getTaxonMap() {
	return this.taxonMap;
    }

    private Map<Pair<String, String>,
	    String> getTripDataRowMap() {
	return tripDataRowMap;
    }

    /*
     * Returns common name cell for formatting
     */
    private void initializeSpeciesRows() {
	int i = 0;
	for (Row row : this.getSummaryResultWorkbook()
		.getSheetAt(0)) {
	    // First two rows are heading rows
	    if (i >= FIRST_SPECIES_ROW) {
		String value = row.getCell(0)
			.getStringCellValue().trim();
		if (!value.isBlank())
		    this.getSpeciesRowMap()
			    .put(row.getCell(0)
				    .getStringCellValue()
				    .trim(), i);
	    }
	    ++i;
	}
    }

    private void initializeTaxonMap() throws IOException {
	File taxonWorkbookFile = new File(this
		.getParametersURI()
		.resolve(this.getParameters()
			.getEbirdTaxonomyWorkbook()));
	XSSFWorkbook taxonWorkbook = new XSSFWorkbook(
		new FileInputStream(taxonWorkbookFile));
	int taxonIdx = -1;
	int catIdx = -1;
	int nameIdx = -1;
	org._3rivers_ashtanga._2013.wos_ebird_summary.taxonomy_order.ObjectFactory objFactory = new org._3rivers_ashtanga._2013.wos_ebird_summary.taxonomy_order.ObjectFactory();
	XSSFSheet sheet = taxonWorkbook.getSheetAt(0);
	for (Row row : sheet) {
	    if (row.getRowNum() == 0) {
		// header
		for (Cell cell : row) {
		    if (cell.getCellType()
			    .equals(CellType.STRING)) {
			String v = cell.getStringCellValue()
				.trim();
			if (EBIRD_TAXON_ORDER_HEADING
				.equals(v)) {
			    taxonIdx = cell
				    .getColumnIndex();
			    if (catIdx >= 0 && nameIdx >= 0)
				break;
			} else if (EBIRD_CATEGORY_HEADER
				.equals(v)) {
			    catIdx = cell.getColumnIndex();
			    if (taxonIdx >= 0
				    && nameIdx >= 0)
				break;
			} else if (EBIRD_COMMON_NAME_HEADING
				.equals(v)) {
			    nameIdx = cell.getColumnIndex();
			    if (taxonIdx >= 0
				    && catIdx >= 0)
				break;
			}
		    }
		}
	    } else {
		// Data
		TaxonType taxonRecord = objFactory
			.createTaxonType();
		taxonRecord.setCategory(row.getCell(catIdx)
			.getStringCellValue().trim());
		taxonRecord.setCommonName(row
			.getCell(nameIdx)
			.getStringCellValue().trim());
		taxonRecord.setTaxonOrder(
			BigInteger.valueOf((long) row
				.getCell(taxonIdx)
				.getNumericCellValue()));
		this.getTaxonMap().put(
			taxonRecord.getCommonName(),
			taxonRecord);
	    }
	}
    }

    private void initializeTripDataRows(
	XSSFWorkbook ebirdDataWbk) {
	int tripIndex = -1;
	int dayIndex = -1;
	int checklistIdx = -1;

	for (Row row : ebirdDataWbk.getSheetAt(0)) {
	    if (tripIndex < 0) {
		// This is first row
		for (Cell cell : row) {
		    if ("Trip".equals(cell
			    .getStringCellValue().trim())) {
			tripIndex = cell.getColumnIndex();
			if (dayIndex >= 0
				&& checklistIdx >= 0)
			    break;
		    } else if ("Day".equals(cell
			    .getStringCellValue().trim())) {
			dayIndex = cell.getColumnIndex();
			if (tripIndex >= 0
				&& checklistIdx >= 0)
			    break;
		    } else if ("Trip Reports/Checklists"
			    .equals(cell
				    .getStringCellValue()
				    .trim())) {
			checklistIdx = cell.getColumnIndex();
			if (tripIndex >= 0 && dayIndex >= 0)
			    break;
		    }
		}
	    } else {
		String checkLists = row.getCell(checklistIdx)
			.getStringCellValue();
		
		if (checkLists != null)
		    checkLists = checkLists.trim();
		else checkLists = "";
		if (!checkLists.isBlank() && checkLists.charAt(0) == '"') {
		    // Take off first and last character
		    checkLists = checkLists.substring(1,checkLists.length()-2);
		}
		this.getTripDataRowMap().put(Pair.of(
			row.getCell(tripIndex)
				.getStringCellValue()
				.trim(),
			row.getCell(dayIndex)
				.getStringCellValue()
				.trim()),
			checkLists);

	    }
	}
    }

    private void reportUnusualBird(
	TripSummaryData speciesData,
	String tripName,
	String tripDay) {
	OtherSpeciesType speciesRecord = this
		.getOtherSpeciesMap()
		.get(speciesData.getSpecies());
	System.out.print(
		"No Species corresponding to Ebird value: "
			+ speciesData.getSpecies());
	String catVal = speciesData.getCategory();
	if (!catVal.isBlank())
	    System.out.print(
		    " (Category: " + catVal.trim() + ")");
	System.out.println();
	speciesRecord = OTHER_SPECIES_OBJ_FACTORY
		.createOtherSpeciesType();
	speciesRecord.setSpecies(speciesData.getSpecies());
	TaxonType taxonRecord = getTaxonMap()
		.get(speciesData.getSpecies().trim());
	speciesRecord.setEbirdCategory(
		taxonRecord.getCategory());
	speciesRecord
		.setTaxonOrder(taxonRecord.getTaxonOrder());
	this.getOtherSpeciesMap().put(
		speciesData.getSpecies(), speciesRecord);
	ObservedByType otherSpeciesObservation = OTHER_SPECIES_OBJ_FACTORY
		.createObservedByType();
	speciesRecord.getObservedBy()
		.add(otherSpeciesObservation);
	// Unusual bird
	otherSpeciesObservation.setCount(
		BigInteger.valueOf(speciesData.getCount()));
	otherSpeciesObservation.setTripDay(tripDay);
	otherSpeciesObservation.setTripName(tripName);
    }

    private void saveData()
	throws FileNotFoundException,
	    IOException,
	    JAXBException {
	File resultWorkbookFile = new File(
		this.getParametersURI()
			.resolve(this.getParameters()
				.getSummaryOutput()));
	resultWorkbookFile.getParentFile().mkdirs();
	this.getSummaryResultWorkbook().write(
		new FileOutputStream(resultWorkbookFile));

	/*
	 * Deprecated
	 */
	/*
	 * if (!this.getOtherSpeciesMap().isEmpty()) { File otherDataFile = new File(
	 * this.getParametersURI() .resolve(this.getParameters()
	 * .getOtherSpeciesOutput() .trim())); System.out.println(
	 * "Information on additional species written to: \n" + otherDataFile
	 * .getAbsolutePath()); AdditionalSpeciesType others = OTHER_SPECIES_OBJ_FACTORY
	 * .createAdditionalSpeciesType(); others.getOtherSpecies().addAll(
	 * this.getOtherSpeciesMap().values()); JAXBContext context = JAXBContext
	 * .newInstance(AdditionalSpeciesType.class .getPackageName()); Marshaller m =
	 * context.createMarshaller(); m.setProperty("jaxb.formatted.output",
	 * Boolean.TRUE); m.marshal(OTHER_SPECIES_OBJ_FACTORY
	 * .createAdditionalSpecies(others), otherDataFile); }
	 */
    }


    private void setFirstTripColumn(
	Integer firstTripColumn) {
	this.firstTripColumn = firstTripColumn;
    }

    private void summarizeEbirdData(
	int tripColumnIndex,
	String tripLists,
	String tripName,
	String tripDay) throws IOException {
	if (tripLists == null || tripLists.isBlank()) {
	    System.err.println(tripName + "-" + tripDay + ": No trip data!");
	    return;
	}
	Integer speciesRowToSum = null;
	int speciesSum = 0;
	for (TripSummaryData speciesData : getDataSourceFactory()
	    .getTripDataSource(tripName,tripDay,tripLists)) {
	String ebirdSpecies = speciesData
		.getSpecies();
	Integer speciesRow = getSpeciesRowMap()
		.get(ebirdSpecies);
	if (speciesRow == null) {
	    // Try again less specifically
	    Matcher matcher = SIMPLE_SPECIES_PATTERN
		    .matcher(ebirdSpecies);
	    if (matcher.matches()) {
		String match = matcher.group(1)
			.trim();
		speciesRow = getSpeciesRowMap()
			.get(match);
	    }
	}
	if (speciesRow != null) {
	    // Normal processing
	    if (speciesRowToSum != speciesRow) {
		if (speciesRowToSum != null) {
		    Cell outputCell = getOutputCell(
			    speciesRowToSum,
			    tripColumnIndex);
		    outputCell.setCellValue(
			    speciesSum);
		}
		speciesSum = 0; // clear out reported data
		speciesRowToSum = speciesRow; // set up next value to report
	    }
	    // accumulate this row
	    speciesSum += speciesData.getCount();
	} else {
	    reportUnusualBird(speciesData, tripName,
		    tripDay);
	}
	}
	// Finish up reporting with last species
	if (speciesRowToSum != null) {
	Cell outputCell = getOutputCell(
		speciesRowToSum, tripColumnIndex);
	outputCell.setCellValue(speciesSum);
	}
    }

    private TripSummaryDataSourceFactory getDataSourceFactory() {
	return this.dataSourceFactory;
    }
}
