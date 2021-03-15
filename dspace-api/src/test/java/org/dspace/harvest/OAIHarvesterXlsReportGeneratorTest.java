/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.harvest.model.OAIHarvesterReport;
import org.junit.Test;

/**
 * Unit tests for {@link OAIHarvesterXlsReportGeneratorImpl}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterXlsReportGeneratorTest {

    private OAIHarvesterXlsReportGeneratorImpl reportGenerator = new OAIHarvesterXlsReportGeneratorImpl();

    @Test
    public void testReportGeneration() throws Exception {

        HarvestedCollection harvestRow = mock(HarvestedCollection.class);
        when(harvestRow.getOaiSource()).thenReturn("https://test/oai");

        OAIHarvesterReport report = new OAIHarvesterReport(20, "oai_cerif_openaire");
        report.addError("publication-01", "Invalid <author> element found", "created");
        report.addError("publication-01", "Invalid <editor> element found", "created");
        report.addError("publication-02", "Fatal error occurs", "none");
        report.addError("publication-03", List.of("Validation error 1", "Validation error 2"), "created");

        InputStream inputStream = reportGenerator.generate(report, harvestRow);

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            assertThat(workbook.getNumberOfSheets(), is(1));

            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows(), is(6));
            asserThatRowHasValues(sheet.getRow(0), "Record identifier", "Record link", "Error", "Action");

            asserThatRowHasValues(sheet.getRow(1), "publication-01",
                "https://test/oai?verb=GetRecord&identifier=publication-01&metadataPrefix=oai_cerif_openaire",
                "Invalid <author> element found", "created");

            asserThatRowHasValues(sheet.getRow(2), "publication-01",
                "https://test/oai?verb=GetRecord&identifier=publication-01&metadataPrefix=oai_cerif_openaire",
                "Invalid <editor> element found", "created");

            asserThatRowHasValues(sheet.getRow(3), "publication-02",
                "https://test/oai?verb=GetRecord&identifier=publication-02&metadataPrefix=oai_cerif_openaire",
                "Fatal error occurs", "none");

            asserThatRowHasValues(sheet.getRow(4), "publication-03",
                "https://test/oai?verb=GetRecord&identifier=publication-03&metadataPrefix=oai_cerif_openaire",
                "Validation error 1", "created");

            asserThatRowHasValues(sheet.getRow(5), "publication-03",
                "https://test/oai?verb=GetRecord&identifier=publication-03&metadataPrefix=oai_cerif_openaire",
                "Validation error 2", "created");

        }
    }

    private void asserThatRowHasValues(Row row, String... cellValues) {
        assertThat(row.getPhysicalNumberOfCells(), is(cellValues.length));
        for (int i = 0; i < cellValues.length; i++) {
            assertThat(row.getCell(i).getStringCellValue(), is(cellValues[i]));
        }
    }
}
