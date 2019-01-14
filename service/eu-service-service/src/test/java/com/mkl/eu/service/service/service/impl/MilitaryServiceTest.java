package com.mkl.eu.service.service.service.impl;

import com.mkl.eu.client.common.exception.FunctionalException;
import com.mkl.eu.client.common.exception.IConstantsCommonException;
import com.mkl.eu.client.common.vo.Request;
import com.mkl.eu.client.service.service.IConstantsServiceException;
import com.mkl.eu.client.service.service.common.ValidateRequest;
import com.mkl.eu.client.service.service.military.ChooseBattleRequest;
import com.mkl.eu.client.service.service.military.SelectForceRequest;
import com.mkl.eu.client.service.service.military.WithdrawBeforeBattleRequest;
import com.mkl.eu.client.service.vo.country.PlayableCountry;
import com.mkl.eu.client.service.vo.diff.DiffResponse;
import com.mkl.eu.client.service.vo.enumeration.*;
import com.mkl.eu.client.service.vo.tables.Tech;
import com.mkl.eu.service.service.persistence.oe.GameEntity;
import com.mkl.eu.service.service.persistence.oe.board.CounterEntity;
import com.mkl.eu.service.service.persistence.oe.board.StackEntity;
import com.mkl.eu.service.service.persistence.oe.country.PlayableCountryEntity;
import com.mkl.eu.service.service.persistence.oe.diff.DiffAttributesEntity;
import com.mkl.eu.service.service.persistence.oe.diff.DiffEntity;
import com.mkl.eu.service.service.persistence.oe.diplo.CountryOrderEntity;
import com.mkl.eu.service.service.persistence.oe.military.BattleCounterEntity;
import com.mkl.eu.service.service.persistence.oe.military.BattleEntity;
import com.mkl.eu.service.service.persistence.oe.ref.province.BorderEntity;
import com.mkl.eu.service.service.persistence.oe.ref.province.EuropeanProvinceEntity;
import com.mkl.eu.service.service.persistence.ref.IProvinceDao;
import com.mkl.eu.service.service.service.AbstractGameServiceTest;
import com.mkl.eu.service.service.util.ArmyInfo;
import com.mkl.eu.service.service.util.IOEUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Test of MilitaryService.
 *
 * @author MKL.
 */
@RunWith(MockitoJUnitRunner.class)
public class MilitaryServiceTest extends AbstractGameServiceTest {
    @InjectMocks
    private MilitaryServiceImpl militaryService;

    @Mock
    private IOEUtil oeUtil;

    @Mock
    private IProvinceDao provinceDao;

    @Test
    public void testChooseBattleFail() {
        Pair<Request<ChooseBattleRequest>, GameEntity> pair = testCheckGame(militaryService::chooseBattle, "chooseBattle");
        Request<ChooseBattleRequest> request = pair.getLeft();
        GameEntity game = pair.getRight();
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(0).setStatus(BattleStatusEnum.SELECT_FORCES);
        game.getBattles().get(0).setProvince("pecs");
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(1).setStatus(BattleStatusEnum.NEW);
        game.getBattles().get(1).setProvince("lyonnais");
        request.setIdCountry(26L);
        testCheckStatus(pair.getRight(), request, militaryService::chooseBattle, "chooseBattle", GameStatusEnum.MILITARY_BATTLES);

        try {
            militaryService.chooseBattle(request);
            Assert.fail("Should break because chooseBattle.request is null");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("chooseBattle.request", e.getParams()[0]);
        }

        request.setRequest(new ChooseBattleRequest());

        try {
            militaryService.chooseBattle(request);
            Assert.fail("Should break because province is null");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("chooseBattle.request.province", e.getParams()[0]);
        }

        request.getRequest().setProvince("");

        try {
            militaryService.chooseBattle(request);
            Assert.fail("Should break because province is empty");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("chooseBattle.request.province", e.getParams()[0]);
        }

        request.getRequest().setProvince("idf");

        try {
            militaryService.chooseBattle(request);
            Assert.fail("Should break because another battle is in process");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_IN_PROCESS, e.getCode());
            Assert.assertEquals("chooseBattle", e.getParams()[0]);
        }

        game.getBattles().get(0).setStatus(BattleStatusEnum.NEW);

        try {
            militaryService.chooseBattle(request);
            Assert.fail("Should break because no battle is in this province");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.INVALID_PARAMETER, e.getCode());
            Assert.assertEquals("chooseBattle.request.province", e.getParams()[0]);
        }
    }

    @Test
    public void testChooseBattleToSelectForces() throws FunctionalException {
        testChooseBattle(false);
    }

    @Test
    public void testChooseBattleToWithdrawBeforeBattle() throws FunctionalException {
        testChooseBattle(true);
    }

    private void testChooseBattle(boolean gotoWithdraw) throws FunctionalException {
        Pair<Request<ChooseBattleRequest>, GameEntity> pair = testCheckGame(militaryService::chooseBattle, "chooseBattle");
        Request<ChooseBattleRequest> request = pair.getLeft();
        GameEntity game = pair.getRight();
        game.getCountries().add(new PlayableCountryEntity());
        game.getCountries().get(0).setId(26L);
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(0).setId(33L);
        game.getBattles().get(0).setStatus(BattleStatusEnum.NEW);
        game.getBattles().get(0).setProvince("idf");
        game.getStacks().add(new StackEntity());
        game.getStacks().get(0).setProvince("idf");
        game.getStacks().get(0).setCountry("france");
        game.getStacks().get(0).getCounters().add(new CounterEntity());
        game.getStacks().get(0).getCounters().get(0).setId(1L);
        game.getStacks().get(0).getCounters().get(0).setType(CounterFaceTypeEnum.ARMY_PLUS);
        game.getStacks().add(new StackEntity());
        game.getStacks().get(1).setProvince("idf");
        game.getStacks().get(1).setCountry("turquie");
        game.getStacks().get(1).getCounters().add(new CounterEntity());
        game.getStacks().get(1).getCounters().get(0).setId(2L);
        game.getStacks().get(1).getCounters().get(0).setType(CounterFaceTypeEnum.LAND_DETACHMENT);
        game.getStacks().get(1).getCounters().add(new CounterEntity());
        game.getStacks().get(1).getCounters().get(1).setId(3L);
        game.getStacks().get(1).getCounters().get(1).setType(CounterFaceTypeEnum.LAND_DETACHMENT);
        game.getStacks().get(1).getCounters().add(new CounterEntity());
        game.getStacks().get(1).getCounters().get(2).setId(4L);
        game.getStacks().get(1).getCounters().get(2).setType(CounterFaceTypeEnum.LAND_DETACHMENT);
        game.getStacks().add(new StackEntity());
        game.getStacks().get(2).setProvince("idf");
        game.getStacks().get(2).setCountry("espagne");
        game.getStacks().get(2).getCounters().add(new CounterEntity());
        game.getStacks().get(2).getCounters().get(0).setId(5L);
        game.getStacks().get(2).getCounters().get(0).setType(CounterFaceTypeEnum.ARMY_PLUS);
        game.getStacks().add(new StackEntity());
        game.getStacks().get(3).setProvince("idf");
        game.getStacks().get(3).setCountry("angleterre");
        game.getStacks().get(3).getCounters().add(new CounterEntity());
        game.getStacks().get(3).getCounters().get(0).setId(6L);
        game.getStacks().get(3).getCounters().get(0).setType(CounterFaceTypeEnum.ARMY_PLUS);
        game.getStacks().get(3).getCounters().add(new CounterEntity());
        game.getStacks().get(3).getCounters().get(1).setId(7L);
        game.getStacks().get(3).getCounters().get(1).setType(CounterFaceTypeEnum.LAND_DETACHMENT);


        request.setIdCountry(26L);
        request.setRequest(new ChooseBattleRequest());
        request.getRequest().setProvince("idf");
        testCheckStatus(pair.getRight(), request, militaryService::chooseBattle, "chooseBattle", GameStatusEnum.MILITARY_BATTLES);

        List<String> allies = new ArrayList<>();
        allies.add("france");
        List<String> enemies = new ArrayList<>();
        enemies.add("espagne");
        if (!gotoWithdraw) {
            allies.add("turquie");
            enemies.add("angleterre");
        }
        when(oeUtil.getAllies(game.getCountries().get(0), game)).thenReturn(allies);
        when(oeUtil.getEnemies(game.getCountries().get(0), game)).thenReturn(enemies);

        simulateDiff();

        DiffResponse response = militaryService.chooseBattle(request);

        DiffEntity diffEntity = retrieveDiffCreated();

        Assert.assertEquals(DiffTypeEnum.MODIFY, diffEntity.getType());
        Assert.assertEquals(DiffTypeObjectEnum.BATTLE, diffEntity.getTypeObject());
        Assert.assertEquals(game.getBattles().get(0).getId(), diffEntity.getIdObject());
        Assert.assertEquals(game.getVersion(), diffEntity.getVersionGame().longValue());
        Assert.assertEquals(game.getId(), diffEntity.getIdGame());
        if (gotoWithdraw) {
            Assert.assertEquals(5, diffEntity.getAttributes().size());
            Assert.assertEquals(DiffAttributeTypeEnum.STATUS, diffEntity.getAttributes().get(0).getType());
            Assert.assertEquals(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE.name(), diffEntity.getAttributes().get(0).getValue());
            Assert.assertEquals(DiffAttributeTypeEnum.ATTACKER_READY, diffEntity.getAttributes().get(1).getType());
            Assert.assertEquals("true", diffEntity.getAttributes().get(1).getValue());
            Assert.assertEquals(DiffAttributeTypeEnum.ATTACKER_COUNTER_ADD, diffEntity.getAttributes().get(2).getType());
            Assert.assertEquals("1", diffEntity.getAttributes().get(2).getValue());
            Assert.assertEquals(DiffAttributeTypeEnum.DEFENDER_READY, diffEntity.getAttributes().get(3).getType());
            Assert.assertEquals("true", diffEntity.getAttributes().get(3).getValue());
            Assert.assertEquals(DiffAttributeTypeEnum.DEFENDER_COUNTER_ADD, diffEntity.getAttributes().get(4).getType());
            Assert.assertEquals("5", diffEntity.getAttributes().get(4).getValue());
        } else {
            Assert.assertEquals(1, diffEntity.getAttributes().size());
            Assert.assertEquals(DiffAttributeTypeEnum.STATUS, diffEntity.getAttributes().get(0).getType());
            Assert.assertEquals(BattleStatusEnum.SELECT_FORCES.name(), diffEntity.getAttributes().get(0).getValue());
        }

        Assert.assertEquals(game.getVersion(), response.getVersionGame().longValue());
        Assert.assertEquals(getDiffAfter(), response.getDiffs());

        if (gotoWithdraw) {
            Assert.assertEquals(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE, game.getBattles().get(0).getStatus());
            Assert.assertEquals(2, game.getBattles().get(0).getCounters().size());
            BattleCounterEntity counterFra = game.getBattles().get(0).getCounters().stream()
                    .filter(c -> c.getCounter().getId().equals(1L))
                    .findAny()
                    .orElse(null);
            Assert.assertEquals(true, counterFra.isPhasing());
            BattleCounterEntity counterEsp = game.getBattles().get(0).getCounters().stream()
                    .filter(c -> c.getCounter().getId().equals(5L))
                    .findAny()
                    .orElse(null);
            Assert.assertEquals(false, counterEsp.isPhasing());
        } else {
            Assert.assertEquals(BattleStatusEnum.SELECT_FORCES, game.getBattles().get(0).getStatus());
        }
    }

    @Test
    public void testSelectForceFail() {
        Pair<Request<SelectForceRequest>, GameEntity> pair = testCheckGame(militaryService::selectForce, "selectForce");
        Request<SelectForceRequest> request = pair.getLeft();
        GameEntity game = pair.getRight();
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(0).setStatus(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE);
        game.getBattles().get(0).setProvince("pecs");
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(1).setStatus(BattleStatusEnum.NEW);
        game.getBattles().get(1).setProvince("lyonnais");
        testCheckStatus(pair.getRight(), request, militaryService::selectForce, "selectForce", GameStatusEnum.MILITARY_BATTLES);
        request.setIdCountry(26L);

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because selectForce.request is null");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("selectForce.request", e.getParams()[0]);
        }

        request.setRequest(new SelectForceRequest());
        request.setIdCountry(12L);

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because idCounter is null");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("selectForce.request.idCounter", e.getParams()[0]);
        }

        request.getRequest().setIdCounter(6L);

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because no battle is in right status");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_STATUS_NONE, e.getCode());
            Assert.assertEquals("selectForce", e.getParams()[0]);
        }

        game.getBattles().get(0).setStatus(BattleStatusEnum.SELECT_FORCES);

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because counter does not exist in the battle");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.INVALID_PARAMETER, e.getCode());
            Assert.assertEquals("selectForce.request.idCounter", e.getParams()[0]);
        }

        BattleCounterEntity battleCounter = new BattleCounterEntity();
        battleCounter.setCounter(new CounterEntity());
        battleCounter.getCounter().setId(6L);
        game.getBattles().get(0).getCounters().add(battleCounter);

        try {
            militaryService.selectForce(request);
        } catch (FunctionalException e) {
            Assert.fail("Should not break " + e.getMessage());
        }

        request.getRequest().setAdd(true);

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because counter does not exist");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.INVALID_PARAMETER, e.getCode());
            Assert.assertEquals("selectForce.request.idCounter", e.getParams()[0]);
        }

        when(oeUtil.getAllies(null, game)).thenReturn(Collections.singletonList("france"));
        game.getStacks().add(new StackEntity());
        game.getStacks().get(0).setProvince("pecs");
        game.getStacks().get(0).setCountry("france");
        game.getStacks().get(0).getCounters().add(new CounterEntity());
        game.getStacks().get(0).getCounters().get(0).setId(6L);
        game.getStacks().get(0).getCounters().get(0).setType(CounterFaceTypeEnum.ARMY_PLUS);

        try {
            militaryService.selectForce(request);
        } catch (FunctionalException e) {
            Assert.fail("Should not break " + e.getMessage());
        }

        game.getStacks().get(0).getCounters().get(0).setType(CounterFaceTypeEnum.TECH_MANOEUVRE);

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because counter is not an army");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.INVALID_PARAMETER, e.getCode());
            Assert.assertEquals("selectForce.request.idCounter", e.getParams()[0]);
        }

        game.getStacks().get(0).getCounters().get(0).setType(CounterFaceTypeEnum.ARMY_PLUS);
        game.getStacks().get(0).setCountry("pologne");

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because counter is not owned");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.INVALID_PARAMETER, e.getCode());
            Assert.assertEquals("selectForce.request.idCounter", e.getParams()[0]);
        }

        game.getStacks().get(0).setCountry("france");
        game.getStacks().get(0).setProvince("idf");

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because counter is not in the right province");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.INVALID_PARAMETER, e.getCode());
            Assert.assertEquals("selectForce.request.idCounter", e.getParams()[0]);
        }

        game.getStacks().get(0).setProvince("pecs");
        game.getBattles().get(0).getNonPhasing().setForces(true);

        try {
            militaryService.selectForce(request);
            Assert.fail("Should break because the attacker already validated its forces");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_SELECT_VALIDATED, e.getCode());
            Assert.assertEquals("selectForce", e.getParams()[0]);
        }
    }

    @Test
    public void testSelectForceAddAttacker() throws FunctionalException {
        testSelectForceSuccess(true, true);
    }

    @Test
    public void testSelectForceRemoveAttacker() throws FunctionalException {
        testSelectForceSuccess(false, true);
    }

    @Test
    public void testSelectForceAddDefender() throws FunctionalException {
        testSelectForceSuccess(true, false);
    }

    @Test
    public void testSelectForceRemoveDefender() throws FunctionalException {
        testSelectForceSuccess(false, false);
    }

    private void testSelectForceSuccess(boolean add, boolean attacker) throws FunctionalException {
        Pair<Request<SelectForceRequest>, GameEntity> pair = testCheckGame(militaryService::selectForce, "selectForce");
        Request<SelectForceRequest> request = pair.getLeft();
        GameEntity game = pair.getRight();
        PlayableCountryEntity country = new PlayableCountryEntity();
        country.setId(12L);
        country.setName("france");
        game.getCountries().add(country);
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(0).setStatus(BattleStatusEnum.SELECT_FORCES);
        game.getBattles().get(0).setProvince("pecs");
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(1).setStatus(BattleStatusEnum.NEW);
        game.getBattles().get(1).setProvince("lyonnais");
        game.getStacks().add(new StackEntity());
        game.getStacks().get(0).setProvince("pecs");
        game.getStacks().get(0).setCountry(country.getName());
        CounterEntity counter = new CounterEntity();
        counter.setId(6L);
        counter.setType(CounterFaceTypeEnum.ARMY_PLUS);
        game.getStacks().get(0).getCounters().add(counter);

        testCheckStatus(pair.getRight(), request, militaryService::selectForce, "selectForce", GameStatusEnum.MILITARY_BATTLES);
        request.setIdCountry(26L);
        request.setRequest(new SelectForceRequest());
        request.setIdCountry(12L);
        request.getRequest().setIdCounter(6L);
        request.getRequest().setAdd(add);

        when(oeUtil.getAllies(country, game)).thenReturn(Collections.singletonList(country.getName()));

        if (!add) {
            BattleCounterEntity battleCounter = new BattleCounterEntity();
            battleCounter.setCounter(new CounterEntity());
            battleCounter.getCounter().setId(6L);
            game.getBattles().get(0).getCounters().add(battleCounter);
        }

        if (attacker) {
            CountryOrderEntity order = new CountryOrderEntity();
            order.setGameStatus(GameStatusEnum.MILITARY_MOVE);
            order.setActive(true);
            order.setCountry(country);
            game.getOrders().add(order);
        }

        simulateDiff();

        DiffResponse response = militaryService.selectForce(request);

        DiffEntity diffEntity = retrieveDiffCreated();

        BattleEntity battle = game.getBattles().get(0);

        Assert.assertEquals(DiffTypeEnum.MODIFY, diffEntity.getType());
        Assert.assertEquals(DiffTypeObjectEnum.BATTLE, diffEntity.getTypeObject());
        Assert.assertEquals(game.getBattles().get(0).getId(), diffEntity.getIdObject());
        Assert.assertEquals(game.getVersion(), diffEntity.getVersionGame().longValue());
        Assert.assertEquals(game.getId(), diffEntity.getIdGame());
        Assert.assertEquals(1, diffEntity.getAttributes().size());
        DiffAttributeTypeEnum diffStatus;
        if (attacker) {
            if (add) {
                diffStatus = DiffAttributeTypeEnum.ATTACKER_COUNTER_ADD;

                Assert.assertEquals(1, battle.getCounters().size());
                BattleCounterEntity battleCounter = battle.getCounters().iterator().next();
                Assert.assertEquals(battle, battleCounter.getBattle());
                Assert.assertEquals(counter, battleCounter.getCounter());
                Assert.assertEquals(true, battleCounter.isPhasing());
            } else {
                diffStatus = DiffAttributeTypeEnum.ATTACKER_COUNTER_REMOVE;

                Assert.assertEquals(0, battle.getCounters().size());
            }
        } else {
            if (add) {
                diffStatus = DiffAttributeTypeEnum.DEFENDER_COUNTER_ADD;

                Assert.assertEquals(1, battle.getCounters().size());
                BattleCounterEntity battleCounter = battle.getCounters().iterator().next();
                Assert.assertEquals(battle, battleCounter.getBattle());
                Assert.assertEquals(counter, battleCounter.getCounter());
                Assert.assertEquals(false, battleCounter.isPhasing());
            } else {
                diffStatus = DiffAttributeTypeEnum.DEFENDER_COUNTER_REMOVE;

                Assert.assertEquals(0, battle.getCounters().size());
            }
        }
        Assert.assertEquals(diffStatus, diffEntity.getAttributes().get(0).getType());
        Assert.assertEquals("6", diffEntity.getAttributes().get(0).getValue());

        Assert.assertEquals(game.getVersion(), response.getVersionGame().longValue());
        Assert.assertEquals(getDiffAfter(), response.getDiffs());
    }

    @Test
    public void testValidateForcesFail() {
        Pair<Request<ValidateRequest>, GameEntity> pair = testCheckGame(militaryService::validateForces, "validateForces");
        Request<ValidateRequest> request = pair.getLeft();
        GameEntity game = pair.getRight();
        PlayableCountryEntity country = new PlayableCountryEntity();
        country.setId(12L);
        country.setName("france");
        game.getCountries().add(country);
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(0).setStatus(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE);
        game.getBattles().get(0).getNonPhasing().setForces(true);
        game.getBattles().get(0).setProvince("pecs");
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(1).setStatus(BattleStatusEnum.NEW);
        game.getBattles().get(1).setProvince("lyonnais");
        testCheckStatus(pair.getRight(), request, militaryService::validateForces, "validateForces", GameStatusEnum.MILITARY_BATTLES);
        request.setIdCountry(26L);

        try {
            militaryService.validateForces(request);
            Assert.fail("Should break because validateForces.request is null");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("validateForces.request", e.getParams()[0]);
        }

        request.setRequest(new ValidateRequest());
        request.setIdCountry(12L);

        try {
            militaryService.validateForces(request);
            Assert.fail("Should break because no battle is in right status");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_STATUS_NONE, e.getCode());
            Assert.assertEquals("validateForces", e.getParams()[0]);
        }

        game.getBattles().get(0).setStatus(BattleStatusEnum.SELECT_FORCES);

        try {
            militaryService.validateForces(request);
            Assert.fail("Should break because invalidate is impossible if no other counter exists");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_INVALIDATE_NO_FORCE, e.getCode());
            Assert.assertEquals("validateForces.request.validate", e.getParams()[0]);
        }

        request.getRequest().setValidate(true);
        game.getBattles().get(0).getNonPhasing().setForces(false);
        game.getStacks().add(new StackEntity());
        game.getStacks().get(0).setProvince("pecs");
        game.getStacks().get(0).setCountry(country.getName());
        CounterEntity counter = new CounterEntity();
        counter.setId(6L);
        counter.setType(CounterFaceTypeEnum.ARMY_PLUS);
        game.getStacks().get(0).getCounters().add(counter);
        BattleCounterEntity bc = new BattleCounterEntity();
        bc.setCounter(counter);
        game.getBattles().get(0).getCounters().add(bc);
        counter = new CounterEntity();
        counter.setId(7L);
        counter.setType(CounterFaceTypeEnum.ARMY_MINUS);
        game.getStacks().get(0).getCounters().add(counter);
        bc = new BattleCounterEntity();
        bc.setCounter(counter);
        game.getBattles().get(0).getCounters().add(bc);
        counter = new CounterEntity();
        counter.setId(8L);
        counter.setType(CounterFaceTypeEnum.ARMY_MINUS);
        game.getStacks().get(0).getCounters().add(counter);

        when(oeUtil.getAllies(country, game)).thenReturn(Collections.singletonList(country.getName()));

        try {
            militaryService.validateForces(request);
            Assert.fail("Should break because validate is impossible if other counter could be selected");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_VALIDATE_OTHER_FORCE, e.getCode());
            Assert.assertEquals("validateForces.request.validate", e.getParams()[0]);
        }
    }

    @Test
    public void testValidateForcesValidatePhasingSuccess() throws FunctionalException {
        testValidateForcesSuccess(true, true, false);
    }

    @Test
    public void testValidateForcesInvalidatePhasingSuccess() throws FunctionalException {
        testValidateForcesSuccess(true, false, true);
    }

    @Test
    public void testValidateForcesValidateNonPhasingSuccess() throws FunctionalException {
        testValidateForcesSuccess(false, true, false);
    }

    @Test
    public void testValidateForcesInvalidateNonPhasingSuccess() throws FunctionalException {
        testValidateForcesSuccess(false, false, true);
    }

    @Test
    public void testValidateForcesValidatePhasingNothing() throws FunctionalException {
        testValidateForcesSuccess(true, true, true);
    }

    @Test
    public void testValidateForcesInvalidatePhasingNothing() throws FunctionalException {
        testValidateForcesSuccess(true, false, false);
    }

    @Test
    public void testValidateForcesValidateNonPhasingNothing() throws FunctionalException {
        testValidateForcesSuccess(false, true, true);
    }

    @Test
    public void testValidateForcesInvalidateNonPhasingNothing() throws FunctionalException {
        testValidateForcesSuccess(false, false, false);
    }

    private void testValidateForcesSuccess(boolean phasing, boolean validate, boolean before) throws FunctionalException {
        Pair<Request<ValidateRequest>, GameEntity> pair = testCheckGame(militaryService::validateForces, "validateForces");
        Request<ValidateRequest> request = pair.getLeft();
        GameEntity game = pair.getRight();
        PlayableCountryEntity country = new PlayableCountryEntity();
        country.setId(12L);
        country.setName("france");
        game.getCountries().add(country);
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(0).setStatus(BattleStatusEnum.SELECT_FORCES);
        game.getBattles().get(0).getNonPhasing().setForces(phasing || before);
        game.getBattles().get(0).getPhasing().setForces(!phasing || before);
        game.getBattles().get(0).setProvince("pecs");
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(1).setStatus(BattleStatusEnum.NEW);
        game.getBattles().get(1).setProvince("lyonnais");
        testCheckStatus(pair.getRight(), request, militaryService::validateForces, "validateForces", GameStatusEnum.MILITARY_BATTLES);
        request.setIdCountry(12L);
        request.setRequest(new ValidateRequest());
        request.getRequest().setValidate(validate);
        game.getStacks().add(new StackEntity());
        game.getStacks().get(0).setProvince("pecs");
        game.getStacks().get(0).setCountry(country.getName());
        CounterEntity counter = new CounterEntity();
        counter.setId(6L);
        counter.setType(CounterFaceTypeEnum.ARMY_PLUS);
        game.getStacks().get(0).getCounters().add(counter);
        BattleCounterEntity bc = new BattleCounterEntity();
        bc.setPhasing(phasing);
        bc.setCounter(counter);
        game.getBattles().get(0).getCounters().add(bc);
        counter = new CounterEntity();
        counter.setId(7L);
        counter.setType(CounterFaceTypeEnum.ARMY_MINUS);
        game.getStacks().get(0).getCounters().add(counter);
        bc = new BattleCounterEntity();
        bc.setPhasing(phasing);
        bc.setCounter(counter);
        game.getBattles().get(0).getCounters().add(bc);
        counter = new CounterEntity();
        counter.setId(8L);
        counter.setType(CounterFaceTypeEnum.ARMY_PLUS);
        game.getStacks().get(0).getCounters().add(counter);

        when(oeUtil.getAllies(country, game)).thenReturn(Collections.singletonList(country.getName()));

        if (phasing) {
            CountryOrderEntity order = new CountryOrderEntity();
            order.setGameStatus(GameStatusEnum.MILITARY_MOVE);
            order.setActive(true);
            order.setCountry(country);
            game.getOrders().add(order);
        }

        simulateDiff();

        DiffResponse response = militaryService.validateForces(request);

        DiffEntity diffEntity = retrieveDiffCreated();

        BattleEntity battle = game.getBattles().get(0);
        if (validate != before) {
            Assert.assertEquals(DiffTypeEnum.MODIFY, diffEntity.getType());
            Assert.assertEquals(DiffTypeObjectEnum.BATTLE, diffEntity.getTypeObject());
            Assert.assertEquals(game.getBattles().get(0).getId(), diffEntity.getIdObject());
            Assert.assertEquals(game.getVersion(), diffEntity.getVersionGame().longValue());
            Assert.assertEquals(game.getId(), diffEntity.getIdGame());
            Assert.assertTrue(diffEntity.getAttributes().size() >= 1);
            DiffAttributeTypeEnum diffStatus;
            if (phasing) {
                diffStatus = DiffAttributeTypeEnum.ATTACKER_READY;

                Assert.assertEquals(validate, battle.getPhasing().isForces());
            } else {
                diffStatus = DiffAttributeTypeEnum.DEFENDER_READY;

                Assert.assertEquals(validate, battle.getNonPhasing().isForces());
            }
            Assert.assertEquals(diffStatus, diffEntity.getAttributes().get(0).getType());
            Assert.assertEquals(Boolean.toString(validate), diffEntity.getAttributes().get(0).getValue());

            DiffAttributesEntity statusChanged = diffEntity.getAttributes().stream()
                    .filter(attribute -> attribute.getType() == DiffAttributeTypeEnum.STATUS)
                    .findAny()
                    .orElse(null);
            if (battle.getPhasing().isForces() && battle.getNonPhasing().isForces()) {
                Assert.assertNotNull(statusChanged);
                Assert.assertEquals(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE.name(), statusChanged.getValue());
                Assert.assertEquals(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE, battle.getStatus());
            } else {
                Assert.assertNull(statusChanged);
                Assert.assertEquals(BattleStatusEnum.SELECT_FORCES, battle.getStatus());
            }
        } else {
            Assert.assertNull(diffEntity);
        }

        Assert.assertEquals(game.getVersion(), response.getVersionGame().longValue());
        Assert.assertEquals(getDiffAfter(), response.getDiffs());
    }

    @Test
    public void testFillBattleModifiers() {
        BattleEntity battle = new BattleEntity();

        BattleCounterEntity battleCounter = new BattleCounterEntity();
        battleCounter.setPhasing(true);
        CounterEntity phasingCounter = new CounterEntity();
        phasingCounter.setType(CounterFaceTypeEnum.ARMY_PLUS);
        phasingCounter.setCountry(PlayableCountry.FRANCE);
        battleCounter.setCounter(phasingCounter);
        battle.getCounters().add(battleCounter);
        battleCounter = new BattleCounterEntity();
        battleCounter.setPhasing(false);
        CounterEntity nonPhasingCounter = new CounterEntity();
        nonPhasingCounter.setType(CounterFaceTypeEnum.ARMY_MINUS);
        nonPhasingCounter.setCountry(PlayableCountry.SPAIN);
        battleCounter.setCounter(nonPhasingCounter);
        battle.getCounters().add(battleCounter);

        EuropeanProvinceEntity idf = new EuropeanProvinceEntity();
        idf.setTerrain(TerrainEnum.PLAIN);
        EuropeanProvinceEntity morbihan = new EuropeanProvinceEntity();
        morbihan.setTerrain(TerrainEnum.DENSE_FOREST);
        EuropeanProvinceEntity lyonnais = new EuropeanProvinceEntity();
        lyonnais.setTerrain(TerrainEnum.SPARSE_FOREST);
        EuropeanProvinceEntity limoges = new EuropeanProvinceEntity();
        limoges.setTerrain(TerrainEnum.DESERT);
        EuropeanProvinceEntity neva = new EuropeanProvinceEntity();
        neva.setTerrain(TerrainEnum.SWAMP);
        EuropeanProvinceEntity tyrol = new EuropeanProvinceEntity();
        tyrol.setTerrain(TerrainEnum.MOUNTAIN);

        when(provinceDao.getProvinceByName("idf")).thenReturn(idf);
        when(provinceDao.getProvinceByName("morbihan")).thenReturn(morbihan);
        when(provinceDao.getProvinceByName("lyonnais")).thenReturn(lyonnais);
        when(provinceDao.getProvinceByName("limoges")).thenReturn(limoges);
        when(provinceDao.getProvinceByName("neva")).thenReturn(neva);
        when(provinceDao.getProvinceByName("tyrol")).thenReturn(tyrol);

        when(oeUtil.getTechnology(Collections.singletonList(phasingCounter), true, militaryService.getReferential(), militaryService.getTables(), battle.getGame()))
                .thenReturn(Tech.ARQUEBUS);
        when(oeUtil.getTechnology(Collections.singletonList(nonPhasingCounter), true, militaryService.getReferential(), militaryService.getTables(), battle.getGame()))
                .thenReturn(Tech.RENAISSANCE);
        List<ArmyInfo> armyPhasing = new ArrayList<>();
        armyPhasing.add(new ArmyInfo());
        armyPhasing.get(0).setType(CounterFaceTypeEnum.ARMY_PLUS);
        armyPhasing.get(0).setCountry(PlayableCountry.FRANCE);
        armyPhasing.get(0).setArmyClass(ArmyClassEnum.IVM);
        List<ArmyInfo> armyNonPhasing = new ArrayList<>();
        armyNonPhasing.add(new ArmyInfo());
        armyNonPhasing.get(0).setType(CounterFaceTypeEnum.ARMY_MINUS);
        armyNonPhasing.get(0).setCountry(PlayableCountry.SPAIN);
        armyNonPhasing.get(0).setArmyClass(ArmyClassEnum.IV);
        when(oeUtil.getArmyInfo(Collections.singletonList(phasingCounter), militaryService.getReferential())).thenReturn(armyPhasing);
        when(oeUtil.getArmyInfo(Collections.singletonList(nonPhasingCounter), militaryService.getReferential())).thenReturn(armyNonPhasing);

        battle.setProvince("idf");
        checkModifiers(battle, Modifiers.init(0));
        Assert.assertEquals(Tech.ARQUEBUS, battle.getPhasing().getTech());
        Assert.assertEquals(Tech.RENAISSANCE, battle.getNonPhasing().getTech());

        battle.setProvince("morbihan");
        checkModifiers(battle, Modifiers.init(-1));

        battle.setProvince("lyonnais");
        checkModifiers(battle, Modifiers.init(-1));

        battle.setProvince("limoges");
        checkModifiers(battle, Modifiers.init(-1));

        battle.setProvince("neva");
        checkModifiers(battle, Modifiers.init(-1));

        battle.setProvince("tyrol");
        checkModifiers(battle, Modifiers.init(-1)
                .addFireNonPhasingFirstDay(1)
                .addShockNonPhasingFirstDay(1)
                .addFireNonPhasingSecondDay(1)
                .addShockNonPhasingSecondDay(1));

        when(oeUtil.getArtilleryBonus(armyPhasing, militaryService.getTables(), battle.getGame())).thenReturn(6);
        when(oeUtil.getArtilleryBonus(armyNonPhasing, militaryService.getTables(), battle.getGame())).thenReturn(5);

        battle.setProvince("idf");
        checkModifiers(battle, Modifiers.init(0)
                .addFirePhasingFirstDay(1)
                .addFirePhasingSecondDay(1));

        when(oeUtil.getArtilleryBonus(armyNonPhasing, militaryService.getTables(), battle.getGame())).thenReturn(7);
        checkModifiers(battle, Modifiers.init(0)
                .addFirePhasingFirstDay(1)
                .addFirePhasingSecondDay(1)
                .addFireNonPhasingFirstDay(1)
                .addFireNonPhasingSecondDay(1));


        when(oeUtil.getArtilleryBonus(armyPhasing, militaryService.getTables(), battle.getGame())).thenReturn(0);
        when(oeUtil.getArtilleryBonus(armyNonPhasing, militaryService.getTables(), battle.getGame())).thenReturn(1);
        when(oeUtil.getCavalryBonus(armyPhasing, TerrainEnum.PLAIN, militaryService.getTables(), battle.getGame())).thenReturn(true);
        when(oeUtil.getCavalryBonus(armyNonPhasing, TerrainEnum.PLAIN, militaryService.getTables(), battle.getGame())).thenReturn(false);
        checkModifiers(battle, Modifiers.init(0)
                .addShockPhasingFirstDay(1)
                .addShockPhasingSecondDay(1));

        when(oeUtil.getCavalryBonus(armyPhasing, TerrainEnum.PLAIN, militaryService.getTables(), battle.getGame())).thenReturn(false);
        when(oeUtil.getCavalryBonus(armyNonPhasing, TerrainEnum.PLAIN, militaryService.getTables(), battle.getGame())).thenReturn(true);
        checkModifiers(battle, Modifiers.init(0)
                .addShockNonPhasingFirstDay(1)
                .addShockNonPhasingSecondDay(1));

        nonPhasingCounter.setType(CounterFaceTypeEnum.LAND_DETACHMENT);
        checkModifiers(battle, Modifiers.init(0)
                .addShockPhasingFirstDay(1)
                .addShockPhasingSecondDay(1)
                .addShockNonPhasingFirstDay(1)
                .addShockNonPhasingSecondDay(1)
                .addFireNonPhasingFirstDay(-1)
                .addFireNonPhasingSecondDay(-1));

        battle.setProvince("lyonnais");
        when(oeUtil.getArtilleryBonus(armyPhasing, militaryService.getTables(), battle.getGame())).thenReturn(10);
        when(oeUtil.getArtilleryBonus(armyNonPhasing, militaryService.getTables(), battle.getGame())).thenReturn(1);
        when(oeUtil.getCavalryBonus(armyPhasing, TerrainEnum.SPARSE_FOREST, militaryService.getTables(), battle.getGame())).thenReturn(true);
        when(oeUtil.getCavalryBonus(armyNonPhasing, TerrainEnum.SPARSE_FOREST, militaryService.getTables(), battle.getGame())).thenReturn(false);
        checkModifiers(battle, Modifiers.init(-1)
                .addFirePhasingFirstDay(1)
                .addFirePhasingSecondDay(1)
                .addShockPhasingFirstDay(2)
                .addShockPhasingSecondDay(2)
                .addFireNonPhasingFirstDay(-1)
                .addFireNonPhasingSecondDay(-1));
    }

    private void checkModifiers(BattleEntity battle, Modifiers modifiers) {
        militaryService.fillBattleModifiers(battle);

        Assert.assertEquals(modifiers.firePF, battle.getPhasing().getFirstDay().getFire());
        Assert.assertEquals(modifiers.shockPF, battle.getPhasing().getFirstDay().getShock());
        Assert.assertEquals(modifiers.pursuitPF, battle.getPhasing().getFirstDay().getPursuit());
        Assert.assertEquals(modifiers.firePS, battle.getPhasing().getSecondDay().getFire());
        Assert.assertEquals(modifiers.shockPS, battle.getPhasing().getSecondDay().getShock());
        Assert.assertEquals(modifiers.pursuitPS, battle.getPhasing().getSecondDay().getPursuit());

        Assert.assertEquals(modifiers.fireNPF, battle.getNonPhasing().getFirstDay().getFire());
        Assert.assertEquals(modifiers.shockNPF, battle.getNonPhasing().getFirstDay().getShock());
        Assert.assertEquals(modifiers.pursuitNPF, battle.getNonPhasing().getFirstDay().getPursuit());
        Assert.assertEquals(modifiers.fireNPS, battle.getNonPhasing().getSecondDay().getFire());
        Assert.assertEquals(modifiers.shockNPS, battle.getNonPhasing().getSecondDay().getShock());
        Assert.assertEquals(modifiers.pursuitNPS, battle.getNonPhasing().getSecondDay().getPursuit());
    }

    private static class Modifiers {
        /** Modifiers Phasing First day. */
        private int firePF;
        private int shockPF;
        private int pursuitPF;
        /** Modifiers Phasing Second day. */
        private int firePS;
        private int shockPS;
        private int pursuitPS;
        /** Modifiers Non Phasing First day. */
        private int fireNPF;
        private int shockNPF;
        private int pursuitNPF;
        /** Modifiers Non Phasing Second day. */
        private int fireNPS;
        private int shockNPS;
        private int pursuitNPS;

        static Modifiers init(int init) {
            Modifiers modifiers = new Modifiers();

            modifiers.firePF = init;
            modifiers.shockPF = init;
            modifiers.pursuitPF = init;
            modifiers.firePS = init - 1;
            modifiers.shockPS = init - 1;
            modifiers.pursuitPS = init;
            modifiers.fireNPF = init;
            modifiers.shockNPF = init;
            modifiers.pursuitNPF = init;
            modifiers.fireNPS = init - 1;
            modifiers.shockNPS = init - 1;
            modifiers.pursuitNPS = init;

            return modifiers;
        }

        Modifiers addFirePhasingFirstDay(int firePF) {
            this.firePF += firePF;

            return this;
        }

        Modifiers addShockPhasingFirstDay(int shockPF) {
            this.shockPF += shockPF;

            return this;
        }

        Modifiers addFirePhasingSecondDay(int firePS) {
            this.firePS += firePS;

            return this;
        }

        Modifiers addShockPhasingSecondDay(int shockPS) {
            this.shockPS += shockPS;

            return this;
        }

        Modifiers addFireNonPhasingFirstDay(int fireNPF) {
            this.fireNPF += fireNPF;

            return this;
        }

        Modifiers addShockNonPhasingFirstDay(int shockNPF) {
            this.shockNPF += shockNPF;

            return this;
        }

        Modifiers addFireNonPhasingSecondDay(int fireNPS) {
            this.fireNPS += fireNPS;

            return this;
        }

        Modifiers addShockNonPhasingSecondDay(int shockNPS) {
            this.shockNPS += shockNPS;

            return this;
        }
    }

    @Test
    public void testWithdrawBeforeBattleFail() {
        Pair<Request<WithdrawBeforeBattleRequest>, GameEntity> pair = testCheckGame(militaryService::withdrawBeforeBattle, "withdrawBeforeBattle");
        Request<WithdrawBeforeBattleRequest> request = pair.getLeft();
        GameEntity game = pair.getRight();
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(0).setStatus(BattleStatusEnum.SELECT_FORCES);
        game.getBattles().get(0).setProvince("idf");
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(1).setStatus(BattleStatusEnum.NEW);
        game.getBattles().get(1).setProvince("lyonnais");
        CountryOrderEntity order = new CountryOrderEntity();
        order.setActive(true);
        order.setGameStatus(GameStatusEnum.MILITARY_MOVE);
        order.setCountry(new PlayableCountryEntity());
        order.getCountry().setId(26L);
        game.getOrders().add(order);
        EuropeanProvinceEntity idf = new EuropeanProvinceEntity();
        idf.setId(1L);
        idf.setName("idf");
        EuropeanProvinceEntity orleans = new EuropeanProvinceEntity();
        orleans.setId(2L);
        orleans.setName("orleans");
        BorderEntity border = new BorderEntity();
        border.setProvinceFrom(idf);
        border.setProvinceTo(orleans);
        idf.getBorders().add(border);
        when(provinceDao.getProvinceByName("idf")).thenReturn(idf);
        testCheckStatus(pair.getRight(), request, militaryService::withdrawBeforeBattle, "withdrawBeforeBattle", GameStatusEnum.MILITARY_BATTLES);
        request.setIdCountry(26L);

        try {
            militaryService.withdrawBeforeBattle(request);
            Assert.fail("Should break because withdrawBeforeBattle.idCountry is the phasing player");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_ONLY_NON_PHASING_CAN_WITHDRAW, e.getCode());
            Assert.assertEquals("withdrawBeforeBattle.request.idCountry", e.getParams()[0]);
        }

        request.setIdCountry(27L);

        try {
            militaryService.withdrawBeforeBattle(request);
            Assert.fail("Should break because withdrawBeforeBattle.request is null");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("withdrawBeforeBattle.request", e.getParams()[0]);
        }

        request.setRequest(new WithdrawBeforeBattleRequest());
        request.getRequest().setWithdraw(true);

        try {
            militaryService.withdrawBeforeBattle(request);
            Assert.fail("Should break because battle is null");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_STATUS_NONE, e.getCode());
            Assert.assertEquals("withdrawBeforeBattle", e.getParams()[0]);
        }

        game.getBattles().get(0).setStatus(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE);

        try {
            militaryService.withdrawBeforeBattle(request);
            Assert.fail("Should break because province is null");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("withdrawBeforeBattle.request.provinceTo", e.getParams()[0]);
        }

        request.getRequest().setProvinceTo("");

        try {
            militaryService.withdrawBeforeBattle(request);
            Assert.fail("Should break because province is empty");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.NULL_PARAMETER, e.getCode());
            Assert.assertEquals("withdrawBeforeBattle.request.provinceTo", e.getParams()[0]);
        }

        request.getRequest().setProvinceTo("toto");

        try {
            militaryService.withdrawBeforeBattle(request);
            Assert.fail("Should break because province does not exist");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsCommonException.INVALID_PARAMETER, e.getCode());
            Assert.assertEquals("withdrawBeforeBattle.request.provinceTo", e.getParams()[0]);
        }

        request.getRequest().setProvinceTo("pecs");
        when(provinceDao.getProvinceByName("pecs")).thenReturn(new EuropeanProvinceEntity());

        try {
            militaryService.withdrawBeforeBattle(request);
            Assert.fail("Should break because province is not next to battle");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.PROVINCES_NOT_NEIGHBOR, e.getCode());
            Assert.assertEquals("withdrawBeforeBattle.request.provinceTo", e.getParams()[0]);
        }

        request.getRequest().setProvinceTo("orleans");
        when(provinceDao.getProvinceByName("orleans")).thenReturn(orleans);

        try {
            militaryService.withdrawBeforeBattle(request);
            Assert.fail("Should break because cannot retreat in this province");
        } catch (FunctionalException e) {
            Assert.assertEquals(IConstantsServiceException.BATTLE_CANT_WITHDRAW, e.getCode());
            Assert.assertEquals("withdrawBeforeBattle.request.provinceTo", e.getParams()[0]);
        }
    }

    @Test
    public void testWithdrawBeforeBattleSuccess() throws FunctionalException {
        Pair<Request<WithdrawBeforeBattleRequest>, GameEntity> pair = testCheckGame(militaryService::withdrawBeforeBattle, "withdrawBeforeBattle");
        Request<WithdrawBeforeBattleRequest> request = pair.getLeft();
        GameEntity game = pair.getRight();
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(0).setStatus(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE);
        game.getBattles().get(0).setProvince("idf");
        game.getBattles().add(new BattleEntity());
        game.getBattles().get(1).setStatus(BattleStatusEnum.NEW);
        game.getBattles().get(1).setProvince("lyonnais");
        PlayableCountryEntity country = new PlayableCountryEntity();
        country.setId(27L);
        game.getCountries().add(country);
        CountryOrderEntity order = new CountryOrderEntity();
        order.setActive(true);
        order.setGameStatus(GameStatusEnum.MILITARY_MOVE);
        order.setCountry(new PlayableCountryEntity());
        order.getCountry().setId(26L);
        game.getOrders().add(order);
        EuropeanProvinceEntity idf = new EuropeanProvinceEntity();
        idf.setId(1L);
        idf.setName("idf");
        idf.setTerrain(TerrainEnum.PLAIN);
        EuropeanProvinceEntity orleans = new EuropeanProvinceEntity();
        orleans.setId(2L);
        orleans.setName("orleans");
        orleans.setTerrain(TerrainEnum.PLAIN);
        BorderEntity border = new BorderEntity();
        border.setProvinceFrom(idf);
        border.setProvinceTo(orleans);
        idf.getBorders().add(border);
        when(provinceDao.getProvinceByName("idf")).thenReturn(idf);
        when(provinceDao.getProvinceByName("orleans")).thenReturn(orleans);
        when(oeUtil.canRetreat(any(), anyBoolean(), anyInt(), any(), any())).thenReturn(true);
        testCheckStatus(pair.getRight(), request, militaryService::withdrawBeforeBattle, "withdrawBeforeBattle", GameStatusEnum.MILITARY_BATTLES);
        request.setIdCountry(27L);
        request.setRequest(new WithdrawBeforeBattleRequest());
        request.getRequest().setWithdraw(true);
        request.getRequest().setProvinceTo("orleans");
        BattleEntity battle = game.getBattles().get(0);

        when(oeUtil.rollDie(game, country)).thenReturn(5);

        militaryService.withdrawBeforeBattle(request);

        Assert.assertTrue(battle.getEnd() != BattleEndEnum.WITHDRAW_BEFORE_BATTLE);

        battle.setStatus(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE);
        request.getRequest().setProvinceTo("idf");

        militaryService.withdrawBeforeBattle(request);

        Assert.assertTrue(battle.getEnd() == BattleEndEnum.WITHDRAW_BEFORE_BATTLE);

        battle.setStatus(BattleStatusEnum.WITHDRAW_BEFORE_BATTLE);
        request.getRequest().setProvinceTo("orleans");
        when(oeUtil.rollDie(game, country)).thenReturn(8);

        militaryService.withdrawBeforeBattle(request);

        Assert.assertTrue(battle.getEnd() == BattleEndEnum.WITHDRAW_BEFORE_BATTLE);
    }
}