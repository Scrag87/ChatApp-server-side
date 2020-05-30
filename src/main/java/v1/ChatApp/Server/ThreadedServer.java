package v1.ChatApp.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadedServer {

  static Map<ClientService, String> clientMap = new ConcurrentHashMap<>(); // id,username
  private final int PORT = 8087;

  private Vector<Client> allClients = new Vector<>();
  private Vector<ClientService> clientsList;

  protected void printOnlineClientsList() {
    System.out.println(clientsList);
  }

  protected void printAllClients() {
    System.out.println(allClients);
  }

  protected synchronized String clientsListAsString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (ClientService clientService : clientsList) {
      stringBuilder.append(clientService.getClientName()).append(" ");
    }
    return stringBuilder.toString();

  }

  public ThreadedServer() {
    System.out.println("Threaded Echo Server");
    dbInit();
    serverInit();
  }

  private void dbInit()  {
    try {
      DbLayer.initializationDbDriver();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    DbLayer dbLayer = new DbLayer();
    dbLayer.createClientTable();

  }

  private void serverInit() {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      clientsList = new Vector<>();

      while (true) {
        System.out.println("Waiting for connection.....");
        Socket socket = serverSocket.accept();
        System.out.println("Client connected");
        new ClientService(this, socket);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    System.out.println("Threaded Echo Server Terminating");
  }

  protected String[] tokenizeCmdRcpMsg(String received) { // /command recipient message
    String[] clientMessage = {"", "", ""};
    StringTokenizer st = new StringTokenizer(received, " ");
    String command = st.nextToken();
    String recipient = st.nextToken();
    StringBuilder str = new StringBuilder();
    while (st.hasMoreTokens()) {
      str.append(st.nextToken()).append(" ");
    }
    clientMessage[0] = command;
    clientMessage[1] = recipient;
    clientMessage[2] = str.toString();
    System.out.println("tokens " + Arrays.toString(clientMessage));
    return clientMessage;
  }


  public synchronized void broadcastClientsList() {
    StringBuilder sb = new StringBuilder("<@#>/u ");
    for (ClientService o : clientsList) {
      sb.append(o.getClientName() + " ");
    }
    broadcastMsg(sb.toString());
  }


  public synchronized void broadcastMsgMinusSender(String msg, ClientService client) {
    Vector<ClientService> tmpList = new Vector<>();
    tmpList.addAll(clientsList);
    tmpList.removeElement(client);
    for (ClientService clientService : tmpList) {
      clientService.sendMsg(msg);
    }
  }

  public synchronized void broadcastMsg(String msg) {
    for (ClientService clientService : clientsList) {
      clientService.sendMsg(msg);
    }
  }

  public synchronized boolean sendPrivateMsg(String msg, ClientService clientService) {
    String[] comRecepMsg = tokenizeCmdRcpMsg(msg);
    for (ClientService client : clientsList) {
      if (client.getClientName().equals(comRecepMsg[1])) {
        client.sendMsg("Private from " + clientService.getClientName() + ": " + comRecepMsg[2]);
        return true;
      }
    }
    return false;
  }

  public synchronized void unsubscribe(ClientService clientService) {
    clientsList.remove(clientService);
    clientMap.remove(clientService);
    broadcastClientsList();
  }

  public synchronized void subscribe(ClientService o) {
    clientsList.add(o);
    clientMap.put(o, "");
    broadcastMsg(o.getClientName() + " join us");
    broadcastClientsList();
  }


}
