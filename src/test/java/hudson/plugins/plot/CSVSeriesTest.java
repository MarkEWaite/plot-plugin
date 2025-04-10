/*
 * Copyright (c) 2008-2009 Yahoo! Inc.  All rights reserved.
 * The copyrights to the contents of this file are licensed under the MIT License (http://www.opensource.org/licenses/mit-license.php)
 */
package hudson.plugins.plot;

import static hudson.plugins.plot.SeriesTestUtils.WORKSPACE_ROOT_DIR;
import static hudson.plugins.plot.SeriesTestUtils.testPlotPoints;
import static hudson.plugins.plot.SeriesTestUtils.testSeries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencsv.CSVReader;
import hudson.FilePath;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

/**
 * Test a CSV series.
 *
 * @author Allen Reese
 */
class CSVSeriesTest {
    private static final Logger LOGGER = Logger.getLogger(CSVSeriesTest.class.getName());

    private static final String[] FILES = {
        "test.csv", "test_trailing_spaces.csv", "test_trailing_semicolon.csv", "test_multiple*.csv"
    };
    private static final int[] LINES = {2, 3, 2, 4}; // lines in the file including header
    private static final int[] COLUMNS = {8, 3, 9, 2}; // columns in the file
    private static final int[] CORRECTED_COLUMNS = {8, 3, 8, 2}; // corrected for the trailing comma case
    private static final int[] TOTAL_POINTS = {8, 6, 8, 6}; // total data points in the file
    private static final String[] LAST_COLUMN_NAME = {"error %", "thing", "error %", "bravo"
    }; // the label on the last column

    @Test
    void testCSVSeriesWithNullExclusionValuesSetsDisplayTableFlag() {
        CSVSeries series;
        for (String file : FILES) {
            series = new CSVSeries(file, null, null, null, true);
            assertTrue(series.getDisplayTableFlag());
        }
    }

    @Test
    void testCSVSeriesWithNoExclusions() throws Exception {
        for (int index = 0; index < FILES.length; index++) {
            // Check the number of columns
            int columns = getNumColumns(FILES[index]);

            assertEquals(COLUMNS[index], columns);

            // Create a new CSV series.
            CSVSeries series = new CSVSeries(FILES[index], "http://localhost:8080/%name%/%index%/", "OFF", "", false);

            LOGGER.info("Created series " + series);
            // test the basic subclass properties.
            testSeries(series, FILES[index], "", "csv");

            // load the series.
            List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
            LOGGER.info("Got " + points.size() + " plot points");
            testPlotPoints(points, TOTAL_POINTS[index]);

            int numberOfPoints = 0;
            for (int lines = 1; lines < LINES[index]; lines++) {
                for (int columnIndex = 0; columnIndex < CORRECTED_COLUMNS[index]; columnIndex++) {
                    PlotPoint point = points.get(numberOfPoints);
                    assertEquals("http://localhost:8080/" + point.getLabel() + "/" + columnIndex + "/", point.getUrl());
                    numberOfPoints++;
                }
            }
        }
    }

    @Test
    void testCSVSeriesIncludeOnlyLastColumn() {
        for (int index = 0; index < FILES.length; index++) {
            // Create a new CSV series.
            CSVSeries series = new CSVSeries(
                    FILES[index],
                    "http://localhost:8080/%name%/%index%/",
                    "INCLUDE_BY_STRING",
                    LAST_COLUMN_NAME[index],
                    false);

            LOGGER.info("Created series " + series);

            // load the series.
            List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
            LOGGER.info("Got " + points.size() + " plot points");
            testPlotPoints(points, LINES[index] - 1); // expect one point per line, minus one header line

            PlotPoint point = points.get(0);
            int columnIndex = CORRECTED_COLUMNS[index] - 1; // correct column to starting index of 0
            assertEquals("http://localhost:8080/" + point.getLabel() + "/" + columnIndex + "/", point.getUrl());
        }
    }

    @Test
    void testCSVSeriesWithTrailingSemicolonDoesntCreateExtraneousPoint() {
        String file = "test_trailing_semicolon.csv";
        // Create a new CSV series.
        CSVSeries series = new CSVSeries(file, "http://localhost:8080/%name%/%index%/", "OFF", "", false);

        LOGGER.info("Created series " + series);
        // test the basic subclass properties.
        testSeries(series, file, "", "csv");

        // load the series.
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 8);
    }

    @Test
    void testCSVExclusionValue_IntArray() {
        /*
         * header before:       Avg,Median,90,min,max,samples,errors,error %
         * header afterwards:   Avg,Median,90,min,max,samples,errors,error %
         */
        CSVSeries series = new CSVSeries("test.csv", null, "EXCLUDE_BY_STRING", "123,345", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 8);
    }

    @Test
    void testCSVInclusionValue_IntArray() {
        /*
         * header before:       Avg,Median,90,min,max,samples,errors,error %
         * header afterwards:
         */
        CSVSeries series = new CSVSeries("test.csv", null, "INCLUDE_BY_STRING", "123,345", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 0);
    }

    @Test
    void testExcludeByRegexInAList() {
        /*
         * header before:       RunId,Trend Measurement Type,HTTP_200,HTTP_201,HTTP_302,HTTP_500,Hits,Throughput,
         * header afterwards:   HTTP_200,HTTP_201,HTTP_302
         */
        CSVSeries series = new CSVSeries(
                "test_regex_webstatistics.csv",
                null,
                "EXCLUDE_BY_STRING",
                "\"HTTP_[4,5]\\d{2}\",\"Hits\",\"Throughput\",\"RunId\",\"Trend Measurement Type\"",
                false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 3);
    }

    @Test
    void testExcludeColumnWithParentheses() {
        /*
         * header before:       Avg,Median,90,(min,max,samples,errors
         * header afterwards:   Median,90,max,samples,errors
         */
        CSVSeries series = new CSVSeries("test_regex_parentheses.csv", null, "EXCLUDE_BY_STRING", "Avg,(min", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 5);
    }

    @Test
    void testIncludeByRegex() {
        /*
         * header before: RunId,Trend Measurement Type,HTTP_200,HTTP_201,HTTP_302,HTTP_500,Hits,Throughput,
         * header afterwards:   HTTP_200,HTTP_201,HTTP_302
         */
        CSVSeries series =
                new CSVSeries("test_regex_webstatistics.csv", null, "INCLUDE_BY_STRING", "\"HTTP_[2,3]\\d{2}\"", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 3);
    }

    @Test
    void testIncludeBySingleRegexWrongPattern_shouldFindNoPoints() {
        CSVSeries series = new CSVSeries("test_regex_webstatistics.csv", null, "INCLUDE_BY_STRING", "\"(HTTP\"", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 0);
    }

    @Test
    void testIncludeBySuffixByRegex() {
        /*
         * header before:       RunId,StartPage_0010_OpenStartPage_testUser_1,StartPage_0010_OpenStartPage_testUser_2,StartPage_0010_OpenStartPage_testUser_3,StartPage_0010_OpenStartPage_testUser_4,Login_0010_Login_testUser_1,Login_0010_Login_testUser_2,Login_0010_Login_testUser_3,Login_0010_Login_testUser_4,UploadAsset_0010_OpenPerformancetestAssets_testUser_1,UploadAsset_0010_OpenPerformancetestAssets_testUser_2,UploadAsset_0010_OpenPerformancetestAssets_testUser_3,UploadAsset_0010_OpenPerformancetestAssets_testUser_4
         * header afterwards:   StartPage_0010_OpenStartPage_testUser_1,StartPage_0010_OpenStartPage_testUser_2,Login_0010_Login_testUser_1,Login_0010_Login_testUser_2,UploadAsset_0010_OpenPerformancetestAssets_testUser_1,UploadAsset_0010_OpenPerformancetestAssets_testUser_2
         */
        CSVSeries series = new CSVSeries(
                "test_regex_by_suffix.csv", null, "INCLUDE_BY_STRING", "\".*testUser_1\",\".*testUser_2\"", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 6);
    }

    @Test
    void testExcludeBySuffixByRegex() {
        /*
         * Testing a little more complex regex with case insensitive and boundaries.
         *
         * header before:       RunId,StartPage_0010_OpenStartPage_testUser_1,StartPage_0010_OpenStartPage_testUser_2,StartPage_0010_OpenStartPage_testUser_3,StartPage_0010_OpenStartPage_testUser_4,Login_0010_Login_testUser_1,Login_0010_Login_testUser_2,Login_0010_Login_testUser_3,Login_0010_Login_testUser_4,UploadAsset_0010_OpenPerformancetestAssets_testUser_1,UploadAsset_0010_OpenPerformancetestAssets_testUser_2,UploadAsset_0010_OpenPerformancetestAssets_testUser_3,UploadAsset_0010_OpenPerformancetestAssets_testUser_4
         * header afterwards:   StartPage_0010_OpenStartPage_testUser_3,StartPage_0010_OpenStartPage_testUser_4,UploadAsset_0010_OpenPerformancetestAssets_testUser_3,UploadAsset_0010_OpenPerformancetestAssets_testUser_4
         */
        CSVSeries series = new CSVSeries(
                "test_regex_by_suffix.csv",
                null,
                "EXCLUDE_BY_STRING",
                "\"(?i)(RunID)\",\"Login_.*\",\".*testUser_[1-2]{1,2}\"",
                false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 4);
    }

    @Test
    void testExcludeHeaderByRegex() {
        /*
         * header before:       recs avg,recs min,recs max,station avg,station min,station max,personalized avg,personalized min,personalized max,autoplay avg,autoplay min,autoplay max,station count,personalized count,autoplay count,threads,host,errors
         * header afterwards:   recs avg,station avg,personalized avg,autoplay avg,station count,personalized count,autoplay count,errors
         */
        CSVSeries series = new CSVSeries(
                "test_exclusions.csv", null, "EXCLUDE_BY_STRING", "\".*min\",\".*max\",\"host\",\"threads\"", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 8);
    }

    @Test
    void testIncludeHeaderByRegex() {
        /*
         * header before:       recs avg,recs min,recs max,station avg,station min,station max,personalized avg,personalized min,personalized max,autoplay avg,autoplay min,autoplay max,station count,personalized count,autoplay count,threads,host,errors
         * header afterwards:   recs avg,station avg,personalized avg,autoplay avg
         */
        CSVSeries series = new CSVSeries("test_exclusions.csv", null, "INCLUDE_BY_STRING", "\".*avg\"", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 4);
    }

    /**
     * Making sure, the 3 exclusionValues are put into the List by surrounding even the String with ""
     */
    @Test
    void testIncludeHeaderByRegexAndEscapedString() {
        /*
         * header before:       recs avg,recs min,recs max,station avg,station min,station max,personalized avg,personalized min,personalized max,autoplay avg,autoplay min,autoplay max,station count,personalized count,autoplay count,threads,host,errors
         * header afterwards:   recs avg,station avg,personalized avg,autoplay avg,autoplay count,errors
         */
        CSVSeries series = new CSVSeries(
                "test_exclusions.csv", null, "INCLUDE_BY_STRING", "\"errors\",\".*avg\",\"autoplay count\"", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 6);
    }

    /**
     * By not surrounding single Strings with "", only the Regex will be put into the List
     */
    @Test
    void testIncludeHeaderByRegexAndUnescapedString() {
        /*
         * header before:       recs avg,recs min,recs max,station avg,station min,station max,personalized avg,personalized min,personalized max,autoplay avg,autoplay min,autoplay max,station count,personalized count,autoplay count,threads,host,errors
         * header afterwards:   recs avg,station avg,personalized avg,autoplay avg
         */
        CSVSeries series = new CSVSeries(
                "test_exclusions.csv", null, "INCLUDE_BY_STRING", "errors,\".*avg\",autoplay count", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 4);
    }

    @Test
    void testIncludeByRegexInAString() {
        /*
         * header before:       RunId,StartPage_0010_OpenStartPage_testUser_1,StartPage_0010_OpenStartPage_testUser_2,StartPage_0010_OpenStartPage_testUser_3,StartPage_0010_OpenStartPage_testUser_4,Login_0010_Login_testUser_1,Login_0010_Login_testUser_2,Login_0010_Login_testUser_3,Login_0010_Login_testUser_4,UploadAsset_0010_OpenPerformancetestAssets_testUser_1,UploadAsset_0010_OpenPerformancetestAssets_testUser_2,UploadAsset_0010_OpenPerformancetestAssets_testUser_3,UploadAsset_0010_OpenPerformancetestAssets_testUser_4
         * header afterwards:   StartPage_0010_OpenStartPage_testUser_1,StartPage_0010_OpenStartPage_testUser_2,StartPage_0010_OpenStartPage_testUser_3,StartPage_0010_OpenStartPage_testUser_4,Login_0010_Login_testUser_1,Login_0010_Login_testUser_2,Login_0010_Login_testUser_3,Login_0010_Login_testUser_4
         */
        CSVSeries series = new CSVSeries(
                "test_regex_by_suffix.csv", null, "INCLUDE_BY_STRING", "\".*_(OpenStartPage|Login)_.*\"", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 8);
    }

    @Test
    void testIncludeByString() {
        /*
         * header before:       Avg,Median,90,min,max,samples,errors,error %
         * header afterwards:   Avg,Median
         */
        CSVSeries series = new CSVSeries("test.csv", null, "INCLUDE_BY_STRING", "Avg,Median", false);
        List<PlotPoint> points = series.loadSeries(WORKSPACE_ROOT_DIR, 0, System.out);
        LOGGER.info("Got " + points.size() + " plot points");
        testPlotPoints(points, 2);
    }

    private static int getNumColumns(String file) throws Exception {
        FilePath[] seriesFiles = WORKSPACE_ROOT_DIR.list(file);
        assertFalse(
                seriesFiles.length < 1,
                "No plot data file found: " + SeriesTestUtils.WORKSPACE_ROOT_DIR.getName() + " " + file);

        LOGGER.info("Loading plot series data from: " + file);
        try (InputStream inputStream = seriesFiles[0].read();
                InputStreamReader inputReader = new InputStreamReader(inputStream);
                CSVReader csvReader = new CSVReader(inputReader)) {

            // save the header line to use it for the plot labels.
            String[] headerLine = csvReader.readNext();

            LOGGER.info("Got " + headerLine.length + " columns");
            return headerLine.length;
        }
    }
}
