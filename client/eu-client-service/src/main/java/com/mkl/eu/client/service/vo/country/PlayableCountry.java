package com.mkl.eu.client.service.vo.country;

import com.mkl.eu.client.service.vo.EuObject;
import com.mkl.eu.client.service.vo.eco.AdministrativeAction;
import com.mkl.eu.client.service.vo.eco.EconomicalSheet;
import com.mkl.eu.client.service.vo.event.EconomicalEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Country (major or former major or future major one).
 *
 * @author MKL
 */
public class PlayableCountry extends EuObject {
    /**
     * Name of the country.
     */
    private String name;
    /**
     * Monarchs (past and present) of the country.
     */
    private List<Monarch> monarchs = new ArrayList<>();
    /**
     * Discoveries of the country.
     */
    private List<Discovery> discoveries = new ArrayList<>();
    /**
     * Economical sheet by turn of the country.
     */
    private List<EconomicalSheet> economicalSheets;
    /**
     * Administrative actions by turn of the country.
     */
    private List<AdministrativeAction> administrativeActions;
    /**
     * Economical events by turn of the country.
     */
    private List<EconomicalEvent> economicalEvents;

    /**
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the monarchs.
     */
    public List<Monarch> getMonarchs() {
        return monarchs;
    }

    /**
     * @param monarchs the monarchs to set.
     */
    public void setMonarchs(List<Monarch> monarchs) {
        this.monarchs = monarchs;
    }

    /**
     * @return the discoveries.
     */
    public List<Discovery> getDiscoveries() {
        return discoveries;
    }

    /**
     * @param discoveries the discoveries to set.
     */
    public void setDiscoveries(List<Discovery> discoveries) {
        this.discoveries = discoveries;
    }

    /**
     * @return the economicalSheets.
     */
    public List<EconomicalSheet> getEconomicalSheets() {
        return economicalSheets;
    }

    /**
     * @param economicalSheets the economicalSheets to set.
     */
    public void setEconomicalSheets(List<EconomicalSheet> economicalSheets) {
        this.economicalSheets = economicalSheets;
    }

    /**
     * @return the administrativeActions.
     */
    public List<AdministrativeAction> getAdministrativeActions() {
        return administrativeActions;
    }

    /**
     * @param administrativeActions the administrativeActions to set.
     */
    public void setAdministrativeActions(List<AdministrativeAction> administrativeActions) {
        this.administrativeActions = administrativeActions;
    }

    /**
     * @return the economicalEvents.
     */
    public List<EconomicalEvent> getEconomicalEvents() {
        return economicalEvents;
    }

    /**
     * @param economicalEvents the economicalEvents to set.
     */
    public void setEconomicalEvents(List<EconomicalEvent> economicalEvents) {
        this.economicalEvents = economicalEvents;
    }
}