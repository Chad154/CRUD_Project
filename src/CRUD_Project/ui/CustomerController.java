package CRUD_Project.ui;

import CRUD_Project.logic.CustomerRESTClient;
import CRUD_Project.model.Customer;
import java.io.IOException;
import java.util.Arrays;
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
    @FXML private TableColumn<Customer, String> colMiddleInitial;
    @FXML private TableColumn<Customer, String> colStreet;
    @FXML private TableColumn<Customer, String> colState;
    @FXML private TableColumn<Customer, String> colZip;

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
        colMiddleInitial.setCellValueFactory(d ->
                new SimpleStringProperty(texto(d.getValue().getMiddleInitial())));
        colStreet.setCellValueFactory(d ->
                new SimpleStringProperty(texto(d.getValue().getStreet())));
        colState.setCellValueFactory(d ->
                new SimpleStringProperty(texto(d.getValue().getState())));
        colZip.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getZip() == null ? "" : d.getValue().getZip().toString()));
        colPhone.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPhone() == null ? "" : d.getValue().getPhone().toString()));

        tvCustomers.setItems(lista);

        tvCustomers.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                ponerEnFormulario(newV);
            }
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

    // ========================= LISTA =========================

    private void cargarLista() {
        CustomerRESTClient client = new CustomerRESTClient();
        try {
            Customer[] customers = client.findAll_XML(Customer[].class);
            lista.clear();
            if (customers != null) {
                lista.addAll(Arrays.asList(customers));
            }
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo cargar la lista:\n" + ex.getMessage());
        } finally {
            client.close();
        }
    }

    // ========================= SEARCH =========================

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
            if (c == null) {
                mostrarInfo("No encontrado", "No existe customer con ese ID.");
            } else {
                ponerEnFormulario(c);
            }
        } catch (NotFoundException ex) {
            mostrarInfo("No encontrado", "No existe customer con ese ID.");
        } catch (Exception ex) {
            mostrarError("Error", "Error buscando customer:\n" + ex);
        } finally {
            client.close();
        }
    }

    // ========================= CREATE =========================

    private void crear() {
        if (!validarFormulario(false)) return;

        // ✅ BLOQUEO EMAIL DUPLICADO (cliente-side)
        if (emailYaExiste(texto(tfEmail.getText()), null)) {
            marcarError(tfEmail, true);
            mostrarError("Error", "Error: el email ya existe");
            return;
        }
        marcarError(tfEmail, false);

        Customer c = construirCustomerDesdeFormulario(null);

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            client.create_XML(c);
            mostrarInfo("OK", "Customer creado.");
            limpiarFormulario();
            cargarLista();

        } catch (ForbiddenException ex) {
            // Si tu server devuelve 403
            mostrarError("Error", "Error: el email ya existe");

        } catch (InternalServerErrorException ex) {
            mostrarError("Servidor", "Error con el servidor al crear.");

        } catch (Exception ex) {
            manejarError("crear", ex);

        } finally {
            client.close();
        }
    }

    // ========================= UPDATE =========================

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

        // ✅ BLOQUEO EMAIL DUPLICADO EN UPDATE (ignorando mi propio ID)
        if (emailYaExiste(texto(tfEmail.getText()), id)) {
            marcarError(tfEmail, true);
            mostrarError("Error", "Error: el email ya existe");
            return;
        }
        marcarError(tfEmail, false);

        Customer c = construirCustomerDesdeFormulario(id);

        CustomerRESTClient client = new CustomerRESTClient();
        try {
            client.edit_XML(c);
            mostrarInfo("OK", "Customer actualizado.");
            cargarLista();

        } catch (ForbiddenException ex) {
            mostrarError("Error", "Error: el email ya existe");

        } catch (Exception ex) {
            manejarError("actualizar", ex);

        } finally {
            client.close();
        }
    }

    // ========================= DELETE =========================

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

    // ========================= EMAIL DUPLICADO =========================

    /**
     * Devuelve true si ya existe un customer con ese email.
     * Si ignoreId != null, ignora ese customer (para update).
     */
    private boolean emailYaExiste(String email, Long ignoreId) {
        if (email == null || email.trim().isEmpty()) return false;

        String target = email.trim().toLowerCase();

        // Si ya tienes lista cargada, úsala (rápido)
        if (!lista.isEmpty()) {
            for (Customer c : lista) {
                if (c == null) continue;
                if (ignoreId != null && c.getId() != null && c.getId().equals(ignoreId)) continue;

                String e = texto(c.getEmail()).toLowerCase();
                if (!e.isEmpty() && e.equals(target)) {
                    return true;
                }
            }
            return false;
        }

        // Si lista está vacía por lo que sea, consulta al servidor
        CustomerRESTClient client = new CustomerRESTClient();
        try {
            Customer[] customers = client.findAll_XML(Customer[].class);
            if (customers == null) return false;

            for (Customer c : customers) {
                if (c == null) continue;
                if (ignoreId != null && c.getId() != null && c.getId().equals(ignoreId)) continue;

                String e = texto(c.getEmail()).toLowerCase();
                if (!e.isEmpty() && e.equals(target)) {
                    return true;
                }
            }
            return false;

        } catch (Exception ex) {
            // Si falla la comprobación, no bloquees por falso positivo
            LOGGER.warning("No se pudo comprobar email duplicado: " + ex.getMessage());
            return false;
        } finally {
            client.close();
        }
    }

    // ========================= MAPEOS FORM <-> MODEL =========================

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
        pfPassword.setStyle(null);
    }

    // ========================= VALIDACIONES (IGUAL QUE SignUPController) =========================

    private boolean validarFormulario(boolean esUpdate) {

        if (!validarObligatorio(tfFirstName, "El nombre es obligatorio.")) return false;
        if (!validarObligatorio(tfLastName, "El apellido es obligatorio.")) return false;
        if (!validarObligatorio(tfMiddleInitial, "MiddleInitial es obligatorio.")) return false;
        if (!validarObligatorio(tfStreet, "La calle es obligatoria.")) return false;
        if (!validarObligatorio(tfCity, "La ciudad es obligatoria.")) return false;
        if (!validarObligatorio(tfState, "El estado es obligatorio.")) return false;
        if (!validarObligatorio(tfZip, "El ZIP es obligatorio.")) return false;
        if (!validarObligatorio(tfPhone, "El teléfono es obligatorio.")) return false;
        if (!validarObligatorio(tfEmail, "El email es obligatorio.")) return false;

        if (!validarMaxLen(tfFirstName, 255, "El nombre no puede contener más de 255 caracteres")) return false;
        if (!validarMaxLen(tfLastName, 255, "El apellido no puede contener más de 255 caracteres")) return false;
        if (!validarMaxLen(tfMiddleInitial, 255, "El apartado MiddleInitial no puede contener más de 255 caracteres")) return false;
        if (!validarMaxLen(tfStreet, 255, "La calle no puede contener más de 255 caracteres")) return false;
        if (!validarMaxLen(tfCity, 255, "La ciudad no puede contener más de 255 caracteres")) return false;
        if (!validarMaxLen(tfState, 255, "El estado no puede contener más de 255 caracteres")) return false;
        if (!validarMaxLen(tfEmail, 255, "El email no puede contener más de 255 caracteres")) return false;

        if (!validarZip(tfZip)) return false;
        if (!validarPhone(tfPhone)) return false;
        if (!validarEmail(tfEmail)) return false;
        if (!validarPassword(pfPassword)) return false;

        return true;
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

    private boolean validarMaxLen(TextField tf, int max, String mensaje) {
        String v = texto(tf.getText());
        if (v.length() > max) {
            marcarError(tf, true);
            mostrarInfo("Validación", mensaje);
            return false;
        }
        marcarError(tf, false);
        return true;
    }

    private boolean validarEmail(TextField tf) {
        String email = texto(tf.getText());
        if (!email.matches("^.+@.+\\..+$")) {
            marcarError(tf, true);
            mostrarInfo("Validación", "Tu email no es valida debe tener este formato ejemplo@ejemplo.ejemplo.");
            return false;
        }
        marcarError(tf, false);
        return true;
    }

    private boolean validarPassword(PasswordField pf) {
        String pw = texto(pf.getText());

        if (pw.isEmpty()) {
            pf.setStyle("-fx-border-color: red;");
            mostrarInfo("Validación", "La contraseña es obligatoria.");
            return false;
        }
        if (pw.length() > 255) {
            pf.setStyle("-fx-border-color: red;");
            mostrarInfo("Validación", "La contraseña no puede contener más de 255 caracteres");
            return false;
        }
        if (pw.length() < 8) {
            pf.setStyle("-fx-border-color: red;");
            mostrarInfo("Validación", "La contraseña debe tener al menos 8 caracteres.");
            return false;
        }

        pf.setStyle(null);
        return true;
    }

    private boolean validarZip(TextField tf) {
        String zip = texto(tf.getText());

        if (!zip.matches("\\d+")) {
            marcarError(tf, true);
            mostrarInfo("Validación", "El campo zip tienen que ser numeros");
            return false;
        }
        if (zip.length() > 10) {
            marcarError(tf, true);
            mostrarInfo("Validación", "El campo ZIP debe contener máximo 10 números.");
            return false;
        }

        marcarError(tf, false);
        return true;
    }

    private boolean validarPhone(TextField tf) {
        String ph = texto(tf.getText());

        if (!ph.matches("\\d+")) {
            marcarError(tf, true);
            mostrarInfo("Validación", "El campo telefono deben ser numeros.");
            return false;
        }
        if (!ph.matches("\\d{1,19}")) {
            marcarError(tf, true);
            mostrarInfo("Validación", "El campo Teléfono debe contener máximo 19 números.");
            return false;
        }

        marcarError(tf, false);
        return true;
    }

    // ========================= ERRORES REST =========================

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

    // ========================= NAVEGACIÓN =========================

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
            LOGGER.severe("Error al cargar la vista de Login: " + e.getMessage());
            mostrarError("Error de Navegación", "No se pudo encontrar la ventana de inicio de sesión.");
        }
    }

    // ========================= UI HELPERS =========================

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
