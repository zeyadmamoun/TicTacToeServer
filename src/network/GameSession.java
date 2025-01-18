/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author mahmo
 */
class GameSession extends Thread {

    private char currentSymbol = 'X';  // Track the current symbol (X or O)
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
            Thread.sleep(100);
            JSONObject startObj = new JSONObject();
            startObj.put("command", "start");
            player1.mouth.writeUTF(startObj.toString());
            //player2.mouth.writeUTF(startObj.toString());

            while (isRunning) {

                if (player1.ear.available() > 0) {
                    String msg = player1.ear.readUTF();
                    handlePlayerMessage(msg, player1, player2);
                }

                if (player2.ear.available() > 0) {
                    String msg = player2.ear.readUTF();
                    handlePlayerMessage(msg, player2, player1);
                }

            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(GameSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handlePlayerMessage(String msg, ClientHandler currentPlayer, ClientHandler otherPlayer) throws IOException {
        JSONObject obj = new JSONObject(msg);
        if (obj.getString("command").equals("exit_game")) {
            endGame();
        } else if (obj.getString("command").equals("move")) {
            int col = obj.getInt("col");
            int row = obj.getInt("row");
            char playerSymbol = currentPlayer == player1 ? 'X' : 'O';
            board[row][col] = playerSymbol;
            // otherPlayer.mouth.writeUTF(move);

            // Update currentSymbol to the symbol we just placed
            currentSymbol = playerSymbol;

            if (checkWinner()) {
                otherPlayer.mouth.writeUTF(msg);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GameSession.class.getName()).log(Level.SEVERE, null, ex);
                }
                notifyPlayersSomeoneWon(currentPlayer, otherPlayer);
                endGame();
                return;
            } else if (isBoardFull()) {
                notifyPlayersDraw();
                endGame();
                return;
            }
            otherPlayer.mouth.writeUTF(msg);
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
            if (board[i][0] == currentSymbol && board[i][1] == currentSymbol && board[i][2] == currentSymbol) {
                return true;
            }
            if (board[0][i] == currentSymbol && board[1][i] == currentSymbol && board[2][i] == currentSymbol) {
                return true;
            }
        }
        // after checking rows and columns we check the diagonal.
        if (board[0][0] == currentSymbol && board[1][1] == currentSymbol && board[2][2] == currentSymbol) {
            return true;
        }
        if (board[0][2] == currentSymbol && board[1][1] == currentSymbol && board[2][0] == currentSymbol) {
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

    // In GameSession.java
    private void endGame() {
        try {
            JSONObject msg = new JSONObject();
            msg.put("command", "exit_game");
            isRunning = false;
            player1.isPlaying = false;
            player2.isPlaying = false;
            player1.mouth.writeUTF(msg.toString());
            player2.mouth.writeUTF(msg.toString());
        } catch (IOException ex) {
            Logger.getLogger(GameSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
