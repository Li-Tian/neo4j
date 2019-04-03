package neo.Wallets.SQLite.sqlitJDBC;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Created by doubi.liu on 2017/5/16.
 * 提供连接对象
 */
public class ConnectionFactory {
    private static String DRIVER;
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    //解析properties文件
    static {
        Properties props = new Properties();
        try {
            URL url = ConnectionFactory.class.getResource("pro.properties");
            FileReader filereader = new FileReader(url.getPath());
            props.load(filereader);
            DRIVER = props.getProperty("drive_class");
            URL = props.getProperty("url");
            USER = props.getProperty("username");
            PASSWORD = props.getProperty("pwd");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    //工厂方法，返回Connection对象
    public static Connection getConnection() throws DataAccessException {
        Connection conn = null;

        try {
            Class.forName(DRIVER);
            conn = DriverManager
                    .getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
        return conn;
    }

    //工厂方法，返回Connection对象
    public static Connection getConnection(String localaddress) throws DataAccessException {
        Connection conn = null;

        try {
            Class.forName(DRIVER);
            String localurl = "jdbc:sqlite:" + localaddress;
            conn = DriverManager.getConnection(localurl);
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
        return conn;
    }

    //工厂方法，返回Connection对象
    public static Connection getConnection(String localaddress, String username, String password) throws DataAccessException {
        Connection conn = null;

        try {
            Class.forName(DRIVER);
            String localurl = "jdbc:sqlite:" + localaddress;
            conn = DriverManager.getConnection(URL, username, password);
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
        return conn;
    }
}
