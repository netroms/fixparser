package no.inarctica.fixparser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MessageTest {

  @Test
  public void testParseValidMessage() {
    String validMessage = "8=FIX.4.4\u00019=142\u000135=W\u000134=0\u000149=justtech\u000152=20180206-21:43:36.000\u0001"
        + "56=user\u0001262=TEST\u000155=EURUSD\u0001268=2\u0001269=0\u0001270=1.31678\u0001271=100000.0\u0001269=1\u0001"
        + "270=1.31667\u0001271=100000.0\u000110=057\u0001";

    Message m = Message.parse(validMessage, true, false);
    assertTrue("Message should be valid", m.isValid());
    assertTrue("Message should have valid checksum", m.isChecksumOk());
  }


  @Test
  public void testParseMessageWithWrongChecksum() {
    String validMessage = "8=FIX.4.4\u00019=142\u000135=W\u000134=0\u000149=--ERROR--\u000152=20180206-21:43:36.000\u0001"
        + "56=user\u0001262=TEST\u000155=EURUSD\u0001268=2\u0001269=0\u0001270=1.31678\u0001271=100000.0\u0001269=1\u0001"
        + "270=1.31667\u0001271=100000.0\u000110=057\u0001";

    Message m = Message.parse(validMessage, true, false);
    assertFalse("Message should not be valid", m.isValid());
    assertFalse("Message should have invalid checksum", m.isChecksumOk());
  }
}