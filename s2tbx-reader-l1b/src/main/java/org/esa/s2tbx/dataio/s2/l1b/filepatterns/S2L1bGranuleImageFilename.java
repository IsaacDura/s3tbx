/*
 *
 * Copyright (C) 2013-2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s2tbx.dataio.s2.l1b.filepatterns;

import org.esa.snap.util.logging.BeamLogManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Norman Fomferra
 */
public class S2L1bGranuleImageFilename {

    final static String REGEX = "(S2A|S2B|S2_)_([A-Z|0-9]{4})_([A-Z|0-9|_]{4})([A-Z|0-9|_]{6})_([A-Z|0-9|_]{4})_([0-9]{8}T[0-9]{6})_S([0-9]{8}T[0-9]{6})(_D(0[1-9]|1[0-2]))(_B[A-B|0-9]{2})(\\.[A-Z|a-z|0-9]{3,4})?";
    final static Pattern PATTERN = Pattern.compile(REGEX);

    public final String name;
    public final String missionID;
    public final String fileClass;
    public final String fileCategory;
    public final String fileSemantic;
    public final String siteCentre;
    public final String creationDate;
    public final String absoluteOrbit;
    public final String tileNumber;
    public final String bandIndex;

    private S2L1bGranuleImageFilename(String name, String missionID, String fileClass, String fileCategory, String fileSemantic, String siteCentre, String creationDate, String instanceID, String absoluteOrbit, String tileNumber, String bandIndex) {
        this.name = name;
        this.missionID = missionID;
        this.fileClass = fileClass;
        this.fileCategory = fileCategory;
        this.fileSemantic = fileSemantic;
        this.siteCentre = siteCentre;
        this.creationDate = creationDate;
        this.absoluteOrbit = absoluteOrbit;
        this.tileNumber = tileNumber;
        this.bandIndex = bandIndex;
    }

    public static boolean isImageFilename(String name) {
        return PATTERN.matcher(name).matches();
    }

    public int getBandIndex() {
        return Integer.parseInt(bandIndex);
    }

    public String getTileNumber() {
        return tileNumber;
    }

    public static S2L1bGranuleImageFilename create(String fileName) {
        final Matcher matcher = PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return new S2L1bGranuleImageFilename(fileName,
                                                 matcher.group(1),
                                                 matcher.group(2),
                                                 matcher.group(3),
                                                 matcher.group(4),
                                                 matcher.group(5),
                                                 matcher.group(6),
                                                 matcher.group(7),
                                                 matcher.group(8),
                                                 matcher.group(9),
                                                 matcher.group(10));
        } else {
            // todo add a warning message too
            BeamLogManager.getSystemLogger().warning(String.format("%s GranuleImageFilename didn't match regexp %s", fileName, PATTERN.toString()));
            return null;
        }
    }
}
