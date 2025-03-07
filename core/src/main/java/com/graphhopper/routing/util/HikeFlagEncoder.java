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
package com.graphhopper.routing.util;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.weighting.PriorityWeighting;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.*;

import java.util.TreeMap;

import static com.graphhopper.routing.ev.RouteNetwork.*;
import static com.graphhopper.routing.util.PriorityCode.*;

/**
 * Defines bit layout for hiking
 *
 * @author Peter Karich
 */
public class HikeFlagEncoder extends FootFlagEncoder {

    public HikeFlagEncoder() {
        this(4, 1, false);
    }

    public HikeFlagEncoder(PMap properties) {
        this(
                properties.getString("name", "hike"),
                properties.getInt("speed_bits", 4),
                properties.getDouble("speed_factor", 1),
                properties.getBool("speed_two_directions", false));

        blockPrivate(properties.getBool("block_private", true));
        blockFords(properties.getBool("block_fords", false));
    }

    protected HikeFlagEncoder(int speedBits, double speedFactor, boolean speedTwoDirections) {
        this("hike", speedBits, speedFactor, speedTwoDirections);
    }

    protected HikeFlagEncoder(String name, int speedBits, double speedFactor, boolean speedTwoDirections) {
        super(name, speedBits, speedFactor, speedTwoDirections);

        routeMap.put(INTERNATIONAL, EX_BEST.getValue());
        routeMap.put(NATIONAL, EX_BEST.getValue());
        routeMap.put(REGIONAL, EX_VERY_NICE.getValue());
        routeMap.put(LOCAL, EX_VERY_NICE.getValue());

        // hiking allows all sac_scale values
        allowedSacScale.add("alpine_hiking");
        allowedSacScale.add("demanding_alpine_hiking");
        allowedSacScale.add("difficult_alpine_hiking");
    }

    @Override
    void collect(ReaderWay way, TreeMap<Double, Integer> weightToPrioMap) {
        String highway = way.getTag("highway");
        if (way.hasTag("foot", "designated"))
            weightToPrioMap.put(100d, PREFER.getValue());

        double maxSpeed = getMaxSpeed(way);
        if (safeHighwayTags.contains(highway) || (isValidSpeed(maxSpeed) && maxSpeed <= 20)) {
            weightToPrioMap.put(40d, PREFER.getValue());
            if (way.hasTag("tunnel", intendedValues)) {
                if (way.hasTag("sidewalk", sidewalksNoValues))
                    weightToPrioMap.put(40d, AVOID.getValue());
                else
                    weightToPrioMap.put(40d, UNCHANGED.getValue());
            }
        } else if ((isValidSpeed(maxSpeed) && maxSpeed > 50) || avoidHighwayTags.contains(highway)) {
            if (way.hasTag("sidewalk", sidewalksNoValues))
                weightToPrioMap.put(45d, BAD.getValue());
            else
                weightToPrioMap.put(45d, AVOID.getValue());
        }

        if (way.hasTag("bicycle", "official") || way.hasTag("bicycle", "designated"))
            weightToPrioMap.put(44d, SLIGHT_AVOID.getValue());
    }


    @Override
    public void applyWayTags(ReaderWay way, EdgeIteratorState edge) {
        PointList pl = edge.fetchWayGeometry(FetchMode.ALL);
        if (!pl.is3D())
            return;

        if (way.hasTag("tunnel", "yes") || way.hasTag("bridge", "yes") || way.hasTag("highway", "steps"))
            // do not change speed
            // note: although tunnel can have a difference in elevation it is unlikely that the elevation data is correct for a tunnel
            return;

        // Decrease the speed for ele increase (incline), and slightly decrease the speed for ele decrease (decline)
        double prevEle = pl.getEle(0);
        double fullDistance = edge.getDistance();

        // for short edges an incline makes no sense and for 0 distances could lead to NaN values for speed, see #432
        if (fullDistance < 2)
            return;

        double eleDelta = Math.abs(pl.getEle(pl.size() - 1) - prevEle);
        double slope = eleDelta / fullDistance;

        IntsRef edgeFlags = edge.getFlags();
        if ((accessEnc.getBool(false, edgeFlags) || accessEnc.getBool(true, edgeFlags))
                && slope > 0.005) {

            // see #1679 => v_hor=4.5km/h for horizontal speed; v_vert=2*0.5km/h for vertical speed (assumption: elevation < edge distance/4.5)
            // s_3d/v=h/v_vert + s_2d/v_hor => v = s_3d / (h/v_vert + s_2d/v_hor) = sqrt(s²_2d + h²) / (h/v_vert + s_2d/v_hor)
            // slope=h/s_2d=~h/2_3d              = sqrt(1+slope²)/(slope+1/4.5) km/h
            // maximum slope is 0.37 (Ffordd Pen Llech)
            double newSpeed = Math.sqrt(1 + slope * slope) / (slope + 1 / 5.4);
            edge.set(avgSpeedEnc, Helper.keepIn(newSpeed, 1, 5));
        }
    }

    @Override
    public boolean supports(Class<?> feature) {
        if (super.supports(feature))
            return true;

        return PriorityWeighting.class.isAssignableFrom(feature);
    }

}
