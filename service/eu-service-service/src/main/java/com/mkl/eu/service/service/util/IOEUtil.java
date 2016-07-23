package com.mkl.eu.service.service.util;

import com.mkl.eu.service.service.persistence.oe.GameEntity;
import com.mkl.eu.service.service.persistence.oe.country.PlayableCountryEntity;
import com.mkl.eu.service.service.persistence.oe.ref.province.AbstractProvinceEntity;

import java.util.List;

/**
 * Utility for OE class.
 *
 * @author MKL.
 */
public interface IOEUtil {
    /**
     * @param country whom we want the administrative value.
     * @return the administrative value of a country.
     */
    int getAdministrativeValue(PlayableCountryEntity country);

    /**
     * @param country whom we want the military value.
     * @return the military value of a country.
     */
    int getMilitaryValue(PlayableCountryEntity country);

    /**
     * @param game    game containing all the counters.
     * @param country whom we want the stability.
     * @return the stability of a country.
     */
    int getStability(GameEntity game, String country);

    /**
     * @param game    game containing all the counters.
     * @param country whom we want the technology box.
     * @param land    to wether we want the land or the naval technology advance.
     * @return the number of the box where the appropriate technology counter of the country is.
     */
    int getTechnologyAdvance(GameEntity game, String country, boolean land);

    /**
     * @param province    the province to settle.
     * @param discoveries the provinces that have been discovered.
     * @param sources     the sources (COL/TP/Owned european province) of supply of the settlement.
     * @param friendlies  the friendly terrains.
     * @return <code>true</code> if the province can be settled, <code>false</code> otherwise.
     */
    boolean canSettle(AbstractProvinceEntity province, List<String> discoveries, List<String> sources, List<String> friendlies);

    /**
     * Rolls a die for a country in the given game. The country can ben <code>null</code> for general die roll.
     *
     * @param game    the game.
     * @param country the country rolling the die. Can be <code>null</code>.
     * @return the result of a die 10.
     */
    int rollDie(GameEntity game, String country);

    /**
     * Rolls a die for a country in the given game. The country can ben <code>null</code> for general die roll.
     *
     * @param game    the game.
     * @param country the country rolling the die. Can be <code>null</code>.
     * @return the result of a die 10.
     */
    int rollDie(GameEntity game, PlayableCountryEntity country);
}
