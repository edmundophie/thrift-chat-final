package if4031.chat;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.thrift.TException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by edmundophie on 9/16/15.
 */
public class ChatHandler implements ChatService.Iface{
    private static final String USER_FILE_NAME = "userToChannel.json";
    private static final String CHANNEL_FILE_NAME = "channelMessages.json";
    private static File userFile;
    private static File channelFile;
    private static Map<String, User> userMap;
    private static Map<String, Channel> channelMap;
    private static Map<String, List<Message>> messageListMap;

    public ChatHandler() {
        //            userFile = new File(this.getClass().getResource("/" + USER_FILE_NAME).getFile());
//            userMap = new ObjectMapper().readValue(userFile, new TypeReference<Map<String, User>>(){});
//            channelFile = new File(this.getClass().getResource("/" + CHANNEL_FILE_NAME).getFile());
//            channelMap = new ObjectMapper().readValue(channelFile, new TypeReference<Map<String, Channel>>() {});
        userMap =  new HashMap<String, User>();
        channelMap =  new HashMap<String, Channel>();
        messageListMap = new HashMap<String, List<Message>>();
    }

    public void ping() throws TException {
        System.out.println("Ping received from client");
    }

    public String login(String nickname) throws TException {
        System.out.println("- Login method invoked");
        String result;

        if(nickname!=null && !userMap.containsKey(nickname))
            result = nickname;
        else
            result = generateRandomNickname();

        userMap.put(result, new User(result));
//        saveMapToFile(userMap, userFile); // TODO async

        return result;
    }

    public boolean join(String nickname, String channelName) throws TException {
        System.out.println("- " + nickname + " requested to join #" + channelName);
        List userChannelList = userMap.get(nickname).getJoinedChannel();

        if(!channelMap.containsKey(channelName)) {
            channelMap.put(channelName, new Channel(channelName));
//            saveMapToFile(channelMap, channelFile); // TODO async
            messageListMap.put(channelName, new ArrayList<Message>());
        }

        if(!userChannelList.contains(channelName)) {
            userChannelList.add(channelName);
//            saveMapToFile(userMap, userFile); // TODO async
            return true;
        }

        return false;
    }

    public boolean leave(String nickname, String channelName) throws TException {
        System.out.println("- " + nickname + " left #" + channelName);
        if(!userMap.get(nickname).getJoinedChannel().contains(channelName)) {
            System.err.println("- Failed to leave channel. " + nickname + " is not a member of #" + channelName);
            return false;
        }
        userMap.get(nickname).getJoinedChannel().remove(channelName);
        return true;
    }

    public boolean exit(String nickname) throws TException {
        System.out.println("- " + nickname + " logged out");
        userMap.remove(nickname);
//        saveMapToFile(userMap, userFile); // TODO async
        return true;
    }

    public boolean sendMessage(String nickname, String message) throws TException {
        System.out.println("- " + nickname + " sent a message");

        List<String> userChannelList = userMap.get(nickname).getJoinedChannel();
        if(userChannelList.size()==0) {
            System.err.println("- Failed to send " + nickname + " message. No channel found.");
            return false;
        }

        Message msg = new Message(nickname, message);
        for(String channelName:userChannelList) {
            channelMap.get(channelName).getMessages().add(msg);
            Message temp = new Message(nickname, "@" + channelName + " " + nickname + ": " + msg.getText());
            messageListMap.get(channelName).add(temp);
        }

//        saveMapToFile(channelMap, channelFile); // TODO async
        return true;
    }

    public boolean sendMessageToChannel(String nickname, String message, String channelName) throws TException {
        System.out.println("- " + nickname + " sent a message to #" + channelName);
        List<String> userChannelList = userMap.get(nickname).getJoinedChannel();
        if(!userChannelList.contains(channelName)) {
            System.err.println("- Failed to send " + nickname + " message to #" + channelName + ". User is not registered to the channel.");
            return false;
        }

        Message msg = new Message(nickname, message);
        channelMap.get(channelName).getMessages().add(msg);
        msg.setText("@" + channelName + " " + nickname + ": " + msg.getText());
        messageListMap.get(channelName).add(msg);

//        saveMapToFile(channelMap, channelFile); // TODO async
        return true;
    }

    public String getAllMessage(String nickname, long lastReceivedMessageTimestamp) throws TException {

        if(nickname.isEmpty()) return "";
        List<Message> response = new ArrayList<Message>();

        for(String channelName:userMap.get(nickname).getJoinedChannel()) {
            List<Message> messageList = messageListMap.get(channelName);

            if(!messageList.isEmpty()) {
                int low=0;
                int high = messageList.size()-1;

                while(low!=high) {
                    int mid = (low+high)/2;
                    if(messageList.get(mid).getTimestamp()<=lastReceivedMessageTimestamp)
                        low+=1;
                    else
                        high=mid;
                }

                if(messageList.get(low).getTimestamp()>lastReceivedMessageTimestamp)
                    response.addAll(messageList.subList(low, messageList.size()));
            }
        }

        String responseMessage = "";
        try {
            if(response!=null && !response.isEmpty()) {
                responseMessage = new ObjectMapper().writeValueAsString(response);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println(responseMessage);
        return responseMessage;
    }

    public String getMessage(String nickname, String channelName, long lastReceivedMessageTimestamp) throws TException {
        return null;
    }

    private String generateRandomNickname() {
        String newNickname;
        Random random = new Random();
        do {
            newNickname = "user" + random.nextInt(99999);
        }while(userMap.containsKey(newNickname));

        return newNickname;
    }

    private void saveMapToFile(Object sourceMap, File destinationFile) {
        System.out.println("- Saving " + destinationFile.getName());
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            writer.writeValue(destinationFile, sourceMap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
