package if4031.chat;

import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.*;

/**
 * Created by edmundophie on 9/16/15.
 */
public class ChatServer {
    public static ChatHandler handler;
    public static ChatService.Processor processor;

    public static void main(String[] args) {
        handler = new ChatHandler();
        processor = new ChatService.Processor(handler);
        Runnable simple = new Runnable() {
            public void run() {
                serve(processor);
            }
        };
        new Thread(simple).start();
    }

    public static void serve(ChatService.Processor processor) {
        try {
//            TServerTransport serverTransport = new TServerSocket(9090);
            TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(9090);
//            TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));
            TServer server = new TNonblockingServer(new TNonblockingServer.Args(serverTransport).processor(processor));
            System.out.println("Starting server...");
            server.serve();
        } catch (TTransportException e) {
            e.printStackTrace();
        }
    }
}
