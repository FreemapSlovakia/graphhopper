/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.reader.dem;

import com.graphhopper.util.Helper;

import java.io.*;

/**
 * Elevation data from NASA (SRTM).
 * <p>
 * Important information about SRTM: the coordinates of the lower-left corner of tile N40W118 are 40
 * degrees north latitude and 118 degrees west longitude. To be more exact, these coordinates refer
 * to the geometric center of the lower left sample, which in the case of SRTM3 data will be about
 * 90 meters in extent.
 * <p>
 *
 * @author Peter Karich
 */
public class FreemapSrtmProvider extends AbstractSRTMElevationProvider {
    public FreemapSrtmProvider() {
        this("");
    }

    public FreemapSrtmProvider(String cacheDir) {
        super(
                "https://www.freemap.sk/~martin/hgt/",
                cacheDir.isEmpty()? "/tmp/srtm": cacheDir,
                "GraphHopper SRTMReader",
                -56,
                60,
                1201
        );
    }

    public static void main(String[] args) throws IOException {
        FreemapSrtmProvider provider = new FreemapSrtmProvider();
        System.out.println(provider.getEle(48.96, 19.92));
    }

    @Override
    public String toString() {
        return "srtm-freemap";
    }

    @Override
    byte[] readFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        BufferedInputStream buff = new BufferedInputStream(is);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        int len;
        while ((len = buff.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
        os.flush();
        Helper.close(buff);
        return os.toByteArray();
    }

    @Override
    String getFileName(double lat, double lon) {
        int minLat = Math.abs(down(lat));
        int minLon = Math.abs(down(lon));
        String str = "";
        if (lat >= 0)
            str += "N";
        else
            str += "S";

        if (minLat < 10)
            str += "0";
        str += minLat;

        if (lon >= 0)
            str += "E";
        else
            str += "W";

        if (minLon < 10)
            str += "0";
        if (minLon < 100)
            str += "0";
        str += minLon;
        return str;
    }

    @Override
    String getDownloadURL(double lat, double lon) {
        return getFileName(lat, lon) + ".HGT";
    }
}
