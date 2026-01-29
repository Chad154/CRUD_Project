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

public class MovementController {

    private static final Logger LOGGER = Logger.getLogger("MovementController");
    
    // --- NUEVO: Límite máximo para evitar errores de precisión en Double ---
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
        // --- COLUMNAS ---
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

        // UI
        tfAccountId.setEditable(false);
        cbType.setItems(FXCollections.observableArrayList("DEPOSIT", "PAYMENT"));
        cbType.getSelectionModel().selectFirst();

        bCreateMovement.setOnAction(this::handleCreateMovement);
        bUndoLastMovement.setOnAction(this::handleUndoLastMovement);
        bGoBack.setOnAction(e -> handleGoBack());
        
        bUndoLastMovement.setDisable(true); 
    }

    public void initData(Stage stage, Account account) {
        this.stage = stage;
        this.account = account;
        tfAccountId.setText(String.valueOf(account.getId()));
        
        // Carga inicial segura
        actualizarSaldoDesdeServidor();
        loadMovements();
    }

    // --- MÉTODO CLAVE: Evita datos obsoletos ---
    private void actualizarSaldoDesdeServidor() {
        AccountRESTClient client = null;
        try {
            client = new AccountRESTClient();
            Account cuentaFresca = client.find_XML(Account.class, String.valueOf(this.account.getId()));
            if (cuentaFresca != null) {
                // IMPORTANTE: Solo actualizamos el saldo para no perder la info de crédito si el XML viene incompleto
                this.account.setBalance(cuentaFresca.getBalance());
            }
        } catch (Exception e) {
            LOGGER.severe("Error sincronizando saldo: " + e.getMessage());
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
            double amountInput = Double.parseDouble(tfAmount.getText());

            if (amountInput <= 0) {
                mostrarError("La cantidad debe ser mayor que cero.");
                return;
            }

            // --- NUEVO: VALIDACIÓN DE LÍMITE ---
            if (amountInput > MAX_AMOUNT_LIMIT) {
                mostrarError("La cantidad supera el límite permitido\n (900.000.000 €).");
                return;
            }

            // 1. LEER DEL SERVIDOR
            actualizarSaldoDesdeServidor();
            double saldoBase = (account.getBalance() != null) ? account.getBalance() : 0.0;
            
            // OBTENER LÍNEA DE CRÉDITO
            double lineaCredito = (account.getCreditLine() != null) ? account.getCreditLine() : 0.0;

            if ("PAYMENT".equals(cbType.getValue())) {
                double fondosDisponibles = saldoBase + lineaCredito;

                if (amountInput > fondosDisponibles) {
                    mostrarError(String.format("Fondos insuficientes.\nSaldo actual: %.2f\nLímite crédito: %.2f\nDisponible: %.2f", 
                            saldoBase, lineaCredito, fondosDisponibles));
                    return;
                }
                amountInput = -amountInput; 
            }

            // 2. CALCULAR
            Double nuevoSaldo = saldoBase + amountInput;

            // 3. CREAR MOVIMIENTO
            Movement movement = new Movement();
            movement.setAmount(amountInput);
            movement.setDescription(cbType.getValue());
            movement.setTimestamp(new Date());
            movement.setBalance(nuevoSaldo);

            movementClient = new MovementRESTClient();
            movementClient.create_XML(movement, String.valueOf(account.getId()));

            // 4. ACTUALIZAR CUENTA 
            this.account.setBalance(nuevoSaldo);
            accountClient = new AccountRESTClient();
            accountClient.updateAccount_XML(this.account);

            // 5. REFRESCAR
            tfAmount.clear();
            loadMovements(); 
            bUndoLastMovement.setDisable(false);

        } catch (NumberFormatException e) {
             mostrarError("Introduce un número válido");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error creando movimiento.");
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
                "¿Deshacer movimiento de " + lastMovement.getAmount() + "€?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            MovementRESTClient movementClient = null;
            AccountRESTClient accountClient = null;
            
            try {
                // 1. LEER SALDO REAL ACTUAL
                actualizarSaldoDesdeServidor();
                Double saldoActual = (this.account.getBalance() != null) ? this.account.getBalance() : 0.0;
                
                // 2. REVERTIR LA OPERACIÓN
                Double saldoRestaurado = saldoActual - lastMovement.getAmount();

                // 3. ACTUALIZAR CUENTA PRIMERO
                this.account.setBalance(saldoRestaurado);
                accountClient = new AccountRESTClient();
                accountClient.updateAccount_XML(this.account);

                // 4. BORRAR EL MOVIMIENTO DESPUÉS
                movementClient = new MovementRESTClient();
                movementClient.remove(lastMovement.getId().toString());

                // 5. BLOQUEAR Y REFRESCAR
                bUndoLastMovement.setDisable(true);
                loadMovements();
                
            } catch (Exception e) {
                mostrarError("Error al deshacer movimiento.");
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