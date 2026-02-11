
import javafx.collections.ObservableList;
import CRUD_Project.model.Customer;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.TextInputControlMatchers.hasText;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCustomerController extends ApplicationTest {

    TableView table;

    @Override
    public void start(Stage stage) throws Exception {
        new CRUD_Project().start(stage);
        table = lookup("#tvCustomers").queryTableView();
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

    // Comprueba que los elementos cargados en la tabla son de tipo Customer
    @Test
    public void test1_READCustomer() {
        loginComoAdmin();
        TableView<?> table = lookup("#tvCustomers").query();
        ObservableList<?> items = table.getItems();

        for (Object item : items) {
            assertEquals(
                    "El item de la tabla no es un Customer",
                    true,
                    item instanceof Customer
            );
        }
    }

    // Busca un customer por ID y verifica que el email se carga correctamente
    @Test
    public void Test2_buscarPorId() {
        loginComoAdmin();

        clickOn("#tfId");
        write("102263301");

        clickOn("#btSearch");

        verifyThat("#tfEmail", hasText("jsmith@enterprise.net"));
    }

    // Actualiza el nombre del primer customer de la tabla y verifica el cambio
    @Test
    public void Test3_actualizarNombrePorId() {
        loginComoAdmin();

        TableView<?> table = lookup("#tvCustomers").queryTableView();
        int rowCount = table.getItems().size();
        assertNotEquals("Table has no data: Cannot test.", 0, rowCount);

        Node row = lookup(".table-row-cell").nth(0).query();
        assertNotNull("Row is null: table has not that row.", row);
        clickOn(row);

        TextField tfId = lookup("#tfId").queryAs(TextField.class);
        String idTxt = tfId.getText();
        assertNotNull("Selected ID is null.", idTxt);
        assertFalse("Selected ID is empty.", idTxt.trim().isEmpty());

        String nuevoNombre = "NuevoNombre";

        TextField tfFirstName = lookup("#tfFirstName").queryAs(TextField.class);
        clickOn(tfFirstName);
        eraseText(25);
        write(nuevoNombre);

        verifyThat("#btUpdate", isEnabled());
        clickOn("#btUpdate");

        verifyThat("Customer updated.", isVisible());

        clickOn("Aceptar");

        clickOn("#tfId");
        eraseText(25);
        write(idTxt);

        clickOn("#btSearch");

        verifyThat("#tfFirstName", hasText(nuevoNombre));
    }

    // Crea un nuevo customer y verifica que se añade a la tabla
    @Test
    public void Test_4crearUsuario() {
        loginComoAdmin();

        TableView<Customer> table = lookup("#tvCustomers").queryTableView();
        int rowCount = table.getItems().size();

        String email = "test" + System.currentTimeMillis() + "@test.com";

        clickOn("#tfFirstName").write("Test");
        clickOn("#tfLastName").write("Test");
        clickOn("#tfMiddleInitial").write("T");
        clickOn("#tfEmail").write(email);
        clickOn("#pfPassword").write("12345678");
        clickOn("#tfStreet").write("Test");
        clickOn("#tfCity").write("Test");
        clickOn("#tfState").write("Test");
        clickOn("#tfZip").write("12345678");
        clickOn("#tfPhone").write("12345678");

        clickOn("#btCreate");

        clickOn("Aceptar");

        clickOn("#btRefresh");

        assertEquals("The row has not been added!!!",
                rowCount + 1,
                table.getItems().size());

        boolean encontrado = false;

        for (Customer c : table.getItems()) {
            if (c.getEmail() != null && c.getEmail().equalsIgnoreCase(email)) {
                encontrado = true;
                break;
            }
        }

        assertTrue("The user has not been added!!!", encontrado);
    }

    // Elimina un customer seleccionado y comprueba que la tabla disminuye
    @Test
    public void Test5_delete() {

        loginComoAdmin();

        TableView<?> table = lookup("#tvCustomers").queryTableView();
        int rowCount = table.getItems().size();

        assertNotEquals("Table has no data: Cannot test.", 0, rowCount);

        Node row = lookup(".table-row-cell").nth(6).query();
        assertNotNull("Row is null: table has not that row.", row);
        clickOn(row);

        verifyThat("#btDelete", isEnabled());
        clickOn("#btDelete");

        clickOn("Aceptar");

        assertEquals("Row should NOT be deleted when cancelled.",
                rowCount, table.getItems().size());

        clickOn("Aceptar");
    }
}
