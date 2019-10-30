package com.mkl.eu.front.client.common;

import com.mkl.eu.client.service.vo.board.Counter;
import com.mkl.eu.front.client.main.GlobalConfiguration;
import javafx.util.StringConverter;

/**
 * Counter converter with province display.
 *
 * @author MKL.
 */
public class CounterInProvinceConverter extends StringConverter<Counter> {
    /** The global configuration for internationalisation. */
    private GlobalConfiguration globalConfiguration;

    /**
     * Constructor.
     *
     * @param globalConfiguration the global configuration.
     */
    public CounterInProvinceConverter(GlobalConfiguration globalConfiguration) {
        this.globalConfiguration = globalConfiguration;
    }

    /** {@inheritDoc} */
    @Override
    public String toString(Counter object) {
        return globalConfiguration.getMessage(object.getType()) + " - " + globalConfiguration.getMessage(object.getCountry()) + " - " + globalConfiguration.getMessage(object.getOwner().getProvince());
    }

    /** {@inheritDoc} */
    @Override
    public Counter fromString(String string) {
        return null;
    }
}