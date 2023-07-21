package server_socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import server_socket.dto.RequestBodyDto;
import server_socket.dto.SendMessage;
import server_socket.entity.Room;


@RequiredArgsConstructor
@Data
public class ConnectedSocket extends Thread {
	
	private final Socket socket;
	private Gson gson;
	
	private String username;
	private Object roomName; 
	
	@Override
	public void run() {
	    gson = new Gson();
	    
	    try {
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	        while (true) {
	            String requestBody = bufferedReader.readLine();

	            if (requestBody == null) {
	                System.out.println("클라이언트와 연결이 끊어졌습니다.");
	                break;
	            }
	            requestController(requestBody);
	        }
	        //강제 종료 시 예외처리
	    } catch (SocketException e) {
	        System.out.println("프로그램을 종료합니다.");
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	private void requestController(String requestBody) {
		
		String resource = gson.fromJson(requestBody, RequestBodyDto.class).getResource();

		
		switch (resource) {
			case "connection":
				connection(requestBody);
				break;
		
			case "createRoom":
				creatRoom(requestBody);
				break;
		
			case "join":
				join(requestBody);
				break;
				
			case "sendMessage":
				sendMessage(requestBody);
				break;
				
			case "roomExit":
				roomExit(requestBody);
				break;
				
            case "sendWhisperMessage":
            	sendWhisperMessage(requestBody);
                break;
		}
	}
	
	// 클라이언트 접속
	private void connection(String requestBody) {
		username = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
		
		//새로운 roomNameList를 생성
		List<String> roomNameList = new ArrayList<>();
		
		//room클래스에 roomName을 가져와 반복을 돌려 roomNameList에 추가한다.
		SimpleGUIserver.roomList.forEach(room -> {
			roomNameList.add(room.getRoomName());
		});
		
		//updateRoomListRequestBodyDto 객체를 생성해 ClientReceiver에 updateRoomList를 요청하고 roomNameList를 전달한다.
		RequestBodyDto<List<String>> updateRoomListRequestBodyDto = 
				new RequestBodyDto<List<String>>("updateRoomList", roomNameList);
		
		//클라이언트와 소통하기위해서는 ServerSender이 클라이언트에 전달한다.
		//updateRoomListRequestBodyDto에는 updateRoomList요청과 roomNameList가 담겨있고 이걸 ServerSender에 전송한다.
		ServerSender.getInstance().send(socket, updateRoomListRequestBodyDto);
	}
	
	// 방 만들기
	private void creatRoom(String requestBody) {
		String roomName = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
		
		//builder를 사용해 newRoom 객체생성
		Room newRoom = Room.builder()
			.roomName(roomName)
			.owner(username)
			.userList(new ArrayList<ConnectedSocket>())
			.build();
		
		SimpleGUIserver.roomList.add(newRoom);
		
		List<String> roomNameList = new ArrayList<>();
		
		SimpleGUIserver.roomList.forEach(room -> {
			roomNameList.add(room.getRoomName());
		});
		
		//위 설명과 동일
		RequestBodyDto<List<String>> updateRoomListRequestBodyDto = 
				new RequestBodyDto<List<String>>("updateRoomList", roomNameList);
		
		SimpleGUIserver.connectedSocketList.forEach(con -> {
			ServerSender.getInstance().send(con.socket, updateRoomListRequestBodyDto);
		});
	}
	
	// 방 들어가기
	private void join(String requestBody) {
		
		//방 재입장 시 채팅창 초기화
		RequestBodyDto<String> chattingTextClearDto =
				new RequestBodyDto<String>("chattingTextClear", null);
		ServerSender.getInstance().send(socket, chattingTextClearDto);
		
		String roomName = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
		
		SimpleGUIserver.roomList.forEach(room -> {
			if(room.getRoomName().equals(roomName)) {
				room.getUserList().add(this);
				
				List<String> usernameList = new ArrayList<>();
				
				room.getUserList().forEach(con -> {
					usernameList.add(con.username);
				});
				
				room.getUserList().forEach(connectedSocket -> {
					RequestBodyDto<List<String>> updateUserListDto = new RequestBodyDto<List<String>> ("updateUserList", usernameList);
					RequestBodyDto<String> joinMessageDto = new RequestBodyDto<String>("showMessage", username + " 님이 들어왔습니다.");
					RequestBodyDto<Object> userListInitSelectedDto = new RequestBodyDto<Object>("userListInitSelected", null);
					
					ServerSender.getInstance().send(connectedSocket.socket, updateUserListDto);
					try {
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					ServerSender.getInstance().send(connectedSocket.socket, joinMessageDto);
				});
			}
		});
	}
	
	// 메세지 보내기
	private void sendMessage(String requestBody) {
		TypeToken<RequestBodyDto<SendMessage>> typeToken = new TypeToken<>() {};
		
		RequestBodyDto<SendMessage> requestBodyDto = gson.fromJson(requestBody, typeToken.getType());
		SendMessage sendMessage = requestBodyDto.getBody();
		
		SimpleGUIserver.roomList.forEach(room -> {
			if(room.getUserList().contains(this)) {
				room.getUserList().forEach(connectedSocket -> {
					RequestBodyDto<String> dto = 
							new RequestBodyDto<String>("showMessage", 
									sendMessage.getFromUsername() + " : " + sendMessage.getMessageBody());
					
					ServerSender.getInstance().send(connectedSocket.socket, dto);
				});
			}
		});
	}
	
	// 방 나가기
	private void roomExit(String requestBody) {
		String roomName = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
		
		for(int i = 0; i < SimpleGUIserver.roomList.size(); i++) {
			Room room = SimpleGUIserver.roomList.get(i);
			
			if(room.getRoomName().equals(roomName)) { 
				room.getUserList().remove(this);
				
				if(room.getUserList().size() != 0) {
					List<String> usernameList = new ArrayList<>();
					
					for(ConnectedSocket connectedSocket : room.getUserList()) {
						usernameList.add(connectedSocket.username);
					}
					
					for(ConnectedSocket con : room.getUserList()) {
						RequestBodyDto<List<String>> updateUserListDto =
								new RequestBodyDto<List<String>>("updateUserList", usernameList);
						RequestBodyDto<String> roomExitMessageDto = 
								new RequestBodyDto<String>("showMessage", username + " 님이 나갔습니다.");
						
						ServerSender.getInstance().send(con.socket, updateUserListDto);
						usernameList.get(0);
						
						try {
							sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						ServerSender.getInstance().send(con.socket, roomExitMessageDto);
					}
				} else {
					SimpleGUIserver.roomList.remove(i);
					
					List<String> roomNameList = new ArrayList<>();
					
					for(Room newRoom : SimpleGUIserver.roomList) {
						roomNameList.add(newRoom.getRoomName());
					}
					
					RequestBodyDto<List<String>> updateRoomListDto = new RequestBodyDto<List<String>>("updateRoomList", roomNameList);
					for(ConnectedSocket connectedSocket : SimpleGUIserver.connectedSocketList) {
						ServerSender.getInstance().send(connectedSocket.socket, updateRoomListDto);
					}
					break;
				}
			}
		}
	}

	//귓속말
	private void sendWhisperMessage(String requestBody) {
	    TypeToken<RequestBodyDto<SendMessage>> typeToken = new TypeToken<>() {};
	    RequestBodyDto<SendMessage> requestBodyDto = gson.fromJson(requestBody, typeToken.getType());
	    SendMessage sendMessage = requestBodyDto.getBody();

	    String fromUsername = sendMessage.getFromUsername();
	    String toUsername = sendMessage.getToUsername();
	    String messageBody = sendMessage.getMessageBody();

	    ConnectedSocket targetSocket = null;
	    List<ConnectedSocket> roomMembers = new ArrayList<>();

	    for (ConnectedSocket connectedSocket : SimpleGUIserver.connectedSocketList) {
	        if (connectedSocket.username.equals(toUsername)) {
	            targetSocket = connectedSocket;
	        }
	    }

	    	if (fromUsername.equals(toUsername)) {
	        String errorMessage = "자신에게 귓속말을 보낼 수 없습니다.";
	        RequestBodyDto<String> errorDto = new RequestBodyDto<>("errorMessage", errorMessage);
	        ServerSender.getInstance().send(socket, errorDto);
	    	} else {
	        RequestBodyDto<SendMessage> whisperMessageDto = new RequestBodyDto<>("receiveWhisperMessage", sendMessage);
	        ServerSender.getInstance().send(targetSocket.socket, whisperMessageDto);

	        RequestBodyDto<SendMessage> selfWhisperMessageDto = new RequestBodyDto<>("receiveWhisperMessage", sendMessage);
	        selfWhisperMessageDto.getBody().setFromUsername(username);
	        ServerSender.getInstance().send(socket, selfWhisperMessageDto);
	        
	    }
	}
}