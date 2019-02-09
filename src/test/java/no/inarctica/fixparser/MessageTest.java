package no.inarctica.fixparser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MessageTest {

  @Test
  public void testParseValidMessage() {
    String validMessage = "8=FIX.4.4\u00019=142\u000135=W\u000134=0\u000149=justtech\u000152=20180206-21:43:36.000\u0001"
        + "56=user\u0001262=TEST\u000155=EURUSD\u0001268=2\u0001269=0\u0001270=1.31678\u0001271=100000.0\u0001269=1\u0001"
        + "270=1.31667\u0001271=100000.0\u000110=057\u0001";

    String m = MessageParser.parse(validMessage);
    assertNotNull("Message should be valid i.e not null", m);
  }

  @Test
  public void testParseMessageWithWrongChecksum() {
    String validMessage = "8=FIX.4.4\u00019=142\u000135=W\u000134=0\u000149=--ERROR--\u000152=20180206-21:43:36.000\u0001"
        + "56=user\u0001262=TEST\u000155=EURUSD\u0001268=2\u0001269=0\u0001270=1.31678\u0001271=100000.0\u0001269=1\u0001"
        + "270=1.31667\u0001271=100000.0\u000110=057\u0001";

    String m = MessageParser.parse(validMessage);
    assertNull("Message should not be valid, i.e null", m);
  }
}