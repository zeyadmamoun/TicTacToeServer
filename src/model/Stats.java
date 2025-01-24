/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author POP
 */
public class Stats {

    private int totalPlayers;
    private int onlinePlayers;
    private int offlinePlayers;

    public Stats(int totalPlayers, int onlinePlayers, int offlinePlayers) {
        this.totalPlayers = totalPlayers;
        this.onlinePlayers = onlinePlayers;
        this.offlinePlayers = offlinePlayers;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }

    public int getOfflinePlayers() {
        return offlinePlayers;
    }

    public void setOfflinePlayers(int offlinePlayers) {
        this.offlinePlayers = offlinePlayers;
    }
}
