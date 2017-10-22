package com.mkl.eu.service.service.service;

import com.mkl.eu.client.common.exception.FunctionalException;
import com.mkl.eu.client.common.exception.IConstantsCommonException;
import com.mkl.eu.client.common.vo.Request;
import com.mkl.eu.client.service.service.IConstantsServiceException;
import com.mkl.eu.client.service.service.common.ValidateRequest;
import com.mkl.eu.client.service.service.military.ChooseBattleRequest;
import com.mkl.eu.client.service.service.military.SelectForceRequest;
import com.mkl.eu.client.service.vo.diff.DiffResponse;
import com.mkl.eu.client.service.vo.enumeration.*;
import com.mkl.eu.service.service.persistence.oe.GameEntity;
import com.mkl.eu.service.service.persistence.oe.board.CounterEntity;
import com.mkl.eu.service.service.persistence.oe.board.StackEntity;
import com.mkl.eu.service.service.persistence.oe.country.PlayableCountryEntity;
import com.mkl.eu.service.service.persistence.oe.diff.DiffEntity;
import com.mkl.eu.service.service.persistence.oe.diplo.CountryOrderEntity;
import com.mkl.eu.service.service.persistence.oe.military.BattleCounterEntity;
import com.mkl.eu.service.service.persistence.oe.military.BattleEntity;
import com.mkl.eu.service.service.service.impl.MilitaryServiceImpl;
import com.mkl.eu.service.service.util.IOEUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        Mockito.when(oeUtil.getAllies(game.getCountries().get(0), game)).thenReturn(allies);
        Mockito.when(oeUtil.getEnemies(game.getCountries().get(0), game)).thenReturn(enemies);

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

        Mockito.when(oeUtil.getAllies(null, game)).thenReturn(Collections.singletonList("france"));
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

        Mockito.when(oeUtil.getAllies(country, game)).thenReturn(Collections.singletonList(country.getName()));

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

        Mockito.when(oeUtil.getAllies(country, game)).thenReturn(Collections.singletonList(country.getName()));

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
        game.getBattles().get(0).getNonPhasing().setForces(before);
        game.getBattles().get(0).getPhasing().setForces(before);
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

        Mockito.when(oeUtil.getAllies(country, game)).thenReturn(Collections.singletonList(country.getName()));

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
            Assert.assertEquals(1, diffEntity.getAttributes().size());
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
        } else {
            Assert.assertNull(diffEntity);
        }

        Assert.assertEquals(game.getVersion(), response.getVersionGame().longValue());
        Assert.assertEquals(getDiffAfter(), response.getDiffs());
    }
}
