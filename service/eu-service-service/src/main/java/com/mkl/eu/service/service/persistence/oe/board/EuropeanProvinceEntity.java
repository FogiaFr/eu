package com.mkl.eu.service.service.persistence.oe.board;

import com.mkl.eu.service.service.persistence.oe.country.CountryEntity;

import javax.persistence.*;

/**
 * European province. A tile on the european board that can contains counters.
 *
 * @author MKL.
 */
@Entity
@Table(name = "PROVINCE_EU")
@PrimaryKeyJoinColumn(name = "ID")
public class EuropeanProvinceEntity extends AbstractProvinceEntity {
    /** Base income of the province. */
    private Integer income;
    /** Owner of the province if no owner counter is present. */
    private CountryEntity defaultOwner;
    /** Flag saying that the province contains a port. */
    private Boolean port;
    /** Flag saying that the province port can be praesidiable. */
    private Boolean praesidiable;

    /** @return the income. */
    @Column(name = "INCOME")
    public Integer getIncome() {
        return income;
    }

    /** @param income the income to set. */
    public void setIncome(Integer income) {
        this.income = income;
    }

    /** @return the defaultOwner. */
    @ManyToOne
    @JoinColumn(name = "ID_COUNTRY")
    public CountryEntity getDefaultOwner() {
        return defaultOwner;
    }

    /** @param defaultOwner the defaultOwner to set. */
    public void setDefaultOwner(CountryEntity defaultOwner) {
        this.defaultOwner = defaultOwner;
    }

    /** @return the port. */
    @Column(name = "PORT", columnDefinition = "BIT")
    public Boolean isPort() {
        return port;
    }

    /** @param port the port to set. */
    public void setPort(Boolean port) {
        this.port = port;
    }

    /** @return the praesidiable. */
    @Column(name = "PRAESIDIABLE", columnDefinition = "BIT")
    public Boolean isPraesidiable() {
        return praesidiable;
    }

    /** @param praesidiable the praesidiable to set. */
    public void setPraesidiable(Boolean praesidiable) {
        this.praesidiable = praesidiable;
    }
}
