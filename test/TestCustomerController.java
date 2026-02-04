/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author chad
 */

import static java.util.function.Predicate.isEqual;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.*;
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

    @Test
    public void loginComoAdminTest() {
        clickOn("#tfUsername");
        write("admin");
        clickOn("#pfPassword");
        write("admin");
        clickOn("#bLogIn");
        verifyThat("Customers CRUD", isVisible());
    }

    @Test
    public void buscarPorId() {
        loginComoAdmin();
        clickOn("#tfId");
        write("5170024757244335422");
        clickOn("#btSearch");
        verifyThat("#tfEmail", hasText("Prueba@prueba.prueba"));
    }
    
    @Test
    public void crearUsuario(){
        loginComoAdmin();
        clickOn("#tfFirsName")
    }

}
