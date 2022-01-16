import java.util.ArrayList;

public class Key extends Item {
  private String keyId;

  public Key(String keyId, String keyName, String startingRoom, int weight, String description, ArrayList<String> aliases) {
    super(weight, keyName, startingRoom, false, description, aliases, true);
    this.keyId = keyId;
  }

  public String getKeyId() {
    return keyId;
  }
}
