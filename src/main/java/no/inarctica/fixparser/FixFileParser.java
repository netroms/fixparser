package no.inarctica.fixparser;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Logger;

/**
 * Main class.
 *
 * This class optionally takes a file as input and parses it and logs some of the message content, if message is parsed correctly.
 */
public class FixFileParser {

  private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(FixFileParser.class);


  private static int ASYNC_PARSE = 0;
  private static boolean LOGGING_ENABLED = true;
  private final static String MSG_START = "8=FIX";
  private final static String DOT = ".";
  private static final String DEFAULT_DATA_FILENAME = "example-fix-data.bin";

  private static ForkJoinPool forkJoinPool;
  private static ExecutorService executorService;


  public static void main(String[] args) throws Exception {
    String fileName;
    if (args.length > 0) {
      fileName = args[0];
    } else {
      log.warn("No parameters supplied!");
      printUsage();
      log.warn("Parsing default supplied data file; filename = " + DEFAULT_DATA_FILENAME + "\n");
      fileName = DEFAULT_DATA_FILENAME;
    }

    if (args.length > 1) {
      if (args[1].equals("0")) {
        LOGGING_ENABLED = false;
        log.info("!!! Disabling logging... !!!");
      }
    }
    if (args.length > 2) {
      try {
        ASYNC_PARSE = Integer.parseInt(args[2]);
      } catch (NumberFormatException e) {
        log.error("Failed to parse supplied ASYNC parameter; See usage...");
        printUsage();
      }
    }

    FixFileParser fixParser = new FixFileParser();

    // read and parse file
    log.info("\nStarting to try parse input file = " + fileName);
    ResultWrapper resultWrapper = fixParser.readFile(fileName);
    resultWrapper.printResult();
    log.info("Program execution finished... Goodbye! ");
  }

  private static void printUsage() {
    log.warn("Usage (all parameters are optional): [filename] [(log_or_not):1|0] "
        + "[(threadpool_type):1(ForkJoin threadpool)|2(Fixed threadpool)");
  }


  public FixFileParser() {
    // Tweak logging for max performance,
    // make all loggers async (so we don't need to define async appender and chain them etc. in log4j2.xml)
    // This is why we need the disruptor lib dependency. See: https://logging.apache.org/log4j/2.x/manual/async.html#AllAsync
    System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
    // Increase asynchronous logger buffer size to 1M messages
    System.setProperty("AsyncLogger.RingBufferSize", "1048576");

    // for experimenting with different thread pool types.
    setAsyncThreadPoolType();
  }

  static class ResultWrapper {

    long totalElapsedTime;
    long startTime = Instant.now().toEpochMilli();
    AtomicInteger fail = new AtomicInteger(0);
    AtomicInteger read = new AtomicInteger(0);
    AtomicInteger parsed = new AtomicInteger(0);

    void setTotalElapsedTime(long totalElapsedTime) {
      this.totalElapsedTime = totalElapsedTime;
    }

    void printResult() {
      // Sum up and log results to the console
      log.info("------------------------------------------------------------------");
      log.info("Total execution time was = " + totalElapsedTime + " (ms)");
      log.info("Total messages read = " + read.get());
      log.info("Total messages parsed = " + parsed.get());
      log.info("Failed to parse = " + fail.get());
      log.info(String.format("Messages parsed/second = %,.2f",
          (parsed.get() / ((double) totalElapsedTime / 1000.0d))));
      log.info("------------------------------------------------------------------");
    }
  }

  public ResultWrapper readFile(String fileName) throws Exception {
    log.info("------------------------------------------------------------------");

    ResultWrapper result = new ResultWrapper();

    Scanner scanner = new Scanner(Paths.get(fileName));
    // set delimiter regexp!
    scanner.useDelimiter(MSG_START + "\\.");

    while (scanner.hasNext()) {

      // Get message
      final String fullMessage = MSG_START + DOT + scanner.next();
      result.read.incrementAndGet();

      if (ASYNC_PARSE == 0) {
        // Use no async parsing, parse message synchronously on main thread.
        handleMessage(result, fullMessage);

      } else {
        // Try parse messages on a thread pool
        final Runnable runnable = () -> handleMessage(result, fullMessage);

        if (ASYNC_PARSE == 1) {
          forkJoinPool.execute(runnable);
        } else if (ASYNC_PARSE == 2) {
          executorService.execute(runnable);
        }
      }

    }
    scanner.close();

    // If async is enabled, shutdown the thread pool,
    // wait/sleep for unfinished tasks still in queue.
    long totalElapsedTime = Instant.now().toEpochMilli() - result.startTime;
    if (ASYNC_PARSE == 1) {
      while (forkJoinPool.getQueuedSubmissionCount() > 0) {
        waitForThreadPool(forkJoinPool.getQueuedSubmissionCount());
      }
      totalElapsedTime = Instant.now().toEpochMilli() - result.startTime;
      forkJoinPool.shutdown();

    } else if (ASYNC_PARSE == 2) {
      ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
      while (tpe.getQueue().size() > 0) {
        waitForThreadPool(tpe.getQueue().size());
      }
      totalElapsedTime = Instant.now().toEpochMilli() - result.startTime;
      tpe.shutdown();
    }

    result.setTotalElapsedTime(totalElapsedTime);

    return result;
  }

  private void setAsyncThreadPoolType() {
    switch (ASYNC_PARSE) {
      case 0:
        log.info("!!! Single threaded synchronous parsing!!!");
        break;
      case 1:
        forkJoinPool = ForkJoinPool.commonPool();
        log.info("!!! Use fork join common pool.. !!!");
        break;
      case 2:
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
        log.info("!!! Use Executors.newFixedThreadPool().. !!!");
        break;
    }
  }

  private void handleMessage(ResultWrapper result, String fullMessage) {
    final Message message = Message.parse(fullMessage, true, false);
    if (!message.isValid()) {
      result.fail.incrementAndGet();
    } else {
      result.parsed.incrementAndGet();
      if (LOGGING_ENABLED) {
        log.info(message.getOutput());
      }
    }
  }

  private void waitForThreadPool(int count) throws InterruptedException {
    Thread.sleep(1000);
    log.info("Sleeping a little bit (1s), waiting for "
        + "the thread pool to finish work in queue; queue.size()=" + count);
  }
}
