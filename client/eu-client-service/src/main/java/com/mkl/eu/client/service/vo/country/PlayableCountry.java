package com.mkl.eu.client.service.vo.country;

import com.mkl.eu.client.service.vo.EuObject;
import com.mkl.eu.client.service.vo.eco.AdministrativeAction;
import com.mkl.eu.client.service.vo.eco.EconomicalSheet;
import com.mkl.eu.client.service.vo.event.EconomicalEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Country (major or former major or future major one).
 *
 * @author MKL
 */
public class PlayableCountry extends EuObject {
    /**
     * Name of the major country turkey.
     */
    public final static String TURKEY = "turquie";
    /**
     * Name of the major country spain.
     */
    public final static String SPAIN = "espagne";
    /**
     * Name of the major country russia.
     */
    public final static String RUSSIA = "russie";
    /**
     * Name of the major country poland.
     */
    public final static String POLAND = "pologne";
    /**
     * Name of the major country england.
     */
    public final static String ENGLAND = "angleterre";
    /**
     * Name of the major country holland.
     */
    public final static String HOLLAND = "hollande";
    /**
     * Name of the major country france.
     */
    public final static String FRANCE = "france";
    /**
     * Name of the major country venice.
     */
    public final static String VENICE = "venise";
    /**
     * Name of the major country portugal.
     */
    public final static String PORTUGAL = "portugal";
    /**
     * Name of the major country sweden.
     */
    public final static String SWEDEN = "suede";
    /**
     * Name of the major country prusse.
     */
    public final static String PRUSSE = "prusse";
    /**
     * Name of the major country habsbourg.
     */
    public final static String HABSBOURG = "habsbourg";
    /**
     * Array of major countries for trade fleet sheet.
     */
    public final static String[] TRADE_FLEET_MAJORS = new String[]{TURKEY, SPAIN, RUSSIA, POLAND,
            ENGLAND, HOLLAND, FRANCE, VENICE, PORTUGAL, SWEDEN, PRUSSE, HABSBOURG};

    static {
        /** Sort the array so that we can use Arrays.binarySearch later on. */
        Arrays.sort(TRADE_FLEET_MAJORS);
    }

    /**
     * Name of the country.
     */
    private String name;
    /** Name of the player. External functional id. */
    private String username;
    /** DTI of the country. */
    private int dti;
    /** FTI of the country. */
    private int fti;
    /** FTI of the country in the ROTW. */
    private int ftiRotw;
    /** Penalty for colonisation. */
    private int colonisationPenalty;
    /** Current land technology of the country. */
    private String landTech;
    /** Current naval technology of the country. */
    private String navalTech;
    /** Flag saying that the country is ready for the next phase (currant phase ok). */
    private boolean ready;
    /**
     * Actual Monarch ruling the country.
     */
    private Monarch monarch;
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
    private List<EconomicalSheet> economicalSheets = new ArrayList<>();
    /**
     * Administrative actions by turn of the country.
     */
    private List<AdministrativeAction> administrativeActions = new ArrayList<>();
    /**
     * Economical events by turn of the country.
     */
    private List<EconomicalEvent> economicalEvents = new ArrayList<>();

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

    /** @return the username. */
    public String getUsername() {
        return username;
    }

    /** @param username the username to set. */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @return the dti. */
    public int getDti() {
        return dti;
    }

    /** @param dti the dti to set. */
    public void setDti(int dti) {
        this.dti = dti;
    }

    /** @return the fti. */
    public int getFti() {
        return fti;
    }

    /** @param fti the fti to set. */
    public void setFti(int fti) {
        this.fti = fti;
    }

    /** @return the ftiRotw. */
    public int getFtiRotw() {
        return ftiRotw;
    }

    /** @param ftiRotw the ftiRotw to set. */
    public void setFtiRotw(int ftiRotw) {
        this.ftiRotw = ftiRotw;
    }

    /** @return the colonisationPenalty. */
    public int getColonisationPenalty() {
        return colonisationPenalty;
    }

    /** @param colonisationPenalty the colonisationPenalty to set. */
    public void setColonisationPenalty(int colonisationPenalty) {
        this.colonisationPenalty = colonisationPenalty;
    }

    /** @return the landTech. */
    public String getLandTech() {
        return landTech;
    }

    /** @param landTech the landTech to set. */
    public void setLandTech(String landTech) {
        this.landTech = landTech;
    }

    /** @return the navalTech. */
    public String getNavalTech() {
        return navalTech;
    }

    /** @param navalTech the navalTech to set. */
    public void setNavalTech(String navalTech) {
        this.navalTech = navalTech;
    }

    /** @return the ready. */
    public boolean isReady() {
        return ready;
    }

    /** @param ready the ready to set. */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    /** @return the monarch. */
    public Monarch getMonarch() {
        return monarch;
    }

    /** @param monarch the monarch to set. */
    public void setMonarch(Monarch monarch) {
        this.monarch = monarch;
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
