package clinet_socket;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import clinet_socket.dto.RequestBodyDto;
import clinet_socket.dto.SendMessage;
import lombok.Getter;

@Getter
public class SimpleGUIClient extends JFrame {
	
	private static SimpleGUIClient instance;
	public static SimpleGUIClient getInstance() {
		if(instance == null) {
			instance = new SimpleGUIClient();
		}
		return instance;
	}
	
	private String username;
	private Socket socket;
	private String roomName;
	private int selectedIndex;
	

	private JPanel mainCardPanel;
	private CardLayout mainCardLayout;
	

	private JPanel loginPanel;
	private JTextField usernameTextField;
	private JButton loginButton ;
	

	private JPanel chattingRoomListPanel;
	private JLabel usernameLabel;
	
	private JScrollPane roomListScrollPanel;
	private DefaultListModel<String> roomListModel;
	private JList roomList;
	
	private JButton createRoomButton ;
	

	private JPanel chattingRoomPanel;
	private JLabel roomLabel;
	private JTextArea chattingTextArea;
	
	private JScrollPane userListScrollPane;
	private DefaultListModel<String> userListModel;
	private JList userList;
	
	private JLabel TargetLabel;
	private JTextField messageTextField;
	private JButton sendButton;
	
	private JButton outButton;
	
	private String targetUsername = "전체";
	private boolean isWhisperMode = false;
    private String fromUsername;
	private SendMessage whisperMessage;
	
	boolean isWhisperMessage;
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SimpleGUIClient frame = SimpleGUIClient.getInstance();	//싱글톤 getInstance로 꺼내야함
					frame.setVisible(true);
					
					ClientReceiver clientReceiver = new ClientReceiver();
					clientReceiver.start();
					
					//어떤 클라이언트가 로그인 했는지 확인요청
					RequestBodyDto<String> requestBodyDto = new RequestBodyDto<String>("connection", frame.username);
					ClientSender.getInstance().send(requestBodyDto);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public SimpleGUIClient() {
		
		try {
			socket = new Socket("127.0.0.1", 8000);
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		//메인패널
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 429, 597);
		mainCardLayout = new CardLayout();
		mainCardPanel = new JPanel();
		mainCardPanel.setLayout(mainCardLayout);
		setContentPane(mainCardPanel);
		
        //로그인패널
        loginPanel = new JPanel();
        loginPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        loginPanel.setLayout(null);
        
        JLabel loginbackground = new JLabel(new ImageIcon("image/loginPanel.jpg"));
        
        loginbackground.setBounds(0, 0, 416, 561);
        loginPanel.add(loginbackground);
        
        mainCardPanel.add(loginPanel, "loginPanel");
        
        //로그인 아이디 필드
        usernameTextField = new JTextField();
        usernameTextField.setBounds(12, 470, 291, 41);
        loginPanel.add(usernameTextField);
        usernameTextField.setColumns(10);

        //로그인 버튼
        loginButton = new JButton("로그인");
        loginButton.setBounds(315, 457, 75, 66);
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                username = usernameTextField.getText();

                if (username.isBlank()) {
                    JOptionPane.showMessageDialog(loginPanel, "닉네임을 확인해주세요.", "입장 실패",JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JOptionPane.showMessageDialog(loginPanel, "환영합니다. " + username + "님!", "로그인 성공",JOptionPane.PLAIN_MESSAGE);

                RequestBodyDto<String> requestBodyDto = new RequestBodyDto<>("connection", username);
                ClientSender.getInstance().send(requestBodyDto);
               
                usernameLabel.setText(username + "   님");

                mainCardLayout.show(mainCardPanel, "chattingRoomListPanel");
                
            }
        });
        loginPanel.add(loginButton);
		
        //대기실
		chattingRoomListPanel = new JPanel();
		chattingRoomListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		chattingRoomListPanel.setLayout(null);
		
		chattingRoomListPanel = new Background("image/chattingbackground2.jpg");

		mainCardPanel.add(chattingRoomListPanel, "chattingRoomListPanel");
		chattingRoomListPanel.setLayout(null);
		
		// 내이름라벨
		usernameLabel = new JLabel("님");
		usernameLabel.setBounds(95, 58, 104, 24);
		usernameLabel.setFont(usernameLabel.getFont().deriveFont(20f).deriveFont(Font.BOLD));
		usernameLabel.setForeground(Color.BLACK);
		usernameLabel.setHorizontalAlignment(SwingConstants.LEFT);
		chattingRoomListPanel.add(usernameLabel);

		
		//대기실 방 목록
		roomListScrollPanel = new JScrollPane(); 
		roomListScrollPanel.setBounds(69, 155, 334, 395);
		chattingRoomListPanel.add(roomListScrollPanel);
		
		roomListModel = new DefaultListModel<String>();
		roomList = new JList<>(roomListModel);

		Font boldFont = roomList.getFont().deriveFont(Font.BOLD);
		roomList.setFont(boldFont);
		roomList.setForeground(Color.BLACK);
		
		roomList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {	//두번클릭 확인
					roomName = roomListModel.get(roomList.getSelectedIndex());	//선택된 방 인덱스번호를 가져와 모델에서 get 해서 roomName 가져옴
					
					roomLabel.setText("방제목:   " + roomName);
					mainCardLayout.show(mainCardPanel, "chattingRoomPanel"); //메인카드패널에서 채팅룸패널로 이동
					RequestBodyDto<String> requestBodyDto = new RequestBodyDto<String>("join", roomName);
					ClientSender.getInstance().send(requestBodyDto);
					
					
				}
			}
		});
		
		roomListScrollPanel.setViewportView(roomList);
		
		//방만들기 버튼
		createRoomButton = new JButton("방만들기");
		createRoomButton.setBounds(299, 53, 104, 34);	//버튼의 모양
		createRoomButton.setFont(createRoomButton.getFont().deriveFont(15f).deriveFont(Font.BOLD));
		createRoomButton.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				roomName = JOptionPane.showInputDialog(chattingRoomListPanel, "방제목을 입력하세요.");
				if(Objects.isNull(roomName)) {	//취소버튼을 눌렀을 경우 null 이 들어가면서 이벤트 종료
					return;
				}
				if(roomName.isBlank()) {	//방제목에 공백이 들어갔을 시 에러메시지를 띄어주면서 리턴걸어서 빠져나감.
					JOptionPane.showMessageDialog(chattingRoomListPanel, "방제목을 입력하세요.", "방만들기 실패", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// roomListModel 안에 roomName이 들어있는데 동일한 방제목이 있을 시 에러메시지 띄어주고 리턴(마우스 이벤트 메소드 빠져나가는 리턴)
				for(int i = 0; i < roomListModel.size(); i++) {
					if(roomListModel.get(i).equals(roomName)) {
						JOptionPane.showMessageDialog(chattingRoomListPanel, "이미 존재하는 방제목입니다.", "방만들기 실패", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				
				// 정상적인 방제목입력 할 시 정상 처리
				RequestBodyDto<String> requestBodyDto = new RequestBodyDto<String>("createRoom", roomName);
				ClientSender.getInstance().send(requestBodyDto);
				mainCardLayout.show(mainCardPanel, "chattingRoomPanel"); //메인카드패널에서 채팅룸패널로 이동
				requestBodyDto = new RequestBodyDto<String>("join", roomName);
				ClientSender.getInstance().send(requestBodyDto);
				roomLabel.setText("방제목:   " + roomName);
			}
		});
		
		chattingRoomListPanel.add(createRoomButton);	//어디에 추가할지 위치
		
		//채팅방패널
		chattingRoomPanel = new JPanel();
		chattingRoomPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		chattingRoomPanel.setLayout(null);
		
		chattingRoomPanel = new Background("image/chattingRoomPanel.jpg");
		
		mainCardPanel.add(chattingRoomPanel, "chattingRoomPanel");
		chattingRoomPanel.setLayout(null);
		
		//방제목라벨
		roomLabel = new JLabel("방제목: ");
		roomLabel.setBounds(45, 10, 244, 19);
		chattingRoomPanel.add(roomLabel);
		roomLabel.setEnabled(false);
		roomLabel.setBorder(BorderFactory.createEmptyBorder());
		roomLabel.setFont(roomLabel.getFont().deriveFont(Font.BOLD));
		roomLabel.setHorizontalAlignment(SwingConstants.LEFT);
		chattingRoomPanel.add(roomLabel);
		
		//채팅창
		JScrollPane chattingTextAreaScrollPane = new JScrollPane();
		chattingTextAreaScrollPane.setBounds(12, 39, 277, 406);
		chattingRoomPanel.add(chattingTextAreaScrollPane);
		chattingTextArea = new JTextArea();
		chattingTextAreaScrollPane.setViewportView(chattingTextArea);
		
		//채팅방안 유저리스트
		userListScrollPane = new JScrollPane();
		userListScrollPane.setBounds(294, 36, 109, 409);
		chattingRoomPanel.add(userListScrollPane);
		
		userListModel = new DefaultListModel<>();
		userList = new JList(userListModel);
		
		//userList 두번클릭시 인덱스번호 들고와서 내자신을 from유저에 , index번호에있는 user를 to유저에 집어넣음
		userList.addMouseListener(new MouseAdapter() {
		@Override
	    public void mouseClicked(MouseEvent e) {
	        if (e.getClickCount() == 2) {
	            selectedIndex = userList.getSelectedIndex();
	            if (selectedIndex >= 0) {
	            	
	                fromUsername = userListModel.getElementAt(selectedIndex);
	                TargetLabel.setText(fromUsername);

	                whisperMessage = SendMessage.builder()
	                        .fromUsername(username)
	                        .toUsername(fromUsername)
	                        .build();
	                
	                messageTextField.setEnabled(true);
	                messageTextField.requestFocus();
	            }
	        }
	    }
	});
		userListScrollPane.setViewportView(userList);
		
		//전체,귓말 라벨
		TargetLabel = new JLabel("전체");
		TargetLabel.setHorizontalAlignment(JLabel.CENTER);
		TargetLabel.setBounds(12, 475, 55, 49);
		chattingRoomPanel.add(TargetLabel);
		
		//채팅입력창
		messageTextField = new JTextField();
		messageTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getExtendedKeyCode() == KeyEvent.VK_ENTER) {
			        String messageBody = messageTextField.getText();

			        whisperMessage.setMessageBody(messageBody);

			        if (!TargetLabel.getText().equals("전체")) {
			            RequestBodyDto<SendMessage> requestBodyDto =
			                    new RequestBodyDto<>("sendWhisperMessage", whisperMessage);
			            ClientSender.getInstance().send(requestBodyDto);
			        } else {
			            SendMessage sendMessage = SendMessage.builder()
			                    .fromUsername(username)
			                    .toUsername("전체")
			                    .messageBody(messageBody)
			                    .build();

			            RequestBodyDto<SendMessage> requestBodyDto =
			                    new RequestBodyDto<>("sendMessage", sendMessage);
			            ClientSender.getInstance().send(requestBodyDto);
			        }
			        TargetLabel.setText("전체");
			        whisperMessage.setFromUsername("");
			        whisperMessage.setToUsername("");
			        messageTextField.setText("");
			    }
			}
		});
		
		messageTextField.setBounds(79, 475, 232, 49);
		chattingRoomPanel.add(messageTextField);
		messageTextField.setColumns(10);
		
		//채팅입력창 전송버튼
		sendButton = new JButton("전송");
		sendButton.addMouseListener(new MouseAdapter() {	

			//전송버튼을 눌렀을때 targetLabel이 user면 귓속말 전체면 공개채팅임 귓말후 모두 초기화시켜줌
		    @Override
		    public void mouseClicked(MouseEvent e) {
		        String messageBody = messageTextField.getText();

		        whisperMessage.setMessageBody(messageBody);

		        if (!TargetLabel.getText().equals("전체")) {
		            RequestBodyDto<SendMessage> requestBodyDto =
		                    new RequestBodyDto<>("sendWhisperMessage", whisperMessage);
		            ClientSender.getInstance().send(requestBodyDto);
		        } else {
		            SendMessage sendMessage = SendMessage.builder()
		                    .fromUsername(username)
		                    .toUsername("전체")
		                    .messageBody(messageBody)
		                    .build();

		            RequestBodyDto<SendMessage> requestBodyDto =
		                    new RequestBodyDto<>("sendMessage", sendMessage);
		            ClientSender.getInstance().send(requestBodyDto);
		        }
		        TargetLabel.setText("전체");
		        whisperMessage.setFromUsername("");
		        whisperMessage.setToUsername("");
		        messageTextField.setText("");
		    }
		});
		
		sendButton.setBounds(323, 475, 80, 49);
		chattingRoomPanel.add(sendButton);
		
		
		//채팅방안 나가기버튼 
		outButton = new JButton("나가기");
		outButton.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				RequestBodyDto<String> requestBodyDto = new RequestBodyDto<String>("roomExit", roomName);
				ClientSender.getInstance().send(requestBodyDto);
				
				mainCardLayout.show(mainCardPanel, "chattingRoomListPanel");
			}
		});
		outButton.setBounds(323, 10, 80, 19);
		chattingRoomPanel.add(outButton);

	}
}