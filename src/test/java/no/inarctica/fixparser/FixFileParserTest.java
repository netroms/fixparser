package no.inarctica.fixparser;

import static org.junit.Assert.assertEquals;

import no.inarctica.fixparser.FixFileParser.ResultWrapper;
import org.junit.Test;

public class FixFileParserTest {

  private static final String DEFAULT_DATA_FILENAME = "example-fix-data.bin";
  private static final String DEFAULT_DATA_FILENAME_WITH_ONE_CHECKSUM_FAIL = "example-fix-data-with-1-wrong-checksum.bin";

  @Test
  public void testParseValidFile() throws Exception {

    final FixFileParser fixFileParser = new FixFileParser();
    final ResultWrapper resultWrapper = fixFileParser.readFile(DEFAULT_DATA_FILENAME);

    final int totalParsedOk = resultWrapper.parsed.get();
    final int totalRead = resultWrapper.read.get();
    final int totalFail = resultWrapper.fail.get();

    assertEquals("Should parse 10 messages OK", 10, totalParsedOk);
    assertEquals("Should read 10 ", 10, totalRead);
    assertEquals("Should fail 0 ", 0, totalFail);
  }

  @Test
  public void testParseFileWithOneChecksumFail() throws Exception {

    final FixFileParser fixFileParser = new FixFileParser();
    final ResultWrapper resultWrapper = fixFileParser.readFile(DEFAULT_DATA_FILENAME_WITH_ONE_CHECKSUM_FAIL);

    final int totalParsedOk = resultWrapper.parsed.get();
    final int totalRead = resultWrapper.read.get();
    final int totalFail = resultWrapper.fail.get();

    assertEquals("Should parse 9 messages OK", 9, totalParsedOk);
    assertEquals("Should read 10 ", 10, totalRead);
    assertEquals("Should fail 1 ", 1, totalFail);
  }

}