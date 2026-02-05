package CRUD_Project.ui;

import CRUD_Project.model.Account;
import CRUD_Project.model.AccountType; 
import CRUD_Project.logic.AccountRESTClient;
import CRUD_Project.logic.MovementRESTClient;
import CRUD_Project.model.Customer;
import CRUD_Project.model.Movement;
import CRUD_Project.ui.MovementController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.ws.rs.core.GenericType;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random; 
import java.util.Set;     
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.stage.Modality;
import java.util.Comparator; 

public class AccountController {

    // --- ELEMENTOS DE LA TABLA ---
    @FXML private TableView<Account> tbAccounts;
    @FXML private TableColumn<Account, Long> tcAccountNumber;
    @FXML private TableColumn<Account, String> tcDescription;
    @FXML private TableColumn<Account, AccountType> tcType;
    @FXML private TableColumn<Account, Double> tcCreditLine;
    @FXML private TableColumn<Account, Double> tcBalance;
    @FXML private TableColumn<Account, Double> tcBeginBalance;
    @FXML private TableColumn<Account, Date> tcOpeningDate;

    // --- ELEMENTOS DEL FORMULARIO ---
    @FXML private TextField tfAccountNumber;
    @FXML private TextField tfDescription;
    @FXML private ComboBox<AccountType> cbType; 
    @FXML private TextField tfCreditLine;
    @FXML private TextField tfBeginBalance; 
    @FXML private DatePicker dpOpeningDate;

    // --- BOTONES Y OTROS ---
    @FXML private Button btCreate;
    @FXML private Button btUpdate;
    @FXML private Button btDelete;
    @FXML private Button btViewMovements;
    @FXML private TextField tfTotalBalance;
    
    @FXML private TextField tfCustomerId; 
    
    @FXML private MenuController hBoxMenuController;

    private Stage stage;
    private AccountRESTClient restClient;
    private ObservableList<Account> accountsData;
    private final Logger LOGGER = Logger.getLogger(AccountController.class.getName());

    private Customer user;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        try {
            restClient = new AccountRESTClient();
        } catch (Exception e) {
            LOGGER.severe("No se pudo conectar con el servidor: " + e.getMessage());
        }

        if (hBoxMenuController != null) {
            hBoxMenuController.setLinkedButton(btViewMovements);
        }

        try {
            cbType.setItems(FXCollections.observableArrayList(AccountType.values()));
        } catch (Exception e) {
            LOGGER.severe("Error types: " + e.getMessage());
        }

        // Listener para controlar el Credit Line
        cbType.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal.toString().equalsIgnoreCase("STANDARD")) {
                    tfCreditLine.setText("0.0");
                    tfCreditLine.setDisable(true); 
                } else {
                    tfCreditLine.setDisable(false); 
                }
            }
        });

        tcAccountNumber.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
        tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance"));
        tcOpeningDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));
        

        tcBalance.setCellValueFactory(cellData -> 
             new SimpleObjectProperty<>(cellData.getValue().getBalance())
        );
        tcBalance.setCellFactory(column -> new TableCell<Account, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f €", item));
                }
            }
        });

        accountsData = FXCollections.observableArrayList();
        tbAccounts.setItems(accountsData);

        tbAccounts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarDatosEnFormulario(newVal);
                habilitarModoEdicion();
            } else {
                limpiarFormulario();
                habilitarModoCreacion();
            }
        });
        
        btCreate.setStyle("-fx-color: #16a9f0;");
        btDelete.setStyle("-fx-color: #d32f2f;");

        btCreate.setOnAction(this::manejarCrearCuenta);
        btUpdate.setOnAction(e -> manejarActualizarCuenta());
        btDelete.setOnAction(e -> manejarEliminarCuenta());
        btViewMovements.setOnAction(e -> manejarVerMovimientos());
    }

    public void initStage(Parent root) {
        try {
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Gestión de Cuentas");
            stage.setResizable(false);
            stage.setOnCloseRequest(this::manejarCierreVentana);
            
            if (this.user != null) {
                cargarDatosDesdeServidor();
            }
            habilitarModoCreacion();
            
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initStage", e);
        }
    }

    public void initData(Stage currentStage, Customer loggedCustomer) {
        this.stage = currentStage;
        this.user = loggedCustomer; 
        
        if (this.user != null) {
            this.stage.setTitle("Cuentas de: " + user.getFirstName() + " " + user.getLastName());
            if (tfCustomerId != null) {
                tfCustomerId.setText(String.valueOf(user.getId()));
            }
            cargarDatosDesdeServidor();
        }
    }

    private Long generarIdUnico() {
        Random random = new Random();
        long min = 1_000_000_000L;
        long max = 9_999_999_999L;
        return min + (long)(random.nextDouble() * (max - min));
    }

    private void cargarDatosEnFormulario(Account account) {
        tfAccountNumber.setText(String.valueOf(account.getId()));
        tfDescription.setText(account.getDescription());
        cbType.getSelectionModel().select(account.getType()); 
        tfCreditLine.setText(String.valueOf(account.getCreditLine()));
        
        tfBeginBalance.setText(account.getBeginBalance() != null ? String.valueOf(account.getBeginBalance()) : "0.0");
        
        if (account.getBeginBalanceTimestamp() != null) {
            dpOpeningDate.setValue(account.getBeginBalanceTimestamp().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            dpOpeningDate.setValue(null);
        }
    }
    
    private void habilitarModoEdicion() {
        tfAccountNumber.setDisable(true);
        cbType.setDisable(true);     
        dpOpeningDate.setDisable(true);
        if (tfBeginBalance != null) tfBeginBalance.setDisable(true); 

        tfDescription.setDisable(false);
        
        AccountType tipo = cbType.getValue();
        if (tipo != null && tipo.toString().equalsIgnoreCase("STANDARD")) {
            tfCreditLine.setDisable(true);
        } else {
            tfCreditLine.setDisable(false);
        }

        btCreate.setDisable(true);
        btUpdate.setDisable(false);
        btDelete.setDisable(false);
    }
    
    private void habilitarModoCreacion() {
        tfAccountNumber.setText(String.valueOf(generarIdUnico()));
        tfAccountNumber.setDisable(true); 
        
        cbType.setDisable(false);
        dpOpeningDate.setDisable(false);
        if (tfBeginBalance != null) tfBeginBalance.setDisable(false);

        tfDescription.setDisable(false);
        
        AccountType tipo = cbType.getValue();
        if (tipo != null && tipo.toString().equalsIgnoreCase("STANDARD")) {
            tfCreditLine.setDisable(true);
        } else {
            tfCreditLine.setDisable(false);
        }

        btCreate.setDisable(false);
        btUpdate.setDisable(true);
        btDelete.setDisable(true);
    }
    
    private void limpiarFormulario() {
        tfDescription.clear();
        cbType.getSelectionModel().clearSelection();
        tfCreditLine.clear();
        if (tfBeginBalance != null) tfBeginBalance.clear();
        dpOpeningDate.setValue(null);
    }

    // --- CRUD ---
    @FXML
    private void manejarCrearCuenta(ActionEvent event) {
         try {
            if (tfDescription.getText().isEmpty() || 
                tfCreditLine.getText().isEmpty() || tfBeginBalance.getText().isEmpty() || 
                cbType.getSelectionModel().getSelectedItem() == null) {
                
                mostrarError("Por favor, rellena todos los campos.");
                return;
            }

            Long idAutomatico = Long.parseLong(tfAccountNumber.getText());

            Account nuevaCuenta = new Account();
            nuevaCuenta.setId(idAutomatico);
            nuevaCuenta.setDescription(tfDescription.getText());
            nuevaCuenta.setType(cbType.getSelectionModel().getSelectedItem());
            
            try {
                nuevaCuenta.setCreditLine(Double.parseDouble(tfCreditLine.getText()));
                Double saldo = Double.parseDouble(tfBeginBalance.getText());
                nuevaCuenta.setBeginBalance(saldo);
                nuevaCuenta.setBalance(saldo);
            } catch (NumberFormatException nfe) {
                mostrarError("El saldo y crédito deben ser valores numéricos.");
                return;
            }
            
            nuevaCuenta.setBeginBalanceTimestamp(new Date());

            if (this.user != null) {
                Set<Customer> customers = new HashSet<>();
                customers.add(this.user);
                nuevaCuenta.setCustomers(customers); 
            } else {
                mostrarError("Error crítico: No hay usuario logueado. Reinicia la sesión.");
                return;
            }

            restClient.createAccount_XML(nuevaCuenta);
            
            mostrarInformacion("Cuenta creada con ID Automático: " + idAutomatico);
            limpiarFormulario();
            habilitarModoCreacion(); 
            cargarDatosDesdeServidor();
            
            // Scroll al final para ver la nueva cuenta
            if (!accountsData.isEmpty()) {
                int ultimoIndex = accountsData.size() - 1;
                tbAccounts.scrollTo(ultimoIndex);
                tbAccounts.getSelectionModel().select(ultimoIndex);
            }
            
        } catch (javax.ws.rs.ClientErrorException e) {
            LOGGER.severe("Error REST: " + e.getMessage());
            mostrarError("Error al guardar. Inténtalo de nuevo.");
            habilitarModoCreacion(); 
            
        } catch (Exception e) { 
            LOGGER.severe("Error general: " + e.getMessage());
            mostrarError("Error inesperado: " + e.getMessage()); 
        }
    }

    private void manejarActualizarCuenta() {
        Account seleccionada = tbAccounts.getSelectionModel().getSelectedItem();
        if(seleccionada == null) return;
        try {
            seleccionada.setDescription(tfDescription.getText());
            if (!tfCreditLine.getText().isEmpty()) {
                seleccionada.setCreditLine(Double.parseDouble(tfCreditLine.getText()));
            }
            restClient.updateAccount_XML(seleccionada);
            mostrarInformacion("Actualizada");
            tbAccounts.refresh(); 
            limpiarFormulario();
            habilitarModoCreacion();
        } catch(Exception e) {
            mostrarError("Error: " + e.getMessage());
        }
    }

    @FXML
    private void manejarEliminarCuenta() {
         Account seleccionada = tbAccounts.getSelectionModel().getSelectedItem();
         if (seleccionada == null) {
             mostrarError("Selecciona una cuenta primero.");
             return;
         }
         
         MovementRESTClient movementClient = null;
         try {
             movementClient = new MovementRESTClient();
             GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};
             List<Movement> movimientos = movementClient.findMovementByAccount_XML(listType, String.valueOf(seleccionada.getId()));
             
             if (movimientos != null && !movimientos.isEmpty()) {
                 mostrarError("No se puede eliminar: Tiene movimientos asociados.");
                 return; 
             }
             
         } catch (Exception e) {
             LOGGER.severe("Error comprobando movimientos: " + e.getMessage());
         } finally {
             // --- CORRECCIÓN BUG NULLPOINTER ---
             try {
                 if (movementClient != null) movementClient.close();
             } catch (Exception ex) {
                 LOGGER.warning("Error cerrando cliente movimientos: " + ex.getMessage());
             }
         }

         Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Borrar la cuenta " + seleccionada.getId() + " permanentemente?", ButtonType.YES, ButtonType.NO);
         confirm.showAndWait();
         
         if (confirm.getResult() == ButtonType.YES) {
             try {
                 restClient.removeAccount(String.valueOf(seleccionada.getId()));
                 mostrarInformacion("Cuenta eliminada.");
                 limpiarFormulario();
                 habilitarModoCreacion(); 
                 cargarDatosDesdeServidor();
             } catch (Exception e) {
                 mostrarError("Error al eliminar: " + e.getMessage());
             }
         }
    }

    @FXML
    private void manejarVerMovimientos() {
        Account seleccion = tbAccounts.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            mostrarError("Selecciona una cuenta primero.");
            return;
        }
        try {
            String ruta = "/CRUD_Project/ui/Movement.fxml"; 
            java.net.URL url = getClass().getResource(ruta);
            if (url == null) url = getClass().getResource("Movement.fxml");

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            MovementController controller = loader.getController();
            Stage modalStage = new Stage();
            
            controller.initData(modalStage, seleccion);

            modalStage.setScene(new Scene(root));
            modalStage.setTitle("Movimientos de: " + seleccion.getId());
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.showAndWait();
            
            cargarDatosDesdeServidor(); 
            tbAccounts.getSelectionModel().clearSelection();
            limpiarFormulario();
            habilitarModoCreacion();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error movimientos: " + e.getMessage());
        }
    }
    
    private void cargarDatosDesdeServidor() {
        if (this.user == null) return; 

        try {
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};
            List<Account> cuentas = restClient.findAccountsByCustomerId_XML(
                    listType, 
                    String.valueOf(this.user.getId())
            );
            
            // Ordenar por Fecha (Antiguas primero -> Nuevas al FINAL)
            cuentas.sort((a1, a2) -> {
                if (a1.getBeginBalanceTimestamp() == null) return -1;
                if (a2.getBeginBalanceTimestamp() == null) return 1;
                return a1.getBeginBalanceTimestamp().compareTo(a2.getBeginBalanceTimestamp());
            });
            
            accountsData.setAll(cuentas);
            calcularBalanceTotal();
        } catch (Exception e) {
            LOGGER.severe("Error de conexión cargando datos: " + e.getMessage());
        }
    }
    
    private void calcularBalanceTotal() {
        double total = accountsData.stream()
                .filter(a -> a.getBalance() != null)
                .mapToDouble(Account::getBalance)
                .sum();
        tfTotalBalance.setText(String.format("%.2f €", total));
    }

    private void manejarCierreVentana(WindowEvent e) { 
        if(restClient != null) restClient.close(); 
    }
    
    private void mostrarError(String m) { 
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait(); 
    }
    
    private void mostrarInformacion(String m) { 
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); 
    }
}