package com.mkl.eu.client.service.vo.board;

import com.mkl.eu.client.service.vo.Country;
import com.mkl.eu.client.service.vo.EuObject;
import com.mkl.eu.client.service.vo.enumeration.CounterTypeEnum;

/**
 * Counter (A+, MNU, fortress,...).
 *
 * @author MKL
 */
public class Counter extends EuObject {
    /** Owner of the counter. */
    private Country country;
    /** Province where the counter is located (String or Province ?). */
    private String province;
    /** Type of the counter. */
    private CounterTypeEnum type;

    /**
     * Constructor.
     */
    public Counter() {

    }

    /** @return the country. */
    public Country getCountry() {
        return country;
    }

    /** @param country the country to set. */
    public void setCountry(Country country) {
        this.country = country;
    }

    /** @return the province. */
    public String getProvince() {
        return province;
    }

    /** @param province the province to set. */
    public void setProvince(String province) {
        this.province = province;
    }

    /** @return the type. */
    public CounterTypeEnum getType() {
        return type;
    }

    /** @param type the type to set. */
    public void setType(CounterTypeEnum type) {
        this.type = type;
    }
}
