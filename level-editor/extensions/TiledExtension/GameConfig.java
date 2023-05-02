import javax.swing.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class GameConfig {

    public static void main(String[] args) throws IOException {

        File configFile = new File("config.txt");
        if (!configFile.exists()) {
            // This branch should only run once ideally

            //Make containing frame for file chooser
            JFrame frame = new JFrame("Symbiosis Finder");
            frame.setSize(100, 100);
            frame.setLocationRelativeTo(null);
            // This allows the window to close without preventing program execution
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setVisible(true);

            final JFileChooser fc = new JFileChooser();

            // Let user pick their jar file
            fc.setName("Select the Symbiosis JAR");
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            } else {
                System.err.println("Symbiosis JAR selection not completed");
                return;
            }

            String symbiosisPath = fc.getSelectedFile().getCanonicalPath();

            // Write to file
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(symbiosisPath);
                writer.close();
                System.out.println("Successfully initialized config.");

            } catch (IOException e) {
                System.err.println("An error occurred with config initialization.");
                e.printStackTrace();
            }
        }
    
    }
}
