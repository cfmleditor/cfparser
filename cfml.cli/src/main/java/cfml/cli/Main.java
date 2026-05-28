package cfml.cli;

import java.io.File;
import cfml.parsing.CFMLParser;
import cfml.parsing.CFMLSource;
import net.htmlparser.jericho.Element;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: cfparser <file.cfm|file.cfc> ...");
            System.exit(1);
        }
        try {
            CFMLParser parser = new CFMLParser();
            for (String path : args) {
                File file = new File(path);
                if (!file.exists()) {
                    System.err.println("File not found: " + path);
                    continue;
                }
                CFMLSource source = parser.addCFMLSource(file);
                System.out.println("Parsed: " + path);
                for (Element tag : source.getAllElements()) {
                    System.out.println("  " + tag.getStartTag().getName()
                            + " [" + tag.getBegin() + "-" + tag.getEnd() + "]");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
