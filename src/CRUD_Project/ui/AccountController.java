package CRUD_Project.ui;

import CRUD_Project.model.Account;
import CRUD_Project.model.AccountType; 
import CRUD_Project.logic.AccountRESTClient;
import CRUD_Project.logic.MovementRESTClient;
import CRUD_Project.model.Customer;
import CRUD_Project.model.Movement;

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

/**
 * Controlador para la vista de Gestión de Cuentas (Account).
 * <p>
 * Esta clase maneja la lógica de la interfaz gráfica (JavaFX) para realizar
 * las operaciones CRUD (Crear, Leer, Actualizar, Borrar) sobre las cuentas bancarias.
 * Se comunica con el servidor mediante un cliente REST (AccountRESTClient).
 * </p>
 * * @author Daniel López López
  * @todo @fixme Hacer que la siguiente clase implemente las interfaces 
 * Initializable y MenuActionsHandler para que al pulsar en las acciones CRUD del 
 * menú Actions se ejecuten los métodos manejadores correspondientes a la vista 
 * que incluye el menú.
 * El método initialize debe llamar a setMenuActionsHandler() para establecer que este
 * controlador es el manejador de acciones del menú.
 
 */
public class AccountController {

    /**
     * TODO: NO TOCAR La siguiente referencia debe llamarse así y tener este tipo.
     * JavaFX asigna automáticamente el campo menuIncludeController cuando usas fx:id="menuInclude".
     */
    @FXML
    private MenuController menuIncludeController;
    
    
    // ELEMENTOS DE LA TABLA (Mapeo con FXML)
    @FXML private TableView<Account> tbAccounts;
    @FXML private TableColumn<Account, Long> tcAccountNumber;
    @FXML private TableColumn<Account, String> tcDescription;
    @FXML private TableColumn<Account, AccountType> tcType;
    @FXML private TableColumn<Account, Double> tcCreditLine;
    @FXML private TableColumn<Account, Double> tcBalance;
    @FXML private TableColumn<Account, Double> tcBeginBalance;
    @FXML private TableColumn<Account, Date> tcOpeningDate;

    // ELEMENTOS DEL FORMULARIO DE EDICIÓN/CREACIÓN
    @FXML private TextField tfAccountNumber;
    @FXML private TextField tfDescription;
    @FXML private ComboBox<AccountType> cbType; 
    @FXML private TextField tfCreditLine;
    @FXML private TextField tfBeginBalance; 
    @FXML private DatePicker dpOpeningDate;

    // BOTONES DE ACCIÓN
    @FXML private Button btCreate;
    @FXML private Button btUpdate;
    @FXML private Button btDelete;
    @FXML private Button btViewMovements;
    
    // CAMPOS INFORMATIVOS
    @FXML private TextField tfTotalBalance;
    @FXML private TextField tfCustomerId; 
    
    // INYECCIÓN DEL CONTROLADOR DEL MENÚ
    @FXML private MenuController hBoxMenuController;

    // VARIABLES DE LÓGICA
    private Stage stage;
    private AccountRESTClient restClient; // Cliente para peticiones HTTP
    private ObservableList<Account> accountsData; // Modelo de datos para la tabla
    private final Logger LOGGER = Logger.getLogger(AccountController.class.getName());

    private Customer user; // Usuario logueado actualmente
    
    /**
     * Establece el Stage (ventana) principal para este controlador.
     * @param stage La ventana de la aplicación.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Método de inicialización de JavaFX. Se ejecuta automáticamente
     * tras cargar el archivo FXML.
     */
    @FXML
    public void initialize() {
        // 1. Inicializar cliente REST
        try {
            restClient = new AccountRESTClient();
        } catch (Exception e) {
            LOGGER.severe("Critical error: Could not connect to the REST server. " + e.getMessage());
        }

        // 2. Vincular botones con el menú (si existe) para gestión de atajos/estados
        if (hBoxMenuController != null) {
            hBoxMenuController.setLinkedButton(btViewMovements);
        }

        // 3. Cargar tipos de cuenta en el ComboBox
        try {
            cbType.setItems(FXCollections.observableArrayList(AccountType.values()));
        } catch (Exception e) {
            LOGGER.severe("Error loading account types: " + e.getMessage());
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

        // 5. Configurar las factorías de celdas (CellFactory) para la tabla
        tcAccountNumber.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
        tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance"));
        tcOpeningDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));
        
        // Formateo especial para columnas de moneda
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

        // 6. Vincular la lista observable con la tabla
        accountsData = FXCollections.observableArrayList();
        tbAccounts.setItems(accountsData);

        // 7. Listener de selección en la tabla:
        // Si selecciono una fila -> Cargo datos en formulario y habilito Edición.
        // Si deselecciono -> Limpio formulario y habilito Creación.
        tbAccounts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarDatosEnFormulario(newVal);
                habilitarModoEdicion();
            } else {
                limpiarFormulario();
                habilitarModoCreacion();
            }
        });
        
        // Estilos CSS rápidos para botones
        btCreate.setStyle("-fx-color: #16a9f0;");
        btDelete.setStyle("-fx-color: #d32f2f;");

        // Asignación de manejadores de eventos
        btCreate.setOnAction(this::manejarCrearCuenta);
        btUpdate.setOnAction(e -> manejarActualizarCuenta());
        btDelete.setOnAction(e -> manejarEliminarCuenta());
        btViewMovements.setOnAction(e -> manejarVerMovimientos());
    }

    /**
     * Configura e inicializa el escenario (Stage) de la ventana.
     * @param root El nodo raíz cargado desde el FXML.
     */
    public void initStage(Parent root) {
        try {
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Account Management");
            stage.setResizable(false);
            // Asegurar cierre limpio de conexiones al salir
            stage.setOnCloseRequest(this::manejarCierreVentana);
            
            if (this.user != null) {
                cargarDatosDesdeServidor();
            }
            habilitarModoCreacion();
            
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in initStage", e);
        }
    }

    /**
     * Recibe los datos del usuario logueado desde la ventana anterior (SignIn).
     * @param currentStage El stage actual.
     * @param loggedCustomer El objeto Customer que ha iniciado sesión.
     */
    public void initData(Stage currentStage, Customer loggedCustomer) {
        this.stage = currentStage;
        this.user = loggedCustomer; 
        
        if (this.user != null) {
            this.stage.setTitle("Accounts of: " + user.getFirstName() + " " + user.getLastName());
            if (tfCustomerId != null) {
                tfCustomerId.setText(String.valueOf(user.getId()));
            }
            cargarDatosDesdeServidor();
        }
    }

    /**
     * Genera un ID aleatorio simulado para la cuenta.
     * @return Long con el ID generado.
     */
    private Long generarIdUnico() {
        Random random = new Random();
        long min = 1_000_000_000L;
        long max = 9_999_999_999L;
        return min + (long)(random.nextDouble() * (max - min));
    }

    /**
     * Vuelca los datos de un objeto Account en los campos del formulario.
     */
    private void cargarDatosEnFormulario(Account account) {
        tfAccountNumber.setText(String.valueOf(account.getId()));
        tfDescription.setText(account.getDescription());
        cbType.getSelectionModel().select(account.getType()); 
        tfCreditLine.setText(String.valueOf(account.getCreditLine()));
        
        tfBeginBalance.setText(account.getBeginBalance() != null ? String.valueOf(account.getBeginBalance()) : "0.0");
        
        // Conversión de Date a LocalDate para el DatePicker
        if (account.getBeginBalanceTimestamp() != null) {
            dpOpeningDate.setValue(account.getBeginBalanceTimestamp().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            dpOpeningDate.setValue(null);
        }
    }
    
    /**
     * Ajusta la interfaz para el modo de EDICIÓN (actualizar/borrar).
     * Bloquea campos que no se pueden cambiar (como ID o fecha creación).
     */
    private void habilitarModoEdicion() {
        tfAccountNumber.setDisable(true);
        cbType.setDisable(true);     
        dpOpeningDate.setDisable(true);
        if (tfBeginBalance != null) tfBeginBalance.setDisable(true); 

        tfDescription.setDisable(false);
        
        // Mantener lógica de negocio: Si es STANDARD, crédito deshabilitado
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
    
    /**
     * Ajusta la interfaz para el modo de CREACIÓN.
     * Limpia selección y habilita todos los campos necesarios.
     */
    private void habilitarModoCreacion() {
        tfAccountNumber.setText(String.valueOf(generarIdUnico()));
        tfAccountNumber.setDisable(true); // El ID es automático
        
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

    // LÓGICA CRUD (Create, Read, Update, Delete)

    /**
     * Maneja la creación de una nueva cuenta.
     * Valida datos, crea el objeto y llama al servicio REST.
     */
    @FXML
    private void manejarCrearCuenta(ActionEvent event) {
         try {
            // 1. Validación básica de campos vacíos
            if (tfDescription.getText().isEmpty() || 
                tfCreditLine.getText().isEmpty() || tfBeginBalance.getText().isEmpty() || 
                cbType.getSelectionModel().getSelectedItem() == null) {
                
                mostrarError("Please fill in all required fields.");
                return;
            }

            // 2. Creación del objeto Account
            Long idAutomatico = Long.parseLong(tfAccountNumber.getText());
            Account nuevaCuenta = new Account();
            nuevaCuenta.setId(idAutomatico);
            nuevaCuenta.setDescription(tfDescription.getText());
            nuevaCuenta.setType(cbType.getSelectionModel().getSelectedItem());
            
            // 3. Validación de formatos numéricos
            try {
                nuevaCuenta.setCreditLine(Double.parseDouble(tfCreditLine.getText()));
                Double saldo = Double.parseDouble(tfBeginBalance.getText());

                // --- NUEVO: Validación de negativos (Añadido para pasar el Test) ---
                if (saldo < 0 || nuevaCuenta.getCreditLine() < 0) {
                    mostrarError("The balance and credit line cannot be negative.");
                    return;
                }
                // ------------------------------------------------------------------

                nuevaCuenta.setBeginBalance(saldo);
                nuevaCuenta.setBalance(saldo); // Saldo inicial = Saldo actual
            } catch (NumberFormatException nfe) {
                mostrarError("The balance and credit must be valid numeric values.");
                return;
            }
            
            nuevaCuenta.setBeginBalanceTimestamp(new Date());

            // 4. Asignación al cliente actual
            if (this.user != null) {
                Set<Customer> customers = new HashSet<>();
                customers.add(this.user);
                nuevaCuenta.setCustomers(customers); 
            } else {
                mostrarError("Critical error: Invalid session. Please restart the application.");
                return;
            }

            // 5. Llamada al servicio REST (POST)
            restClient.createAccount_XML(nuevaCuenta);
            
            // 6. Feedback y actualización de UI
            mostrarInformacion("Successfully created account. ID: " + idAutomatico);
            limpiarFormulario();
            habilitarModoCreacion(); 
            cargarDatosDesdeServidor();
            
            // UX: Hacer Scroll al final para que el usuario vea la cuenta creada
            if (!accountsData.isEmpty()) {
                int ultimoIndex = accountsData.size() - 1;
                tbAccounts.scrollTo(ultimoIndex);
                tbAccounts.getSelectionModel().select(ultimoIndex);
            }
            
        } catch (javax.ws.rs.ClientErrorException e) {
            LOGGER.severe("Error in REST request: " + e.getMessage());
            mostrarError("The account could not be created. Server error.");
            habilitarModoCreacion(); 
            
        } catch (Exception e) { 
            LOGGER.severe("Unhandled exception: " + e.getMessage());
            mostrarError("An unexpected error occurred: " + e.getMessage()); 
        }
    }

    /**
     * Actualiza la cuenta seleccionada con los datos del formulario.
     */
    private void manejarActualizarCuenta() {
        Account seleccionada = tbAccounts.getSelectionModel().getSelectedItem();
        if(seleccionada == null) return;
        
        try {
            seleccionada.setDescription(tfDescription.getText());
            if (!tfCreditLine.getText().isEmpty()) {
                seleccionada.setCreditLine(Double.parseDouble(tfCreditLine.getText()));
            }
            // Llamada REST (PUT)
            restClient.updateAccount_XML(seleccionada);
            
            mostrarInformacion("Successfully updated account.");
            tbAccounts.refresh(); // Refrescar vista visual
            limpiarFormulario();
            habilitarModoCreacion();
        } catch(Exception e) {
            mostrarError("Error updating: " + e.getMessage());
        }
    }

    /**
     * Elimina una cuenta, previa comprobación de que no tenga movimientos.
     */
    @FXML
    private void manejarEliminarCuenta() {
         Account seleccionada = tbAccounts.getSelectionModel().getSelectedItem();
         if (seleccionada == null) {
             mostrarError("Select an account first.");
             return;
         }
         
         // 1. Comprobación de integridad: ¿Tiene movimientos?
         MovementRESTClient movementClient = null;
         try {
             movementClient = new MovementRESTClient();
             GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};
             List<Movement> movimientos = movementClient.findMovementByAccount_XML(listType, String.valueOf(seleccionada.getId()));
             
             if (movimientos != null && !movimientos.isEmpty()) {
                 mostrarError("Cannot be deleted: The account has associated transactions.");
                 return; // Cancelamos borrado
             }
             
         } catch (Exception e) {
             LOGGER.severe("Error verifying movements: " + e.getMessage());
         } finally {
             // IMPORTANTE: Cerrar cliente en bloque finally para evitar fugas de memoria
             // y protegerlo con try-catch para que no rompa el flujo (evita NullPointer).
             try {
                 if (movementClient != null) movementClient.close();
             } catch (Exception ex) {
                 LOGGER.warning("Error closing client transactions: " + ex.getMessage());
             }
         }

         // 2. Confirmación del usuario
         Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete the account " + seleccionada.getId() + "?", ButtonType.YES, ButtonType.NO);
         confirm.showAndWait();
         
         // 3. Borrado (DELETE)
         if (confirm.getResult() == ButtonType.YES) {
             try {
                 restClient.removeAccount(String.valueOf(seleccionada.getId()));
                 mostrarInformacion("Account permanently deleted.");
                 limpiarFormulario();
                 habilitarModoCreacion(); 
                 cargarDatosDesdeServidor(); // Recargar tabla
             } catch (Exception e) {
                 mostrarError("Error deleting on server: " + e.getMessage());
             }
         }
    }

    /**
     * Abre la ventana modal para ver los movimientos de la cuenta seleccionada.
     */
    @FXML
    private void manejarVerMovimientos() {
        Account seleccion = tbAccounts.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            mostrarError("You must select an account to view its transactions.");
            return;
        }
        try {
            // Carga dinámica del FXML de movimientos
            String ruta = "/CRUD_Project/ui/Movement.fxml"; 
            java.net.URL url = getClass().getResource(ruta);
            if (url == null) url = getClass().getResource("Movement.fxml");

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // Pasar datos al controlador de movimientos
            MovementController controller = loader.getController();
            Stage modalStage = new Stage();
            controller.initData(modalStage, seleccion);

            // Configuración ventana modal
            modalStage.setScene(new Scene(root));
            modalStage.setTitle("Account transactions: " + seleccion.getId());
            modalStage.initModality(Modality.APPLICATION_MODAL); // Bloquea ventana padre
            modalStage.showAndWait();
            
            // Al volver, refrescamos datos por si hubo cambios de saldo
            cargarDatosDesdeServidor(); 
            tbAccounts.getSelectionModel().clearSelection();
            limpiarFormulario();
            habilitarModoCreacion();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error opening movement window: " + e.getMessage());
        }
    }
    
    /**
     * Carga la lista de cuentas del usuario desde el servidor REST.
     * Incluye lógica de ordenación por fecha.
     */
    private void cargarDatosDesdeServidor() {
        if (this.user == null) return; 

        try {
            // Uso de GenericType para listas en JAX-RS
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};
            List<Account> cuentas = restClient.findAccountsByCustomerId_XML(
                    listType, 
                    String.valueOf(this.user.getId())
            );
            
            // ORDENACIÓN: Las cuentas más nuevas (por fecha) se ponen al final.
            // Esto facilita la visualización al crear una nueva.
            cuentas.sort((a1, a2) -> {
                if (a1.getBeginBalanceTimestamp() == null) return -1;
                if (a2.getBeginBalanceTimestamp() == null) return 1;
                return a1.getBeginBalanceTimestamp().compareTo(a2.getBeginBalanceTimestamp());
            });
            
            accountsData.setAll(cuentas);
            calcularBalanceTotal();
        } catch (Exception e) {
            LOGGER.severe("Connection error loading data: " + e.getMessage());
        }
    }
    
    /**
     * Calcula y muestra el saldo total de todas las cuentas.
     */
    private void calcularBalanceTotal() {
        double total = accountsData.stream()
                .filter(a -> a.getBalance() != null)
                .mapToDouble(Account::getBalance)
                .sum();
        tfTotalBalance.setText(String.format("%.2f €", total));
    }

    /**
     * Cierra el cliente REST al cerrar la ventana para liberar recursos.
     */
    private void manejarCierreVentana(WindowEvent e) { 
        if(restClient != null) restClient.close(); 
    }
    
    // Métodos auxiliares para mostrar alertas
    private void mostrarError(String m) { 
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait(); 
    }
    
    private void mostrarInformacion(String m) { 
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); 
    }
}