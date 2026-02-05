cd src
javac -d ../bin main/java/com/groupcomm/server/*.java main/java/com/groupcomm/shared/*.java main/java/com/groupcomm/patterns/*.java
java -cp ../bin com.groupcomm.server.GroupCommunicationServer
