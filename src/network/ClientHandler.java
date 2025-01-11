/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import database.UsersDao;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author zeyad_maamoun
 */
public class ClientHandler extends Thread {

    private String username;
    private String password;
    private boolean isPlaying;
    private DataInputStream ear;
    private DataOutputStream mouth;
    private Socket socket;
    private static Vector<ClientHandler> clients = new Vector<>();
    private static ArrayList<String> playersList = new ArrayList<>();

    public ClientHandler(Socket socket) {
        try {
            ear = new DataInputStream(socket.getInputStream());
            mouth = new DataOutputStream(socket.getOutputStream());
            this.socket = socket;
            clients.add(this);
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                String clientMsg = ear.readUTF();
                parseJsonCommand(clientMsg);
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
            case "playerResponse":
                System.out.println(jsonMessage.toString());
                playerResponsetHandler(jsonMessage);
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
                updatePlayerListForAll();
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
        playersList.add(this.username);
        JSONObject obj = new JSONObject();
        obj.put("command", "players_list");
        obj.put("list", playersList);
        try {
            for (int i = 0; i < clients.size(); i++) {
                //obj.put("list", playersList);
                clients.get(i).mouth.writeUTF(obj.toString());
            }
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
                    mouth.writeUTF(jsonMessage.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }
    
    private void playerResponsetHandler(JSONObject jsonMessage) {
        String toPlayer = jsonMessage.getString("toplayer");
        for (int i = 0; i < clients.size(); i++) {

            if (clients.get(i).username.equals(toPlayer)) {
                try {
                    mouth.writeUTF(jsonMessage.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    private void refuseRequestHandler() {
    }

}