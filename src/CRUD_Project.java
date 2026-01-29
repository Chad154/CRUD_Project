/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import CRUD_Project.ui.MovementController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author imad
 */
public class CRUD_Project extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        // Carga directa de la vista
        Parent root = FXMLLoader.load(getClass().getResource("/CRUD_Project/ui/account.fxml"));
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}