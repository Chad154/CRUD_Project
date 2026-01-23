/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package CRUD_Project.ui;

import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javax.ws.rs.InternalServerErrorException;
import CRUD_Project.logic.CustomerRESTClient;
import CRUD_Project.model.Customer;
/**
 * Controlador para la ventana de cambio de contraseña.
 * Gestiona la interfaz de usuario y la lógica para cambiar la contraseña del cliente.
 * 
 * @author Daniel López López
 */
public class ChangeController {
    
    /**
     * Campo de contraseña para la contraseña actual del usuario.
     */
    @FXML
    private PasswordField CurrentPassword;
    /**
     * Campo de contraseña para la nueva contraseña del usuario.
     */
    @FXML
    private PasswordField NewPassword;
    /**
     * Campo de contraseña para confirmar la nueva contraseña.
     */
    @FXML
    private PasswordField ConfirmPassword;
    /**
     * Botón para ejecutar el cambio de contraseña.
     */
    @FXML
    private Button btChange;
    /**
     * Botón para cancelar la operación y cerrar la ventana.
     */
    @FXML
    private Button btCancel;
    
    private static final Logger LOGGER = Logger.getLogger(ChangeController.class.getName());
    
    private Customer loggedCustomer;
    
    /**
     * Establece el cliente que ha iniciado sesión para las operaciones de cambio de contraseña.
     * 
     * @param customer El objeto Customer que representa al usuario logueado
     */
    public void setCustomer(Customer customer) {
        this.loggedCustomer = customer;
        LOGGER.info("Customer recibido: " + (customer != null ? customer.getEmail() : "null"));
    }
    
    /**
     * Inicializa la ventana de cambio de contraseña.
     * Configura la escena, los listeners de los campos y los manejadores de eventos.
     * 
     * @param stage El escenario principal de la aplicación
     * @param root El nodo raíz de la interfaz gráfica
     */
    public void init(Stage stage, Parent root) {
        try {
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Change Password");
            stage.setResizable(false);

            // Listeners de texto
            CurrentPassword.textProperty().addListener(this::handleCurrentPasswordTextChange);
            NewPassword.textProperty().addListener(this::handleNewPasswordTextChange);
            ConfirmPassword.textProperty().addListener(this::handleConfirmPasswordTextChange);

            // Listeners de focus
            CurrentPassword.focusedProperty().addListener(this::handleCurrentPasswordFocusChange);
            NewPassword.focusedProperty().addListener(this::handleNewPasswordFocusChange);
            ConfirmPassword.focusedProperty().addListener(this::handleConfirmPasswordFocusChange);

            // Configurar manejadores de botones manualmente
            btChange.setOnAction(this::handleBtchangeOnMethod);
            btCancel.setOnAction(this::handleBtcancelOnMethod);

            stage.show();
            CurrentPassword.requestFocus();
            
            // Mostrar que se ha iniciado sesión
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Has iniciado sesión correctamente", ButtonType.OK);
            alert.setHeaderText("Sesión Iniciada");
            alert.showAndWait();
        } catch (Exception e) {
            LOGGER.severe("Error al inicializar la ventana: " + e.getMessage());
        }
    }
    
    /**
     * Maneja los cambios en el campo de contraseña actual.
     * Valida que el campo no esté vacío y aplica estilos visuales según el resultado.
     * 
     * @param observable El valor observable que ha cambiado
     * @param oldValue El valor anterior del texto
     * @param newValue El nuevo valor del texto
     */
    private void handleCurrentPasswordTextChange(ObservableValue observable, String oldValue, String newValue) {
        try {
            if (newValue == null || newValue.trim().isEmpty()) {
                throw new Exception("Debe introducir la contraseña actual.");
            }
            CurrentPassword.setStyle(null); //Quitar rojo cuando es válido
            LOGGER.info("Texto en contraseña actual válido");
        } catch (Exception e) {
            CurrentPassword.setStyle("-fx-border-color: red;"); //Poner rojo cuando es inválido
            LOGGER.warning(e.getMessage());
        }
    }
    
    /**
     * Maneja los cambios en el campo de nueva contraseña.
     * Valida que la contraseña tenga al menos 8 caracteres.
     * 
     * @param observable El valor observable que ha cambiado
     * @param oldValue El valor anterior del texto
     * @param newValue El nuevo valor del texto
     */
    private void handleNewPasswordTextChange(ObservableValue observable, String oldValue, String newValue) {
        try {
            if (newValue == null || newValue.trim().length() < 8) {
                throw new Exception("La nueva contraseña debe tener al menos 8 caracteres.");
            }
            NewPassword.setStyle(null); //Quitar rojo cuando es válido
            LOGGER.info("Nueva contraseña válida");
        } catch (Exception e) {
            NewPassword.setStyle("-fx-border-color: red;"); //Poner rojo cuando es inválido
            LOGGER.warning(e.getMessage());
        }
    }
    
    /**
     * Maneja los cambios en el campo de confirmación de contraseña.
     * Valida que coincida con la nueva contraseña y tenga al menos 8 caracteres.
     * 
     * @param observable El valor observable que ha cambiado
     * @param oldValue El valor anterior del texto
     * @param newValue El nuevo valor del texto
     */
    private void handleConfirmPasswordTextChange(ObservableValue observable, String oldValue, String newValue) {
        try {
            String newPass = NewPassword.getText();
            if (newValue == null || newValue.trim().length() < 8) {
                throw new Exception("La confirmación debe tener al menos 8 caracteres.");
            }
            if (!newValue.equals(newPass)) {
                throw new Exception("Las contraseñas no coinciden.");
            }
            ConfirmPassword.setStyle(null); //Quitar rojo cuando es válido
            LOGGER.info("Confirmación de contraseña válida");
        } catch (Exception e) {
            ConfirmPassword.setStyle("-fx-border-color: red;"); //Poner rojo cuando es inválido
            LOGGER.warning(e.getMessage());
        }
    }
    
    /**
     * Maneja el evento de clic en el botón Cambiar.
     * Ejecuta todas las validaciones y realiza el cambio de contraseña si son exitosas.
     * 
     * @param event El evento de acción del botón
     */
    private void handleBtchangeOnMethod(ActionEvent event) {
        try {
            String current = CurrentPassword.getText();
            String newPass = NewPassword.getText();
            String confirm = ConfirmPassword.getText();

            //Resetear todos los estilos primero
            CurrentPassword.setStyle(null);
            NewPassword.setStyle(null);
            ConfirmPassword.setStyle(null);

            //Validar campos vacíos
            if (current == null || current.trim().isEmpty()) {
                CurrentPassword.setStyle("-fx-border-color: red;");
                throw new Exception("La contraseña actual es obligatoria.");
            }

            if (newPass == null || newPass.trim().isEmpty()) {
                NewPassword.setStyle("-fx-border-color: red;");
                throw new Exception("La nueva contraseña es obligatoria.");
            }

            if (confirm == null || confirm.trim().isEmpty()) {
                ConfirmPassword.setStyle("-fx-border-color: red;");
                throw new Exception("La confirmación de contraseña es obligatoria.");
            }

            //Validar contraseña actual
            if (!current.equals(loggedCustomer.getPassword())) {
                CurrentPassword.setStyle("-fx-border-color: red;");
                throw new Exception("La contraseña actual no es correcta.");
            }

            //Validar que la nueva contraseña no sea igual a la actual
            if (newPass.equals(loggedCustomer.getPassword())) {
                NewPassword.setStyle("-fx-border-color: red;");
                throw new Exception("La nueva contraseña no puede ser igual a la contraseña actual.");
            }

            //Validar longitud mínima
            if (newPass.length() < 8) {
                NewPassword.setStyle("-fx-border-color: red;");
                throw new Exception("La nueva contraseña debe tener al menos 8 caracteres.");
            }

            //Validar que las contraseñas coincidan
            if (!confirm.equals(newPass)) {
                ConfirmPassword.setStyle("-fx-border-color: red;");
                throw new Exception("Las contraseñas no coinciden.");
            }

            //Si todo está correcto, proceder con el cambio
            CustomerRESTClient client = new CustomerRESTClient();
            loggedCustomer.setPassword(newPass);
            client.edit_XML(loggedCustomer);

            showAlert(Alert.AlertType.INFORMATION, "Cambio exitoso", 
                     "La contraseña se ha cambiado correctamente para: " + loggedCustomer.getEmail());

            client.close();
            clearForm();
            

        } catch (InternalServerErrorException e) {
            LOGGER.severe("Error del servidor: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error del Servidor", 
              "Ha ocurrido un error interno en el servidor. Por favor, intente más tarde.");
        } catch (Exception e) {
            LOGGER.warning("Error en cambio de contraseña: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            
        }
        
    }
    
    /**
     * Maneja el evento de clic en el botón Cancelar.
     * Cierra la ventana actual sin realizar cambios.
     * 
     * @param event El evento de acción del botón
     */
private void handleBtcancelOnMethod(ActionEvent event) {
    try {
        // Obtener el stage actual
        Stage currentStage = (Stage) btCancel.getScene().getWindow();

        // Cargar la ventana de Sign In
        FXMLLoader loader = new FXMLLoader(getClass().getResource("signin.fxml"));
        Parent root = loader.load();

        // Obtener su controlador e iniciar
        SignInController controller = loader.getController();
        controller.init(currentStage, root);

    } catch (Exception e) {
        LOGGER.severe("Error al volver a Sign In: " + e.getMessage());
        new Alert(Alert.AlertType.ERROR, "Error al volver a Sign In").showAndWait();
    }
}

    
    /**
     * Maneja los cambios de focus en el campo de contraseña actual.
     * 
     * @param observable La propiedad observable del focus
     * @param oldValue El valor anterior del focus
     * @param newValue El nuevo valor del focus
     */
    private void handleCurrentPasswordFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            LOGGER.info("onFocus");
        } else if(oldValue) {
            LOGGER.info("onBlur");
        }
    }
    
    /**
     * Maneja los cambios de focus en el campo de nueva contraseña.
     * 
     * @param observable La propiedad observable del focus
     * @param oldValue El valor anterior del focus
     * @param newValue El nuevo valor del focus
     */
    private void handleNewPasswordFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            LOGGER.info("onFocus");
        } else if(oldValue) {
            LOGGER.info("onBlur");
        }
    }
    
    /**
     * Maneja los cambios de focus en el campo de confirmación de contraseña.
     * 
     * @param observable La propiedad observable del focus
     * @param oldValue El valor anterior del focus
     * @param newValue El nuevo valor del focus
     */
    private void handleConfirmPasswordFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            LOGGER.info("onFocus");
        } else if(oldValue) {
            LOGGER.info("onBlur");
        }
    }
    
    /**
     * Muestra una alerta al usuario con el tipo, encabezado y contenido especificados.
     * 
     * @param type El tipo de alerta (INFORMATION, ERROR, WARNING, etc.)
     * @param header El texto del encabezado de la alerta
     * @param content El contenido principal de la alerta
     */
    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
    
    /**
     * Obtiene el cliente actualmente logueado en el sistema.
     * 
     * @return El objeto Customer representando al usuario logueado
     */
    public Customer getLoggedCustomer() {
        return loggedCustomer;
    }
    
    public void clearForm() {
 
    CurrentPassword.textProperty().removeListener(this::handleCurrentPasswordTextChange);
    NewPassword.textProperty().removeListener(this::handleNewPasswordTextChange);
    ConfirmPassword.textProperty().removeListener(this::handleConfirmPasswordTextChange);


    CurrentPassword.clear();
    NewPassword.clear();
    ConfirmPassword.clear();

    CurrentPassword.setStyle("");
    NewPassword.setStyle("");
    ConfirmPassword.setStyle("");


    CurrentPassword.textProperty().addListener(this::handleCurrentPasswordTextChange);
    NewPassword.textProperty().addListener(this::handleNewPasswordTextChange);
    ConfirmPassword.textProperty().addListener(this::handleConfirmPasswordTextChange);
}

}