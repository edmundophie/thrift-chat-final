# thrift-chat
CLI Chat Program Based on Apache Thrift 

## Requirements
 - [Maven](https://maven.apache.org/download.cgi) installed
 - JRE >= 1.7

## How to Build
1. Resolve maven dependency  

	 ```
	 $ mvn dependency:copy-dependencies
	 ```
2. Build `jar` using maven `mvn`  

	 ```
	 $ mvn package
	 ```

## How to Run	 
1. Run `ChatServer` from the generated `jar` in `target` folder  

	 ```
	 $ java -cp target/dependency/*:target/thrift-chat-1.0-SNAPSHOT.jar if4031.chat.ChatServer
	 ```
2. Run `ChatClient` from the generated `jar` in `target` folder  

	 ```
	 $ java -cp target/dependency/*:target/thrift-chat-1.0-SNAPSHOT.jar if4031.chat.ChatClient
	 ```

## Testing
![alt text](https://github.com/edmundophie/thrift-chat-final/blob/master/testing-screenshot/1.png "Testing Screenshot 1")
![alt text](https://github.com/edmundophie/thrift-chat-final/blob/master/testing-screenshot/2.png "Testing Screenshot 2")

## Team Member
- Edmund Ophie 13512095
- Kevin 13512097
