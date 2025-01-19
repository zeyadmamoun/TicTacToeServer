package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.derby.jdbc.ClientDriver;

public class UsersDao {
    private static final String URL = "jdbc:derby://localhost:1527/users";
    private static final String USERNAME_DB = "root";
    private static final String PASSWORD_DB = "root";
    
    // Initialize the driver once when the class is loaded
    static {
        try {
            DriverManager.registerDriver(new ClientDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database driver", e);
        }
    }
    
    // Helper method to get connection
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME_DB, PASSWORD_DB);
    }

    public static boolean checkUserExist(String userName) throws SQLException {
        String query = "SELECT * FROM USERS WHERE USERNAME = ?";
        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement(query, 
                 ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            
            pst.setString(1, userName);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static boolean registerUser(String userName, String password, String confirmPassword) 
            throws SQLException {
        if (!password.equals(confirmPassword) || checkUserExist(userName)) {
            return false;
        }

        String query = "INSERT INTO USERS (USERNAME, PASSWORD) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement(query)) {
            
            pst.setString(1, userName);
            pst.setString(2, password);
            return pst.executeUpdate() > 0;
        }
    }

    public static boolean login(String checkUserName, String checkPassWord) throws SQLException {
        String query = "SELECT * FROM USERS WHERE USERNAME = ? AND PASSWORD = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query, 
                 ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            
            ps.setString(1, checkUserName);
            ps.setString(2, checkPassWord);
            
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    String userNameDb = resultSet.getString("USERNAME");
                    String passWordDb = resultSet.getString("PASSWORD");
                    if (checkUserName.equals(userNameDb) && checkPassWord.equals(passWordDb)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public static int getUserScore(String username) throws SQLException {
        String query = "SELECT SCORE FROM USERS WHERE USERNAME = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            
            ps.setString(1, username);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("SCORE");
                }
                return 0;
            }
        }
    }

    public static void updateScore(String username, int additionalScore) throws SQLException {
        String query = "UPDATE Users SET score = score + ? WHERE USERNAME = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            
            ps.setInt(1, additionalScore);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }
}