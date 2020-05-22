package v1.ChatApp.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ThreadedServer {
  Message message ;


  private static int clientCount;
  static Map<ClientService, String> clientMap = new ConcurrentHashMap<>(); // id,username
  private final int PORT = 8087;
  private Vector<Client> allClients = new Vector<>();
  private Vector<ClientService> clientsList;

  public Vector<Client> getAllClients() {
    return allClients;
  }

  protected void printOnlineClientsList() {
    System.out.println(clientsList);
  }
  protected void printAllClients() {
    System.out.println(allClients);
  }
  protected synchronized String  clientsListAsString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (ClientService clientService : clientsList) {
     stringBuilder.append(clientService.getClientName()).append(" ");
      }
   return  stringBuilder.toString();

  }

  public synchronized void addClientToMap(ClientService clientService, String name) {
    clientMap.put(clientService, name);
  }
  public synchronized void addClientToAllClientsList(String username, String password) {
    allClients.add(new Client(username,password));
  }
  public ThreadedServer() {
    System.out.println("Threaded Echo Server");
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      clientsList = new Vector<>();

      while (true){
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
  protected Message  tokenizeRegMsg(String received) { // /command recipient message
    StringTokenizer st = new StringTokenizer(received, " ");
    String command = st.nextToken();
    String recipient = st.nextToken();
    StringBuilder str = new StringBuilder();
    while (st.hasMoreTokens()) {
      str.append(st.nextToken()).append(" ");
    }
    return new Message(command,recipient,str.toString());
  }

  public synchronized boolean isUsernameBusy(String nickname) {
    // Map l8tr
    for (Client client: allClients) {
      if (client.name.equals(nickname)) {
        return true;
      }
    }
    return false;
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
    tmpList = null;
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
    broadcastMsg("<@#> " + o.getClientName() + " join us");
    broadcastClientsList();

  }

  public  String join( Collection collection, String delimiter )
  {
    return (String) collection.stream()
        .map( Object::toString )
        .collect( Collectors.joining( delimiter ) );
  }
}
