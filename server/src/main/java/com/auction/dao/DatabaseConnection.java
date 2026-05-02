package com.auction.dao;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
 
/**
 * DatabaseConnection - Singleton quản lý kết nối JDBC thread-safe.
 *
 * Dùng synchronized để đảm bảo mỗi lúc chỉ 1 thread dùng connection.
 * Tự động reconnect nếu connection bị đóng.
 *
 * Cách dùng trong DAO:
 *   synchronized (DatabaseConnection.getInstance()) {
 *       Connection conn = DatabaseConnection.getInstance().getConnection();
 *       ...
 *   }
 */
public class DatabaseConnection {
 
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
 
    private static final String URL      = "jdbc:mysql://localhost:3306/auction_db"
                                         + "?useSSL=false&serverTimezone=UTC"
                                         + "&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "n05122007"; // đổi theo máy của bạn
 
    // ── Singleton ──────────────────────────────────────────────────────────
    private static volatile DatabaseConnection instance;
    private Connection connection;
 
    private DatabaseConnection() {
        connect();
    }
 
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }
 
    // ── Connection ─────────────────────────────────────────────────────────
 
    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            LOGGER.info("Database connected successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL Driver not found", e);
            throw new RuntimeException("MySQL Driver not found", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Cannot connect to database", e);
            throw new RuntimeException("Cannot connect to database", e);
        }
    }
 
    /**
     * Lấy connection.
     * Luôn gọi trong khối synchronized để thread-safe:
     *
     *   synchronized (DatabaseConnection.getInstance()) {
     *       Connection conn = DatabaseConnection.getInstance().getConnection();
     *       ...
     *   }
     */
    public synchronized Connection getConnection() {
        try {
            // Tự động reconnect nếu connection bị đóng hoặc timeout
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                LOGGER.info("Connection lost — reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error checking connection, reconnecting...", e);
            connect();
        }
        return connection;
    }
 
    public synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing connection", e);
        }
    }
}