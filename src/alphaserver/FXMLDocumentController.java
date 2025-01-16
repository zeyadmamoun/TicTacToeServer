/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alphaserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import network.ClientHandler;

/**
 *
 * @author zeyad_maamoun
 */
public class FXMLDocumentController implements Initializable {
    
    Server server;
    boolean isRunning = false;
    boolean isFirstRun = true;
    boolean isAcceptingClients=true;
    @FXML
    private Button button;
    
    @FXML
    private void handleButtonAction(ActionEvent event) {
        if (isFirstRun == true) {
            server.start();
            button.setText("Stop Server");
            isFirstRun = false;
            isRunning = true;
        } else if(isRunning){
            server.suspend();
            button.setText("start Server");
            isRunning = false;
        } else if(isRunning == false){
            server.resume();
            button.setText("stop Server");
            isRunning = true;
        }
    } 
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        server = new Server();
    }  
    
    class Server extends Thread {
        
        ServerSocket serverSocket;
        
        public Server() {
            try {
                serverSocket = new ServerSocket(5005);
            } catch (IOException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }

        @Override
        public void run() {
            while(isAcceptingClients)
            {     
                try {
                    Socket s = serverSocket.accept();
                    new ClientHandler(s);    //here should i call the registertion method or in the client handler.
                } catch (IOException ex) {
                    Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                }  
            }
        }
        
        
    }
    
}
