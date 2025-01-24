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
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import model.Stats;
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
    private BorderPane borderPane;
    private PieChart pieChart;
    private ObservableList<PieChart.Data> pieChartData;
    
    @FXML
    private void handleButtonAction(ActionEvent event) {
        if (isFirstRun == true) {
            server.start();
            button.setText("Stop Server");
            isFirstRun = false;
            isRunning = true;
            showPieChart();
        } else if(isRunning){
            server.suspend();
            button.setText("start Server");
            isRunning = false;
        } else if(isRunning == false){
            server.resume();
            button.setText("stop Server");
            isRunning = true;
            showPieChart();
        }
    } 
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        server = new Server();
        ClientHandler.setController(this);
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

    public void showPieChart() {
        try {
            Stats stats = UsersDao.getChartStats();

            if (pieChart == null) {

                pieChartData = FXCollections.observableArrayList(
                        new PieChart.Data("Online: " + stats.getOnlinePlayers(), stats.getOnlinePlayers()),
                        new PieChart.Data("Offline: " + stats.getOfflinePlayers(), stats.getOfflinePlayers())
                );
                borderPane.getStylesheets().add(getClass().getResource("PieChartStyle.css").toExternalForm());
                pieChart = new PieChart(pieChartData);
                pieChart.setClockwise(true);
                pieChart.setLabelLineLength(50);
                pieChart.setLabelsVisible(true);
                pieChart.setStartAngle(180);
                borderPane.setCenter(pieChart);
            } else {
                Platform.runLater(() -> {
                    pieChartData.get(0).setPieValue(stats.getOnlinePlayers());
                    pieChartData.get(1).setPieValue(stats.getOfflinePlayers());
                    pieChartData.get(0).setName("Online: " + stats.getOnlinePlayers());
                    pieChartData.get(1).setName("Offline: " + stats.getOfflinePlayers());
                    pieChart.setTitle("Total Players : " + stats.getTotalPlayers());
                });
            }
        } catch (SQLException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
