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
import java.util.HashSet; // Import necesario para la relación
import java.util.List;
import java.util.Set;     // Import necesario para la relación
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.stage.Modality;

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
    
    @FXML private MenuController hBoxMenuController;

    private Stage stage;
    private AccountRESTClient restClient;
    private ObservableList<Account> accountsData;
    private final Logger LOGGER = Logger.getLogger(AccountController.class.getName());

    // --- NUEVA VARIABLE: Para guardar el usuario logueado ---
    private Customer user;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        // 1. Cliente REST
        try {
            restClient = new AccountRESTClient();
        } catch (Exception e) {
            LOGGER.severe("No se pudo conectar con el servidor: " + e.getMessage());
        }

        // 2. Menú
        if (hBoxMenuController != null && hBoxMenuController.getViewChange() != null) {
            hBoxMenuController.getViewChange().setOnAction(e -> manejarVerMovimientos());
        }

        // 3. ComboBox
        try {
            cbType.setItems(FXCollections.observableArrayList(AccountType.values()));
        } catch (Exception e) {
            LOGGER.severe("Error types: " + e.getMessage());
        }

        // 4. Configurar Columnas
        tcAccountNumber.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
        tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance"));
        tcOpeningDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));

        // --- COLUMNA BALANCE (Robusta) ---
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

        // 5. Lista y Listener
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

        // 6. Botones
        btCreate.setOnAction(this::manejarCrearCuenta);
        btUpdate.setOnAction(e -> manejarActualizarCuenta());
        btDelete.setOnAction(e -> manejarEliminarCuenta());
        btViewMovements.setOnAction(e -> manejarVerMovimientos());
        
        // NOTA: He quitado cargarDatosDesdeServidor() de aquí.
        // Se llamará en initData cuando ya tengamos el usuario.
    }

    // Método para inicializar el Stage (si se llama manualmente desde Main)
    public void initStage(Parent root) {
        try {
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Gestión de Cuentas");
            stage.setResizable(false);
            stage.setOnCloseRequest(this::manejarCierreVentana);
            
            // Solo carga si tenemos usuario (por seguridad)
            if (this.user != null) {
                cargarDatosDesdeServidor();
            }
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initStage", e);
        }
    }

    // --- NUEVO MÉTODO IMPLEMENTADO ---
    // Este método es llamado por SignInController
    public void initData(Stage currentStage, Customer loggedCustomer) {
        this.stage = currentStage;
        this.user = loggedCustomer; // Guardamos el usuario
        
        // Configuraciones visuales extras
        if (this.user != null) {
            this.stage.setTitle("Cuentas de: " + user.getFirstName() + " " + user.getLastName());
            // IMPORTANTE: Ahora que tenemos usuario, cargamos SUS datos
            cargarDatosDesdeServidor();
        }
    }

    // --- MÉTODOS DE FORMULARIO ---
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
        tfCreditLine.setDisable(false);
        btCreate.setDisable(true);
        btUpdate.setDisable(false);
        btDelete.setDisable(false);
    }
    
    private void habilitarModoCreacion() {
        tfAccountNumber.setDisable(false);
        cbType.setDisable(false);
        dpOpeningDate.setDisable(false);
        if (tfBeginBalance != null) tfBeginBalance.setDisable(false);

        tfDescription.setDisable(false);
        tfCreditLine.setDisable(false);
        btCreate.setDisable(false);
        btUpdate.setDisable(true);
        btDelete.setDisable(true);
    }
    
    private void limpiarFormulario() {
        tfAccountNumber.clear();
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
            Account nuevaCuenta = new Account();
            if (!tfAccountNumber.getText().isEmpty()) 
                nuevaCuenta.setId(Long.parseLong(tfAccountNumber.getText()));
            // Sugerencia: Si está vacío, podrías generar ID automático aquí:
            // else nuevaCuenta.setId(System.currentTimeMillis());

            nuevaCuenta.setDescription(tfDescription.getText());
            nuevaCuenta.setType(cbType.getSelectionModel().getSelectedItem());
            nuevaCuenta.setCreditLine(!tfCreditLine.getText().isEmpty() ? Double.parseDouble(tfCreditLine.getText()) : 0.0);
            
            Double saldo = (tfBeginBalance != null && !tfBeginBalance.getText().isEmpty()) ? Double.parseDouble(tfBeginBalance.getText()) : 0.0;
            nuevaCuenta.setBeginBalance(saldo);
            nuevaCuenta.setBalance(saldo); 
            nuevaCuenta.setBeginBalanceTimestamp(new Date());

            // --- CAMBIO CLAVE: Relacionar con el Usuario Logueado ---
            if (this.user != null) {
                Set<Customer> customers = new HashSet<>();
                customers.add(this.user);
                nuevaCuenta.setCustomers(customers); // Asignamos el dueño
            } else {
                mostrarError("Error: No hay usuario logueado. Reinicia la sesión.");
                return;
            }

            restClient.createAccount_XML(nuevaCuenta);
            mostrarInformacion("Cuenta Creada");
            limpiarFormulario();
            cargarDatosDesdeServidor();
        } catch (Exception e) { 
            e.printStackTrace();
            mostrarError("Error al crear: " + e.getMessage()); 
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
            // NO TOCAMOS EL BALANCE AQUI
            restClient.updateAccount_XML(seleccionada);
            mostrarInformacion("Actualizada");
            tbAccounts.refresh(); 
            limpiarFormulario();
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
                 mostrarError("No se puede eliminar la cuenta " + seleccionada.getId() + ".\n\n" +
                              "MOTIVO: Tiene " + movimientos.size() + " movimientos asociados.\n" +
                              "Debes eliminar los movimientos primero.");
                 return; 
             }
             
         } catch (Exception e) {
             LOGGER.severe("Error comprobando movimientos: " + e.getMessage());
             mostrarError("Error de conexión al comprobar la cuenta.");
             return;
         } finally {
             if (movementClient != null) movementClient.close();
         }

         Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Borrar la cuenta " + seleccionada.getId() + " permanentemente?", ButtonType.YES, ButtonType.NO);
         confirm.showAndWait();
         
         if (confirm.getResult() == ButtonType.YES) {
             try {
                 restClient.removeAccount(String.valueOf(seleccionada.getId()));
                 mostrarInformacion("Cuenta eliminada correctamente.");
                 limpiarFormulario();
                 cargarDatosDesdeServidor();
             } catch (Exception e) {
                 mostrarError("Error inesperado al eliminar: " + e.getMessage());
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

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error movimientos: " + e.getMessage());
        }
    }
    
    // --- CAMBIO CLAVE: Cargar solo datos del usuario ---
    private void cargarDatosDesdeServidor() {
        if (this.user == null) return; // Seguridad

        try {
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};
            
            // Usamos findAccountsByCustomerId en vez de findAll
            List<Account> cuentas = restClient.findAccountsByCustomerId_XML(
                    listType, 
                    String.valueOf(this.user.getId())
            );
            
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