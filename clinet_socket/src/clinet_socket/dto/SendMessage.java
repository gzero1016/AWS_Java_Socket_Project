package clinet_socket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessage {
	private String fromUsername;
	private String toUsername;
	private String messageBody;
}