package com.mkl.eu.client.service.vo.diplo;

import com.mkl.eu.client.service.vo.country.PlayableCountry;

/**
 * Turn order of countries.
 *
 * @author MKL.
 */
public class CountryOrder {
    /** Country concerned by the turn order. */
    // TODO TG-18 add a 'fake' playable country in case of minor alone in war
    private PlayableCountry country;
    /** Position of the country in the turn order. */
    private int position;
    /** Activity of this order segment (ie the one whose it is the turn). */
    private boolean active;
    /** Flag saying that the country is ready for the next phase (currant phase ok). */
    private boolean ready;

    /** @return the country. */
    public PlayableCountry getCountry() {
        return country;
    }

    /** @param country the country to set. */
    public void setCountry(PlayableCountry country) {
        this.country = country;
    }

    /** @return the position. */
    public int getPosition() {
        return position;
    }

    /** @param position the position to set. */
    public void setPosition(int position) {
        this.position = position;
    }

    /** @return the active. */
    public boolean isActive() {
        return active;
    }

    /** @param active the active to set. */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** @return the ready. */
    public boolean isReady() {
        return ready;
    }

    /** @param ready the ready to set. */
    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
