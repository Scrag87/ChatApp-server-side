package v1.ChatApp.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientService {
  private ThreadedServer myServer;
  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;
  private String name;
  boolean isLoggedIn;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ClientService(ThreadedServer myServer, Socket socket) {
    try {
      this.myServer = myServer;
      this.socket = socket;
      this.in = new DataInputStream(socket.getInputStream());
      this.out = new DataOutputStream(socket.getOutputStream());
      this.name = "";
      this.isLoggedIn = true;
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

  public void authentication(){
    myServer.subscribe(this);
  }

  public void readMessages() {
    boolean isFirstMsg = true;
    System.out.println("readMessages()");
    while (true) {



      if (isFirstMsg) {
        sendMsg("enter your nickname(or ' /end ' to exit): ");
        if(checkName(getMsgFromClient())){
          isFirstMsg=false;
          continue;
        }
        isFirstMsg = true;
        continue;
      }
      String strFromClient = getMsgFromClient();
      if (strFromClient.equals("/p")) {
        myServer.printClientsList();
        continue;
      }

      if (strFromClient.startsWith("/w")) {
        myServer.sendPrivateMsg(strFromClient, this);
        continue;
      }

      System.out.println("от " + name + ": " + strFromClient);
      if (strFromClient.equals("/end")) {
        sendMsg("<@#> Connection closed");

        return;
      }
      myServer.broadcastMsgMinusSender(name + ": " + strFromClient,this);
    }
  }

  private boolean checkName(String msgFromClient) {
    if (!myServer.isNickBusy(msgFromClient)) {
      sendMsg("<@#> " + msgFromClient + " successfully registered!" );
      setName(msgFromClient);
      myServer.broadcastMsg("<@#> "+ name + " join us");
      myServer.subscribe(this);
      return true;
    } else {
      sendMsg("<@#> "+ "Busy");
      return false;
    }
  }

  private String getMsgFromClient() {
    String strFromClient = null;
    try {
      strFromClient = in.readUTF();
      System.out.println(strFromClient);
    } catch (IOException e) {
      //если поток обрывается вместо клиента пишем /end
     // e.printStackTrace();
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
    myServer.unsubscribe(this);
    myServer.broadcastMsgMinusSender("<@#> "+name + " left us",this);
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

  @Override
  public String toString() {
    System.out.println();
    return "ClientService{" + "socket=" + socket + ", name='" + name + '\'' + '}';
  }
}
