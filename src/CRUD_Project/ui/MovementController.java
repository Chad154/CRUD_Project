package CRUD_Project.ui;

import CRUD_Project.logic.MovementRESTClient;
import CRUD_Project.model.Movement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javax.ws.rs.core.GenericType;

public class MovementController {

    private static final Logger LOGGER = Logger.getLogger("MovementController");

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

    @FXML
    public void initialize() {
        // 1. Configurar Columnas (Mapeo con el modelo Java)
        colDate.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTimestamp()));
        colDescription.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colAmount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAmount()));
        colBalance.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getBalance()));

        // Formato visual de la fecha en la tabla (Día/Mes/Año Hora:Minuto)
        colDate.setCellFactory(column -> new TableCell<Movement, Date>() {
            private final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : format.format(item));
            }
        });

        // 2. Configurar UI
        tfAccountId.setEditable(false);
        cbType.setItems(FXCollections.observableArrayList("DEPOSIT", "PAYMENT"));
        cbType.getSelectionModel().selectFirst();

        bCreateMovement.setOnAction(this::handleCreateMovement);
        bUndoLastMovement.setOnAction(this::handleUndoLastMovement);
        bUndoLastMovement.setDisable(true);

        // 3. Carga inicial
        String idPrueba = "2654785441"; 
        tfAccountId.setText(idPrueba);
        loadMovements(idPrueba);
    }

    private void handleCreateMovement(ActionEvent event) {
        MovementRESTClient restClient = null;
        try {
            String accountId = tfAccountId.getText();
            String type = cbType.getValue();
            double amountInput = Double.parseDouble(tfAmount.getText());

            if (amountInput <= 0) {
                new Alert(Alert.AlertType.ERROR, "Amount must be greater than zero").show();
                return;
            }

            // --- VALIDACIÓN DE SALDO ---
            if ("PAYMENT".equals(type)) {
                double currentBalance = tvMovements.getItems().stream()
                        .mapToDouble(Movement::getAmount)
                        .sum();

                if (amountInput > currentBalance) {
                    new Alert(Alert.AlertType.WARNING, "Insufficient funds. Current balance: " + currentBalance).show();
                    return;
                }
                amountInput = -amountInput; 
            }

            // --- CREACIÓN DEL OBJETO CON FECHA LOCAL ---
            Movement movement = new Movement();
            movement.setAmount(amountInput);
            movement.setDescription(type);
            movement.setTimestamp(new Date()); // <--- AQUÍ capturas la fecha local del ordenador

            restClient = new MovementRESTClient();
            restClient.create_XML(movement, accountId);

            bUndoLastMovement.setDisable(false);
            loadMovements(accountId); // Refresca para ver el nuevo registro
            tfAmount.clear();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid amount format").show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error creating movement").show();
        } finally {
            if (restClient != null) restClient.close();
        }
    }

    private void handleUndoLastMovement(ActionEvent event) {
        if (tvMovements.getItems().isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Undo last movement?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            Movement lastMovement = tvMovements.getItems().get(tvMovements.getItems().size() - 1);
            MovementRESTClient restClient = null;
            try {
                restClient = new MovementRESTClient();
                restClient.remove(lastMovement.getId().toString());
                loadMovements(tfAccountId.getText());
                bUndoLastMovement.setDisable(true);
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Error undoing movement").show();
            } finally {
                if (restClient != null) restClient.close();
            }
        }
    }

    private void loadMovements(String accountId) {
        MovementRESTClient restClient = null;
        try {
            restClient = new MovementRESTClient();
            GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};
            List<Movement> movimientos = restClient.findMovementByAccount_XML(listType, accountId);

            if (movimientos != null) {
                tvMovements.setItems(FXCollections.observableArrayList(movimientos));
            } else {
                tvMovements.getItems().clear();
            }
        } catch (Exception e) {
            LOGGER.severe("Error loading: " + e.getMessage());
        } finally {
            if (restClient != null) restClient.close();
        }
    }
}