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

    // --- CAMBIO CLAVE: USAR initialize() ---
    // Este método lo llama JavaFX automáticamente al cargar el FXML.
    // Así los botones siempre funcionarán, vengas del Main o del LogOut.
    @FXML
    public void initialize() {
        LOGGER.info("Initializing SignIn window (Automatic).");

        // Listeners y lógica
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

    // Mantenemos este método vacío para compatibilidad con tu Main (CRUD_Project.java)
    // para que no te de error de compilación, pero ya no hace falta que haga nada.
    public void init(Stage stage) {
        // Ya se ha inicializado en el método initialize() de arriba.
        // Puedes dejarlo vacío o usarlo si necesitas configurar algo específico del Stage (título, icono).
    }

    public void init(Stage stage, Parent root) {
        // Compatibilidad
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

    // INICIO DE SESIÓN
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

            // ===== LOGIN ADMIN (TEMPORAL - va a Accounts para probar) =====
            if (user.equalsIgnoreCase("admin") && pass.equals("admin")) {

                // Crear un Customer dummy para testing
                Customer dummyCustomer = new Customer();
                dummyCustomer.setId(1L);
                dummyCustomer.setFirstName("Admin");
                dummyCustomer.setEmail("admin@test.com");

                // Cargar Account.fxml en lugar de Customer.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/CRUD_Project/ui/account.fxml"));
                Parent root = loader.load();

                Stage currentStage = (Stage) bLogIn.getScene().getWindow();
                currentStage.setScene(new Scene(root));
                currentStage.show();

                AccountController controller = loader.getController();
                controller.initData(currentStage, dummyCustomer);

                return; 
            }

            // ===== LOGIN NORMAL (con BD) =====
            CustomerRESTClient resCustomer = new CustomerRESTClient();
            Customer customer = resCustomer.findCustomerByEmailPassword_XML(
                    Customer.class,
                    user,
                    pass
            );
            resCustomer.close();

            // ABRIR LA VENTANA DE CUENTAS
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CRUD_Project/ui/account.fxml"));
            Parent root = loader.load();

            AccountController controller = loader.getController();
            
            // Obtenemos el Stage actual
            Stage currentStage = (Stage) bLogIn.getScene().getWindow();
            
            // Cambiamos la escena
            currentStage.setScene(new Scene(root));
            currentStage.show();

            // PASAMOS EL USUARIO LOGUEADO
            controller.initData(currentStage, customer);

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
            e.printStackTrace();
            new Alert(AlertType.INFORMATION,
                    "ERROR inesperado, pruebe mas tarde")
                    .showAndWait();
        }
    }

    //campos--------------------
    private void handletfUsernameTextChange(ObservableValue observable, String oldValue, String newValue) {
        try {
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

    private void handletfUsernameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            LOGGER.info("onFocus");
            tfUsername.requestFocus();
        } else if (oldValue) {
            LOGGER.info("onBlur");
        }
    }

    private void handleftPasswordTextChange(ObservableValue observable, String oldValue, String newValue) {
        try {
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

    private void handletfPasswordFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            LOGGER.info("onFocus");
            pfPassword.requestFocus();
        } else if (oldValue) {
            LOGGER.info("onBlur");
        }
    }
}