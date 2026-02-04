
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.TextInputControlMatchers.hasText;

public class TestCustomerController extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        new CRUD_Project().start(stage);
    }

    private void loginComoAdmin() {
        clickOn("#tfUsername");
        write("admin");
        clickOn("#pfPassword");
        write("admin");
        clickOn("#bLogIn");
    }

    private void cerrarAlertSiExiste() {
        if (lookup("Aceptar").tryQuery().isPresent()) {
            clickOn("Aceptar");
        } else if (lookup("OK").tryQuery().isPresent()) {
            clickOn("OK");
        }
    }

    @Test
    public void loginComoAdminTest() {
        loginComoAdmin();
        verifyThat("Customers CRUD", isVisible());
    }

    @Test
    public void buscarPorId() {
        loginComoAdmin();

        clickOn("#tfId");
        write("102263301");

        clickOn("#btSearch");

        verifyThat("#tfEmail", hasText("jsmith@enterprise.net"));
    }

    @Test
    public void crearUsuario() {
        loginComoAdmin();

        clickOn("#tfFirstName");
        write("Test");

        clickOn("#tfLastName");
        write("Test");

        clickOn("#tfMiddleInitial");
        write("T");

        clickOn("#tfEmail");
        write("Test@test.test1");

        clickOn("#pfPassword");
        write("12345678");

        clickOn("#tfStreet");
        write("Test");

        clickOn("#tfCity");
        write("Test");

        clickOn("#tfState");
        write("Test");

        clickOn("#tfZip");
        write("12345678");

        clickOn("#tfPhone");
        write("12345678");

        clickOn("#btCreate");

        verifyThat(".dialog-pane", isVisible());
        verifyThat("Customer creado.", isVisible());
        cerrarAlertSiExiste();

        clickOn("#btRefresh");
    }

    @Test
    public void actualizarNombrePorId() {
        loginComoAdmin();

        // 1) Buscar por ID fijo
        clickOn("#tfId");
        write("102263301");
        clickOn("#btSearch");

        // 2) Cambiar nombre (First Name)
        clickOn("#tfFirstName");
        write("NuevoNombre");

        // 3) Update
        clickOn("#btUpdate");

        // 4) Verificar mensaje de OK
        verifyThat(".dialog-pane", isVisible());
        verifyThat("Customer actualizado.", isVisible());
        cerrarAlertSiExiste();
    }

    @Test
    public void borrarCustomer() {
        loginComoAdmin();
    }
}
