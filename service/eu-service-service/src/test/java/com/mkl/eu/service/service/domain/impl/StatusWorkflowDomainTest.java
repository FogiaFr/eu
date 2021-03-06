package com.mkl.eu.service.service.domain.impl;

import com.mkl.eu.client.service.util.GameUtil;
import com.mkl.eu.client.service.vo.country.PlayableCountry;
import com.mkl.eu.client.service.vo.enumeration.*;
import com.mkl.eu.client.service.vo.tables.*;
import com.mkl.eu.service.service.domain.ICounterDomain;
import com.mkl.eu.service.service.domain.IStatusWorkflowDomain;
import com.mkl.eu.service.service.persistence.IGameDao;
import com.mkl.eu.service.service.persistence.board.ICounterDao;
import com.mkl.eu.service.service.persistence.eco.IEconomicalSheetDao;
import com.mkl.eu.service.service.persistence.oe.GameEntity;
import com.mkl.eu.service.service.persistence.oe.board.CounterEntity;
import com.mkl.eu.service.service.persistence.oe.board.StackEntity;
import com.mkl.eu.service.service.persistence.oe.country.PlayableCountryEntity;
import com.mkl.eu.service.service.persistence.oe.diff.DiffAttributesEntity;
import com.mkl.eu.service.service.persistence.oe.diff.DiffEntity;
import com.mkl.eu.service.service.persistence.oe.diplo.CountryInWarEntity;
import com.mkl.eu.service.service.persistence.oe.diplo.CountryOrderEntity;
import com.mkl.eu.service.service.persistence.oe.diplo.WarEntity;
import com.mkl.eu.service.service.persistence.oe.eco.EconomicalSheetEntity;
import com.mkl.eu.service.service.persistence.oe.military.BattleEntity;
import com.mkl.eu.service.service.persistence.oe.military.SiegeEntity;
import com.mkl.eu.service.service.persistence.oe.ref.country.CountryEntity;
import com.mkl.eu.service.service.persistence.oe.ref.province.AbstractProvinceEntity;
import com.mkl.eu.service.service.persistence.oe.ref.province.EuropeanProvinceEntity;
import com.mkl.eu.service.service.persistence.ref.IProvinceDao;
import com.mkl.eu.service.service.service.ListEquals;
import com.mkl.eu.service.service.service.impl.AbstractBack;
import com.mkl.eu.service.service.service.impl.EconomicServiceImpl;
import com.mkl.eu.service.service.util.DiffUtil;
import com.mkl.eu.service.service.util.IOEUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.stream.Collectors;

import static com.mkl.eu.service.service.service.AbstractGameServiceTest.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Test of StatusWorkflowDomainImpl.
 *
 * @author MKL.
 */
@RunWith(MockitoJUnitRunner.class)
public class StatusWorkflowDomainTest {
    @InjectMocks
    private StatusWorkflowDomainImpl statusWorkflowDomain;

    @Mock
    private ICounterDomain counterDomain;

    @Mock
    private IOEUtil oeUtil;

    @Mock
    private IGameDao gameDao;

    @Mock
    private IProvinceDao provinceDao;

    @Mock
    private ICounterDao counterDao;

    @Mock
    private IEconomicalSheetDao economicalSheetDao;

    /** Variable used to store something coming from a mock. */
    private EconomicalSheetEntity sheetEntity;

    @Before
    public void init() {
        AbstractBack.TABLES = new Tables();
    }

    @Test
    public void testComputeEndMinorLogisticsNoCountries() throws Exception {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setVersion(5L);
        game.setTurn(5);
        game.setStatus(GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE);
        game.getOrders().add(new CountryOrderEntity());
        game.getOrders().add(new CountryOrderEntity());
        game.getOrders().add(new CountryOrderEntity());

        checkDiffsForMilitary(game);

        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE, game.getStatus());
        Assert.assertEquals(0, game.getOrders().size());
    }

    private void checkDiffsForMilitary(GameEntity game) {
        List<DiffEntity> diffs = statusWorkflowDomain.computeEndMinorLogistics(game);

        DiffEntity diff = diffs.stream()
                .filter(d -> d != null && d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(game.getId(), diff.getIdGame());
        Assert.assertEquals(game.getVersion(), diff.getVersionGame().longValue());
        Assert.assertEquals(DiffTypeEnum.MODIFY, diff.getType());
        Assert.assertEquals(DiffTypeObjectEnum.GAME, diff.getTypeObject());
        Assert.assertEquals(null, diff.getIdObject());
        Assert.assertEquals(1, diff.getAttributes().size());
        Assert.assertEquals(DiffAttributeTypeEnum.STATUS, diff.getAttributes().get(0).getType());
        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE.name(), diff.getAttributes().get(0).getValue());
    }

    /**
     * No Wars, 4 countries played, one minor. Order expected:
     * <ul>
     * <li>france (init 21)</li>
     * <li>russie (init 15)</li>
     * <li>espagne (init 12)</li>
     * <li>turquie (init 9)</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testComputeEndMinorLogisticsNoWars() throws Exception {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setVersion(5L);
        game.setTurn(5);
        game.setStatus(GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        france.setUsername("france");
        game.getCountries().add(france);
        PlayableCountryEntity espagne = new PlayableCountryEntity();
        espagne.setName("espagne");
        espagne.setUsername("espagne");
        game.getCountries().add(espagne);
        PlayableCountryEntity turquie = new PlayableCountryEntity();
        turquie.setName("turquie");
        turquie.setUsername("turquie");
        game.getCountries().add(turquie);
        PlayableCountryEntity russie = new PlayableCountryEntity();
        russie.setName("russie");
        russie.setUsername("russie");
        game.getCountries().add(russie);
        PlayableCountryEntity pologne = new PlayableCountryEntity();
        pologne.setName("pologne");
        pologne.setUsername(null);
        game.getCountries().add(pologne);

        when(oeUtil.getInitiative(france)).thenReturn(21);
        when(oeUtil.getInitiative(espagne)).thenReturn(12);
        when(oeUtil.getInitiative(turquie)).thenReturn(9);
        when(oeUtil.getInitiative(russie)).thenReturn(15);
        when(oeUtil.getInitiative(pologne)).thenReturn(14);

        checkDiffsForMilitary(game);

        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE, game.getStatus());
        Assert.assertEquals(4, game.getOrders().size());
        Assert.assertEquals(game, game.getOrders().get(0).getGame());
        Assert.assertEquals(france.getName(), game.getOrders().get(0).getCountry().getName());
        Assert.assertEquals(0, game.getOrders().get(0).getPosition());
        Assert.assertEquals(game, game.getOrders().get(1).getGame());
        Assert.assertEquals(russie.getName(), game.getOrders().get(1).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(1).getPosition());
        Assert.assertEquals(game, game.getOrders().get(2).getGame());
        Assert.assertEquals(espagne.getName(), game.getOrders().get(2).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(2).getPosition());
        Assert.assertEquals(game, game.getOrders().get(3).getGame());
        Assert.assertEquals(turquie.getName(), game.getOrders().get(3).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(3).getPosition());
    }

    /**
     * 5 countries played:
     * <ul>
     * <li>france (init 21)</li>
     * <li>russie (init 15)</li>
     * <li>pologne (init 14)</li>
     * <li>espagne (init 12)</li>
     * <li>turquie (init 9)</li>
     * </ul>
     * <p>
     * One war. france and turquie against russie, espagne and pologne (but pologne in LIMITED).
     * Expected order:
     * <ul>
     * <li>pologne (init 14)</li>
     * <li>espagne and russie (init 12)</li>
     * <li>france and turquie (init 9)</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testComputeEndMinorLogisticsSimpleWar() throws Exception {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setVersion(5L);
        game.setTurn(5);
        game.setStatus(GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        france.setUsername("france");
        game.getCountries().add(france);
        PlayableCountryEntity espagne = new PlayableCountryEntity();
        espagne.setName("espagne");
        espagne.setUsername("espagne");
        game.getCountries().add(espagne);
        PlayableCountryEntity turquie = new PlayableCountryEntity();
        turquie.setName("turquie");
        turquie.setUsername("turquie");
        game.getCountries().add(turquie);
        PlayableCountryEntity russie = new PlayableCountryEntity();
        russie.setName("russie");
        russie.setUsername("russie");
        game.getCountries().add(russie);
        PlayableCountryEntity pologne = new PlayableCountryEntity();
        pologne.setName("pologne");
        pologne.setUsername("pologne");
        game.getCountries().add(pologne);
        game.getWars().add(new WarEntity());
        game.getWars().get(0).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(0).setOffensive(true);
        game.getWars().get(0).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(1).setOffensive(true);
        game.getWars().get(0).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(1).getCountry().setName(turquie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(2).setOffensive(false);
        game.getWars().get(0).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(2).getCountry().setName(espagne.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(3).setOffensive(false);
        game.getWars().get(0).getCountries().get(3).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(3).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(3).getCountry().setName(russie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(4).setOffensive(false);
        game.getWars().get(0).getCountries().get(4).setImplication(WarImplicationEnum.LIMITED);
        game.getWars().get(0).getCountries().get(4).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(4).getCountry().setName(pologne.getName());

        when(oeUtil.getInitiative(france)).thenReturn(21);
        when(oeUtil.getInitiative(espagne)).thenReturn(12);
        when(oeUtil.getInitiative(turquie)).thenReturn(9);
        when(oeUtil.getInitiative(russie)).thenReturn(15);
        when(oeUtil.getInitiative(pologne)).thenReturn(14);

        checkDiffsForMilitary(game);

        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE, game.getStatus());
        Assert.assertEquals(5, game.getOrders().size());
        Assert.assertEquals(game, game.getOrders().get(0).getGame());
        Assert.assertEquals(pologne.getName(), game.getOrders().get(0).getCountry().getName());
        Assert.assertEquals(0, game.getOrders().get(0).getPosition());
        Assert.assertEquals(game, game.getOrders().get(1).getGame());
        Assert.assertEquals(espagne.getName(), game.getOrders().get(1).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(1).getPosition());
        Assert.assertEquals(game, game.getOrders().get(2).getGame());
        Assert.assertEquals(russie.getName(), game.getOrders().get(2).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(2).getPosition());
        Assert.assertEquals(game, game.getOrders().get(3).getGame());
        Assert.assertEquals(france.getName(), game.getOrders().get(3).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(3).getPosition());
        Assert.assertEquals(game, game.getOrders().get(4).getGame());
        Assert.assertEquals(turquie.getName(), game.getOrders().get(4).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(4).getPosition());
    }

    /**
     * 5 countries played:
     * <ul>
     * <li>france (init 21)</li>
     * <li>russie (init 15)</li>
     * <li>pologne (init 14)</li>
     * <li>espagne (init 12)</li>
     * <li>turquie (init 9)</li>
     * <li>venise (init 10)</li>
     * </ul>
     * <p>
     * Two wars.
     * First one: france and turquie against russie, espagne and pologne (but pologne in LIMITED).
     * Second one: pologne and venise against hongrie (minor).
     * <p>
     * Expected order:
     * <ul>
     * <li>espagne and russie (init 12)</li>
     * <li>pologne and venise (init 10)</li>
     * <li>france and turquie (init 9)</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testComputeEndMinorLogisticsSimpleWars() throws Exception {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setVersion(5L);
        game.setTurn(5);
        game.setStatus(GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        france.setUsername("france");
        game.getCountries().add(france);
        PlayableCountryEntity espagne = new PlayableCountryEntity();
        espagne.setName("espagne");
        espagne.setUsername("espagne");
        game.getCountries().add(espagne);
        PlayableCountryEntity turquie = new PlayableCountryEntity();
        turquie.setName("turquie");
        turquie.setUsername("turquie");
        game.getCountries().add(turquie);
        PlayableCountryEntity russie = new PlayableCountryEntity();
        russie.setName("russie");
        russie.setUsername("russie");
        game.getCountries().add(russie);
        PlayableCountryEntity pologne = new PlayableCountryEntity();
        pologne.setName("pologne");
        pologne.setUsername("pologne");
        game.getCountries().add(pologne);
        PlayableCountryEntity venise = new PlayableCountryEntity();
        venise.setName("venise");
        venise.setUsername("venise");
        game.getCountries().add(venise);
        game.getWars().add(new WarEntity());
        game.getWars().get(0).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(0).setOffensive(true);
        game.getWars().get(0).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(1).setOffensive(true);
        game.getWars().get(0).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(1).getCountry().setName(turquie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(2).setOffensive(false);
        game.getWars().get(0).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(2).getCountry().setName(espagne.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(3).setOffensive(false);
        game.getWars().get(0).getCountries().get(3).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(3).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(3).getCountry().setName(russie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(4).setOffensive(false);
        game.getWars().get(0).getCountries().get(4).setImplication(WarImplicationEnum.LIMITED);
        game.getWars().get(0).getCountries().get(4).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(4).getCountry().setName(pologne.getName());
        game.getWars().add(new WarEntity());
        game.getWars().get(1).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(0).setOffensive(true);
        game.getWars().get(1).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(0).getCountry().setName(pologne.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(1).setOffensive(true);
        game.getWars().get(1).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(1).getCountry().setName(venise.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(2).setOffensive(false);
        game.getWars().get(1).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(2).getCountry().setName("hungaria");

        when(oeUtil.getInitiative(france)).thenReturn(21);
        when(oeUtil.getInitiative(espagne)).thenReturn(12);
        when(oeUtil.getInitiative(turquie)).thenReturn(9);
        when(oeUtil.getInitiative(russie)).thenReturn(15);
        when(oeUtil.getInitiative(pologne)).thenReturn(14);
        when(oeUtil.getInitiative(venise)).thenReturn(10);

        checkDiffsForMilitary(game);

        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE, game.getStatus());
        Assert.assertEquals(6, game.getOrders().size());
        Assert.assertEquals(game, game.getOrders().get(0).getGame());
        Assert.assertEquals(espagne.getName(), game.getOrders().get(0).getCountry().getName());
        Assert.assertEquals(0, game.getOrders().get(0).getPosition());
        Assert.assertEquals(game, game.getOrders().get(1).getGame());
        Assert.assertEquals(russie.getName(), game.getOrders().get(1).getCountry().getName());
        Assert.assertEquals(0, game.getOrders().get(1).getPosition());
        Assert.assertEquals(game, game.getOrders().get(2).getGame());
        Assert.assertEquals(pologne.getName(), game.getOrders().get(2).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(2).getPosition());
        Assert.assertEquals(game, game.getOrders().get(3).getGame());
        Assert.assertEquals(venise.getName(), game.getOrders().get(3).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(3).getPosition());
        Assert.assertEquals(game, game.getOrders().get(4).getGame());
        Assert.assertEquals(france.getName(), game.getOrders().get(4).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(4).getPosition());
        Assert.assertEquals(game, game.getOrders().get(5).getGame());
        Assert.assertEquals(turquie.getName(), game.getOrders().get(5).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(5).getPosition());
    }

    /**
     * 5 countries played:
     * <ul>
     * <li>france (init 21)</li>
     * <li>russie (init 15)</li>
     * <li>pologne (init 14)</li>
     * <li>espagne (init 12)</li>
     * <li>turquie (init 9)</li>
     * <li>venise (init 10)</li>
     * </ul>
     * <p>
     * Two wars.
     * First one: france and turquie against russie, espagne and pologne (but pologne in LIMITED).
     * Second one: france against venise.
     * <p>
     * Expected order:
     * <ul>
     * <li>pologne (init 14)</li>
     * <li>espagne and russie (init 12)</li>
     * <li>venise (init 10)</li>
     * <li>france and turquie (init 9)</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testComputeEndMinorLogisticsMediumWars() throws Exception {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setVersion(5L);
        game.setTurn(5);
        game.setStatus(GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        france.setUsername("france");
        game.getCountries().add(france);
        PlayableCountryEntity espagne = new PlayableCountryEntity();
        espagne.setName("espagne");
        espagne.setUsername("espagne");
        game.getCountries().add(espagne);
        PlayableCountryEntity turquie = new PlayableCountryEntity();
        turquie.setName("turquie");
        turquie.setUsername("turquie");
        game.getCountries().add(turquie);
        PlayableCountryEntity russie = new PlayableCountryEntity();
        russie.setName("russie");
        russie.setUsername("russie");
        game.getCountries().add(russie);
        PlayableCountryEntity pologne = new PlayableCountryEntity();
        pologne.setName("pologne");
        pologne.setUsername("pologne");
        game.getCountries().add(pologne);
        PlayableCountryEntity venise = new PlayableCountryEntity();
        venise.setName("venise");
        venise.setUsername("venise");
        game.getCountries().add(venise);
        game.getWars().add(new WarEntity());
        game.getWars().get(0).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(0).setOffensive(true);
        game.getWars().get(0).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(1).setOffensive(true);
        game.getWars().get(0).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(1).getCountry().setName(turquie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(2).setOffensive(false);
        game.getWars().get(0).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(2).getCountry().setName(espagne.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(3).setOffensive(false);
        game.getWars().get(0).getCountries().get(3).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(3).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(3).getCountry().setName(russie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(4).setOffensive(false);
        game.getWars().get(0).getCountries().get(4).setImplication(WarImplicationEnum.LIMITED);
        game.getWars().get(0).getCountries().get(4).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(4).getCountry().setName(pologne.getName());
        game.getWars().add(new WarEntity());
        game.getWars().get(1).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(0).setOffensive(true);
        game.getWars().get(1).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(1).setOffensive(false);
        game.getWars().get(1).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(1).getCountry().setName(venise.getName());


        when(oeUtil.getInitiative(france)).thenReturn(21);
        when(oeUtil.getInitiative(espagne)).thenReturn(12);
        when(oeUtil.getInitiative(turquie)).thenReturn(9);
        when(oeUtil.getInitiative(russie)).thenReturn(15);
        when(oeUtil.getInitiative(pologne)).thenReturn(14);
        when(oeUtil.getInitiative(venise)).thenReturn(10);

        checkDiffsForMilitary(game);

        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE, game.getStatus());
        Assert.assertEquals(6, game.getOrders().size());
        Assert.assertEquals(game, game.getOrders().get(0).getGame());
        Assert.assertEquals(pologne.getName(), game.getOrders().get(0).getCountry().getName());
        Assert.assertEquals(0, game.getOrders().get(0).getPosition());
        Assert.assertEquals(game, game.getOrders().get(1).getGame());
        Assert.assertEquals(espagne.getName(), game.getOrders().get(1).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(1).getPosition());
        Assert.assertEquals(game, game.getOrders().get(2).getGame());
        Assert.assertEquals(russie.getName(), game.getOrders().get(2).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(2).getPosition());
        Assert.assertEquals(game, game.getOrders().get(3).getGame());
        Assert.assertEquals(venise.getName(), game.getOrders().get(3).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(3).getPosition());
        Assert.assertEquals(game, game.getOrders().get(4).getGame());
        Assert.assertEquals(france.getName(), game.getOrders().get(4).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(4).getPosition());
        Assert.assertEquals(game, game.getOrders().get(5).getGame());
        Assert.assertEquals(turquie.getName(), game.getOrders().get(5).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(5).getPosition());
    }

    /**
     * 5 countries played:
     * <ul>
     * <li>france (init 21)</li>
     * <li>russie (init 15)</li>
     * <li>pologne (init 14)</li>
     * <li>espagne (init 12)</li>
     * <li>turquie (init 9)</li>
     * <li>venise (init 10)</li>
     * <li>hollande (init 25)</li>
     * </ul>
     * <p>
     * Two wars.
     * First one: france and turquie against russie, espagne and pologne (but pologne in LIMITED).
     * Second one: france and hollande against venise.
     * <p>
     * Expected order:
     * <ul>
     * <li>pologne (init 14)</li>
     * <li>espagne and russie (init 12)</li>
     * <li>venise (init 10)</li>
     * <li>france, hollande and turquie (init 9)</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testComputeEndMinorLogisticsMediumWars2() throws Exception {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setVersion(5L);
        game.setTurn(5);
        game.setStatus(GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        france.setUsername("france");
        game.getCountries().add(france);
        PlayableCountryEntity espagne = new PlayableCountryEntity();
        espagne.setName("espagne");
        espagne.setUsername("espagne");
        game.getCountries().add(espagne);
        PlayableCountryEntity turquie = new PlayableCountryEntity();
        turquie.setName("turquie");
        turquie.setUsername("turquie");
        game.getCountries().add(turquie);
        PlayableCountryEntity russie = new PlayableCountryEntity();
        russie.setName("russie");
        russie.setUsername("russie");
        game.getCountries().add(russie);
        PlayableCountryEntity pologne = new PlayableCountryEntity();
        pologne.setName("pologne");
        pologne.setUsername("pologne");
        game.getCountries().add(pologne);
        PlayableCountryEntity venise = new PlayableCountryEntity();
        venise.setName("venise");
        venise.setUsername("venise");
        game.getCountries().add(venise);
        PlayableCountryEntity hollande = new PlayableCountryEntity();
        hollande.setName("hollande");
        hollande.setUsername("hollande");
        game.getCountries().add(hollande);
        game.getWars().add(new WarEntity());
        game.getWars().get(0).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(0).setOffensive(true);
        game.getWars().get(0).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(1).setOffensive(true);
        game.getWars().get(0).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(1).getCountry().setName(turquie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(2).setOffensive(false);
        game.getWars().get(0).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(2).getCountry().setName(espagne.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(3).setOffensive(false);
        game.getWars().get(0).getCountries().get(3).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(3).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(3).getCountry().setName(russie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(4).setOffensive(false);
        game.getWars().get(0).getCountries().get(4).setImplication(WarImplicationEnum.LIMITED);
        game.getWars().get(0).getCountries().get(4).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(4).getCountry().setName(pologne.getName());
        game.getWars().add(new WarEntity());
        game.getWars().get(1).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(0).setOffensive(true);
        game.getWars().get(1).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(1).setOffensive(false);
        game.getWars().get(1).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(1).getCountry().setName(venise.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(2).setOffensive(true);
        game.getWars().get(1).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(2).getCountry().setName(hollande.getName());

        when(oeUtil.getInitiative(france)).thenReturn(21);
        when(oeUtil.getInitiative(espagne)).thenReturn(12);
        when(oeUtil.getInitiative(turquie)).thenReturn(9);
        when(oeUtil.getInitiative(russie)).thenReturn(15);
        when(oeUtil.getInitiative(pologne)).thenReturn(14);
        when(oeUtil.getInitiative(venise)).thenReturn(10);
        when(oeUtil.getInitiative(hollande)).thenReturn(25);

        checkDiffsForMilitary(game);

        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE, game.getStatus());
        Assert.assertEquals(7, game.getOrders().size());
        Assert.assertEquals(game, game.getOrders().get(0).getGame());
        Assert.assertEquals(pologne.getName(), game.getOrders().get(0).getCountry().getName());
        Assert.assertEquals(0, game.getOrders().get(0).getPosition());
        Assert.assertEquals(game, game.getOrders().get(1).getGame());
        Assert.assertEquals(espagne.getName(), game.getOrders().get(1).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(1).getPosition());
        Assert.assertEquals(game, game.getOrders().get(2).getGame());
        Assert.assertEquals(russie.getName(), game.getOrders().get(2).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(2).getPosition());
        Assert.assertEquals(game, game.getOrders().get(3).getGame());
        Assert.assertEquals(venise.getName(), game.getOrders().get(3).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(3).getPosition());
        Assert.assertEquals(game, game.getOrders().get(4).getGame());
        Assert.assertEquals(france.getName(), game.getOrders().get(4).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(4).getPosition());
        Assert.assertEquals(game, game.getOrders().get(5).getGame());
        Assert.assertEquals(turquie.getName(), game.getOrders().get(5).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(5).getPosition());
        Assert.assertEquals(game, game.getOrders().get(6).getGame());
        Assert.assertEquals(hollande.getName(), game.getOrders().get(6).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(6).getPosition());
    }

    /**
     * 5 countries played:
     * <ul>
     * <li>france (init 21)</li>
     * <li>russie (init 15)</li>
     * <li>pologne (init 9)</li>
     * <li>espagne (init 9)</li>
     * <li>turquie (init 9)</li>
     * <li>venise (init 10)</li>
     * <li>hollande (init 25)</li>
     * </ul>
     * <p>
     * Two wars.
     * First one: france and turquie against russie, espagne and pologne (but pologne in LIMITED).
     * Second one: france and hollande against venise.
     * <p>
     * Expected order:
     * <ul>
     * <li>venise (init 10)</li>
     * <li>france, hollande and turquie (init 9)</li>
     * <li>espagne and russie (init 9)</li>
     * <li>pologne (init 9)</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testComputeEndMinorLogisticsComplexWars() throws Exception {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setVersion(5L);
        game.setTurn(5);
        game.setStatus(GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        france.setUsername("france");
        game.getCountries().add(france);
        PlayableCountryEntity espagne = new PlayableCountryEntity();
        espagne.setName("espagne");
        espagne.setUsername("espagne");
        game.getCountries().add(espagne);
        PlayableCountryEntity turquie = new PlayableCountryEntity();
        turquie.setName("turquie");
        turquie.setUsername("turquie");
        game.getCountries().add(turquie);
        PlayableCountryEntity russie = new PlayableCountryEntity();
        russie.setName("russie");
        russie.setUsername("russie");
        game.getCountries().add(russie);
        PlayableCountryEntity pologne = new PlayableCountryEntity();
        pologne.setName("pologne");
        pologne.setUsername("pologne");
        game.getCountries().add(pologne);
        PlayableCountryEntity venise = new PlayableCountryEntity();
        venise.setName("venise");
        venise.setUsername("venise");
        game.getCountries().add(venise);
        PlayableCountryEntity hollande = new PlayableCountryEntity();
        hollande.setName("hollande");
        hollande.setUsername("hollande");
        game.getCountries().add(hollande);
        game.getWars().add(new WarEntity());
        game.getWars().get(0).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(0).setOffensive(true);
        game.getWars().get(0).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(1).setOffensive(true);
        game.getWars().get(0).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(1).getCountry().setName(turquie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(2).setOffensive(false);
        game.getWars().get(0).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(2).getCountry().setName(espagne.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(3).setOffensive(false);
        game.getWars().get(0).getCountries().get(3).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(3).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(3).getCountry().setName(russie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(4).setOffensive(false);
        game.getWars().get(0).getCountries().get(4).setImplication(WarImplicationEnum.LIMITED);
        game.getWars().get(0).getCountries().get(4).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(4).getCountry().setName(pologne.getName());
        game.getWars().add(new WarEntity());
        game.getWars().get(1).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(0).setOffensive(true);
        game.getWars().get(1).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(1).setOffensive(false);
        game.getWars().get(1).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(1).getCountry().setName(venise.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(2).setOffensive(true);
        game.getWars().get(1).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(2).getCountry().setName(hollande.getName());

        when(oeUtil.getInitiative(france)).thenReturn(21);
        when(oeUtil.getInitiative(espagne)).thenReturn(9);
        when(oeUtil.getInitiative(turquie)).thenReturn(9);
        when(oeUtil.getInitiative(russie)).thenReturn(15);
        when(oeUtil.getInitiative(pologne)).thenReturn(9);
        when(oeUtil.getInitiative(venise)).thenReturn(10);
        when(oeUtil.getInitiative(hollande)).thenReturn(25);

        checkDiffsForMilitary(game);

        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE, game.getStatus());
        Assert.assertEquals(7, game.getOrders().size());
        Assert.assertEquals(game, game.getOrders().get(0).getGame());
        Assert.assertEquals(venise.getName(), game.getOrders().get(0).getCountry().getName());
        Assert.assertEquals(0, game.getOrders().get(0).getPosition());
        Assert.assertEquals(game, game.getOrders().get(1).getGame());
        Assert.assertEquals(france.getName(), game.getOrders().get(1).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(1).getPosition());
        Assert.assertEquals(game, game.getOrders().get(2).getGame());
        Assert.assertEquals(turquie.getName(), game.getOrders().get(2).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(2).getPosition());
        Assert.assertEquals(game, game.getOrders().get(3).getGame());
        Assert.assertEquals(hollande.getName(), game.getOrders().get(3).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(3).getPosition());
        Assert.assertEquals(game, game.getOrders().get(4).getGame());
        Assert.assertEquals(espagne.getName(), game.getOrders().get(4).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(4).getPosition());
        Assert.assertEquals(game, game.getOrders().get(5).getGame());
        Assert.assertEquals(russie.getName(), game.getOrders().get(5).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(5).getPosition());
        Assert.assertEquals(game, game.getOrders().get(6).getGame());
        Assert.assertEquals(pologne.getName(), game.getOrders().get(6).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(6).getPosition());
    }

    /**
     * 5 countries played:
     * <ul>
     * <li>france (init 21)</li>
     * <li>russie (init 15)</li>
     * <li>pologne (init 9)</li>
     * <li>espagne (init 10)</li>
     * <li>turquie (init 11)</li>
     * <li>venise (init 12)</li>
     * <li>hollande (init 25)</li>
     * </ul>
     * <p>
     * Three wars.
     * First one: venise and turquie against russie.
     * Second one: france against venise and espagne.
     * Third one: espagne against turquie.
     * <p>
     * Expected order:
     * <ul>
     * <li>hollande (init 25)</li>
     * <li>france (init 21)</li>
     * <li>russie (init 15)</li>
     * <li>venise, espagne and turquie (init 10)</li>
     * <li>pologne (init 9)</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testComputeEndMinorLogisticsSpartaWars() throws Exception {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setVersion(5L);
        game.setTurn(5);
        game.setStatus(GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        france.setUsername("france");
        game.getCountries().add(france);
        PlayableCountryEntity espagne = new PlayableCountryEntity();
        espagne.setName("espagne");
        espagne.setUsername("espagne");
        game.getCountries().add(espagne);
        PlayableCountryEntity turquie = new PlayableCountryEntity();
        turquie.setName("turquie");
        turquie.setUsername("turquie");
        game.getCountries().add(turquie);
        PlayableCountryEntity russie = new PlayableCountryEntity();
        russie.setName("russie");
        russie.setUsername("russie");
        game.getCountries().add(russie);
        PlayableCountryEntity pologne = new PlayableCountryEntity();
        pologne.setName("pologne");
        pologne.setUsername("pologne");
        game.getCountries().add(pologne);
        PlayableCountryEntity venise = new PlayableCountryEntity();
        venise.setName("venise");
        venise.setUsername("venise");
        game.getCountries().add(venise);
        PlayableCountryEntity hollande = new PlayableCountryEntity();
        hollande.setName("hollande");
        hollande.setUsername("hollande");
        game.getCountries().add(hollande);
        game.getWars().add(new WarEntity());
        game.getWars().get(0).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(0).setOffensive(true);
        game.getWars().get(0).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(0).getCountry().setName(venise.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(1).setOffensive(true);
        game.getWars().get(0).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(1).getCountry().setName(turquie.getName());
        game.getWars().get(0).getCountries().add(new CountryInWarEntity());
        game.getWars().get(0).getCountries().get(2).setOffensive(false);
        game.getWars().get(0).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(0).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(0).getCountries().get(2).getCountry().setName(russie.getName());
        game.getWars().add(new WarEntity());
        game.getWars().get(1).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(0).setOffensive(true);
        game.getWars().get(1).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(0).getCountry().setName(france.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(1).setOffensive(false);
        game.getWars().get(1).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(1).getCountry().setName(venise.getName());
        game.getWars().get(1).getCountries().add(new CountryInWarEntity());
        game.getWars().get(1).getCountries().get(2).setOffensive(false);
        game.getWars().get(1).getCountries().get(2).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(1).getCountries().get(2).setCountry(new CountryEntity());
        game.getWars().get(1).getCountries().get(2).getCountry().setName(espagne.getName());
        game.getWars().add(new WarEntity());
        game.getWars().get(2).setType(WarTypeEnum.CLASSIC_WAR);
        game.getWars().get(2).getCountries().add(new CountryInWarEntity());
        game.getWars().get(2).getCountries().get(0).setOffensive(true);
        game.getWars().get(2).getCountries().get(0).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(2).getCountries().get(0).setCountry(new CountryEntity());
        game.getWars().get(2).getCountries().get(0).getCountry().setName(espagne.getName());
        game.getWars().get(2).getCountries().add(new CountryInWarEntity());
        game.getWars().get(2).getCountries().get(1).setOffensive(false);
        game.getWars().get(2).getCountries().get(1).setImplication(WarImplicationEnum.FULL);
        game.getWars().get(2).getCountries().get(1).setCountry(new CountryEntity());
        game.getWars().get(2).getCountries().get(1).getCountry().setName(turquie.getName());

        when(oeUtil.getInitiative(france)).thenReturn(21);
        when(oeUtil.getInitiative(espagne)).thenReturn(10);
        when(oeUtil.getInitiative(turquie)).thenReturn(11);
        when(oeUtil.getInitiative(russie)).thenReturn(15);
        when(oeUtil.getInitiative(pologne)).thenReturn(9);
        when(oeUtil.getInitiative(venise)).thenReturn(12);
        when(oeUtil.getInitiative(hollande)).thenReturn(25);

        checkDiffsForMilitary(game);

        Assert.assertEquals(GameStatusEnum.MILITARY_MOVE, game.getStatus());
        Assert.assertEquals(7, game.getOrders().size());
        Assert.assertEquals(game, game.getOrders().get(0).getGame());
        Assert.assertEquals(hollande.getName(), game.getOrders().get(0).getCountry().getName());
        Assert.assertEquals(0, game.getOrders().get(0).getPosition());
        Assert.assertEquals(game, game.getOrders().get(1).getGame());
        Assert.assertEquals(france.getName(), game.getOrders().get(1).getCountry().getName());
        Assert.assertEquals(1, game.getOrders().get(1).getPosition());
        Assert.assertEquals(game, game.getOrders().get(2).getGame());
        Assert.assertEquals(russie.getName(), game.getOrders().get(2).getCountry().getName());
        Assert.assertEquals(2, game.getOrders().get(2).getPosition());
        Assert.assertEquals(game, game.getOrders().get(3).getGame());
        Assert.assertEquals(venise.getName(), game.getOrders().get(3).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(3).getPosition());
        Assert.assertEquals(game, game.getOrders().get(4).getGame());
        Assert.assertEquals(turquie.getName(), game.getOrders().get(4).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(4).getPosition());
        Assert.assertEquals(game, game.getOrders().get(5).getGame());
        Assert.assertEquals(espagne.getName(), game.getOrders().get(5).getCountry().getName());
        Assert.assertEquals(3, game.getOrders().get(5).getPosition());
        Assert.assertEquals(game, game.getOrders().get(6).getGame());
        Assert.assertEquals(pologne.getName(), game.getOrders().get(6).getCountry().getName());
        Assert.assertEquals(4, game.getOrders().get(6).getPosition());
    }

    @Test
    public void testNextRound() {
        GameEntity game = new GameEntity();
        game.setTurn(5);
        StackEntity roundStack = new StackEntity();
        roundStack.setProvince("B_MR_END");
        CounterEntity roundCounter = new CounterEntity();
        roundCounter.setType(CounterFaceTypeEnum.GOOD_WEATHER);
        roundCounter.setOwner(roundStack);
        roundStack.getCounters().add(roundCounter);
        game.getStacks().add(roundStack);
        CountryOrderEntity order = new CountryOrderEntity();
        order.setActive(true);
        order.setReady(true);
        order.setPosition(6);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setActive(true);
        order.setReady(false);
        order.setPosition(5);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setActive(false);
        order.setReady(true);
        order.setPosition(0);
        game.getOrders().add(order);

        DiffEntity winter0 = new DiffEntity();
        winter0.setId(1L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_W0", game)).thenReturn(winter0);
        DiffEntity summer1 = new DiffEntity();
        summer1.setId(2L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_S1", game)).thenReturn(summer1);
        DiffEntity summer2 = new DiffEntity();
        summer2.setId(3L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_S2", game)).thenReturn(summer2);
        DiffEntity winter2 = new DiffEntity();
        winter2.setId(4L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_W2", game)).thenReturn(winter2);
        DiffEntity summer3 = new DiffEntity();
        summer3.setId(5L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_S3", game)).thenReturn(summer3);
        DiffEntity winter3 = new DiffEntity();
        winter3.setId(6L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_W3", game)).thenReturn(winter3);
        DiffEntity winter4 = new DiffEntity();
        winter4.setId(7L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_W4", game)).thenReturn(winter4);
        DiffEntity summer5 = new DiffEntity();
        summer5.setId(8L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_S5", game)).thenReturn(summer5);
        DiffEntity winter5 = new DiffEntity();
        winter5.setId(9L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_W5", game)).thenReturn(winter5);
        DiffEntity end = new DiffEntity();
        end.setId(-1L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_End", game)).thenReturn(end);

        checkNextRound(game, "B_MR_END", 1, true, winter0, false);

        checkNextRound(game, "B_MR_END", 3, true, summer1, false);

        checkNextRound(game, "B_MR_END", 5, true, summer2, false);

        checkNextRound(game, "B_MR_END", 6, true, winter2, false);

        checkNextRound(game, "B_MR_W0", 1, false, summer1, false);

        checkNextRound(game, "B_MR_S1", 7, false, summer2, false);

        checkNextRound(game, "B_MR_S2", 4, false, winter2, false);

        checkNextRound(game, "B_MR_W2", 8, false, winter3, false);

        checkNextRound(game, "B_MR_W3", 10, false, summer5, false);

        checkNextRound(game, "B_MR_W4", 2, false, summer5, false);

        checkNextRound(game, "B_MR_W4", 8, false, winter5, false);

        checkNextRound(game, "B_MR_W4", 9, false, end, true);

        checkNextRound(game, "B_MR_S5", 5, false, winter5, false);

        checkNextRound(game, "B_MR_S5", 6, false, end, true);

        checkNextRound(game, "B_MR_W5", 1, false, end, true);

        checkNextRound(game, "B_MR_W5", 8, false, end, true);

        checkNextRound(game, "B_MR_W5", 10, false, end, true);
    }

    private void checkNextRound(GameEntity game, String roundBefore, int die, boolean init, DiffEntity roundMove, boolean end) {
        game.getStacks().get(0).setProvince(roundBefore);
        when(oeUtil.getRoundBox(game)).thenReturn(roundBefore);
        when(oeUtil.rollDie(game, (PlayableCountryEntity) null)).thenReturn(die);

        List<DiffEntity> diffs = statusWorkflowDomain.nextRound(game, init);

        long alreadyReady = game.getOrders().stream()
                .filter(CountryOrderEntity::isReady)
                .count();

        long activeNotZero = game.getOrders().stream()
                .filter(o -> o.isActive() && o.getPosition() != 0)
                .count();

        long zeroNotActive = game.getOrders().stream()
                .filter(o -> !o.isActive() && o.getPosition() == 0)
                .count();

        Assert.assertEquals(0, alreadyReady);
        Assert.assertEquals(0, activeNotZero);
        Assert.assertEquals(0, zeroNotActive);

        if (end) {
            Assert.assertTrue(diffs.contains(roundMove));
        } else {

            Assert.assertEquals(4, diffs.size());

            DiffEntity diff = diffs.stream()
                    .filter(d -> d == roundMove)
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull(diff);

            diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.STACK)
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull(diff);
            Assert.assertEquals(null, diff.getIdObject());
            Assert.assertEquals(1, diff.getAttributes().size());
            Assert.assertEquals(MovePhaseEnum.NOT_MOVED.name(), getAttribute(diff, DiffAttributeTypeEnum.MOVE_PHASE));

            diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull(diff);
            Assert.assertEquals(null, diff.getIdObject());
            Assert.assertEquals(1, diff.getAttributes().size());
            Assert.assertEquals(GameStatusEnum.MILITARY_MOVE.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));

            diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.TURN_ORDER)
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull(diff);
            Assert.assertEquals(null, diff.getIdObject());
            Assert.assertEquals(2, diff.getAttributes().size());
            Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.ACTIVE));
            Assert.assertEquals(GameStatusEnum.MILITARY_MOVE.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));
        }
    }

    @Test
    public void testEndRound() {
        GameEntity game = new GameEntity();
        StackEntity stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("idf");
        stack.getCounters().add(createCounter(1L, null, CounterFaceTypeEnum.PILLAGE_MINUS, stack));
        game.getStacks().add(stack);
        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("orleans");
        stack.getCounters().add(createCounter(2L, null, CounterFaceTypeEnum.PILLAGE_PLUS, stack));
        game.getStacks().add(stack);
        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("picardie");
        stack.getCounters().add(createCounter(3L, null, CounterFaceTypeEnum.PILLAGE_PLUS, stack));
        stack.getCounters().add(createCounter(4L, null, CounterFaceTypeEnum.PILLAGE_MINUS, stack));
        game.getStacks().add(stack);
        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("vendee");
        stack.getCounters().add(createCounter(5L, null, CounterFaceTypeEnum.PILLAGE_MINUS, stack));
        stack.getCounters().add(createCounter(6L, null, CounterFaceTypeEnum.PILLAGE_PLUS, stack));
        game.getStacks().add(stack);
        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("auvergne");
        stack.getCounters().add(createCounter(7L, null, CounterFaceTypeEnum.REVOLT_MINUS, stack));
        game.getStacks().add(stack);


        DiffEntity end = new DiffEntity();
        end.setId(1L);
        when(counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_End", game)).thenReturn(end);
        when(counterDomain.removeCounter(any())).thenAnswer(removeCounterAnswer());
        when(counterDomain.switchCounter(any(), any(), anyInt(), any())).thenAnswer(switchCounterAnswer());

        List<DiffEntity> diffs = statusWorkflowDomain.endRound(game);

        Assert.assertEquals(8, diffs.size());
        DiffEntity diff = diffs.stream()
                .filter(d -> d == end)
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.TURN_ORDER)
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER && Objects.equals(1L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.COUNTER && Objects.equals(2L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER && Objects.equals(4L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER && Objects.equals(5L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.STACK && Objects.equals(null, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
    }

    @Test
    public void testEndMilitaryPhaseStillBattle() {
        int currentTurn = 12;
        int notCurrentTurn = 11;
        GameEntity game = new GameEntity();
        game.setTurn(currentTurn);
        game.setStatus(GameStatusEnum.MILITARY_BATTLES);
        BattleEntity battle = new BattleEntity();
        battle.setStatus(BattleStatusEnum.NEW);
        battle.setTurn(currentTurn);
        game.getBattles().add(battle);
        SiegeEntity siege = new SiegeEntity();
        siege.setStatus(SiegeStatusEnum.NEW);
        siege.setTurn(notCurrentTurn);
        game.getSieges().add(siege);

        List<DiffEntity> diffs = statusWorkflowDomain.endMilitaryPhase(game);

        Assert.assertEquals(0, diffs.size());
        Assert.assertEquals(GameStatusEnum.MILITARY_BATTLES, game.getStatus());
    }

    @Test
    public void testEndMilitaryPhaseStillSiege() {
        int currentTurn = 12;
        int notCurrentTurn = 11;
        GameEntity game = new GameEntity();
        game.setTurn(currentTurn);
        game.setStatus(GameStatusEnum.MILITARY_SIEGES);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        game.getCountries().add(france);
        PlayableCountryEntity spain = new PlayableCountryEntity();
        spain.setName("spain");
        game.getCountries().add(spain);

        CountryOrderEntity order = new CountryOrderEntity();
        order.setCountry(france);
        order.setPosition(0);
        order.setActive(true);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setCountry(spain);
        order.setPosition(1);
        order.setActive(false);
        game.getOrders().add(order);

        WarEntity war = new WarEntity();
        CountryInWarEntity countryWar = new CountryInWarEntity();
        countryWar.setOffensive(true);
        countryWar.setImplication(WarImplicationEnum.FULL);
        countryWar.setCountry(new CountryEntity());
        countryWar.getCountry().setName(france.getName());
        war.getCountries().add(countryWar);
        countryWar = new CountryInWarEntity();
        countryWar.setOffensive(false);
        countryWar.setImplication(WarImplicationEnum.FULL);
        countryWar.setCountry(new CountryEntity());
        countryWar.getCountry().setName(spain.getName());
        war.getCountries().add(countryWar);
        game.getWars().add(war);

        BattleEntity battle = new BattleEntity();
        battle.setStatus(BattleStatusEnum.NEW);
        battle.setTurn(notCurrentTurn);
        battle.setWar(war);
        game.getBattles().add(battle);

        SiegeEntity siege = new SiegeEntity();
        siege.setStatus(SiegeStatusEnum.NEW);
        siege.setTurn(currentTurn);
        siege.setWar(war);
        siege.setBesiegingOffensive(true);
        game.getSieges().add(siege);

        List<DiffEntity> diffs = statusWorkflowDomain.endMilitaryPhase(game);

        Assert.assertEquals(0, diffs.size());
        Assert.assertEquals(GameStatusEnum.MILITARY_SIEGES, game.getStatus());
    }

    @Test
    public void testEndMilitaryPhase() {
        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_MOVE)
                .futureBattle()
                .futureSiege()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_BATTLES)
                        .changeStatus()
                        .addBattle());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_MOVE)
                .futureSiege()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_MOVE)
                        .changeActivePlayer());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_MOVE)
                .lastPlayerInTurnOrder()
                .futureSiege()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_SIEGES)
                        .changeStatus()
                        .changeActivePlayer()
                        .addSiege());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_MOVE)
                .lastPlayerInTurnOrder()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_MOVE)
                        .changeStatus()
                        .changeActivePlayer()
                        .nextRound());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_BATTLES)
                .pendingBattle()
                .futureSiege()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_BATTLES));

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_BATTLES)
                .futureSiege()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_MOVE)
                        .changeStatus()
                        .changeActivePlayer());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_BATTLES)
                .lastPlayerInTurnOrder()
                .futureSiege()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_SIEGES)
                        .changeStatus()
                        .changeActivePlayer()
                        .addSiege());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_BATTLES)
                .lastPlayerInTurnOrder()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_MOVE)
                        .changeStatus()
                        .changeActivePlayer()
                        .nextRound());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_SIEGES)
                .pendingSiege()
                .pendingOtherPlayerSiege()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_SIEGES));

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_SIEGES)
                .pendingOtherPlayerSiege()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_SIEGES)
                        .changeActivePlayer());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_SIEGES)
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_MOVE)
                        .changeStatus()
                        .changeActivePlayer()
                        .nextRound());

        EndMilitaryPhaseBuilder.create()
                .status(GameStatusEnum.MILITARY_SIEGES)
                .lastPlayerInTurnOrder()
                .whenEndMilitaryPhase(statusWorkflowDomain, this)
                .thenExpect(EndMilitaryPhaseResultBuilder.create()
                        .status(GameStatusEnum.MILITARY_MOVE)
                        .changeStatus()
                        .changeActivePlayer()
                        .nextRound());
    }

    static class EndMilitaryPhaseBuilder {
        boolean lastPlayerInTurnOrder;
        boolean pendingBattle;
        boolean pendingSiege;
        boolean pendingOtherPlayerSiege;
        boolean futureBattle;
        boolean futureSiege;
        GameStatusEnum status;
        GameEntity game;
        List<DiffEntity> diffs;

        static EndMilitaryPhaseBuilder create() {
            return new EndMilitaryPhaseBuilder();
        }

        EndMilitaryPhaseBuilder lastPlayerInTurnOrder() {
            lastPlayerInTurnOrder = true;
            return this;
        }

        EndMilitaryPhaseBuilder pendingBattle() {
            pendingBattle = true;
            return this;
        }

        EndMilitaryPhaseBuilder pendingSiege() {
            pendingSiege = true;
            return this;
        }

        EndMilitaryPhaseBuilder pendingOtherPlayerSiege() {
            pendingOtherPlayerSiege = true;
            return this;
        }

        EndMilitaryPhaseBuilder futureBattle() {
            futureBattle = true;
            return this;
        }

        EndMilitaryPhaseBuilder futureSiege() {
            futureSiege = true;
            return this;
        }

        EndMilitaryPhaseBuilder status(GameStatusEnum status) {
            this.status = status;
            return this;
        }

        EndMilitaryPhaseBuilder whenEndMilitaryPhase(IStatusWorkflowDomain statusWorkflowDomain, StatusWorkflowDomainTest testClass) {
            int currentTurn = 12;
            int notCurrentTurn = 11;
            game = new GameEntity();
            game.setTurn(currentTurn);
            game.setStatus(status);
            PlayableCountryEntity france = new PlayableCountryEntity();
            france.setName("france");
            game.getCountries().add(france);
            PlayableCountryEntity spain = new PlayableCountryEntity();
            spain.setName("spain");
            game.getCountries().add(spain);

            CountryOrderEntity order = new CountryOrderEntity();
            order.setCountry(france);
            order.setPosition(0);
            order.setActive(!lastPlayerInTurnOrder);
            game.getOrders().add(order);
            order = new CountryOrderEntity();
            order.setCountry(spain);
            order.setPosition(1);
            order.setActive(lastPlayerInTurnOrder);
            game.getOrders().add(order);

            WarEntity war = new WarEntity();
            war.setId(666l);
            CountryInWarEntity countryWar = new CountryInWarEntity();
            countryWar.setOffensive(true);
            countryWar.setImplication(WarImplicationEnum.FULL);
            countryWar.setCountry(new CountryEntity());
            countryWar.getCountry().setName(france.getName());
            war.getCountries().add(countryWar);
            countryWar = new CountryInWarEntity();
            countryWar.setOffensive(false);
            countryWar.setImplication(WarImplicationEnum.FULL);
            countryWar.setCountry(new CountryEntity());
            countryWar.getCountry().setName(spain.getName());
            war.getCountries().add(countryWar);
            game.getWars().add(war);

            BattleEntity battle = new BattleEntity();
            battle.setStatus(BattleStatusEnum.NEW);
            battle.setTurn(notCurrentTurn);
            battle.setWar(war);
            game.getBattles().add(battle);

            SiegeEntity siege = new SiegeEntity();
            siege.setStatus(SiegeStatusEnum.NEW);
            siege.setTurn(notCurrentTurn);
            siege.setWar(war);
            game.getSieges().add(siege);

            if (pendingBattle) {
                battle = new BattleEntity();
                battle.setStatus(BattleStatusEnum.NEW);
                battle.setTurn(currentTurn);
                battle.setWar(war);
                battle.setPhasingOffensive(!lastPlayerInTurnOrder);
                game.getBattles().add(battle);
            }
            if (pendingSiege) {
                siege = new SiegeEntity();
                siege.setStatus(SiegeStatusEnum.NEW);
                siege.setTurn(currentTurn);
                siege.setWar(war);
                siege.setBesiegingOffensive(!lastPlayerInTurnOrder);
                game.getSieges().add(siege);
            }
            if (pendingOtherPlayerSiege) {
                siege = new SiegeEntity();
                siege.setStatus(SiegeStatusEnum.NEW);
                siege.setTurn(currentTurn);
                siege.setWar(war);
                siege.setBesiegingOffensive(lastPlayerInTurnOrder);
                game.getSieges().add(siege);
            }
            if (futureBattle) {
                StackEntity stack = new StackEntity();
                stack.setProvince("idf");
                stack.setMovePhase(MovePhaseEnum.FIGHTING);
                game.getStacks().add(stack);
                stack = new StackEntity();
                stack.setProvince("idf");
                stack.setMovePhase(MovePhaseEnum.FIGHTING);
                game.getStacks().add(stack);
            }
            if (futureSiege) {
                StackEntity stack = new StackEntity();
                stack.setProvince("idf");
                stack.setMovePhase(MovePhaseEnum.BESIEGING);
                game.getStacks().add(stack);
                stack = new StackEntity();
                stack.setProvince("idf");
                stack.setMovePhase(MovePhaseEnum.BESIEGING);
                game.getStacks().add(stack);
            }

            //noinspection unchecked
            when(testClass.oeUtil.searchWar(anyList(), anyList(), any())).thenReturn(new ImmutablePair<>(war, false));
            AbstractProvinceEntity idf = new EuropeanProvinceEntity();
            when(testClass.provinceDao.getProvinceByName("idf")).thenReturn(idf);
            when(testClass.oeUtil.getController(idf, game)).thenReturn(france.getName());
            when(testClass.counterDomain.moveSpecialCounter(CounterFaceTypeEnum.GOOD_WEATHER, null, "B_MR_W-1", game)).thenReturn(DiffUtil.createDiff(game, DiffTypeEnum.MOVE, DiffTypeObjectEnum.COUNTER));

            diffs = statusWorkflowDomain.endMilitaryPhase(game);

            return this;
        }

        EndMilitaryPhaseBuilder thenExpect(EndMilitaryPhaseResultBuilder result) {
            Assert.assertEquals("The status of the game is not correct.", result.status, game.getStatus());
            DiffEntity changeStatus = diffs.stream()
                    .filter(diff -> diff.getType() == DiffTypeEnum.MODIFY && diff.getTypeObject() == DiffTypeObjectEnum.GAME)
                    .findAny()
                    .orElse(null);
            if (result.changeStatus) {
                Assert.assertNotNull("A modify status diff event was expected.", changeStatus);
                Assert.assertEquals("The modify status diff event has the wrong new status.", result.status.name(), getAttribute(changeStatus, DiffAttributeTypeEnum.STATUS));
            } else {
                Assert.assertNull("A modify status diff event was not expected.", changeStatus);
            }
            DiffEntity changeActivePlayer = diffs.stream()
                    .filter(diff -> diff.getType() == DiffTypeEnum.MODIFY && diff.getTypeObject() == DiffTypeObjectEnum.TURN_ORDER)
                    .findAny()
                    .orElse(null);
            if (result.changeActivePlayer) {
                Assert.assertNotNull("A modify turn order diff event was expected.", changeActivePlayer);
                Assert.assertEquals("The modify turn order diff event has the wrong new active player.", result.nextRound ? "0" : "1", getAttribute(changeActivePlayer, DiffAttributeTypeEnum.ACTIVE));
            } else {
                Assert.assertNull("A modify turn order diff event was not expected.", changeActivePlayer);
            }
            DiffEntity addBattle = diffs.stream()
                    .filter(diff -> diff.getType() == DiffTypeEnum.ADD && diff.getTypeObject() == DiffTypeObjectEnum.BATTLE)
                    .findAny()
                    .orElse(null);
            if (result.addBattle) {
                Assert.assertNotNull("A add battle diff event was expected.", addBattle);
                Assert.assertEquals("The add battle diff event has the wrong province.", "idf", getAttribute(addBattle, DiffAttributeTypeEnum.PROVINCE));
                Assert.assertEquals("The add battle diff event has the wrong turn.", game.getTurn().toString(), getAttribute(addBattle, DiffAttributeTypeEnum.TURN));
                Assert.assertEquals("The add battle diff event has the wrong status.", BattleStatusEnum.NEW.name(), getAttribute(addBattle, DiffAttributeTypeEnum.STATUS));
                Assert.assertEquals("The add battle diff event has the wrong war.", "666", getAttribute(addBattle, DiffAttributeTypeEnum.ID_WAR));
                Assert.assertEquals("The add battle diff event has the wrong phasing offensive.", "false", getAttribute(addBattle, DiffAttributeTypeEnum.PHASING_OFFENSIVE));
            } else {
                Assert.assertNull("A add battle order diff event was not expected.", addBattle);
            }
            int expectedBattles = 1 + (pendingBattle ? 1 : 0) + (result.addBattle ? 1 : 0);
            Assert.assertEquals("The game has not the right number of battles.", expectedBattles, game.getBattles().size());
            DiffEntity addSiege = diffs.stream()
                    .filter(diff -> diff.getType() == DiffTypeEnum.ADD && diff.getTypeObject() == DiffTypeObjectEnum.SIEGE)
                    .findAny()
                    .orElse(null);
            if (result.addSiege) {
                Assert.assertNotNull("A add siege diff event was expected.", addSiege);
                Assert.assertEquals("The add siege diff event has the wrong province.", "idf", getAttribute(addSiege, DiffAttributeTypeEnum.PROVINCE));
                Assert.assertEquals("The add siege diff event has the wrong turn.", game.getTurn().toString(), getAttribute(addSiege, DiffAttributeTypeEnum.TURN));
                Assert.assertEquals("The add siege diff event has the wrong status.", SiegeStatusEnum.NEW.name(), getAttribute(addSiege, DiffAttributeTypeEnum.STATUS));
                Assert.assertEquals("The add siege diff event has the wrong war.", "666", getAttribute(addSiege, DiffAttributeTypeEnum.ID_WAR));
                Assert.assertEquals("The add siege diff event has the wrong phasing offensive.", "false", getAttribute(addSiege, DiffAttributeTypeEnum.PHASING_OFFENSIVE));
            } else {
                Assert.assertNull("A add siege order diff event was not expected.", addSiege);
            }
            int expectedSieges = 1 + (pendingSiege ? 1 : 0) + (pendingOtherPlayerSiege ? 1 : 0) + (result.addSiege ? 1 : 0);
            Assert.assertEquals("The game has not the right number of sieges.", expectedSieges, game.getSieges().size());
            DiffEntity nextRound = diffs.stream()
                    .filter(diff -> diff.getType() == DiffTypeEnum.MOVE && diff.getTypeObject() == DiffTypeObjectEnum.COUNTER)
                    .findAny()
                    .orElse(null);
            if (result.nextRound) {
                Assert.assertNotNull("A next round diff event was expected.", nextRound);
            } else {
                Assert.assertNull("A next round diff event was not expected.", nextRound);
            }

            return this;
        }
    }

    static class EndMilitaryPhaseResultBuilder {
        boolean changeActivePlayer;
        boolean changeStatus;
        boolean addBattle;
        boolean addSiege;
        boolean nextRound;
        GameStatusEnum status;

        static EndMilitaryPhaseResultBuilder create() {
            return new EndMilitaryPhaseResultBuilder();
        }

        EndMilitaryPhaseResultBuilder changeActivePlayer() {
            changeActivePlayer = true;
            return this;
        }

        EndMilitaryPhaseResultBuilder changeStatus() {
            changeStatus = true;
            return this;
        }

        EndMilitaryPhaseResultBuilder addBattle() {
            addBattle = true;
            return this;
        }

        EndMilitaryPhaseResultBuilder addSiege() {
            addSiege = true;
            return this;
        }

        EndMilitaryPhaseResultBuilder nextRound() {
            nextRound = true;
            return this;
        }

        EndMilitaryPhaseResultBuilder status(GameStatusEnum status) {
            this.status = status;
            return this;
        }
    }

    @Test
    public void testEndRedeploymentPhaseToNextPlayer() {
        GameEntity game = new GameEntity();
        game.setStatus(GameStatusEnum.REDEPLOYMENT);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        game.getCountries().add(france);
        PlayableCountryEntity turkey = new PlayableCountryEntity();
        turkey.setName("turkey");
        game.getCountries().add(turkey);
        PlayableCountryEntity spain = new PlayableCountryEntity();
        spain.setName("spain");
        game.getCountries().add(spain);
        CountryOrderEntity order = new CountryOrderEntity();
        order.setPosition(0);
        order.setCountry(france);
        order.setActive(true);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setPosition(0);
        order.setCountry(turkey);
        order.setActive(true);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setPosition(1);
        order.setCountry(spain);
        game.getOrders().add(order);

        List<DiffEntity> diffs = statusWorkflowDomain.endRedeploymentPhase(game);

        Assert.assertEquals(GameStatusEnum.REDEPLOYMENT, game.getStatus());
        Assert.assertEquals(2, diffs.size());
        DiffEntity diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.TURN_ORDER)
                .findAny()
                .orElse(null);
        Assert.assertEquals("1", getAttribute(diff, DiffAttributeTypeEnum.ACTIVE));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.STACK)
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
    }

    @Test
    public void testEndRedeploymentPhaseAdjustSiegeworks() {
        GameEntity game = new GameEntity();
        game.setStatus(GameStatusEnum.REDEPLOYMENT);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setName("france");
        game.getCountries().add(france);
        PlayableCountryEntity turkey = new PlayableCountryEntity();
        turkey.setName("turkey");
        game.getCountries().add(turkey);
        PlayableCountryEntity spain = new PlayableCountryEntity();
        spain.setName("spain");
        game.getCountries().add(spain);
        CountryOrderEntity order = new CountryOrderEntity();
        order.setPosition(0);
        order.setCountry(france);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setPosition(0);
        order.setCountry(turkey);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setPosition(1);
        order.setActive(true);
        order.setCountry(spain);
        game.getOrders().add(order);

        StackEntity stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("pecs");
        stack.getCounters().add(createCounter(1L, null, CounterFaceTypeEnum.SIEGEWORK_MINUS, stack));
        stack.getCounters().add(createCounter(2L, null, CounterFaceTypeEnum.SIEGEWORK_PLUS, stack));
        game.getStacks().add(stack);

        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("idf");
        stack.getCounters().add(createCounter(11L, null, CounterFaceTypeEnum.SIEGEWORK_MINUS, stack));
        game.getStacks().add(stack);
        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("idf");
        stack.setMovePhase(MovePhaseEnum.BESIEGING);
        game.getStacks().add(stack);

        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("silesie");
        stack.getCounters().add(createCounter(21L, null, CounterFaceTypeEnum.SIEGEWORK_PLUS, stack));
        game.getStacks().add(stack);
        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("silesie");
        stack.setMovePhase(MovePhaseEnum.BESIEGING);
        game.getStacks().add(stack);

        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("orleanais");
        stack.getCounters().add(createCounter(31L, null, CounterFaceTypeEnum.SIEGEWORK_MINUS, stack));
        stack.getCounters().add(createCounter(32L, null, CounterFaceTypeEnum.SIEGEWORK_PLUS, stack));
        game.getStacks().add(stack);
        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("orleanais");
        stack.setMovePhase(MovePhaseEnum.BESIEGING);
        game.getStacks().add(stack);

        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("vendee");
        stack.getCounters().add(createCounter(41L, null, CounterFaceTypeEnum.SIEGEWORK_PLUS, stack));
        stack.getCounters().add(createCounter(42L, null, CounterFaceTypeEnum.SIEGEWORK_PLUS, stack));
        game.getStacks().add(stack);
        stack = new StackEntity();
        stack.setGame(game);
        stack.setProvince("vendee");
        stack.setMovePhase(MovePhaseEnum.BESIEGING);
        game.getStacks().add(stack);

        when(counterDomain.removeCounter(any())).thenAnswer(removeCounterAnswer());
        when(counterDomain.switchCounter(any(), any(), any(), any())).thenAnswer(switchCounterAnswer());

        List<DiffEntity> diffs = statusWorkflowDomain.endRedeploymentPhase(game);

        Assert.assertEquals(GameStatusEnum.EXCHEQUER, game.getStatus());
        Assert.assertEquals(7, diffs.size());
        DiffEntity diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                .findAny()
                .orElse(null);
        Assert.assertEquals(GameStatusEnum.EXCHEQUER.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER
                        && Objects.equals(1L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER
                        && Objects.equals(2L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.COUNTER
                        && Objects.equals(21L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(CounterFaceTypeEnum.SIEGEWORK_MINUS.name(), getAttribute(diff, DiffAttributeTypeEnum.TYPE));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER
                        && Objects.equals(32L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER
                        && (Objects.equals(41L, d.getIdObject()) || Objects.equals(42L, d.getIdObject())))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.COUNTER
                        && (Objects.equals(41L, d.getIdObject()) || Objects.equals(42L, d.getIdObject())))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(CounterFaceTypeEnum.SIEGEWORK_MINUS.name(), getAttribute(diff, DiffAttributeTypeEnum.TYPE));
    }

    @Test
    public void testEndRedeploymentPhaseExceptionalTaxes() {
        GameEntity game = new GameEntity();
        game.setTurn(12);
        game.setStatus(GameStatusEnum.REDEPLOYMENT);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setId(1L);
        france.setName("france");
        game.getCountries().add(france);
        PlayableCountryEntity turkey = new PlayableCountryEntity();
        turkey.setId(2L);
        turkey.setName("turkey");
        game.getCountries().add(turkey);
        PlayableCountryEntity spain = new PlayableCountryEntity();
        spain.setId(3L);
        spain.setName("spain");
        game.getCountries().add(spain);
        CountryOrderEntity order = new CountryOrderEntity();
        order.setPosition(0);
        order.setCountry(france);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setPosition(0);
        order.setCountry(turkey);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setPosition(1);
        order.setActive(true);
        order.setCountry(spain);
        game.getOrders().add(order);

        EconomicalSheetEntity sheet = new EconomicalSheetEntity();
        sheet.setId(1L);
        sheet.setTurn(game.getTurn() - 1);
        sheet.setExcTaxesMod(2);
        france.getEconomicalSheets().add(sheet);
        sheet = new EconomicalSheetEntity();
        sheet.setId(2L);
        sheet.setTurn(game.getTurn());
        france.getEconomicalSheets().add(sheet);

        sheet = new EconomicalSheetEntity();
        sheet.setId(11L);
        sheet.setTurn(game.getTurn());
        sheet.setExcTaxesMod(2);
        turkey.getEconomicalSheets().add(sheet);

        sheet = new EconomicalSheetEntity();
        sheet.setId(21L);
        sheet.setTurn(game.getTurn() - 1);
        sheet.setExcTaxesMod(5);
        spain.getEconomicalSheets().add(sheet);
        sheet = new EconomicalSheetEntity();
        sheet.setId(22L);
        sheet.setTurn(game.getTurn());
        sheet.setExcTaxesMod(-2);
        spain.getEconomicalSheets().add(sheet);

        when(oeUtil.rollDie(any(), (PlayableCountryEntity) any())).thenReturn(1);

        fillTables();

        List<DiffEntity> diffs = statusWorkflowDomain.endRedeploymentPhase(game);

        Assert.assertEquals(GameStatusEnum.EXCHEQUER, game.getStatus());
        Assert.assertEquals(4, diffs.size());
        DiffEntity diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                .findAny()
                .orElse(null);
        Assert.assertEquals(GameStatusEnum.EXCHEQUER.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(2L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(null, getAttributeFull(diff, DiffAttributeTypeEnum.EXC_TAXES));
        Assert.assertEquals("1", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(11L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals("30", getAttribute(diff, DiffAttributeTypeEnum.EXC_TAXES));
        Assert.assertEquals("2", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(22L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals("-10", getAttribute(diff, DiffAttributeTypeEnum.EXC_TAXES));
        Assert.assertEquals("3", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
    }

    @Test
    public void testEndRedeploymentPhaseExchequer() {
        GameEntity game = new GameEntity();
        game.setTurn(10);
        game.setStatus(GameStatusEnum.REDEPLOYMENT);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setId(1L);
        france.setName("france");
        game.getCountries().add(france);
        PlayableCountryEntity turkey = new PlayableCountryEntity();
        turkey.setId(2L);
        turkey.setName("turkey");
        game.getCountries().add(turkey);
        PlayableCountryEntity spain = new PlayableCountryEntity();
        spain.setId(3L);
        spain.setName("spain");
        game.getCountries().add(spain);
        CountryOrderEntity order = new CountryOrderEntity();
        order.setPosition(0);
        order.setCountry(france);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setPosition(0);
        order.setCountry(turkey);
        game.getOrders().add(order);
        order = new CountryOrderEntity();
        order.setPosition(1);
        order.setActive(true);
        order.setCountry(spain);
        game.getOrders().add(order);

        EconomicalSheetEntity franceSheet = new EconomicalSheetEntity();
        franceSheet.setId(1L);
        franceSheet.setTurn(game.getTurn());
        franceSheet.setRtDiplo(100);
        franceSheet.setPillages(10);
        franceSheet.setGoldRotw(25);
        franceSheet.setExcTaxes(-10);
        franceSheet.setInterestExpense(2);
        franceSheet.setMandRefundExpense(20);
        franceSheet.setAdmTotalExpense(200);
        franceSheet.setMilitaryExpense(100);
        franceSheet.setGrossIncome(400);
        france.getEconomicalSheets().add(franceSheet);
        france.getEconomicalSheets().add(new EconomicalSheetEntity());

        spain.getEconomicalSheets().add(new EconomicalSheetEntity());
        EconomicalSheetEntity spainSheet = new EconomicalSheetEntity();
        spainSheet.setId(2L);
        spainSheet.setTurn(game.getTurn());
        spainSheet.setGrossIncome(98);
        spain.getEconomicalSheets().add(spainSheet);

        turkey.getEconomicalSheets().add(new EconomicalSheetEntity());

        when(oeUtil.getStability(game, france.getName())).thenReturn(1);
        when(oeUtil.getStability(game, spain.getName())).thenReturn(-3);
        when(oeUtil.getWarStatus(game, france)).thenReturn(WarStatusEnum.PEACE);
        when(oeUtil.getWarStatus(game, spain)).thenReturn(WarStatusEnum.CLASSIC_WAR);
        when(oeUtil.rollDie(game, france)).thenReturn(10);
        when(oeUtil.rollDie(game, spain)).thenReturn(0);

        fillTables();

        List<DiffEntity> diffs = statusWorkflowDomain.endRedeploymentPhase(game);

        Assert.assertEquals(GameStatusEnum.EXCHEQUER, game.getStatus());
        Assert.assertEquals(3, diffs.size());
        DiffEntity diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                .findAny()
                .orElse(null);
        Assert.assertEquals(GameStatusEnum.EXCHEQUER.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));
        Assert.assertEquals("false", getAttribute(diff, DiffAttributeTypeEnum.ACTIVE));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(1L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(france.getId() + "", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        Assert.assertEquals("125", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_ROYAL_TREASURE));
        Assert.assertEquals(125, franceSheet.getRtBefExch().intValue());
        Assert.assertEquals("322", getAttribute(diff, DiffAttributeTypeEnum.EXPENSES));
        Assert.assertEquals(322, franceSheet.getExpenses().intValue());
        Assert.assertEquals("1", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_COL));
        Assert.assertEquals(1, franceSheet.getExchequerColumn().intValue());
        Assert.assertEquals("2", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_MOD));
        Assert.assertEquals(2, franceSheet.getExchequerBonus().intValue());
        Assert.assertEquals("10", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_DIE));
        Assert.assertEquals(10, franceSheet.getExchequerDie().intValue());
        Assert.assertEquals("240", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_REGULAR));
        Assert.assertEquals(240, franceSheet.getRegularIncome().intValue());
        Assert.assertEquals("160", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_PRESTIGE));
        Assert.assertEquals(160, franceSheet.getPrestigeIncome().intValue());
        Assert.assertEquals("80", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_MAX_NAT_LOAN));
        Assert.assertEquals(80, franceSheet.getMaxNatLoan().intValue());
        Assert.assertEquals("82", getAttribute(diff, DiffAttributeTypeEnum.REMAINING_EXPENSES));
        Assert.assertEquals(82, franceSheet.getRemainingExpenses().intValue());
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(2L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(spain.getId() + "", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_ROYAL_TREASURE));
        Assert.assertEquals(0, spainSheet.getRtBefExch().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.EXPENSES));
        Assert.assertEquals(0, spainSheet.getExpenses().intValue());
        Assert.assertEquals("-3", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_COL));
        Assert.assertEquals(-3, spainSheet.getExchequerColumn().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_MOD));
        Assert.assertEquals(0, spainSheet.getExchequerBonus().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_DIE));
        Assert.assertEquals(0, spainSheet.getExchequerDie().intValue());
        Assert.assertEquals("29", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_REGULAR));
        Assert.assertEquals(29, spainSheet.getRegularIncome().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_PRESTIGE));
        Assert.assertEquals(0, spainSheet.getPrestigeIncome().intValue());
        Assert.assertEquals("49", getAttribute(diff, DiffAttributeTypeEnum.EXCHEQUER_MAX_NAT_LOAN));
        Assert.assertEquals(49, spainSheet.getMaxNatLoan().intValue());
        Assert.assertEquals("-29", getAttribute(diff, DiffAttributeTypeEnum.REMAINING_EXPENSES));
        Assert.assertEquals(-29, spainSheet.getRemainingExpenses().intValue());
    }

    private void fillTables() {
        AbstractBack.TABLES = new Tables();
        Result result = new Result();
        result.setColumn(1);
        result.setDie(10);
        result.setResult(ResultEnum.CRITICAL_HIT);
        AbstractBack.TABLES.getResults().add(result);
        result = new Result();
        result.setColumn(0);
        result.setDie(1);
        result.setResult(ResultEnum.FAILED);
        AbstractBack.TABLES.getResults().add(result);
        result = new Result();
        result.setColumn(-3);
        result.setDie(1);
        result.setResult(ResultEnum.FUMBLE);
        AbstractBack.TABLES.getResults().add(result);
        Exchequer exchequer = new Exchequer();
        exchequer.setResult(ResultEnum.CRITICAL_HIT);
        exchequer.setRegular(60);
        exchequer.setPrestige(40);
        exchequer.setNatLoan(20);
        AbstractBack.TABLES.getExchequers().add(exchequer);
        exchequer = new Exchequer();
        exchequer.setResult(ResultEnum.FAILED);
        exchequer.setRegular(20);
        exchequer.setPrestige(20);
        exchequer.setNatLoan(40);
        AbstractBack.TABLES.getExchequers().add(exchequer);
        exchequer = new Exchequer();
        exchequer.setResult(ResultEnum.FUMBLE);
        exchequer.setRegular(30);
        exchequer.setPrestige(00);
        exchequer.setNatLoan(40);
        AbstractBack.TABLES.getExchequers().add(exchequer);
    }

    @Test
    public void testEndExchequerFirstTurnPeriod() {
        GameEntity game = new GameEntity();
        game.setTurn(7);
        game.setStatus(GameStatusEnum.EXCHEQUER);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setId(1L);
        france.setName("france");
        france.setReady(true);
        game.getCountries().add(france);
        PlayableCountryEntity turkey = new PlayableCountryEntity();
        turkey.setId(2L);
        turkey.setName("turkey");
        turkey.setReady(true);
        game.getCountries().add(turkey);
        PlayableCountryEntity spain = new PlayableCountryEntity();
        spain.setId(3L);
        spain.setName("spain");
        spain.setReady(true);
        game.getCountries().add(spain);

        EconomicalSheetEntity franceSheet = new EconomicalSheetEntity();
        franceSheet.setId(1L);
        franceSheet.setTurn(game.getTurn());
        franceSheet.setPrestigeSpent(16);
        franceSheet.setNatLoan(10);
        franceSheet.setInterLoan(25);
        franceSheet.setRemainingExpenses(-10);
        franceSheet.setRtBefExch(-5);
        franceSheet.setPrestigeIncome(24);
        franceSheet.setGrossIncome(400);
        france.getEconomicalSheets().add(franceSheet);
        EconomicalSheetEntity previous = new EconomicalSheetEntity();
        previous.setId(11L);
        previous.setTurn(game.getTurn() - 1);
        previous.setPeriodWealth(500);
        france.getEconomicalSheets().add(previous);

        EconomicalSheetEntity spainSheet = new EconomicalSheetEntity();
        spainSheet.setId(2L);
        spainSheet.setTurn(game.getTurn());
        spainSheet.setRemainingExpenses(0);
        spainSheet.setGrossIncome(100);
        spain.getEconomicalSheets().add(spainSheet);
        previous = new EconomicalSheetEntity();
        previous.setId(21L);
        previous.setTurn(game.getTurn() - 2);
        previous.setPeriodWealth(500);
        spain.getEconomicalSheets().add(previous);

        turkey.getEconomicalSheets().add(new EconomicalSheetEntity());

        AbstractBack.TABLES = new Tables();
        Period period = new Period();
        period.setBegin(7);
        period.setEnd(15);
        period.setName(Period.PERIOD_II);
        AbstractBack.TABLES.getPeriods().add(period);

        List<DiffEntity> diffs = statusWorkflowDomain.endExchequerPhase(game);

        Assert.assertEquals(GameStatusEnum.STABILITY, game.getStatus());
        Assert.assertEquals(3, diffs.size());
        DiffEntity diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                .findAny()
                .orElse(null);
        Assert.assertEquals(GameStatusEnum.STABILITY.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));
        Assert.assertEquals("false", getAttribute(diff, DiffAttributeTypeEnum.ACTIVE));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(1L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(france.getId() + "", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        Assert.assertEquals("61", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_BALANCE));
        Assert.assertEquals(61, franceSheet.getRtBalance().intValue());
        Assert.assertEquals("56", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_AFTER_EXCHEQUER));
        Assert.assertEquals(56, franceSheet.getRtAftExch().intValue());
        Assert.assertEquals("8", getAttribute(diff, DiffAttributeTypeEnum.PRESTIGE_VPS));
        Assert.assertEquals(8, franceSheet.getPrestigeVP().intValue());
        Assert.assertEquals("408", getAttribute(diff, DiffAttributeTypeEnum.WEALTH));
        Assert.assertEquals(408, franceSheet.getWealth().intValue());
        Assert.assertEquals("408", getAttribute(diff, DiffAttributeTypeEnum.PERIOD_WEALTH));
        Assert.assertEquals(408, franceSheet.getPeriodWealth().intValue());
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(2L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(spain.getId() + "", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_BALANCE));
        Assert.assertEquals(0, spainSheet.getRtBalance().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_AFTER_EXCHEQUER));
        Assert.assertEquals(0, spainSheet.getRtAftExch().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.PRESTIGE_VPS));
        Assert.assertEquals(0, spainSheet.getPrestigeVP().intValue());
        Assert.assertEquals("100", getAttribute(diff, DiffAttributeTypeEnum.WEALTH));
        Assert.assertEquals(100, spainSheet.getWealth().intValue());
        Assert.assertEquals("100", getAttribute(diff, DiffAttributeTypeEnum.PERIOD_WEALTH));
        Assert.assertEquals(100, spainSheet.getPeriodWealth().intValue());
    }

    @Test
    public void testEndExchequerExistingPeriod() {
        GameEntity game = new GameEntity();
        game.setTurn(10);
        game.setStatus(GameStatusEnum.EXCHEQUER);
        PlayableCountryEntity france = new PlayableCountryEntity();
        france.setId(1L);
        france.setName("france");
        france.setReady(true);
        game.getCountries().add(france);
        PlayableCountryEntity turkey = new PlayableCountryEntity();
        turkey.setId(2L);
        turkey.setName("turkey");
        turkey.setUsername("sato");
        turkey.setReady(true);
        game.getCountries().add(turkey);
        PlayableCountryEntity spain = new PlayableCountryEntity();
        spain.setId(3L);
        spain.setName(PlayableCountry.SPAIN);
        spain.setReady(true);
        game.getCountries().add(spain);
        PlayableCountryEntity poland = new PlayableCountryEntity();
        poland.setId(4L);
        poland.setName("poland");
        poland.setReady(true);
        game.getCountries().add(poland);
        PlayableCountryEntity england = new PlayableCountryEntity();
        england.setId(5L);
        england.setName("england");
        england.setReady(true);
        game.getCountries().add(england);

        EconomicalSheetEntity franceSheet = new EconomicalSheetEntity();
        franceSheet.setId(1L);
        franceSheet.setTurn(game.getTurn());
        franceSheet.setPrestigeSpent(16);
        franceSheet.setNatLoan(10);
        franceSheet.setInterLoan(25);
        franceSheet.setRemainingExpenses(-10);
        franceSheet.setRtBefExch(-5);
        franceSheet.setPrestigeIncome(24);
        franceSheet.setGrossIncome(400);
        france.getEconomicalSheets().add(franceSheet);
        EconomicalSheetEntity previous = new EconomicalSheetEntity();
        previous.setId(11L);
        previous.setTurn(game.getTurn() - 1);
        previous.setPeriodWealth(500);
        france.getEconomicalSheets().add(previous);

        EconomicalSheetEntity spainSheet = new EconomicalSheetEntity();
        spainSheet.setId(2L);
        spainSheet.setTurn(game.getTurn());
        spainSheet.setRemainingExpenses(0);
        spainSheet.setGrossIncome(100);
        spain.getEconomicalSheets().add(spainSheet);
        previous = new EconomicalSheetEntity();
        previous.setId(21L);
        previous.setTurn(game.getTurn() - 2);
        previous.setPeriodWealth(500);
        spain.getEconomicalSheets().add(previous);

        turkey.getEconomicalSheets().add(new EconomicalSheetEntity());

        EconomicalSheetEntity polandSheet = new EconomicalSheetEntity();
        polandSheet.setId(3L);
        polandSheet.setTurn(game.getTurn());
        polandSheet.setRemainingExpenses(0);
        polandSheet.setGrossIncome(100);
        poland.getEconomicalSheets().add(polandSheet);

        AbstractBack.TABLES = new Tables();
        Period period = new Period();
        period.setBegin(7);
        period.setEnd(15);
        period.setName(Period.PERIOD_II);
        AbstractBack.TABLES.getPeriods().add(period);

        when(oeUtil.getAdministrativeValue(france)).thenReturn(9);
        when(oeUtil.getAdministrativeValue(spain)).thenReturn(6);
        when(oeUtil.getAdministrativeValue(poland)).thenReturn(3);
        when(oeUtil.getEnemies(france, game)).thenReturn(Collections.emptyList());
        when(oeUtil.getEnemies(spain, game)).thenReturn(Arrays.asList("hanover", "england"));
        when(oeUtil.getEnemies(poland, game)).thenReturn(Arrays.asList("crimea", "turkey"));
        when(counterDao.getNationalTerritoriesUnderAttack(eq(france.getName()), anyList(), anyLong())).thenReturn(Arrays.asList("idf", "orleanais"));
        when(counterDao.getNationalTerritoriesUnderAttack(eq(spain.getName()), anyList(), anyLong())).thenReturn(Collections.singletonList("catalunya"));
        when(counterDao.getNationalTerritoriesUnderAttack(eq(poland.getName()), anyList(), anyLong())).thenReturn(Arrays.asList("mazovia", "smolensk"));
        when(oeUtil.getProsperity(france, game)).thenReturn(1);
        when(oeUtil.getProsperity(spain, game)).thenReturn(0);
        when(oeUtil.getProsperity(poland, game)).thenReturn(-1);

        List<DiffEntity> diffs = statusWorkflowDomain.endExchequerPhase(game);

        Assert.assertEquals(GameStatusEnum.STABILITY, game.getStatus());
        Assert.assertEquals(4, diffs.size());
        DiffEntity diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                .findAny()
                .orElse(null);
        Assert.assertEquals(GameStatusEnum.STABILITY.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));
        Assert.assertEquals("false", getAttribute(diff, DiffAttributeTypeEnum.ACTIVE));
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(1L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(france.getId() + "", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        Assert.assertEquals("61", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_BALANCE));
        Assert.assertEquals(61, franceSheet.getRtBalance().intValue());
        Assert.assertEquals("56", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_AFTER_EXCHEQUER));
        Assert.assertEquals(56, franceSheet.getRtAftExch().intValue());
        Assert.assertEquals("8", getAttribute(diff, DiffAttributeTypeEnum.PRESTIGE_VPS));
        Assert.assertEquals(8, franceSheet.getPrestigeVP().intValue());
        Assert.assertEquals("408", getAttribute(diff, DiffAttributeTypeEnum.WEALTH));
        Assert.assertEquals(408, franceSheet.getWealth().intValue());
        Assert.assertEquals("908", getAttribute(diff, DiffAttributeTypeEnum.PERIOD_WEALTH));
        Assert.assertEquals(908, franceSheet.getPeriodWealth().intValue());
        Assert.assertEquals("12", getAttribute(diff, DiffAttributeTypeEnum.STAB_MODIFIER));
        Assert.assertEquals(12, franceSheet.getStabModifier().intValue());
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(2L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(spain.getId() + "", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_BALANCE));
        Assert.assertEquals(0, spainSheet.getRtBalance().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_AFTER_EXCHEQUER));
        Assert.assertEquals(0, spainSheet.getRtAftExch().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.PRESTIGE_VPS));
        Assert.assertEquals(0, spainSheet.getPrestigeVP().intValue());
        Assert.assertEquals("100", getAttribute(diff, DiffAttributeTypeEnum.WEALTH));
        Assert.assertEquals(100, spainSheet.getWealth().intValue());
        Assert.assertEquals("100", getAttribute(diff, DiffAttributeTypeEnum.PERIOD_WEALTH));
        Assert.assertEquals(100, spainSheet.getPeriodWealth().intValue());
        Assert.assertEquals("1", getAttribute(diff, DiffAttributeTypeEnum.STAB_MODIFIER));
        Assert.assertEquals(1, spainSheet.getStabModifier().intValue());
        diff = diffs.stream()
                .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                        && Objects.equals(3L, d.getIdObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(diff);
        Assert.assertEquals(poland.getId() + "", getAttribute(diff, DiffAttributeTypeEnum.ID_COUNTRY));
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_BALANCE));
        Assert.assertEquals(0, polandSheet.getRtBalance().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.ROYAL_TREASURE_AFTER_EXCHEQUER));
        Assert.assertEquals(0, polandSheet.getRtAftExch().intValue());
        Assert.assertEquals("0", getAttribute(diff, DiffAttributeTypeEnum.PRESTIGE_VPS));
        Assert.assertEquals(0, polandSheet.getPrestigeVP().intValue());
        Assert.assertEquals("100", getAttribute(diff, DiffAttributeTypeEnum.WEALTH));
        Assert.assertEquals(100, polandSheet.getWealth().intValue());
        Assert.assertEquals("100", getAttribute(diff, DiffAttributeTypeEnum.PERIOD_WEALTH));
        Assert.assertEquals(100, polandSheet.getPeriodWealth().intValue());
        Assert.assertEquals("-8", getAttribute(diff, DiffAttributeTypeEnum.STAB_MODIFIER));
        Assert.assertEquals(-8, polandSheet.getStabModifier().intValue());
    }

    @Test
    public void testEndStabilityPhase() {
        EndStabilityBuilder.create()
                .inflationDie(6).exploitedGold(99)
                .whenEndStability(this, statusWorkflowDomain)
                .thenExpect(false);
        EndStabilityBuilder.create()
                .inflationDie(7)
                .whenEndStability(this, statusWorkflowDomain)
                .thenExpect(true);
        EndStabilityBuilder.create()
                .inflationDie(7).inflationMax()
                .whenEndStability(this, statusWorkflowDomain)
                .thenExpect(false);

        EndStabilityBuilder.create()
                .inflationDie(2).exploitedGold(100)
                .whenEndStability(this, statusWorkflowDomain)
                .thenExpect(false);
        EndStabilityBuilder.create()
                .inflationDie(3).exploitedGold(100)
                .whenEndStability(this, statusWorkflowDomain)
                .thenExpect(true);
        EndStabilityBuilder.create()
                .inflationDie(3).exploitedGold(100).inflationMax()
                .whenEndStability(this, statusWorkflowDomain)
                .thenExpect(false);

        EndStabilityBuilder.create()
                .inflationDie(6)
                .addCountry(EndStabilityCountryBuilder.create().id(1L).rtAfterExch(100).stabExpense(50).exploitedGold(20).minInflation(5))
                .addCountry(EndStabilityCountryBuilder.create().id(2L).rtAfterExch(100).stabExpense(50).exploitedGold(20).minInflation(10))
                .addCountry(EndStabilityCountryBuilder.create().id(3L).rtAfterExch(200).stabExpense(50).exploitedGold(20).minInflation(10))
                .addCountry(EndStabilityCountryBuilder.create().id(4L).rtAfterExch(-200).stabExpense(50).exploitedGold(20).minInflation(10))
                .addCountry(EndStabilityCountryBuilder.create().id(5L).rtAfterExch(-200).stabExpense(50).minInflation(10))
                .addCountry(EndStabilityCountryBuilder.create().id(6L).noSheet())
                .whenEndStability(this, statusWorkflowDomain)
                .thenExpect(false, EndStabilityResultBuilder.create().id(1L).inflation(5).rtPeace(50).rtEnd(45),
                        EndStabilityResultBuilder.create().id(2L).inflation(10).rtPeace(50).rtEnd(40),
                        EndStabilityResultBuilder.create().id(3L).inflation(15).rtPeace(150).rtEnd(135),
                        EndStabilityResultBuilder.create().id(4L).inflation(25).rtPeace(-250).rtEnd(-275),
                        EndStabilityResultBuilder.create().id(5L).inflation(13).rtPeace(-250).rtEnd(-263),
                        EndStabilityResultBuilder.create().id(6L).noSheet());
    }

    static class EndStabilityBuilder {
        int exploitedGold;
        int inflationDie;
        boolean inflationMax;
        List<EndStabilityCountryBuilder> countries = new ArrayList<>();
        GameEntity game;
        List<DiffEntity> diffs;

        static EndStabilityBuilder create() {
            return new EndStabilityBuilder();
        }

        EndStabilityBuilder exploitedGold(int exploitedGold) {
            this.exploitedGold = exploitedGold;
            return this;
        }

        EndStabilityBuilder inflationDie(int inflationDie) {
            this.inflationDie = inflationDie;
            return this;
        }

        EndStabilityBuilder inflationMax() {
            this.inflationMax = true;
            return this;
        }

        EndStabilityBuilder addCountry(EndStabilityCountryBuilder country) {
            this.countries.add(country);
            return this;
        }

        EndStabilityBuilder whenEndStability(StatusWorkflowDomainTest testClass, IStatusWorkflowDomain statusWorkflowDomain) {
            game = new GameEntity();
            game.setId(5L);
            game.setTurn(10);

            when(testClass.counterDao.getGoldExploitedRotw(game.getId())).thenReturn(exploitedGold);
            when(testClass.oeUtil.rollDie(game)).thenReturn(inflationDie);
            if (inflationMax) {
                when(testClass.counterDomain.increaseInflation(game)).thenReturn(Optional.empty());
            } else {
                when(testClass.counterDomain.increaseInflation(game)).thenReturn(Optional.of(DiffUtil.createDiff(game, DiffTypeEnum.MOVE, DiffTypeObjectEnum.COUNTER, 666L)));
            }

            for (EndStabilityCountryBuilder countryBuilder : countries) {
                PlayableCountryEntity country = new PlayableCountryEntity();
                country.setId(countryBuilder.id);
                country.setName(country.getId() + "");
                game.getCountries().add(country);
                EconomicalSheetEntity sheet = new EconomicalSheetEntity();
                sheet.setCountry(country);
                sheet.setTurn(9);
                country.getEconomicalSheets().add(sheet);
                if (!countryBuilder.noSheet) {
                    sheet = new EconomicalSheetEntity();
                    sheet.setId(country.getId());
                    sheet.setCountry(country);
                    sheet.setTurn(10);
                    sheet.setRtAftExch(countryBuilder.rtAfterExch);
                    sheet.setStab(countryBuilder.stabExpense);
                    country.getEconomicalSheets().add(sheet);
                }

                when(testClass.counterDao.getGoldExploitedAmerica(country.getName(), game.getId())).thenReturn(countryBuilder.exploitedGold);
                when(testClass.oeUtil.getMinimalInflation(10, country.getName(), AbstractBack.TABLES, game)).thenReturn(countryBuilder.minInflation);
                when(testClass.oeUtil.getInflationBox(game)).thenReturn("B_PB_1D");
            }
            when(testClass.counterDomain.moveSpecialCounter(CounterFaceTypeEnum.TURN, null, GameUtil.getTurnBox(11), game)).thenReturn(DiffUtil.createDiff(game, DiffTypeEnum.MOVE, DiffTypeObjectEnum.COUNTER, 667L));

            diffs = statusWorkflowDomain.endStabilityPhase(game);

            return this;
        }

        EndStabilityBuilder thenExpect(boolean increaseInflation, EndStabilityResultBuilder... results) {
            int nbDiffs = 3;
            DiffEntity diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull("The modify game diff event was not created.", diff);
            Assert.assertEquals("The new status of the game is wrong.", GameStatusEnum.ADMINISTRATIVE_ACTIONS_CHOICE.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));
            Assert.assertEquals("The new turn of the game is wrong.", "11", getAttribute(diff, DiffAttributeTypeEnum.TURN));
            Assert.assertEquals("The deactivation attribute was not sent.", "false", getAttribute(diff, DiffAttributeTypeEnum.ACTIVE));
            diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.INVALIDATE && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET)
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull("The invalidate eco sheet diff event was not created.", diff);
            Assert.assertEquals("The new turn of the eco sheet is wrong.", "11", getAttribute(diff, DiffAttributeTypeEnum.TURN));
            diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                            Objects.equals(d.getIdObject(), 667L))
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull("The move turn counter diff event was not created.", diff);

            diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                            Objects.equals(d.getIdObject(), 666L))
                    .findAny()
                    .orElse(null);
            if (increaseInflation) {
                Assert.assertNotNull("The increase inflation diff event is missing.", diff);
                nbDiffs++;
            } else {
                Assert.assertNull("The increase inflation diff event should not be sent.", diff);
            }

            for (EndStabilityResultBuilder result : results) {
                DiffEntity modifySheetDiff = diffs.stream()
                        .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET
                                && Objects.equals(d.getIdObject(), result.id))
                        .findAny()
                        .orElse(null);
                DiffEntity createSheetDiff = diffs.stream()
                        .filter(d -> d.getType() == DiffTypeEnum.ADD && d.getTypeObject() == DiffTypeObjectEnum.ECO_SHEET)
                        .flatMap(d -> d.getAttributes().stream())
                        .filter(attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY && Objects.equals(attr.getValue(), result.id + ""))
                        .map(DiffAttributesEntity::getDiff)
                        .findAny()
                        .orElse(null);
                if (result.noSheet) {
                    Assert.assertNull("A country without sheet should not trigger any sheet event.", modifySheetDiff);
                    Assert.assertNull("A country without sheet should not trigger any sheet event.", createSheetDiff);
                } else {
                    EconomicalSheetEntity sheet = game.getCountries().stream()
                            .filter(country -> Objects.equals(country.getId(), result.id))
                            .flatMap(country -> country.getEconomicalSheets().stream())
                            .filter(es -> Objects.equals(es.getTurn(), 10))
                            .findAny()
                            .orElse(null);
                    Assert.assertNotNull("The modify eco sheet diff event is missing.", modifySheetDiff);
                    Assert.assertEquals("The id country attribute in the modify eco sheet is wrong.", result.id + "", getAttribute(modifySheetDiff, DiffAttributeTypeEnum.ID_COUNTRY));
                    Assert.assertEquals("The RT peace attribute in the modify eco sheet is wrong.", result.rtPeace + "", getAttribute(modifySheetDiff, DiffAttributeTypeEnum.ROYAL_TREASURE_PEACE));
                    Assert.assertEquals("The RT peace in the eco sheet is wrong.", result.rtPeace, sheet.getRtPeace());
                    Assert.assertEquals("The inflation attribute in the modify eco sheet is wrong.", result.inflation + "", getAttribute(modifySheetDiff, DiffAttributeTypeEnum.INFLATION));
                    Assert.assertEquals("The inflation in the eco sheet is wrong.", result.inflation, sheet.getInflation());
                    Assert.assertEquals("The RT end attribute in the modify eco sheet is wrong.", result.rtEnd + "", getAttribute(modifySheetDiff, DiffAttributeTypeEnum.ROYAL_TREASURE_END));
                    Assert.assertEquals("The RT end in the eco sheet is wrong.", result.rtEnd, sheet.getRtEnd());

                    EconomicalSheetEntity newSheet = game.getCountries().stream()
                            .filter(country -> Objects.equals(country.getId(), result.id))
                            .flatMap(country -> country.getEconomicalSheets().stream())
                            .filter(es -> Objects.equals(es.getTurn(), 11))
                            .findAny()
                            .orElse(null);
                    Assert.assertNotNull("The add eco sheet diff event is missing.", createSheetDiff);
                    Assert.assertEquals("The turn attribute in the add eco sheet is wrong.", "11", getAttribute(createSheetDiff, DiffAttributeTypeEnum.TURN));
                    Assert.assertNotNull("The turn in the new eco sheet is wrong.", newSheet);
                    Assert.assertEquals("The RT start attribute in the add eco sheet is wrong.", result.rtEnd + "", getAttribute(createSheetDiff, DiffAttributeTypeEnum.ROYAL_TREASURE_START));
                    Assert.assertEquals("The RT start in the new eco sheet is wrong.", result.rtEnd, newSheet.getRtStart());

                    nbDiffs += 2;
                }
            }

            Assert.assertEquals("Number of diffs returned is wrong.", nbDiffs, diffs.size());

            return this;
        }
    }

    static class EndStabilityCountryBuilder {
        Long id;
        boolean noSheet;
        Integer rtAfterExch;
        Integer stabExpense;
        int exploitedGold;
        int minInflation;

        static EndStabilityCountryBuilder create() {
            return new EndStabilityCountryBuilder();
        }

        EndStabilityCountryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        EndStabilityCountryBuilder noSheet() {
            this.noSheet = true;
            return this;
        }

        EndStabilityCountryBuilder rtAfterExch(Integer rtAfterExch) {
            this.rtAfterExch = rtAfterExch;
            return this;
        }

        EndStabilityCountryBuilder stabExpense(Integer stabExpense) {
            this.stabExpense = stabExpense;
            return this;
        }

        EndStabilityCountryBuilder exploitedGold(int exploitedGold) {
            this.exploitedGold = exploitedGold;
            return this;
        }

        EndStabilityCountryBuilder minInflation(int minInflation) {
            this.minInflation = minInflation;
            return this;
        }
    }

    static class EndStabilityResultBuilder {
        Long id;
        boolean noSheet;
        Integer rtPeace;
        Integer inflation;
        Integer rtEnd;

        static EndStabilityResultBuilder create() {
            return new EndStabilityResultBuilder();
        }

        EndStabilityResultBuilder id(Long id) {
            this.id = id;
            return this;
        }

        EndStabilityResultBuilder noSheet() {
            this.noSheet = true;
            return this;
        }

        EndStabilityResultBuilder rtPeace(Integer rtPeace) {
            this.rtPeace = rtPeace;
            return this;
        }

        EndStabilityResultBuilder inflation(Integer inflation) {
            this.inflation = inflation;
            return this;
        }

        EndStabilityResultBuilder rtEnd(Integer rtEnd) {
            this.rtEnd = rtEnd;
            return this;
        }
    }

    @Test
    public void testComputeSheet1() {
        testComputeSheet("france", 200, true);
    }

    @Test
    public void testComputeSheet2() {
        testComputeSheet("angleterre", 50, false);
    }

    private void testComputeSheet(String name, long tradeIncome, boolean createSheet) {
        GameEntity game = new GameEntity();
        game.setId(12L);
        game.setTurn(2);
        PlayableCountryEntity country = new PlayableCountryEntity();
        country.setName(name);
        country.setUsername(name);
        country.setDti(3);
        country.setFti(2);
        country.getEconomicalSheets().add(new EconomicalSheetEntity());
        country.getEconomicalSheets().get(0).setTurn(0);
        country.getEconomicalSheets().add(new EconomicalSheetEntity());
        country.getEconomicalSheets().get(1).setTurn(1);
        if (!createSheet) {
            sheetEntity = new EconomicalSheetEntity();
            sheetEntity.setTurn(2);
            sheetEntity.setTradeCenterLoss(13);
            sheetEntity.setCountry(country);

            country.getEconomicalSheets().add(sheetEntity);
        }
        game.getCountries().add(country);
        Map<String, List<CounterFaceTypeEnum>> centers = new HashMap<>();
        centers.put("france", new ArrayList<>());
        centers.get("france").add(CounterFaceTypeEnum.TRADE_CENTER_MEDITERRANEAN);
        centers.get("france").add(CounterFaceTypeEnum.TRADE_CENTER_ATLANTIC);
        centers.put("angleterre", new ArrayList<>());
        centers.get("angleterre").add(CounterFaceTypeEnum.TRADE_CENTER_INDIAN);
        when(economicalSheetDao.getTradeCenters(game.getId())).thenReturn(centers);

        if (createSheet) {
            when(economicalSheetDao.create(anyObject())).thenAnswer(invocation -> {
                sheetEntity = (EconomicalSheetEntity) invocation.getArguments()[0];
                return sheetEntity;
            });
        }
        Map<String, Integer> provinces = new HashMap<>();
        provinces.put("idf", 12);
        provinces.put("lyonnais", 5);
        provinces.put("languedoc", 8);
        when(economicalSheetDao.getOwnedAndControlledProvinces(name, game.getId())).thenReturn(provinces);
        List<String> vassals = new ArrayList<>();
        vassals.add("sabaudia");
        vassals.add("alsacia");
        when(counterDao.getVassals(name, game.getId())).thenReturn(vassals);
        Map<String, Integer> provincesAlsacia = new HashMap<>();
        provincesAlsacia.put("alsacia", 9);
        when(economicalSheetDao.getOwnedAndControlledProvinces("alsacia", game.getId())).thenReturn(provincesAlsacia);
        Map<String, Integer> provincesSabaudia = new HashMap<>();
        provincesSabaudia.put("bresse", 4);
        provincesSabaudia.put("nice", 8);
        when(economicalSheetDao.getOwnedAndControlledProvinces("sabaudia", game.getId())).thenReturn(provincesSabaudia);
        List<String> provinceNames = new ArrayList<>();
        provinceNames.add("idf");
        provinceNames.add("lyonnais");
        provinceNames.add("languedoc");
        provinceNames.add("bresse");
        provinceNames.add("nice");
        provinceNames.add("alsacia");
        List<String> pillagedProvinces = new ArrayList<>();
        pillagedProvinces.add("lyonnais");
        pillagedProvinces.add("nice");
        when(economicalSheetDao.getPillagedProvinces(argThat(new ListEquals<>(provinceNames)), eq(game.getId()))).thenReturn(pillagedProvinces);
        when(economicalSheetDao.getMnuIncome(name, pillagedProvinces, game.getId())).thenReturn(60);
        List<String> provincesOwnedNotPilaged = new ArrayList<>();
        provincesOwnedNotPilaged.add("idf");
        provincesOwnedNotPilaged.add("languedoc");
        when(economicalSheetDao.getGoldIncome(argThat(new ListEquals<>(provincesOwnedNotPilaged)), eq(game.getId()))).thenReturn(
                20);
        when(economicalSheetDao.getFleetLevelIncome(name, game.getId())).thenReturn(30);
        when(economicalSheetDao.getFleetLevelMonopoly(name, game.getId())).thenReturn(12);
        when(economicalSheetDao.getColTpIncome(name, game.getId())).thenReturn(new ImmutablePair<>(22, 18));
        when(economicalSheetDao.getExoResIncome(name, game.getId())).thenReturn(8);

        Tables tables = new Tables();
        List<TradeIncome> domTrades = new ArrayList<>();
        TradeIncome trade = new TradeIncome();
        trade.setCountryValue(2);
        trade.setMinValue(40);
        trade.setMaxValue(80);
        trade.setValue(6);
        domTrades.add(trade);
        trade = new TradeIncome();
        trade.setCountryValue(3);
        trade.setMaxValue(39);
        trade.setValue(13);
        domTrades.add(trade);
        trade = new TradeIncome();
        trade.setCountryValue(3);
        trade.setMinValue(40);
        trade.setMaxValue(79);
        trade.setValue(16);
        domTrades.add(trade);
        trade = new TradeIncome();
        trade.setCountryValue(3);
        trade.setMinValue(80);
        trade.setValue(18);
        domTrades.add(trade);
        tables.getDomesticTrades().addAll(domTrades);
        List<TradeIncome> forTrades = new ArrayList<>();
        trade = new TradeIncome();
        trade.setCountryValue(1);
        trade.setMaxValue(49);
        trade.setValue(5);
        forTrades.add(trade);
        trade = new TradeIncome();
        trade.setCountryValue(2);
        trade.setMaxValue(49);
        trade.setValue(10);
        forTrades.add(trade);
        trade = new TradeIncome();
        trade.setCountryValue(2);
        trade.setMinValue(50);
        trade.setMaxValue(100);
        trade.setValue(12);
        forTrades.add(trade);
        trade = new TradeIncome();
        trade.setCountryValue(3);
        trade.setMaxValue(49);
        trade.setValue(15);
        forTrades.add(trade);
        tables.getForeignTrades().addAll(forTrades);
        EconomicServiceImpl.TABLES = tables;

        statusWorkflowDomain.computeEconomicalSheets(game);

        InOrder inOrder = inOrder(economicalSheetDao, counterDao);

        if (createSheet) {
            inOrder.verify(economicalSheetDao).create(anyObject());
        }
        inOrder.verify(economicalSheetDao).getOwnedAndControlledProvinces(name, game.getId());
        inOrder.verify(counterDao).getVassals(name, game.getId());
        inOrder.verify(economicalSheetDao).getOwnedAndControlledProvinces("sabaudia", game.getId());
        inOrder.verify(economicalSheetDao).getOwnedAndControlledProvinces("alsacia", game.getId());
        inOrder.verify(economicalSheetDao).getPillagedProvinces(argThat(new ListEquals<>(provinceNames)), eq(game.getId()));
        inOrder.verify(economicalSheetDao).getMnuIncome(name, pillagedProvinces, game.getId());
        inOrder.verify(economicalSheetDao).getGoldIncome(argThat(new ListEquals<>(provincesOwnedNotPilaged)), eq(game.getId()));
        inOrder.verify(economicalSheetDao).getFleetLevelIncome(name, game.getId());
        inOrder.verify(economicalSheetDao).getFleetLevelMonopoly(name, game.getId());
        inOrder.verify(economicalSheetDao).getColTpIncome(name, game.getId());
        inOrder.verify(economicalSheetDao).getExoResIncome(name, game.getId());

        Assert.assertEquals(3, country.getEconomicalSheets().size());
        Assert.assertEquals(sheetEntity, country.getEconomicalSheets().get(2));
        Assert.assertEquals(country, sheetEntity.getCountry());
        Assert.assertEquals(2, sheetEntity.getTurn().intValue());
        Assert.assertEquals(25, sheetEntity.getProvincesIncome().longValue());
        Assert.assertEquals(21, sheetEntity.getVassalIncome().longValue());
        Assert.assertEquals(13, sheetEntity.getLostIncome().longValue());
        Assert.assertEquals(33, sheetEntity.getLandIncome().longValue());
        Assert.assertEquals(60, sheetEntity.getMnuIncome().longValue());
        Assert.assertEquals(20, sheetEntity.getGoldIncome().longValue());
        Assert.assertEquals(80, sheetEntity.getIndustrialIncome().longValue());
        Assert.assertEquals(16, sheetEntity.getDomTradeIncome().longValue());
        Assert.assertEquals(10, sheetEntity.getForTradeIncome().longValue());
        Assert.assertEquals(30, sheetEntity.getFleetLevelIncome().longValue());
        Assert.assertEquals(12, sheetEntity.getFleetMonopIncome().longValue());
        Assert.assertEquals(tradeIncome, sheetEntity.getTradeCenterIncome().longValue());
        if (createSheet) {
            Assert.assertNull(sheetEntity.getTradeCenterLoss());
            Assert.assertEquals(tradeIncome + 68, sheetEntity.getTradeIncome().longValue());
            Assert.assertEquals(tradeIncome + 229, sheetEntity.getIncome().longValue());
            Assert.assertEquals(tradeIncome + 229, sheetEntity.getGrossIncome().longValue());
        } else {
            Assert.assertEquals(13, sheetEntity.getTradeCenterLoss().longValue());
            Assert.assertEquals(tradeIncome + 55, sheetEntity.getTradeIncome().longValue());
            Assert.assertEquals(tradeIncome + 216, sheetEntity.getIncome().longValue());
            Assert.assertEquals(tradeIncome + 216, sheetEntity.getGrossIncome().longValue());
        }
        Assert.assertEquals(22, sheetEntity.getColIncome().longValue());
        Assert.assertEquals(18, sheetEntity.getTpIncome().longValue());
        Assert.assertEquals(8, sheetEntity.getExoResIncome().longValue());
        Assert.assertEquals(48, sheetEntity.getRotwIncome().longValue());
    }

    @Test
    public void testDeployLeaders() {
        // No leader, no limit, nothing to do
        DeployLeadersBuilder.create()
                .whenDeployLeaders(statusWorkflowDomain, this)
                .thenExpect(DeployLeadersResultBuilder.create());

        // France has no leader, but one general in its limit => add one anonymous leader
        // While spain has one anonymous general but no limit => remove one anonymous leader
        DeployLeadersBuilder.create()
                .addActiveCountry("france").addActiveCountry("spain")
                .addExistingLeader(LeaderBuilder.create().id(1L).code("Legion-1").country("spain").type(LeaderTypeEnum.GENERAL).stats("C 333").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Nabo").country("france").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addLimit("france", LimitTypeEnum.LEADER_GENERAL, 1)
                .whenDeployLeaders(statusWorkflowDomain, this)
                .thenExpect(DeployLeadersResultBuilder.create()
                        .removeLeader(1L)
                        .addLeader(LeaderMatch.create().matchCodes("Nabo").country("france")));

        // If France was not an active country, then no anonymous leader for him
        // If spain existing leader was not anonymous, then no removing
        DeployLeadersBuilder.create()
                .addInactiveCountry("france").addActiveCountry("spain")
                .addExistingLeader(LeaderBuilder.create().id(1L).code("Legion-1").country("spain").type(LeaderTypeEnum.GENERAL).stats("C 333"))
                .addTableLeader(LeaderBuilder.create().code("Nabo").country("france").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addLimit("france", LimitTypeEnum.LEADER_GENERAL, 1)
                .whenDeployLeaders(statusWorkflowDomain, this)
                .thenExpect(DeployLeadersResultBuilder.create());

        // France has no admiral and limit is 2 -> 2 of the 3 anonymous admirals are picked
        // England has 3 conquistadores, 2 of them anonymous and limit is 2 -> lowest rank anonymous conquistador is removed
        DeployLeadersBuilder.create()
                .addActiveCountry("france").addActiveCountry("england")
                .addExistingLeader(LeaderBuilder.create().id(1L).code("Legion-1").country("england").type(LeaderTypeEnum.GENERAL).stats("C 333").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(2L).code("Legion-2").country("england").type(LeaderTypeEnum.GENERAL).stats("D 111").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(3L).code("Cornwell").country("england").type(LeaderTypeEnum.GENERAL).stats("E 434"))
                .addTableLeader(LeaderBuilder.create().code("Legion-1").country("france").type(LeaderTypeEnum.ADMIRAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-2").country("france").type(LeaderTypeEnum.ADMIRAL).stats("C 222").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-3").country("france").type(LeaderTypeEnum.ADMIRAL).stats("D 333").anonymous())
                .addLimit("france", LimitTypeEnum.LEADER_ADMIRAL, 2)
                .addLimit("england", LimitTypeEnum.LEADER_GENERAL, 2)
                .whenDeployLeaders(statusWorkflowDomain, this)
                .thenExpect(DeployLeadersResultBuilder.create()
                        .removeLeader(2L)
                        .addLeader(LeaderMatch.create().matchCodes("Legion-1", "Legion-2", "Legion-3").country("france"))
                        .addLeader(LeaderMatch.create().matchCodes("Legion-1", "Legion-2", "Legion-3").country("france")));

        // France has no admiral and limit is 4 -> only 3 anonymous admirals are picked because only 3 different anonymous admirals
        // England has 3 conquistadores, 2 of them anonymous and limit is 2 -> lowest rank anonymous conquistador is removed (rank exchange with previous test)
        DeployLeadersBuilder.create()
                .addActiveCountry("france").addActiveCountry("england")
                .addExistingLeader(LeaderBuilder.create().id(1L).code("Legion-1").country("england").type(LeaderTypeEnum.GENERAL).stats("D 333").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(2L).code("Legion-2").country("england").type(LeaderTypeEnum.GENERAL).stats("C 111").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(3L).code("Cornwell").country("england").type(LeaderTypeEnum.GENERAL).stats("E 434"))
                .addTableLeader(LeaderBuilder.create().code("Legion-1").country("france").type(LeaderTypeEnum.ADMIRAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-2").country("france").type(LeaderTypeEnum.ADMIRAL).stats("C 222").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-3").country("france").type(LeaderTypeEnum.ADMIRAL).stats("D 333").anonymous())
                .addLimit("france", LimitTypeEnum.LEADER_ADMIRAL, 4)
                .addLimit("england", LimitTypeEnum.LEADER_GENERAL, 2)
                .whenDeployLeaders(statusWorkflowDomain, this)
                .thenExpect(DeployLeadersResultBuilder.create()
                        .removeLeader(1L)
                        .addLeader(LeaderMatch.create().matchCodes("Legion-1").country("france"))
                        .addLeader(LeaderMatch.create().matchCodes("Legion-2").country("france"))
                        .addLeader(LeaderMatch.create().matchCodes("Legion-3").country("france")));

        // France has 1 anonymous general, has a pool of 3 anonymous generals and a limit of 3 -> the other 2 generals are picked
        // France has 1 anonymous admiral, and 1 names admiral spawn this turn, with a limit of 1 admiral -> the named admiral is created and the anonymous admiral is removed
        // France has 3 explorers, 2 of them anonymous, with a limit of 0 -> the 2 anonymous explorers are removed
        // England has no general, a pool of 4 anonymous generals and a limit of 1 -> 1 anonymous general is picked
        // Spain has 2 anonymous generals, a pool of 4 anonymous generals and a limit of 2 general and 1 general america -> 1 anonymous general is created
        DeployLeadersBuilder.create()
                .addActiveCountry("france").addActiveCountry("england").addActiveCountry("spain")
                .addExistingLeader(LeaderBuilder.create().id(1L).code("Legion-G1").country("france").type(LeaderTypeEnum.GENERAL).stats("D 333").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(2L).code("Legion-A2").country("france").type(LeaderTypeEnum.ADMIRAL).stats("C 111").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(3L).code("Cabot").country("france").type(LeaderTypeEnum.EXPLORER).stats("E 434"))
                .addExistingLeader(LeaderBuilder.create().id(4L).code("Legion-E1").country("france").type(LeaderTypeEnum.EXPLORER).stats("E 434").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(5L).code("Legion-E2").country("france").type(LeaderTypeEnum.EXPLORER).stats("E 111").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(6L).code("Legion-G1").country("spain").type(LeaderTypeEnum.GENERAL).stats("D 333").anonymous())
                .addExistingLeader(LeaderBuilder.create().id(7L).code("Legion-G2").country("spain").type(LeaderTypeEnum.GENERAL).stats("C 212").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G1").country("france").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G2").country("france").type(LeaderTypeEnum.GENERAL).stats("C 222").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G3").country("france").type(LeaderTypeEnum.GENERAL).stats("D 333").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Turbot").country("france").type(LeaderTypeEnum.ADMIRAL).begin(5).stats("B 512"))
                .addTableLeader(LeaderBuilder.create().code("Legion-G1").country("england").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G2").country("england").type(LeaderTypeEnum.GENERAL).stats("C 222").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G3").country("england").type(LeaderTypeEnum.GENERAL).stats("D 333").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G4").country("england").type(LeaderTypeEnum.GENERAL).stats("C 121").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G1").country("spain").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G2").country("spain").type(LeaderTypeEnum.GENERAL).stats("C 222").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G3").country("spain").type(LeaderTypeEnum.GENERAL).stats("D 333").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G4").country("spain").type(LeaderTypeEnum.GENERAL).stats("C 121").anonymous())
                .addLimit("france", LimitTypeEnum.LEADER_GENERAL, 3)
                .addLimit("france", LimitTypeEnum.LEADER_ADMIRAL, 1)
                .addLimit("england", LimitTypeEnum.LEADER_GENERAL, 1)
                .addLimit("spain", LimitTypeEnum.LEADER_GENERAL, 2)
                .addLimit("spain", LimitTypeEnum.LEADER_GENERAL_AMERICA, 1)
                .whenDeployLeaders(statusWorkflowDomain, this)
                .thenExpect(DeployLeadersResultBuilder.create()
                        .addLeader(LeaderMatch.create().matchCodes("Legion-G2").country("france"))
                        .addLeader(LeaderMatch.create().matchCodes("Legion-G3").country("france"))
                        .addLeader(LeaderMatch.create().matchCodes("Turbot").country("france"))
                        .removeLeader(2L)
                        .removeLeader(4L)
                        .removeLeader(5L)
                        .addLeader(LeaderMatch.create().matchCodes("Legion-G1", "Legion-G2", "Legion-G3", "Legion-G4").country("england"))
                        .addLeader(LeaderMatch.create().matchCodes("Legion-G3", "Legion-G4").country("spain")));

        // France has 1 general that will arrive this turn, a pool of 1 general and a limit of 1 general => the named general is picked
        // Spain has 1 general that will arrive next turn, a pool of 1 general and a limit of 1 general => the anonymous general is picked
        // England has 1 general that will arrive this turn, but already on map, a pool of 1 general and a limit of 1 general => no leader is picked
        // Poland has 1 general on map and 1 general wounded, a pool of 2 general and a limit of 2 general => an anonymous general is picked
        DeployLeadersBuilder.create()
                .addActiveCountry("france").addActiveCountry("spain").addActiveCountry("england").addActiveCountry("poland")
                .addExistingLeader(LeaderBuilder.create().id(1L).code("Malborough").country("england").type(LeaderTypeEnum.GENERAL).stats("A 666"))
                .addExistingLeader(LeaderBuilder.create().id(2L).code("Sovietzky").country("poland").type(LeaderTypeEnum.GENERAL).stats("B 454"))
                .addWoundedLeader(LeaderBuilder.create().id(3L).code("Kolkozky").country("poland").type(LeaderTypeEnum.GENERAL).stats("C 322"))
                .addTableLeader(LeaderBuilder.create().code("Bayard").country("france").type(LeaderTypeEnum.GENERAL).stats("A 116").begin(5))
                .addTableLeader(LeaderBuilder.create().code("Infante").country("spain").type(LeaderTypeEnum.GENERAL).stats("B 435").begin(6))
                .addTableLeader(LeaderBuilder.create().code("Malborough").country("england").type(LeaderTypeEnum.GENERAL).stats("A 666").begin(5))
                .addTableLeader(LeaderBuilder.create().code("Legion-G1").country("france").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G1").country("spain").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G1").country("england").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G1").country("poland").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addTableLeader(LeaderBuilder.create().code("Legion-G2").country("poland").type(LeaderTypeEnum.GENERAL).stats("E 111").anonymous())
                .addLimit("france", LimitTypeEnum.LEADER_GENERAL, 1)
                .addLimit("spain", LimitTypeEnum.LEADER_GENERAL, 1)
                .addLimit("england", LimitTypeEnum.LEADER_GENERAL, 1)
                .addLimit("poland", LimitTypeEnum.LEADER_GENERAL, 2)
                .whenDeployLeaders(statusWorkflowDomain, this)
                .thenExpect(DeployLeadersResultBuilder.create()
                        .addLeader(LeaderMatch.create().matchCodes("Bayard").country("france"))
                        .addLeader(LeaderMatch.create().matchCodes("Legion-G1").country("spain"))
                        .addLeader(LeaderMatch.create().matchCodes("Legion-G1", "Legion-G2").country("poland")));
    }

    static class DeployLeadersBuilder {
        List<PlayableCountryEntity> countries = new ArrayList<>();
        List<LeaderBuilder> existingLeaders = new ArrayList<>();
        List<LeaderBuilder> woundedLeaders = new ArrayList<>();
        List<LeaderBuilder> tableLeaders = new ArrayList<>();
        List<Limit> limits = new ArrayList<>();
        List<DiffEntity> diffs;

        static DeployLeadersBuilder create() {
            return new DeployLeadersBuilder();
        }

        DeployLeadersBuilder addActiveCountry(String name) {
            PlayableCountryEntity country = new PlayableCountryEntity();
            country.setName(name);
            country.setUsername(name);
            countries.add(country);
            return this;
        }

        DeployLeadersBuilder addInactiveCountry(String name) {
            PlayableCountryEntity country = new PlayableCountryEntity();
            country.setName(name);
            countries.add(country);
            return this;
        }

        DeployLeadersBuilder addExistingLeader(LeaderBuilder leader) {
            this.existingLeaders.add(leader);
            return this;
        }

        DeployLeadersBuilder addWoundedLeader(LeaderBuilder leader) {
            this.woundedLeaders.add(leader);
            return this;
        }

        DeployLeadersBuilder addTableLeader(LeaderBuilder leader) {
            this.tableLeaders.add(leader);
            return this;
        }

        DeployLeadersBuilder addLimit(String country, LimitTypeEnum type, Integer number) {
            Limit limit = new Limit();
            limit.setCountry(country);
            limit.setType(type);
            limit.setNumber(number);
            limits.add(limit);
            return this;
        }

        DeployLeadersBuilder whenDeployLeaders(StatusWorkflowDomainImpl statusWorkflowDomain, StatusWorkflowDomainTest testClass) {
            GameEntity game = new GameEntity();
            Tables tables = new Tables();
            AbstractBack.TABLES = tables;
            game.setTurn(5);
            game.getCountries().addAll(countries);

            Period period = new Period();
            period.setBegin(1);
            period.setEnd(6);
            tables.getPeriods().add(period);
            limits.forEach(limit -> limit.setPeriod(period));
            tables.getLimits().addAll(limits);

            StackEntity stack = new StackEntity();
            stack.setId(1L);
            stack.setGame(game);
            stack.setProvince("idf");
            game.getStacks().add(stack);
            existingLeaders.forEach(leader -> stack.getCounters().add(createLeader(leader, tables, stack)));

            StackEntity roundStack = new StackEntity();
            roundStack.setId(2L);
            roundStack.setGame(game);
            roundStack.setProvince("B_MR_S2");
            game.getStacks().add(roundStack);
            woundedLeaders.forEach(leader -> roundStack.getCounters().add(createLeader(leader, tables, roundStack)));

            tableLeaders.forEach(leader -> createLeaderTable(leader, tables));

            when(testClass.counterDomain.createLeader(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
                CounterEntity counter = createCounter(666L, invocation.getArgumentAt(2, String.class), CounterFaceTypeEnum.LEADER);
                counter.setCode(invocation.getArgumentAt(1, String.class));
                stack.getCounters().add(counter);

                return createLeaderAnswer().answer(invocation);
            });
            when(testClass.counterDomain.removeCounter(any())).thenAnswer(removeCounterAnswer());
            when(testClass.oeUtil.rollDie(any(), (String) any(), anyInt())).thenReturn(1);

            diffs = statusWorkflowDomain.deployLeaders(game);

            return this;
        }

        DeployLeadersBuilder thenExpect(DeployLeadersResultBuilder result) {
            int nbDiff = 0;
            for (Long id : result.removedLeaders) {
                DiffEntity diff = diffs.stream()
                        .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                                Objects.equals(d.getIdObject(), id))
                        .findAny()
                        .orElse(null);
                Assert.assertNotNull("The remove counter " + id + " was not sent.", diff);
                nbDiff++;
            }

            for (LeaderMatch leaderMatch : result.addedLeaders) {
                DiffEntity diff = diffs.stream()
                        .filter(d -> d.getType() == DiffTypeEnum.ADD && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                                StringUtils.equals(leaderMatch.country, getAttribute(d, DiffAttributeTypeEnum.COUNTRY)) &&
                                leaderMatch.codes.contains(getAttribute(d, DiffAttributeTypeEnum.CODE)))
                        .findAny()
                        .orElse(null);
                Assert.assertNotNull("The add counter for codes " + leaderMatch.codes + " and country " + leaderMatch.country + " was not sent.", diff);
                Assert.assertEquals("The add counter for codes " + leaderMatch.codes + " and country " + leaderMatch.country + " has the wrong province attribute.",
                        GameUtil.getTurnBox(5), getAttribute(diff, DiffAttributeTypeEnum.PROVINCE));
                nbDiff++;
            }

            Assert.assertEquals("The number of diffs sent is wrong.", nbDiff, diffs.size());

            return this;
        }
    }

    static class DeployLeadersResultBuilder {
        List<Long> removedLeaders = new ArrayList<>();
        List<LeaderMatch> addedLeaders = new ArrayList<>();

        static DeployLeadersResultBuilder create() {
            return new DeployLeadersResultBuilder();
        }

        DeployLeadersResultBuilder removeLeader(Long id) {
            removedLeaders.add(id);
            return this;
        }

        DeployLeadersResultBuilder addLeader(LeaderMatch leader) {
            addedLeaders.add(leader);
            return this;
        }
    }

    static class LeaderMatch {
        List<String> codes = new ArrayList<>();
        String country;

        static LeaderMatch create() {
            return new LeaderMatch();
        }

        LeaderMatch matchCodes(String... codes) {
            this.codes.addAll(Arrays.asList(codes));
            return this;
        }

        LeaderMatch country(String country) {
            this.country = country;
            return this;
        }
    }

    @Test
    public void testInitRound() {
        InitRoundBuilder.create()
                .whenInitRound(statusWorkflowDomain, this)
                .thenExpect(InitRoundResultBuilder.create().status(GameStatusEnum.MILITARY_MOVE));

        // Leader wounded until W3 but we go in S3, no leader return
        InitRoundBuilder.create().nextRound("S3")
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france"))
                .whenInitRound(statusWorkflowDomain, this)
                .thenExpect(InitRoundResultBuilder.create().status(GameStatusEnum.MILITARY_MOVE));

        // Leader wounded until W3 and we go in W3, leader returns but since france is not active country, no deploy
        InitRoundBuilder.create().nextRound("W3")
                .addInactiveCountry("france")
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france"))
                .whenInitRound(statusWorkflowDomain, this)
                .thenExpect(InitRoundResultBuilder.create().status(GameStatusEnum.MILITARY_MOVE)
                        .addLeaderHealed(1L));

        // Leader wounded until W3 and we go in W3, leader returns and since france is active country, next phase will be hierarchy
        InitRoundBuilder.create().nextRound("W3")
                .addActiveCountry("france")
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france"))
                .whenInitRound(statusWorkflowDomain, this)
                .thenExpect(InitRoundResultBuilder.create().status(GameStatusEnum.MILITARY_HIERARCHY)
                        .addLeaderHealed(1L)
                        .addCountryNotReady("france"));

        // France needs to deploy its leader, but not genes which is not active
        InitRoundBuilder.create().nextRound("W3")
                .addActiveCountry("france").addActiveCountry("spain")
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france"))
                .addLeader("S3", LeaderBuilder.create().id(2L).code("Doria").country("genes"))
                .whenInitRound(statusWorkflowDomain, this)
                .thenExpect(InitRoundResultBuilder.create().status(GameStatusEnum.MILITARY_HIERARCHY)
                        .addLeaderHealed(1L).addLeaderHealed(2L)
                        .addCountryNotReady("france"));

        // France needs to deploy its leader, and spain has to deploy its minor (genes) leader
        InitRoundBuilder.create().nextRound("W3")
                .addActiveCountry("france").addActiveCountry("spain")
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france"))
                .addLeader("S3", LeaderBuilder.create().id(2L).code("Doria").country("genes"))
                .addPatrons("genes", "spain")
                .whenInitRound(statusWorkflowDomain, this)
                .thenExpect(InitRoundResultBuilder.create().status(GameStatusEnum.MILITARY_HIERARCHY)
                        .addLeaderHealed(1L).addLeaderHealed(2L)
                        .addCountryNotReady("france").addCountryNotReady("spain"));

        // France needs to deploy its leader, and spain has to deploy its minor (genes) leader
        InitRoundBuilder.create().nextRound("W3")
                .addActiveCountry("france").addActiveCountry("spain")
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france"))
                .addLeader("S3", LeaderBuilder.create().id(2L).code("Doria").country("genes"))
                .addPatrons("genes", "spain", "france")
                .whenInitRound(statusWorkflowDomain, this)
                .thenExpect(InitRoundResultBuilder.create().status(GameStatusEnum.MILITARY_HIERARCHY)
                        .addLeaderHealed(1L).addLeaderHealed(2L)
                        .addCountryNotReady("france").addCountryNotReady("spain"));
    }

    @Test
    public void manageLeaders() {
        InitRoundBuilder.create()
                .whenManageLeaders(statusWorkflowDomain, this)
                .thenExpect(ManageLeaderResultBuilder.create());

        // A named leader that is still is the round track will go on the turn track
        InitRoundBuilder.create()
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france").end(5))
                .whenManageLeaders(statusWorkflowDomain, this)
                .thenExpect(ManageLeaderResultBuilder.create()
                        .returnLeader(1L));

        // But a named leader on the round track that die at this turn will just be removed
        InitRoundBuilder.create()
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france").end(4))
                .whenManageLeaders(statusWorkflowDomain, this)
                .thenExpect(ManageLeaderResultBuilder.create()
                        .removeLeader(1L));

        // France has a named leader and an anonymous leader on the map, and a named leader in the round track => anonymous removed, round one moved
        InitRoundBuilder.create()
                .addActiveCountry("france")
                .addLeader("W3", LeaderBuilder.create().id(1L).code("Napo").country("france"))
                .addLeader("idf", LeaderBuilder.create().id(2L).code("Nabo").country("france"))
                .addLeader("idf", LeaderBuilder.create().id(3L).code("Legion-G1").country("france").anonymous())
                .whenManageLeaders(statusWorkflowDomain, this)
                .thenExpect(ManageLeaderResultBuilder.create()
                        .removeLeader(3L)
                        .returnLeader(1L));

        // France has a named leader on the map that will die, and an anonymous leader on the round track that will be removed
        // Genes has an anonymous leader on the map and on the round track that will be moved, and a leader on the round track that will die
        // England has an anonymous leader on the map that will be removed
        InitRoundBuilder.create()
                .addActiveCountry("france").addActiveCountry("england")
                .addLeader("idf", LeaderBuilder.create().id(2L).code("Nabo").country("france").end(4))
                .addLeader("W3", LeaderBuilder.create().id(3L).code("Legion-G1").country("france").anonymous())
                .addLeader("genoa", LeaderBuilder.create().id(4L).code("Legion-G1").country("genes").anonymous())
                .addLeader("S3", LeaderBuilder.create().id(5L).code("Legion-G2").country("genes").anonymous())
                .addLeader("End", LeaderBuilder.create().id(6L).code("Doria").country("genes").end(4))
                .addLeader("london", LeaderBuilder.create().id(7L).code("Legion-G1").country("england").anonymous())
                .whenManageLeaders(statusWorkflowDomain, this)
                .thenExpect(ManageLeaderResultBuilder.create()
                        .removeLeader(3L).removeLeader(3L).removeLeader(6L).removeLeader(7L)
                        .returnLeader(5L));
    }

    static class InitRoundBuilder {
        String nextRound;
        List<PlayableCountryEntity> countries = new ArrayList<>();
        Map<String, List<LeaderBuilder>> leaders = new HashMap<>();
        Map<String, List<String>> patrons = new HashMap<>();
        List<DiffEntity> diffs;
        List<String> countriesNotReady;

        static InitRoundBuilder create() {
            return new InitRoundBuilder();
        }

        InitRoundBuilder nextRound(String nextRound) {
            if (!nextRound.startsWith("B_MR_")) {
                nextRound = "B_MR_" + nextRound;
            }
            this.nextRound = nextRound;
            return this;
        }

        InitRoundBuilder addActiveCountry(String name) {
            PlayableCountryEntity country = new PlayableCountryEntity();
            country.setName(name);
            country.setUsername(name);
            this.countries.add(country);
            return this;
        }

        InitRoundBuilder addInactiveCountry(String name) {
            PlayableCountryEntity country = new PlayableCountryEntity();
            country.setName(name);
            this.countries.add(country);
            return this;
        }

        InitRoundBuilder addPatrons(String country, String... patrons) {
            this.patrons.put(country, Arrays.asList(patrons));
            return this;
        }

        InitRoundBuilder addLeader(String province, LeaderBuilder leader) {
            if (!this.leaders.containsKey(province)) {
                this.leaders.put(province, new ArrayList<>());
            }
            this.leaders.get(province).add(leader);
            return this;
        }

        InitRoundBuilder whenInitRound(StatusWorkflowDomainImpl statusWorkflowDomain, StatusWorkflowDomainTest testClass) {
            AbstractBack.TABLES = new Tables();
            GameEntity game = new GameEntity();
            game.setId(2L);
            game.setTurn(5);
            game.getCountries().addAll(countries);
            StackEntity stackTurn = new StackEntity();
            stackTurn.setId(666L);
            stackTurn.setGame(game);
            stackTurn.setProvince(GameUtil.getTurnBox(game.getTurn()));
            game.getStacks().add(stackTurn);
            for (String province : leaders.keySet()) {
                String realProvince = province.startsWith("B_MR_") ? province : "B_MR_" + province;
                StackEntity stack = new StackEntity();
                stack.setId(2L);
                stack.setGame(game);
                stack.setProvince(realProvince);
                game.getStacks().add(stack);
                for (LeaderBuilder leader : leaders.get(province)) {
                    stack.getCounters().add(createLeader(leader, AbstractBack.TABLES, stack));
                }
            }
            leaders.values().stream()
                    .flatMap(Collection::stream)
                    .map(leader -> leader.country)
                    .filter(country -> !patrons.containsKey(country))
                    .forEach(country -> patrons.put(country, Collections.singletonList(country)));
            for (String country : patrons.keySet()) {
                when(testClass.counterDao.getPatrons(country, game.getId())).thenReturn(patrons.get(country));
            }
            when(testClass.counterDomain.moveSpecialCounter(any(), any(), any(), any())).thenAnswer(moveSpecialCounterAnswer());
            when(testClass.counterDomain.moveToSpecialBox(any(), any(), any())).thenAnswer(invocation -> {
                CounterEntity counter = invocation.getArgumentAt(0, CounterEntity.class);
                if (counter != null) {
                    stackTurn.getCounters().add(counter);
                }
                return moveToSpecialBoxAnswer().answer(invocation);
            });

            diffs = statusWorkflowDomain.initNewRound(nextRound, game);
            countriesNotReady = game.getCountries().stream()
                    .filter(country -> !country.isReady())
                    .map(PlayableCountryEntity::getName)
                    .collect(Collectors.toList());

            return this;
        }

        InitRoundBuilder whenManageLeaders(StatusWorkflowDomainImpl statusWorkflowDomain, StatusWorkflowDomainTest testClass) {
            AbstractBack.TABLES = new Tables();
            GameEntity game = new GameEntity();
            game.setId(2L);
            game.setTurn(5);
            game.getCountries().addAll(countries);
            StackEntity stackTurn = new StackEntity();
            stackTurn.setId(666L);
            stackTurn.setGame(game);
            stackTurn.setProvince(GameUtil.getTurnBox(game.getTurn()));
            game.getStacks().add(stackTurn);
            for (String province : leaders.keySet()) {
                boolean provinceOk = province.startsWith("B_MR_") || (!province.startsWith("W") && !province.startsWith("S") && !province.startsWith("End"));
                String realProvince = provinceOk ? province : "B_MR_" + province;
                StackEntity stack = new StackEntity();
                stack.setId(1L);
                stack.setGame(game);
                stack.setProvince(realProvince);
                game.getStacks().add(stack);
                for (LeaderBuilder leader : leaders.get(province)) {
                    stack.getCounters().add(createLeader(leader, AbstractBack.TABLES, stack));
                }
            }
            when(testClass.counterDomain.moveSpecialCounter(any(), any(), any(), any())).thenAnswer(moveSpecialCounterAnswer());
            when(testClass.counterDomain.moveToSpecialBox(any(), any(), any())).thenAnswer(invocation -> {
                CounterEntity counter = invocation.getArgumentAt(0, CounterEntity.class);
                if (counter != null) {
                    stackTurn.getCounters().add(counter);
                }
                return moveToSpecialBoxAnswer().answer(invocation);
            });
            when(testClass.counterDomain.removeCounter(any())).thenAnswer(removeCounterAnswer());

            diffs = statusWorkflowDomain.manageLeadersEndTurn(game);

            return this;
        }

        InitRoundBuilder thenExpect(InitRoundResultBuilder result) {
            int nbDiffs = 2;
            DiffEntity diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.GAME)
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull("The modify game for status change was not sent.", diff);
            Assert.assertEquals("The new status in the modify game diff is wrong.", result.status.name(), getAttribute(diff, DiffAttributeTypeEnum.STATUS));
            DiffEntity difModifyTurnOrder = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.TURN_ORDER)
                    .findAny()
                    .orElse(null);
            DiffEntity diffModifyStacks = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MODIFY && d.getTypeObject() == DiffTypeObjectEnum.STACK)
                    .findAny()
                    .orElse(null);
            if (result.status == GameStatusEnum.MILITARY_HIERARCHY) {
                Assert.assertEquals("The active attribute in the modify game diff was not sent.", "false", getAttribute(diff, DiffAttributeTypeEnum.ACTIVE));
                Assert.assertNull("The modify turn order was sent but was not supposed to.", difModifyTurnOrder);
                Assert.assertNull("The modify stacks was sent but was not supposed to.", diffModifyStacks);
            } else {
                Assert.assertNull("The active attribute in the modify game diff was sent but should not had.", getAttributeFull(diff, DiffAttributeTypeEnum.ACTIVE));
                Assert.assertNotNull("The modify turn order was not sent but was supposed to.", difModifyTurnOrder);
                Assert.assertEquals("The active attribute of the modify turn order diff is wrong", "0", getAttribute(difModifyTurnOrder, DiffAttributeTypeEnum.ACTIVE));
                Assert.assertNotNull("The modify stacks was not sent but was supposed to.", diffModifyStacks);
                Assert.assertEquals("The move phase attribute of the modify stacks diff is wrong", MovePhaseEnum.NOT_MOVED.name(), getAttribute(diffModifyStacks, DiffAttributeTypeEnum.MOVE_PHASE));
                nbDiffs += 2;
            }

            diff = diffs.stream()
                    .filter(d -> d.getType() == DiffTypeEnum.MOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                            d.getIdObject() == null)
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull("The move round counter diff was not sent.", diff);
            Assert.assertEquals("The next round province attribute is wrong.", nextRound, getAttribute(diff, DiffAttributeTypeEnum.PROVINCE_TO));

            for (Long leader : result.leaderHealed) {
                diff = diffs.stream()
                        .filter(d -> d.getType() == DiffTypeEnum.MOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                                Objects.equals(d.getIdObject(), leader))
                        .findAny()
                        .orElse(null);
                Assert.assertNotNull("The move leader diff was not sent.", diff);
                Assert.assertEquals("The province attribute of the move leader diff is wrong.", GameUtil.getTurnBox(5), getAttribute(diff, DiffAttributeTypeEnum.PROVINCE_TO));
                nbDiffs++;
            }

            if (result.status == GameStatusEnum.MILITARY_HIERARCHY) {
                for (String country : result.countriesNotReady) {
                    Assert.assertTrue("Country " + country + " should have some leader to deploy but is already ready.", countriesNotReady.contains(country));
                }

                Assert.assertEquals("Some countries were not expected to have some leader to deploy.", result.countriesNotReady.size(), countriesNotReady.size());
            }

            Assert.assertEquals("The number of diffs received is wrong.", nbDiffs, diffs.size());

            return this;
        }

        InitRoundBuilder thenExpect(ManageLeaderResultBuilder result) {
            int nbDiffs = 0;

            for (Long leader : result.removedLeaders) {
                DiffEntity diff = diffs.stream()
                        .filter(d -> d.getType() == DiffTypeEnum.REMOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                                Objects.equals(d.getIdObject(), leader))
                        .findAny()
                        .orElse(null);
                Assert.assertNotNull("The remove leader diff was not sent.", diff);
                nbDiffs++;
            }

            for (Long leader : result.returningLeaders) {
                DiffEntity diff = diffs.stream()
                        .filter(d -> d.getType() == DiffTypeEnum.MOVE && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                                Objects.equals(d.getIdObject(), leader))
                        .findAny()
                        .orElse(null);
                Assert.assertNotNull("The move leader diff was not sent.", diff);
                Assert.assertEquals("The province attribute of the move leader diff is wrong.", GameUtil.getTurnBox(5), getAttribute(diff, DiffAttributeTypeEnum.PROVINCE_TO));
                nbDiffs++;
            }

            for (LeaderMatch leaderMatch : result.addedLeaders) {
                DiffEntity diff = diffs.stream()
                        .filter(d -> d.getType() == DiffTypeEnum.ADD && d.getTypeObject() == DiffTypeObjectEnum.COUNTER &&
                                StringUtils.equals(leaderMatch.country, getAttribute(d, DiffAttributeTypeEnum.COUNTRY)) &&
                                leaderMatch.codes.contains(getAttribute(d, DiffAttributeTypeEnum.CODE)))
                        .findAny()
                        .orElse(null);
                Assert.assertNotNull("The add counter for codes " + leaderMatch.codes + " and country " + leaderMatch.country + " was not sent.", diff);
                Assert.assertEquals("The add counter for codes " + leaderMatch.codes + " and country " + leaderMatch.country + " has the wrong province attribute.",
                        GameUtil.getTurnBox(5), getAttribute(diff, DiffAttributeTypeEnum.PROVINCE));
                nbDiffs++;
            }

            Assert.assertEquals("The number of diffs received is wrong.", nbDiffs, diffs.size());

            return this;
        }
    }

    static class InitRoundResultBuilder {
        GameStatusEnum status;
        List<Long> leaderHealed = new ArrayList<>();
        List<String> countriesNotReady = new ArrayList<>();

        static InitRoundResultBuilder create() {
            return new InitRoundResultBuilder();
        }

        InitRoundResultBuilder status(GameStatusEnum status) {
            this.status = status;
            return this;
        }

        InitRoundResultBuilder addLeaderHealed(Long leader) {
            this.leaderHealed.add(leader);
            return this;
        }

        InitRoundResultBuilder addCountryNotReady(String country) {
            this.countriesNotReady.add(country);
            return this;
        }
    }

    static class ManageLeaderResultBuilder {
        List<Long> removedLeaders = new ArrayList<>();
        List<Long> returningLeaders = new ArrayList<>();
        List<LeaderMatch> addedLeaders = new ArrayList<>();

        static ManageLeaderResultBuilder create() {
            return new ManageLeaderResultBuilder();
        }

        ManageLeaderResultBuilder removeLeader(Long id) {
            this.removedLeaders.add(id);
            return this;
        }

        ManageLeaderResultBuilder returnLeader(Long id) {
            this.returningLeaders.add(id);
            return this;
        }

        ManageLeaderResultBuilder addLeader(LeaderMatch leader) {
            this.addedLeaders.add(leader);
            return this;
        }
    }
}
