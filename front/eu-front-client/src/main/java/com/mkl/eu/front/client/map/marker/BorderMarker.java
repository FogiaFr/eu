package com.mkl.eu.front.client.map.marker;

import com.mkl.eu.client.service.vo.enumeration.BorderEnum;

/**
 * Not a marker but Border was already used.
 *
 * @author MKL
 */
public class BorderMarker {
    /** Province bound to. */
    private IMapMarker province;
    /** Type of border. */
    private BorderEnum type;

    /**
     * Constructor.
     *
     * @param province   the province.
     * @param typeBorder the type of border.
     */
    public BorderMarker(IMapMarker province, String typeBorder) {
        this.province = province;
        if (typeBorder != null) {
            this.type = BorderEnum.valueOf(typeBorder);
        }
    }

    /** @return the province. */
    public IMapMarker getProvince() {
        return province;
    }

    /** @return the type. */
    public BorderEnum getType() {
        return type;
    }
}
