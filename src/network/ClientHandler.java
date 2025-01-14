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
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author zeyad_maamoun
 */
public class ClientHandler extends Thread {

    boolean isRunning = true;
    String username;
    private String password;
    private boolean isPlaying;
    DataInputStream ear;
    DataOutputStream mouth;
    Socket socket;
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
        while (isRunning) {
            try {
                String clientMsg = ear.readUTF();
                if (!isPlaying) {
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
            player1.isRunning = false;
            player2.isRunning = false;
            player1.isPlaying = true;
            player2.isPlaying = true;
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
}

class GameSession extends Thread {

    char currentPlayer = 'X';
    boolean isPlayerOneTurn;
    char[][] board = new char[3][3];
    ClientHandler player1;
    ClientHandler player2;
    boolean isRunning = true;

    public GameSession(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1;
        this.player2 = player2;
        initializeGame();
        start();
    }

    @Override
    public void run() {
        try {
            JSONObject obj;
            // Initial notification to both players
            JSONObject startObj = new JSONObject();
            startObj.put("command", "start");
            player1.mouth.writeUTF(startObj.toString());
            while (isRunning) {
                // Handle game messages from both players
                if (player1.ear.available() > 0) {
                    String move = player1.ear.readUTF();
                    obj = new JSONObject(move);
                    int col = obj.getInt("col");
                    int row = obj.getInt("row");
                    board[row][col] = currentPlayer;
                    player2.mouth.writeUTF(move);
                    if (checkWinner()) {
                        if (currentPlayer == 'X') {
                            notifyPlayersSomeoneWon(player1, player2);
                            System.out.println("server says X won");
                        } else {
                            notifyPlayersSomeoneWon(player2, player1);
                            System.out.println("server says O won");
                            break;
                        }
                    } else if (isBoardFull()) {
                        notifyPlayersDraw();
                        System.out.println("server says board full");
                        break;
                    }
                    isPlayerOneTurn = !isPlayerOneTurn;
                    currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            if(board[i][j]!=' '){
                                System.out.println(board[i][j]);
                            }else{
                                System.out.println("-");
                            }
                        }
                    }
                    System.out.println("============");
                }

                if (player2.ear.available() > 0) {
                    String move = player2.ear.readUTF();

                    obj = new JSONObject(move);
                    int col = obj.getInt("col");
                    int row = obj.getInt("row");

                    board[row][col] = currentPlayer;
                    player1.mouth.writeUTF(move);

                    if (checkWinner()) {
                        if (currentPlayer == 'X') {
                            notifyPlayersSomeoneWon(player1, player2);
                            System.out.println("server says X won");
                        } else {
                            notifyPlayersSomeoneWon(player2, player1);
                            System.out.println("server says O won");
                            break;
                        }
                    } else if (isBoardFull()) {
                        notifyPlayersDraw();
                        System.out.println("server says board full");
                        break;
                    }
                    isPlayerOneTurn = !isPlayerOneTurn;
                    currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            if(board[i][j]!=' '){
                                System.out.println(board[i][j]);
                            }else{
                                System.out.println("-");
                            }
                        }
                    }
                    System.out.println("============");
                }

                Thread.sleep(50); // Small delay to prevent CPU overuse
            }

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(GameSession.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // Reset game state when session ends

            isRunning = false;
        }
    }

    public final void initializeGame() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
    }

    public boolean checkWinner() {
        // Check rows and columns
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == currentPlayer && board[i][1] == currentPlayer && board[i][2] == currentPlayer) {
                return true;
            }
            if (board[0][i] == currentPlayer && board[1][i] == currentPlayer && board[2][i] == currentPlayer) {
                return true;
            }
        }
        // after checking rows and columns we check the diagonal.
        if (board[0][0] == currentPlayer && board[1][1] == currentPlayer && board[2][2] == currentPlayer) {
            return true;
        }
        if (board[0][2] == currentPlayer && board[1][1] == currentPlayer && board[2][0] == currentPlayer) {
            return true;
        }
        return false;
    }

    void notifyPlayersSomeoneWon(ClientHandler winner, ClientHandler loser) {
        JSONObject obj = new JSONObject();
        obj.put("command", "win");
        obj.put("winner", winner.username);
        obj.put("loser", loser.username);
        try {
            player1.mouth.writeUTF(obj.toString());
            player2.mouth.writeUTF(obj.toString());
        } catch (IOException ex) {
            Logger.getLogger(GameSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void notifyPlayersDraw() {
        JSONObject obj = new JSONObject();
        obj.put("command", "draw");
        obj.put("player1", player1.username);
        obj.put("player2", player2.username);
        try {
            player1.mouth.writeUTF(obj.toString());
            player2.mouth.writeUTF(obj.toString());
        } catch (IOException ex) {
            Logger.getLogger(GameSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
}
