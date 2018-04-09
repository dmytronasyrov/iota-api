import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

//curl -H "Content-Type: application/json" -H "X-IOTA-API-Version: 1" -d '{"command": "getNodeInfo"}' http://176.9.3.149:14265

public class App {

  public static void main(String[] args) {
    Properties config = getNodeConfig();
    IotaAPI api = iota(config);
    GetNodeInfoResponse response = api.getNodeInfo();

    System.out.println("IOTA info: " + response.toString());
  }

  private static IotaAPI iota(Properties conf) {
    return new IotaAPI.Builder()
        .protocol(conf.getProperty("iota.node.protocol"))
        .host(conf.getProperty("iota.node.host"))
        .port(conf.getProperty("iota.node.port"))
        .build();
  }

  private static Properties getNodeConfig() {
    try {
      Properties nodeConfig = new Properties();
      FileReader fileReader = new FileReader("iota-node.properties");
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      nodeConfig.load(bufferedReader);

      return nodeConfig;
    } catch (IOException e) {
      e.printStackTrace();

      return null;
    }
  }

}