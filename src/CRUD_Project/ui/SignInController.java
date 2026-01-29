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
import javafx.scene.Node;

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

    public void init(Stage stage) {
        LOGGER.info("Initializing window.");

        // solo listeners y lógica
        bExit.setOnAction(this::handlebExitMethod);
        bLogIn.setOnAction(this::handlebLogInMethod);
        Hipervinculo.setOnAction(this::handleHiperVinculoMethod);

        tfUsername.textProperty().addListener(this::handletfUsernameTextChange);
        tfUsername.focusedProperty().addListener(this::handletfUsernameFocusChange);

        pfPassword.textProperty().addListener(this::handleftPasswordTextChange);
        pfPassword.focusedProperty().addListener(this::handletfPasswordFocusChange);
        
        bExit.setCancelButton(true);
        bLogIn.setDefaultButton(true);
    }

    public void init(Stage stage, Parent root) {
        // no usar root, solo mantener compatibilidad
        init(stage);
    }

    //hipervinculo, inicia la ventana signup y cierra signin
    @FXML
    private void handleHiperVinculoMethod(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/CRUD_Project/ui/SignUp.fxml")
            );

            Parent root = loader.load();

            // coger el stage actual desde el evento
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Sign Up");
            stage.setResizable(false);

            // init del controller destino (solo lógica)
            SignUPController c = loader.getController();
            c.init(stage);

        } catch (Exception e) {
            e.printStackTrace();
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
            String user = tfUsername.getText().trim();
            String pass = pfPassword.getText().trim();

            // Validación campos vacíos
            if (user.equals("") || pass.equals("")) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Los campos usuario y contraseña \n deben estar informados",
                        ButtonType.OK);
                alert.showAndWait();

                if (user.equals("")) {
                    tfUsername.setStyle("-fx-border-color: red;");
                }
                if (pass.equals("")) {
                    pfPassword.setStyle("-fx-border-color: red;");
                }
                return;
            }

            // ===== LOGIN ADMIN (sin BD) =====
            if (user.equalsIgnoreCase("admin") && pass.equals("admin")) {

                // Cargar Customer.fxml y cambiar la escena en el mismo Stage
                FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer.fxml"));
                Parent root = loader.load();

                Stage currentStage = (Stage) bLogIn.getScene().getWindow();
                Scene newScene = new Scene(root);
                currentStage.setScene(newScene);
                currentStage.setTitle("Customers CRUD");
                currentStage.show();

                // Si tu CustomerController usa init(Stage) lo llamas aquí
                CustomerController controller = loader.getController();
                controller.init(currentStage);

                return; // importante: no seguir con login normal
            }

            // ===== LOGIN NORMAL (con BD) =====
            CustomerRESTClient resCustomer = new CustomerRESTClient();
            Customer customer = resCustomer.findCustomerByEmailPassword_XML(
                    Customer.class,
                    user,
                    pass
            );
            resCustomer.close();

            // Tu flujo normal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CambioContraseña.fxml"));
            Parent root = loader.load();

            ChangeController controller = loader.getController();
            controller.setCustomer(customer);

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
