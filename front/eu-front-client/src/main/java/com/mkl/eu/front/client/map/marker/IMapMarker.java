package com.mkl.eu.front.client.map.marker;

import de.fhpotsdam.unfolding.UnfoldingMap;

import java.util.List;

/**
 * Interface of all the map markers.
 *
 * @author MKL
 */
public interface IMapMarker {
    /** Name of the marker property for the terrain. */
    String PROP_TERRAIN = "terrain";

    /**
     * Sets the highlightColor.
     *
     * @param highlightColor the highlightColor to set.
     */
    void setHighlightColor(int highlightColor);

    /** @return the neighbours. */
    List<BorderMarker> getNeighbours();

    /**
     * Add a neighbour.
     *
     * @param neighbour the neighbour to add.
     */
    void addNeighbours(BorderMarker neighbour);

    /** @return the id; */
    String getId();

    /** @return the stacks. */
    List<StackMarker> getStacks();

    /**
     * Sets the stacks to the marker.
     *
     * @param stacks the stacks to set.
     */
    void setStacks(List<StackMarker> stacks);

    /**
     * Add a stack to the marker.
     *
     * @param stack the stack to add.
     */
    void addStack(StackMarker stack);

    /**
     * Remove a stack to the marker.
     *
     * @param stack the stack to remove.
     */
    void removeStack(StackMarker stack);

    /**
     * Returns the stack at the x/y coordinates, <code>null</code> if none.
     *
     * @param map the map.
     * @param x   X coordinate.
     * @param y   Y coordinate.
     * @return the stack at the x/y coordinates, <code>null</code> if none.
     */
    StackMarker getStack(UnfoldingMap map, int x, int y);

    /**
     * Method called when the marker is hovered.
     *
     * @param map the map.
     * @param x   X coordinate.
     * @param y   Y coordinate.
     */
    void hover(UnfoldingMap map, int x, int y);

    /**
     * Draws this marker.
     *
     * @param map           The map to draw on.
     * @param stackToIgnore stack not to draw.
     */
    void draw(UnfoldingMap map, StackMarker stackToIgnore);

    /** @return In case of Provinces in various pieces, returns the MultiProvinceMarker parent of all. */
    IMapMarker getParent();

    /** @param parent the parent to set.. */
    void setParent(IMapMarker parent);
}