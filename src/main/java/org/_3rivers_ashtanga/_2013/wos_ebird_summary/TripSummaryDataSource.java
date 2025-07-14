package org._3rivers_ashtanga._2013.wos_ebird_summary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class TripSummaryDataSource
    implements
	Iterable<TripSummaryData>
{

    private final SortedSet<
	    TripSummaryData> summaryList = new TreeSet<>();

    public TripSummaryDataSource(File dataWbkFile)
	throws FileNotFoundException,
	    IOException {
	XSSFWorkbook dataWbk = new XSSFWorkbook(
		new FileInputStream(dataWbkFile));
	XSSFSheet dataSheet = dataWbk.getSheetAt(0);
	int catIdx = -1;
	int speciesIdx = -1;
	int colCnt = -1;
	for (Row row : dataSheet) {
	    if (colCnt < 0) {
		// this is the first row
		// ignore the 'sum' column
		colCnt = row.getLastCellNum() - 1;
		for (Cell cell : row) {
		    String cellValue = cell
			    .getStringCellValue().trim();
		    if (catIdx < 0 && "Category"
			    .equals(cellValue)) {
			catIdx = cell.getColumnIndex();
			if (speciesIdx >= 0)
			    break;
		    }
		    if (speciesIdx < 0 && "Species"
			    .equals(cellValue)) {
			speciesIdx = cell.getColumnIndex();
			if (catIdx >= 0)
			    break;
		    }
		}
	    } else {
		Cell speciesCell = row.getCell(speciesIdx);
		String speciesName = "";
		if (speciesCell != null) {
		    speciesName = speciesCell.getStringCellValue();
		    if (speciesName != null)
			speciesName = speciesName.trim();
		    else speciesName = "";
		}
		// Skip rows with no species
		if (!speciesName.isBlank()
			&& Character.isUpperCase(
				speciesName.charAt(0))) {
		    int cnt = 0;
		    for (int i = speciesIdx
			    + 1; i < colCnt; ++i) {
			Cell dataCell = row.getCell(i);
			if (dataCell != null) {
			    CellType cellType = dataCell
				    .getCellType();
			    switch (cellType) {
			    case BLANK:
			    case BOOLEAN:
			    case ERROR:
			    case FORMULA:
				break;
			    case NUMERIC:
				cnt += dataCell
					.getNumericCellValue();
				break;
			    case STRING:
				if ("X".equalsIgnoreCase(
					dataCell.getStringCellValue()
						.trim()))
				    // we know at least one was seen
				    cnt += 1;
				break;
			    case _NONE:
			    default:
				break;

			    }
			}
		    }
		    if (cnt > 0) {
			String catValue = "";
			Cell catCell = null;
			// Looks like if category isn't required it isn't included
			if (catIdx >= 0)
			    catCell = row.getCell(catIdx);
			if (catCell != null) {
			    catValue = catCell
				    .getStringCellValue();
			}
			this.getSummaryList()
				.add(new TripSummaryData(
					speciesName,
					catValue, cnt));
		    }
		}

	    }
	}
    }

    @Override
    public Iterator<TripSummaryData> iterator() {
	return getSummaryList().iterator();
    }

    private SortedSet<TripSummaryData> getSummaryList() {
	return this.summaryList;
    }

}
