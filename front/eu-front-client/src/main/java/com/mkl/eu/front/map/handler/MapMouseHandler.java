package com.mkl.eu.front.map.handler;

import com.mkl.eu.front.map.marker.IMapMarker;
import com.mkl.eu.front.map.marker.MyMarkerManager;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.events.MapEventBroadcaster;
import de.fhpotsdam.unfolding.events.PanMapEvent;
import de.fhpotsdam.unfolding.events.ZoomMapEvent;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.event.MouseEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Forking of MouseHandler because the coordinates of the mouse are private without accessor.
 * @author MKL
 */
public class MapMouseHandler extends MapEventBroadcaster {

    /**
     * Creates a MouseHandler for the given maps.
     *
     * @param p
     *            The PApplet.
     * @param maps
     *            One or more maps.
     */
    public MapMouseHandler(PApplet p, UnfoldingMap... maps) {
        this(p, Arrays.asList(maps));
    }

    /**
     * Creates a MouseHandler for the given maps.
     *
     * @param p
     *            The PApplet.
     * @param maps
     *            A list of maps.
     */
    public MapMouseHandler(PApplet p, List<UnfoldingMap> maps) {
        super(maps);

        p.registerMethod("mouseEvent", this);
    }

    public void mouseClicked() {
        for (UnfoldingMap map : maps) {
            if (map.isHit(mouseX, mouseY)) {
                Marker marker = map.getDefaultMarkerManager().getFirstHitMarker(mouseX, mouseY);
                if (marker != null && map.getDefaultMarkerManager() instanceof MyMarkerManager) {
                    ((MyMarkerManager)map.getDefaultMarkerManager()).select(marker);
                }
                if (mouseButton == PConstants.LEFT && mouseEvent.getCount() == 2) {

                    // Pan + Zoom (order is important)
                    PanMapEvent panMapEvent = new PanMapEvent(this, map.getId());
                    Location location = map.getLocation(mouseX, mouseY);
                    panMapEvent.setToLocation(location);
                    eventDispatcher.fireMapEvent(panMapEvent);

                    ZoomMapEvent zoomMapEvent = new ZoomMapEvent(this, map.getId(), ZoomMapEvent.ZOOM_BY_LEVEL, 1);
                    zoomMapEvent.setTransformationCenterLocation(location);
                    eventDispatcher.fireMapEvent(zoomMapEvent);
                }
            }
        }
    }

    public void mouseWheel(float delta) {
        for (UnfoldingMap map : maps) {
            if (map.isHit(mouseX, mouseY)) {
                // log.debug("mouse: fire zoomBy for " + map.getId());

                ZoomMapEvent zoomMapEvent = new ZoomMapEvent(this, map.getId(), ZoomMapEvent.ZOOM_BY_LEVEL);

                // Use location as zoom center, so listening maps can zoom correctly
                Location location = map.getLocation(mouseX, mouseY);
                zoomMapEvent.setTransformationCenterLocation(location);

                // Zoom in or out
                if (delta < 0) {
                    zoomMapEvent.setZoomLevelDelta(1);
                } else if (delta > 0) {
                    zoomMapEvent.setZoomLevelDelta(-1);
                }

                eventDispatcher.fireMapEvent(zoomMapEvent);
            }
        }
    }

    public void mouseDragged() {
        for (UnfoldingMap map : maps) {
            if (map.isHit(mouseX, mouseY)) {
                if (mouseButton == PConstants.LEFT) {
                    // log.debug("mouse: fire panTo for " + map.getId());

                    // Pan between two locations, so other listening maps can pan correctly

                    Location oldLocation = map.getLocation(pmouseX, pmouseY);
                    Location newLocation = map.getLocation(mouseX, mouseY);

                    PanMapEvent panMapEvent = new PanMapEvent(this, map.getId(), PanMapEvent.PAN_BY);
                    panMapEvent.setFromLocation(oldLocation);
                    panMapEvent.setToLocation(newLocation);
                    eventDispatcher.fireMapEvent(panMapEvent);
                }
            }
        }
    }

    public void mouseMoved() {
        for (UnfoldingMap map : maps) {
            if (map.isHit(mouseX, mouseY)) {
                Marker marker = map.getDefaultMarkerManager().getFirstHitMarker(mouseX, mouseY);
                if (marker != null && marker instanceof IMapMarker) {
                    ((IMapMarker) marker).hover(map, mouseX, mouseY);
                }
            }
        }
    }

    // --------------------------------------------------------------
    // Shamelessly copied code from Processing PApplet. No other way to hook into
    // register Processing mouse event and still have the same functionality with pmouseX, etc.
    // --------------------------------------------------------------

    private int mouseX;
    private int mouseY;
    private int pmouseX, pmouseY;
    private int emouseX, emouseY;
    private boolean firstMouse;
    private int mouseButton;
    private MouseEvent mouseEvent;

    public void mouseEvent(MouseEvent event) {
        int action = event.getAction();
        mouseEvent = event;

        if ((action == MouseEvent.DRAG) || (action == MouseEvent.MOVE)) {
            pmouseX = emouseX;
            pmouseY = emouseY;
            mouseX = event.getX();
            mouseY = event.getY();
        }

        mouseButton = event.getButton();

        if (firstMouse) {
            pmouseX = mouseX;
            pmouseY = mouseY;
            firstMouse = false;
        }

        switch (action) {
            case MouseEvent.CLICK:
                mouseClicked();
                break;
            case MouseEvent.DRAG:
                mouseDragged();
                break;
            case MouseEvent.MOVE:
                mouseMoved();
                break;
            case MouseEvent.WHEEL:
                mouseWheel(event.getCount());
                break;
        }

        if ((action == MouseEvent.DRAG) || (action == MouseEvent.MOVE)) {
            emouseX = mouseX;
            emouseY = mouseY;
        }
    }
}
