package CRUD_Project.ui;

import CRUD_Project.logic.CustomerRESTClient;
import CRUD_Project.model.Customer;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.net.URL;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

public class CustomerController {

    private static final Logger LOGGER = Logger.getLogger(CustomerController.class.getName());

    // Campos (UI)
    @FXML private TextField tfId;
    @FXML private TextField tfFirstName;
    @FXML private TextField tfLastName;
    @FXML private TextField tfMiddleInitial;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;

    @FXML private TextField tfStreet;
    @FXML private TextField tfCity;
    @FXML private TextField tfState;
    @FXML private TextField tfZip;
    @FXML private TextField tfPhone;

    // Botones
    @FXML private Button btSearch;
    @FXML private Button btCreate;
    @FXML private Button btUpdate;
    @FXML private Button btDelete;
    @FXML private Button btRefresh;
    @FXML private Button btExit;

    // Tabla
    @FXML private TableView<Customer> tvCustomers;
    @FXML private TableColumn<Customer, String> colId;
    @FXML private TableColumn<Customer, String> colFirstName;
    @FXML private TableColumn<Customer, String> colLastName;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, String> colCity;
    @FXML private TableColumn<Customer, String> colPhone;

    private final ObservableList<Customer> lista = FXCollections.observableArrayList();

    public void init(Stage stage) {

        colId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getId() == null ? "" : String.valueOf(data.getValue().getId())));
        colFirstName.setCellValueFactory(data ->
                new SimpleStringProperty(texto(data.getValue().getFirstName())));
        colLastName.setCellValueFactory(data ->
                new SimpleStringProperty(texto(data.getValue().getLastName())));
        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(texto(data.getValue().getEmail())));
        colCity.setCellValueFactory(data ->
                new SimpleStringProperty(texto(data.getValue().getCity())));
        colPhone.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPhone() == null ? "" : String.valueOf(data.getValue().getPhone())));

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
        
        btExit.setCancelButton(true);
        btSearch.setDefaultButton(true);
        tfId.requestFocus();
    }

    private void cargarLista() {
        CustomerRESTClient client = new CustomerRESTClient();
        try {
            Customer[] customers = client.findAll_XML(Customer[].class);
            lista.clear();
            if (customers != null) lista.addAll(Arrays.asList(customers));
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo cargar la lista:\n" + ex.getMessage());
        } finally {
            client.close();
        }
    }

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

    private void crear() {
        if (!validarFormulario(false)) return;

        Customer c = construirCustomerDesdeFormulario(null);

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

    private void actualizar() {
        String idTxt = texto(tfId.getText());
        if (!esNumero(idTxt)) {
            marcarError(tfId, true);
            mostrarInfo("Validación", "Para Update necesitas un ID numérico.");
            return;
        }
        marcarError(tfId, false);

        if (!validarFormulario(true)) return;

        Long id = Long.parseLong(idTxt);
        Customer c = construirCustomerDesdeFormulario(id);

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

    private Customer construirCustomerDesdeFormulario(Long id) {
        Customer c = new Customer();
        if (id != null) c.setId(id);

        c.setFirstName(texto(tfFirstName.getText()));
        c.setLastName(texto(tfLastName.getText()));
        c.setMiddleInitial(texto(tfMiddleInitial.getText()));
        c.setEmail(texto(tfEmail.getText()));
        c.setPassword(texto(pfPassword.getText()));
  
        c.setStreet(texto(tfStreet.getText()));
        c.setCity(texto(tfCity.getText()));
        c.setState(texto(tfState.getText()));

        c.setZip(Integer.parseInt(texto(tfZip.getText())));
        c.setPhone(Long.parseLong(texto(tfPhone.getText())));

        return c;
    }

    private void ponerEnFormulario(Customer c) {
        tfId.setText(c.getId() == null ? "" : String.valueOf(c.getId()));
        tfFirstName.setText(texto(c.getFirstName()));
        tfLastName.setText(texto(c.getLastName()));
        tfMiddleInitial.setText(texto(c.getMiddleInitial()));
        tfEmail.setText(texto(c.getEmail()));
        pfPassword.setText(texto(c.getPassword()));

        tfStreet.setText(texto(c.getStreet()));
        tfCity.setText(texto(c.getCity()));
        tfState.setText(texto(c.getState()));
        tfZip.setText(c.getZip() == null ? "" : String.valueOf(c.getZip()));
        tfPhone.setText(c.getPhone() == null ? "" : String.valueOf(c.getPhone()));
    }

    private void limpiarFormulario() {
        tfId.clear();
        tfFirstName.clear();
        tfLastName.clear();
        tfMiddleInitial.clear();
        tfEmail.clear();
        pfPassword.clear();

        tfStreet.clear();
        tfCity.clear();
        tfState.clear();
        tfZip.clear();
        tfPhone.clear();

        tvCustomers.getSelectionModel().clearSelection();

        marcarError(tfId, false);
        marcarError(tfFirstName, false);
        marcarError(tfLastName, false);
        marcarError(tfMiddleInitial, false);
        marcarError(tfEmail, false);
        marcarError(tfStreet, false);
        marcarError(tfCity, false);
        marcarError(tfState, false);
        marcarError(tfZip, false);
        marcarError(tfPhone, false);
    }

    /**
     * Validaciones básicas (ajústalas si tu profe pide reglas concretas).
     * - create/update: mismos checks.
     * - id se valida fuera (solo en update/delete/search).
     */
    private boolean validarFormulario(boolean esUpdate) {
        boolean ok = true;

        ok &= validarObligatorio(tfFirstName, "First Name obligatorio.");
        ok &= validarObligatorio(tfLastName, "Last Name obligatorio.");
        ok &= validarEmail(tfEmail);
        ok &= validarPassword(pfPassword);

        ok &= validarObligatorio(tfStreet, "Street obligatoria.");
        ok &= validarObligatorio(tfCity, "City obligatoria.");
        ok &= validarState(tfState);
        ok &= validarZip(tfZip);
        ok &= validarPhone(tfPhone);

        // MiddleInitial opcional, pero si lo ponen: 1 letra
        String mi = texto(tfMiddleInitial.getText());
        if (!mi.isEmpty() && !mi.matches("^[A-Za-z]$")) {
            marcarError(tfMiddleInitial, true);
            mostrarInfo("Validación", "Middle Initial debe ser 1 letra (opcional).");
            return false;
        } else {
            marcarError(tfMiddleInitial, false);
        }

        return ok;
    }

    private boolean validarObligatorio(TextField tf, String mensaje) {
        if (texto(tf.getText()).isEmpty()) {
            marcarError(tf, true);
            mostrarInfo("Validación", mensaje);
            return false;
        }
        marcarError(tf, false);
        return true;
    }

    private boolean validarEmail(TextField tf) {
        String email = texto(tf.getText());
        if (email.isEmpty() || !email.matches("^.+@.+\\..+$")) {
            marcarError(tf, true);
            mostrarInfo("Validación", "Email no válido (ej: a@b.com).");
            return false;
        }
        marcarError(tf, false);
        return true;
    }

    private boolean validarPassword(PasswordField pf) {
        String pw = texto(pf.getText());
        if (pw.isEmpty() || pw.length() < 4) {
            pf.setStyle("-fx-border-color: red;");
            mostrarInfo("Validación", "Password obligatorio (mínimo 4 caracteres).");
            return false;
        }
        pf.setStyle(null);
        return true;
    }

    private boolean validarState(TextField tf) {
        String st = texto(tf.getText());
        if (st.isEmpty() || st.length() < 2 || st.length() > 30) {
            marcarError(tf, true);
            mostrarInfo("Validación", "State obligatorio (2 a 30 caracteres).");
            return false;
        }
        marcarError(tf, false);
        return true;
    }

    private boolean validarZip(TextField tf) {
        String zip = texto(tf.getText());
        if (!zip.matches("^\\d{4,10}$")) {
            marcarError(tf, true);
            mostrarInfo("Validación", "ZIP debe ser numérico (4 a 10 dígitos).");
            return false;
        }
        marcarError(tf, false);
        return true;
    }

    private boolean validarPhone(TextField tf) {
        String ph = texto(tf.getText());
        if (!ph.matches("^\\d{6,15}$")) {
            marcarError(tf, true);
            mostrarInfo("Validación", "Phone debe ser numérico (6 a 15 dígitos).");
            return false;
        }
        marcarError(tf, false);
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
                    + "\n\nSi el servidor exige algún campo adicional, revisa el modelo Customer.");
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
