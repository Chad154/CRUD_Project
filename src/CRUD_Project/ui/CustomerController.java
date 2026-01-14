package CRUD_Project.ui;

import CRUD_Project.logic.CustomerRESTClient;
import CRUD_Project.model.Customer;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

public class CustomerController{

    private static final Logger LOGGER = Logger.getLogger(CustomerController.class.getName());

    @FXML private TextField tfId;
    @FXML private TextField tfFirstName;
    @FXML private TextField tfLastName;
    @FXML private TextField tfEmail;

    @FXML private Button btSearch;
    @FXML private Button btCreate;
    @FXML private Button btUpdate;
    @FXML private Button btDelete;
    @FXML private Button btRefresh;
    @FXML private Button btExit;

    @FXML private TableView<Customer> tvCustomers;
    @FXML private TableColumn<Customer, String> colId;
    @FXML private TableColumn<Customer, String> colFirstName;
    @FXML private TableColumn<Customer, String> colLastName;
    @FXML private TableColumn<Customer, String> colEmail;

    private final ObservableList<Customer> lista = FXCollections.observableArrayList();

 
    public void init(Stage stage){

        colId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getId() == null ? "" : String.valueOf(data.getValue().getId())));
        colFirstName.setCellValueFactory(data ->
                new SimpleStringProperty(texto(data.getValue().getFirstName())));
        colLastName.setCellValueFactory(data ->
                new SimpleStringProperty(texto(data.getValue().getLastName())));
        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(texto(data.getValue().getEmail())));

        tvCustomers.setItems(lista);

        tvCustomers.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) ponerEnFormulario(newV);
        });

        btRefresh.setOnAction(e -> cargarLista());
        btSearch.setOnAction(e -> buscarPorId());
        btCreate.setOnAction(e -> crear());
        btUpdate.setOnAction(e -> actualizar());
        btDelete.setOnAction(e -> borrar());
        btExit.setOnAction(e -> cerrar());

        cargarLista();
    }

    // =========================
    // LISTA: intenta JSON primero (sin wrapper)
    // =========================
    private void cargarLista() {
    CustomerRESTClient client = new CustomerRESTClient();
    try {
        Customer[] customers = client.findAll_XML(Customer[].class);
        lista.clear();
        if (customers != null) {
            lista.addAll(Arrays.asList(customers));
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        mostrarError("Error", "No se pudo cargar la lista:\n" + ex.getMessage());
    } finally {
        client.close();
    }
}


    // =========================
    // READ BY ID
    // =========================
    private void buscarPorId() {
        String idTxt = texto(tfId.getText());

        if (!esNumero(idTxt)) {
            marcarError(tfId, true);
            mostrarInfo("Validación", "El ID debe ser numérico y no vacío.");
            return;
        }
        marcarError(tfId, false);

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            Customer c = client.find_XML(Customer.class, idTxt);
            if (c == null) mostrarInfo("No encontrado", "No existe customer con ese ID.");
            else ponerEnFormulario(c);
        } catch (NotFoundException ex) {
            mostrarInfo("No encontrado", "No existe customer con ese ID.");
        } catch (Exception ex) {
            mostrarError("Error", "Error buscando customer:\n" + ex);
        } finally {
            client.close();
        }
    }

    // =========================
    // CREATE
    // =========================
    private void crear() {
        if (!validarSinId()) return;

        Customer c = new Customer();
        c.setFirstName(texto(tfFirstName.getText()));
        c.setLastName(texto(tfLastName.getText()));
        c.setEmail(texto(tfEmail.getText()));

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            client.create_XML(c);
            mostrarInfo("OK", "Customer creado.");
            limpiarFormulario();
            cargarLista();
        } catch (Exception ex) {
            manejarError("crear", ex);
        } finally {
            client.close();
        }
    }

    // =========================
    // UPDATE
    // =========================
    private void actualizar() {
        String idTxt = texto(tfId.getText());

        if (!esNumero(idTxt)) {
            marcarError(tfId, true);
            mostrarInfo("Validación", "Para Update necesitas un ID numérico.");
            return;
        }
        marcarError(tfId, false);

        if (!validarSinId()) return;

        Customer c = new Customer();
        c.setId(Long.parseLong(idTxt)); // id Long
        c.setFirstName(texto(tfFirstName.getText()));
        c.setLastName(texto(tfLastName.getText()));
        c.setEmail(texto(tfEmail.getText()));

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            client.edit_XML(c);
            mostrarInfo("OK", "Customer actualizado.");
            cargarLista();
        } catch (Exception ex) {
            manejarError("actualizar", ex);
        } finally {
            client.close();
        }
    }

    // =========================
    // DELETE
    // =========================
    private void borrar() {
        String idTxt = texto(tfId.getText());

        if (!esNumero(idTxt)) {
            marcarError(tfId, true);
            mostrarInfo("Validación", "Para borrar necesitas un ID numérico.");
            return;
        }
        marcarError(tfId, false);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que quieres borrar el customer con ID " + idTxt + "?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirmar borrado");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            client.remove(idTxt);
            mostrarInfo("OK", "Customer borrado.");
            limpiarFormulario();
            cargarLista();
        } catch (Exception ex) {
            manejarError("borrar", ex);
        } finally {
            client.close();
        }
    }

    // =========================
    // Helpers
    // =========================
    private void ponerEnFormulario(Customer c) {
        tfId.setText(c.getId() == null ? "" : String.valueOf(c.getId()));
        tfFirstName.setText(texto(c.getFirstName()));
        tfLastName.setText(texto(c.getLastName()));
        tfEmail.setText(texto(c.getEmail()));
    }

    private void limpiarFormulario() {
        tfId.clear();
        tfFirstName.clear();
        tfLastName.clear();
        tfEmail.clear();
        tvCustomers.getSelectionModel().clearSelection();
        marcarError(tfId, false);
        marcarError(tfFirstName, false);
        marcarError(tfLastName, false);
        marcarError(tfEmail, false);
    }

    private boolean validarSinId() {
        if (texto(tfFirstName.getText()).isEmpty()) {
            marcarError(tfFirstName, true);
            mostrarInfo("Validación", "First Name obligatorio.");
            return false;
        }
        marcarError(tfFirstName, false);

        if (texto(tfLastName.getText()).isEmpty()) {
            marcarError(tfLastName, true);
            mostrarInfo("Validación", "Last Name obligatorio.");
            return false;
        }
        marcarError(tfLastName, false);

        String email = texto(tfEmail.getText());
        if (email.isEmpty() || !email.matches("^.+@.+\\..+$")) {
            marcarError(tfEmail, true);
            mostrarInfo("Validación", "Email no válido (ej: a@b.com).");
            return false;
        }
        marcarError(tfEmail, false);

        return true;
    }

    private void manejarError(String accion, Throwable ex) {
        LOGGER.warning("Error al " + accion + ": " + ex);

        if (ex instanceof NotFoundException) {
            mostrarInfo("No encontrado", "No existe el customer.");
        } else if (ex instanceof InternalServerErrorException) {
            mostrarError("Servidor", "Error interno del servidor al " + accion + ".");
        } else if (ex instanceof ClientErrorException) {
            mostrarError("REST",
                    "Error REST al " + accion + ":\n" + ex.getMessage()
                    + "\n\nSi el servidor exige más campos, añade esos campos al formulario.");
        } else {
            mostrarError("Error", "Error inesperado al " + accion + ":\n" + ex);
        }
    }

    private void cerrar() {
        ((Stage) btExit.getScene().getWindow()).close();
    }

    private void mostrarInfo(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void mostrarError(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void marcarError(TextField tf, boolean error) {
        tf.setStyle(error ? "-fx-border-color: red;" : null);
    }

    private boolean esNumero(String s) {
        return s != null && !s.trim().isEmpty() && s.trim().matches("\\d+");
    }

    private String texto(String s) {
        return (s == null) ? "" : s.trim();
    }
}
