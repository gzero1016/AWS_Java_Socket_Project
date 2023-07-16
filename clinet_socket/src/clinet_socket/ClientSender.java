package clinet_socket;

import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;

import clinet_socket.dto.RequestBodyDto;


public class ClientSender {
	
	private Gson gson;
	
	//싱글톤
	private static ClientSender instance;
	
	private ClientSender() {
		gson = new Gson();
	}
	
	public static ClientSender getInstance() {
		if(instance == null) {
			instance = new ClientSender();
		}
		return instance;
	}
	
	public void send(RequestBodyDto<?> requestBodyDto) {
		try {
			PrintWriter printWriter = 
					new PrintWriter(SimpleGUIClient.getInstance().getSocket().getOutputStream(), true);
			printWriter.println(gson.toJson(requestBodyDto));
			
		} 
		catch (IOException e) {
			e.printStackTrace(); 
		}
	}
}