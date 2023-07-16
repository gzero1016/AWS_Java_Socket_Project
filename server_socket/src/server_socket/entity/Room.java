package server_socket.entity;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import server_socket.ConnectedSocket;

@Builder
@Data
public class Room {
	private String roomName;
	private String userName;
	private String owner;
	private List<ConnectedSocket> userList;
	private List<ConnectedSocket> waitingRoomUserList;
}
