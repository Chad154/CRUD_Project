
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.TextInputControlMatchers.hasText;

public class TestCustomerController extends ApplicationTest {

    /**
     * Este método lo llama TestFX para levantar la aplicación JavaFX antes de
     * ejecutar los tests. Aquí se arranca tu aplicación real (CRUD_Project)
     * usando el Stage que proporciona el framework.
     */
    @Override
    public void start(Stage stage) throws Exception {
        new CRUD_Project().start(stage);
    }

    /**
     * Realiza el login en la pantalla de SignIn como usuario admin. Pasos: -
     * Click en el campo de username y escribe "admin" - Click en el campo de
     * password y escribe "admin" - Click en el botón de login
     */
    private void loginComoAdmin() {
        clickOn("#tfUsername");   // Selecciona el TextField de usuario por su fx:id
        write("admin");           // Escribe el usuario

        clickOn("#pfPassword");   // Selecciona el PasswordField por su fx:id
        write("admin");           // Escribe la contraseña

        clickOn("#bLogIn");       // Pulsa el botón de login
    }

    /**
     * Cierra un Alert si aparece. Se usa para que los tests no se queden
     * bloqueados esperando que el usuario cierre el diálogo.
     *
     * Busca primero un botón con texto "Aceptar" (típico en español), si no
     * existe busca "OK" (típico en inglés). Si lo encuentra, hace click para
     * cerrar el diálogo.
     */
    private void cerrarAlertSiExiste() {
        if (lookup("Aceptar").tryQuery().isPresent()) {
            clickOn("Aceptar");
        } else if (lookup("OK").tryQuery().isPresent()) {
            clickOn("OK");
        }
    }

    /**
     * Test: comprueba que el login como admin funciona. - Ejecuta
     * loginComoAdmin() - Verifica que en la siguiente pantalla exista el texto
     * "Customers CRUD" (esto indica que se cargó la vista principal del CRUD).
     */
    @Test
    public void loginComoAdminTest() {
        loginComoAdmin();
        verifyThat("Customers CRUD", isVisible());
    }

    /**
     * Test: busca un customer por ID y verifica que los datos se cargan en el
     * formulario. Pasos: - Login como admin - Escribe un ID concreto en el
     * TextField #tfId - Pulsa el botón de buscar #btSearch - Verifica que el
     * campo email (#tfEmail) tenga el texto esperado
     *
     * Nota: este test depende de que el customer con ese ID exista en el
     * servidor/bd y tenga el email "jsmith@enterprise.net".
     */
    @Test
    public void buscarPorId() {
        loginComoAdmin();

        clickOn("#tfId");         // Campo donde se mete el ID a buscar
        write("102263301");       // ID que se va a buscar

        clickOn("#btSearch");     // Botón de búsqueda

        // Comprueba que el email cargado en el TextField sea el esperado
        verifyThat("#tfEmail", hasText("jsmith@enterprise.net"));
    }

    /**
     * Test: crea un usuario (customer) nuevo desde el formulario. Pasos: -
     * Login como admin - Rellena todos los campos obligatorios del formulario -
     * Pulsa el botón Create (#btCreate) - Verifica que aparece un diálogo
     * (Alert) y que contiene el texto "Customer creado." - Cierra el Alert para
     * que el test no quede bloqueado - Pulsa Refresh para recargar la
     * tabla/lista
     *
     * Nota importante: Este test puede fallar si el email "Test@test.test1" ya
     * existe o si el servidor tiene reglas adicionales.
     */
    @Test
    public void crearUsuario() {
        loginComoAdmin();

        // Relleno de datos del customer
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

        // Ejecuta el create
        clickOn("#btCreate");

        // Verifica que sale un diálogo y el mensaje de éxito
        verifyThat(".dialog-pane", isVisible());
        verifyThat("Customer created.", isVisible());

        // Cierra el Alert
        cerrarAlertSiExiste();

        // Refresca la tabla/lista para que se vea el nuevo registro
        clickOn("#btRefresh");
    }

    /**
     * Test: actualiza el nombre de un customer existente usando su ID. Pasos: -
     * Login como admin - Introduce el ID y busca el customer - Va al campo
     * FirstName y escribe "NuevoNombre" - Pulsa Update (#btUpdate) - Verifica
     * que aparece un Alert con el texto "Customer actualizado." - Cierra el
     * Alert
     *
     * Nota: tal como está, este test escribe "NuevoNombre" al final del texto
     * actual porque no limpia el campo antes. Si el campo tiene "John",
     * quedaría "JohnNuevoNombre". Si quieres reemplazar el valor, deberías
     * hacer: doubleClickOn("#tfFirstName"); write("NuevoNombre"); o:
     * clickOn("#tfFirstName"); eraseText(n); write("NuevoNombre");
     */
    @Test
    public void actualizarNombrePorId() {
        loginComoAdmin();

        clickOn("#tfId");
        write("102263301");

        clickOn("#btSearch");

        clickOn("#tfFirstName");
        eraseText(15);
        write("NuevoNombre");

        clickOn("#btUpdate");

        verifyThat(".dialog-pane", isVisible());
        verifyThat("Customer updated.", isVisible());

        cerrarAlertSiExiste();
    }

}
