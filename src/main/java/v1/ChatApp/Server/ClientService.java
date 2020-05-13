package v1.ChatApp.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientService {
  Client client;

  Message message;
  private ThreadedServer myServer;
  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;
  private String clientName;
  private String clientPassword;
  private String clientNickname;
  private boolean isLoggedIn = false;
//  private boolean isAuth = false;


  public String getClientPassword() {
    return clientPassword;
  }

  public void setClientPassword(String clientPassword) {
    this.clientPassword = clientPassword;
  }

  public String getClientNickname() {
    return clientNickname;
  }

  public void setClientNickname(String clientNickname) {
    this.clientNickname = clientNickname;
  }

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

      if (strFromClient.equals("/p")) {
        myServer.printOnlineClientsList();
        myServer.printAllClients();
        continue;
      }

      if (strFromClient.startsWith("<@#>/uc")) {
        if (isLoggedIn) {
          sendMsg("<@#> already auth!");
          continue;
        }
        System.out.println("<@#>/uc");
        auth(strFromClient);
      }

      if (strFromClient.startsWith("<@#>/reg")) {
        System.out.println("<@#>/reg");
        registration(strFromClient);
      }
    }
    myServer.subscribe(this);
  }

  private void exitApp() {
    System.exit(11);
  }

  private boolean auth(String strFromClient) {
    String[] command = strFromClient.split(" ");
    String username = command[1];
    String password = command[2];
    if (!myServer.getAllClients().isEmpty()) {
      for (Client client : myServer.getAllClients()) {
        if (client.name.equals(username)) {
          if (client.password.equals(password)) {
            isLoggedIn = true;
            setClientName(username);
            setClientPassword(password);
            sendMsg("<@#> " + username + " successfully auth!");
            return true;
          } else {
            sendMsg("<@#> " + username + " password wrong!");
            return false;
          }
        }
      }
    }
    sendMsg("<@#> " + username + " not exist!");
    return false;
  }

  public void registration(String strFromClient) { // command username password
    String[] command = strFromClient.split(" ");
    String username = command[1];
    String password = command[2];
    if (checkName(username)) {
      myServer.addClientToAllClientsList(username, password);
      sendMsg("<@#> " + username + " successfully registered!");
    } else {
      sendMsg("<@#> " + username + " is Busy");
    }
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

  private boolean checkName(String msgFromClient) {
    if (!myServer.isUsernameBusy(msgFromClient)) {
      return true;
    } else {
      return false;
    }
  }

  private String getMsgFromClient() {
    String strFromClient = null;
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

    if (command.equals("<@#>/uc")) {
      String username = command[1];
      String password = command[2];
    }
    //    if (msg.startsWith("<@#>/reg")) {
    //      String username = command[1];
    //      String password = command[2];
    //      if (checkName(username)) {
    //        setClientName(username);
    //
    //        setClientPassword(password);
    //        sendMsg("<@#> " + username + " successfully registered!");
    //      }
    //    }

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
