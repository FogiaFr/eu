package com.mkl.eu.front.map.marker;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.marker.AbstractMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;

import java.util.ArrayList;
import java.util.List;

/**
 * Province marker that consists of various pieces.
 *
 * @author MKL
 */
public class MultiProvinceMarker extends MultiMarker implements IMapMarker {
    /** Neighbours of the province. */
    private List<BorderMarker> neighbours = new ArrayList<>();
    /** Counters of the province. */
    private List<CounterMarker> counters = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param markers sons of the multi provinc emarker.
     */
    public MultiProvinceMarker(List<Marker> markers) {
        this.markers = markers;
    }

    /** {@inheritDoc} */
    @Override
    public void draw(UnfoldingMap map, StackMarker stackToIgnore) {
        for (Marker marker : markers) {
            if (marker instanceof IMapMarker) {
                ((IMapMarker) marker).draw(map, stackToIgnore);
            } else {
                marker.draw(map);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setHighlightColor(int highlightColor) {
        for (Marker subMarker : getMarkers()) {
            if (subMarker instanceof AbstractMarker) {
                ((AbstractMarker) subMarker).setHighlightColor(highlightColor);
            }
        }
    }

    /** @return the first IMapMarker of the markers. */
    private IMapMarker getFirstMapMarker() {
        IMapMarker mapMarker = null;

        if (markers != null) {
            for (Marker marker : markers) {
                if (marker instanceof IMapMarker) {
                    mapMarker = (IMapMarker) marker;
                    break;
                }
            }
        }

        return mapMarker;
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

    /** {@inheritDoc} */
    @Override
    public List<StackMarker> getStacks() {
        return getFirstMapMarker().getStacks();
    }

    /** {@inheritDoc} */
    @Override
    public void setStacks(List<StackMarker> stacks) {
        getFirstMapMarker().setStacks(stacks);
    }

    /** {@inheritDoc} */
    @Override
    public void addStack(StackMarker stack) {
        getFirstMapMarker().addStack(stack);
    }

    /** {@inheritDoc} */
    @Override
    public StackMarker getStack(UnfoldingMap map, int x, int y) {
        return getFirstMapMarker().getStack(map, x, y);
    }

    /** {@inheritDoc} */
    @Override
    public void removeStack(StackMarker stack) {
        getFirstMapMarker().removeStack(stack);
    }

    /** {@inheritDoc} */
    @Override
    public void hover(UnfoldingMap map, int x, int y) {
        getFirstMapMarker().hover(map, x, y);
    }
}