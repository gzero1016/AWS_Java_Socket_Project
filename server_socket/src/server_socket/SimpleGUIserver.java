package server_socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import server_socket.entity.Room;


public class SimpleGUIserver {
	
	public static List<ConnectedSocket> connectedSocketList = new ArrayList<>();
	public static List<Room> roomList = new ArrayList<>();	//리스트인 ConnectedSocket을 담는 Room 리스트를 생성
	
	public static void main(String[] args) {
		
		try {
			ServerSocket serverSocket = new ServerSocket(8000);
			System.out.println("[ 서버실행 ]");
			
			while(true) {
				Socket socket = serverSocket.accept();
				System.out.println("접속");
				ConnectedSocket connectedSocket = new ConnectedSocket(socket);	//Thread 객체
				connectedSocket.start();
				connectedSocketList.add(connectedSocket);	//생성된 Thread 객체 리스트에 넣기
			}
			
		} catch (IOException e) {
			e.printStackTrace(); 
		}
	}
}