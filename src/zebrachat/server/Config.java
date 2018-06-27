package zebrachat.server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
    private static final String CONFIG_FILENAME = "configuration.xml";
    private static Logger log = Logger.getLogger("zebrachat.server");
    private static int PORT;
    private static int RETAINED_MESSAGES_NUMBER;
    private static int MAXIMUM_CONNECTIONS;
    static Map<String, String> users = new HashMap<>();

    public static int getPort() {
        return PORT;
    }

    public static int getRetainedMessagesNumber() {
        return RETAINED_MESSAGES_NUMBER;
    }

    public static int getMaximumConnections() {
        return MAXIMUM_CONNECTIONS;
    }

    private static void assignServerValue(String xmlTagName, String value) {
        switch(xmlTagName) {
            case "port": PORT = Integer.valueOf(value); break;
            case "maximumConnections": MAXIMUM_CONNECTIONS = Integer.valueOf(value); break;
            case "retainedMessagesNumber": RETAINED_MESSAGES_NUMBER = Integer.valueOf(value); break;
        }
    }

    private static void readServerValues(Element server) {
        for(Node node = server.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element el = (Element) node;
            Text textNode = (Text) el.getFirstChild();
            String value = textNode.getData().trim();
            assignServerValue(el.getTagName(), value);
        }
    }

    private static void readUsersValues(Element usersElement) {
        for(Node node = usersElement.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element el = (Element) node;
            Element nameEl = (Element) el.getFirstChild();
            String name = nameEl.getTextContent().trim();
            Element pwEl = (Element) el.getLastChild();
            String pw = pwEl.getTextContent().trim();
            users.put(name, pw);
        }
    }

    private static void parseConfigurationFile() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setIgnoringElementContentWhitespace(true);
        Document document;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            File file = new File(CONFIG_FILENAME);
            document = builder.parse(file);
            Element root = document.getDocumentElement();
            Element server = (Element)root.getElementsByTagName("server").item(0);
            readServerValues(server);
            Element users = (Element) root.getElementsByTagName("users").item(0);
            readUsersValues(users);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while parsing configuration", e);
            System.out.println("Error: could not read configuration file.");
            System.exit(1);
        }
    }

    static {
        parseConfigurationFile();
    }
}
