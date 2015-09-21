package if4031.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.*;
import java.util.*;

/**
 * Created by edmundophie on 9/15/15.
 */
public class ChatClient {
    private static boolean isLoggedIn = false;
    private static String nickname = null;
    private static ChatService.Client client;
    private static Thread messageListenerThread;
    private static Long lastReceivedMessageTimestamp;

    public static void main(String[] args) {
        try {
            TTransport transport = new TFramedTransport(new TSocket("localhost", 9090));
            transport.open();

            TProtocol protocol = new TBinaryProtocol(transport);
            client = new ChatService.Client(protocol);

            perform(client);

            transport.close();
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void perform(ChatService.Client client) throws TException, InterruptedException, IOException {
        System.out.println("Client started");
        client.ping();
        String command;
        do {
            String consoleNickname = (!isLoggedIn)?"$ ":nickname+"$ ";
//            System.out.print(consoleNickname);

            String input = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();

            String parameter = null;
            int i = input.indexOf(" ");

            if(i>-1) {
                command = input.substring(0, i);
                parameter = input.substring(i + 1);
            } else
                command=input;

            if(command.equalsIgnoreCase("NICK")) {
                login(parameter);
            }
            else if(command.equalsIgnoreCase("JOIN")) {
                join(parameter);
            }
            else if(command.equalsIgnoreCase("LEAVE")) {
                leave(parameter);
            }
            else if(command.equalsIgnoreCase("LOGOUT")) {
                logout();
            }
            else if(command.equalsIgnoreCase("EXIT")) {
                exit();
            }
            else if(command.charAt(0)=='@') {
                sendMessageToChannel(command.substring(1), parameter);
            }
            else {
                sendMessage(input);
            }
        } while (!command.equalsIgnoreCase("EXIT"));
    }

    private static void printInvalidCommand() {
        System.err.println("Invalid Command!");
    }

    private static void login(String parameter) throws TException {
        if(isLoggedIn)
            System.err.println("Please logout first!");
        else {

            nickname = client.login(parameter);
            isLoggedIn = true;
            lastReceivedMessageTimestamp = System.currentTimeMillis()-1;
            System.out.println("Login successfull as " + nickname + " !");

            Runnable messageListener = new Runnable() {
                public void run() {
                    System.out.println("Message listener started...");
                    while (isLoggedIn) {
                            String messages = getMessages();
                            if (messages != null && !messages.isEmpty()) {
                                try {
                                    List<Message> messageList = new ObjectMapper().readValue(messages, new TypeReference<List<Message>>() {
                                    });
                                    Collections.sort(messageList);

                                    lastReceivedMessageTimestamp = messageList.get(messageList.size() - 1).getTimestamp();

                                    for(Message msg:messageList)
                                        System.out.println(msg.getText());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                }
            };

            messageListenerThread = new Thread(messageListener);
            messageListenerThread.start();
        }
    }

    private static void logout() throws TException {
        boolean result;
        synchronized (client) {
            result = client.exit(nickname);
        }
        if(result) {
            nickname = "";
            isLoggedIn = false;
            System.out.println("You have been logged out!");
        }
        else
            System.err.println("Failed to log out!");
    }

    private static void exit() throws TException, InterruptedException {
        logout();
        messageListenerThread.join();
    }

    private static void join(String parameter) throws TException, InterruptedException {
        if(!isLoggedIn) System.err.println("Please login first!");
        else if(parameter==null) printInvalidCommand();
        else {
            boolean result;
            synchronized (client) {
                result = client.join(nickname, parameter);
            }
            if(result) {
                System.out.println("#" + parameter + " joined successfully");
                // TODO post-join action. Retrieve channel messages?
            }
            else
                System.err.println("You are already a member of #" + parameter + " !");
        }
    }

    private static void leave(String parameter) throws TException {
        if(!isLoggedIn) System.err.println("Please login first!");
        else if(parameter==null) printInvalidCommand();
        else  {
            boolean result;
            synchronized (client) {
                result = client.leave(nickname, parameter);
            }
            if(result) {
                System.out.println("You are no longer member of #" + parameter);
            }
            else
                System.err.println("Failed to leave #" + parameter);
        }
    }

    private static void sendMessage(String parameter) throws TException, InterruptedException {
        if(!isLoggedIn) System.err.println("Please login first!");
        else {
            boolean result;
            synchronized (client) {
                result = client.sendMessage(nickname, parameter);
            }
            if (result) {
//                System.out.println("Your message has been sent"); // TODO post-deliver action?
            }
            else
                System.err.println("Failed to send message!");
        }
    }

    private static void sendMessageToChannel(String channelName, String parameter) throws TException {
        if(!isLoggedIn) System.err.println("Please login first!");
        else if(parameter==null) printInvalidCommand();
        else {
            boolean result;
            synchronized (client) {
                result = client.sendMessageToChannel(nickname, parameter, channelName);
            }
            if (result) {
//                System.out.println("Your message has been sent"); // TODO post-deliver action?
            }
            else
                System.err.println("Failed to send message!");
        }
    }

    private static String getMessages() {
        String messages = null;
        try {
            synchronized (client) {
                messages = client.getAllMessage(nickname, lastReceivedMessageTimestamp);
            }
        } catch (TException e) {
            e.printStackTrace();
        }

        return messages;
    }
}
