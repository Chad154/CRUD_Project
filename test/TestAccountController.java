import CRUD_Project.ui.SignInController;
import CRUD_Project.model.AccountType;
import CRUD_Project.model.Account;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.Test;
import org.junit.FixMethodOrder;
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
    public void test01_ValidacionesFormulario() {
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

    /**
     * TEST 02: Ciclo Standard.
     * Verifica la creación y el borrado de una cuenta de tipo Standard (sin línea de crédito).
     */
    @Test
    public void test02_CrearYBorrarStandard() {
        //FIXME borrar esta sentencia
        System.out.println("TEST 2: Cuenta STANDARD");
        login();

        String nombre = "STD_" + System.currentTimeMillis();
        
        // Crear
        clickOn("#tfDescription").write(nombre);
        ComboBox<AccountType> cbType = lookup("#cbType").queryComboBox();
        interact(() -> cbType.getSelectionModel().select(AccountType.STANDARD));
        doubleClickOn("#tfBeginBalance").write("100");
        
        clickOn("#btCreate");
        esperar(1); 
        verifyThat(".dialog-pane", isVisible()); 
        push(KeyCode.ENTER); esperar(2); 

        // Verificar
        seleccionarUltimaFila();
        verifyThat("#tfDescription", hasText(nombre));

        // Borrar
        borrarCuentaSeleccionada();
        
        System.out.println("   -> Standard OK.");
    }

    /**
     * TEST 03: Modificación.
     * Verifica la actualización de una cuenta y la persistencia de los cambios.
     */
    @Test
    public void test03_ModificarCuenta() {
        System.out.println("TEST 3: Modificar Cuenta");
        login();

        String nombreOriginal = "MOD_" + System.currentTimeMillis();
        crearCuentaAuxiliar(nombreOriginal, "500");

        // Modificar
        seleccionarUltimaFila();
        clickOn("#tfDescription");
        push(KeyCode.END); // Ir al final
        write("_UPDATED");
        
        // Guardar
        clickOn("#btUpdate");
        esperar(1); 
        verifyThat(".dialog-pane", isVisible()); 
        push(KeyCode.ENTER); esperar(2); 

        // Recargar para verificar persistencia real
        TableView<Account> tabla = lookup("#tbAccounts").queryTableView();
        interact(() -> {
            tabla.refresh();
            tabla.getSelectionModel().clearSelection();
        });
        
        seleccionarUltimaFila();
        
        // 4. Verificar cambio
        verifyThat("#tfDescription", hasText(nombreOriginal + "_UPDATED"));

        // 5. Limpieza
        borrarCuentaSeleccionada();
        System.out.println("   -> Modificación OK.");
    }

    /**
     * TEST 04: Navegación.
     * Verifica el acceso a la vista de movimientos mediante botón y menú contextual.
     */
    @Test
    public void test04_NavegacionMovimientos() {
        System.out.println("TEST 4: Navegación Movimientos");
        login();
        
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
    public void test05_LogOut() {
        System.out.println("TEST 5: Log Out");
        login();

        clickOn("#menuActions");
        esperar(1);
        clickOn("#miLogOut");
        
        esperar(1);
        verifyThat("#bLogIn", isVisible());
        
        System.out.println("   -> Logout OK.");
    }

    /**
     * TEST 06: Integridad Referencial.
     * Verifica que NO se puede borrar una cuenta si tiene movimientos.
     */
    @Test
    public void test06_ErrorBorrarConMovimientos() {
        System.out.println("TEST 6: Integridad (Bloqueo borrado)");
        login();
        
        // Crear Cuenta
        crearCuentaAuxiliar("CON_MOV_" + System.currentTimeMillis(), "0");
        seleccionarUltimaFila();

        // Entrar a Movimientos
        clickOn("#btViewMovements");
        esperar(2); 
        
        // Crear Movimiento (Para activar el bloqueo de borrado)
        clickOn("#tfAmount").write("50");      
        clickOn("#bCreateMovement");           
        esperar(1);

        // Salir
        push(KeyCode.ESCAPE);
        esperar(1);

        // Intentar Borrar
        seleccionarUltimaFila();
        clickOn("#btDelete");
        
        // Verificar Error
        // Esperamos una alerta de ERROR (el controlador detecta que la lista de movimientos no está vacía)
        esperar(1);
        verifyThat(".dialog-pane", isVisible());
        
        push(KeyCode.ENTER); 
        
        System.out.println("   -> Error de integridad verificado.");
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