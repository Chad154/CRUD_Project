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
            if (tfAmount.getText().isEmpty()) return;
            double amountInput = Double.parseDouble(tfAmount.getText());

            if (amountInput <= 0) {
                mostrarError("The amount must be greater than zero.");
                return;
            }

            // Validar limite maximo
            if (amountInput > MAX_AMOUNT_LIMIT) {
                mostrarError("The amount exceeds the permitted limit (€900,000,000).");
                return;
            }

            // Obtener balance del servidor
            actualizarSaldoDesdeServidor();
            double saldoBase = (account.getBalance() != null) ? account.getBalance() : 0.0;
            
            // Obteber linea de credito
            double lineaCredito = (account.getCreditLine() != null) ? account.getCreditLine() : 0.0;

            if ("PAYMENT".equals(cbType.getValue())) {
                double fondosDisponibles = saldoBase + lineaCredito;

                if (amountInput > fondosDisponibles) {
                    mostrarError(String.format("Insufficient funds.\nCurrent balance: %.2f\nCredit limit: %.2f\nAvailable: %.2f", 
                            saldoBase, lineaCredito, fondosDisponibles));
                    return;
                }
                amountInput = -amountInput; 
            }

            // Calculo local
            Double nuevoSaldo = saldoBase + amountInput;

            // Crear movimiento
            Movement movement = new Movement();
            movement.setAmount(amountInput);
            movement.setDescription(cbType.getValue());
            movement.setTimestamp(new Date());
            movement.setBalance(nuevoSaldo);

            movementClient = new MovementRESTClient();
            movementClient.create_XML(movement, String.valueOf(account.getId()));

            // Actualizar cuenta
            this.account.setBalance(nuevoSaldo);
            accountClient = new AccountRESTClient();
            accountClient.updateAccount_XML(this.account);

            // Refrescar
            tfAmount.clear();
            loadMovements(); 
            bUndoLastMovement.setDisable(false);

        } catch (NumberFormatException e) {
             mostrarError("Enter a valid number.");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error creating movement.");
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