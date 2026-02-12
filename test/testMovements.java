/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import CRUD_Project.model.Movement;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.matcher.base.NodeMatchers.isFocused;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
//Para poder usar las flechas y moverse en el checkbox
import javafx.scene.input.KeyCode;
import org.junit.After;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Ignore;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.testfx.api.FxToolkit;



/**
 *
 * @author imad
 * @fixme Los métodos de test presentados son insuficientes.
 * @fixme Crear sendos métodos de test para Read,Create y Delete (último movimiento) sobre la tabla de Movements que verifiquen sobre los items de la tabla cada caso de uso.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testMovements extends ApplicationTest {

    private Stage stage; // variable de instancia

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;  // guardamos el stage
        new CRUD_Project().start(stage);
    }

    @Before
    public void test1_Navigate_Login_To_Movements() {
        clickOn("#tfUsername");
        write("jsmith@enterprise.net");
        
        clickOn("#pfPassword");
        write("abcd*1234");
        
        clickOn("#bLogIn");

        clickOn("STANDARD");

        clickOn("#btViewMovements");

        verifyThat("#tvMovements", isVisible());
    }

    @After
    public void closeStage() throws Exception {
        FxToolkit.cleanupStages(); // Cierra todos los Stage abiertos, cada test es independiente y no pueden depender todos del mismo stage
    }
    @Ignore
    @Test
    public void test2_VerifyFocus_On_AmountField() {
        verifyThat("#tfAmount", isFocused());
    }
    
    //Recorrer coleciones(foreach o clase.stream().foreach()) para preguntar si en la tabla con un asserto, los datos son tipo movimiento
    //Coger los elementos de las tablas y preguntar si es un movimiento
    //Delete controlar q objeto estas seleccionando de la tabla estas borrando,movimiento selecciona el ultimo por fecha, coger eso y comprobar si ese objeto en concreto esta en la tabla
    @Ignore
    @Test
    public void test3_READMovements(){

        TableView<Movement> table = lookup("#tvMovements").query();
        ObservableList<Movement> items = table.getItems();

        for (Object item : items) {
            assertEquals(
                "El item de la tabla no es un Movement",
                true,
                item instanceof Movement
            );
        }
    }
    @Ignore
    @Test
    public void test4_CREATE_Deposit_Movement() {

        TableView<Movement> table = lookup("#tvMovements").query();

        int initialSize = table.getItems().size();

        clickOn("#cbType"); 
        type(KeyCode.ENTER); // Deposit

        clickOn("#tfAmount");
        write("100");

        clickOn("#bCreateMovement");

        int newSize = table.getItems().size();

        assertEquals(
            "El número de filas debería aumentar en 1 tras crear un movimiento",
            initialSize + 1,
            newSize
        );

        // Obtener el último movimiento según timestamp (es la misma logica del controlador)
        Movement lastMovement = table.getItems()
            .stream()
            .max((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
            .orElse(null);

        assertNotNull(lastMovement);// Verifica que se ha creado el movimiento

        // Verificar datos del movimiento creado
        assertEquals(true, lastMovement.getAmount() == 100); // La cantidad es la misma
        assertEquals(true, lastMovement.getDescription().equalsIgnoreCase("DEPOSIT"));// Verifica que es un deposito

        verifyThat("#bUndoLastMovement", isEnabled());
    }

    @Ignore
    @Test
    public void test5_CREATE_Payment_Movement() {
        // Obtener tabla de movimientos
        TableView<Movement> table = lookup("#tvMovements").query();
        int initialSize = table.getItems().size();
        
        clickOn("#cbType"); 
        type(KeyCode.DOWN); 
        type(KeyCode.ENTER); // Payment

        // Introducir cantidad
        clickOn("#tfAmount");
        write("100");

        // Crear movimiento
        clickOn("#bCreateMovement");

        int newSize = table.getItems().size();
        
        assertEquals(
            "El número de filas debería aumentar en 1 tras crear un movimiento",
            initialSize + 1,
            newSize
        );

        // Verificar si aparece alerta
        if (lookup(".dialog-pane").tryQuery().isPresent()) {
            // Cerrar alerta
            type(KeyCode.ENTER); 
            // Verificar botón Undo deshabilitado
            verifyThat("#bUndoLastMovement", isDisabled()); 
        } else {
            // Obtener último movimiento según timestamp 
            Movement lastMovement = table.getItems()
                .stream()
                .max((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .orElse(null);

            // Verificar que se ha creado el movimiento
            assertNotNull(lastMovement);

            // Verificar datos del movimiento creado
            assertEquals(true, lastMovement.getAmount() == -100); // La cantidad es la misma
            assertEquals(true, lastMovement.getDescription().equalsIgnoreCase("PAYMENT"));// Verifica que es un deposito
            
            // Verificar que el botón Undo se habilita
            verifyThat("#bUndoLastMovement", isEnabled());

            // Deshacer para dejar el balance como estaba
            clickOn("#bUndoLastMovement");
            type(KeyCode.ENTER);
        }
    }
    @Ignore
    @Test
    public void test6_DELETE_Movement() {

        TableView<Movement> table = lookup("#tvMovements").query();
        int initialSize = table.getItems().size();

        // Creamos un movimiento para poder borrarlo despues
        clickOn("#tfAmount");
        write("100");
        clickOn("#bCreateMovement");

        // Obtenemos el movimiento recién creado según timestamp
        Movement lastMovement = table.getItems()
            .stream()
            .max((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
            .orElse(null);

        assertNotNull(lastMovement);

        // Hacemos Undo
        clickOn("#bUndoLastMovement");
        type(KeyCode.ENTER);

        // Verificamos que el movimiento se ha borrado
        boolean exists = table.getItems().stream()
            .anyMatch(m -> m.getId().equals(lastMovement.getId()));

        assertEquals(false, exists);

        // Verificamos tamaño de la tabla
        int newSize = table.getItems().size();
        assertEquals(initialSize, newSize);

        // Botón Undo deshabilitado
        verifyThat("#bUndoLastMovement", isDisabled());
    }

    @Ignore
    @Test
    public void test7_Verify_Invalid_Amount_Input() {//verificar cantidades invalidas
        clickOn("#tfAmount");
        write("gjgf");
        
        clickOn("#bCreateMovement");

        // Verificamos que la ventana de alerta es visible
        verifyThat(".dialog-pane", isVisible());
        
        type(KeyCode.ENTER);

    }
    
}