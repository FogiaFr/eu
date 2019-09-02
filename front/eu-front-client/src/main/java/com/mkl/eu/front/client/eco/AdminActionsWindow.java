package com.mkl.eu.front.client.eco;

import com.mkl.eu.client.common.util.CommonUtil;
import com.mkl.eu.client.service.service.IEconomicService;
import com.mkl.eu.client.service.service.common.ValidateRequest;
import com.mkl.eu.client.service.service.eco.AddAdminActionRequest;
import com.mkl.eu.client.service.service.eco.RemoveAdminActionRequest;
import com.mkl.eu.client.service.util.CounterUtil;
import com.mkl.eu.client.service.util.GameUtil;
import com.mkl.eu.client.service.util.MaintenanceUtil;
import com.mkl.eu.client.service.vo.Game;
import com.mkl.eu.client.service.vo.board.Counter;
import com.mkl.eu.client.service.vo.country.PlayableCountry;
import com.mkl.eu.client.service.vo.diff.Diff;
import com.mkl.eu.client.service.vo.diff.DiffAttributes;
import com.mkl.eu.client.service.vo.eco.AdministrativeAction;
import com.mkl.eu.client.service.vo.eco.Competition;
import com.mkl.eu.client.service.vo.eco.CompetitionRound;
import com.mkl.eu.client.service.vo.enumeration.*;
import com.mkl.eu.client.service.vo.ref.country.CountryReferential;
import com.mkl.eu.client.service.vo.tables.BasicForce;
import com.mkl.eu.client.service.vo.tables.Limit;
import com.mkl.eu.client.service.vo.tables.Tech;
import com.mkl.eu.client.service.vo.tables.Unit;
import com.mkl.eu.front.client.event.AbstractDiffListenerContainer;
import com.mkl.eu.front.client.main.GameConfiguration;
import com.mkl.eu.front.client.main.GlobalConfiguration;
import com.mkl.eu.front.client.map.marker.CounterMarker;
import com.mkl.eu.front.client.map.marker.IMapMarker;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mkl.eu.client.common.util.CommonUtil.add;
import static com.mkl.eu.client.common.util.CommonUtil.findFirst;

/**
 * Window containing the administrative actions.
 *
 * @author MKL.
 */
@Component
@Scope(value = "prototype")
public class AdminActionsWindow extends AbstractDiffListenerContainer {
    /** Economic service. */
    @Autowired
    private IEconomicService economicService;
    /** Internationalisation. */
    @Autowired
    private MessageSource message;
    /** Configuration of the application. */
    @Autowired
    private GlobalConfiguration globalConfiguration;
    /** Game. */
    private Game game;
    /** Markers of the loaded game. */
    private List<IMapMarker> markers;
    /** Global node. */
    private TabPane tabPane;

    /********************************************/
    /**        Nodes about maintenance          */
    /********************************************/
    /** The TitledPane containing all the other nodes. */
    private TitledPane maintenancePane;
    /** The TableView containing the already planned actions. */
    private TableView<AdministrativeAction> maintenanceTable;
    /** The ChoiceBox containing the remaining counters. */
    private ChoiceBox<Counter> maintenanceCountersChoice;

    /********************************************/
    /**        Nodes about purchase             */
    /********************************************/
    /** The TitledPane containing all the other nodes. */
    private TitledPane purchasePane;
    /** The TableView containing the already planned actions. */
    private TableView<AdministrativeAction> purchaseTable;

    /********************************************/
    /**        Nodes about TFI                  */
    /********************************************/
    /** The TitledPane containing all the other nodes. */
    private TitledPane tfiPane;
    /** The TableView containing the already planned actions. */
    private TableView<AdministrativeAction> tfiTable;

    /********************************************/
    /**        Nodes about Domestic Operation   */
    /********************************************/
    /** The TitledPane containing all the other nodes. */
    private TitledPane domesticPane;
    /** The TableView containing the already planned actions. */
    private TableView<AdministrativeAction> domesticTable;

    /********************************************/
    /**        Nodes about Establishments       */
    /********************************************/
    /** The TitledPane containing all the other nodes. */
    private TitledPane establishmentPane;
    /** The TableView containing the already planned actions. */
    private TableView<AdministrativeAction> establishmentTable;

    /********************************************/
    /**        Nodes about Technology           */
    /********************************************/
    /** The TitledPane containing all the other nodes. */
    private TitledPane technologyPane;
    /** The TableView containing the already planned actions. */
    private TableView<AdministrativeAction> technologyTable;

    /********************************************/
    /**        Nodes about past actions         */
    /********************************************/
    /** The selected country. */
    private ChoiceBox<PlayableCountry> choiceListCountry;
    /** The selected turn. */
    private ChoiceBox<Integer> choiceListTurn;
    /** The tableList with past actions. */
    private TableView<AdministrativeAction> tableList;

    /********************************************/
    /**    Nodes about past competitions        */
    /********************************************/
    /** The selected turn. */
    private ChoiceBox<Integer> choiceCompetitionTurn;
    /** The selected competition. */
    private ChoiceBox<Competition> choiceCompetition;
    /** The tableList with past actions. */
    private TableView<CompetitionRound> tableCompetition;


    /**
     * Constructor.
     *
     * @param game       the game to set.
     * @param gameConfig the gameConfig to set.
     */
    public AdminActionsWindow(Game game, List<IMapMarker> markers, GameConfiguration gameConfig) {
        super(gameConfig);
        this.game = game;
        this.markers = markers;
    }

    /** @return the tabPane. */
    public TabPane getTabPane() {
        return tabPane;
    }

    /**
     * Initialize the window.
     */
    @PostConstruct
    public void init() {
        tabPane = new TabPane();
        PlayableCountry country = CommonUtil.findFirst(game.getCountries(), playableCountry -> playableCountry.getId().equals(gameConfig.getIdCountry()));
        if (country != null) {
            tabPane.getTabs().add(createAdminForm(country));
        }
        tabPane.getTabs().add(createAdminList(country));
        tabPane.getTabs().add(createCompetition());
    }

    /**
     * Creates the tab for the form of the current administrative actions.
     *
     * @param country currently logged (<ocde>null</ocde> if not in this game).
     * @return the tab for the form of the current administrative actions.
     */
    private Tab createAdminForm(PlayableCountry country) {
        Tab tab = new Tab(message.getMessage("admin_action.form", null, globalConfiguration.getLocale()));
        tab.setClosable(false);

        Node unitMaintenancePane = createMaintenanceNode(country);

        Node unitPurchasePane = createPurchaseNode(country);
        Node tfiPane = createTfiNode(country);
        Node domesticPane = createDomesticOperationNode(country);
        Node establishmentPane = createEstablishmentNode(country);
        Node technologyPane = createTechnologyNode(country);
        Node actions = createActionsNode();

        VBox vBox = new VBox();
        vBox.getChildren().addAll(unitMaintenancePane, unitPurchasePane, tfiPane, domesticPane, establishmentPane, technologyPane, actions);

        tab.setContent(vBox);

        return tab;
    }

    /**
     * Create the node for the unit maintenance.
     *
     * @param country of the current player.
     * @return the node for the unit maintenance.
     */
    private Node createMaintenanceNode(PlayableCountry country) {
        maintenancePane = new TitledPane();
        maintenancePane.setExpanded(false);

        maintenanceTable = new TableView<>();
        configureAdminActionTable(maintenanceTable, this::removeAdminAction);

        HBox hBox = new HBox();

        maintenanceCountersChoice = new ChoiceBox<>();
        maintenanceCountersChoice.converterProperty().set(new StringConverter<Counter>() {
            /** {@inheritDoc} */
            @Override
            public String toString(Counter object) {
                return object.getOwner().getProvince() + " - " + object.getType();
            }

            /** {@inheritDoc} */
            @Override
            public Counter fromString(String string) {
                return null;
            }
        });

        ChoiceBox<AdminActionTypeEnum> choiceType = new ChoiceBox<>();
        choiceType.converterProperty().set(new StringConverter<AdminActionTypeEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(AdminActionTypeEnum object) {
                return message.getMessage("admin_action.type." + object, null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public AdminActionTypeEnum fromString(String string) {
                return null;
            }
        });

        ChoiceBox<CounterFaceTypeEnum> toCounterChoice = new ChoiceBox<>();
        toCounterChoice.setVisible(false);
        toCounterChoice.converterProperty().set(new StringConverter<CounterFaceTypeEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(CounterFaceTypeEnum object) {
                return message.getMessage(object + "", null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public CounterFaceTypeEnum fromString(String string) {
                return null;
            }
        });

        choiceType.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != oldValue) {
                if (newValue == AdminActionTypeEnum.LF) {
                    toCounterChoice.setVisible(true);
                    Counter start = maintenanceCountersChoice.getSelectionModel().getSelectedItem();
                    IMapMarker province = CommonUtil.findFirst(markers, marker -> StringUtils.equals(marker.getId(), start.getOwner().getProvince()));
                    List<CounterFaceTypeEnum> fortressTypes = getLowerFortresses(start.getType(), province.getFortressLevel());
                    toCounterChoice.setItems(FXCollections.observableArrayList(fortressTypes));
                } else {
                    toCounterChoice.setItems(FXCollections.observableArrayList());
                    toCounterChoice.setVisible(false);
                }
            }
        });

        maintenanceCountersChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != oldValue) {
                if (newValue == null) {
                    choiceType.setItems(FXCollections.observableArrayList());
                } else if (CounterUtil.isLandArmy(newValue.getType())) {
                    choiceType.setItems(FXCollections.observableArrayList(AdminActionTypeEnum.DIS, AdminActionTypeEnum.LM));
                } else if (CounterUtil.isArmy(newValue.getType())) {
                    choiceType.setItems(FXCollections.observableArrayList(AdminActionTypeEnum.DIS));
                } else if (CounterUtil.isFortress(newValue.getType())) {
                    choiceType.setItems(FXCollections.observableArrayList(AdminActionTypeEnum.DIS, AdminActionTypeEnum.LF));
                }
            }
        });

        Button btn = new Button(message.getMessage("add", null, globalConfiguration.getLocale()));
        btn.setOnAction(event -> {
            Counter counter = maintenanceCountersChoice.getSelectionModel().getSelectedItem();
            AdminActionTypeEnum type = choiceType.getSelectionModel().getSelectedItem();
            CounterFaceTypeEnum toCounter = toCounterChoice.getSelectionModel().getSelectedItem();

            callService(economicService::addAdminAction, () -> new AddAdminActionRequest(country.getId(), type, counter.getId(), toCounter), "Error when creating administrative action.");
        });

        hBox.getChildren().addAll(maintenanceCountersChoice, choiceType, toCounterChoice, btn);

        VBox vBox = new VBox();

        vBox.getChildren().addAll(maintenanceTable, hBox);

        maintenancePane.setContent(vBox);

        updateMaintenanceNode(country);

        return maintenancePane;
    }

    /**
     * @param actual the actual fortress type.
     * @param level  of the natural fortress of the province.
     * @return the potential lower fortresses that can be maintained given the actual fortress type and the natural fortress level of the province.
     */
    private List<CounterFaceTypeEnum> getLowerFortresses(CounterFaceTypeEnum actual, int level) {
        List<CounterFaceTypeEnum> fortresses = new ArrayList<>();

        if (actual != null) {
            switch (actual) {
                case FORTRESS_5:
                    if (level < 4) {
                        fortresses.add(CounterFaceTypeEnum.FORTRESS_4);
                    }
                case FORTRESS_4:
                    if (level < 3) {
                        fortresses.add(CounterFaceTypeEnum.FORTRESS_3);
                    }
                case FORTRESS_3:
                    if (level < 2) {
                        fortresses.add(CounterFaceTypeEnum.FORTRESS_2);
                    }
                case FORTRESS_2:
                    if (level < 1) {
                        fortresses.add(CounterFaceTypeEnum.FORTRESS_1);
                    }
                case FORTRESS_1:
                    break;
                case ARSENAL_5_ST_PETER:
                    if (level < 4) {
                        fortresses.add(CounterFaceTypeEnum.ARSENAL_4_ST_PETER);
                    }
                case ARSENAL_4_ST_PETER:
                    if (level < 3) {
                        fortresses.add(CounterFaceTypeEnum.ARSENAL_3_ST_PETER);
                    }
                case ARSENAL_3_ST_PETER:
                    if (level < 2) {
                        fortresses.add(CounterFaceTypeEnum.ARSENAL_2_ST_PETER);
                    }
                case ARSENAL_2_ST_PETER:
                    if (level < 1) {
                        fortresses.add(CounterFaceTypeEnum.ARSENAL_1_ST_PETER);
                    }
                case ARSENAL_1_ST_PETER:
                case ARSENAL_0_ST_PETER:
                    break;
                case ARSENAL_3_SEBASTOPOL:
                    if (level < 2) {
                        fortresses.add(CounterFaceTypeEnum.ARSENAL_2_SEBASTOPOL);
                    }
                case ARSENAL_2_SEBASTOPOL:
                    break;
                case ARSENAL_3_GIBRALTAR:
                    if (level < 2) {
                        fortresses.add(CounterFaceTypeEnum.ARSENAL_2_GIBRALTAR);
                    }
                case ARSENAL_2_GIBRALTAR:
                    break;
                case ARSENAL_4:
                    if (level < 3) {
                        fortresses.add(CounterFaceTypeEnum.ARSENAL_3);
                    }
                case ARSENAL_3:
                    if (level < 2) {
                        fortresses.add(CounterFaceTypeEnum.ARSENAL_2);
                    }
                case ARSENAL_2:
                    break;
            }
        }

        return fortresses;
    }

    /**
     * Update the unit maintenance node with the current game.
     *
     * @param country of the current player.
     */
    private void updateMaintenanceNode(PlayableCountry country) {
        List<AdministrativeAction> actions = country.getAdministrativeActions().stream()
                .filter(admAct -> admAct.getStatus() == AdminActionStatusEnum.PLANNED &&
                        (admAct.getType() == AdminActionTypeEnum.DIS || admAct.getType() == AdminActionTypeEnum.LM || admAct.getType() == AdminActionTypeEnum.LF))
                .collect(Collectors.toList());

        List<Counter> counters = game.getStacks().stream().flatMap(stack -> stack.getCounters().stream()
                .filter(counter -> StringUtils.equals(counter.getCountry(), country.getName()) &&
                        CounterUtil.isArmy(counter.getType())))
                .collect(Collectors.toList());
        List<Counter> conscriptCounters = new ArrayList<>();
        List<Counter> fortresses = game.getStacks().stream().flatMap(stack -> stack.getCounters().stream()
                .filter(counter -> StringUtils.equals(counter.getCountry(), country.getName()) &&
                        CounterUtil.isFortress(counter.getType())))
                .collect(Collectors.toList());
        Map<Pair<Integer, Boolean>, Integer> orderedFortresses = fortresses.stream().collect(Collectors.groupingBy(
                this::getFortressKeyFromCounter,
                Collectors.summingInt(value -> 1)));

        actions.stream().forEach(action -> {
            Counter counter = CommonUtil.findFirst(counters, o -> o.getId().equals(action.getIdObject()));
            if (counter != null) {
                if (action.getType() == AdminActionTypeEnum.LM) {
                    conscriptCounters.add(counter);
                }
                counters.remove(counter);
            }
            counter = CommonUtil.findFirst(fortresses, o -> o.getId().equals(action.getIdObject()));
            if (counter != null) {
                if (action.getType() == AdminActionTypeEnum.LF) {
                    CommonUtil.addOne(orderedFortresses,
                            new ImmutablePair<>(CounterUtil.getFortressLevelFromType(action.getCounterFaceType()), counter.getOwner().getProvince().startsWith("r")));
                }
                CommonUtil.subtractOne(orderedFortresses, getFortressKeyFromCounter(counter));
                fortresses.remove(counter);
            }
        });

        Map<CounterFaceTypeEnum, Long> forces = counters.stream().collect(Collectors.groupingBy(Counter::getType, Collectors.counting()));
        List<BasicForce> basicForces = globalConfiguration.getTables().getBasicForces().stream()
                .filter(basicForce -> StringUtils.equals(basicForce.getCountry(), country.getName()) &&
                        basicForce.getPeriod().getBegin() <= game.getTurn() &&
                        basicForce.getPeriod().getEnd() >= game.getTurn()).collect(Collectors.toList());
        // TODO manage wars
        List<Unit> units = globalConfiguration.getTables().getUnits().stream()
                .filter(unit -> StringUtils.equals(unit.getCountry(), country.getName()) &&
                        (unit.getAction() == UnitActionEnum.MAINT_WAR || unit.getAction() == UnitActionEnum.MAINT) &&
                        !unit.isSpecial() &&
                        (StringUtils.equals(unit.getTech().getName(), country.getLandTech()) || StringUtils.equals(unit.getTech().getName(), country.getNavalTech()))).collect(Collectors.toList());
        Integer unitMaintenanceCost = MaintenanceUtil.computeUnitMaintenance(forces, basicForces, units);

        Map<CounterFaceTypeEnum, Long> conscriptForces = conscriptCounters.stream().collect(Collectors.groupingBy(Counter::getType, Collectors.counting()));
        List<Unit> conscriptUnits = globalConfiguration.getTables().getUnits().stream()
                .filter(unit -> StringUtils.equals(unit.getCountry(), country.getName()) &&
                        unit.getAction() == UnitActionEnum.MAINT_WAR &&
                        unit.isSpecial() &&
                        StringUtils.equals(unit.getTech().getName(), country.getLandTech())).collect(Collectors.toList());
        Integer unitMaintenanceConscriptCost = MaintenanceUtil.computeUnitMaintenance(conscriptForces, null, conscriptUnits);

        Tech ownerLandTech = CommonUtil.findFirst(globalConfiguration.getTables().getTechs(), tech -> StringUtils.equals(tech.getName(), country.getLandTech()));

        Integer fortressesMaintenance = MaintenanceUtil.computeFortressesMaintenance(
                orderedFortresses,
                globalConfiguration.getTables().getTechs(),
                ownerLandTech,
                game.getTurn());

        List<Counter> missions = game.getStacks().stream().flatMap(stack -> stack.getCounters().stream()
                .filter(counter -> StringUtils.equals(counter.getCountry(), country.getName()) &&
                        counter.getType() == CounterFaceTypeEnum.MISSION))
                .collect(Collectors.toList());

        Integer missionMaintenance = missions.size();

        maintenancePane.setText(message.getMessage("admin_action.form.unit_maintenance", new Object[]{add(unitMaintenanceCost, unitMaintenanceConscriptCost), fortressesMaintenance, missionMaintenance}, globalConfiguration.getLocale()));
        maintenanceTable.setItems(FXCollections.observableArrayList(actions));
        ObservableList<Counter> counterList = FXCollections.observableArrayList(counters);
        counterList.addAll(fortresses);
        maintenanceCountersChoice.setItems(counterList);
    }

    /**
     * @param counter whose we want the key.
     * @return the key used for computing fortress maintenance. It is a Pair consisting of level and location (<code>true</code> for ROTW).
     */
    private Pair<Integer, Boolean> getFortressKeyFromCounter(Counter counter) {
        return new ImmutablePair<>(CounterUtil.getFortressLevelFromType(counter.getType()), GameUtil.isRotwProvince(counter.getOwner().getProvince()));
    }

    /**
     * Create the node for the unit purchase.
     *
     * @param country of the current player.
     * @return the node for the unit purchase.
     */
    private Node createPurchaseNode(PlayableCountry country) {
        purchasePane = new TitledPane();
        purchasePane.setExpanded(false);

        purchaseTable = new TableView<>();
        configureAdminActionTable(purchaseTable, this::removeAdminAction);

        HBox hBox = new HBox();

        ChoiceBox<IMapMarker> purchaseProvincesChoice = new ChoiceBox<>();
        purchaseProvincesChoice.converterProperty().set(new StringConverter<IMapMarker>() {
            /** {@inheritDoc} */
            @Override
            public String toString(IMapMarker object) {
                return message.getMessage(object.getId(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public IMapMarker fromString(String string) {
                return null;
            }
        });

        ChoiceBox<CounterFaceTypeEnum> purchaseTypeChoice = new ChoiceBox<>();
        purchaseTypeChoice.converterProperty().set(new StringConverter<CounterFaceTypeEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(CounterFaceTypeEnum object) {
                return object.name();
            }

            /** {@inheritDoc} */
            @Override
            public CounterFaceTypeEnum fromString(String string) {
                return null;
            }
        });

        Button btn = new Button(message.getMessage("add", null, globalConfiguration.getLocale()));
        btn.setOnAction(event -> {
            IMapMarker province = purchaseProvincesChoice.getSelectionModel().getSelectedItem();
            CounterFaceTypeEnum type = purchaseTypeChoice.getSelectionModel().getSelectedItem();

            callService(economicService::addAdminAction, () -> new AddAdminActionRequest(country.getId(), AdminActionTypeEnum.PU, province.getId(), type), "Error when creating administrative action.");
        });

        hBox.getChildren().addAll(purchaseProvincesChoice, purchaseTypeChoice, btn);

        VBox vBox = new VBox();

        vBox.getChildren().addAll(purchaseTable, hBox);

        purchasePane.setContent(vBox);

        purchaseProvincesChoice.setItems(FXCollections.observableArrayList(markers.stream()
                .filter(marker -> StringUtils.equals(country.getName(), marker.getOwner()) &&
                        StringUtils.equals(country.getName(), marker.getController())).collect(Collectors.toList())));

        List<Unit> forces = globalConfiguration.getTables().getUnits().stream()
                .filter(unit -> StringUtils.equals(unit.getCountry(), country.getName()) &&
                        unit.getAction() == UnitActionEnum.PURCHASE &&
                        (StringUtils.equals(unit.getTech().getName(), country.getLandTech()) || StringUtils.equals(unit.getTech().getName(), country.getNavalTech()))).collect(Collectors.toList());


        purchaseProvincesChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != oldValue) {
                if (newValue == null) {
                    purchaseTypeChoice.setItems(null);
                } else {
                    List<CounterFaceTypeEnum> faces = forces.stream().filter(force -> newValue.isPort() || force.getTech().isLand()).flatMap(force -> getFacesFromPurchaseForce(force.getType(), country.getName()).stream()).collect(Collectors.toList());
                    CounterFaceTypeEnum fortress = CommonUtil.findFirst(newValue.getStacks().stream()
                                    .flatMap(s -> s.getCounters().stream())
                                    .map(CounterMarker::getType),
                            CounterUtil::isFortress);
                    int fortressLevel = newValue.getFortressLevel();
                    if (fortress != null) {
                        fortressLevel = CounterUtil.getFortressLevelFromType(fortress);
                    }
                    List<CounterFaceTypeEnum> nextFortresses = CounterUtil.getFortressesFromLevel(fortressLevel + 1);
                    faces.addAll(nextFortresses);
                    purchaseTypeChoice.setItems(FXCollections.observableArrayList(faces));
                }
            }
        });

        updatePurchaseNode(country);

        return purchasePane;
    }

    /**
     * Update the unit purchase node with the current game.
     *
     * @param country of the current player.
     */
    private void updatePurchaseNode(PlayableCountry country) {
        List<AdministrativeAction> actions = country.getAdministrativeActions().stream()
                .filter(admAct -> admAct.getStatus() == AdminActionStatusEnum.PLANNED &&
                        admAct.getType() == AdminActionTypeEnum.PU)
                .collect(Collectors.toList());

        Map<Boolean, Double> currentPurchase = actions.stream()
                .filter(a -> !CounterUtil.isFortress(a.getCounterFaceType()))
                .collect(Collectors.groupingBy(action -> isLand(action.getCounterFaceType()), Collectors.summingDouble(action -> CounterUtil.getSizeFromType(action.getCounterFaceType()))));
        Map<LimitTypeEnum, Integer> maxPurchase = globalConfiguration.getTables().getLimits().stream().filter(
                limit -> StringUtils.equals(limit.getCountry(), country.getName()) &&
                        limit.getPeriod().getBegin() <= game.getTurn() &&
                        limit.getPeriod().getEnd() >= game.getTurn()).collect(Collectors.groupingBy(Limit::getType, Collectors.summingInt(Limit::getNumber)));

        purchasePane.setText(message.getMessage("admin_action.form.unit_purchase", new Object[]{currentPurchase.get(true), maxPurchase.get(LimitTypeEnum.PURCHASE_LAND_TROOPS), currentPurchase.get(false), maxPurchase.get(LimitTypeEnum.PURCHASE_NAVAL_TROOPS)}, globalConfiguration.getLocale()));
        purchaseTable.setItems(FXCollections.observableArrayList(actions));
    }

    /**
     * Returns whether the type of the counter face is land or not.
     *
     * @param face the face.
     * @return if it is land.
     */
    private Boolean isLand(CounterFaceTypeEnum face) {
        Boolean land = null;

        if (face != null) {
            switch (face) {
                case ARMY_PLUS:
                case ARMY_MINUS:
                case LAND_DETACHMENT:
                case LAND_DETACHMENT_TIMAR:
                case LAND_DETACHMENT_KOZAK:
                    land = true;
                    break;
                case FLEET_PLUS:
                case FLEET_MINUS:
                case NAVAL_DETACHMENT:
                case NAVAL_TRANSPORT:
                case NAVAL_GALLEY:
                    land = false;
                    break;
                default:
                    break;
            }
        }

        return land;
    }

    /**
     * Returns the different possibilities for a type of force for a counter type.
     *
     * @param force   type of force in a unit of action PURCHASE.
     * @param country for special rules (turkey, france, hollande, england,...)
     * @return the List of type faces.
     */
    private List<CounterFaceTypeEnum> getFacesFromPurchaseForce(ForceTypeEnum force, String country) {
        List<CounterFaceTypeEnum> faces = new ArrayList<>();

        if (force != null) {
            switch (force) {
                case ARMY_MINUS:
                    faces.add(CounterFaceTypeEnum.ARMY_MINUS);
                    if (StringUtils.equals(PlayableCountry.TURKEY, country)) {
                        faces.add(CounterFaceTypeEnum.ARMY_TIMAR_MINUS);
                    }
                    break;
                case LD:
                    faces.add(CounterFaceTypeEnum.LAND_DETACHMENT);
                    if (StringUtils.equals(PlayableCountry.TURKEY, country)) {
                        faces.add(CounterFaceTypeEnum.LAND_DETACHMENT_TIMAR);
                    }
                    break;
                case FLEET_GALLEY_MINUS:
                case FLEET_MINUS:
                    faces.add(CounterFaceTypeEnum.FLEET_MINUS);
                    break;
                case NWD:
                    faces.add(CounterFaceTypeEnum.NAVAL_DETACHMENT);
                    break;
                case NGD:
                    faces.add(CounterFaceTypeEnum.NAVAL_GALLEY);
                    break;
                case NTD:
                    faces.add(CounterFaceTypeEnum.NAVAL_TRANSPORT);
                    break;
                default:
                    break;
            }
        }

        return faces;
    }

    /**
     * Create the node for the trade fleet implantation.
     *
     * @param country of the current player.
     * @return the node for the trade fleet implantation.
     */
    private Node createTfiNode(PlayableCountry country) {
        tfiPane = new TitledPane();
        tfiPane.setExpanded(false);

        tfiTable = new TableView<>();
        configureAdminActionTable(tfiTable, this::removeAdminAction);

        HBox hBox = new HBox();

        ChoiceBox<IMapMarker> provincesChoice = new ChoiceBox<>();
        provincesChoice.converterProperty().set(new StringConverter<IMapMarker>() {
            /** {@inheritDoc} */
            @Override
            public String toString(IMapMarker object) {
                return message.getMessage(object.getId(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public IMapMarker fromString(String string) {
                return null;
            }
        });

        ChoiceBox<InvestmentEnum> investChoice = new ChoiceBox<>();
        investChoice.converterProperty().set(new StringConverter<InvestmentEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(InvestmentEnum object) {
                return message.getMessage("admin_action.investment." + object.name(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public InvestmentEnum fromString(String string) {
                return null;
            }
        });

        Button btn = new Button(message.getMessage("add", null, globalConfiguration.getLocale()));
        btn.setOnAction(event -> {
            IMapMarker province = provincesChoice.getSelectionModel().getSelectedItem();
            InvestmentEnum investment = investChoice.getSelectionModel().getSelectedItem();

            callService(economicService::addAdminAction, () -> new AddAdminActionRequest(country.getId(), AdminActionTypeEnum.TFI, province.getId(), investment), "Error when creating administrative action.");
        });

        hBox.getChildren().addAll(provincesChoice, investChoice, btn);

        VBox vBox = new VBox();

        vBox.getChildren().addAll(tfiTable, hBox);

        tfiPane.setContent(vBox);

        provincesChoice.setItems(FXCollections.observableArrayList(markers.stream()
                .filter(IMapMarker::isTradeZone).collect(Collectors.toList())));

        investChoice.setItems(FXCollections.observableArrayList(InvestmentEnum.values()));

        updateTfiNode(country);

        return tfiPane;
    }

    /**
     * Update the trade fleet implantation node with the current game.
     *
     * @param country of the current player.
     */
    private void updateTfiNode(PlayableCountry country) {
        List<AdministrativeAction> actions = country.getAdministrativeActions().stream()
                .filter(admAct -> admAct.getStatus() == AdminActionStatusEnum.PLANNED &&
                        admAct.getType() == AdminActionTypeEnum.TFI)
                .collect(Collectors.toList());

        Long currentTfis = actions.stream()
                .collect(Collectors.counting());
        Limit limitTfis = CommonUtil.findFirst(globalConfiguration.getTables().getLimits().stream(),
                limit -> StringUtils.equals(limit.getCountry(), country.getName()) &&
                        limit.getPeriod().getBegin() <= game.getTurn() &&
                        limit.getPeriod().getEnd() >= game.getTurn() &&
                        limit.getType() == LimitTypeEnum.ACTION_TFI);
        Integer maxTfis = 0;
        if (limitTfis != null) {
            maxTfis = limitTfis.getNumber();
        }

        tfiPane.setText(message.getMessage("admin_action.form.tfi", new Object[]{currentTfis, maxTfis}, globalConfiguration.getLocale()));
        tfiTable.setItems(FXCollections.observableArrayList(actions));
    }

    /**
     * Create the node for the domestic operation (MNU, DTI, FTI, taxes).
     *
     * @param country of the current player.
     * @return the node for the domestic operation.
     */
    private Node createDomesticOperationNode(PlayableCountry country) {
        domesticPane = new TitledPane();
        domesticPane.setExpanded(false);

        domesticTable = new TableView<>();
        configureAdminActionTable(domesticTable, this::removeAdminAction);

        HBox hBox = new HBox();

        ChoiceBox<AdminActionTypeEnum> typesChoice = new ChoiceBox<>();
        typesChoice.converterProperty().set(new StringConverter<AdminActionTypeEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(AdminActionTypeEnum object) {
                return message.getMessage("admin_action.type." + object.name(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public AdminActionTypeEnum fromString(String string) {
                return null;
            }
        });

        ChoiceBox<IMapMarker> provincesChoice = new ChoiceBox<>();
        provincesChoice.setVisible(false);
        provincesChoice.converterProperty().set(new StringConverter<IMapMarker>() {
            /** {@inheritDoc} */
            @Override
            public String toString(IMapMarker object) {
                return message.getMessage(object.getId(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public IMapMarker fromString(String string) {
                return null;
            }
        });

        ChoiceBox<CounterFaceTypeEnum> faceChoice = new ChoiceBox<>();
        faceChoice.setVisible(false);
        faceChoice.converterProperty().set(new StringConverter<CounterFaceTypeEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(CounterFaceTypeEnum object) {
                return object.name();
            }

            /** {@inheritDoc} */
            @Override
            public CounterFaceTypeEnum fromString(String string) {
                return null;
            }
        });

        ChoiceBox<InvestmentEnum> investChoice = new ChoiceBox<>();
        investChoice.setVisible(false);
        investChoice.converterProperty().set(new StringConverter<InvestmentEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(InvestmentEnum object) {
                return message.getMessage("admin_action.investment." + object.name(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public InvestmentEnum fromString(String string) {
                return null;
            }
        });

        typesChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != oldValue) {
                if (newValue == AdminActionTypeEnum.MNU) {
                    provincesChoice.setVisible(true);
                    provincesChoice.setItems(FXCollections.observableArrayList(markers.stream()
                            .filter(marker -> StringUtils.equals(country.getName(), marker.getOwner()) &&
                                    StringUtils.equals(country.getName(), marker.getController())).collect(Collectors.toList())));
                    faceChoice.setVisible(true);
                    CountryReferential countryRef = CommonUtil.findFirst(globalConfiguration.getReferential().getCountries(),
                            c -> StringUtils.equals(c.getName(), country.getName()));
                    List<CounterFaceTypeEnum> mnus = countryRef.getLimits().stream()
                            .filter(l -> CounterUtil.isManufacture(l.getType()))
                            .map(l -> CounterUtil.getManufactureFace(l.getType()))
                            .collect(Collectors.toList());
                    faceChoice.setItems(FXCollections.observableArrayList(mnus));
                    investChoice.setVisible(true);
                    investChoice.setItems(FXCollections.observableArrayList(InvestmentEnum.values()));
                } else if (newValue == AdminActionTypeEnum.DTI || newValue == AdminActionTypeEnum.FTI) {
                    provincesChoice.setVisible(false);
                    provincesChoice.setItems(FXCollections.observableArrayList());
                    faceChoice.setVisible(false);
                    faceChoice.setItems(FXCollections.observableArrayList());
                    investChoice.setVisible(true);
                    investChoice.setItems(FXCollections.observableArrayList(InvestmentEnum.values()));
                } else {
                    provincesChoice.setVisible(false);
                    provincesChoice.setItems(FXCollections.observableArrayList());
                    faceChoice.setVisible(false);
                    faceChoice.setItems(FXCollections.observableArrayList());
                    investChoice.setVisible(false);
                    investChoice.setItems(FXCollections.observableArrayList());
                }
            }
        });

        Button btn = new Button(message.getMessage("add", null, globalConfiguration.getLocale()));
        btn.setOnAction(event -> {
            AdminActionTypeEnum type = typesChoice.getSelectionModel().getSelectedItem();
            IMapMarker province = provincesChoice.getSelectionModel().getSelectedItem();
            String provinceName = province != null ? province.getId() : null;
            CounterFaceTypeEnum face = faceChoice.getSelectionModel().getSelectedItem();
            InvestmentEnum investment = investChoice.getSelectionModel().getSelectedItem();

            callService(economicService::addAdminAction, () -> new AddAdminActionRequest(country.getId(), type, provinceName, face, investment), "Error when creating administrative action.");
        });

        hBox.getChildren().addAll(typesChoice, provincesChoice, faceChoice, investChoice, btn);

        VBox vBox = new VBox();

        vBox.getChildren().addAll(domesticTable, hBox);

        domesticPane.setContent(vBox);

        typesChoice.setItems(FXCollections.observableArrayList(AdminActionTypeEnum.MNU, AdminActionTypeEnum.DTI, AdminActionTypeEnum.FTI, AdminActionTypeEnum.EXL));

        updateDomesticOperationNode(country);

        return domesticPane;
    }

    /**
     * Update the domestic operation node with the current game.
     *
     * @param country of the current player.
     */
    private void updateDomesticOperationNode(PlayableCountry country) {
        List<AdministrativeAction> actions = country.getAdministrativeActions().stream()
                .filter(admAct -> admAct.getStatus() == AdminActionStatusEnum.PLANNED &&
                        (admAct.getType() == AdminActionTypeEnum.MNU || admAct.getType() == AdminActionTypeEnum.EXL
                                || admAct.getType() == AdminActionTypeEnum.DTI || admAct.getType() == AdminActionTypeEnum.FTI))
                .collect(Collectors.toList());

        Long currentDoms = actions.stream()
                .collect(Collectors.counting());

        domesticPane.setText(message.getMessage("admin_action.form.domestic_operations", new Object[]{currentDoms, 1}, globalConfiguration.getLocale()));
        domesticTable.setItems(FXCollections.observableArrayList(actions));
    }

    /**
     * Create the node for the establishments (COL and TP).
     *
     * @param country of the current player.
     * @return the node for the establishment.
     */
    private Node createEstablishmentNode(PlayableCountry country) {
        establishmentPane = new TitledPane();
        establishmentPane.setExpanded(false);

        establishmentTable = new TableView<>();
        configureAdminActionTable(establishmentTable, this::removeAdminAction);

        HBox hBox = new HBox();

        ChoiceBox<AdminActionTypeEnum> typesChoice = new ChoiceBox<>();
        typesChoice.converterProperty().set(new StringConverter<AdminActionTypeEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(AdminActionTypeEnum object) {
                return message.getMessage("admin_action.type." + object.name(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public AdminActionTypeEnum fromString(String string) {
                return null;
            }
        });

        ChoiceBox<IMapMarker> provincesChoice = new ChoiceBox<>();
        provincesChoice.converterProperty().set(new StringConverter<IMapMarker>() {
            /** {@inheritDoc} */
            @Override
            public String toString(IMapMarker object) {
                return message.getMessage(object.getId(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public IMapMarker fromString(String string) {
                return null;
            }
        });

        ChoiceBox<InvestmentEnum> investChoice = new ChoiceBox<>();
        investChoice.converterProperty().set(new StringConverter<InvestmentEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(InvestmentEnum object) {
                return message.getMessage("admin_action.investment." + object.name(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public InvestmentEnum fromString(String string) {
                return null;
            }
        });

        Button btn = new Button(message.getMessage("add", null, globalConfiguration.getLocale()));
        btn.setOnAction(event -> {
            AdminActionTypeEnum type = typesChoice.getSelectionModel().getSelectedItem();
            IMapMarker province = provincesChoice.getSelectionModel().getSelectedItem();
            String provinceName = province != null ? province.getId() : null;
            InvestmentEnum investment = investChoice.getSelectionModel().getSelectedItem();

            callService(economicService::addAdminAction, () -> new AddAdminActionRequest(country.getId(), type, provinceName, investment), "Error when creating administrative action.");
        });

        hBox.getChildren().addAll(typesChoice, provincesChoice, investChoice, btn);

        VBox vBox = new VBox();

        vBox.getChildren().addAll(establishmentTable, hBox);

        establishmentPane.setContent(vBox);

        typesChoice.setItems(FXCollections.observableArrayList(AdminActionTypeEnum.COL, AdminActionTypeEnum.TP));
        provincesChoice.setItems(FXCollections.observableArrayList(markers.stream()
                .filter(IMapMarker::isRotw)
                .collect(Collectors.toList())));
        investChoice.setItems(FXCollections.observableArrayList(InvestmentEnum.values()));

        updateEstablishmentNode(country);

        return establishmentPane;
    }

    /**
     * Update the establishment node with the current game.
     *
     * @param country of the current player.
     */
    private void updateEstablishmentNode(PlayableCountry country) {
        List<AdministrativeAction> actions = country.getAdministrativeActions().stream()
                .filter(admAct -> admAct.getStatus() == AdminActionStatusEnum.PLANNED &&
                        (admAct.getType() == AdminActionTypeEnum.COL || admAct.getType() == AdminActionTypeEnum.TP))
                .collect(Collectors.toList());

        Map<AdminActionTypeEnum, Long> currentActions = actions.stream()
                .collect(Collectors.groupingBy(AdministrativeAction::getType, Collectors.counting()));
        Map<LimitTypeEnum, Integer> maxPurchase = globalConfiguration.getTables().getLimits().stream().filter(
                limit -> StringUtils.equals(limit.getCountry(), country.getName()) &&
                        limit.getPeriod().getBegin() <= game.getTurn() &&
                        limit.getPeriod().getEnd() >= game.getTurn()).collect(Collectors.groupingBy(Limit::getType, Collectors.summingInt(Limit::getNumber)));


        establishmentPane.setText(message.getMessage("admin_action.form.establishment",
                new Object[]{currentActions.get(AdminActionTypeEnum.COL), maxPurchase.get(LimitTypeEnum.ACTION_COL),
                        currentActions.get(AdminActionTypeEnum.TP), maxPurchase.get(LimitTypeEnum.ACTION_TP)},
                globalConfiguration.getLocale()));
        establishmentTable.setItems(FXCollections.observableArrayList(actions));
    }

    /**
     * Create the node for the technology (land and naval).
     *
     * @param country of the current player.
     * @return the node for the technology.
     */
    private Node createTechnologyNode(PlayableCountry country) {
        technologyPane = new TitledPane();
        technologyPane.setExpanded(false);

        technologyTable = new TableView<>();
        configureAdminActionTable(technologyTable, this::removeAdminAction);

        HBox hBox = new HBox();

        ChoiceBox<AdminActionTypeEnum> typesChoice = new ChoiceBox<>();
        typesChoice.converterProperty().set(new StringConverter<AdminActionTypeEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(AdminActionTypeEnum object) {
                return message.getMessage("admin_action.type." + object.name(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public AdminActionTypeEnum fromString(String string) {
                return null;
            }
        });

        ChoiceBox<InvestmentEnum> investChoice = new ChoiceBox<>();
        investChoice.converterProperty().set(new StringConverter<InvestmentEnum>() {
            /** {@inheritDoc} */
            @Override
            public String toString(InvestmentEnum object) {
                return message.getMessage("admin_action.investment." + object.name(), null, globalConfiguration.getLocale());
            }

            /** {@inheritDoc} */
            @Override
            public InvestmentEnum fromString(String string) {
                return null;
            }
        });

        Button btn = new Button(message.getMessage("add", null, globalConfiguration.getLocale()));
        btn.setOnAction(event -> {
            AdminActionTypeEnum type = typesChoice.getSelectionModel().getSelectedItem();
            InvestmentEnum investment = investChoice.getSelectionModel().getSelectedItem();

            callService(economicService::addAdminAction, () -> new AddAdminActionRequest(country.getId(), type, investment), "Error when creating administrative action.");
        });

        hBox.getChildren().addAll(typesChoice, investChoice, btn);

        VBox vBox = new VBox();

        vBox.getChildren().addAll(technologyTable, hBox);

        technologyPane.setContent(vBox);

        typesChoice.setItems(FXCollections.observableArrayList(AdminActionTypeEnum.ELT, AdminActionTypeEnum.ENT));
        investChoice.setItems(FXCollections.observableArrayList(InvestmentEnum.values()));

        updateTechnologyNode(country);

        return technologyPane;
    }

    /**
     * Update the technology node with the current game.
     *
     * @param country of the current player.
     */
    private void updateTechnologyNode(PlayableCountry country) {
        List<AdministrativeAction> actions = country.getAdministrativeActions().stream()
                .filter(admAct -> admAct.getStatus() == AdminActionStatusEnum.PLANNED &&
                        (admAct.getType() == AdminActionTypeEnum.ELT || admAct.getType() == AdminActionTypeEnum.ENT))
                .collect(Collectors.toList());

        Map<AdminActionTypeEnum, Long> currentActions = actions.stream()
                .collect(Collectors.groupingBy(AdministrativeAction::getType, Collectors.counting()));

        technologyPane.setText(message.getMessage("admin_action.form.technology",
                new Object[]{currentActions.get(AdminActionTypeEnum.ELT), 1,
                        currentActions.get(AdminActionTypeEnum.ENT), 1},
                globalConfiguration.getLocale()));
        technologyTable.setItems(FXCollections.observableArrayList(actions));
    }

    /**
     * Method called for removing a PLANNED administrative action.
     *
     * @param param the administrative action to remove.
     */
    private void removeAdminAction(AdministrativeAction param) {
        callService(economicService::removeAdminAction, () -> new RemoveAdminActionRequest(param.getId()), "Error when creating administrative action.");
    }

    /**
     * @return a Node containing all transverse actions for administrative actions.
     */
    private Node createActionsNode() {
        HBox actions = new HBox();

        Button validation = new Button(message.getMessage("validate", null, globalConfiguration.getLocale()));
        validation.setOnAction(event -> callService(economicService::validateAdminActions, () -> new ValidateRequest(true), "Error when validating administrative actions."));
        actions.getChildren().add(validation);

        Button invalidation = new Button(message.getMessage("invalidate", null, globalConfiguration.getLocale()));
        invalidation.setOnAction(event -> callService(economicService::validateAdminActions, () -> new ValidateRequest(false), "Error when invalidating administrative actions."));
        actions.getChildren().add(invalidation);

        return actions;
    }

    /**
     * Creates the tab for the administrative actions already done.
     *
     * @param country of the current player.
     * @return the tab for the administrative actions already done
     */
    private Tab createAdminList(PlayableCountry country) {
        Tab tab = new Tab(message.getMessage("admin_action.list", null, globalConfiguration.getLocale()));
        tab.setClosable(false);

        choiceListCountry = new ChoiceBox<>();
        choiceListCountry.setItems(FXCollections.observableArrayList(game.getCountries()));
        choiceListCountry.converterProperty().set(new StringConverter<PlayableCountry>() {
            /** {@inheritDoc} */
            @Override
            public String toString(PlayableCountry object) {
                return object.getName();
            }

            /** {@inheritDoc} */
            @Override
            public PlayableCountry fromString(String string) {
                PlayableCountry country = null;

                for (PlayableCountry countryTest : game.getCountries()) {
                    if (StringUtils.equals(string, countryTest.getName())) {
                        country = countryTest;
                        break;
                    }
                }

                return country;
            }
        });

        choiceListTurn = new ChoiceBox<>();

        HBox hBox = new HBox();
        hBox.getChildren().addAll(choiceListCountry, choiceListTurn);

        tableList = new TableView<>();
        configureAdminActionTable(tableList, null);

        VBox vBox = new VBox();
        vBox.getChildren().addAll(hBox, tableList);

        tab.setContent(vBox);

        choiceListCountry.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != oldValue) {
                List<Integer> turns = newValue.getAdministrativeActions().stream()
                        .filter(action -> action.getStatus() == AdminActionStatusEnum.DONE)
                        .map(AdministrativeAction::getTurn)
                        .distinct()
                        .collect(Collectors.toList());
                Collections.sort(turns, Comparator.reverseOrder());
                turns.add(0, 0);
                Integer turnBefore = choiceListTurn.getSelectionModel().getSelectedItem();
                choiceListTurn.setItems(FXCollections.observableArrayList(turns));
                if (turns.contains(turnBefore)) {
                    choiceListTurn.getSelectionModel().select(turnBefore);
                }
            }
        });
        choiceListCountry.getSelectionModel().select(country);

        choiceListTurn.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue == 0) {
                List<AdministrativeAction> actions = choiceListCountry.getSelectionModel().getSelectedItem().getAdministrativeActions().stream()
                        .filter(action -> action.getStatus() == AdminActionStatusEnum.DONE)
                        .sorted(Comparator.comparing(AdministrativeAction::getTurn).reversed())
                        .collect(Collectors.toList());
                tableList.setItems(FXCollections.observableArrayList(actions));
            } else if (!newValue.equals(oldValue)) {
                List<AdministrativeAction> actions = choiceListCountry.getSelectionModel().getSelectedItem().getAdministrativeActions().stream()
                        .filter(action -> action.getStatus() == AdminActionStatusEnum.DONE && action.getTurn().equals(newValue))
                        .collect(Collectors.toList());
                tableList.setItems(FXCollections.observableArrayList(actions));
            }
        });

        return tab;
    }

    /**
     * Update the list of past actions.
     */
    private void updateActionList() {
        PlayableCountry country = choiceListCountry.getSelectionModel().getSelectedItem();
        Integer turn = choiceListTurn.getSelectionModel().getSelectedItem();
        if (country != null && turn != null) {
            List<AdministrativeAction> actions = country.getAdministrativeActions().stream()
                    .filter(action -> action.getStatus() == AdminActionStatusEnum.DONE &&
                            (turn == 0 || action.getTurn().equals(turn)))
                    .sorted(Comparator.comparing(AdministrativeAction::getTurn).reversed())
                    .collect(Collectors.toList());
            tableList.setItems(FXCollections.observableArrayList(actions));
        }
    }

    /**
     * Configure the administrative actions tableList.
     *
     * @param table to configure.
     */
    private void configureAdminActionTable(TableView<AdministrativeAction> table, Consumer<AdministrativeAction> callback) {
        table.setTableMenuButtonVisible(true);
        table.setPrefWidth(750);
        TableColumn<AdministrativeAction, String> column;

        if (callback == null) {
            column = new TableColumn<>(message.getMessage("admin_action.turn", null, globalConfiguration.getLocale()));
            column.prefWidthProperty().bind(table.widthProperty().multiply(0.05));
            column.setCellValueFactory(new PropertyValueFactory<>("turn"));
            table.getColumns().add(column);
        }

        column = new TableColumn<>(message.getMessage("admin_action.action", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        column.setCellValueFactory(param -> new ReadOnlyStringWrapper(message.getMessage("admin_action.type." + param.getValue().getType(), null, globalConfiguration.getLocale())));
        table.getColumns().add(column);

        column = new TableColumn<>(message.getMessage("admin_action.info", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        column.setCellValueFactory(param -> new ReadOnlyStringWrapper(getInfo(param.getValue())));
        table.getColumns().add(column);

        column = new TableColumn<>(message.getMessage("admin_action.cost", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.05));
        column.setCellValueFactory(new PropertyValueFactory<>("cost"));
        table.getColumns().add(column);

        column = new TableColumn<>(message.getMessage("admin_action.column", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.05));
        column.setCellValueFactory(new PropertyValueFactory<>("column"));
        table.getColumns().add(column);

        column = new TableColumn<>(message.getMessage("admin_action.bonus", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.05));
        column.setCellValueFactory(new PropertyValueFactory<>("bonus"));
        table.getColumns().add(column);

        if (callback == null) {
            column = new TableColumn<>(message.getMessage("admin_action.die", null, globalConfiguration.getLocale()));
            column.prefWidthProperty().bind(table.widthProperty().multiply(0.05));
            column.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getDie() == null ? "" : param.getValue().getDie().toString()));
            table.getColumns().add(column);

            column = new TableColumn<>(message.getMessage("admin_action.result", null, globalConfiguration.getLocale()));
            column.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
            column.setCellValueFactory(param -> new ReadOnlyStringWrapper(getResult(param.getValue().getResult(), param.getValue().getSecondaryDie(),
                    param.getValue().isSecondaryResult())));
            table.getColumns().add(column);
        }

        if (callback != null) {
            column = new TableColumn<>(message.getMessage("admin_action.actions", null, globalConfiguration.getLocale()));
            column.prefWidthProperty().bind(table.widthProperty().multiply(0.25));
            column.setCellValueFactory(new PropertyValueFactory<>("NONE"));
            Callback<TableColumn<AdministrativeAction, String>, TableCell<AdministrativeAction, String>> cellFactory = new Callback<TableColumn<AdministrativeAction, String>, TableCell<AdministrativeAction, String>>() {
                @Override
                public TableCell<AdministrativeAction, String> call(TableColumn<AdministrativeAction, String> param) {
                    return new TableCell<AdministrativeAction, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                                setText(null);
                            } else {
                                Button btn = new Button(message.getMessage("delete", null, globalConfiguration.getLocale()));
                                btn.setOnAction(event -> {
                                    AdministrativeAction adminAction = getTableView().getItems().get(getIndex());
                                    callback.accept(adminAction);
                                });
                                setGraphic(btn);
                                setText(null);
                            }
                        }
                    };
                }
            };
            column.setCellFactory(cellFactory);
            table.getColumns().add(column);
        }
    }

    private String getInfo(AdministrativeAction action) {
        StringBuilder info = new StringBuilder();

        if (action.getType() == AdminActionTypeEnum.LF) {
            Counter counter = game.getStacks().stream().flatMap(stack -> stack.getCounters().stream()
                    .filter(counter1 -> counter1.getId().equals(action.getIdObject()))).findFirst().orElse(null);
            if (counter != null) {
                String provinceName = message.getMessage(counter.getOwner().getProvince(), null, globalConfiguration.getLocale());
                info.append(provinceName).append(" - ").append(counter.getType());
            }
            if (action.getCounterFaceType() != null) {
                info.append(" -> ").append(action.getCounterFaceType());
            }
        } else if (StringUtils.isNotEmpty(action.getProvince())) {
            String provinceName = message.getMessage(action.getProvince(), null, globalConfiguration.getLocale());
            info.append(provinceName);
            if (action.getCounterFaceType() != null) {
                info.append(" - ").append(action.getCounterFaceType());
            }
        }

        return info.toString();
    }

    /**
     * Creates the tab for the competitions.
     *
     * @return the tab for the competitions.
     */
    private Tab createCompetition() {
        Tab tab = new Tab(message.getMessage("admin_action.competitions", null, globalConfiguration.getLocale()));
        tab.setClosable(false);

        choiceCompetitionTurn = new ChoiceBox<>();

        choiceCompetition = new ChoiceBox<>();
        choiceCompetition.converterProperty().set(new StringConverter<Competition>() {
            /** {@inheritDoc} */
            @Override
            public String toString(Competition object) {
                String province = message.getMessage(object.getProvince(), null, globalConfiguration.getLocale());
                String type = message.getMessage("competition.type." + object.getType(), null, globalConfiguration.getLocale());
                return province + " - " + type;
            }

            /** {@inheritDoc} */
            @Override
            public Competition fromString(String string) {
                return null;
            }
        });

        HBox hBox = new HBox();
        hBox.getChildren().addAll(choiceCompetitionTurn, choiceCompetition);

        tableCompetition = new TableView<>();
        configureCompetitions(tableCompetition);

        VBox vBox = new VBox();
        vBox.getChildren().addAll(hBox, tableCompetition);

        tab.setContent(vBox);

        choiceCompetitionTurn.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                choiceCompetition.setItems(null);
            } else if (!newValue.equals(oldValue)) {
                List<Competition> competitions = game.getCompetitions().stream()
                        .filter(comp -> comp.getTurn().equals(newValue))
                        .collect(Collectors.toList());
                choiceCompetition.setItems(FXCollections.observableArrayList(competitions));
            }
        });

        choiceCompetition.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                tableCompetition.setItems(null);
            } else if (!newValue.equals(oldValue)) {
                Collections.sort(newValue.getRounds(), (o1, o2) -> {
                    int compare = o1.getRound().compareTo(o2.getRound());
                    if (compare == 0) {
                        compare = o1.getCountry().compareTo(o2.getCountry());
                    }
                    return compare;
                });
                tableCompetition.setItems(FXCollections.observableArrayList(newValue.getRounds()));
            }
        });

        updateCompetitions();

        return tab;
    }

    /**
     * Update the list of competitions.
     */
    private void updateCompetitions() {
        Integer turn = choiceCompetitionTurn.getSelectionModel().getSelectedItem();
        Competition competition = choiceCompetition.getSelectionModel().getSelectedItem();

        List<Integer> turns = game.getCompetitions().stream()
                .map(Competition::getTurn)
                .distinct()
                .sorted(Comparator.<Integer>reverseOrder())
                .collect(Collectors.toList());
        choiceCompetitionTurn.setItems(FXCollections.observableArrayList(turns));

        if (turn != null) {
            choiceCompetitionTurn.getSelectionModel().select(turn);
            if (competition != null) {
                choiceCompetition.getSelectionModel().select(competition);
            }
        }
    }

    private void configureCompetitions(TableView<CompetitionRound> table) {
        table.setTableMenuButtonVisible(true);
        table.setPrefWidth(750);
        TableColumn<CompetitionRound, String> column;

        column = new TableColumn<>(message.getMessage("admin_action.competition.round", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        column.setCellValueFactory(new PropertyValueFactory<>("round"));
        table.getColumns().add(column);

        column = new TableColumn<>(message.getMessage("admin_action.competition.country", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        column.setCellValueFactory(param -> new ReadOnlyStringWrapper(message.getMessage(param.getValue().getCountry(), null, globalConfiguration.getLocale())));
        table.getColumns().add(column);

        column = new TableColumn<>(message.getMessage("admin_action.competition.column", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        column.setCellValueFactory(new PropertyValueFactory<>("column"));
        table.getColumns().add(column);

        column = new TableColumn<>(message.getMessage("admin_action.competition.die", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        column.setCellValueFactory(new PropertyValueFactory<>("die"));
        table.getColumns().add(column);

        column = new TableColumn<>(message.getMessage("admin_action.competition.result", null, globalConfiguration.getLocale()));
        column.prefWidthProperty().bind(table.widthProperty().multiply(0.4));
        column.setCellValueFactory(param -> new ReadOnlyStringWrapper(getResult(param.getValue().getResult(), param.getValue().getSecondaryDie(),
                param.getValue().isSecondaryResult())));
        table.getColumns().add(column);
    }

    /**
     * Displays the result of an administrative action/competition round.
     *
     * @param result          main result.
     * @param secondaryDie    if result is average, another die is rolled.
     * @param secondaryResult if another die was rolled, tells if it was a success or not.
     * @return the display of a result.
     */
    private String getResult(ResultEnum result, Integer secondaryDie, Boolean secondaryResult) {
        StringBuilder sb = new StringBuilder();

        sb.append(message.getMessage("admin_action.result." + result, null, globalConfiguration.getLocale()));
        if (secondaryDie != null && secondaryResult != null) {
            sb.append(" (")
                    .append(secondaryDie)
                    .append(" -> ")
                    .append(message.getMessage("admin_action.secondary_result." + secondaryResult, null, globalConfiguration.getLocale()))
                    .append(")");
        }

        return sb.toString();
    }

    /**
     * Update the window given the diff.
     *
     * @param diff that will update the window.
     */
    public void update(Diff diff) {
        switch (diff.getTypeObject()) {
            case ADM_ACT:
                updateAdmAct(diff);
                break;
            case COUNTRY:
                if (gameConfig.getIdCountry().equals(diff.getIdObject())) {
                    PlayableCountry country = CommonUtil.findFirst(game.getCountries(), playableCountry -> playableCountry.getId().equals(gameConfig.getIdCountry()));
                    if (country != null) {
                        updateMaintenanceNode(country);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Process a eco sheet diff event.
     *
     * @param diff involving an administrative action.
     */
    private void updateAdmAct(Diff diff) {
        switch (diff.getType()) {
            case ADD:
            case REMOVE:
                DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TYPE);
                if (attribute != null) {
                    AdminActionTypeEnum type = AdminActionTypeEnum.valueOf(attribute.getValue());
                    PlayableCountry country = CommonUtil.findFirst(game.getCountries(), playableCountry -> playableCountry.getId().equals(gameConfig.getIdCountry()));
                    if (type != null && country != null) {
                        switch (type) {
                            case DIS:
                            case LM:
                            case LF:
                                updateMaintenanceNode(country);
                                break;
                            case PU:
                                updatePurchaseNode(country);
                                break;
                            case TFI:
                                updateTfiNode(country);
                                break;
                            case MNU:
                            case DTI:
                            case FTI:
                            case EXL:
                                updateDomesticOperationNode(country);
                                break;
                            case COL:
                            case TP:
                                updateEstablishmentNode(country);
                                break;
                            case ELT:
                            case ENT:
                                updateTechnologyNode(country);
                                break;
                            default:
                                break;
                        }
                    }
                }
                break;
            case VALIDATE:
                PlayableCountry country = CommonUtil.findFirst(game.getCountries(), playableCountry -> playableCountry.getId().equals(gameConfig.getIdCountry()));
                if (country != null) {
                    updateMaintenanceNode(country);
                    updatePurchaseNode(country);
                    updateTfiNode(country);
                    updateDomesticOperationNode(country);
                    updateEstablishmentNode(country);
                    updateTechnologyNode(country);
                }
                updateActionList();
                updateCompetitions();
                break;
            default:
                break;
        }
    }
}
