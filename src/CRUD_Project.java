
import CRUD_Project.ui.SignInController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CRUD_Project extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/CRUD_Project/ui/SignIn.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Sign in");
        stage.setResizable(false);

        SignInController controller = loader.getController();
        controller.init(stage); // solo lógica, NO escena

        stage.show();
    }
}
