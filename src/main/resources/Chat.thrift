namespace java if4031.chat

typedef i32 int
typedef string String
typedef bool boolean 

service ChatService
{
	void ping(),
	String login(1:String nickname),
	String join(1:String channelName),
	boolean leave(1:String channelName),
	boolean exit(1:String channelName),
	boolean sendMessage(),
	boolean sendMessageToChannel(1:String message, 2:String channelName)
}