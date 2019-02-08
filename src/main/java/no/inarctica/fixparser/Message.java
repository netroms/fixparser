package no.inarctica.fixparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;

/**
 * Class representing a limited FIX message.
 *
 * Call Message.parse(m) to parse and construct a message.
 */
class Message {

  private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(Message.class);


  private static final String CHECKSUM_CHARSEQ = "\00110=";
  private static final String SOH_DELIMITER = "\\u0001";
  private static final Pattern VALID_KEY = Pattern.compile("-?\\d+?");
  private static final FieldType GROUP_START_TAG = FieldType.BUYORSELL;

  private final ArrayList<FieldGroup> groups = new ArrayList<>();
  private final TreeMap<Integer, Field> fieldMap = new TreeMap<>();

  private boolean checksumOk = false;
  private boolean syntaxOk = true;


  /**
   * Class representing a group of fields
   */
  static private class FieldGroup {

    final TreeMap<Integer, Field> fieldMap = new TreeMap<>();

    static FieldGroup makeGroup(String... all) {
      FieldGroup fieldGroup = new FieldGroup();
      for (String p : all) {
        String[] kv = p.split("=");
        String k = kv[0];
        String v = kv[1];
        Field f = new Field(k, v);
        fieldGroup.fieldMap.put(f.getKey().getInt(), f);
      }
      return fieldGroup;
    }

    String getValue(FieldType fieldType) {
      return getField(fieldType).getValue();
    }

    Field getField(FieldType fieldType) {
      return this.fieldMap.get(fieldType.getInt());
    }
  }

  /**
   * Class representing a 'valid' field
   */
  static private class Field {

    FieldType key;
    String value;

    Field(FieldType fieldType, String v) {
      this.key = fieldType;
      this.value = v;
    }

    Field(String k, String v) {
      this.key = FieldType.fromString(k);
      this.value = v;
    }

    FieldType getKey() {
      return key;
    }

    String getValue() {
      return value;
    }
  }


  // Private constructors.
  // Forces us to use static Message.parse(m)
  private Message() {
  }


  private String getValue(FieldType fieldType) {
    return getField(fieldType).getValue();
  }

  private Field getField(FieldType fieldType) {
    return this.fieldMap.get(fieldType.getInt());
  }


  public boolean isValid() {
    return syntaxOk && checksumOk;
  }

  public boolean isChecksumOk() {
    return checksumOk;
  }

  public boolean isSyntaxOk() {
    return syntaxOk;
  }

  /**
   * Try to parse a full FIX message as specified.
   *
   * @param fullMessage The full message string to parse and possibly validate.
   * @param validateChecksum Calculate checksum and compare with checksum in message
   * @param validateSyntax Validate message syntax is correct.
   * @return Returns either a Message instance, if parsing was ok, or null if it failed.
   */
  static Message parse(String fullMessage, boolean validateChecksum, boolean validateSyntax) {

    Message m = new Message();

    // Split message on key/value pairs, delimited by SOH
    Iterator<String> i = Arrays.asList(fullMessage.split(SOH_DELIMITER)).iterator();

    while (i.hasNext()) {
      // Get next k/v pair...
      String p = i.next();

      String[] kv = p.split("=");
      if (kv.length == 2) {
        String k = kv[0];

        if (!validateSyntax || isValidKey(k)) {
          FieldType fieldType = FieldType.fromString(k);

          // only process known/defined tags
          if (FieldType.UNKNOWN != fieldType) {
            Field knownFieldType = new Field(fieldType, kv[1]);

            // Assumption, if it is a group field start, process whole sequence
            // (since spec says it always comes in the same sequence)
            if (GROUP_START_TAG == fieldType) {
              m.groups.add(FieldGroup.makeGroup(p, i.next(), i.next()));

            } else {
              m.fieldMap.put(knownFieldType.getKey().getInt(), knownFieldType);
            }
          }

        } else {
          m.syntaxOk = false;
          logger.error(String.format("Failed to parse key to int; "
              + "key=%s, fullMessage=%s", k, fullMessage));
        }

      } else {
        if (validateChecksum) {
          m.syntaxOk = false;
          logger.error(String.format("Failed to parse key/value pair, "
                  + "split on '=' failed; p=%s, kv.length=%s, fullMessage=%s",
              p, kv.length, fullMessage));
        }
      }
    }

    if (validateChecksum) {
      int calculatedChecksum = checksumFullMessage(fullMessage);
      int checksumInMessage = Integer.parseInt(m.getValue(FieldType.CHECKSUM));
      if (calculatedChecksum == checksumInMessage) {
        m.checksumOk = true;
      } else {
        logger.error(String.format("Checksum failure; calculated=%s, message=%s, fullMessage=%s",
            calculatedChecksum, checksumInMessage, fullMessage));
      }
    }

    return m;
  }


  private static int checksumFullMessage(String data) {
    int sum = 0;
    int end = data.lastIndexOf(CHECKSUM_CHARSEQ);
    int len = end > -1 ? end + 1 : data.length();
    for (int i = 0; i < len; i++) {
      sum += data.charAt(i);
    }
    return sum & 0xFF;
  }

  private static boolean isValidKey(String str) {
    return VALID_KEY.matcher(str).matches();
  }


  // Generate string of parsed message to be logged
  String getOutput() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getValue(FieldType.SYMBOL));
    for (FieldGroup g : this.groups) {
      Field buyOrSell = g.getField(FieldType.BUYORSELL);
      sb.append(buyOrSell.getValue().equals("0") ? " B " : " S ");
      sb.append(g.getValue(FieldType.PRICE));
      sb.append(";").append(g.getValue(FieldType.AMOUNT));
    }
    sb.append(" ").append(getValue(FieldType.TIMESTAMP));
    return sb.toString();
  }
}
