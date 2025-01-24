package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.derby.jdbc.ClientDriver;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsersDao {

    private static final String URL = "jdbc:derby://localhost:1527/users";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    private static final Logger LOGGER = Logger.getLogger(UsersDao.class.getName());

    // Private constructor to prevent instantiation
    private UsersDao() {
    }

    // Get database connection
    private static Connection getConnection() throws SQLException {
        try {
            DriverManager.registerDriver(new ClientDriver());
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to establish database connection", e);
            throw e;
        }
    }

    // Helper method to close database resources
    private static void closeResources(Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing database resources", e);
        }
    }

    public static boolean checkUserExist(String userName) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(
                    "SELECT 1 FROM USERS WHERE USERNAME = ?",
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
            );
            ps.setString(1, userName);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            closeResources(conn, ps, rs);
        }
    }

    public static boolean registerUser(String userName, String password, String confirmPassword) throws SQLException {
        if (!password.equals(confirmPassword)) {
            return false;
        }

        if (checkUserExist(userName)) {
            return false;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement("INSERT INTO USERS (USERNAME, PASSWORD, SCORE,IS_LOGGED_IN) VALUES (?, ?, 0,true)");
            ps.setString(1, userName);
            ps.setString(2, password);
            return ps.executeUpdate() > 0;
        } finally {
            closeResources(conn, ps, null);
        }
    }

    public static boolean login(String userName, String password) throws SQLException {
        Connection conn = null;
        PreparedStatement checkLoginPs = null;
        PreparedStatement loginPs = null;
        ResultSet rs = null;
        PreparedStatement updatePs = null;

        try {
            conn = getConnection();

            // First check if user is already logged in
            checkLoginPs = conn.prepareStatement(
                    "SELECT IS_LOGGED_IN FROM USERS WHERE USERNAME = ?",
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );
            checkLoginPs.setString(1, userName);
            rs = checkLoginPs.executeQuery();

            if (rs.next() && rs.getBoolean("IS_LOGGED_IN")) {
                LOGGER.warning("Login attempt rejected - user already logged in: " + userName);
                return false;
            }

            // Close the first ResultSet before executing another query
            rs.close();

            // Proceed with normal login check
            loginPs = conn.prepareStatement(
                    "SELECT 1 FROM USERS WHERE USERNAME = ? AND PASSWORD = ?",
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );
            loginPs.setString(1, userName);
            loginPs.setString(2, password);
            rs = loginPs.executeQuery();

            if (rs.next()) {
                // Update login status in a separate statement
                updatePs = conn.prepareStatement("UPDATE USERS SET IS_LOGGED_IN = TRUE WHERE USERNAME = ?");
                updatePs.setString(1, userName);
                updatePs.executeUpdate();
                LOGGER.info("Login status updated to TRUE for user: " + userName);
                return true;
            }
            return false;
        } finally {
            if (checkLoginPs != null) {
                checkLoginPs.close();
            }
            if (updatePs != null) {
                updatePs.close();
            }
            closeResources(conn, loginPs, rs);
        }
    }

    public static int getUserScore(String username) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement("SELECT SCORE FROM USERS WHERE USERNAME = ?");
            ps.setString(1, username);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("SCORE");
            }
            return 0;
        } finally {
            closeResources(conn, ps, rs);
        }
    }

    public static Map<String, Integer> getUserScores() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Map<String, Integer> map = new HashMap<>();
        try {
            conn = getConnection();
            ps = conn.prepareStatement("SELECT USERNAME , SCORE FROM USERS");
            rs = ps.executeQuery();
            while (rs.next()) {
                //System.out.println(rs.getInt("SCORE"));
                map.put(rs.getString("USERNAME"), rs.getInt("SCORE"));
            }
            return map;
        } finally {
            closeResources(conn, ps, rs);
        }
    }

    public static boolean logout(String userName) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(
                    "SELECT IS_LOGGED_IN FROM USERS WHERE USERNAME = ?",
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );
            ps.setString(1, userName);
            rs = ps.executeQuery();

            if (!rs.next() || !rs.getBoolean("IS_LOGGED_IN")) {
                LOGGER.warning("Logout failed - user not found or not logged in: " + userName);
                return false;
            }

            rs.close();
            ps.close();

            ps = conn.prepareStatement("UPDATE USERS SET IS_LOGGED_IN = FALSE WHERE USERNAME = ?");
            ps.setString(1, userName);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                LOGGER.info("Logout successful for user: " + userName);
                return true;
            }
            return false;
        } finally {
            closeResources(conn, ps, rs);
        }
    }

    public static int logoutAllUsers() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement("UPDATE USERS SET IS_LOGGED_IN = FALSE WHERE IS_LOGGED_IN = TRUE");
            int rowsAffected = ps.executeUpdate();

            LOGGER.info("Logged out " + rowsAffected + " users");
            return rowsAffected;
        } finally {
            closeResources(conn, ps, null);
        }
    }

    public static void updateScore(String username, int scoreIncrement) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement verifyPs = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            // Update the score
            ps = conn.prepareStatement("UPDATE USERS SET SCORE = SCORE + ? WHERE USERNAME = ?");
            ps.setInt(1, scoreIncrement);
            ps.setString(2, username);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Score update failed - user not found: " + username);
            }

            // Verify the update
            verifyPs = conn.prepareStatement("SELECT SCORE FROM USERS WHERE USERNAME = ?");
            verifyPs.setString(1, username);
            rs = verifyPs.executeQuery();

            if (rs.next()) {
                int newScore = rs.getInt("SCORE");
                LOGGER.log(Level.INFO, "Score updated for user {0}: {1}", new Object[]{username, newScore});
            }
        } finally {
            if (verifyPs != null) {
                verifyPs.close();
            }
            closeResources(conn, ps, rs);
        }
    }
}
