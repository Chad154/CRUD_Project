/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;

/**
 *
 * @author imad
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testMovements extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        new CRUD_Project().start(stage);
    }

    @Test
    public void test1_Navigate_Login_To_Movements() {
        clickOn("#tfUsername");
        write("j@j.j");
        
        clickOn("#pfPassword");
        write("12345678");
        
        clickOn("#bLogIn");

        clickOn("STANDARD");

        clickOn("#btViewMovements");

        verifyThat("#tvMovements", isVisible());
    }
    
    @Test
    public void test2_VerifyFocus_On_AmountField() {
        verifyThat("#tfAmount", isFocused());
    }
    
    @Test
    public void test3_Create_Deposit_Movement() {
        clickOn("#cbType"); 
        //Directamente enter porque la primera opcion es deposit
        type(KeyCode.ENTER);
        
        clickOn("#tfAmount");
        write("100");
        
        clickOn("#bCreateMovement");
        //Verificamos que se habilita undomovement porque de haberse creado el movimiento, este se habilita para poder deshacerlo
        verifyThat("#bUndoLastMovement", isEnabled());
    }
    
    @Test
    public void test4_Undo_Last_Movement() {
        clickOn("#bUndoLastMovement");
        type(KeyCode.ENTER);
        verifyThat("#bUndoLastMovement", isDisabled());
    }
    
    @Test
    public void test5_Create_Payment_Movement() {
        clickOn("#cbType"); 
        //flecha abajo y clicamos en payment
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);
        
        clickOn("#tfAmount");
        write("100");
        
        clickOn("#bCreateMovement");
        
        if (lookup(".dialog-pane").tryQuery().isPresent()) {// Comprueba si existe una ventana de alerta
            //Cerrar la alerta
            type(KeyCode.ENTER); 
            //Verificamos que esta deshabilitado
            verifyThat("#bUndoLastMovement", isDisabled()); 
            
        } else {
            //Si no sale la alerta verificamos que esta habilitado
            verifyThat("#bUndoLastMovement", isEnabled());
            //Para dejar el balance de antes
            clickOn("#bUndoLastMovement");
            type(KeyCode.ENTER);
        }
    }
    @Test
    public void test6_Verify_Invalid_Amount_Input() {//verificar cantidades invalidas
        clickOn("#tfAmount");
        write("-100");
        
        clickOn("#bCreateMovement");

        // Verificamos que la ventana de alerta es visible
        verifyThat(".dialog-pane", isVisible());
        
        type(KeyCode.ENTER);

    }
    
}