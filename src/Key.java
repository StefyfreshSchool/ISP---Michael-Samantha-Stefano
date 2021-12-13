import java.util.ArrayList;

public class Key extends Item {
  private String keyId;

  public Key(String keyId, String keyName, int weight, String description, ArrayList<String> aliases) {
    super(weight, keyName, false, description, aliases);
    this.keyId = keyId;
  }

  public String getKeyId() {
    return keyId;
  }
}
