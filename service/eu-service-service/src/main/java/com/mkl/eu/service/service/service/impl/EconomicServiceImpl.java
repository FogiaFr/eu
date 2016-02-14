package com.mkl.eu.service.service.service.impl;

import com.mkl.eu.client.common.exception.FunctionalException;
import com.mkl.eu.client.common.exception.IConstantsCommonException;
import com.mkl.eu.client.common.exception.TechnicalException;
import com.mkl.eu.client.common.util.CommonUtil;
import com.mkl.eu.client.common.vo.Request;
import com.mkl.eu.client.common.vo.SimpleRequest;
import com.mkl.eu.client.service.service.IConstantsServiceException;
import com.mkl.eu.client.service.service.IEconomicService;
import com.mkl.eu.client.service.service.eco.AddAdminActionRequest;
import com.mkl.eu.client.service.service.eco.EconomicalSheetCountry;
import com.mkl.eu.client.service.service.eco.LoadEcoSheetsRequest;
import com.mkl.eu.client.service.service.eco.RemoveAdminActionRequest;
import com.mkl.eu.client.service.vo.diff.Diff;
import com.mkl.eu.client.service.vo.diff.DiffResponse;
import com.mkl.eu.client.service.vo.enumeration.*;
import com.mkl.eu.client.service.vo.tables.Limit;
import com.mkl.eu.client.service.vo.tables.Tech;
import com.mkl.eu.client.service.vo.tables.Unit;
import com.mkl.eu.client.service.vo.util.MaintenanceUtil;
import com.mkl.eu.service.service.mapping.eco.EconomicalSheetMapping;
import com.mkl.eu.service.service.persistence.board.ICounterDao;
import com.mkl.eu.service.service.persistence.board.IStackDao;
import com.mkl.eu.service.service.persistence.diff.IDiffDao;
import com.mkl.eu.service.service.persistence.eco.IAdminActionDao;
import com.mkl.eu.service.service.persistence.eco.IEconomicalSheetDao;
import com.mkl.eu.service.service.persistence.oe.GameEntity;
import com.mkl.eu.service.service.persistence.oe.board.CounterEntity;
import com.mkl.eu.service.service.persistence.oe.board.StackEntity;
import com.mkl.eu.service.service.persistence.oe.country.PlayableCountryEntity;
import com.mkl.eu.service.service.persistence.oe.diff.DiffAttributesEntity;
import com.mkl.eu.service.service.persistence.oe.diff.DiffEntity;
import com.mkl.eu.service.service.persistence.oe.eco.AdministrativeActionEntity;
import com.mkl.eu.service.service.persistence.oe.eco.EconomicalSheetEntity;
import com.mkl.eu.service.service.persistence.oe.ref.province.AbstractProvinceEntity;
import com.mkl.eu.service.service.persistence.oe.ref.province.EuropeanProvinceEntity;
import com.mkl.eu.service.service.persistence.oe.ref.province.RotwProvinceEntity;
import com.mkl.eu.service.service.persistence.ref.IProvinceDao;
import com.mkl.eu.service.service.persistence.tables.ITablesDao;
import com.mkl.eu.service.service.service.GameDiffsInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the Economic Service.
 *
 * @author MKL.
 */
@Service
@Transactional(rollbackFor = {TechnicalException.class, FunctionalException.class})
public class EconomicServiceImpl extends AbstractService implements IEconomicService {
    /** Counter Face Type for armies. */
    private static final List<CounterFaceTypeEnum> ARMY_TYPES = new ArrayList<>();
    /** Counter Face Type for land armies. */
    private static final List<CounterFaceTypeEnum> ARMY_LAND_TYPES = new ArrayList<>();
    /** Counter Face Type for naval armies. */
    private static final List<CounterFaceTypeEnum> ARMY_NAVAL_TYPES = new ArrayList<>();
    /** Counter Face Type for fortresses. */
    private static final List<CounterFaceTypeEnum> FORTRESS_TYPES = new ArrayList<>();
    /** EconomicalSheet DAO. */
    @Autowired
    private IEconomicalSheetDao economicalSheetDao;
    /** AdminAction DAO. */
    @Autowired
    private IAdminActionDao adminActionDao;
    /** Tables DAO. */
    @Autowired
    private ITablesDao tablesDao;
    /** Counter DAO. */
    @Autowired
    private ICounterDao counterDao;
    /** Stack DAO. */
    @Autowired
    private IStackDao stackDao;
    /** Province DAO. */
    @Autowired
    private IProvinceDao provinceDao;
    /** Diff DAO. */
    @Autowired
    private IDiffDao diffDao;
    /** Game mapping. */
    @Autowired
    private EconomicalSheetMapping ecoSheetsMapping;

    /**
     * Filling the static List.
     */
    static {
        ARMY_TYPES.add(CounterFaceTypeEnum.ARMY_PLUS);
        ARMY_TYPES.add(CounterFaceTypeEnum.ARMY_MINUS);
        ARMY_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT);
        ARMY_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT_EXPLORATION);
        ARMY_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT_TIMAR);
        ARMY_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT_KOZAK);
        ARMY_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT_EXPLORATION_KOZAK);
        ARMY_TYPES.add(CounterFaceTypeEnum.FLEET_PLUS);
        ARMY_TYPES.add(CounterFaceTypeEnum.FLEET_MINUS);
        ARMY_TYPES.add(CounterFaceTypeEnum.NAVAL_DETACHMENT);
        ARMY_TYPES.add(CounterFaceTypeEnum.NAVAL_DETACHMENT_EXPLORATION);
        ARMY_TYPES.add(CounterFaceTypeEnum.NAVAL_GALLEY);
        ARMY_TYPES.add(CounterFaceTypeEnum.NAVAL_TRANSPORT);

        ARMY_LAND_TYPES.add(CounterFaceTypeEnum.ARMY_PLUS);
        ARMY_LAND_TYPES.add(CounterFaceTypeEnum.ARMY_MINUS);
        ARMY_LAND_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT);
        ARMY_LAND_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT_EXPLORATION);
        ARMY_LAND_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT_TIMAR);
        ARMY_LAND_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT_KOZAK);
        ARMY_LAND_TYPES.add(CounterFaceTypeEnum.LAND_DETACHMENT_EXPLORATION_KOZAK);

        ARMY_NAVAL_TYPES.add(CounterFaceTypeEnum.FLEET_PLUS);
        ARMY_NAVAL_TYPES.add(CounterFaceTypeEnum.FLEET_MINUS);
        ARMY_NAVAL_TYPES.add(CounterFaceTypeEnum.NAVAL_DETACHMENT);
        ARMY_NAVAL_TYPES.add(CounterFaceTypeEnum.NAVAL_DETACHMENT_EXPLORATION);
        ARMY_NAVAL_TYPES.add(CounterFaceTypeEnum.NAVAL_GALLEY);
        ARMY_NAVAL_TYPES.add(CounterFaceTypeEnum.NAVAL_TRANSPORT);

        FORTRESS_TYPES.add(CounterFaceTypeEnum.FORTRESS_1);
        FORTRESS_TYPES.add(CounterFaceTypeEnum.FORTRESS_2);
        FORTRESS_TYPES.add(CounterFaceTypeEnum.FORTRESS_3);
        FORTRESS_TYPES.add(CounterFaceTypeEnum.FORTRESS_4);
        FORTRESS_TYPES.add(CounterFaceTypeEnum.FORTRESS_5);
    }

    /** {@inheritDoc} */
    @Override
    public List<EconomicalSheetCountry> loadEconomicSheets(SimpleRequest<LoadEcoSheetsRequest> request) throws FunctionalException, TechnicalException {
        failIfNull(new AbstractService.CheckForThrow<>().setTest(request).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_LOAD_ECO_SHEETS).setParams(METHOD_LOAD_ECO_SHEETS));
        failIfNull(new AbstractService.CheckForThrow<>().setTest(request.getRequest()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_LOAD_ECO_SHEETS, PARAMETER_REQUEST).setParams(METHOD_LOAD_ECO_SHEETS));

        List<EconomicalSheetEntity> sheetEntities = economicalSheetDao.loadSheets(
                request.getRequest().getIdCountry(),
                request.getRequest().getTurn(),
                request.getRequest().getIdGame());

        return ecoSheetsMapping.oesToVosCountry(sheetEntities);
    }

    /** {@inheritDoc} */
    @Override
    public DiffResponse computeEconomicalSheets(Long idGame) {
        GameEntity game = gameDao.lock(idGame);

        Map<String, List<CounterFaceTypeEnum>> tradeCenters = economicalSheetDao.getTradeCenters(game.getId());

        for (PlayableCountryEntity country : game.getCountries()) {
            computeEconomicalSheet(country, game, tradeCenters);
        }

        DiffEntity diff = new DiffEntity();
        diff.setIdGame(game.getId());
        diff.setVersionGame(game.getVersion());
        diff.setType(DiffTypeEnum.INVALIDATE);
        diff.setTypeObject(DiffTypeObjectEnum.ECO_SHEET);
        DiffAttributesEntity diffAttributes = new DiffAttributesEntity();
        diffAttributes.setType(DiffAttributeTypeEnum.TURN);
        diffAttributes.setValue(Integer.toString(game.getTurn()));
        diffAttributes.setDiff(diff);
        diff.getAttributes().add(diffAttributes);

        createDiff(diff);
        List<Diff> diffs = new ArrayList<>();
        diffs.add(diffMapping.oeToVo(diff));

        DiffResponse response = new DiffResponse();
        response.setDiffs(diffs);
        response.setVersionGame(game.getVersion());

        return response;
    }

    /**
     * Compute the economical sheet of a country for the turn of the game.
     *
     * @param country      the country.
     * @param game         the game.
     * @param tradeCenters the trade centers and their owners.
     */
    private void computeEconomicalSheet(PlayableCountryEntity country, GameEntity game, Map<String, List<CounterFaceTypeEnum>> tradeCenters) {
        EconomicalSheetEntity sheet = CommonUtil.findFirst(country.getEconomicalSheets(), economicalSheetEntity -> economicalSheetEntity.getTurn().equals(game.getTurn()));
        if (sheet == null) {
            sheet = new EconomicalSheetEntity();
            sheet.setCountry(country);
            sheet.setTurn(game.getTurn());

            economicalSheetDao.create(sheet);

            country.getEconomicalSheets().add(sheet);
        }

        Long idGame = game.getId();
        String name = country.getName();

        Map<String, Integer> provinces = economicalSheetDao.getOwnedAndControlledProvinces(name, idGame);
        sheet.setProvincesIncome(provinces.values().stream().collect(Collectors.summingInt(value -> value)));

        Map<String, Integer> vassalProvinces = new HashMap<>();
        List<String> vassals = counterDao.getVassals(name, idGame);
        for (String vassal : vassals) {
            vassalProvinces.putAll(economicalSheetDao.getOwnedAndControlledProvinces(vassal, idGame));
        }
        sheet.setVassalIncome(vassalProvinces.values().stream().collect(Collectors.summingInt(value -> value)));

        List<String> provinceNames = new ArrayList<>();
        provinceNames.addAll(provinces.keySet());
        provinceNames.addAll(vassalProvinces.keySet());
        List<String> pillagedProvinces = economicalSheetDao.getPillagedProvinces(provinceNames, idGame);

        Integer pillagedIncome = pillagedProvinces.stream().collect(Collectors.summingInt(provinces::get));

        sheet.setPillages(pillagedIncome);

        sheet.setLandIncome(CommonUtil.add(sheet.getProvincesIncome(), sheet.getVassalIncome(), sheet.getPillages(), sheet.getEventLandIncome()));

        sheet.setMnuIncome(economicalSheetDao.getMnuIncome(name, pillagedProvinces, idGame));

        List<String> provincesOwnedNotPilaged = provinces.keySet().stream().filter(s -> !pillagedProvinces.contains(s)).collect(Collectors.toList());
        sheet.setGoldIncome(economicalSheetDao.getGoldIncome(provincesOwnedNotPilaged, idGame));

        sheet.setIndustrialIncome(CommonUtil.add(sheet.getMnuIncome(), sheet.getGoldIncome()));

        sheet.setDomTradeIncome(tablesDao.getTradeIncome(CommonUtil.add(sheet.getProvincesIncome(), sheet.getVassalIncome()), country.getDti(), false));

        // TODO needs War to know the blocked trade
        sheet.setForTradeIncome(tablesDao.getTradeIncome(0, country.getFti(), true));

        sheet.setFleetLevelIncome(economicalSheetDao.getFleetLevelIncome(name, idGame));

        sheet.setFleetMonopIncome(economicalSheetDao.getFleetLevelMonopoly(name, idGame));

        Integer tradeCentersIncome = 0;

        if (tradeCenters.get(name) != null) {
            for (CounterFaceTypeEnum tradeCenter : tradeCenters.get(name)) {
                if (tradeCenter == CounterFaceTypeEnum.TRADE_CENTER_ATLANTIC) {
                    tradeCentersIncome += 100;
                } else if (tradeCenter == CounterFaceTypeEnum.TRADE_CENTER_MEDITERRANEAN) {
                    tradeCentersIncome += 100;
                } else if (tradeCenter == CounterFaceTypeEnum.TRADE_CENTER_INDIAN) {
                    tradeCentersIncome += 50;
                }
            }
        }

        sheet.setTradeCenterIncome(tradeCentersIncome);

        Integer sum = CommonUtil.add(sheet.getDomTradeIncome(), sheet.getForTradeIncome(), sheet.getFleetLevelIncome(), sheet.getFleetMonopIncome(), sheet.getTradeCenterIncome());
        if (sheet.getTradeCenterLoss() != null) {
            sum -= sheet.getTradeCenterLoss();
        }
        sheet.setTradeIncome(sum);

        Pair<Integer, Integer> colTpIncome = economicalSheetDao.getColTpIncome(name, idGame);
        sheet.setColIncome(colTpIncome.getLeft());
        sheet.setTpIncome(colTpIncome.getRight());
        sheet.setExoResIncome(economicalSheetDao.getExoResIncome(name, idGame));

        sheet.setRotwIncome(CommonUtil.add(sheet.getColIncome(), sheet.getTpIncome(), sheet.getExoResIncome()));

        sheet.setIncome(CommonUtil.add(sheet.getLandIncome(), sheet.getIndustrialIncome(), sheet.getTradeIncome(), sheet.getRotwIncome(), sheet.getSpecialIncome()));

        sheet.setGrossIncome(CommonUtil.add(sheet.getIncome(), sheet.getEventIncome()));
    }

    /** {@inheritDoc} */
    @Override
    public DiffResponse addAdminAction(Request<AddAdminActionRequest> request) throws FunctionalException, TechnicalException {
        failIfNull(new AbstractService.CheckForThrow<>().setTest(request).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_ADD_ADM_ACT).setParams(METHOD_ADD_ADM_ACT));
        failIfNull(new AbstractService.CheckForThrow<>().setTest(request.getAuthent()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_AUTHENT).setParams(METHOD_ADD_ADM_ACT));

        GameDiffsInfo gameDiffs = checkGameAndGetDiffs(request.getGame(), METHOD_ADD_ADM_ACT, PARAMETER_ADD_ADM_ACT);
        GameEntity game = gameDiffs.getGame();

        failIfNull(new AbstractService.CheckForThrow<>().setTest(request.getRequest()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST).setParams(METHOD_ADD_ADM_ACT));

        failIfNull(new AbstractService.CheckForThrow<>().setTest(request.getRequest().getIdCountry()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_COUNTRY).setParams(METHOD_ADD_ADM_ACT));

        PlayableCountryEntity country = CommonUtil.findFirst(game.getCountries(), c -> c.getId().equals(request.getRequest().getIdCountry()));

        failIfNull(new AbstractService.CheckForThrow<>().setTest(country).setCodeError(IConstantsCommonException.INVALID_PARAMETER)
                .setMsgFormat(MSG_OBJECT_NOT_FOUND).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_COUNTRY).setParams(METHOD_ADD_ADM_ACT, request.getRequest().getIdCountry()));

        failIfNull(new AbstractService.CheckForThrow<>().setTest(request.getRequest().getType()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_TYPE).setParams(METHOD_ADD_ADM_ACT));

        AdministrativeActionEntity admAct;
        switch (request.getRequest().getType()) {
            case LM:
                // TODO check if country is at war
            case DIS:
                admAct = computeDisbandLowMaintenance(request, game, country);
                break;
            case PU:
                admAct = computePurchase(request, game, country);
                break;
            default:
                admAct = null;
                break;
        }

        List<DiffEntity> diffs = gameDiffs.getDiffs();

        if (admAct != null) {
            admAct.setType(request.getRequest().getType());
            adminActionDao.create(admAct);

            DiffEntity diff = new DiffEntity();
            diff.setIdGame(game.getId());
            diff.setVersionGame(game.getVersion());
            diff.setType(DiffTypeEnum.ADD);
            diff.setTypeObject(DiffTypeObjectEnum.ADM_ACT);
            diff.setIdObject(admAct.getId());
            DiffAttributesEntity diffAttributes = new DiffAttributesEntity();
            diffAttributes.setType(DiffAttributeTypeEnum.ID_COUNTRY);
            diffAttributes.setValue(admAct.getCountry().getId().toString());
            diffAttributes.setDiff(diff);
            diff.getAttributes().add(diffAttributes);
            diffAttributes = new DiffAttributesEntity();
            diffAttributes.setType(DiffAttributeTypeEnum.TURN);
            diffAttributes.setValue(admAct.getTurn().toString());
            diffAttributes.setDiff(diff);
            diff.getAttributes().add(diffAttributes);
            diffAttributes = new DiffAttributesEntity();
            diffAttributes.setType(DiffAttributeTypeEnum.TYPE);
            diffAttributes.setValue(admAct.getType().name());
            diffAttributes.setDiff(diff);
            diff.getAttributes().add(diffAttributes);
            if (admAct.getCost() != null) {
                diffAttributes = new DiffAttributesEntity();
                diffAttributes.setType(DiffAttributeTypeEnum.COST);
                diffAttributes.setValue(admAct.getCost().toString());
                diffAttributes.setDiff(diff);
                diff.getAttributes().add(diffAttributes);
            }
            if (admAct.getIdObject() != null) {
                diffAttributes = new DiffAttributesEntity();
                diffAttributes.setType(DiffAttributeTypeEnum.ID_OBJECT);
                diffAttributes.setValue(admAct.getIdObject().toString());
                diffAttributes.setDiff(diff);
                diff.getAttributes().add(diffAttributes);
            }
            if (admAct.getProvince() != null) {
                diffAttributes = new DiffAttributesEntity();
                diffAttributes.setType(DiffAttributeTypeEnum.PROVINCE);
                diffAttributes.setValue(admAct.getProvince());
                diffAttributes.setDiff(diff);
                diff.getAttributes().add(diffAttributes);
            }
            if (admAct.getCounterFaceType() != null) {
                diffAttributes = new DiffAttributesEntity();
                diffAttributes.setType(DiffAttributeTypeEnum.COUNTER_FACE_TYPE);
                diffAttributes.setValue(admAct.getCounterFaceType().name());
                diffAttributes.setDiff(diff);
                diff.getAttributes().add(diffAttributes);
            }

            diffs.add(diff);
        }

        DiffResponse response = new DiffResponse();
        response.setDiffs(diffMapping.oesToVos(diffs));
        response.setVersionGame(game.getVersion());

        response.setMessages(getMessagesSince(request));

        return response;
    }

    /**
     * Computes the creation of a PLANNED administrative action of type PURCHASE.
     *
     * @param request request containing the info about the action to create.
     * @param game    in which the action will be created.
     * @param country owner of the action.
     * @return the administrative action to create.
     * @throws FunctionalException
     */
    private AdministrativeActionEntity computeDisbandLowMaintenance(Request<AddAdminActionRequest> request, GameEntity game, PlayableCountryEntity country) throws FunctionalException {
        failIfNull(new CheckForThrow<>().setTest(request.getRequest().getIdObject()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_OBJECT).setParams(METHOD_ADD_ADM_ACT));
        CounterEntity counter = CommonUtil.findFirst(game.getStacks().stream().flatMap(stack -> stack.getCounters().stream()),
                c -> c.getId().equals(request.getRequest().getIdObject()));
        failIfNull(new CheckForThrow<>().setTest(counter).setCodeError(IConstantsCommonException.INVALID_PARAMETER)
                .setMsgFormat(MSG_OBJECT_NOT_FOUND).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_OBJECT).setParams(METHOD_ADD_ADM_ACT, request.getRequest().getIdObject()));
        failIfFalse(new CheckForThrow<Boolean>().setTest(StringUtils.equals(counter.getCountry(), country.getName())).setCodeError(IConstantsServiceException.COUNTER_NOT_OWNED)
                .setMsgFormat("{1}: {0} The counter {2} is not owned by the country {3}.").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_OBJECT).setParams(METHOD_ADD_ADM_ACT, request.getRequest().getIdObject(), request.getRequest().getIdCountry()));
        if (request.getRequest().getType() == AdminActionTypeEnum.LM) {
            failIfFalse(new CheckForThrow<Boolean>().setTest(ARMY_LAND_TYPES.contains(counter.getType())).setCodeError(IConstantsServiceException.COUNTER_CANT_MAINTAIN_LOW)
                    .setMsgFormat("{1}: {0} The counter {2} has the type {3} which cannot be maintained low.").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_OBJECT).setParams(METHOD_ADD_ADM_ACT, request.getRequest().getIdObject(), counter.getType()));
        } else {
            failIfFalse(new CheckForThrow<Boolean>().setTest(ARMY_TYPES.contains(counter.getType())).setCodeError(IConstantsServiceException.COUNTER_CANT_DISBAND)
                    .setMsgFormat("{1}: {0} The counter {2} has the type {3} which cannot be disbanded.").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_OBJECT).setParams(METHOD_ADD_ADM_ACT, request.getRequest().getIdObject(), counter.getType()));
        }

        List<AdministrativeActionEntity> actions = adminActionDao.findAdminActions(request.getRequest().getIdCountry(), game.getTurn(),
                request.getRequest().getIdObject(), AdminActionTypeEnum.LM, AdminActionTypeEnum.DIS);
        failIfFalse(new CheckForThrow<Boolean>().setTest(actions == null || actions.isEmpty()).setCodeError(IConstantsServiceException.COUNTER_ALREADY_PLANNED)
                .setMsgFormat("{1}: {0} The counter {2} has already a DIS or LM administrative action PLANNED this turn.").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_OBJECT).setParams(METHOD_ADD_ADM_ACT, request.getRequest().getIdObject()));


        AdministrativeActionEntity admAct = new AdministrativeActionEntity();
        admAct.setCountry(country);
        admAct.setStatus(AdminActionStatusEnum.PLANNED);
        admAct.setTurn(game.getTurn());
        admAct.setIdObject(counter.getId());
        return admAct;
    }

    /**
     * Computes the creation of a PLANNED administrative action of type PURCHASE.
     *
     * @param request request containing the info about the action to create.
     * @param game    in which the action will be created.
     * @param country owner of the action.
     * @return the administrative action to create.
     * @throws FunctionalException
     */
    private AdministrativeActionEntity computePurchase(Request<AddAdminActionRequest> request, GameEntity game, PlayableCountryEntity country) throws FunctionalException {
        String province = request.getRequest().getProvince();
        CounterFaceTypeEnum faceType = request.getRequest().getCounterFaceType();
        failIfNull(new CheckForThrow<>().setTest(faceType).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_COUNTER_FACE_TYPE).setParams(METHOD_ADD_ADM_ACT));
        failIfEmpty(new CheckForThrow<String>().setTest(province).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_PROVINCE).setParams(METHOD_ADD_ADM_ACT));

        AbstractProvinceEntity prov = provinceDao.getProvinceByName(province);

        failIfNull(new CheckForThrow<>().setTest(prov).setCodeError(IConstantsCommonException.INVALID_PARAMETER)
                .setMsgFormat(MSG_OBJECT_NOT_FOUND).setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_PROVINCE).setParams(METHOD_ADD_ADM_ACT, province));

        List<StackEntity> stacks = stackDao.getStacksOnProvince(province, game.getId());
        String owner = null;

        boolean port = false;
        int fortressLevel = 0;

        if (prov instanceof EuropeanProvinceEntity) {
            EuropeanProvinceEntity euProv = (EuropeanProvinceEntity) prov;
            owner = euProv.getDefaultOwner();
            if (euProv.isPort() != null) {
                port = euProv.isPort();
            }
            if (!port && euProv.isArsenal() != null) {
                port = euProv.isArsenal();
            }
            if (euProv.getFortress() != null) {
                fortressLevel = euProv.getFortress();
            }
        }

        boolean isFortress = FORTRESS_TYPES.contains(faceType);

        CounterEntity ownCounter = CommonUtil.findFirst(stacks.stream().flatMap(stack -> stack.getCounters().stream()), counter -> counter.getType() == CounterFaceTypeEnum.OWN);
        if (ownCounter != null) {
            owner = ownCounter.getCountry();
        }

        boolean provinceOk = StringUtils.equals(country.getName(), owner);

        if (provinceOk || isFortress) {
            CounterEntity ctrlCounter = CommonUtil.findFirst(stacks.stream().flatMap(stack -> stack.getCounters().stream()), counter -> counter.getType() == CounterFaceTypeEnum.CONTROL);
            if (ctrlCounter != null) {
                provinceOk = StringUtils.equals(country.getName(), ctrlCounter.getCountry());
            }
        }

        failIfFalse(new CheckForThrow<Boolean>().setTest(provinceOk).setCodeError(IConstantsServiceException.PROVINCE_NOT_OWN_CONTROL)
                .setMsgFormat("{1}: {0} The province {2} is not owned and controlled by {3}.").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_PROVINCE).setParams(METHOD_ADD_ADM_ACT, province, country.getName()));

        boolean faceConsistent = isFortress;

        if (port) {
            faceConsistent |= ARMY_TYPES.contains(faceType);
        } else {
            faceConsistent |= ARMY_LAND_TYPES.contains(faceType);
        }

        failIfFalse(new CheckForThrow<Boolean>().setTest(faceConsistent).setCodeError(IConstantsServiceException.COUNTER_CANT_PURCHASE)
                .setMsgFormat("{1}: {0} The counter face type {2} cannot be purchased on province {3}.").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_COUNTER_FACE_TYPE).setParams(METHOD_ADD_ADM_ACT, faceType, province));

        boolean land = ARMY_LAND_TYPES.contains(faceType);
        final List<CounterFaceTypeEnum> faces;
        final LimitTypeEnum limitType;
        if (land) {
            faces = ARMY_LAND_TYPES;
            limitType = LimitTypeEnum.PURCHASE_LAND_TROOPS;
        } else {
            faces = ARMY_NAVAL_TYPES;
            limitType = LimitTypeEnum.PURCHASE_NAVAL_TROOPS;
        }

        Integer fortressCost = 25 * (MaintenanceUtil.getFortressLevelFromType(faceType) - 1);
        if (isFortress) {
            int actualLevel = fortressLevel;
            CounterEntity fortressCounter = CommonUtil.findFirst(stacks.stream().flatMap(stack -> stack.getCounters().stream()), counter -> StringUtils.equals(country.getName(), counter.getCountry()) && FORTRESS_TYPES.contains(counter.getType()));
            if (fortressCounter != null) {
                actualLevel = MaintenanceUtil.getFortressLevelFromType(fortressCounter.getType());
            }

            int desiredLevel = MaintenanceUtil.getFortressLevelFromType(faceType);


            Tech actualTech = CommonUtil.findFirst(getTables().getTechs(), tech -> StringUtils.equals(tech.getName(), country.getLandTech()));
            boolean canPurchaseFortress = desiredLevel != 0 && actualTech != null;
            final String fortressTech;
            if (desiredLevel == 2) {
                fortressTech = Tech.MEDIEVAL;
            } else if (desiredLevel == 3) {
                fortressTech = Tech.RENAISSANCE;
            } else if (desiredLevel == 4 || desiredLevel == 5) {
                fortressTech = Tech.BAROQUE;
            } else {
                fortressTech = null;
            }
            Tech targetTech = CommonUtil.findFirst(getTables().getTechs(), tech -> StringUtils.equals(tech.getName(), fortressTech));
            if (actualTech != null && targetTech != null) {
                canPurchaseFortress = actualTech.getBeginTurn() >= targetTech.getBeginTurn();
            }
            if (desiredLevel == 5) {
                canPurchaseFortress = game.getTurn() >= 40;
            }

            failIfFalse(new CheckForThrow<Boolean>().setTest(desiredLevel == actualLevel + 1).setCodeError(IConstantsServiceException.COUNTER_CANT_PURCHASE)
                    .setMsgFormat("{1}: {0} The counter face type {2} cannot be purchased on province {3}.").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_COUNTER_FACE_TYPE).setParams(METHOD_ADD_ADM_ACT, faceType, province));


            failIfFalse(new CheckForThrow<Boolean>().setTest(canPurchaseFortress).setCodeError(IConstantsServiceException.FORTRESS_CANT_PURCHASE)
                    .setMsgFormat("{1}: {0} The fortress {2} cannot be purchased because actual technology is {3}.").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_COUNTER_FACE_TYPE).setParams(METHOD_ADD_ADM_ACT, faceType, country.getLandTech()));

            if (prov instanceof RotwProvinceEntity) {
                // Fortress 1 costs 25 ducats in rotw (not pertinent in Europe)
                if (fortressCost == 0) {
                    fortressCost = 25;
                } else {
                    fortressCost *= 2;
                }
            }
            boolean doubleCost = (desiredLevel == 4 && game.getTurn() < 40);
            if (desiredLevel == 3) {
                Tech arquebusTech = CommonUtil.findFirst(getTables().getTechs(), tech -> StringUtils.equals(tech.getName(), Tech.ARQUEBUS));
                if (actualTech != null && arquebusTech != null) {
                    doubleCost = actualTech.getBeginTurn() < arquebusTech.getBeginTurn();
                }
            }

            if (doubleCost) {
                fortressCost *= 2;
            }
        }

        List<AdministrativeActionEntity> actions = adminActionDao.findAdminActions(request.getRequest().getIdCountry(), game.getTurn(),
                null, AdminActionTypeEnum.PU);

        Integer plannedSize = actions.stream().filter(action -> faces.contains(action.getCounterFaceType())).collect(Collectors.summingInt(action -> MaintenanceUtil.getSizeFromType(action.getCounterFaceType())));
        Integer size = MaintenanceUtil.getSizeFromType(faceType);
        Integer maxPurchase = getTables().getLimits().stream().filter(
                limit -> StringUtils.equals(limit.getCountry(), country.getName()) &&
                        limit.getType() == limitType &&
                        limit.getPeriod().getBegin() <= game.getTurn() &&
                        limit.getPeriod().getEnd() >= game.getTurn()).collect(Collectors.summingInt(Limit::getNumber));

        boolean purchaseOk = (land && plannedSize + size <= 3 * maxPurchase) || (!land && plannedSize + size <= maxPurchase);

        failIfFalse(new CheckForThrow<Boolean>().setTest(purchaseOk).setCodeError(IConstantsServiceException.PURCHASE_LIMIT_EXCEED)
                .setMsgFormat("{1}: {0} The counter face type {2} cannot be purchased because country limits were exceeded ({3}/{4}).").setName(PARAMETER_ADD_ADM_ACT, PARAMETER_REQUEST, PARAMETER_COUNTER_FACE_TYPE).setParams(METHOD_ADD_ADM_ACT, faceType, plannedSize, maxPurchase));

        ForceTypeEnum type = MaintenanceUtil.getPurchaseForceFromFace(faceType);
        Unit unitCost = CommonUtil.findFirst(getTables().getUnits(), unit -> StringUtils.equals(country.getName(), unit.getCountry()) &&
                        !unit.isSpecial() &&
                        unit.getAction() == UnitActionEnum.PURCHASE &&
                        unit.getType() == type &&
                        (StringUtils.equals(unit.getTech().getName(), country.getLandTech()) || StringUtils.equals(unit.getTech().getName(), country.getNavalTech()))
        );

        AdministrativeActionEntity admAct = new AdministrativeActionEntity();
        admAct.setCountry(country);
        admAct.setStatus(AdminActionStatusEnum.PLANNED);
        admAct.setTurn(game.getTurn());
        admAct.setProvince(province);
        admAct.setCounterFaceType(faceType);
        if (unitCost != null) {
            int cost = MaintenanceUtil.getPurchasePrice(plannedSize, maxPurchase, unitCost.getPrice(), size);
            admAct.setCost(cost);
        }
        if (isFortress && fortressCost != null) {
            admAct.setCost(fortressCost);
        }
        return admAct;
    }

    /** {@inheritDoc} */
    @Override
    public DiffResponse removeAdminAction(Request<RemoveAdminActionRequest> request) throws FunctionalException, TechnicalException {
        failIfNull(new AbstractService.CheckForThrow<>().setTest(request).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_REMOVE_ADM_ACT).setParams(METHOD_REMOVE_ADM_ACT));
        failIfNull(new AbstractService.CheckForThrow<>().setTest(request.getAuthent()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_REMOVE_ADM_ACT, PARAMETER_AUTHENT).setParams(METHOD_REMOVE_ADM_ACT));

        GameDiffsInfo gameDiffs = checkGameAndGetDiffs(request.getGame(), METHOD_REMOVE_ADM_ACT, PARAMETER_REMOVE_ADM_ACT);
        GameEntity game = gameDiffs.getGame();

        failIfNull(new AbstractService.CheckForThrow<>().setTest(request.getRequest()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_REMOVE_ADM_ACT, PARAMETER_REQUEST).setParams(METHOD_REMOVE_ADM_ACT));

        failIfNull(new AbstractService.CheckForThrow<>().setTest(request.getRequest().getIdAdmAct()).setCodeError(IConstantsCommonException.NULL_PARAMETER)
                .setMsgFormat(MSG_MISSING_PARAMETER).setName(PARAMETER_REMOVE_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_ADM_ACT).setParams(METHOD_REMOVE_ADM_ACT));

        AdministrativeActionEntity action = adminActionDao.load(request.getRequest().getIdAdmAct());

        failIfNull(new AbstractService.CheckForThrow<>().setTest(action).setCodeError(IConstantsCommonException.INVALID_PARAMETER)
                .setMsgFormat(MSG_OBJECT_NOT_FOUND).setName(PARAMETER_REMOVE_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_ADM_ACT).setParams(METHOD_REMOVE_ADM_ACT, request.getRequest().getIdAdmAct()));

        failIfFalse(new CheckForThrow<Boolean>().setTest(StringUtils.equals(request.getAuthent().getUsername(), action.getCountry().getUsername()))
                .setCodeError(IConstantsCommonException.ACCESS_RIGHT)
                .setMsgFormat(MSG_ACCESS_RIGHT).setName(PARAMETER_REMOVE_ADM_ACT, PARAMETER_AUTHENT, PARAMETER_USERNAME).setParams(METHOD_MOVE_COUNTER, request.getAuthent().getUsername(), action.getCountry().getUsername()));

        failIfFalse(new CheckForThrow<Boolean>().setTest(action.getStatus() == AdminActionStatusEnum.PLANNED).setCodeError(IConstantsServiceException.ACTION_NOT_PLANNED)
                .setMsgFormat("{1}: {0} The administrative action {2} is not PLANNED and cannot be removed.").setName(PARAMETER_REMOVE_ADM_ACT, PARAMETER_REQUEST, PARAMETER_ID_ADM_ACT).setParams(METHOD_REMOVE_ADM_ACT, request.getRequest().getIdAdmAct()));

        adminActionDao.delete(action);

        List<DiffEntity> diffs = gameDiffs.getDiffs();

        DiffEntity diff = new DiffEntity();
        diff.setIdGame(game.getId());
        diff.setVersionGame(game.getVersion());
        diff.setType(DiffTypeEnum.REMOVE);
        diff.setTypeObject(DiffTypeObjectEnum.ADM_ACT);
        diff.setIdObject(action.getId());
        DiffAttributesEntity diffAttributes = new DiffAttributesEntity();
        diffAttributes.setType(DiffAttributeTypeEnum.ID_COUNTRY);
        diffAttributes.setValue(action.getCountry().getId().toString());
        diffAttributes.setDiff(diff);
        diff.getAttributes().add(diffAttributes);
        diffAttributes = new DiffAttributesEntity();
        diffAttributes.setType(DiffAttributeTypeEnum.TYPE);
        diffAttributes.setValue(action.getType().name());
        diffAttributes.setDiff(diff);
        diff.getAttributes().add(diffAttributes);

        diffs.add(diff);

        DiffResponse response = new DiffResponse();
        response.setDiffs(diffMapping.oesToVos(diffs));
        response.setVersionGame(game.getVersion());

        response.setMessages(getMessagesSince(request));

        return response;
    }

    /** {@inheritDoc} */
    @Override
    public DiffResponse computeAdminActions(Long idGame) throws FunctionalException, TechnicalException {
        return null;
    }
}
