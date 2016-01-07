package com.mkl.eu.front.client.socket;

import com.mkl.eu.client.common.vo.SocketInfo;
import com.mkl.eu.client.service.vo.diff.DiffResponse;
import com.mkl.eu.front.client.event.AbstractDiffListenerContainer;
import com.mkl.eu.front.client.event.DiffEvent;
import com.mkl.eu.front.client.main.GameConfiguration;
import com.mkl.eu.front.client.vo.AuthentHolder;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Socket for the server client side.
 *
 * @author MKL.
 */
@Component
@Scope(value = "prototype")
public class ClientSocket extends AbstractDiffListenerContainer implements Runnable {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSocket.class);
    /** Component holding the authentication information. */
    @Autowired
    private AuthentHolder authentHolder;
    /** Socket to communicate with server. */
    private Socket socket;
    /** Configuration of the game. */
    private GameConfiguration gameConfig;
    /** Terminate this process. */
    private boolean terminate;

    /**
     * Constructor.
     *
     * @param gameConfig the gameConfig to set.
     */
    public ClientSocket(GameConfiguration gameConfig) {
        this.gameConfig = gameConfig;
    }

    /**
     * Initialize the socket.
     */
    @PostConstruct
    public void init() {
        try {
            socket = new Socket("127.0.0.1", 2009);

            SocketInfo info = new SocketInfo();
            info.setUsername(authentHolder.getUsername());
            info.setPassword(authentHolder.getPassword());
            info.setIdGame(gameConfig.getIdGame());
            info.setIdCountry(gameConfig.getIdCountry());

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(info);
        } catch (Exception e) {
            LOGGER.error("Error when initializing with server.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            DiffResponse response;

            while ((response = (DiffResponse) in.readObject()) != null) {
                LOGGER.info("Receiving a diff for game " + gameConfig.getIdGame());
                DiffEvent event = new DiffEvent(response, gameConfig.getIdGame());
                Platform.runLater(() -> processDiffEvent(event));
            }
        } catch (Exception e) {
            if (!terminate) {
                LOGGER.error("Error when communicating with server.", e);
            }
        }
    }

    /** @param terminate the terminate to set. */
    public void setTerminate(boolean terminate) {
        this.terminate = terminate;

        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.error("Error when closing the socket.", e);
        }
    }
}