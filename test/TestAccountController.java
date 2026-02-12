import CRUD_Project.ui.SignInController;
import CRUD_Project.model.AccountType;
import CRUD_Project.model.Account;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.TextInputControlMatchers.hasText;


/**
 * Clase de pruebas UI para la gestión de cuentas (AccountController).
 * <p>
 * Cobertura Completa:
 * 1. Validaciones (Campos vacíos, Texto en numéricos, numeros negativos).
 * 2. CRUD básico (Crear, Leer, Actualizar, Borrar).
 * 3. Navegación (Botones y Menús).
 * 4. Integridad Referencial (No borrar si hay movimientos).
 * </p>
 * @author Daniel López López
 * @fixme Los métodos de test presentados son insuficientes.
 * @fixme Crear sendos métodos de test para Read,Create,Update y Delete sobre la tabla de Cuentas que verifiquen sobre los items de la tabla cada caso de uso.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAccountController extends ApplicationTest {

    /**
     * Inicializa la aplicación cargando la vista de inicio de sesión.
     * @param stage Escenario principal de la aplicación.
     * @throws Exception Si ocurre un error al cargar el FXML.
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/CRUD_Project/ui/SignIn.fxml"));
        Parent root = loader.load();
        SignInController controller = loader.getController();
        controller.init(stage);
        stage.setScene(new Scene(root));
        stage.show();
    }


    //TESTS

    /**
     * TEST 01: Validaciones de Formulario.
     * Verifica que el sistema rechaza: campos vacíos, texto en campos numéricos y números negativos.
     */
    @Test
    @Ignore
    public void test_ValidacionesFormulario() {
        System.out.println("TEST 1: Validaciones");
        login(); 

        // Campos vacíos
        clickOn("#btCreate");
        esperar(1); 
        verifyThat(".dialog-pane", isVisible()); // Alerta esperada
        push(KeyCode.ENTER); 
        
        // Formato incorrecto (Texto en campos numéricos)
        clickOn("#tfDescription").write("Cuenta Error Texto");
        ComboBox<AccountType> cbType = lookup("#cbType").queryComboBox();
        interact(() -> cbType.getSelectionModel().select(AccountType.CREDIT));
        
        doubleClickOn("#tfCreditLine").write("TEXTO");
        doubleClickOn("#tfBeginBalance").write("MIL");
        
        clickOn("#btCreate");
        esperar(1); 
        verifyThat(".dialog-pane", isVisible()); // Alerta esperada (Excepción NumberFormat)
        push(KeyCode.ENTER); 

        // Números Negativos
        doubleClickOn("#tfDescription").write("Cuenta Error Negativo");
        doubleClickOn("#tfCreditLine").write("-100");
        doubleClickOn("#tfBeginBalance").write("-50");

        clickOn("#btCreate");
        esperar(1);
        verifyThat(".dialog-pane", isVisible()); // Alerta esperada (Validación lógica)
        push(KeyCode.ENTER);
        
        System.out.println("   -> Validaciones (Vacío, Texto y Negativos) OK.");
    }
    
    @Test
    @Ignore
    public void test_Read() {
        clickOn("#tfUsername").write("jsmith@enterprise.net");
        clickOn("#pfPassword").write("abcd*1234");
        clickOn("#bLogIn");
        
        verifyThat("#tbAccounts", isVisible());
        TableView<?> table = lookup("#tbAccounts").query();
        ObservableList<?> items = table.getItems();
        assertFalse("La tabla debería tener datos", items.isEmpty());
        for (Object item : items) {
            assertEquals(
                "El item de la tabla no es un Account",
                true,
                item instanceof Account
            );
        }
    }
    
    @Test
    @Ignore
    public void test_Create() {
        clickOn("#tfUsername").write("jsmith@enterprise.net");
        clickOn("#pfPassword").write("abcd*1234");
        clickOn("#bLogIn");
        verifyThat("#tbAccounts", isVisible());
        
        TableView<Account> tabla = lookup("#tbAccounts").queryTableView();
        int filasAntes = tabla.getItems().size();
        String descTest = "Nueva Cuenta Test " + System.currentTimeMillis();

        clickOn("#tfDescription").write(descTest);
        ComboBox<AccountType> cbType = lookup("#cbType").queryComboBox();
        interact(() -> cbType.getSelectionModel().select(AccountType.CREDIT));
        doubleClickOn("#tfCreditLine").write("500.0");
        doubleClickOn("#tfBeginBalance").write("100.0");

        clickOn("#btCreate");
        verifyThat(".dialog-pane", isVisible());
        push(KeyCode.ENTER);

        assertEquals("La tabla debería tener una fila más", filasAntes + 1, tabla.getItems().size());

        Account nueva = tabla.getItems().get(tabla.getItems().size() - 1);
        assertEquals("La descripción debe coincidir", descTest, nueva.getDescription());
        assertEquals("El saldo inicial debe ser 100.0", Double.valueOf(100.0), nueva.getBalance());
        assertNotNull("El ID no debe ser null", nueva.getId());
        assertEquals("El tipo debe ser CREDIT", AccountType.CREDIT, nueva.getType());
        assertEquals("La línea de crédito debe ser 500.0", Double.valueOf(500.0), nueva.getCreditLine());
        assertNotNull("La cuenta no debe ser null", nueva);
    }
    
    @Test
    @Ignore
    public void test_Update() {

        clickOn("#tfUsername").write("jsmith@enterprise.net");
        clickOn("#pfPassword").write("abcd*1234");
        clickOn("#bLogIn");

        verifyThat("#tbAccounts", isVisible());

        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        assertFalse("La tabla no debería estar vacía", table.getItems().isEmpty());

        Account original = table.getItems().stream()
                .filter(a -> a.getType() == AccountType.CREDIT)
                .findFirst()
                .orElse(null);

        assertNotNull(
            "No hay cuentas de crédito para probar UPDATE",
            original
        );

        interact(() -> table.getSelectionModel().select(original));

        String nuevaDesc = "Update_Desc_" + System.currentTimeMillis();
        String nuevaLine = "200.0";

        clickOn("#tfDescription");
        push(KeyCode.CONTROL, KeyCode.A);
        write(nuevaDesc);

        clickOn("#tfCreditLine");
        push(KeyCode.CONTROL, KeyCode.A);
        write(nuevaLine);

        clickOn("#btUpdate");

        verifyThat(".dialog-pane", isVisible());
        push(KeyCode.ENTER);

        Account updated = table.getItems().stream()
                .filter(a -> a.getId().equals(original.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull("La cuenta actualizada no existe en la tabla", updated);
        assertEquals("La descripción no se actualizó", nuevaDesc, updated.getDescription());
        assertEquals("La línea de crédito no se actualizó",
                Double.valueOf(nuevaLine), updated.getCreditLine());
    }
    
    @Test
    @Ignore
    public void test_Delete_Sin_Mov() {

        clickOn("#tfUsername").write("jsmith@enterprise.net");
        clickOn("#pfPassword").write("abcd*1234");
        clickOn("#bLogIn");
        verifyThat("#tbAccounts", isVisible());

        TableView<Account> table = lookup("#tbAccounts").queryTableView();

        assertTrue("La tabla no tiene cuentas para borrar",
                table.getItems().size() > 0);

        int rowsBefore = table.getItems().size();

        interact(() -> {
            int last = table.getItems().size() - 1;
            table.scrollTo(last);
            table.getSelectionModel().select(last);
        });

        clickOn("#btDelete");

        verifyThat(".dialog-pane", isVisible());
        push(KeyCode.ENTER);

        verifyThat(".dialog-pane", isVisible());

        push(KeyCode.ENTER);

        assertEquals("La cuenta no se eliminó de la tabla",
                rowsBefore - 1,
                table.getItems().size());
    }
    
    @Test
    @Ignore
    public void test_Delete_Con_Mov() {

        clickOn("#tfUsername").write("jsmith@enterprise.net");
        clickOn("#pfPassword").write("abcd*1234");
        clickOn("#bLogIn");
        verifyThat("#tbAccounts", isVisible());

        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        assertFalse("La tabla debería tener cuentas", table.getItems().isEmpty());

        Account conMovimientos = table.getItems().stream()
                .filter(a -> a.getMovements() != null && !a.getMovements().isEmpty())
                .findFirst()
                .orElse(null);

        assertNotNull(
            "No hay cuentas con movimientos para probar el caso",
            conMovimientos
        );

        int rowsBefore = table.getItems().size();
        Long idCuenta = conMovimientos.getId();

        interact(() -> table.getSelectionModel().select(conMovimientos));

        clickOn("#btDelete");

        verifyThat(".dialog-pane", isVisible());
        push(KeyCode.ENTER);

        assertEquals(
            "La tabla no debería cambiar al intentar borrar una cuenta con movimientos",
            rowsBefore,
            table.getItems().size()
        );
        assertTrue(
            "La cuenta con movimientos no debería haberse eliminado",
            table.getItems().stream().anyMatch(a -> a.getId().equals(idCuenta))
        );
    }
    
    
    @Test
    @Ignore
    public void test_Delete_Con_Y_Sin_Movimientos() {

        clickOn("#tfUsername").write("jsmith@enterprise.net");
        clickOn("#pfPassword").write("abcd*1234");
        clickOn("#bLogIn");
        verifyThat("#tbAccounts", isVisible());

        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        assertFalse("La tabla debería tener cuentas", table.getItems().isEmpty());

        Account cuentaSinMov = table.getItems().stream()
                .filter(a -> a.getMovements() == null || a.getMovements().isEmpty())
                .findFirst()
                .orElse(null);

        assertNotNull("No hay cuentas sin movimientos para probar", cuentaSinMov);

        int rowsBefore = table.getItems().size();

        interact(() -> table.getSelectionModel().select(cuentaSinMov));
        clickOn("#btDelete");
        verifyThat(".dialog-pane", isVisible());
        push(KeyCode.ENTER);
        push(KeyCode.ENTER);

        assertEquals("La cuenta sin movimientos no se eliminó",
                rowsBefore - 1,
                table.getItems().size());

        Account cuentaConMov = table.getItems().stream()
                .filter(a -> a.getMovements() != null && !a.getMovements().isEmpty())
                .findFirst()
                .orElse(null);

        assertNotNull("No hay cuentas con movimientos para probar", cuentaConMov);

        rowsBefore = table.getItems().size();
        Long idCuenta = cuentaConMov.getId();

        interact(() -> table.getSelectionModel().select(cuentaConMov));
        clickOn("#btDelete");
        verifyThat(".dialog-pane", isVisible());
        push(KeyCode.ENTER);

        assertEquals("La tabla no debería cambiar al intentar borrar cuenta con movimientos",
                rowsBefore,
                table.getItems().size());

        assertTrue("La cuenta con movimientos no debería haberse eliminado",
                table.getItems().stream().anyMatch(a -> a.getId().equals(idCuenta)));
    }

    /**
     * TEST 04: Navegación.
     * Verifica el acceso a la vista de movimientos mediante botón y menú contextual.
     */
    @Test
    @Ignore
    public void test_NavegarMovimientos() {
        System.out.println("TEST 4: Navegación Movimientos");
        clickOn("#tfUsername").write("jsmith@enterprise.net");
        clickOn("#pfPassword").write("abcd*1234");
        clickOn("#bLogIn");
        verifyThat("#tbAccounts", isVisible());
        esperar(2);
        
        crearCuentaAuxiliar("MOV_" + System.currentTimeMillis(), "1000");

        // Botón
        seleccionarUltimaFila();
        clickOn("#btViewMovements"); 
        esperar(2); 
        push(KeyCode.ESCAPE); 
        esperar(1);

        // Menú Error (Sin selección)
        TableView<Account> tabla = lookup("#tbAccounts").queryTableView();
        interact(() -> tabla.getSelectionModel().clearSelection());
        
        clickOn("#menuActions");     
        clickOn("#miViewMovements"); 
        esperar(1); 
        verifyThat(".dialog-pane", isVisible()); 
        push(KeyCode.ENTER); 
        
        // Menú Éxito
        seleccionarUltimaFila();
        clickOn("#menuActions");
        clickOn("#miViewMovements");
        esperar(2);
        push(KeyCode.ESCAPE); 
        esperar(1);
        
        // Limpieza
        seleccionarUltimaFila();
        borrarCuentaSeleccionada();
        
        System.out.println("   -> Navegación OK.");
    }

    /**
     * TEST 05: Logout.
     * Verifica el cierre de sesión y el retorno a la pantalla inicial.
     */
    @Test
    @Ignore
    public void test_LogOut() {
        System.out.println("TEST 5: Log Out");
        login();

        clickOn("#menuActions");
        esperar(1);
        clickOn("#miLogOut");
        
        esperar(1);
        verifyThat("#bLogIn", isVisible());
        
        System.out.println("   -> Logout OK.");
    }


    // MÉTODOS AUXILIARES

    /**
     * Pausa la ejecución del hilo actual.
     * @param segundos Tiempo de espera en segundos.
     */
    private void esperar(int segundos) {
        try { Thread.sleep(segundos * 1000); } catch (InterruptedException e) {}
    }

    /**
     * Realiza el proceso de inicio de sesión con credenciales predefinidas.
     */
    private void login() {
        esperar(1);
        clickOn("#tfUsername").write("jsmith@enterprise.net");
        clickOn("#pfPassword").write("abcd*1234");
        clickOn("#bLogIn");
        verifyThat("#tbAccounts", isVisible());
        esperar(2);
    }

    /**
     * Helper para crear una cuenta auxiliar de tipo CREDIT.
     * @param nombre Descripción de la cuenta.
     * @param credito Límite de crédito.
     */
    private void crearCuentaAuxiliar(String nombre, String credito) {
        clickOn("#tfDescription");
        push(KeyCode.CONTROL, KeyCode.A); 
        push(KeyCode.BACK_SPACE);
        write(nombre);
        
        ComboBox<AccountType> cbType = lookup("#cbType").queryComboBox();
        interact(() -> cbType.getSelectionModel().select(AccountType.CREDIT));
        
        doubleClickOn("#tfCreditLine").write(credito);
        doubleClickOn("#tfBeginBalance").write("0");
        
        clickOn("#btCreate");
        esperar(1); 
        verifyThat(".dialog-pane", isVisible()); 
        push(KeyCode.ENTER); esperar(2);
    }

    /**
     * Selecciona la última fila de la tabla de cuentas.
     * Útil tras la creación de nuevos registros.
     */
    private void seleccionarUltimaFila() {
        TableView<Account> tabla = lookup("#tbAccounts").queryTableView();
        interact(() -> {
            int total = tabla.getItems().size();
            if (total > 0) {
                int ultimo = total - 1;
                tabla.scrollTo(ultimo);
                tabla.getSelectionModel().select(ultimo);
                tabla.requestFocus();
            }
        });
        esperar(1);
    }

    /**
     * Elimina la cuenta actualmente seleccionada en la tabla.
     * Maneja las confirmaciones de diálogo esperadas.
     */
    private void borrarCuentaSeleccionada() {
        clickOn("#btDelete");
        esperar(1); 
        verifyThat(".dialog-pane", isVisible()); // ¿Seguro?
        push(KeyCode.ENTER); 
        esperar(1); 
        verifyThat(".dialog-pane", isVisible()); // Borrado OK
        push(KeyCode.ENTER); 
    }

}