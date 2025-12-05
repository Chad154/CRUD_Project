package CRUD_Project.ui;

import java.io.IOException;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import CRUD_Project.logic.CustomerRESTClient;
import CRUD_Project.model.Customer;

public class SignUPController {
    
    //Declaracion de las variables 
    @FXML
    private TextField tfName;
    @FXML
    private TextField tfLastname;
    @FXML
    private TextField tfMidleeInitial;
    @FXML
    private TextField tfStreet;
    @FXML
    private TextField tfCity;
    @FXML
    private TextField tfState;
    @FXML
    private TextField tfZip;
    @FXML
    private TextField tfPhone;
    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pwPassword;
    @FXML
    private PasswordField pfPasswordValidation;
    @FXML
    private Button btSignUp;
    @FXML
    private Button btExit;

    private static final Logger LOGGER = Logger.getLogger("proyectosignup.signin.ui");

    private Stage stage;

    //Metodo Init para la ventana
    public void init(Stage stage) {
        try {
            this.stage = stage;
            stage.setTitle("Sign Up");
            stage.setResizable(false);
            LOGGER.info("Initialize Sign Up Window");

            // estado inicial
            btSignUp.setDisable(true);
            btExit.setDisable(false);

            // foco inicial
            tfName.requestFocus();

            // manejadores de botones
            btExit.setOnAction(this::handlebExitMethod);
            btSignUp.setOnAction(this::handlebSignUpMethod);

            // habilitar el botón solo cuando todos los campos estén rellenos
            tfName.textProperty().addListener(this::validateForm);
            tfLastname.textProperty().addListener(this::validateForm);
            tfMidleeInitial.textProperty().addListener(this::validateForm);
            tfStreet.textProperty().addListener(this::validateForm);
            tfCity.textProperty().addListener(this::validateForm);
            tfState.textProperty().addListener(this::validateForm);
            tfZip.textProperty().addListener(this::validateForm);
            tfPhone.textProperty().addListener(this::validateForm);
            tfEmail.textProperty().addListener(this::validateForm);
            pwPassword.textProperty().addListener(this::validateForm);
            pfPasswordValidation.textProperty().addListener(this::validateForm);

        } catch (Exception e) {
            LOGGER.severe("Error inicializando la ventana: " + e.getMessage());
        }
    }

    // Aqui esta todo el codigo sobre el funcionamiento de los botones
    
    //Metodo para el funcionamiento al pulsar el boton de exit
    private void handlebExitMethod(ActionEvent event) {
        try {
            // Cerrar la ventana actual
            Stage ventanaActual = (Stage) btExit.getScene().getWindow();
            ventanaActual.close();

            // Cargar la ventana de Sign In
            FXMLLoader loader = new FXMLLoader(getClass().getResource("signin.fxml"));
            Parent root = loader.load();

            // Crear nueva ventana
            Stage nuevaVentana = new Stage();
            nuevaVentana.setTitle("Sign In");

            // Inicializar su controlador (EL init ya se encarga de crear la Scene)
            SignInController controller = loader.getController();
            controller.init(nuevaVentana, root);

            // Mostrarla (ya se hace dentro del init, pero si no, puedes dejarlo igual)
            nuevaVentana.show();

        } catch (IOException e) {
            LOGGER.severe("Error al abrir la ventana de Sign In: " + e.getMessage());
            new Alert(Alert.AlertType.ERROR, "Error al cargar la ventana de inicio de sesión").showAndWait();
        }
    }

    //Metodo sobre el comportamiento al pulsar el boton de SignUp
    private void handlebSignUpMethod(ActionEvent event) {
        CustomerRESTClient client = null;
        try {
            //Validacion de el tamaño del campo
            if (tfName.getText().trim().length() > 255) {
                new Alert(AlertType.INFORMATION, "El nombre no puede contener más de 255 caracteres").showAndWait();
                return;
            } else if (tfLastname.getText().trim().length() > 255) {
                new Alert(AlertType.INFORMATION, "El apellido no puede contener más de 255 caracteres").showAndWait();
                return;
            } else if (tfMidleeInitial.getText().trim().length() > 255) {
                new Alert(AlertType.INFORMATION, "El apartado MidleeInitial no puede contener \nmás de 255 caracteres").showAndWait();
                return;
            } else if (tfStreet.getText().trim().length() > 255) {
                new Alert(AlertType.INFORMATION, "La calle no puede contener más de 255 caracteres").showAndWait();
                return;
            } else if (tfCity.getText().trim().length() > 255) {
                new Alert(AlertType.INFORMATION, "La ciudad no puede contener más de 255 caracteres").showAndWait();
                return;
            } else if (tfState.getText().trim().length() > 255) {
                new Alert(AlertType.INFORMATION, "El estado no puede contener más de 255 caracteres").showAndWait();
                return;
            } else if (pwPassword.getText().trim().length() > 255) {
                new Alert(AlertType.INFORMATION, "La contraseña no puede contener más de 255 caracteres").showAndWait();
                return;
            }
            //Validacio de que el campo ZIP sean numeros
            if (!tfZip.getText().trim().matches("\\d+")) {
                new Alert(AlertType.INFORMATION, "El campo zip tienen que ser numeros").showAndWait();
                return;
            }
            // Validación ZIP no puede tener mas de 10 numeros
            if (tfZip.getText().trim().length() > 10) {
                new Alert(AlertType.INFORMATION, "El campo ZIP debe contener máximo 10 números.").showAndWait();
                return;
            }
            //Validacion de que el campo telefono sean numeros
            if (!tfPhone.getText().trim().matches("\\d+")) {
                new Alert(AlertType.INFORMATION, "El campo telefono deben ser numeros.").showAndWait();
                return;
            }
            // Validación Teléfono no puede tener mas de 19 numeros
            if (!tfPhone.getText().trim().matches("\\d{1,19}")) {
                new Alert(AlertType.INFORMATION, "El campo Teléfono debe contener máximo 19 números.").showAndWait();
                return;
            }

            // Validación Email debe tener algo antes y después de @
            if (!tfEmail.getText().trim().matches("^.+@.+\\..+$")) {
                new Alert(AlertType.INFORMATION, "Tu email no es valida debe tener \neste formato ejemplo@ejemplo.ejemplo.").showAndWait();
                return;
            }

            // Validación Contraseña: mínimo 8 caracteres
            if (pwPassword.getText().trim().length() < 8) {
                new Alert(AlertType.INFORMATION, "La contraseña debe tener al menos 8 caracteres.").showAndWait();
                return;
            }

            // Validar contraseñas iguales
            if (!pwPassword.getText().equals(pfPasswordValidation.getText())) {
                new Alert(AlertType.ERROR, "Las contraseñas no coinciden").showAndWait();
                return;
            }

            // Crear nuevo cliente
            Customer customer = new Customer();
            customer.setFirstName(tfName.getText().trim());
            customer.setLastName(tfLastname.getText().trim());
            customer.setMiddleInitial(tfMidleeInitial.getText().trim());
            customer.setStreet(tfStreet.getText().trim());
            customer.setCity(tfCity.getText().trim());
            customer.setState(tfState.getText().trim());
            customer.setZip(Integer.parseInt(tfZip.getText().trim()));
            customer.setPhone(Long.parseLong(tfPhone.getText().trim()));
            customer.setEmail(tfEmail.getText().trim());
            customer.setPassword(pwPassword.getText().trim());

            client = new CustomerRESTClient();
            client.create_XML(customer);
            client.close();

            new Alert(AlertType.INFORMATION, "Usuario creado correctamente").showAndWait();
            clearForm();

            //Manejo de excepciones
        } catch (InternalServerErrorException e) {
            LOGGER.warning(e.getLocalizedMessage());
            new Alert(AlertType.INFORMATION, "Error con el servidor").showAndWait();
        } catch (ForbiddenException e) {
            LOGGER.warning(e.getLocalizedMessage());
            new Alert(AlertType.INFORMATION, "Error: el email ya existe").showAndWait();
        } catch (NumberFormatException e) {
            new Alert(AlertType.ERROR, "El ZIP o el Teléfono deben ser numéricos").showAndWait();
        } catch (Exception e) {
            new Alert(AlertType.ERROR, "Error al crear usuario:\n" + e.getMessage()).showAndWait();
            LOGGER.severe("Error en SignUp: " + e.getMessage());
        }
    }

    // Limpia todos los campos
    private void clearForm() {
        tfName.clear();
        tfLastname.clear();
        tfMidleeInitial.clear();
        tfStreet.clear();
        tfCity.clear();
        tfState.clear();
        tfZip.clear();
        tfPhone.clear();
        tfEmail.clear();
        pwPassword.clear();
        pfPasswordValidation.clear();
        tfName.requestFocus();
    }

    // Valida si todos los campos están rellenados para habilitar el botón Sign Up
    private void validateForm(ObservableValue<? extends String> obs, String oldVal, String newVal) {
        boolean allFilled
                = !tfName.getText().trim().isEmpty()
                && !tfLastname.getText().trim().isEmpty()
                && !tfMidleeInitial.getText().trim().isEmpty()
                && !tfStreet.getText().trim().isEmpty()
                && !tfCity.getText().trim().isEmpty()
                && !tfState.getText().trim().isEmpty()
                && !tfZip.getText().trim().isEmpty()
                && !tfPhone.getText().trim().isEmpty()
                && !tfEmail.getText().trim().isEmpty()
                && !pwPassword.getText().trim().isEmpty()
                && !pfPasswordValidation.getText().trim().isEmpty();

        btSignUp.setDisable(!allFilled);
    }
}