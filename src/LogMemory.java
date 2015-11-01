import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.ArrayList;
import java.io.PrintWriter;

class LogMemory {

  static class MemorySlice {
    int memoryUsage;
    long realTime;
    long monoTime;

    MemorySlice(int mU, long rT, long mT) {
      memoryUsage = mU;
      realTime = rT;
      monoTime = mT;
    }
  }

  public static void main(String[] args) {
    Scanner fileReader=null;
    File rawMemData;
    long monotonicTime=0, realTime = 0;
    Map<String, Collection<MemorySlice>> memoryPerProcess = new HashMap<>();

    try {
      rawMemData = new File(args[0]);
      // Attempt to recover if no argument was submitted
      if (!rawMemData.exists()) {
        rawMemData = new File("meminfo.dat");
      }
      fileReader = new Scanner(rawMemData); // Could throw FNF Exception
      while (fileReader.hasNext()) {
        String currentLine = fileReader.nextLine().trim();
        String[] lineComponents = currentLine.split("\\s+");
        // Read the time synchronization data
        if (lineComponents[0].toLowerCase().equals("uptime:")) {
          monotonicTime = Long.parseLong(lineComponents[1]);
          realTime = Long.parseLong(lineComponents[3]);
        }

        // Read the relevant memory data
        if (lineComponents.length == 4 && lineComponents[3].equals("process:")) {
          currentLine = fileReader.nextLine().trim();
          lineComponents = currentLine.split("\\s+");
          while (!lineComponents[0].equals("Total") && !currentLine.equals("")) {
            int pssUsage = Integer.parseInt(lineComponents[0]);
            String processName = lineComponents[2];

            // Store the values of each process into a list in the map
            Collection<MemorySlice> values = memoryPerProcess.get(processName);
            if (values == null) {
              values = new ArrayList<MemorySlice>();
              memoryPerProcess.put(processName, values);
            }
            MemorySlice currentEntry = new MemorySlice(pssUsage, realTime, monotonicTime);
            values.add(currentEntry);

            currentLine = fileReader.nextLine().trim();
            lineComponents = currentLine.split("\\s+");
          }
        }
      }
    } catch (FileNotFoundException e) {
      System.out.println("File not found");
      System.out.println("usage: java LogMemory meminfo.dat");
      System.out.println("Terminating program.");
      System.exit(1);
    } finally {
      fileReader.close();
    }

    PrintWriter writer=null;
    try {
      writer = new PrintWriter("memInfo.csv");
      writer.println("Process-Name,Monotonic-Time,Real-Time,Memory-Usage");
      Set<String> keys = memoryPerProcess.keySet();
      for (String s : keys) {
        Collection<MemorySlice> slices = memoryPerProcess.get(s);
        writer.printf("%s,", s);
        for (MemorySlice m : slices) {
          writer.printf("%d,%d,%d", m.monoTime, m.realTime, m.memoryUsage);
        }
        writer.println();
      }
    } catch (FileNotFoundException e) {

    } finally {
      writer.close();
    }
  }

}
