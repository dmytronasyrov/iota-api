import com.google.gson.Gson;
import jota.IotaAPI;
import jota.dto.response.*;
import jota.error.ArgumentException;
import jota.model.Bundle;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.utils.Converter;
import jota.utils.InputValidator;
import jota.utils.SeedRandomGenerator;
import jota.utils.TrytesConverter;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

//curl -H "Content-Type: application/json" -H "X-IOTA-API-Version: 1" -d '{"command": "getNodeInfo"}' http://176.9.3.149:14265
//curl -H "Content-Type: application/json" -H "X-IOTA-API-Version: 1" -d '{"command": "getNodeInfo"}' https://nodes.testnet.iota.org:443

public class App {

  private static final int MIN_WEIGHT_MAGNITUDE = 14;
  private static final int DEPTH = 9;

  public static void main(String[] args) {
    try {
      Item item = new Item("BMW", "12000 USD");

      Properties config = getNodeConfig();
      IotaAPI iota = iota(config);

      GetNodeInfoResponse infoResponse = iota.getNodeInfo();
      System.out.println("IOTA INFO: " + infoResponse.toString());
      System.out.println("---------------------------------------------------------\n");

      String seed = seed();
      System.out.println("IOTA SEED: " + seed);
      System.out.println("IOTA VALID SEED: " + String.valueOf(InputValidator.isAddress(seed)));
      System.out.println("---------------------------------------------------------\n");

      account(iota, seed);

      List<String> addresses = addresses(iota, seed);
      String address = addresses.get(0);
      System.out.println("IOTA ADDRESSES: " + addresses.toString());
      System.out.println("---------------------------------------------------------\n");

      String tag = TrytesConverter.toTrytes("Pharos");
      System.out.println("IOTA TAG TRYTES: " + tag);

      long startTime = System.currentTimeMillis();
      SendTransferResponse transferResponse = sendTransfer(iota, seed, address, item, tag);
      System.out.println("IOTA TRANSACTIONS: " + transferResponse.getTransactions().toString());
      System.out.println("IOTA SUCCESS: " + Arrays.toString(transferResponse.getSuccessfully()));
      long endTime = System.currentTimeMillis();
      NumberFormat formatter = new DecimalFormat("#0.00000");
      System.out.print("Execution time is " + formatter.format((endTime - startTime) / 1000d) + " seconds\n");
      System.out.println("---------------------------------------------------------\n");

      String[] hashes = transferResponse.getTransactions().stream().map(Transaction::getHash).toArray(String[]::new);
      System.out.println("IOTA HASHES: " + Arrays.toString(hashes));
      findByHashes(iota, hashes);

      String[] tagsToFind = { "DCWCPCFDCDGD" };
      System.out.println("IOTA TAGS: " + Arrays.toString(tagsToFind));
      findByTags(iota, tagsToFind);

      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static String seed() {
    return SeedRandomGenerator.generateNewSeed();
  }

  private static List<String> addresses(IotaAPI iota, String seed) throws ArgumentException {
    GetNewAddressResponse address = iota.getNewAddress(seed, 2, 0, true, 5, false);

    return address.getAddresses();
  }

  private static GetAccountDataResponse account(IotaAPI iota, String seed) throws ArgumentException {
    GetAccountDataResponse accounts = iota.getAccountData(seed, 2, 0, true, 0, true, 0, 0, true, 0);
    System.out.println("IOTA ACCOUNTS: " + accounts.toString());
    System.out.println("---------------------------------------------------------\n");

    return accounts;
  }

//  private static GetBalancesAndFormatResponse inputs(IotaAPI iota, String seed) throws ArgumentException {
//    GetBalancesAndFormatResponse inputs = iota.getInputs(seed, 2, 0, 0, 1);
//    System.out.println("IOTA INPUTS: " + inputs.toString());
//    System.out.println("---------------------------------------------------------\n");
//
//    return inputs;
//  }

  private static SendTransferResponse sendTransfer(IotaAPI iota, String seed, String address, Item item, String tag) throws ArgumentException {
    final String trytes = TrytesConverter.toTrytes(item.toJson());
    System.out.println("IOTA MSG TRYTES: " + trytes);
    Transfer transfer = new Transfer(address, 0, trytes, tag);
    List<Transfer> transfers = new ArrayList<>();
    transfers.add(transfer);

    return iota.sendTransfer(seed, 2, DEPTH, MIN_WEIGHT_MAGNITUDE, transfers, null, null, false);
  }

  private static void findByTags(IotaAPI iota, String[] tags) throws ArgumentException {
//    FindTransactionResponse findResponse = iota.findTransactions(null, tags, null, null);
//    String[] hashes = findResponse.getHashes();
//    System.out.println("IOTA TAGS HASHES: " + Arrays.toString(hashes));

    List<Transaction> transactions = iota.findTransactionObjectsByTag(tags);
    System.out.println("IOTA TRANSACTIONS BY TAGS: " + transactions.toString());
    getItemsFromTxs(transactions);
    System.out.println("---------------------------------------------------------\n");
  }

  private static void findByHashes(IotaAPI iota, String[] hashes) throws ArgumentException {
    List<Transaction> transactions = iota.findTransactionsObjectsByHashes(hashes);
    System.out.println("IOTA TRANSACTIONS BY HASHES: " + transactions.toString());
    getItemsFromTxs(transactions);
    System.out.println("---------------------------------------------------------\n");
  }

  private static void getItemsFromTxs(List<Transaction> transactions) {
    for (Transaction tx : transactions) {
      String dataEncrypt = StringUtils.stripEnd(tx.getSignatureFragments(), "9");
      String json = TrytesConverter.toString(dataEncrypt);
      Item item = new Gson().fromJson(json, Item.class);
      System.out.println("IOTA ITEM: " + item.toString());
    }
  }

//  private static void getTransfers(IotaAPI iota, String seed) throws ArgumentException {
//    GetTransferResponse transfers = iota.getTransfers(seed, 2, 0, 0, false);
//    System.out.println("IOTA TRANSFERS: " + transfers.toString());
//
//    for (Bundle bundle : transfers.getTransfers()) {
//      for (Transaction tx : bundle.getTransactions()) {
//        System.out.println(tx.getBranchTransaction());
////        System.out.println("TRANSACTION MESSAGE: " + TrytesConverter.toString(tx.getBundle()));
//      }
//    }
//
//    System.out.println("---------------------------------------------------------\n");
//  }

  private static IotaAPI iota(Properties conf) {
    return new IotaAPI.Builder()
        .protocol(conf.getProperty("iota.node.protocol"))
        .host(conf.getProperty("iota.node.host"))
        .port(conf.getProperty("iota.node.port"))
        .build();
  }

  private static Properties getNodeConfig() throws IOException {
    Properties nodeConfig = new Properties();
    FileReader fileReader = new FileReader("iota-node.properties");
    BufferedReader bufferedReader = new BufferedReader(fileReader);
    nodeConfig.load(bufferedReader);

    return nodeConfig;
  }

}