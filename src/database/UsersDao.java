package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.derby.jdbc.ClientDriver;

/**
 *
 * @author zeyad_maamoun
 */
public class UsersDao {

    private static String url = "jdbc:derby://localhost:1527/users";
    private static String username_db = "root";
    private static String password_db = "root";

    public static boolean checkUserExist(String userName) throws SQLException {
        boolean isUserExist = false;
        DriverManager.deregisterDriver(new ClientDriver());
        Connection connection = DriverManager.getConnection(url, username_db, password_db);
        PreparedStatement pst = connection.prepareStatement("SELECT * FROM USERS WHERE USERNAME = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        pst.setString(1, userName);
        ResultSet rs = pst.executeQuery();
        //move the cursor and check if there's a record will return true and vice versa
        isUserExist = rs.next();
        pst.close();
        connection.close();
        return isUserExist;
    }

    public static boolean registerUser(String userName, String password, String confirmPassword) throws SQLException {
        if (!password.equals(confirmPassword)) {
            return false;
        }

        if (checkUserExist(userName)) {
            return false;
        }

        DriverManager.deregisterDriver(new ClientDriver());

        try (Connection connection = DriverManager.getConnection(url, username_db, password_db);
                PreparedStatement pst = connection.prepareStatement("INSERT INTO USERS (USERNAME, PASSWORD) VALUES (?, ?)")) {
            pst.setString(1, userName);
            pst.setString(2, password);
            int rowsInserted = pst.executeUpdate();
            return rowsInserted > 0;
        }
    }

     public static boolean login(String checkUserName, String checkPassWord) throws SQLException {
        boolean checkerData = false;

        DriverManager.registerDriver(new ClientDriver());

        Connection connection = DriverManager.getConnection(url, username_db, password_db);
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM USERS WHERE USERNAME = ? AND PASSWORD = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        ps.setString(1, checkUserName);
        ps.setString(2, checkPassWord);

        ResultSet resultSet = ps.executeQuery();

        while (resultSet.next()) {
            String userNameDb = resultSet.getString("USERNAME");
            String passWordDb = resultSet.getString("PASSWORD");
            if (checkUserName.equals(userNameDb) && checkPassWord.equals(passWordDb)) {
                checkerData = true;
                break;
            } else {
                checkerData = false;
            }
        }

        return checkerData;
    }
    
//    public static boolean login(String checkUserName, String checkPassWord) throws SQLException {
//        boolean checkerData = false;
//
//        DriverManager.registerDriver(new ClientDriver());
//        Connection connection = DriverManager.getConnection(url, username_db, password_db);
//        PreparedStatement ps = connection.prepareStatement(
//                "SELECT * FROM USERS WHERE USERNAME = ? AND PASSWORD = ?",
//                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY
//        );
//
//        ps.setString(1, checkUserName);
//        ps.setString(2, checkPassWord);
//
//        ResultSet resultSet = ps.executeQuery();
//
//        if (resultSet.next()) {
//            // Update the login status to true
//            PreparedStatement updateStatus = connection.prepareStatement("UPDATE USERS SET IS_LOGGED_IN = TRUE WHERE USERNAME = ?");
//            updateStatus.setString(1, checkUserName);
//            updateStatus.executeUpdate();
//            updateStatus.close();
//            checkerData = true;
//            System.out.println("Login status updated to TRUE for user: " + checkUserName);
//
//        }
//
//        ps.close();
//        connection.close();
//        return checkerData;
//    }

    public static int getUserScore(String username) throws SQLException {
        int score = 0;
        DriverManager.registerDriver(new ClientDriver());
        Connection connection = DriverManager.getConnection(url, username_db, password_db);
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM USERS WHERE USERNAME = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        ps.setString(1, username);
        ResultSet resultSet = ps.executeQuery();

        while (resultSet.next()) {
            String userNameDb = resultSet.getString("USERNAME");
            if (username.equals(userNameDb)) {
                score = resultSet.getInt("SCORE");
                break;
            }
        }

        return score;
    }

    public static void updateScore(String username, int additionalScore) throws SQLException {
        System.out.println("update score");
        DriverManager.registerDriver(new ClientDriver());
        Connection connection = DriverManager.getConnection(url, username_db, password_db);

        PreparedStatement ps = connection.prepareStatement("UPDATE Users SET score = score + ? WHERE USERNAME = ?");
        
        ps.setInt(1, additionalScore);
        ps.setString(2, username);
        ps.executeUpdate();

    }

}
