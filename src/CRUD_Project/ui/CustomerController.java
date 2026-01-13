/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CRUD_Project.ui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Sta ge;

/**
 * FXML Controller class
 *
 * @author chad
 */
public class CustomerController implements Initializable {

    @FXML
    private Label lblCustomerID;
    @FXML
    private TextField tfCustomerID;
    @FXML
    private Button btSearch;
    @FXML
    private Label lblFirstName;
    @FXML
    private TextField tfFirstName;
    @FXML
    private Label lblCity;
    @FXML
    private TextField tfCity;
    @FXML
    private Label lblZIP;
    @FXML
    private TextField tfZIP;
    @FXML
    private Label lblPhone;
    @FXML
    private TextField tfPhone;
    @FXML
    private Label lblLastName;
    @FXML
    private TextField tfLastName;
    @FXML
    private Label lblStreet;
    @FXML
    private TextField tfStreet;
    @FXML
    private Label lblEmail;
    @FXML
    private TextField tfEmail;
    @FXML
    private Button btExit;

    /**
     * Initializes the controller class.
     */
    
    private Stage stage;
    
    public void init(Stage stage) {
        // TODO
    }    



    
}
