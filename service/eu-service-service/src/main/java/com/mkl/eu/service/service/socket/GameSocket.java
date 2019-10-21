package com.mkl.eu.service.service.socket;

import com.mkl.eu.client.common.vo.SocketInfo;
import com.mkl.eu.client.service.vo.diff.DiffResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;

/**
 * Socket for a client server side.
 *
 * @author MKL.
 */
public class GameSocket implements Runnable {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketHandler.class);
    /** Socket to communicate with client. */
    private Socket socket;
    /** Output stream used to write response to the client. */
    private ObjectOutputStream outStream;
    /** Handler to be referenced to after the init. */
    private SocketHandler handler;
    /** Information about this socket (game id, login, ...). */
    private SocketInfo info;
    /** Terminate this process. */
    private boolean terminate;

    /**
     * Constructor.
     *
     * @param socket  the socket to set.
     * @param handler the handler to set.
     */
    public GameSocket(Socket socket, SocketHandler handler) {
        this.socket = socket;
        this.handler = handler;

        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            info = (SocketInfo) in.readObject();

            LOGGER.info("New client on game " + info.getIdGame() + " for player " + info.getIdCountry());

            outStream = new ObjectOutputStream(socket.getOutputStream());

            handler.addActiveClient(this, info.getIdGame());
        } catch (Exception e) {
            LOGGER.info("Error in client socket before init.", e);
        }
    }

    /** {@inheritDoc */
    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Object result;
            while ((result = in.readObject()) != null) {
                if (result instanceof String && StringUtils.equals("TERMINATE", (String) result)) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Client socket aborted: " + e.getMessage());
        }

        LOGGER.info("Closing client on game " + info.getIdGame() + " for player " + info.getIdCountry());
        handler.removeActiveClient(this, info.getIdGame());
    }

    /**
     * Push a response to a client.
     *
     * @param response        to push.
     * @param idCountries List of countries that will receive this response.
     */
    public void push(DiffResponse response, List<Long> idCountries) {
        response.getDiffs().removeIf(diff -> diff.getIdObject() != null && !Objects.equals(diff.getIdObject(), info.getIdCountry()));
        if (idCountries == null || idCountries.isEmpty() || idCountries.contains(info.getIdCountry())) {
            try {
                outStream.writeObject(response);
            } catch (SocketException e) {
                terminate = true;
            } catch (Exception e) {
                LOGGER.error("Error when sending response to client.", e);
            }
        }
    }
}
