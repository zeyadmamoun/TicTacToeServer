/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alphaserver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author zeyad_maamouna
 */
public class AlphaServer extends Application {

    FXMLDocumentController c;
//
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResourceAsStream("FXMLDocument.fxml"));
        c = loader.getController();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> {

            c.isAcceptingClients = false;
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
