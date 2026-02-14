package CRUD_Project.ui;

import CRUD_Project.logic.AccountRESTClient;
import CRUD_Project.logic.MovementRESTClient;
import CRUD_Project.model.Account;
import CRUD_Project.model.Movement;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javax.ws.rs.core.GenericType;

/**
 * @todo @fixme Hacer que la siguiente clase implemente las interfaces 
 * Initializable y MenuActionsHandler para que al pulsar en las acciones CRUD del 
 * menú Actions se ejecuten los métodos manejadores correspondientes a la vista 
 * que incluye el menú.
 * El método initialize debe llamar a setMenuActionsHandler() para establecer que este
 * controlador es el manejador de acciones del menú.
 */
public class MovementController {

    /**
     * TODO: NO TOCAR La siguiente referencia debe llamarse así y tener este tipo.
     * JavaFX asigna automáticamente el campo menuIncludeController cuando usas fx:id="menuInclude".
     */
    @FXML
    private MenuController menuIncludeController;
    
    private static final Logger LOGGER = Logger.getLogger("MovementController");
    //Deposito maximo para evitar errores con cifras muy grandes
    private static final double MAX_AMOUNT_LIMIT = 900_000_000.0; 

    @FXML private TableView<Movement> tvMovements;
    @FXML private TableColumn<Movement, Date> colDate;
    @FXML private TableColumn<Movement, String> colDescription;
    @FXML private TableColumn<Movement, Double> colAmount;
    @FXML private TableColumn<Movement, Double> colBalance;

    @FXML private TextField tfAccountId;
    @FXML private TextField tfAmount;
    @FXML private ChoiceBox<String> cbType;
    
    @FXML private Button bUndoLastMovement;
    @FXML private Button bGoBack;
    @FXML private Button bCreateMovement;

    private Stage stage;
    private Account account; 

    @FXML
    public void initialize() {
        // COLUMNAS
        colDate.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTimestamp()));
        colDescription.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colAmount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAmount()));
        colBalance.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getBalance()));

        // Formatos
        colDate.setCellFactory(column -> new TableCell<Movement, Date>() {
            private final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : format.format(item));
            }
        });
        
        colBalance.setCellFactory(column -> new TableCell<Movement, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%.2f €", item));
            }
        });

        
        tfAccountId.setEditable(false);
        cbType.setItems(FXCollections.observableArrayList("DEPOSIT", "PAYMENT"));
        cbType.getSelectionModel().selectFirst();
        //Estilo y funciones de enter y esc
        bCreateMovement.setStyle("-fx-color: #16a9f0;"); 
        bCreateMovement.setDefaultButton(true);           
        bGoBack.setCancelButton(true);                    

        bCreateMovement.setOnAction(this::handleCreateMovement);
        bUndoLastMovement.setOnAction(this::handleUndoLastMovement);
        bGoBack.setOnAction(e -> handleGoBack());
        
        bUndoLastMovement.setDisable(true); 
    }


    public void initData(Stage stage, Account account) {
        this.stage = stage;
        this.account = account;
        tfAccountId.setText(String.valueOf(account.getId()));
        
        // Carga inicial con datos actualizados
        actualizarSaldoDesdeServidor();
        loadMovements();

        javafx.application.Platform.runLater(() -> {
            tfAmount.requestFocus();
            tfAmount.deselect();
        });
    }

    // Para evitar datos obsoletos, con un el objeto account que se pasa
    private void actualizarSaldoDesdeServidor() {
        AccountRESTClient client = null;
        try {
            client = new AccountRESTClient();
            Account cuentaFresca = client.find_XML(Account.class, String.valueOf(this.account.getId()));
            if (cuentaFresca != null) {
                //Obtenemos el balance
                this.account.setBalance(cuentaFresca.getBalance());
            }
        } catch (Exception e) {
            LOGGER.severe("Error synchronizing balance: " + e.getMessage());
        } finally {
            if (client != null) client.close();
        }
    }

    @FXML
    private void handleCreateMovement(ActionEvent event) {
        MovementRESTClient movementClient = null;
        AccountRESTClient accountClient = null;

        try {
            // validaciones
            if (tfAmount.getText().isEmpty()) return;
            double amountInput;
            try {
                amountInput = Double.parseDouble(tfAmount.getText());
            } catch (NumberFormatException e) {
                mostrarError("Introduce un número válido");
                return;
            }
            if (amountInput <= 0) {
                mostrarError("La cantidad debe ser positiva");
                return;
            }
            if (amountInput > MAX_AMOUNT_LIMIT) {
                mostrarError("Límite superado");
                return;
            }

            movementClient = new MovementRESTClient();
            accountClient = new AccountRESTClient();

            // Recuperar datos
            Account cuentaFresca = accountClient.find_XML(Account.class, String.valueOf(this.account.getId()));
            
            GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};
            List<Movement> historial = movementClient.findMovementByAccount_XML(listType, String.valueOf(cuentaFresca.getId()));

            double saldoBaseCalculo;

            if (historial == null || historial.isEmpty()) {
                // Aqui y SOLO aquí usamos el saldo inicial en caso de que sea el primer movimiento.
                saldoBaseCalculo = (cuentaFresca.getBeginBalance() != null) ? cuentaFresca.getBeginBalance() : 0.0;
            } else {
                //Aqui cuando hay otros movimientos, tenemos que suar el ultimo de todos y ordenamos para usarlo
                historial.sort((m1, m2) -> {
                    if (m1.getTimestamp() == null) return -1;
                    if (m2.getTimestamp() == null) return 1;
                    return m1.getTimestamp().compareTo(m2.getTimestamp());
                });

                Movement ultimoMovimientoReal = historial.get(historial.size() - 1);
                
                // Usamos el saldo con el que quedó la cuenta tras ese movimiento
                saldoBaseCalculo = (ultimoMovimientoReal.getBalance() != null) ? ultimoMovimientoReal.getBalance() : 0.0;
            }

            // Verificar fondos
            double lineaCredito = (cuentaFresca.getCreditLine() != null) ? cuentaFresca.getCreditLine() : 0.0;
            
            if ("PAYMENT".equals(cbType.getValue())) {
                double totalDisponible = saldoBaseCalculo + lineaCredito;
                
                if (amountInput > totalDisponible) {
                    mostrarError(String.format("Fondos insuficientes.\nSaldo Actual: %.2f\nCrédito: %.2f\nTotal Disp: %.2f", 
                            saldoBaseCalculo, lineaCredito, totalDisponible));
                    return;
                }
                amountInput = -amountInput; // Convertir a negativo
            }

            // Calcular nuevo saldo
            Double nuevoSaldoFinal = saldoBaseCalculo + amountInput;

            Movement movement = new Movement();
            movement.setAmount(amountInput);
            movement.setDescription(cbType.getValue());
            movement.setTimestamp(new Date());
            movement.setBalance(nuevoSaldoFinal); 

            // Crear el movimiento
            movementClient.create_XML(movement, String.valueOf(cuentaFresca.getId()));

            // Actualizar la cuenta
            cuentaFresca.setBalance(nuevoSaldoFinal);
            accountClient.updateAccount_XML(cuentaFresca);

            // Refrescar vista
            this.account.setBalance(nuevoSaldoFinal);
            tfAmount.clear();
            loadMovements();
            bUndoLastMovement.setDisable(false);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error: " + e.getMessage());
        } finally {
            if (movementClient != null) movementClient.close();
            if (accountClient != null) accountClient.close();
        }
    }

    private void handleUndoLastMovement(ActionEvent event) {
        if (tvMovements.getItems().isEmpty()) return;

        Movement lastMovement = tvMovements.getItems().stream()
                .max((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .orElse(null);
        
        if (lastMovement == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "¿Undo movement of " + lastMovement.getAmount() + "€?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            MovementRESTClient movementClient = null;
            AccountRESTClient accountClient = null;
            
            try {
                // leer saldo actual 
                actualizarSaldoDesdeServidor();
                Double saldoActual = (this.account.getBalance() != null) ? this.account.getBalance() : 0.0;
                
                // revertir la operacion
                Double saldoRestaurado = saldoActual - lastMovement.getAmount();

                // actualizar cuenta
                this.account.setBalance(saldoRestaurado);
                accountClient = new AccountRESTClient();
                accountClient.updateAccount_XML(this.account);

                // eliminar movimiento
                movementClient = new MovementRESTClient();
                movementClient.remove(lastMovement.getId().toString());

                // bloquear y refrescar
                bUndoLastMovement.setDisable(true);
                loadMovements();
                
            } catch (Exception e) {
                mostrarError("Error undoing movement.");
                e.printStackTrace();
            } finally {
                if (movementClient != null) movementClient.close();
                if (accountClient != null) accountClient.close();
            }
        }
    }

    private void loadMovements() {
        MovementRESTClient restClient = null;
        try {
            restClient = new MovementRESTClient();
            GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};
            List<Movement> movimientos = restClient.findMovementByAccount_XML(listType, String.valueOf(account.getId()));

            if (movimientos != null && !movimientos.isEmpty()) {
                movimientos.sort(Comparator.comparing(Movement::getTimestamp));
                tvMovements.setItems(FXCollections.observableArrayList(movimientos));
            } else {
                tvMovements.getItems().clear();
            }
        } catch (Exception e) {
            tvMovements.getItems().clear();
        } finally {
            if (restClient != null) restClient.close();
        }
    }

    private void handleGoBack() {
        if (this.stage != null) this.stage.close();
    }
    
    private void mostrarError(String m) { new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait(); }
}