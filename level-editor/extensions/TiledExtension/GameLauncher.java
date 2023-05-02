import javax.swing.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class GameLauncher {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Invalid initialization of Game Launcher");
            return;
        }

        boolean onMac = args[1].equals("macos");

        String tempFilePath = args[0];
        

        File configFile = new File("config.txt");
        if (!configFile.exists()) {
             throw new Exception("Run config first");
        }
        String symbiosisPath = null;

        //Read the jar location
        try (Scanner reader = new Scanner(configFile)) {
            symbiosisPath = reader.nextLine();
            System.out.println("Launching " + symbiosisPath);

        } catch (IOException e) {
            System.err.println("An error occurred while reading from the config.");
            e.printStackTrace();
        }

        //Launch Symbiosis        
        ProcessBuilder builder = new ProcessBuilder();
        if (onMac) {
            // Without this the application will just run without showing anything
            builder.command("java", "-XstartOnFirstThread", "-jar", symbiosisPath, tempFilePath);
        } else {
            builder.command("java", "-jar", symbiosisPath, tempFilePath);
        }
        builder.inheritIO();
        Process p = builder.start();
        System.out.print("Game Exit Code : " +p.waitFor());

    }
}
