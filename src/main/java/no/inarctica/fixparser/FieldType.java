package no.inarctica.fixparser;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing the FIX message fields we are interested in.
 */
public enum FieldType {

  UNKNOWN(-1),
  BEGINSTRING(8),
  BODYLENGTH(9),
  MSGTYPE(35),
  TIMESTAMP(52),
  SYMBOL(55),
  NUMENTRIES(268),
  BUYORSELL(269),
  PRICE(270),
  AMOUNT(271),
  CHECKSUM(10);

  private final int tag;

  // Make a static lookup Map for better performance.
  private static final Map<Integer, FieldType> lookup = new HashMap<>();

  static {
    for (FieldType s : EnumSet.allOf(FieldType.class)) {
      lookup.put(s.getInt(), s);
    }
  }

  FieldType(int tag) {
    this.tag = tag;
  }

  public int getInt() {
    return tag;
  }

  public static FieldType fromString(String code) {
    FieldType fieldType = lookup.get(Integer.parseInt(code));
    return fieldType == null ? UNKNOWN : fieldType;
  }
}