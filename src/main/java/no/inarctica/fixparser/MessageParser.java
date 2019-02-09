package no.inarctica.fixparser;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Class representing a limited FIX message.
 *
 * Call Message.parse(m) to parse and construct a message.
 */
class MessageParser {


  private static final String CHECKSUM_CHARSEQ = "\00110=";
  private static final String SOH_DELIMITER = "\\u0001";
  private static final String KV_DELIM = "=";

  private static final int CHECKSUM_TAG = 10;
  private static final int BUYORSELL_TAG = 269;
  private static final int TIMESTAMP_TAG = 52;

  // Private constructors.
  // Forces us to use static MessageParser.parse(m)
  private MessageParser() {
  }


  /**
   * Try to parse a full FIX message as specified.
   *
   * @param fullMessage The full message string to parse and possibly validate.
   * @return Returns either a Message instance, if parsing was ok, or null if it failed.
   */
  static String parse(final String fullMessage) {
    int calculatedChecksum = checksumFullMessage(fullMessage);
    // Split message on key/value pairs, delimited by SOH
    Iterator<String> i = Arrays.asList(fullMessage.split(SOH_DELIMITER)).iterator();

    String timestamp = "";
    final String[] groups = new String[2];

    int groupCounter = 0;

    while (i.hasNext()) {
      // Get next k/v pair...
      final String p = i.next();
      if (p.startsWith("10=") || p.startsWith("269=") || p.startsWith("52=")) {
        final String[] kv = p.split(KV_DELIM);
        if (kv.length == 2) {
          final int k = Integer.parseInt(kv[0]);
          final String v = kv[1];
          switch (k) {
            case CHECKSUM_TAG:
              int checksumInMessage = Integer.parseInt(v);
              if (calculatedChecksum != checksumInMessage) {
                return null;
              }
              break;
            case BUYORSELL_TAG:
              final String pricePair = i.next();
              final String amountPair = i.next();
              groups[groupCounter++] = (v.equals("0") ? " B " : " S ")
                  + pricePair.split(KV_DELIM)[1] + ";" + amountPair.split(KV_DELIM)[1];
              break;
            case TIMESTAMP_TAG:
              timestamp = v;
              break;
          }
        }
      }
    }

    return groups[0] + groups[1] + " " + timestamp;
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

}
