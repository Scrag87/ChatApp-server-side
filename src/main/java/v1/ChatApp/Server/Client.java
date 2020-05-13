package v1.ChatApp.Server;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Client {
  private static int count;
  String name;
  String password;
  String ipAddress;
  int id;
  LocalDate dateOfCreation;
  static List<Client> clientList = new ArrayList<>();

  public Client(String name, String password) {
    count++;
    this.name = name;
    this.password = password;
    this.dateOfCreation = LocalDate.now();
    this.id = count;

  }

  public String getName() {
    return name;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String toString() {
    return "Client{"
        + "name='"
        + name
        + '\''
        + ", password='"
        + password
        + '\''
        + ", id="
        + id
        + ", dateOfCreation="
        + dateOfCreation
        + '}';
  }
}
