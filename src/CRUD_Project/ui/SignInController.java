/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CRUD_Project.ui;

import java.io.IOException;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import CRUD_Project.logic.CustomerRESTClient;
import CRUD_Project.model.Customer;

/**
 *
 * @author Imad
 */
public class SignInController {

    @FXML
    private TextField tfUsername;
    @FXML
    private PasswordField pfPassword;
    @FXML
    private Button bLogIn;
    @FXML
    private Hyperlink Hipervinculo;
    @FXML
    private Button bExit;

    private static final Logger LOGGER = Logger.getLogger("proyectosignup.signup.ui");

    public void init(Stage stage, Parent root) {
        LOGGER.info("Initializing window.");
        Scene scene = new Scene(root);
        stage.setScene(scene);
        //establecer titulo de la ventana
        stage.setTitle("Sign in");
        //La ventana no debe ser redimensionable.
        stage.setResizable(false);
        //desabilitar botones
        //boton.setDisable(true);
        //Los campos de username y password tendrán un texto de fondo.
        //Se enfoca inicialmente en el campo username.
        //asociar eventos a manejadores
        bExit.setOnAction(this::handlebExitMethod);
        bLogIn.setOnAction(this::handlebLogInMethod);
        Hipervinculo.setOnAction(this::handleHiperVinculoMethod);
        //asociacion de manejadores a properties
        tfUsername.textProperty().addListener(this::handletfUsernameTextChange);
        tfUsername.focusedProperty().addListener(this::handletfUsernameFocusChange);

        pfPassword.textProperty().addListener(this::handleftPasswordTextChange);
        pfPassword.focusedProperty().addListener(this::handletfPasswordFocusChange);

        //Mostar la ventana
        stage.show();

    }

    //hipervinculo, inicia la ventana signup y cierra signin
    private void handleHiperVinculoMethod(ActionEvent event) {
        try {
            // Obtener el Stage actual
            Stage currentStage = (Stage) Hipervinculo.getScene().getWindow();

            // Cargar el nuevo FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SignUP.fxml"));
            Parent root = loader.load();

            // Inicializar el nuevo controlador en el mismo Stage
            SignUPController controller = loader.getController();
            controller.init(currentStage); // ← tu método original

            // Crear nueva escena y asignarla al Stage actual
            Scene newScene = new Scene(root);
            currentStage.setScene(newScene);
            currentStage.setTitle("Sign Up");
            currentStage.show();

        } catch (IOException e) {
            LOGGER.severe("Error al abrir ventana de Sign Up: " + e.getMessage());
            new Alert(Alert.AlertType.ERROR, "Error al cargar la ventana de registro").showAndWait();
        }
    }

    //botones-------------------
    //cierra el programa
    private void handlebExitMethod(ActionEvent event) {
        Platform.exit();
    }

    //cierra signin y abre la ventana changepassword
    private void handlebLogInMethod(ActionEvent event) {
        try {
            if (this.tfUsername.getText().trim().equals("") || this.pfPassword.getText().trim().equals("")) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Los campos usuario y contraseña \n deben estar informados",
                        ButtonType.OK);
                alert.showAndWait();

                if (this.tfUsername.getText().trim().equals("")) {
                    tfUsername.setStyle("-fx-border-color: red;");
                }
                if (this.pfPassword.getText().trim().equals("")) {
                    pfPassword.setStyle("-fx-border-color: red;");
                }
                return;
            }

            CustomerRESTClient resCustomer = new CustomerRESTClient();
            Customer customer = resCustomer.findCustomerByEmailPassword_XML(
                    Customer.class,
                    tfUsername.getText().trim(),
                    pfPassword.getText().trim()
            );

            resCustomer.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CambioContraseña.fxml"));
            Parent root = loader.load();

            // Obtener controlador de Change Password
            ChangeController controller = loader.getController();

            controller.setCustomer(customer);

            // Inicializar ventana
            Stage currentStage = (Stage) bLogIn.getScene().getWindow();
            controller.init(currentStage, root);

        } catch (InternalServerErrorException e) {
            LOGGER.warning(e.getLocalizedMessage());
            new Alert(AlertType.INFORMATION,
                    "ERROR: Problemas con el servidor, pruebe más tarde.")
                    .showAndWait();
        } catch (NotAuthorizedException e) {
            LOGGER.warning(e.getLocalizedMessage());
            new Alert(AlertType.INFORMATION,
                    "ERROR: Usuario o Contraseña incorrectos")
                    .showAndWait();
        } catch (Exception e) {
            LOGGER.warning(e.getLocalizedMessage());
            new Alert(AlertType.INFORMATION,
                    "ERROR inesperado, pruebe mas tarde")
                    .showAndWait();
        }
    }

    //campos--------------------
    /**
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    //username
    private void handletfUsernameTextChange(ObservableValue observable, String oldValue, String newValue) {
        try {

            //Quitar borde rojo si el usuario escribe algo
            if (!newValue.trim().isEmpty()) {
                tfUsername.setStyle(null);
            }

            if (newValue.length() > 200) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Usuario demasiado largo",
                        ButtonType.OK);
                alert.showAndWait();
                tfUsername.clear();
            }
        } catch (Exception e) {
            LOGGER.warning(e.getLocalizedMessage());
            new Alert(AlertType.INFORMATION, "ERROR: Usuario demasiado largo").showAndWait();
        }
    }

    /**
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    private void handletfUsernameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (newValue) {//si NewValue es true estas ganando el foco, y en else seria la perdida,y viceversa.  !oldValue, si lo niego, quiere decir q si era false,no estaba enfocado
            LOGGER.info("onFocus");
            tfUsername.requestFocus();
        } else if (oldValue) {
            LOGGER.info("onBlur");
        }
    }

    /**
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    //password
    private void handleftPasswordTextChange(ObservableValue observable, String oldValue, String newValue) {
        try {

            //Quitar borde rojo si el usuario escribe algo
            if (!newValue.trim().isEmpty()) {
                pfPassword.setStyle(null);
            }

            if (newValue.length() > 200) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Contraseña demasiada larga",
                        ButtonType.OK);
                alert.showAndWait();
                pfPassword.clear();
            }
        } catch (Exception e) {
            LOGGER.warning(e.getLocalizedMessage());
            new Alert(AlertType.INFORMATION, "ERROR: Contraseña demasiada larga").showAndWait();
        }
    }

    /**
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    private void handletfPasswordFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            LOGGER.info("onFocus");
            pfPassword.requestFocus();
        } else if (oldValue) {
            LOGGER.info("onBlur");
        }
    }

}
