package com.mkl.eu.front.client.map.marker;

import com.mkl.eu.client.service.vo.enumeration.TerrainEnum;
import com.mkl.eu.front.client.map.MapConfiguration;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.SimplePolygonMarker;
import de.fhpotsdam.unfolding.utils.GeoUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import org.apache.commons.lang3.StringUtils;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** @author MKL. */
public class ProvinceMarker extends SimplePolygonMarker implements IMapMarker {
    /** Neighbours of the province. */
    private List<BorderMarker> neighbours = new ArrayList<>();
    /** Counters of the province. */
    private List<StackMarker> stacks = new ArrayList<>();
    /** Lower left location of the shape. X and Y are inversed due to unfolding bug. */
    private Location topLeft;
    /** Upper right location of the shape. X and Y are inversed due to unfolding bug. */
    private Location bottomRight;
    /** Center of the shape. */
    private Location center;
    /** Parent. */
    private IMapMarker parent;

    /**
     * Constructor.
     *
     * @param locations borders of the province.
     */
    public ProvinceMarker(List<Location> locations) {
        this(locations, null);
    }

    /**
     * Constructor.
     *
     * @param locations  borders of the province.
     * @param properties of the province.
     */
    public ProvinceMarker(List<Location> locations, HashMap<String, Object> properties) {
        super(locations, properties);

        computeExtremes();
    }

    /** Calculate the lower left and upper right locations of the province. */
    private void computeExtremes() {
        for (Location location : locations) {
            if (topLeft == null) {
                topLeft = new Location(location);
            } else {
                if (location.getLon() < topLeft.getLon()) {
                    topLeft.setLon(location.getLon());
                }
                if (location.getLat() > topLeft.getLat()) {
                    topLeft.setLat(location.getLat());
                }
            }
            if (bottomRight == null) {
                bottomRight = new Location(location);
            } else {
                if (location.getLon() > bottomRight.getLon()) {
                    bottomRight.setLon(location.getLon());
                }
                if (location.getLat() < bottomRight.getLat()) {
                    bottomRight.setLat(location.getLat());
                }
            }
        }

        center = GeoUtils.getCentroid(locations);
    }

    /** {@inheritDoc} */
    @Override
    public void draw(UnfoldingMap map, List<StackMarker> stacksToIgnore) {
        Location bottomRightBorder = map.getBottomRightBorder();

        Location topLeftBorder = map.getTopLeftBorder();

        // map.getBottomRightBorder and map.getTopLeftBorder have inversed locations (x and y).
        if (bottomRightBorder.getLon() > topLeft.getLon() && topLeftBorder.getLon() < bottomRight.getLon()
                && bottomRightBorder.getLat() < topLeft.getLat() && topLeftBorder.getLat() > bottomRight.getLat()) {
            if ((MapConfiguration.isWithColor() || selected)) {
                super.draw(map);
            }


            PGraphics pg = map.mapDisplay.getOuterPG();

            pg.pushStyle();
            pg.imageMode(PConstants.CORNER);
            float[] xy = map.mapDisplay.getObjectFromLocation(center);
            float relativeSize = Math.abs(xy[0] - map.mapDisplay.getObjectFromLocation(new Location(center.getLat()
                    + COUNTER_SIZE, center.getLon() + COUNTER_SIZE))[0]);
            int indexHovered = -1;
            for (int i = 0; i < stacks.size(); i++) {
                // The stack being dragged or hovered is drawn by the MarkerManager.
                if (stacksToIgnore.contains(stacks.get(i))) {
                    continue;
                }
                for (int j = 0; j < stacks.get(i).getCounters().size(); j++) {
                    CounterMarker counter = stacks.get(i).getCounters().get(j);
                    float x0 = xy[0] - relativeSize * (stacks.size()) / 2;

                    pg.image(counter.getImage(), x0 + relativeSize * j / 10 + relativeSize * i
                            , xy[1] + relativeSize * (j - 5) / 10, relativeSize, relativeSize);
                }
            }
            pg.popStyle();
        }
    }

    /** {@inheritDoc} */
    @Override
    public IMapMarker getParent() {
        return parent;
    }

    /** {@inheritDoc} */
    @Override
    public void setParent(IMapMarker parent) {
        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInside(UnfoldingMap map, float checkX, float checkY) {
        ScreenPosition tl = map.getScreenPosition(topLeft);
        ScreenPosition br = map.getScreenPosition(bottomRight);
        // fast response for false using the smaller rectangle possible to size the shape.
        if (tl.x > checkX || br.x < checkX
                || tl.y > checkY || br.y < checkY) {
            return false;
        }

        List<ScreenPosition> positions = new ArrayList<>();
        for (Location location : locations) {
            ScreenPosition pos = map.getScreenPosition(location);
            positions.add(pos);
        }

        List<List<ScreenPosition>> rings = new ArrayList<>();
        if (interiorRingLocationArray != null) {
            for (List<Location> ring : interiorRingLocationArray) {
                List<ScreenPosition> ringPositions = new ArrayList<>();

                for (Location location : ring) {
                    ScreenPosition pos = map.getScreenPosition(location);
                    ringPositions.add(pos);
                }

                if (!ringPositions.isEmpty()) {
                    rings.add(ringPositions);
                }
            }
        }

        return isInside(checkX, checkY, positions, rings);
    }

    /**
     * Checks whether the position is within the border of the vectors. Uses a polygon containment algorithm.
     * This method is used for both ScreenPosition as well as Location checks.
     *
     * @param checkX        The x position to check if inside.
     * @param checkY        The y position to check if inside.
     * @param vectors       The vectors of the polygon
     * @param interiorRings The vectors of the interior rings polygon
     * @return True if inside, false otherwise.
     */
    protected boolean isInside(float checkX, float checkY, List<? extends PVector> vectors, List<? extends List<? extends PVector>> interiorRings) {
        boolean inside = false;
        for (int i = 0, j = vectors.size() - 1; i < vectors.size(); j = i++) {
            PVector pi = vectors.get(i);
            PVector pj = vectors.get(j);
            boolean first = (((pi.y <= checkY) && (checkY < pj.y)) || ((pj.y <= checkY) && (checkY < pi.y)));
            boolean second = (checkX < (pj.x - pi.x) * (checkY - pi.y) / (pj.y - pi.y) + pi.x);
            if (first && second) {
                inside = !inside;
            }
        }

        if (inside) {
            for (List<? extends PVector> ring : interiorRings) {
                if (isInside(checkX, checkY, ring)) {
                    inside = false;
                    break;
                }
            }
        }

        return inside;
    }

    /** @return the neighbours. */
    public List<BorderMarker> getNeighbours() {
        return neighbours;
    }

    /**
     * Add a neighbour.
     *
     * @param neighbour the neighbour to add.
     */
    public void addNeighbours(BorderMarker neighbour) {
        neighbours.add(neighbour);
    }

    /** @return the stacks. */
    @Override
    public List<StackMarker> getStacks() {
        return stacks;
    }

    /** {@inheritDoc} */
    @Override
    public void setStacks(List<StackMarker> stacks) {
        this.stacks = stacks;
    }

    /** {@inheritDoc} */
    @Override
    public void addStack(StackMarker stack) {
        if (stack.getProvince() != null) {
            stack.getProvince().removeStack(stack);
        }
        stacks.add(stack);

        stack.setProvince(getRealStackOwner());
    }

    /** {@inheritDoc} */
    @Override
    public void removeStack(StackMarker stack) {
        stack.setProvince(null);
        stacks.remove(stack);
    }

    /** {@inheritDoc} */
    @Override
    public StackMarker getStack(UnfoldingMap map, int x, int y) {
        StackMarker stack = null;

        float[] xy = map.mapDisplay.getObjectFromLocation(center);
        float relativeSize = Math.abs(xy[0] - map.mapDisplay.getObjectFromLocation(new Location(center.getLat()
                + COUNTER_SIZE, center.getLon() + COUNTER_SIZE))[0]);
        float x0 = xy[0] - relativeSize * (stacks.size()) / 2;

        if (x >= x0 && x < x0 + stacks.size() * relativeSize && y >= xy[1] - relativeSize / 2 && y < xy[1] + relativeSize / 2) {
            int index = (int) ((x - x0) / relativeSize);
            stack = stacks.get(index);
        }

        return stack;
    }

    /** @return the real stack owner (the MultiProvinceMarker if it is the case). */
    private IMapMarker getRealStackOwner() {
        IMapMarker province = this;
        if (parent != null) {
            province = parent;
        }

        return province;
    }

    /** {@inheritDoc} */
    @Override
    public void setProperties(HashMap<String, Object> props) {
        HashMap<String, Object> properties = null;
        if (props != null) {
            properties = new HashMap<>();
            for (String key : props.keySet()) {
                Object value = props.get(key);

                if (StringUtils.equals(PROP_TERRAIN, key)) {
                    TerrainEnum terrain = null;
                    try {
                        terrain = TerrainEnum.valueOf((String) value);
                    } catch (Exception e) {
                        // null value if not parsable
                    }
                    value = terrain;
                }
                properties.put(key, value);
            }
        }
        super.setProperties(properties);
    }
}
