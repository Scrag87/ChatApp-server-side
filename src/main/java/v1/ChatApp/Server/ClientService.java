package v1.ChatApp.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

public class ClientService {
  DbLayer database;
  private ThreadedServer myServer;
  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;
  private String clientName;
  private boolean isLoggedIn;


  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public ClientService(ThreadedServer myServer, Socket socket) {
    try {
      this.myServer = myServer;
      this.socket = socket;
      this.in = new DataInputStream(socket.getInputStream());
      this.out = new DataOutputStream(socket.getOutputStream());
      this.clientName = "";
      this.isLoggedIn = false;
      this.database = new DbLayer();
      new Thread(
          () -> {
            try {
              authentication();
              readMessages();
            } finally {
              closeConnection();
            }
          })
          .start();
    } catch (IOException e) {
      throw new RuntimeException("Проблемы при создании обработчика клиента");
    }
  }

  public void authentication() {
    while (!isLoggedIn) {
      String strFromClient = getMsgFromClient();
      if (strFromClient.equals("/end")) {
        sendMsg("<@#> Connection closed");
        closeConnection();
        return;
      }

      if (strFromClient.equals("/p")) { // FIXME: 5/30/20 in production )
        myServer.printOnlineClientsList();
        myServer.printAllClients();
        continue;
      }

      if (strFromClient.startsWith("<@#>/uc")) {  // <@#>/uc u1 p1
        if (isLoggedIn) {
          sendMsg("<@#> already auth!");
          continue;
        }
        System.out.println("<@#>/uc");
        System.out.println("Auth "+auth(strFromClient));
      }

      if (strFromClient.startsWith("<@#>/reg")) {
        System.out.println("<@#>/reg");
        registration(strFromClient);
      }
    }

  }

  private boolean auth(String strFromClient) {
    String[] command = strFromClient.split(" ");
    String username = command[1];
    String password = command[2];

    if (!database.isClientInDbByName(username)){
      sendMsg("<@#> " + username + " not registered");
      return false;
    }else if (!database.getClientCredentialByName(username)[1].equals(password)){
      sendMsg("<@#> " + username + " password wrong!");
      return false;
    } else {
      setClientName(username);
      isLoggedIn = true;
      sendMsg("<@#> " + username + " successfully auth!");
      myServer.subscribe(this);
      return true;
    }
  }

  public void registration(String strFromClient) { // command username password

    Connection connection =  database.openConnection();
    try {
      System.out.println("Connection closed > " + connection.isClosed());
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
    String[] command = strFromClient.split(" ");
    String username = command[1];
    String password = command[2];
    if (!isTaken(username)) {
      database.addClient(username, password);
      sendMsg("<@#> " + username + " successfully registered!");
    } else {
      sendMsg("<@#> " + username + " is Busy");
    }
    System.out.println("Connection closed > "+database.closeConnection(connection));
  }

  public void readMessages() {
    System.out.println("readMessages()");
    while (true) {
      String strFromClient = getMsgFromClient();

      if (strFromClient.equals("/p")) {
        myServer.printOnlineClientsList();
        myServer.printAllClients();
        continue;
      }

      if (strFromClient.startsWith("/w")) {
        if (myServer.sendPrivateMsg(strFromClient, this)) {
        } else {
          sendMsg("User unavailable");
        }
        continue;
      }

      System.out.println("от " + clientName + ": " + strFromClient);

      if (strFromClient.equals("/end")) {
        sendMsg("<@#> Connection closed");
        myServer.unsubscribe(this);
        myServer.broadcastMsgMinusSender("<@#> " + clientName + " left us", this);
        return;
      }

      parseMessage(strFromClient);
    }
  }

  private boolean isTaken(String clientName) {
    return database.isClientInDbByName(clientName);
  }

  private String getMsgFromClient() {
    String strFromClient ;
    try {
      strFromClient = in.readUTF();
      System.out.println(strFromClient);
    } catch (IOException e) {
      // если поток обрывается вместо клиента пишем /end
      System.out.println("handle IOException in getMsgFromClient() ");
      strFromClient = "/end";
    }
    return strFromClient;
  }

  public void sendMsg(String msg) {
    try {
      out.writeUTF(msg);
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void closeConnection() {
    try {
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void parseMessage(String message) {
    if (message.startsWith("<@#>")) {
      parseCommand(message);
    } else {
      myServer.broadcastMsgMinusSender(clientName + ": " + message, this);
    }
  }

  /**
   * /uc,/reg - command | username | password; /u - command | clientsList ; /w - message|
   *
   * @param msg
   */
  private void parseCommand(String msg) { // FIXME: 5/13/20
    String[] command = msg.split(" ");

    if (command.equals("<@#>/u")) {
      sendMsg("<@#>/u " + myServer.clientsListAsString());
    }

    if (command.equals("<@#>/w")) {
      if (myServer.sendPrivateMsg(msg, this)) {
      } else {
        sendMsg("User unavailable");
      }
    }
  }

  @Override
  public String toString() {
    System.out.println();
    return "ClientService{" + "socket=" + socket + ", name='" + clientName + '\'' + '}';
  }
}
