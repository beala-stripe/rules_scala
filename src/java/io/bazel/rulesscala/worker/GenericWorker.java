package io.bazel.rulesscala.worker;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class GenericWorker {
  protected final Processor processor;

  public GenericWorker(Processor p) {
    processor = p;
  }

  protected void setupOutput(PrintStream ps) {
    System.setOut(ps);
    System.setErr(ps);
  }

  // Mostly lifted from bazel
  private void runPersistentWorker() throws IOException {
    PrintStream originalStdOut = System.out;
    PrintStream originalStdErr = System.err;

    while (true) {
      try {
        System.out.println("About to parse");
        System.out.flush();
        WorkRequest request = WorkRequest.parseDelimitedFrom(System.in);
        System.out.println("Done parsing");
        System.out.flush();
        if (request == null) {
          System.out.println("Breaking");
          System.out.flush();
          break;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int exitCode = 0;

        try (PrintStream ps = new PrintStream(baos)) {
          setupOutput(ps);

          try {
            System.out.println("Kicking off request");
            System.out.flush();
            processor.processRequest(request.getArgumentsList());
            System.out.println("Done with request");
            System.out.flush();
          } catch (Exception e) {
            e.printStackTrace();
            System.err.flush();
            exitCode = 1;
          }
        } finally {
          System.setOut(originalStdOut);
          System.setErr(originalStdErr);
        }

        WorkResponse.newBuilder()
            .setOutput(baos.toString())
            .setExitCode(exitCode)
            .build()
            .writeDelimitedTo(System.out);
        System.out.flush();
      } catch (Exception e) {
        e.printStackTrace();
        System.err.flush();
        throw e;
      } finally {
        System.gc();
      }
    }
  }

  public static <T> String[] appendToString(String[] init, List<T> rest) {
    String[] tmp = new String[init.length + rest.size()];
    System.arraycopy(init, 0, tmp, 0, init.length);
    int baseIdx = init.length;
    for (T t : rest) {
      tmp[baseIdx] = t.toString();
      baseIdx += 1;
    }
    return tmp;
  }

  public static String[] merge(String[]... arrays) {
    int totalLength = 0;
    for (String[] arr : arrays) {
      totalLength += arr.length;
    }

    String[] result = new String[totalLength];
    int offset = 0;
    for (String[] arr : arrays) {
      System.arraycopy(arr, 0, result, offset, arr.length);
      offset += arr.length;
    }
    return result;
  }

  private boolean contains(String[] args, String s) {
    for (String str : args) {
      if (str.equals(s)) return true;
    }
    return false;
  }

  private static List<String> normalize(List<String> args) throws IOException {
    if (args.size() == 1 && args.get(0).startsWith("@")) {
      return Files.readAllLines(Paths.get(args.get(0).substring(1)), UTF_8);
    } else {
      return args;
    }
  }

  /** This is expected to be called by a main method */
  public void run(String[] argArray) throws Exception {
    if (contains(argArray, "--persistent_worker")) {
      runPersistentWorker();
    } else {
      List<String> args = Arrays.asList(argArray);
      processor.processRequest(normalize(args));
    }
  }
}
