package server_socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
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
	
	private final Socket socket;	//매개변수를 socket으로 받는 생성자
	private Gson gson;
	
	private String username;
	private SendMessage toUsername;
	private String fromUsername;
	private String whisperMessage;
	private String room;
	private Object roomName; 
	
	@Override
	public void run() {
		gson = new Gson();
		
		while(true) {
				try {
					BufferedReader bufferedReader = 
							new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String requestBody = bufferedReader.readLine();
					
					requestController(requestBody);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
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
		username = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();	// Body가 username을 받아옴
		
		List<String> roomNameList = new ArrayList<>();	// roomNameList 생성
		
		SimpleGUIserver.roomList.forEach(room -> {	// roomNameList에 roomName을 하나씩 빼서 새롭게 정의
			roomNameList.add(room.getRoomName());
		});
		
		RequestBodyDto<List<String>> updateRoomListRequestBodyDto = 
				new RequestBodyDto<List<String>>("updateRoomList", roomNameList);
		
		ServerSender.getInstance().send(socket, updateRoomListRequestBodyDto);	// 자기 자신에게만 방리스트 전달
	}
	
	// 방 만들기
	private void creatRoom(String requestBody) {
		String roomName = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();	// Body가 roomName을 받아옴
		
		
		Room newRoom = Room.builder()
			.roomName(roomName)	//받아온 roomName
			.owner(username)
			.userList(new ArrayList<ConnectedSocket>())
			.build();
		
		SimpleGUIserver.roomList.add(newRoom);	//server클래스 static리스트에 위에서 새로 생성된 리스트를 담겠다.
		
		List<String> roomNameList = new ArrayList<>();	//roomNameList 생성
		
		SimpleGUIserver.roomList.forEach(room -> {	//roomNameList에 roomName을 하나씩 빼서 새롭게 정의
			roomNameList.add(room.getRoomName());
		});
		
		RequestBodyDto<List<String>> updateRoomListRequestBodyDto = 
				new RequestBodyDto<List<String>>("updateRoomList", roomNameList);
		
		SimpleGUIserver.connectedSocketList.forEach(con -> {	//접속한 사용자 모두에서 roomNameList를 뿌려준다.
			ServerSender.getInstance().send(con.socket, updateRoomListRequestBodyDto);
		});
	}
	
	// 방 들어가기
	private void join(String requestBody) {
		
		// 방 나갔을시 채팅창 초기화
		RequestBodyDto<String> chattingTextClearDto =
				new RequestBodyDto<String>("chattingTextClear", null);
		ServerSender.getInstance().send(socket, chattingTextClearDto);
		
		String roomName = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
		
		SimpleGUIserver.roomList.forEach(room -> {
			if(room.getRoomName().equals(roomName)) {
				room.getUserList().add(this);	//나 자신을 리스트에 추가
				
				List<String> usernameList = new ArrayList<>();
				
				room.getUserList().forEach(con -> {
					usernameList.add(con.username);
				});
				
				room.getUserList().forEach(connectedSocket -> {
					RequestBodyDto<List<String>> updateUserListDto = new RequestBodyDto<List<String>> ("updateUserList", usernameList);
					RequestBodyDto<String> joinMessageDto = new RequestBodyDto<String>("showMessage", username + "님이 들어왔습니다.");
					
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
		SendMessage sendMessage = requestBodyDto.getBody();	//Map으로 꺼낼 수 있던 것을 sendMessage 객체로 꺼낼 수 있음
		
		SimpleGUIserver.roomList.forEach(room -> {	//roomList를 forEach로 돌려서 내가 어느 룸에 들어있는지 확인
			if(room.getUserList().contains(this)) {	//내가 들어있는지 확인하며 내가 포함되어있으면 true 를 준다.
				room.getUserList().forEach(connectedSocket -> {
					RequestBodyDto<String> dto = 
							new RequestBodyDto<String>("showMessage", 
									sendMessage.getFromUsername() + ": " + sendMessage.getMessageBody());
					
					ServerSender.getInstance().send(connectedSocket.socket, dto);
				});
			}
		});
	}
	
	// 방 나가기
	private void roomExit(String requestBody) {
		String roomName = (String) gson.fromJson(requestBody, RequestBodyDto.class).getBody();
		
		for(int i = 0; i < SimpleGUIserver.roomList.size(); i++) {	//서버의 룸 리스트를 계속 반복하여 room에 넣어주고
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
								new RequestBodyDto<String>("showMessage", username + "님이 나갔습니다.");
						
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

	    // 귓속말 메시지 보낼 대상과 귓속말을 보낼 방 인원 리스트 준비
	    ConnectedSocket targetSocket = null;
	    List<ConnectedSocket> roomMembers = new ArrayList<>();

	    // 대상 사용자와 방 인원을 찾음
	    for (ConnectedSocket connectedSocket : SimpleGUIserver.connectedSocketList) {
	        if (connectedSocket.username.equals(toUsername)) {
	            targetSocket = connectedSocket;
	        }
	    }

	    // 귓속말 메시지 보낼 대상 사용자가 존재하지 않는 경우
	    if (targetSocket == null) {
	        String errorMessage = "귓속말 메시지를 보낼 대상 사용자가 존재하지 않습니다.";
	        RequestBodyDto<String> errorDto = new RequestBodyDto<>("errorMessage", errorMessage);
	        ServerSender.getInstance().send(socket, errorDto);
	    } else if (fromUsername.equals(toUsername)) {
	        // 본인에게 귓속말을 보내는 경우
	        String errorMessage = "자신에게 귓속말을 보낼 수 없습니다.";
	        RequestBodyDto<String> errorDto = new RequestBodyDto<>("errorMessage", errorMessage);
	        ServerSender.getInstance().send(socket, errorDto);
	    } else {
	        // 대상 사용자에게 귓속말 메시지 전송
	        RequestBodyDto<SendMessage> whisperMessageDto = new RequestBodyDto<>("receiveWhisperMessage", sendMessage);
	        ServerSender.getInstance().send(targetSocket.socket, whisperMessageDto);

	        // 발신자에게도 메시지 전송
	        RequestBodyDto<SendMessage> selfWhisperMessageDto = new RequestBodyDto<>("receiveWhisperMessage", sendMessage);
	        selfWhisperMessageDto.getBody().setFromUsername(username);
	        ServerSender.getInstance().send(socket, selfWhisperMessageDto);
	        
	    }
	}
}