package v1.ChatApp.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbLayer {

  Connection connection;

  public static void initializationDbDriver() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
  }

  public void createClientTable() {
    System.out.println("createClientTable if not exist");
    String createTableUser =
        "CREATE TABLE  IF NOT EXISTS USERS(id INTEGER primary key AUTOINCREMENT, name TEXT UNIQUE, "
            + "password TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP);";
    connection = openConnection();
    try {
      try (
          Statement statement = connection.createStatement()) {
        statement.execute(createTableUser);
      }
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
    closeConnection(connection);
  }

  public boolean addClient(String name, String password) {
    System.out.println("Adding client " + name + " / " + password);
    connection = openConnection();

    String createSql = "INSERT INTO USERS (name, password, created_at) VALUES ('%s','%s',%s);";
    createSql = String.format(createSql, name, password, " datetime('now','localtime')");
    try {
      try (Statement statement = connection.createStatement()) {
        System.out.println(createSql);
        if (isClientInDbByName(name)) {
          System.out.println("Client exist!");
          closeConnection(connection);
          return false;
        } else {
          statement.executeUpdate(createSql);
          closeConnection(connection);
          return true;
        }

      }
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
    return false;
  }


  public boolean isClientInDbByName(String name) {

    connection = openConnection(); //2 connection
    String query = "SELECT name from USERS where name = '" + name + "'";
    try {
      try (Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(query);) {
        while (resultSet.next()) {
          if (resultSet.getString("name").toLowerCase().equalsIgnoreCase(name)) {
            System.out.println("isClientInDbByName " + true);
            closeConnection(connection);//close 2nd
            return true;
          }
        }
      }
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
    closeConnection(connection);
    return false;

  }

  public String[] getClientCredentialByName(String name) {
    String query = "SELECT name, password from USERS where name = '" + name + "'";
    connection = openConnection();
    try {
      try (
          Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery(query);) {
        while (result.next()) {
          if (result.getString("name").toLowerCase().equalsIgnoreCase(name)) {
            String username = result.getString("name");
            String password = result.getString("password");
            closeConnection(connection);
            return new String[]{username, password};
          }
        }

      }
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
    System.out.println("User does not exist.");
    closeConnection(connection);
    return new String[]{"", ""};
  }

  public Connection openConnection() {
    System.out.println("\t\tOpenning connection to DB...");

    try {
      connection = DriverManager.getConnection(
          "jdbc:sqlite:identifier.sqlite");
      if (connection.isClosed()) {
        connection = DriverManager.getConnection(
            "jdbc:sqlite:identifier.sqlite");
      } else {
        System.out.println("\t\tConnection open!");
      }

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
    return connection;
  }

  public boolean closeConnection(Connection connection) {
    System.out.println("\t\tClosing Connection ...");
    try {
      if (!connection.isClosed()) {
        connection.close();
        System.out.println("\t\tConnection is closed - " + connection.isClosed());
        return connection.isClosed();
      }
      System.out.println("\t\tWAS already Closed by try()");
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
    return true;
  }

}



