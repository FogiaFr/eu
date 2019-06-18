package com.mkl.eu.front.client.game;

import com.mkl.eu.client.common.exception.FunctionalException;
import com.mkl.eu.client.common.util.CommonUtil;
import com.mkl.eu.client.common.vo.Request;
import com.mkl.eu.client.common.vo.SimpleRequest;
import com.mkl.eu.client.service.service.IChatService;
import com.mkl.eu.client.service.service.IEconomicService;
import com.mkl.eu.client.service.service.IGameService;
import com.mkl.eu.client.service.service.chat.LoadRoomRequest;
import com.mkl.eu.client.service.service.eco.*;
import com.mkl.eu.client.service.service.game.LoadGameRequest;
import com.mkl.eu.client.service.service.game.LoadTurnOrderRequest;
import com.mkl.eu.client.service.util.CounterUtil;
import com.mkl.eu.client.service.util.GameUtil;
import com.mkl.eu.client.service.vo.Game;
import com.mkl.eu.client.service.vo.board.Counter;
import com.mkl.eu.client.service.vo.board.Stack;
import com.mkl.eu.client.service.vo.chat.Message;
import com.mkl.eu.client.service.vo.chat.MessageDiff;
import com.mkl.eu.client.service.vo.chat.Room;
import com.mkl.eu.client.service.vo.country.PlayableCountry;
import com.mkl.eu.client.service.vo.diff.Diff;
import com.mkl.eu.client.service.vo.diff.DiffAttributes;
import com.mkl.eu.client.service.vo.diplo.CountryOrder;
import com.mkl.eu.client.service.vo.eco.AdministrativeAction;
import com.mkl.eu.client.service.vo.eco.Competition;
import com.mkl.eu.client.service.vo.eco.TradeFleet;
import com.mkl.eu.client.service.vo.enumeration.*;
import com.mkl.eu.front.client.chat.ChatWindow;
import com.mkl.eu.front.client.eco.AdminActionsWindow;
import com.mkl.eu.front.client.eco.EcoWindow;
import com.mkl.eu.front.client.event.DiffEvent;
import com.mkl.eu.front.client.event.ExceptionEvent;
import com.mkl.eu.front.client.event.IDiffListener;
import com.mkl.eu.front.client.main.GameConfiguration;
import com.mkl.eu.front.client.main.GlobalConfiguration;
import com.mkl.eu.front.client.main.UIUtil;
import com.mkl.eu.front.client.map.InteractiveMap;
import com.mkl.eu.front.client.map.marker.IMapMarker;
import com.mkl.eu.front.client.map.marker.MarkerUtils;
import com.mkl.eu.front.client.socket.ClientSocket;
import com.mkl.eu.front.client.vo.AuthentHolder;
import de.fhpotsdam.unfolding.marker.Marker;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import processing.core.PApplet;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mkl.eu.client.common.util.CommonUtil.findFirst;

/**
 * Popup used when loading a game. Holds the actions of opening other popups (map, chat, actions,...).
 * Is not a popup anymore.
 *
 * @author MKL.
 */
@Component
@Scope(value = "prototype")
public class GamePopup implements IDiffListener, EventHandler<WindowEvent>, ApplicationContextAware {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GamePopup.class);
    /** Content of this component. */
    private TabPane content;
    /** Flag saying that the popup has already been closed. */
    private boolean closed;
    /** Spring application context. */
    private ApplicationContext context;
    /** Internationalisation. */
    @Autowired
    private MessageSource message;
    /** Configuration of the application. */
    @Autowired
    private GlobalConfiguration globalConfiguration;
    /** Game service. */
    @Autowired
    private IGameService gameService;
    /** Chat service. */
    @Autowired
    private IChatService chatService;
    /** Economic service. */
    @Autowired
    private IEconomicService economicService;
    /** PApplet for the intercative map. */
    private InteractiveMap map;
    /** Flag saying that we already initialized the map. */
    private boolean mapInit;
    /** Window containing all the chat. */
    private ChatWindow chatWindow;
    /** Window containing the economics. */
    private EcoWindow ecoWindow;
    /** Window containing the administrative actions. */
    private AdminActionsWindow adminActionsWindow;
    /** Socket listening to server diff on this game. */
    private ClientSocket client;
    /** Component holding the authentication information. */
    @Autowired
    private AuthentHolder authentHolder;
    /** Game displayed. */
    private Game game;
    /** Game config to store between constructor and Spring init PostConstruct and to spread to other UIs. */
    private GameConfiguration gameConfig;
    /** Component to be refreshed when status changed. */
    private VBox activeCountries = new VBox();
    /** Title to be refreshed when status changed. */
    private Text info = new Text();

    public GamePopup(Long idGame, Long idCountry) {
        gameConfig = new GameConfiguration();
        gameConfig.setIdGame(idGame);
        gameConfig.setIdCountry(idCountry);
    }

    /** @return the content. */
    public Node getContent() {
        return content;
    }

    /**
     * Initialize the popup.
     *
     * @throws FunctionalException Functional exception.
     */
    @PostConstruct
    public void init() throws FunctionalException {
        content = new TabPane();
        initGame();
        Map<String, Marker> markers = MarkerUtils.createMarkers(game);
        initMap(markers);
        initUI();
        initChat();
        initEco(markers);
    }

    /**
     * Load the game.
     *
     * @throws FunctionalException Functional exception.
     */
    private void initGame() throws FunctionalException {
        SimpleRequest<LoadGameRequest> request = new Request<>();
        authentHolder.fillAuthentInfo(request);
        request.setRequest(new LoadGameRequest(gameConfig.getIdGame(), gameConfig.getIdCountry()));

        game = gameService.loadGame(request);
        gameConfig.setVersionGame(game.getVersion());
        Optional<Message> opt = game.getChat().getGlobalMessages().stream().max((o1, o2) -> (int) (o1.getId() - o2.getId()));
        if (opt.isPresent()) {
            gameConfig.setMaxIdGlobalMessage(opt.get().getId());
        }
        Long maxIdMessage = null;
        for (Room room : game.getChat().getRooms()) {
            opt = room.getMessages().stream().max((o1, o2) -> (int) (o1.getId() - o2.getId()));
            if (opt.isPresent() && (maxIdMessage == null || opt.get().getId() > maxIdMessage)) {
                maxIdMessage = opt.get().getId();
            }
        }
        gameConfig.setMaxIdMessage(maxIdMessage);

        client = context.getBean(ClientSocket.class, gameConfig);
        client.addDiffListener(this);

        new Thread(client).start();
    }

    /**
     * Initialize the interactive map.
     *
     * @param markers displayed on the map.
     */
    private void initMap(Map<String, Marker> markers) {
        map = context.getBean(InteractiveMap.class, game, gameConfig, markers);
        map.addDiffListener(this);
    }

    /**
     * Initialize the chat window.
     */
    private void initChat() {
        chatWindow = context.getBean(ChatWindow.class, game.getChat(), game.getCountries(), gameConfig);
        chatWindow.addDiffListener(this);
        Tab tab = new Tab(message.getMessage("chat.title", null, globalConfiguration.getLocale()));
        tab.setClosable(false);
        tab.setContent(chatWindow.getTabPane());
        content.getTabs().add(tab);
    }

    /**
     * Initialize the eco window.
     *
     * @param markers displayed on the map.
     */
    private void initEco(Map<String, Marker> markers) {
        ecoWindow = context.getBean(EcoWindow.class, game.getCountries(), game.getTradeFleets(), gameConfig);
        ecoWindow.addDiffListener(this);
        Tab tab = new Tab(message.getMessage("eco.title", null, globalConfiguration.getLocale()));
        tab.setClosable(false);
        tab.setContent(ecoWindow.getTabPane());
        content.getTabs().add(tab);

        List<IMapMarker> mapMarkers = markers.values().stream().filter(marker -> marker instanceof IMapMarker).map(marker -> (IMapMarker) marker).collect(Collectors.toList());
        adminActionsWindow = context.getBean(AdminActionsWindow.class, game, mapMarkers, gameConfig);
        adminActionsWindow.addDiffListener(this);
        tab = new Tab(message.getMessage("admin_action.title", null, globalConfiguration.getLocale()));
        tab.setClosable(false);
        tab.setContent(adminActionsWindow.getTabPane());
        content.getTabs().add(tab);
    }

    /**
     * Initialize all the UIs on the popup.
     */
    private void initUI() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        grid.add(info, 0, 0, 1, 1);
        updateTitle();
        updateActivePlayers();

        grid.add(activeCountries, 1, 0, 1, 5);

        Button mapBtn = new Button(message.getMessage("game.popup.map", null, globalConfiguration.getLocale()));
        mapBtn.setOnAction(event -> {
            if (!mapInit) {
                PApplet.runSketch(new String[]{"InteractiveMap"}, map);
                mapInit = true;
            } else {
                if (map.isVisible()) {
                    map.requestFocus();
                } else {
                    map.setVisible(true);
                }
            }
        });
        grid.add(mapBtn, 0, 1, 1, 1);

        Tab tab = new Tab(message.getMessage("game.popup.global", null, globalConfiguration.getLocale()));
        tab.setClosable(false);
        tab.setContent(grid);
        content.getTabs().add(tab);
    }

    private void updateTitle() {
        StringBuilder sb = new StringBuilder();

        sb.append(message.getMessage("game.popup.turn", new Object[]{game.getTurn()}, globalConfiguration.getLocale()));
        sb.append("\n");
        String statusText = message.getMessage("game.status." + game.getStatus(), null, globalConfiguration.getLocale());
        sb.append(message.getMessage("game.popup.info_phase", new Object[]{statusText}, globalConfiguration.getLocale()));

        info.setText(sb.toString());
    }

    /**
     * Update the list of active countries.
     */
    private void updateActivePlayers() {
        activeCountries.getChildren().clear();
        switch (game.getStatus()) {
            case ECONOMICAL_EVENT:
            case POLITICAL_EVENT:
            case DIPLOMACY:
            case ADMINISTRATIVE_ACTIONS_CHOICE:
            case MILITARY_HIERARCHY:
                List<PlayableCountry> activePlayers = GameUtil.getActivePlayers(game).stream()
                        .collect(Collectors.toList());
                game.getCountries().stream()
                        .filter(c -> StringUtils.isNotEmpty(c.getUsername()))
                        .forEach(country -> {
                            HBox hBox = new HBox();
                            Text text = new Text(country.getName());
                            hBox.getChildren().add(text);
                            if (activePlayers.contains(country)) {
                                try {
                                    Image img = new Image(new FileInputStream(new File("data/img/cross.png")), 16, 16, true, false);
                                    ImageView imgView = new ImageView(img);
                                    hBox.getChildren().add(imgView);
                                } catch (FileNotFoundException e) {
                                    LOGGER.error("Image located at data/img/cross.png not found.", e);
                                }
                            } else {
                                try {
                                    Image img = new Image(new FileInputStream(new File("data/img/check.png")), 16, 16, true, false);
                                    ImageView imgView = new ImageView(img);
                                    hBox.getChildren().add(imgView);
                                } catch (FileNotFoundException e) {
                                    LOGGER.error("Image located at data/img/check.png not found.", e);
                                }
                            }
                            activeCountries.getChildren().add(hBox);
                        });
                break;
            case MILITARY_CAMPAIGN:
            case MILITARY_SUPPLY:
            case MILITARY_MOVE:
            case MILITARY_BATTLES:
            case MILITARY_SIEGES:
            case MILITARY_NEUTRALS:
                int activePosition = game.getOrders().stream()
                        .filter(order -> order.getGameStatus() == GameStatusEnum.MILITARY_MOVE && order.isActive())
                        .map(CountryOrder::getPosition)
                        .findFirst()
                        .orElse(-1);
                game.getOrders().stream()
                        .filter(order -> order.getGameStatus() == GameStatusEnum.MILITARY_MOVE)
                        .sorted(Comparator.comparing(CountryOrder::getPosition))
                        .forEach(order -> {
                            HBox hBox = new HBox();
                            Text text = new Text(order.getCountry().getName());
                            hBox.getChildren().add(text);
                            if (order.isActive()) {
                                try {
                                    Image img = new Image(new FileInputStream(new File("data/img/cross.png")), 16, 16, true, false);
                                    ImageView imgView = new ImageView(img);
                                    hBox.getChildren().add(imgView);
                                } catch (FileNotFoundException e) {
                                    LOGGER.error("Image located at data/img/cross.png not found.", e);
                                }
                            } else if (order.getPosition() < activePosition) {
                                try {
                                    Image img = new Image(new FileInputStream(new File("data/img/check.png")), 16, 16, true, false);
                                    ImageView imgView = new ImageView(img);
                                    hBox.getChildren().add(imgView);
                                } catch (FileNotFoundException e) {
                                    LOGGER.error("Image located at data/img/check.png not found.", e);
                                }
                            }
                            activeCountries.getChildren().add(hBox);
                        });
                break;
            default:
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleException(ExceptionEvent event) {
        UIUtil.showException(event.getException(), globalConfiguration, message);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void update(DiffEvent event) {
        if (event.getIdGame().equals(game.getId())) {
            for (Diff diff : event.getResponse().getDiffs()) {
                if (gameConfig.getVersionGame() >= diff.getVersionGame()) {
                    continue;
                }
                switch (diff.getTypeObject()) {
                    case COUNTRY:
                        updateCountry(game, diff);
                        break;
                    case COUNTER:
                        updateCounter(game, diff);
                        break;
                    case STACK:
                        updateStack(game, diff);
                        break;
                    case ROOM:
                        updateRoom(game, diff);
                        break;
                    case ECO_SHEET:
                        updateEcoSheet(game, diff);
                        break;
                    case ADM_ACT:
                        updateAdmAct(game, diff);
                        break;
                    case STATUS:
                        updateStatus(game, diff);
                        break;
                    case TURN_ORDER:
                        updateTurnOrder(game, diff);
                        break;
                    default:
                        LOGGER.error("Unknown diff " + diff);
                        break;
                }
                map.update(diff);
                chatWindow.update(diff);
                ecoWindow.update(diff);
                adminActionsWindow.update(diff);
            }
            ecoWindow.updateComplete();

            event.getResponse().getMessages().forEach(message -> {
                if ((message.getIdRoom() == null && message.getId() > gameConfig.getMaxIdGlobalMessage())
                        || (message.getIdRoom() != null && message.getId() > gameConfig.getMaxIdMessage())) {
                    Message msg = new Message();
                    msg.setId(message.getId());
                    msg.setMessage(message.getMessage());
                    msg.setDateRead(message.getDateRead());
                    msg.setDateSent(message.getDateSent());
                    PlayableCountry country = CommonUtil.findFirst(game.getCountries(), playableCountry -> message.getIdSender().equals(playableCountry.getId()));
                    msg.setSender(country);
                    if (message.getIdRoom() == null) {
                        game.getChat().getGlobalMessages().add(msg);
                    } else {
                        Room room = CommonUtil.findFirst(game.getChat().getRooms(), room1 -> message.getIdRoom().equals(room1.getId()));
                        if (room != null) {
                            room.getMessages().add(msg);
                        }
                    }
                }
            });

            chatWindow.update(event.getResponse().getMessages());

            if (event.getResponse().getVersionGame() != null) {
                game.setVersion(event.getResponse().getVersionGame());
                gameConfig.setVersionGame(event.getResponse().getVersionGame());
            }
            Optional<MessageDiff> opt = event.getResponse().getMessages().stream().filter(messageDiff -> messageDiff.getIdRoom() == null).max((o1, o2) -> (int) (o1.getId() - o2.getId()));
            if (opt.isPresent() && opt.get().getId() > gameConfig.getMaxIdGlobalMessage()) {
                gameConfig.setMaxIdGlobalMessage(opt.get().getId());
            }
            opt = event.getResponse().getMessages().stream().filter(messageDiff -> messageDiff.getIdRoom() != null).max((o1, o2) -> (int) (o1.getId() - o2.getId()));
            if (opt.isPresent() && opt.get().getId() > gameConfig.getMaxIdMessage()) {
                gameConfig.setMaxIdMessage(opt.get().getId());
            }
        }
    }

    /**
     * Process a country diff event.
     *
     * @param game to update.
     * @param diff involving a country.
     */
    private void updateCountry(Game game, Diff diff) {
        switch (diff.getType()) {
            case MODIFY:
                modifyCountry(game, diff);
                break;
            default:
                LOGGER.error("Unknown diff " + diff);
                break;
        }
    }

    /**
     * Process the modify country diff event.
     *
     * @param game to update.
     * @param diff involving a modify country.
     */
    private void modifyCountry(Game game, Diff diff) {
        PlayableCountry country = game.getCountries().stream()
                .filter(c -> diff.getIdObject().equals(c.getId()))
                .findFirst()
                .orElse(null);

        if (country != null) {
            DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.DTI);
            if (attribute != null) {
                Integer dti = Integer.parseInt(attribute.getValue());
                country.setDti(dti);
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.FTI);
            if (attribute != null) {
                Integer fti = Integer.parseInt(attribute.getValue());
                country.setFti(fti);
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.FTI_ROTW);
            if (attribute != null) {
                Integer ftiRotw = Integer.parseInt(attribute.getValue());
                country.setFtiRotw(ftiRotw);
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TECH_LAND);
            if (attribute != null) {
                country.setLandTech(attribute.getValue());
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TECH_NAVAL);
            if (attribute != null) {
                country.setNavalTech(attribute.getValue());
            }
        } else {
            LOGGER.error("Invalid country in country modify event.");
        }
    }

    /**
     * Process a counter diff event.
     *
     * @param game to update.
     * @param diff involving a counter.
     */
    private void updateCounter(Game game, Diff diff) {
        switch (diff.getType()) {
            case ADD:
                addCounter(game, diff);
                break;
            case MOVE:
                moveCounter(game, diff);
                break;
            case REMOVE:
                removeCounter(game, diff);
                break;
            case MODIFY:
                modifyCounter(game, diff);
                break;
            default:
                LOGGER.error("Unknown diff " + diff);
                break;
        }
    }

    /**
     * Process the add counter diff event.
     *
     * @param game to update.
     * @param diff involving a add counter.
     */
    private void addCounter(Game game, Diff diff) {
        Stack stack;
        boolean newStack = false;
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STACK);
        if (attribute != null) {
            Long idStack = Long.parseLong(attribute.getValue());
            stack = findFirst(game.getStacks(), stack1 -> idStack.equals(stack1.getId()));
            if (stack == null) {
                stack = new Stack();
                stack.setId(idStack);
                game.getStacks().add(stack);
                newStack = true;
            }
        } else {
            LOGGER.error("Missing stack id in counter add event.");
            stack = new Stack();
            newStack = true;
        }

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.PROVINCE);
        if (attribute != null) {
            stack.setProvince(attribute.getValue());
        } else {
            LOGGER.error("Missing province in counter add event.");
        }

        Counter counter = new Counter();
        counter.setId(diff.getIdObject());
        counter.setOwner(stack);
        stack.getCounters().add(counter);

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TYPE);
        if (attribute != null) {
            counter.setType(CounterFaceTypeEnum.valueOf(attribute.getValue()));
        } else {
            LOGGER.error("Missing type in counter add event.");
        }

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.COUNTRY);
        if (attribute != null) {
            counter.setCountry(attribute.getValue());
            if (newStack) {
                stack.setCountry(attribute.getValue());
            }
        } else {
            LOGGER.error("Missing country in counter add event.");
        }

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.LEVEL);
        if (attribute != null) {
            Integer level = Integer.parseInt(attribute.getValue());
            updateCounterLevel(counter, level, game);
        }
    }

    /**
     * Process the move counter diff event.
     *
     * @param game to update.
     * @param diff involving a move counter.
     */
    private void moveCounter(Game game, Diff diff) {
        Stack stack = null;
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STACK_FROM);
        if (attribute != null) {
            Long idStack = Long.parseLong(attribute.getValue());
            stack = findFirst(game.getStacks(), stack1 -> idStack.equals(stack1.getId()));
        }
        if (stack == null) {
            LOGGER.error("Missing stack from in counter move event.");
            return;
        }

        Counter counter = findFirst(stack.getCounters(), counter1 -> diff.getIdObject().equals(counter1.getId()));
        if (counter == null) {
            LOGGER.error("Missing counter in counter move event.");
            return;
        }

        Stack stackTo;
        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STACK_TO);
        if (attribute != null) {
            Long idStack = Long.parseLong(attribute.getValue());
            stackTo = findFirst(game.getStacks(), stack1 -> idStack.equals(stack1.getId()));
            if (stackTo == null) {
                stackTo = new Stack();
                stackTo.setId(idStack);
                stackTo.setCountry(counter.getCountry());

                attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.PROVINCE_TO);
                if (attribute != null) {
                    stackTo.setProvince(attribute.getValue());
                } else {
                    LOGGER.error("Missing province_to in counter move event.");
                }

                game.getStacks().add(stackTo);
            }
        } else {
            LOGGER.error("Missing stack id in counter add event.");
            stackTo = new Stack();
            stackTo.setCountry(counter.getCountry());
        }

        stack.getCounters().remove(counter);
        stackTo.getCounters().add(counter);
        counter.setOwner(stackTo);

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STACK_DEL);
        if (attribute != null) {
            destroyStack(game, attribute);
        }
    }

    /**
     * Process the remove counter diff event.
     *
     * @param game to update.
     * @param diff involving a remove counter.
     */
    private void removeCounter(Game game, Diff diff) {
        Stack stack = null;
        Counter counter = null;
        for (Stack stackVo : game.getStacks()) {
            for (Counter counterVo : stackVo.getCounters()) {
                if (diff.getIdObject().equals(counterVo.getId())) {
                    counter = counterVo;
                    stack = stackVo;
                    break;
                }
            }
            if (counter != null) {
                break;
            }
        }

        if (counter == null) {
            LOGGER.error("Missing counter in counter remove event.");
            return;
        }

        stack.getCounters().remove(counter);

        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STACK_DEL);
        if (attribute != null) {
            Long idStack = Long.parseLong(attribute.getValue());
            if (idStack.equals(stack.getId())) {
                game.getStacks().remove(stack);
            } else {
                LOGGER.error("Stack to del is not the counter owner in counter remove event.");
            }
        }

        updateCounterLevel(counter, 0, game);
    }

    /**
     * Process the modify counter diff event.
     *
     * @param game to update.
     * @param diff involving a modify counter.
     */
    private void modifyCounter(Game game, Diff diff) {
        Counter counter = findFirst(game.getStacks().stream()
                        .flatMap(stack -> stack.getCounters().stream()),
                counter1 -> diff.getIdObject().equals(counter1.getId()));
        if (counter == null) {
            LOGGER.error("Missing counter in counter move event.");
            return;
        }

        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TYPE);
        if (attribute != null) {
            CounterFaceTypeEnum type = CounterFaceTypeEnum.valueOf(attribute.getValue());
            counter.setType(type);
        }
        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.VETERANS);
        if (attribute != null) {
            Double veterans = Double.valueOf(attribute.getValue());
            counter.setVeterans(veterans);
        }
        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.LEVEL);
        if (attribute != null) {
            Integer level = Integer.parseInt(attribute.getValue());
            updateCounterLevel(counter, level, game);
        }
    }

    /**
     * Update the level of a counter.
     *
     * @param counter whose level changed.
     * @param level   for trade fleet or establishment.
     * @param game    to update.
     */
    private void updateCounterLevel(Counter counter, Integer level, Game game) {
        if (CounterUtil.isTradingFleet(counter.getType())) {
            TradeFleet tradeFleet = game.getTradeFleets().stream()
                    .filter(tf -> StringUtils.equals(tf.getProvince(), counter.getOwner().getProvince()) &&
                            StringUtils.equals(tf.getCountry(), counter.getCountry()))
                    .findFirst()
                    .orElse(null);

            if (tradeFleet == null) {
                tradeFleet = new TradeFleet();
                tradeFleet.setCountry(counter.getCountry());
                tradeFleet.setProvince(counter.getOwner().getProvince());
                game.getTradeFleets().add(tradeFleet);
            }

            tradeFleet.setLevel(level);
        } else if (CounterUtil.isEstablishment(counter.getType())) {
            LOGGER.error("Establishment not yet implemented.");
        } else if (level != 0) {
            LOGGER.error("Unknown effect of level for this type: " + counter.getType());
        }
    }

    /**
     * Process a stack diff event.
     *
     * @param game to update.
     * @param diff involving a counter.
     */
    private void updateStack(Game game, Diff diff) {
        switch (diff.getType()) {
            case ADD:
                break;
            case MOVE:
                moveStack(game, diff);
                break;
            case MODIFY:
                modifyStack(game, diff);
                break;
            case REMOVE:
                break;
            default:
                LOGGER.error("Unknown diff " + diff);
                break;
        }
    }

    /**
     * Process the move stack diff event.
     *
     * @param game to update.
     * @param diff involving a move stack.
     */
    private void moveStack(Game game, Diff diff) {
        Stack stack;
        Long idStack = diff.getIdObject();
        stack = findFirst(game.getStacks(), stack1 -> idStack.equals(stack1.getId()));
        if (stack == null) {
            LOGGER.error("Missing stack in stack move event.");
            return;
        }

        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.PROVINCE_FROM);
        if (attribute != null) {
            if (!StringUtils.equals(attribute.getValue(), stack.getProvince())) {
                LOGGER.error("Stack was not in from province in stack move event.");
            }
        } else {
            LOGGER.error("Missing province from in stack move event.");
        }

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.PROVINCE_TO);
        if (attribute != null) {
            stack.setProvince(attribute.getValue());
        } else {
            LOGGER.error("Missing province to in stack move event.");
        }

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.MOVE_POINTS);
        if (attribute != null) {
            stack.setMove(Integer.parseInt(attribute.getValue()));
        }

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.MOVE_PHASE);
        if (attribute != null) {
            stack.setMovePhase(MovePhaseEnum.valueOf(attribute.getValue()));
        }
    }

    /**
     * Process the modify stack diff event.
     *
     * @param game to update.
     * @param diff involving a modify stack.
     */
    private void modifyStack(Game game, Diff diff) {
        Stack stack;
        Long idStack = diff.getIdObject();
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.MOVE_PHASE);
        if (idStack != null) {
            stack = findFirst(game.getStacks(), stack1 -> idStack.equals(stack1.getId()));
            if (stack != null) {
                if (attribute != null) {
                    stack.setMovePhase(MovePhaseEnum.valueOf(attribute.getValue()));
                }

                attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.COUNTRY);
                if (attribute != null) {
                    stack.setCountry(attribute.getValue());
                }
            }
        } else if (attribute != null && StringUtils.equals(attribute.getValue(), MovePhaseEnum.NOT_MOVED.name())) {
            // If no stack set and new move phase is NOT_MOVED, then it is the reset of each round of MOVED stacks.
            game.getStacks().stream()
                    .filter(stack1 -> stack1.getMovePhase() == MovePhaseEnum.MOVED)
                    .forEach(stack1 -> {
                        stack1.setMove(0);
                        stack1.setMovePhase(MovePhaseEnum.NOT_MOVED);
                    });
        }
    }

    /**
     * Generic destroyStack diff update.
     *
     * @param game      to update.
     * @param attribute of type destroy stack.
     */
    private void destroyStack(Game game, DiffAttributes attribute) {
        Long idStack = Long.parseLong(attribute.getValue());
        Stack stack = findFirst(game.getStacks(), stack1 -> idStack.equals(stack1.getId()));
        if (stack != null) {
            game.getStacks().remove(stack);
        } else {
            LOGGER.error("Missing stack for destroy stack generic event.");
        }
    }

    /**
     * Process a room diff event.
     *
     * @param game to update.
     * @param diff involving a room.
     */
    private void updateRoom(Game game, Diff diff) {
        switch (diff.getType()) {
            case ADD:
                addRoom(game, diff);
                break;
            case LINK:
                inviteKickRoom(game, diff);
                break;
            default:
                LOGGER.error("Unknown diff " + diff);
                break;
        }
    }

    /**
     * Process the add room diff event.
     *
     * @param game to update.
     * @param diff involving a add room.
     */
    private void addRoom(Game game, Diff diff) {
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null) {
            Long idCountry = Long.parseLong(attribute.getValue());

            if (idCountry.equals(gameConfig.getIdCountry())) {
                Room room = new Room();
                room.setId(diff.getIdObject());
                attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.NAME);
                if (attribute != null) {
                    room.setName(attribute.getValue());
                } else {
                    LOGGER.error("Missing name in room add event.");
                }
                room.setVisible(true);
                room.setPresent(true);
                PlayableCountry country = findFirst(game.getCountries(), country1 -> idCountry.equals(country1.getId()));
                room.setOwner(country);
                room.getCountries().add(country);

                game.getChat().getRooms().add(room);
            }
        } else {
            LOGGER.error("Missing country id in counter add event.");
        }
    }

    /**
     * Process the link room diff event.
     *
     * @param game to update.
     * @param diff involving a add room.
     */
    private void inviteKickRoom(Game game, Diff diff) {
        boolean invite = false;
        Long idRoom = diff.getIdObject();
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.INVITE);
        if (attribute != null) {
            invite = Boolean.parseBoolean(attribute.getValue());
        }

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null) {
            Long idCountry = Long.parseLong(attribute.getValue());

            if (idCountry.equals(gameConfig.getIdCountry())) {

                Room room = CommonUtil.findFirst(game.getChat().getRooms(), room1 -> idRoom.equals(room1.getId()));
                if (room == null) {
                    SimpleRequest<LoadRoomRequest> request = new SimpleRequest<>();
                    authentHolder.fillAuthentInfo(request);
                    request.setRequest(new LoadRoomRequest(gameConfig.getIdGame(), gameConfig.getIdCountry(), idRoom));
                    try {
                        room = chatService.loadRoom(request);
                        game.getChat().getRooms().add(room);
                    } catch (FunctionalException e) {
                        LOGGER.error("Can't load room.", e);
                    }

                    return;
                } else {
                    room.setPresent(invite);
                }
            }
            Room room = CommonUtil.findFirst(game.getChat().getRooms(), room1 -> idRoom.equals(room1.getId()));
            PlayableCountry country = CommonUtil.findFirst(game.getCountries(), playableCountry -> idCountry.equals(playableCountry.getId()));
            if (room != null && country != null) {
                if (invite) {
                    room.getCountries().add(country);
                } else {
                    room.getCountries().remove(country);
                }
            }
        } else {
            LOGGER.error("Missing country id in counter add event.");
        }
    }

    /**
     * Process a economical sheet diff event.
     *
     * @param game to update.
     * @param diff involving an economical sheet.
     */
    private void updateEcoSheet(Game game, Diff diff) {
        switch (diff.getType()) {
            case INVALIDATE:
                invalidateSheet(game, diff);
                break;
            default:
                LOGGER.error("Unknown diff " + diff);
                break;
        }
    }

    /**
     * Process the invalidate sheet diff event.
     *
     * @param game to update.
     * @param diff involving an invalidate sheet.
     */
    private void invalidateSheet(Game game, Diff diff) {
        Long idCountry = null;
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null && !StringUtils.isEmpty(attribute.getValue())) {
            idCountry = Long.parseLong(attribute.getValue());
        }

        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TURN);
        if (attribute != null) {
            Integer turn = Integer.parseInt(attribute.getValue());
            SimpleRequest<LoadEcoSheetsRequest> request = new SimpleRequest<>();
            authentHolder.fillAuthentInfo(request);
            request.setRequest(new LoadEcoSheetsRequest(gameConfig.getIdGame(), idCountry, turn));
            try {
                java.util.List<EconomicalSheetCountry> sheets = economicService.loadEconomicSheets(request);

                if (sheets != null) {
                    for (EconomicalSheetCountry sheet : sheets) {
                        PlayableCountry country = CommonUtil.findFirst(game.getCountries(), playableCountry -> sheet.getIdCountry().equals(playableCountry.getId()));
                        if (country != null) {
                            int index = country.getEconomicalSheets().indexOf(
                                    CommonUtil.findFirst(country.getEconomicalSheets(), o -> o.getId().equals(sheet.getSheet().getId())));
                            if (index != -1) {
                                country.getEconomicalSheets().set(index, sheet.getSheet());
                            } else {
                                country.getEconomicalSheets().add(sheet.getSheet());
                            }
                        }
                    }
                }
            } catch (FunctionalException e) {
                LOGGER.error("Can't load economic sheets.", e);
            }
        } else {
            LOGGER.error("Missing turn in invalidate sheet event.");
        }
    }

    /**
     * Process a administrative action diff event.
     *
     * @param game to update.
     * @param diff involving an administrative action.
     */
    private void updateAdmAct(Game game, Diff diff) {
        switch (diff.getType()) {
            case ADD:
                addAdmAct(game, diff);
                break;
            case REMOVE:
                removeAdmAct(game, diff);
                break;
            case VALIDATE:
                validateAdmAct(game, diff);
                break;
            default:
                LOGGER.error("Unknown diff " + diff);
                break;
        }
    }

    /**
     * Process the add administrative action diff event.
     *
     * @param game to update.
     * @param diff involving a add administrative action.
     */
    private void addAdmAct(Game game, Diff diff) {
        PlayableCountry country = null;
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null) {
            Long idCountry = Long.parseLong(attribute.getValue());
            country = findFirst(game.getCountries(), c -> idCountry.equals(c.getId()));
        }

        if (country != null) {
            AdministrativeAction admAct = new AdministrativeAction();
            admAct.setId(diff.getIdObject());
            admAct.setStatus(AdminActionStatusEnum.PLANNED);
            country.getAdministrativeActions().add(admAct);

            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TURN);
            if (attribute != null) {
                admAct.setTurn(Integer.parseInt(attribute.getValue()));
            } else {
                LOGGER.error("Missing turn in adm act add event.");
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TYPE);
            if (attribute != null) {
                admAct.setType(AdminActionTypeEnum.valueOf(attribute.getValue()));
            } else {
                LOGGER.error("Missing type in adm act add event.");
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.COST);
            if (attribute != null) {
                admAct.setCost(Integer.parseInt(attribute.getValue()));
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_OBJECT);
            if (attribute != null) {
                admAct.setIdObject(Long.parseLong(attribute.getValue()));
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.PROVINCE);
            if (attribute != null) {
                admAct.setProvince(attribute.getValue());
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.COUNTER_FACE_TYPE);
            if (attribute != null) {
                admAct.setCounterFaceType(CounterFaceTypeEnum.valueOf(attribute.getValue()));
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.COLUMN);
            if (attribute != null) {
                admAct.setColumn(Integer.parseInt(attribute.getValue()));
            }
            attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.BONUS);
            if (attribute != null) {
                admAct.setBonus(Integer.parseInt(attribute.getValue()));
            }
        } else {
            LOGGER.error("Missing or wrong country in adm act add event.");
        }
    }

    /**
     * Process the remove administrative action diff event.
     *
     * @param game to update.
     * @param diff involving a remove administrative action.
     */
    private void removeAdmAct(Game game, Diff diff) {
        PlayableCountry country = null;
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null) {
            Long idCountry = Long.parseLong(attribute.getValue());
            country = findFirst(game.getCountries(), c -> idCountry.equals(c.getId()));
        }

        if (country != null) {
            AdministrativeAction admAct = CommonUtil.findFirst(country.getAdministrativeActions().stream(), action -> action.getId().equals(diff.getIdObject()));
            if (admAct != null) {
                country.getAdministrativeActions().remove(admAct);
            } else {
                LOGGER.error("Wrong administrative action id in adm act remove event.");
            }
        } else {
            LOGGER.error("Missing or wrong country in adm act remove event.");
        }
    }

    /**
     * Process the validate administrative action event.
     *
     * @param game to update.
     * @param diff involving a validate administrative action.
     */
    private void validateAdmAct(Game game, Diff diff) {
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.TURN);
        if (attribute != null) {
            Integer turn = Integer.parseInt(attribute.getValue());
            SimpleRequest<LoadAdminActionsRequest> request = new SimpleRequest<>();
            authentHolder.fillAuthentInfo(request);
            request.setRequest(new LoadAdminActionsRequest(gameConfig.getIdGame(), turn));
            try {
                java.util.List<AdministrativeActionCountry> actions = economicService.loadAdminActions(request);

                for (AdministrativeActionCountry action : actions) {
                    PlayableCountry country = CommonUtil.findFirst(game.getCountries(), playableCountry -> action.getIdCountry().equals(playableCountry.getId()));
                    if (country != null) {
                        int index = country.getAdministrativeActions().indexOf(
                                CommonUtil.findFirst(country.getAdministrativeActions(), o -> o.getId().equals(action.getAction().getId())));
                        if (index != -1) {
                            country.getAdministrativeActions().set(index, action.getAction());
                        } else {
                            country.getAdministrativeActions().add(action.getAction());
                        }
                    }
                }
            } catch (FunctionalException e) {
                LOGGER.error("Can't load administrative actions.", e);
            }

            SimpleRequest<LoadCompetitionsRequest> requestComp = new SimpleRequest<>();
            authentHolder.fillAuthentInfo(requestComp);
            requestComp.setRequest(new LoadCompetitionsRequest(gameConfig.getIdGame(), turn));
            try {
                java.util.List<Competition> competitions = economicService.loadCompetitions(requestComp);

                if (CollectionUtils.isNotEmpty(competitions)) {
                    game.getCompetitions().addAll(competitions);
                }
            } catch (FunctionalException e) {
                LOGGER.error("Can't load competitions.", e);
            }
        } else {
            LOGGER.error("Missing turn in invalidate administrative action event.");
        }
    }

    /**
     * Process a status diff event.
     *
     * @param game to update.
     * @param diff involving a status.
     */
    private void updateStatus(Game game, Diff diff) {
        switch (diff.getType()) {
            case MODIFY:
                modifyStatus(game, diff);
                break;
            case VALIDATE:
                validateStatus(game, diff);
                break;
            case INVALIDATE:
                invalidateStatus(game, diff);
                break;
            default:
                LOGGER.error("Unknown diff " + diff);
                break;
        }
        updateTitle();
        updateActivePlayers();
    }

    /**
     * Process the modify status action diff event.
     *
     * @param game to update.
     * @param diff involving a modify status.
     */
    private void modifyStatus(Game game, Diff diff) {
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STATUS);
        if (attribute != null) {
            game.setStatus(GameStatusEnum.valueOf(attribute.getValue()));

            // New turn order for military phase
            if (game.getStatus() == GameStatusEnum.MILITARY_MOVE) {
                SimpleRequest<LoadTurnOrderRequest> request = new SimpleRequest<>();
                authentHolder.fillAuthentInfo(request);
                request.setRequest(new LoadTurnOrderRequest(gameConfig.getIdGame(), GameStatusEnum.MILITARY_MOVE));
                try {
                    List<CountryOrder> orders = gameService.loadTurnOrder(request);

                    game.getOrders().removeAll(game.getOrders().stream()
                            .filter(order -> order.getGameStatus() == GameStatusEnum.MILITARY_MOVE)
                            .collect(Collectors.toList()));
                    game.getOrders().addAll(orders);
                } catch (FunctionalException e) {
                    LOGGER.error("Can't load turn order.", e);
                }
            }
        }
    }

    /**
     * Process the validate status action diff event.
     *
     * @param game to update.
     * @param diff involving a validatation status.
     */
    private void validateStatus(Game game, Diff diff) {
        PlayableCountry country = null;
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null) {
            Long idCountry = Long.parseLong(attribute.getValue());
            country = findFirst(game.getCountries(), c -> idCountry.equals(c.getId()));
        }

        switch (game.getStatus()) {
            case ADMINISTRATIVE_ACTIONS_CHOICE:
                if (country != null) {
                    country.setReady(true);
                } else {
                    game.getCountries().stream()
                            .filter(c -> StringUtils.isNotEmpty(c.getUsername()))
                            .forEach(c -> c.setReady(true));
                }
                break;
            case MILITARY_MOVE:
                if (country != null) {
                    Long idCountry = country.getId();
                    game.getOrders().stream()
                            .filter(order -> order.getCountry().getId().equals(idCountry) &&
                                    order.isActive())
                            .forEach(order -> order.setActive(false));
                } else {
                    game.getOrders().stream()
                            .forEach(order -> order.setActive(false));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Process the invalidate status action diff event.
     *
     * @param game to update.
     * @param diff involving an invalidattion status.
     */
    private void invalidateStatus(Game game, Diff diff) {
        PlayableCountry country = null;
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null) {
            Long idCountry = Long.parseLong(attribute.getValue());
            country = findFirst(game.getCountries(), c -> idCountry.equals(c.getId()));
        }

        switch (game.getStatus()) {
            case ADMINISTRATIVE_ACTIONS_CHOICE:
                if (country != null) {
                    country.setReady(false);
                } else {
                    game.getCountries().stream()
                            .filter(c -> StringUtils.isNotEmpty(c.getUsername()))
                            .forEach(c -> c.setReady(false));
                }
                break;
            case MILITARY_MOVE:
            default:
                break;
        }
    }

    /**
     * Process a turn order diff event.
     *
     * @param game to update.
     * @param diff involving a turn order.
     */
    private void updateTurnOrder(Game game, Diff diff) {
        switch (diff.getType()) {
            case VALIDATE:
                validateTurnOrder(game, diff);
                break;
            case INVALIDATE:
                invalidateTurnOrder(game, diff);
                break;
            case MODIFY:
                modifyTurnOrder(game, diff);
                break;
            default:
                LOGGER.error("Unknown diff " + diff);
                break;
        }
        updateActivePlayers();
    }

    /**
     * Process the validate turn order diff event.
     *
     * @param game to update.
     * @param diff involving an validate turn order.
     */
    private void validateTurnOrder(Game game, Diff diff) {
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STATUS);
        if (StringUtils.isEmpty(attribute.getValue())) {
            LOGGER.error("Missing status in modify turn order event.");
        }
        GameStatusEnum gameStatus = GameStatusEnum.valueOf(attribute.getValue());

        Long tmp = null;
        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null) {
            tmp = Long.parseLong(attribute.getValue());
        }
        Long idCountry = tmp;

        game.getOrders().stream()
                .filter(o -> o.getGameStatus() == gameStatus &&
                        (idCountry == null || idCountry.equals(o.getCountry().getId())))
                .forEach(o -> o.setReady(true));
    }

    /**
     * Process the invalidate turn order diff event.
     *
     * @param game to update.
     * @param diff involving an invalidate turn order.
     */
    private void invalidateTurnOrder(Game game, Diff diff) {
        DiffAttributes attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STATUS);
        if (StringUtils.isEmpty(attribute.getValue())) {
            LOGGER.error("Missing status in modify turn order event.");
        }
        GameStatusEnum gameStatus = GameStatusEnum.valueOf(attribute.getValue());

        Long tmp = null;
        attribute = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ID_COUNTRY);
        if (attribute != null) {
            tmp = Long.parseLong(attribute.getValue());
        }
        Long idCountry = tmp;

        game.getOrders().stream()
                .filter(o -> o.getGameStatus() == gameStatus &&
                        (idCountry == null || idCountry.equals(o.getCountry().getId())))
                .forEach(o -> o.setReady(false));
    }

    /**
     * Process the modify turn order diff event.
     *
     * @param game to update.
     * @param diff involving an modify turn order.
     */
    private void modifyTurnOrder(Game game, Diff diff) {
        DiffAttributes attributeActive = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.ACTIVE);
        DiffAttributes attributeStatus = findFirst(diff.getAttributes(), attr -> attr.getType() == DiffAttributeTypeEnum.STATUS);
        if (StringUtils.isEmpty(attributeActive.getValue())) {
            LOGGER.error("Missing active in modify turn order event.");
        }
        if (StringUtils.isEmpty(attributeStatus.getValue())) {
            LOGGER.error("Missing status in modify turn order event.");
        }
        int position = Integer.valueOf(attributeActive.getValue());
        GameStatusEnum gameStatus = GameStatusEnum.valueOf(attributeStatus.getValue());

        game.getOrders().stream()
                .filter(o -> o.getGameStatus() == gameStatus)
                .forEach(o -> o.setActive(false));
        game.getOrders().stream()
                .filter(o -> o.getGameStatus() == gameStatus &&
                        o.getPosition() == position)
                .forEach(o -> o.setActive(true));
    }

    /** {@inheritDoc} */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(WindowEvent event) {
        if (!closed) {
            map.destroy();
            client.setTerminate(true);
            closed = true;
        }
    }
}
