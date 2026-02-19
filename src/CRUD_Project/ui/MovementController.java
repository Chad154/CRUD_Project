package CRUD_Project.ui;

import CRUD_Project.logic.AccountRESTClient;
import CRUD_Project.logic.MovementRESTClient;
import CRUD_Project.model.Account;
import CRUD_Project.model.Movement;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
public class MovementController implements Initializable, MenuActionsHandler {

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
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (menuIncludeController != null) {
            menuIncludeController.setMenuActionsHandler(this);
        }

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

    @Override
    public void onCreate() {// Se usa handleCreateMovement
        handleCreateMovement(new ActionEvent());
    }

    @Override
    public void onRefresh() {// Se usa loadMovements()
        actualizarSaldoDesdeServidor();
        loadMovements();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Action Refresh");
        alert.setHeaderText(null);
        alert.setContentText("Refreshed succesfully");
        alert.showAndWait();
    }

    @Override
    public void onUpdate() {//Como movimientos no tiene udate simplemente enseñamos un alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Action Update");
        alert.setHeaderText(null);
        alert.setContentText("You can not update in movements. "
                + "You can only update in Customers and Accounts views");
        alert.showAndWait();
    }

    @Override
    public void onDelete() {//Se usa  handleUndoLastMovement
        // Si esta deshabilitado salta un mensaje para inducar al usuario
        if (!bUndoLastMovement.isDisabled()) {
            handleUndoLastMovement(new ActionEvent());
        } else {
            mostrarError("You can only delete the last movements created.");
        }
    }

    public void initData(Stage stage, Account account) {
        this.stage = stage;
        this.account = account;
        tfAccountId.setText(String.valueOf(account.getId()));
        
        actualizarSaldoDesdeServidor();
        loadMovements();

        javafx.application.Platform.runLater(() -> {
            tfAmount.requestFocus();
            tfAmount.deselect();
        });
    }

    private void actualizarSaldoDesdeServidor() {
        AccountRESTClient client = null;
        try {
            client = new AccountRESTClient();
            Account cuentaFresca = client.find_XML(Account.class, String.valueOf(this.account.getId()));
            if (cuentaFresca != null) {
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
            if (tfAmount.getText().isEmpty()) return;
            double amountInput;
            try {
                amountInput = Double.parseDouble(tfAmount.getText());
            } catch (NumberFormatException e) {
                mostrarError("Introduce a valid number");
                return;
            }
            if (amountInput <= 0) {
                mostrarError("The amount must be positive");
                return;
            }
            if (amountInput > MAX_AMOUNT_LIMIT) {
                mostrarError("Limit passed");
                return;
            }

            movementClient = new MovementRESTClient();
            accountClient = new AccountRESTClient();

            Account cuentaFresca = accountClient.find_XML(Account.class, String.valueOf(this.account.getId()));
            GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};
            List<Movement> historial = movementClient.findMovementByAccount_XML(listType, String.valueOf(cuentaFresca.getId()));

            double saldoBaseCalculo;
            if (historial == null || historial.isEmpty()) {
                saldoBaseCalculo = (cuentaFresca.getBeginBalance() != null) ? cuentaFresca.getBeginBalance() : 0.0;
            } else {
                historial.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
                Movement ultimoMovimientoReal = historial.get(historial.size() - 1);
                saldoBaseCalculo = (ultimoMovimientoReal.getBalance() != null) ? ultimoMovimientoReal.getBalance() : 0.0;
            }

            double lineaCredito = (cuentaFresca.getCreditLine() != null) ? cuentaFresca.getCreditLine() : 0.0;
            if ("PAYMENT".equals(cbType.getValue())) {
                double totalDisponible = saldoBaseCalculo + lineaCredito;
                if (amountInput > totalDisponible) {
                    mostrarError(String.format("You dont have enough money.\nSaldo Actual: %.2f\nCredit: %.2f", saldoBaseCalculo, lineaCredito));
                    return;
                }
                amountInput = -amountInput;
            }

            Double nuevoSaldoFinal = saldoBaseCalculo + amountInput;
            Movement movement = new Movement();
            movement.setAmount(amountInput);
            movement.setDescription(cbType.getValue());
            movement.setTimestamp(new Date());
            movement.setBalance(nuevoSaldoFinal); 

            movementClient.create_XML(movement, String.valueOf(cuentaFresca.getId()));
            cuentaFresca.setBalance(nuevoSaldoFinal);
            accountClient.updateAccount_XML(cuentaFresca);

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

    @FXML
    private void handleUndoLastMovement(ActionEvent event) {
        if (tvMovements.getItems().isEmpty()) return;

        Movement lastMovement = tvMovements.getItems().stream()
                .max((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .orElse(null);
        
        if (lastMovement == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "¿Undo last movement of " + lastMovement.getAmount() + "€?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            MovementRESTClient movementClient = null;
            AccountRESTClient accountClient = null;
            
            try {
                actualizarSaldoDesdeServidor();
                Double saldoActual = (this.account.getBalance() != null) ? this.account.getBalance() : 0.0;
                Double saldoRestaurado = saldoActual - lastMovement.getAmount();

                this.account.setBalance(saldoRestaurado);
                accountClient = new AccountRESTClient();
                accountClient.updateAccount_XML(this.account);

                movementClient = new MovementRESTClient();
                movementClient.remove(lastMovement.getId().toString());

                // IMPORTANTE: Tras usar Undo/Delete, se vuelve a bloquear hasta el siguiente Create
                bUndoLastMovement.setDisable(true);
                loadMovements();
                
            } catch (Exception e) {
                mostrarError("Error UndoMovement.");
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