/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alphaserver;

import database.UsersDao;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Vector;
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
    boolean isAcceptingClients = false;
    ServerSocket serverSocket;
    ClientHandler clientHandler;
    private static Vector<ClientHandler> clients = new Vector<>();
    @FXML
    private Button button;
    @FXML
    private void handleButtonAction(ActionEvent event) {
        if (!isRunning) {
            try {
                UsersDao.logoutAllUsers();
            } catch (SQLException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
            isAcceptingClients = true;
            ClientHandler.clientHandlerThread = true;
            server = new Server();
            server.start();
            button.setText("Stop Server");
            isRunning = true;
        } else {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
            // Stop the server
            isAcceptingClients = false;
            ClientHandler.clientHandlerThread = false;
            for (int i = 0; i < clients.size(); i++) {
                clients.get(i).destroy();
            }
            isRunning = false;
            server.interrupt();
            server = null;
            button.setText("Start Server");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    class Server extends Thread {

        public Server() {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                serverSocket = new ServerSocket(5005);
            } catch (IOException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        @Override
        public void run() {
            while (isAcceptingClients) {
                try {
                    Socket s = serverSocket.accept();
                    clients.add(new ClientHandler(s));
                } catch (SocketException se) {
                    break;
                } catch (IOException ex) {
                    Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
