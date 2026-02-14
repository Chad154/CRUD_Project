package CRUD_Project.ui;

import CRUD_Project.logic.CustomerRESTClient;
import CRUD_Project.model.Customer;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
/**
 * @todo @fixme Hacer que la siguiente clase implemente las interfaces 
 * Initializable y MenuActionsHandler para que al pulsar en las acciones CRUD del 
 * menú Actions se ejecuten los métodos manejadores correspondientes a la vista 
 * que incluye el menú.
 * El método initialize debe llamar a setMenuActionsHandler() para establecer que este
 * controlador es el manejador de acciones del menú.
 */
public class CustomerController {

    /**
     * TODO: NO TOCAR La siguiente referencia debe llamarse así y tener este tipo.
     * JavaFX asigna automáticamente el campo menuIncludeController cuando usas fx:id="menuInclude".
     */
    @FXML
    private MenuController menuIncludeController;

    // Logger para registrar errores e información relevante de la pantalla
    private static final Logger LOGGER = Logger.getLogger(CustomerController.class.getName());

    // Campos del formulario (FXML)
    @FXML
    private TextField tfId;
    @FXML
    private TextField tfFirstName;
    @FXML
    private TextField tfLastName;
    @FXML
    private TextField tfMiddleInitial;
    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pfPassword;

    @FXML
    private TextField tfStreet;
    @FXML
    private TextField tfCity;
    @FXML
    private TextField tfState;
    @FXML
    private TextField tfZip;
    @FXML
    private TextField tfPhone;

    // Botones (FXML)
    @FXML
    private Button btSearch;
    @FXML
    private Button btCreate;
    @FXML
    private Button btUpdate;
    @FXML
    private Button btDelete;
    @FXML
    private Button btRefresh;
    @FXML
    private Button btExit;

    // Tabla y columnas (FXML)
    @FXML
    private TableView<Customer> tvCustomers;
    @FXML
    private TableColumn<Customer, String> colId;
    @FXML
    private TableColumn<Customer, String> colFirstName;
    @FXML
    private TableColumn<Customer, String> colLastName;
    @FXML
    private TableColumn<Customer, String> colEmail;
    @FXML
    private TableColumn<Customer, String> colCity;
    @FXML
    private TableColumn<Customer, String> colPhone;
    @FXML
    private TableColumn<Customer, String> colMiddleInitial;
    @FXML
    private TableColumn<Customer, String> colStreet;
    @FXML
    private TableColumn<Customer, String> colState;
    @FXML
    private TableColumn<Customer, String> colZip;

    // Lista observable que alimenta la TableView
    private final ObservableList<Customer> lista = FXCollections.observableArrayList();

    /**
     * Inicializa la pantalla: - Configura las columnas de la tabla. - Enlaza
     * listeners y acciones de botones. - Carga la lista inicial de customers
     * desde el servidor.
     */
    public void init(Stage stage) {

        // Configuración de columnas (mapea propiedades del modelo a texto en la tabla)
        colId.setCellValueFactory(data
                -> new SimpleStringProperty(data.getValue().getId() == null ? "" : String.valueOf(data.getValue().getId())));
        colFirstName.setCellValueFactory(data
                -> new SimpleStringProperty(texto(data.getValue().getFirstName())));
        colLastName.setCellValueFactory(data
                -> new SimpleStringProperty(texto(data.getValue().getLastName())));
        colEmail.setCellValueFactory(data
                -> new SimpleStringProperty(texto(data.getValue().getEmail())));
        colCity.setCellValueFactory(data
                -> new SimpleStringProperty(texto(data.getValue().getCity())));
        colMiddleInitial.setCellValueFactory(d
                -> new SimpleStringProperty(texto(d.getValue().getMiddleInitial())));
        colStreet.setCellValueFactory(d
                -> new SimpleStringProperty(texto(d.getValue().getStreet())));
        colState.setCellValueFactory(d
                -> new SimpleStringProperty(texto(d.getValue().getState())));
        colZip.setCellValueFactory(d
                -> new SimpleStringProperty(d.getValue().getZip() == null ? "" : d.getValue().getZip().toString()));
        colPhone.setCellValueFactory(d
                -> new SimpleStringProperty(d.getValue().getPhone() == null ? "" : d.getValue().getPhone().toString()));

        // Enlaza la lista observable a la tabla
        tvCustomers.setItems(lista);

        // Cuando el usuario selecciona una fila, se vuelcan los datos al formulario
        tvCustomers.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                runSafe("populate form", () -> ponerEnFormulario(newV));
            }
        });

        // Acciones de botones, encapsuladas en runSafe para evitar que una excepción rompa la UI
        btRefresh.setOnAction(e -> runSafe("refresh list", this::cargarLista));
        btSearch.setOnAction(e -> runSafe("search by id", this::buscarPorId));
        btCreate.setOnAction(e -> runSafe("create customer", this::crear));
        btUpdate.setOnAction(e -> runSafe("update customer", this::actualizar));
        btDelete.setOnAction(e -> runSafe("delete customer", this::borrar));
        btExit.setOnAction(e -> runSafe("exit", this::cerrar));

        // Carga inicial
        cargarLista();

        // Atajos de teclado típicos
        btExit.setCancelButton(true);
        btSearch.setDefaultButton(true);
        tfId.requestFocus();
    }

    /**
     * Carga todos los customers desde el servidor y refresca la TableView.
     */
    private void cargarLista() {
        CustomerRESTClient client = new CustomerRESTClient();
        try {
            Customer[] customers = client.findAll_XML(Customer[].class);
            lista.clear();
            if (customers != null) {
                lista.addAll(Arrays.asList(customers));
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not load customer list.", ex);
            mostrarError("Error", "Could not load the list:\n" + safeMsg(ex));
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error closing REST client (load list).", ex);
            }
        }
    }

    /**
     * Busca un customer por ID (introducido en tfId) y lo muestra en el
     * formulario si existe.
     */
    private void buscarPorId() {
        String idTxt = texto(tfId.getText());

        // Validación: el ID debe ser numérico y no vacío
        if (!esNumero(idTxt)) {
            marcarError(tfId, true);
            mostrarInfo("Validation", "ID must be numeric and not empty.");
            return;
        }
        marcarError(tfId, false);

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            Customer c = client.find_XML(Customer.class, idTxt);
            if (c == null) {
                mostrarInfo("Not found", "No customer exists with that ID.");
            } else {
                ponerEnFormulario(c);
            }
        } catch (NotFoundException ex) {
            mostrarInfo("Not found", "No customer exists with that ID.");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error searching customer by ID.", ex);
            mostrarError("Error", "Error searching customer:\n" + safeMsg(ex));
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error closing REST client (search).", ex);
            }
        }
    }

    // ========================= CREATE =========================
    /**
     * Crea un customer: - Valida el formulario. - Comprueba email duplicado. -
     * Envía el create al servidor.
     */
    private void crear() {
        if (!validarFormulario(false)) {
            return;
        }

        // Bloqueo de email duplicado en cliente
        if (emailYaExiste(texto(tfEmail.getText()), null)) {
            marcarError(tfEmail, true);
            mostrarError("Error", "Email already exists.");
            return;
        }
        marcarError(tfEmail, false);

        Customer c;
        try {
            c = construirCustomerDesdeFormulario(null);
        } catch (IllegalArgumentException ex) {
            // Se usa para errores de parseo o validaciones adicionales internas
            LOGGER.log(Level.INFO, "Validation error while building customer.", ex);
            mostrarInfo("Validation", safeMsg(ex));
            return;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unexpected error while building customer.", ex);
            mostrarError("Error", "Unexpected error building customer:\n" + safeMsg(ex));
            return;
        }

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            client.create_XML(c);
            mostrarInfo("OK", "Customer created.");
            limpiarFormulario();
            cargarLista();

        } catch (ForbiddenException ex) {
            // Si el servidor devuelve 403 por duplicado u otra regla
            LOGGER.log(Level.INFO, "Server rejected creation (forbidden).", ex);
            mostrarError("Error", "Email already exists.");

        } catch (InternalServerErrorException ex) {
            LOGGER.log(Level.WARNING, "Server error while creating customer.", ex);
            mostrarError("Server", "Server error while creating customer.");

        } catch (Exception ex) {
            manejarError("create", ex);

        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error closing REST client (create).", ex);
            }
        }
    }

    // ========================= UPDATE =========================
    /**
     * Actualiza un customer: - Requiere ID numérico. - Valida formulario. -
     * Comprueba email duplicado ignorando el propio ID. - Envía edit al
     * servidor.
     */
    private void actualizar() {
        String idTxt = texto(tfId.getText());
        if (!esNumero(idTxt)) {
            marcarError(tfId, true);
            mostrarInfo("Validation", "Update requires a numeric ID.");
            return;
        }
        marcarError(tfId, false);

        if (!validarFormulario(true)) {
            return;
        }

        Long id;
        try {
            id = Long.parseLong(idTxt);
        } catch (NumberFormatException ex) {
            marcarError(tfId, true);
            LOGGER.log(Level.INFO, "Invalid ID format.", ex);
            mostrarInfo("Validation", "ID format is invalid.");
            return;
        }

        // Bloqueo de email duplicado en update ignorando mi propio ID
        if (emailYaExiste(texto(tfEmail.getText()), id)) {
            marcarError(tfEmail, true);
            mostrarError("Error", "Email already exists.");
            return;
        }
        marcarError(tfEmail, false);

        Customer c;
        try {
            c = construirCustomerDesdeFormulario(id);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.INFO, "Validation error while building customer.", ex);
            mostrarInfo("Validation", safeMsg(ex));
            return;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unexpected error while building customer.", ex);
            mostrarError("Error", "Unexpected error building customer:\n" + safeMsg(ex));
            return;
        }

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            client.edit_XML(c);
            mostrarInfo("OK", "Customer updated.");
            cargarLista();

        } catch (ForbiddenException ex) {
            LOGGER.log(Level.INFO, "Server rejected update (forbidden).", ex);
            mostrarError("Error", "Email already exists.");

        } catch (Exception ex) {
            manejarError("update", ex);

        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error closing REST client (update).", ex);
            }
        }
    }

    // ========================= DELETE =========================
    /**
     * Borra un customer por ID: - Requiere ID numérico. - Pide confirmación. -
     * Envía remove al servidor.
     */
    private void borrar() {
        String idTxt = texto(tfId.getText());
        if (!esNumero(idTxt)) {
            marcarError(tfId, true);
            mostrarInfo("Validation", "Delete requires a numeric ID.");
            return;
        }
        marcarError(tfId, false);

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete the customer with ID " + idTxt + "?",
                ButtonType.OK, ButtonType.CANCEL
        );
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete confirmation");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            client.remove(idTxt);
            mostrarInfo("OK", "Customer deleted.");
            limpiarFormulario();
            cargarLista();
        } catch (Exception ex) {
            manejarError("delete", ex);
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error closing REST client (delete).", ex);
            }
        }
    }

    // ========================= EMAIL DUPLICATE CHECK =========================
    /**
     * Devuelve true si ya existe un customer con ese email. Si ignoreId !=
     * null, ignora ese customer (caso update).
     */
    private boolean emailYaExiste(String email, Long ignoreId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String target = email.trim().toLowerCase();

        // Si ya está la lista cargada, se usa directamente (rápido)
        if (!lista.isEmpty()) {
            try {
                for (Customer c : lista) {
                    if (c == null) {
                        continue;
                    }
                    if (ignoreId != null && c.getId() != null && c.getId().equals(ignoreId)) {
                        continue;
                    }

                    String e = texto(c.getEmail()).toLowerCase();
                    if (!e.isEmpty() && e.equals(target)) {
                        return true;
                    }
                }
                return false;
            } catch (Exception ex) {
                // Si algo raro pasa leyendo la lista, no bloqueamos con falso positivo
                LOGGER.log(Level.WARNING, "Could not check duplicate email using local list.", ex);
                return false;
            }
        }

        // Si la lista está vacía, consulta al servidor
        CustomerRESTClient client = new CustomerRESTClient();
        try {
            Customer[] customers = client.findAll_XML(Customer[].class);
            if (customers == null) {
                return false;
            }

            for (Customer c : customers) {
                if (c == null) {
                    continue;
                }
                if (ignoreId != null && c.getId() != null && c.getId().equals(ignoreId)) {
                    continue;
                }

                String e = texto(c.getEmail()).toLowerCase();
                if (!e.isEmpty() && e.equals(target)) {
                    return true;
                }
            }
            return false;

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not check duplicate email using server list.", ex);
            return false;
        } finally {
            try {
                client.close();
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error closing REST client (email check).", ex);
            }
        }
    }

    // ========================= FORM <-> MODEL MAPPING =========================
    /**
     * Construye un Customer desde el formulario. Lanza IllegalArgumentException
     * si zip/phone no se pueden parsear.
     */
    private Customer construirCustomerDesdeFormulario(Long id) {
        Customer c = new Customer();
        if (id != null) {
            c.setId(id);
        }

        // Copia campos de texto
        c.setFirstName(texto(tfFirstName.getText()));
        c.setLastName(texto(tfLastName.getText()));
        c.setMiddleInitial(texto(tfMiddleInitial.getText()));
        c.setEmail(texto(tfEmail.getText()));
        c.setPassword(texto(pfPassword.getText()));

        c.setStreet(texto(tfStreet.getText()));
        c.setCity(texto(tfCity.getText()));
        c.setState(texto(tfState.getText()));

        // Parseo seguro de ZIP (int) y Phone (long)
        try {
            c.setZip(Integer.parseInt(texto(tfZip.getText())));
        } catch (NumberFormatException ex) {
            marcarError(tfZip, true);
            throw new IllegalArgumentException("ZIP must be numeric.");
        }

        try {
            c.setPhone(Long.parseLong(texto(tfPhone.getText())));
        } catch (NumberFormatException ex) {
            marcarError(tfPhone, true);
            throw new IllegalArgumentException("Phone must be numeric.");
        }

        return c;
    }

    /**
     * Vuelca los datos del Customer al formulario.
     */
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

    /**
     * Limpia el formulario y estilos de error.
     */
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
        pfPassword.setStyle(null);
    }

    // ========================= VALIDATIONS =========================
    /**
     * Valida el formulario con reglas: - Obligatorios no vacíos. - Longitud
     * máxima. - Formato email. - Zip/Phone numéricos y límites. - Password
     * mínimo 8 y máximo 255.
     */
    private boolean validarFormulario(boolean esUpdate) {

        if (!validarObligatorio(tfFirstName, "First name is required.")) {
            return false;
        }
        if (!validarObligatorio(tfLastName, "Last name is required.")) {
            return false;
        }
        if (!validarObligatorio(tfMiddleInitial, "Middle initial is required.")) {
            return false;
        }
        if (!validarObligatorio(tfStreet, "Street is required.")) {
            return false;
        }
        if (!validarObligatorio(tfCity, "City is required.")) {
            return false;
        }
        if (!validarObligatorio(tfState, "State is required.")) {
            return false;
        }
        if (!validarObligatorio(tfZip, "ZIP is required.")) {
            return false;
        }
        if (!validarObligatorio(tfPhone, "Phone is required.")) {
            return false;
        }
        if (!validarObligatorio(tfEmail, "Email is required.")) {
            return false;
        }

        if (!validarMaxLen(tfFirstName, 255, "First name cannot exceed 255 characters.")) {
            return false;
        }
        if (!validarMaxLen(tfLastName, 255, "Last name cannot exceed 255 characters.")) {
            return false;
        }
        if (!validarMaxLen(tfMiddleInitial, 255, "Middle initial cannot exceed 255 characters.")) {
            return false;
        }
        if (!validarMaxLen(tfStreet, 255, "Street cannot exceed 255 characters.")) {
            return false;
        }
        if (!validarMaxLen(tfCity, 255, "City cannot exceed 255 characters.")) {
            return false;
        }
        if (!validarMaxLen(tfState, 255, "State cannot exceed 255 characters.")) {
            return false;
        }
        if (!validarMaxLen(tfEmail, 255, "Email cannot exceed 255 characters.")) {
            return false;
        }

        if (!validarZip(tfZip)) {
            return false;
        }
        if (!validarPhone(tfPhone)) {
            return false;
        }
        if (!validarEmail(tfEmail)) {
            return false;
        }
        if (!validarPassword(pfPassword)) {
            return false;
        }

        return true;
    }

    private boolean validarObligatorio(TextField tf, String mensaje) {
        try {
            if (texto(tf.getText()).isEmpty()) {
                marcarError(tf, true);
                mostrarInfo("Validation", mensaje);
                return false;
            }
            marcarError(tf, false);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error validating required field.", ex);
            mostrarError("Error", "Unexpected validation error:\n" + safeMsg(ex));
            return false;
        }
    }

    private boolean validarMaxLen(TextField tf, int max, String mensaje) {
        try {
            String v = texto(tf.getText());
            if (v.length() > max) {
                marcarError(tf, true);
                mostrarInfo("Validation", mensaje);
                return false;
            }
            marcarError(tf, false);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error validating max length.", ex);
            mostrarError("Error", "Unexpected validation error:\n" + safeMsg(ex));
            return false;
        }
    }

    private boolean validarEmail(TextField tf) {
        try {
            String email = texto(tf.getText());
            if (!email.matches("^.+@.+\\..+$")) {
                marcarError(tf, true);
                mostrarInfo("Validation", "Invalid email format. Example: example@example.com");
                return false;
            }
            marcarError(tf, false);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error validating email.", ex);
            mostrarError("Error", "Unexpected validation error:\n" + safeMsg(ex));
            return false;
        }
    }

    private boolean validarPassword(PasswordField pf) {
        try {
            String pw = texto(pf.getText());

            if (pw.isEmpty()) {
                pf.setStyle("-fx-border-color: red;");
                mostrarInfo("Validation", "Password is required.");
                return false;
            }
            if (pw.length() > 255) {
                pf.setStyle("-fx-border-color: red;");
                mostrarInfo("Validation", "Password cannot exceed 255 characters.");
                return false;
            }
            if (pw.length() < 8) {
                pf.setStyle("-fx-border-color: red;");
                mostrarInfo("Validation", "Password must be at least 8 characters.");
                return false;
            }

            pf.setStyle(null);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error validating password.", ex);
            mostrarError("Error", "Unexpected validation error:\n" + safeMsg(ex));
            return false;
        }
    }

    private boolean validarZip(TextField tf) {
        try {
            String zip = texto(tf.getText());

            if (!zip.matches("\\d+")) {
                marcarError(tf, true);
                mostrarInfo("Validation", "ZIP must be numeric.");
                return false;
            }
            if (zip.length() > 10) {
                marcarError(tf, true);
                mostrarInfo("Validation", "ZIP must contain at most 10 digits.");
                return false;
            }

            marcarError(tf, false);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error validating ZIP.", ex);
            mostrarError("Error", "Unexpected validation error:\n" + safeMsg(ex));
            return false;
        }
    }

    private boolean validarPhone(TextField tf) {
        try {
            String ph = texto(tf.getText());

            if (!ph.matches("\\d+")) {
                marcarError(tf, true);
                mostrarInfo("Validation", "Phone must be numeric.");
                return false;
            }
            if (!ph.matches("\\d{1,19}")) {
                marcarError(tf, true);
                mostrarInfo("Validation", "Phone must contain at most 19 digits.");
                return false;
            }

            marcarError(tf, false);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error validating phone.", ex);
            mostrarError("Error", "Unexpected validation error:\n" + safeMsg(ex));
            return false;
        }
    }

    // ========================= REST ERROR HANDLING =========================
    /**
     * Centraliza mensajes de error según el tipo de excepción REST.
     */
    private void manejarError(String accion, Throwable ex) {
        LOGGER.log(Level.WARNING, "Error during " + accion + ".", ex);

        if (ex instanceof NotFoundException) {
            mostrarInfo("Not found", "Customer does not exist.");
        } else if (ex instanceof InternalServerErrorException) {
            mostrarError("Server", "Internal server error during " + accion + ".");
        } else if (ex instanceof ClientErrorException) {
            mostrarError(
                    "REST",
                    "REST error during " + accion + ":\n" + safeMsg(ex)
                    + "\n\nIf the server requires additional fields, review the Customer model."
            );
        } else {
            mostrarError("Error", "Unexpected error during " + accion + ":\n" + safeMsg(ex));
        }
    }

    // ========================= NAVIGATION =========================
    /**
     * Cierra la pantalla actual volviendo al SignIn.
     */
    private void cerrar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CRUD_Project/ui/SignIn.fxml"));
            Parent root = loader.load();

            SignInController controller = loader.getController();

            Stage currentStage = (Stage) btExit.getScene().getWindow();
            controller.init(currentStage);

            currentStage.setScene(new Scene(root));
            currentStage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading SignIn view.", e);
            mostrarError("Navigation error", "Could not load the login window.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected navigation error.", e);
            mostrarError("Navigation error", "Unexpected error while navigating:\n" + safeMsg(e));
        }
    }

    // ========================= UI HELPERS =========================
    /**
     * Muestra un Alert informativo con título y mensaje.
     */
    private void mostrarInfo(String titulo, String msg) {
        try {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setTitle(titulo);
            a.setHeaderText(null);
            a.showAndWait();
        } catch (Exception ex) {
            // Si por algún motivo el Alert falla, al menos se loguea
            LOGGER.log(Level.WARNING, "Could not show info dialog: " + titulo, ex);
        }
    }

    /**
     * Muestra un Alert de error con título y mensaje.
     */
    private void mostrarError(String titulo, String msg) {
        try {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setTitle(titulo);
            a.setHeaderText(null);
            a.showAndWait();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Could not show error dialog: " + titulo, ex);
        }
    }

    /**
     * Pone borde rojo si error=true, o limpia estilo si error=false.
     */
    private void marcarError(TextField tf, boolean error) {
        try {
            tf.setStyle(error ? "-fx-border-color: red;" : null);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Could not set field style.", ex);
        }
    }

    /**
     * Comprueba si un string es un número entero positivo (solo dígitos) y no
     * vacío.
     */
    private boolean esNumero(String s) {
        try {
            return s != null && !s.trim().isEmpty() && s.trim().matches("\\d+");
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Error checking numeric string.", ex);
            return false;
        }
    }

    /**
     * Normaliza texto: si null devuelve "", si no hace trim().
     */
    private String texto(String s) {
        return (s == null) ? "" : s.trim();
    }

    /**
     * Ejecuta una acción encapsulada en try/catch para que la UI no se rompa
     * por excepciones.
     */
    private void runSafe(String actionName, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unhandled error during " + actionName + ".", ex);
            mostrarError("Error", "Unexpected error:\n" + safeMsg(ex));
        }
    }

    /**
     * Evita mensajes null y devuelve un texto seguro para mostrar.
     */
    private String safeMsg(Throwable ex) {
        if (ex == null) {
            return "Unknown error.";
        }
        String m = ex.getMessage();
        return (m == null || m.trim().isEmpty()) ? ex.getClass().getSimpleName() : m.trim();
    }
}
