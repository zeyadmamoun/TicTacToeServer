/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import alphaserver.FXMLDocumentController;
import database.UsersDao;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author zeyad_maamoun
 */
public class ClientHandler extends Thread {

    public static ClientHandler clientHandler;
    boolean isRunning = true;
    String username;
    int score;
    private String password;
    public boolean isPlaying = false;
    DataInputStream ear;
    DataOutputStream mouth;
    Socket socket;
    private static Vector<ClientHandler> clients = new Vector<>();
    private static ArrayList<String> playersList = new ArrayList<>();
    private boolean isClientLeft = false;
    public static boolean clientHandlerThread = true;
    static FXMLDocumentController controller;

    public ClientHandler(Socket socket) {
        try {
            ear = new DataInputStream(socket.getInputStream());
            mouth = new DataOutputStream(socket.getOutputStream());
            this.socket = socket;
            System.out.println("clinet is about to added in the list");
            clients.add(this);
            System.out.println("the player list : " + clients.size());
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setController(FXMLDocumentController newController) {
        controller = newController;
    }


    @Override
    public void run() {
        while (clientHandlerThread) {
            if (isClientLeft) {//by mohamed
                closingWithClient();
                break;

            }
            try {
                if (!isPlaying && ear.available() > 0) {
                    String clientMsg = ear.readUTF();
                    parseJsonCommand(clientMsg);
                }
//                String clientMsg = ear.readUTF();
                // parseJsonCommand(clientMsg);
            } catch (IOException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void parseJsonCommand(String controlMessage) {
        JSONObject jsonMessage = new JSONObject(controlMessage);
        switch (jsonMessage.getString("command")) {
            case "register":
                username = jsonMessage.getString("username");
                password = jsonMessage.getString("password");
                registerHandler(username, password);
                break;
            case "login":
                username = jsonMessage.getString("username");
                password = jsonMessage.getString("password");
                loginHandler(username, password);
                break;
            case "requestToPlay":
                requestToPlayHandler(jsonMessage);
                break;
            case "send_list":
                updatePlayerListForAll();
                break;
            case "send_player_score":
                updatePlayerScore();
                break;
            case "playerResponse":
                int response = jsonMessage.getInt("response");
                if (response == 1) {
                    playerResponseHandler(jsonMessage);
                } else {
                    playerResponsetHandler(jsonMessage);
                }
                break;
            case "move":
                System.out.println("test");
                break;
            case "closetoleave": //by mohamed
                playerWantToClose();
                System.out.println("Client want to leave");
                break;

            case "I'm_gone": {
                try {
                    //by mohamed
                    UsersDao.logout(username);
                    controller.showPieChart();
                } catch (SQLException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            isClientLeft = true;
            System.out.println("Client left the game");
            break;
        }
    }

    private void registerHandler(String userName, String password) {
        boolean status = false;
        JSONObject obj = new JSONObject();
        try {
            status = UsersDao.registerUser(userName, password, password);
            if (status == true) {
                int score = UsersDao.getUserScore(userName);
                obj.put("command", "register_response");
                obj.put("status", 1);
                obj.put("username", userName);
                obj.put("score", 0);
                mouth.writeUTF(obj.toString());
                controller.showPieChart();
            } else {
                obj.put("command", "register_response");
                obj.put("status", 0);
                obj.put("username", userName);
                obj.put("score", 0);
                mouth.writeUTF(obj.toString());
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loginHandler(String userName, String password) {
        boolean status = false;
        JSONObject obj = new JSONObject();
        try {
            status = UsersDao.login(userName, password);

            if (status == true) {
                int score = UsersDao.getUserScore(userName);
                obj.put("command", "login_response");
                obj.put("status", 1);
                obj.put("username", userName);
                obj.put("score", score);
                mouth.writeUTF(obj.toString());
                controller.showPieChart();
            } else {
                obj.put("command", "login_response");
                obj.put("status", 0);
                obj.put("username", userName);
                obj.put("score", 0);
                mouth.writeUTF(obj.toString());
            }

        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void updatePlayerListForAll() {
        Map<String, Integer> newplayerListMap = new HashMap<>();
        Map<String, Integer> playerListMap = new HashMap<>();
        try {
            playerListMap = UsersDao.getUserScores();
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        JSONObject obj = new JSONObject();
        obj.put("command", "players_list");
        playersList.clear();
        try {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).isPlaying == false) {
                    //playersList.add(clients.get(i).username);
                    for (Map.Entry<String, Integer> me : playerListMap.entrySet()) {

                        // Printing keys
                        if (clients.get(i).username.equals(me.getKey())) {
                            newplayerListMap.put(me.getKey(), me.getValue());
                        }

                    }
                }
            }
            obj.put("list", newplayerListMap);
            for (ClientHandler client : clients) {
                client.mouth.writeUTF(obj.toString());
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updatePlayerScore() {
        try {
            score = UsersDao.getUserScore(username);
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        JSONObject obj = new JSONObject();
        obj.put("command", "players_score");
        obj.put("score", score);
        try {
            mouth.writeUTF(obj.toString());
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    void directMessage(String msg, String username) {

        JSONObject obj = new JSONObject();
        obj.put("type", "message");
        obj.put("message", msg);
        obj.put("username", username);

        for (ClientHandler c : clients) {
            if (c.username.equals(username)) {
                try {
                    c.mouth.writeUTF(obj.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void requestToPlayHandler(JSONObject jsonMessage) {
        String toPlayer = jsonMessage.getString("player2");
        for (int i = 0; i < clients.size(); i++) {

            if (clients.get(i).username.equals(toPlayer)) {
                try {
                    clients.get(i).mouth.writeUTF(jsonMessage.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }
//accept 

    private void playerResponseHandler(JSONObject jsonMessage) {
        ClientHandler player1 = null;
        ClientHandler player2 = null;
        String toPlayer = jsonMessage.getString("toplayer");
        String fromplayer = jsonMessage.getString("fromplayer");
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).username.equals(toPlayer)) {
                player1 = clients.get(i);
                try {
                    clients.get(i).mouth.writeUTF(jsonMessage.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).username.equals(fromplayer)) {
                player2 = clients.get(i);
            }
        }
        if (player1 != null && player2 != null) {
            player1.isPlaying = true;
            player2.isPlaying = true;
            updatePlayerListForAll();
            new GameSession(player1, player2);
        }

    }
//reject

    private void playerResponsetHandler(JSONObject jsonMessage) {
        String toPlayer = jsonMessage.getString("toplayer");
        for (int i = 0; i < clients.size(); i++) {

            if (clients.get(i).username.equals(toPlayer)) {
                try {
                    clients.get(i).mouth.writeUTF(jsonMessage.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    //closing

    private void playerWantToClose() { //by mohamed

        JSONObject responeToClient = new JSONObject();
        responeToClient.put("command", "acceptclosing");
        playersList.remove(this.username);
        clients.remove(this);
        updatePlayerListForAll();
        try {
            mouth.writeUTF(responeToClient.toString());

        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void closingWithClient() { //by mohamed
        try {
            mouth.close();
            ear.close();
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void destroy() {
        try {
            clientHandlerThread = false;

            if (ear != null) {
                ear.close();
            }
            if (mouth != null) {
                mouth.close();
            }

            if (socket != null) {
                socket.close();
            }

            clients.remove(this);

            username = null;
            isPlaying = false;

            interrupt();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
