package org._3rivers_ashtanga._2013.wos_ebird_summary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;

import org._3rivers_ashtanga._2013.jaxb.jaxb_app.JAXBAppClass;
import org._3rivers_ashtanga._2013.jaxb.jaxb_app.JAXBAppFactoryClass;
import org._3rivers_ashtanga._2013.wos_ebird_summary.input_parameters.ParametersType;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbookFactory;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
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

    private WosEbirdSummary(
	URI baseUri,
	ParametersType parameters,
	Marshaller marshaller,
	NamedNodeMap attMap)
	throws IOException {
	super(baseUri, parameters, marshaller, attMap);
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
	File resultWorkbookFile = new File(
		this.getParametersURI()
			.resolve(this.getParameters()
				.getSummaryOutput()));
	File sourceWorkbookFile = new File(
		this.getParametersURI()
			.resolve(this.getParameters()
				.getWorkbookSource()));
	File ebirdDataWorkbookFile = new File(this.getParametersURI().resolve(
		this.getParameters().getEbirdDataWorkbook()));
	resultWorkbookFile.getParentFile().mkdirs();
	try {
	    Files.copy(sourceWorkbookFile.toPath(),
		    resultWorkbookFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
	    // We're going to copy source workbook to result to start
	    XSSFWorkbook result = new XSSFWorkbook(new FileInputStream(
			    resultWorkbookFile));
	    XSSFWorkbook ebirdDataWorkbook
	    = new XSSFWorkbook(new FileInputStream(ebirdDataWorkbookFile));
	    XSSFSheet resultSheet = result.getSheetAt(0);
	    XSSFRow tripRow = resultSheet.getRow(0);
	    XSSFRow dayRow = resultSheet.getRow(1);
	    String tripName = null;
	    String tripDay = null;
	    for (Cell tripCell : tripRow) {
		int columnIndex = tripCell.getColumnIndex();
		String tripCellValue = tripCell.getStringCellValue();
		if (tripCell.getColumnIndex() > 0
			&& !tripCellValue.isBlank()) {
		    tripName = tripCellValue;
		}
		if (tripName != null) {
		    tripDay = dayRow.getCell(columnIndex).getStringCellValue();
		}
	    }
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}

    }
}
