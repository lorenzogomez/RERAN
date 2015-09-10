import java.io.*;
import java.util.Scanner;

public class CleanRecordedEvents {
  public static void main(String[] args) {
    Scanner fileReader;
    File f;
    try {
      f = new File(args[0]);
      fileReader = new Scanner(f);
      String finalLine = "";
      while (fileReader.hasNext()) {
        finalLine = fileReader.nextLine();
      }
      if (finalLine.split(" ").length != 7) {
        PrintWriter pw = new PrintWriter(f.getAbsolutePath() + ".dat");
        fileReader.close();
        Scanner reader = new Scanner(f);
        String line;
        while (reader.hasNextLine()) {
          line = reader.nextLine();
          if (reader.hasNextLine()) {
            pw.println(line);
          }
        }
        reader.close();
        pw.close();
      } else {
        fileReader.close();
      }
    } catch(FileNotFoundException e) {
      System.out.println("File not found");
      System.out.println("usage: java CleanRecordedEvents recordedEvents.txt");
      e.printStackTrace();
      System.out.println("Terminating program.");
      System.exit(1);
    }
  }
}
