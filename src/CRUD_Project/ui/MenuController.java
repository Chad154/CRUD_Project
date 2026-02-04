package CRUD_Project.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class MenuController {

    private static final Logger LOGGER = Logger.getLogger("MenuController");

    @FXML private MenuBar menuBar;
    @FXML private MenuItem miViewMovements; // Corregido para coincidir con el ID del FXML
    @FXML private MenuItem miLogOut;

    // Referencia al botón físico de la ventana de Accounts
    private Button linkedButton;

    /**
     * Permite al AccountController vincular su botón btViewMovements
     * para que el menú pueda disparar su acción.
     */
    public void setLinkedButton(Button button) {
        this.linkedButton = button;
    }

    /**
     * IMPORTANTE: Mantenemos este método por compatibilidad si otros 
     * controladores lo usan para asignar lógica manual.
     */
    public MenuItem getViewChange() {
        return miViewMovements;
    }

    /**
     * Al pulsar "View Movements" en el menú, disparamos el clic del botón vinculado.
     */
    @FXML
    private void handleViewMovementsMenu(ActionEvent event) {
        if (linkedButton != null) {
            LOGGER.info("Disparando evento del botón vinculado desde el menú.");
            linkedButton.fire();
        } else {
            LOGGER.warning("No hay ningún botón vinculado al menú de movimientos.");
        }
    }

    /**
     * Acción de Cerrar Sesión: Cierra la ventana actual y abre Login.
     */
    @FXML
    private void handleLogOut(ActionEvent event) {
        try {
            // 1. Obtener la ventana actual y cerrarla
            Stage currentStage = (Stage) menuBar.getScene().getWindow();
            currentStage.close();

            // 2. Cargar el FXML de SignIn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CRUD_Project/ui/SignIn.fxml"));
            Parent root = loader.load();
            
            // 3. Inicializar el controlador de SignIn
            SignInController controller = loader.getController();
            
            Stage stage = new Stage();
            
            // Llamamos al método init para activar los eventos de la ventana de login
            controller.init(stage); 
            
            // 4. Configurar y mostrar la escena
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.setResizable(false);
            stage.show();
            controller.init(stage);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cerrar sesión", e);
        }
    }

    @FXML
    private void handleHelpAction(ActionEvent event) {
        new Alert(Alert.AlertType.INFORMATION, "Bank App v1.0").showAndWait();
    }
}
