package CRUD_Project.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class MenuController {
    
    private static final Logger LOGGER = Logger.getLogger("MenuController");

    @FXML private MenuBar menuBar;
    @FXML private MenuItem ViewChange; // Referencia al botón de movimientos
    @FXML private MenuItem miLogOut;

    /**
     * IMPORTANTE: Este método permite que AccountController acceda al botón
     * para asignarle la lógica de abrir la ventana de movimientos.
     */
    public MenuItem getViewChange() {
        return ViewChange;
    }

    /**
     * Acción de Cerrar Sesión: Cierra la ventana actual y abre Login.
     */
    @FXML
    private void handleLogOut(ActionEvent event) {
        try {
            // 1. Obtener la ventana actual y cerrarla
            // Usamos el menuBar para encontrar en qué ventana estamos
            Stage currentStage = (Stage) menuBar.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CRUD_Project/ui/SignIn.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cerrar sesión", e);
        }
    }

    @FXML
    private void handleHelpAction(ActionEvent event) {
        new Alert(Alert.AlertType.INFORMATION, "Bank App v1.0").showAndWait();
    }
}