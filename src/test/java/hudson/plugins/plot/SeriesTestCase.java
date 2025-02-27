/*
 * Copyright (c) 2008-2009 Yahoo! Inc.  All rights reserved.
 * The copyrights to the contents of this file are licensed under the MIT License (http://www.opensource.org/licenses/mit-license.php)
 */

package hudson.plugins.plot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hudson.FilePath;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Stub to hold common series test functionality.
 *
 * @author Allen Reese
 */
public class SeriesTestCase {
    private static final Logger LOGGER = Logger.getLogger(SeriesTestCase.class.getName());

    @SuppressWarnings("visibilitymodifier")
    protected final FilePath workspaceRootDir = createTestDirectory();

    public void testSeries(Series series, String file, String label, String type) {
        // verify the properties was created correctly
        assertNotNull(series);

        assertEquals("File name is not configured correctly", file, series.file);
        assertEquals("Label is not configured correctly", label, series.label);
        assertEquals("Type is not configured correctly", type, series.fileType);
    }

    public void testPlotPoints(List<PlotPoint> points, int expected) {
        assertTrue("Must have more than 0 columns", expected > -1);

        assertNotNull("loadSeries failed to return any points", points);
        if (points.size() != expected) {
            StringBuilder debug = new StringBuilder();
            int i = 0;
            for (PlotPoint p : points) {
                debug.append("[").append(i++).append("]").append(p).append("\n");
            }

            assertEquals(
                    "loadSeries loaded wrong number of points: expected " + expected + ", got " + points.size() + "\n"
                            + debug,
                    expected,
                    points.size());
        }

        // validate each point.
        for (int i = 0; i < points.size(); i++) {
            assertNotNull("loadSeries returned null point at index " + i, points.get(i));
            assertNotNull(
                    "loadSeries returned null yvalue at index " + i,
                    points.get(i).getYvalue());
            assertNotNull(
                    "loadSeries returned null url at index " + i, points.get(i).getUrl());
            assertNotNull(
                    "loadSeries returned null label at index " + i,
                    points.get(i).getLabel());

            // make sure the yvalue's can be parsed
            try {
                Double.parseDouble(points.get(i).getYvalue());
            } catch (NumberFormatException nfe) {
                fail("loadSeries returned invalid yvalue "
                        + points.get(i).getYvalue() + " at index " + i
                        + " Exception " + nfe);
            }
        }
    }

    private FilePath createTestDirectory() {
        File file = new File("target/test-classes/");
        FilePath dir = new FilePath(file);
        LOGGER.info("Workspace File path: " + file.getAbsolutePath());
        LOGGER.info("Workspace Dir path: " + dir.getName());
        return dir;
    }
}
