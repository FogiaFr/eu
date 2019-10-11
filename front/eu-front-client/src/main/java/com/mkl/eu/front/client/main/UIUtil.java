package com.mkl.eu.front.client.main;

import com.mkl.eu.client.common.exception.FunctionalException;
import com.mkl.eu.client.common.exception.TechnicalException;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class for UI.
 *
 * @author MKL.
 */
public final class UIUtil {
    /**
     * Private constructor for Utility class.
     */
    private UIUtil() {

    }

    /**
     * @param countryCode         code of the country.
     * @param message             internationalization.
     * @param globalConfiguration configuration containing the locale.
     * @return the country name to be displayed given the country code and the locale.
     */
    public static String getCountryName(String countryCode, MessageSource message, GlobalConfiguration globalConfiguration) {
        if (StringUtils.isEmpty(countryCode)) {
            countryCode = "none";
        }

        return message.getMessage(countryCode, null, globalConfiguration.getLocale());
    }

    public static void doInJavaFx(Runnable runnable) {
        // With Platform.runLater, this dialog can be displayed
        // even in non-javaFX component (the map)
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    /**
     * Display an alert dialog displaying the exception.
     *
     * @param ex                  the exception.
     * @param globalConfiguration global configuration containing the Locale.
     * @param message             message source containing the internationalization.
     */
    public static void showException(Exception ex, GlobalConfiguration globalConfiguration, MessageSource message) {
        doInJavaFx(() -> showExceptionInternal(ex, globalConfiguration, message));
    }

    /**
     * Display an alert dialog displaying the exception.
     *
     * @param ex                  the exception.
     * @param globalConfiguration global configuration containing the Locale.
     * @param message             message source containing the internationalization.
     */
    private static void showExceptionInternal(Exception ex, GlobalConfiguration globalConfiguration, MessageSource message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(message.getMessage("exception.dialog.title", null, globalConfiguration.getLocale()));
        alert.setHeaderText(message.getMessage("exception.dialog.header", null, globalConfiguration.getLocale()));

        String code = "exception.unknown";
        Object[] params = null;
        if (ex instanceof TechnicalException) {
            code = ((TechnicalException) ex).getCode();
            params = ((TechnicalException) ex).getParams();
        } else if (ex instanceof FunctionalException) {
            code = ((FunctionalException) ex).getCode();
            params = ((FunctionalException) ex).getParams();
        }
        alert.setContentText(message.getMessage(code, params, globalConfiguration.getLocale()));


        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label(message.getMessage("exception.dialog.stacktrace", null, globalConfiguration.getLocale()));

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();

    }
}
