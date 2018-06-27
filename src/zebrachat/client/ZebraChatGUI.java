package zebrachat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import zebrachat.protocol.ChatMessage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ZebraChatGUI extends Application {
    private ZebraClient client;
    private TextArea chatText;
    private TextField inputText;
    private DateTimeFormatter timeFormatter;
    private Button connectButton;
    private Stage connectDialog;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setScene(createScene());
        primaryStage.show();

        timeFormatter = DateTimeFormatter.ofPattern("k:m:s");
        connectDialog = createDialog(primaryStage);
        connectDialog.showAndWait();
        connectButton.setOnAction((ActionEvent e) -> connectDialog.showAndWait());
        inputText.setOnAction((ActionEvent e) -> sendMessage());

        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                client.listenForMessages((ChatMessage msg) -> displayMessage(msg));
                return null;
            }
        };
        Thread messageThread = new Thread(task);
        messageThread.setDaemon(true);
        messageThread.start();
    }

    private Scene createScene() {
        GridPane grid = setUpGrid();

        connectButton = new Button("Join chat");
        HBox hBox = new HBox(connectButton);
        hBox.setAlignment(Pos.CENTER);
        grid.add(hBox, 0, 0);

        inputText = new TextField();
        grid.add(inputText, 0, 2);

        chatText = new TextArea();
        chatText.setEditable(false);
        chatText.setFocusTraversable(false);
        chatText.setPrefSize(400, 300);
        chatText.setWrapText(true);
        chatText.setOnKeyPressed((KeyEvent e) -> inputText.requestFocus());
        grid.add(chatText, 0, 1);

        return new Scene(grid);
    }

    private GridPane setUpGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setGridLinesVisible(false);
        return grid;
    }

    private void displayMessage(ChatMessage msg) {
        Platform.runLater(() -> {
            String time = msg.getReceiptTime().format(timeFormatter);
            if (msg.getUsername() != null) {
                chatText.appendText("[" + time + "] " + msg.getUsername() + ": " + msg.getText() + "\n");
            } else {
                chatText.appendText("[" + time + "] " + msg.getText() + "\n");
            }
            chatText.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void connectToServer(String hostName, String username, String password) throws Exception {
        client = new ZebraClient(hostName, username, password);
        if (!client.requestLogin()) {
            chatText.appendText("Wrong login\n");
        } else if (!client.receivePermission()) {
            chatText.appendText("Chat is full\n");
        }
    }

    private void sendMessage() {
        try {
            if (inputText.getText().isEmpty()) return;
            client.sendChatMessage(inputText.getText().trim());
        } catch (IOException ex) {
            chatText.appendText("Connection error!\n");
        }
        inputText.clear();
    }

    private Stage createDialog(Stage parentStage) {
        Stage dialog = new Stage();
        dialog.initOwner(parentStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(15);

        Label host = new Label("Host:");
        grid.add(host, 0, 0);
        TextField hosttext = new TextField();
        grid.add(hosttext, 1, 0);
        Label port = new Label("Port:");
        grid.add(port, 2, 0);
        TextField porttext = new TextField("1234");
        grid.add(porttext, 3, 0);

        Label name = new Label("Username:");
        grid.add(name, 0, 1);
        TextField nametext = new TextField();
        grid.add(nametext, 1, 1);
        Label pw = new Label("Password:");
        grid.add(pw, 2, 1);
        TextField pwtext = new PasswordField();
        grid.add(pwtext, 3, 1);

        DropShadow redGlow = new DropShadow();
        redGlow.setColor(Color.RED);
        redGlow.setOffsetY(0);
        redGlow.setOffsetX(0);
        redGlow.setWidth(50);
        redGlow.setHeight(50);

        Button ok = new Button("OK");
        HBox hBox = new HBox(ok);
        hBox.setAlignment(Pos.CENTER);
        ok.setOnAction(event -> {
            boolean fullInput = true;
            if(hosttext.getText().isEmpty()) {
                hosttext.setEffect(redGlow);
                fullInput = false;
            }
            if(porttext.getText().isEmpty()) {
                porttext.setEffect(redGlow);
                fullInput = false;
            }
            if(nametext.getText().isEmpty()) {
                nametext.setEffect(redGlow);
                fullInput = false;
            }
            if(pwtext.getText().isEmpty()) {
                pwtext.setEffect(redGlow);
                fullInput = false;
            }

            String hostName, username, password;
            if (fullInput) {
                hostName = hosttext.getText().trim();
                username = nametext.getText().trim();
                password = pwtext.getText().trim();
                dialog.hide();

                try {
                    connectToServer(hostName, username, password);
                    chatText.clear();
                } catch (Exception e) {
                    chatText.appendText("Could not connect to chat. Try again.\n");
                }
            }
        });
        grid.add(hBox, 3, 2);

        dialog.setScene(new Scene(grid));
        return dialog;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
