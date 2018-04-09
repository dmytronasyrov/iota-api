import com.google.gson.Gson;

public class Item {

  private final String title;
  private final String price;

  public Item(String title, String price) {
    this.title = title;
    this.price = price;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  @Override
  public String toString() {
    return "Item{" +
        "title='" + title + '\'' +
        ", price='" + price + '\'' +
        '}';
  }
}
