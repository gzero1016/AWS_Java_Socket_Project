package clinet_socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import clinet_socket.dto.RequestBodyDto;
import clinet_socket.dto.SendMessage;

public class ClientReceiver extends Thread {
	
	private Gson gson;

	
	@Override
	public void run() {
		gson = new Gson();
		
		SimpleGUIClient simpleGUIClient = SimpleGUIClient.getInstance();
		//SimpleGUIClient를 싱글톤으로 만들어야 사용가능하다.
		
		while(true) {
			try {
				BufferedReader bufferedReader = 
						new BufferedReader(new InputStreamReader
								(SimpleGUIClient.getInstance().getSocket().getInputStream()));
				String requestBody = bufferedReader.readLine();
				
				requestController(requestBody);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void requestController(String requestBody) {

		String resource = gson.fromJson(requestBody, RequestBodyDto.class).getResource();	//해당 주소에있는 값 가져오기
		
		switch(resource) {
			case "updateRoomList":
				updateRoomList(requestBody);
				break;
				
			case "showMessage":
				showMessage(requestBody);
				break;
				
				
			case "updateUserList":
				updateUserList(requestBody);
				break;
				
			case "chattingTextClear":
				chattingTextClear(requestBody);
				break;
				
            case "receiveWhisperMessage":
                receiveWhisperMessage(requestBody);
                break;
				
		}
	}
	
	//귓속말
	private void receiveWhisperMessage(String requestBody) {
	    TypeToken<RequestBodyDto<SendMessage>> typeToken = new TypeToken<>() {};
	    RequestBodyDto<SendMessage> requestBodyDto = gson.fromJson(requestBody, typeToken.getType());
	    SendMessage whisperMessage = requestBodyDto.getBody();

	    String fromUsername = whisperMessage.getFromUsername();
	    String toUsername = whisperMessage.getToUsername();
	    String messageBody = whisperMessage.getMessageBody();
	    
	    String whisperMessageContent = "[귓말] " + fromUsername + " --> " + toUsername + ": " + messageBody + "\n";
	    SimpleGUIClient.getInstance().getChattingTextArea().append(whisperMessageContent);
	}
	
	// 방 목록 업데이트 리스트
	private void updateRoomList(String requestBody) {
		List<String> roomList = (List<String>) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
		SimpleGUIClient.getInstance().getRoomListModel().clear();
		SimpleGUIClient.getInstance().getRoomListModel().addAll(roomList);
	}

	// 메세지
	private void showMessage(String requestBody) {
		String messageContent = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
		SimpleGUIClient.getInstance().getChattingTextArea().append("[전체] " + messageContent + "\n");
	}
	
	//방 유저 업데이트 리스트
	private void updateUserList(String requestBody) {
	    List<String> usernameList = (List<String>) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
	    String roomOwner = usernameList.get(0);
	    String clientUsername = SimpleGUIClient.getInstance().getUsername();

	    SimpleGUIClient.getInstance().getUserListModel().clear();

	    for (String username : usernameList) {
	        if (username.equals(clientUsername) && username.equals(roomOwner)) {
	        	
	            SimpleGUIClient.getInstance().getUserListModel().addElement("<html><b><font color='blue'>" + username + "( 방장 )</font></b></html>");
	        } else if (username.equals(clientUsername)) {
	        	
	            SimpleGUIClient.getInstance().getUserListModel().addElement("<html><b><font color='blue'>" + username + "</font></b></html>");
	        } else if (username.equals(roomOwner)) {
	        	
	            SimpleGUIClient.getInstance().getUserListModel().addElement("<html><b>" + username + "( 방장 )</b></html>");
	        } else {
	        	
	            SimpleGUIClient.getInstance().getUserListModel().addElement(username);
	        }
	    }
	}
	
	// 채팅방 삭제
	private void chattingTextClear(String requestBody) {
			SimpleGUIClient.getInstance().getChattingTextArea().setText("");
	}
	
}