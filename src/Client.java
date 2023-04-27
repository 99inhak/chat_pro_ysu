
import java.io.*;
import java.net.*;
import java.sql.Time;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class Client extends Frame implements ActionListener, Runnable {

    JTextArea display;
    JScrollPane sp;
    JTextField text;
    JLabel lword;
    BufferedWriter output;
    BufferedReader input;
    Socket client;
    String clientdata = "";

    // 클라이언트 접속하는 사용자가 입력한 정보를 담을 배열
    static String[] userINPUT = new String[3]; // 이름, 서버주소, 서버포트
    static String[] userINFO = new String[2]; // 내 ip 주소, 내 포트번호


    // 클라이언트 생성자. 매개변수 받아 클라이언트 GUI 생성------------------------------------------------------
    public Client(String name, String sip, int n) {
        super(name);
        display = new JTextArea();
        display.setEditable(false);
        sp = new JScrollPane(display);
        add(sp, BorderLayout.CENTER);

        Panel pword = new Panel(new BorderLayout());
        lword = new JLabel("대화말");
        text = new JTextField(30);
        text.addActionListener(this);
        pword.add(lword, BorderLayout.WEST);
        pword.add(text, BorderLayout.CENTER);
        add(pword, BorderLayout.SOUTH);

        addWindowListener(new WinListener());
        setSize(300, 200);
        setVisible(true);
    }
    // ---------------------------------------------------------------------------------------------------------


    // ---------------------------------------------------------------------------------------------------------
    //클라이언트 쓰레드 실행
    @Override
    public void run() {
        try {
            while (true) {
                String serverdata = input.readLine();
                StringTokenizer st = new StringTokenizer(serverdata, "|");
                String protocol = st.nextToken();

                int cnt =0;
                String [] msg = new String[st.countTokens()];
                while (st.hasMoreTokens()){
                    msg[cnt] = st.nextToken();
                    cnt++;
                }

                // 서버가 액션폼으로 일반 채팅한 경우
                if (protocol.equals("server")) {
                    if (msg[0].equals("quit")) {
                        display.append("\n서버와의 연결이 끊어졌습니다.");
                        output.flush();
                    } else {
                        display.append("서버 : " + msg[0] + "\n");
                        display.setCaretPosition(display.getDocument().getLength());
                        output.flush();
                    }

                    // 서버와 프로토콜로 통신하는 경우
                } else {
                    switch (protocol) {
                        case "9999": // 입장 시 내 이름 보냈던 통신 리시브
                            // 9999|주소|포트
                            userINFO[0] = msg[0]; // 주소
                            userINFO[1] = msg[1]; // 포트
                            break;
                        case "1001": // 입장 프로토콜 수행 후 바로 입장 메세지 프로토콜
                            if(msg[0].equals(userINFO[0]) && msg[1].equals(userINFO[1])){
                                display.append("-------------------"+msg[2]+"-------------------");
                            }
                            break;
                        case "2000":
                            display.append(msg[0] + " : " + msg[1] + "\n");
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ---------------------------------------------------------------------------------------------------------
    // 메인에서 정보를 모두 입력하고나서 생성하고 실행한 클라이언트의 실행 메소드
    public void runClient() {
        try {
            // 서버 접속 대기 그냥 넣어봄
            display.append("서버와 접속중 . . .\n");
            Thread.sleep(3000);

            // 소켓 client 변수에 서버 주소와 포트로 생성
            client = new Socket(userINPUT[1], Integer.parseInt(userINPUT[2]));

            // 서버 정보를 담은 소켓으로 버퍼 읽고 쓰는 객체들 생성
            input = new BufferedReader(new InputStreamReader(client.getInputStream(), "euc-kr"));
            output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "euc-kr"));

            // 서버 연결 후 내 이름 보내줌
            display.append("서버와 연결되었습니다.\n");
            output.write("1000|" + userINPUT[0] + "\n");
            output.flush();

            Thread th = new Thread(this);
            th.start();

        } catch (Exception e) {
            try {
                System.out.println("SYSTEM) 서버와 연결에 실패했습니다. \nSYSTEM) 서버가 작동 중인지 확인해주세요.\n");
                System.exit(0);
                // e.printStackTrace();

            } catch (Exception e1) {
                e1.printStackTrace();
                System.exit(0);
            }

        }
    }
    // ---------------------------------------------------------------------------------------------------------


    // ---------------------------------------------------------------------------------------------------------
    // 클라이언트 gui 채팅 입력 폼으로 입력 시 처리.
    public void actionPerformed(ActionEvent ae) {
        clientdata = text.getText();
        try {
            display.append(userINPUT[0] + " : " + clientdata + "\n"); // 내 폼에 내 채팅
            display.setCaretPosition(display.getDocument().getLength());
            output.write(userINFO[0] + "|" + userINFO[1] + "|" + clientdata + "\r\n"); // 서버에 내 정보와 함게 메세지 보냄
            output.flush();

            text.setText("");
            if (clientdata.equals("quit")) {
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // ---------------------------------------------------------------------------------------------------------


    class WinListener extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
    }


    // ---------------------------------------------------------------------------------------------------------
    // 메인 함수. 클라이언트 정보를 입력하고 정보를 바탕으로 소캣을 생성.
    // 서버와 통신하하기 위한 자료를 입력받는 곳.
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in); // 입력받기 위한 객체 생성
        int controller = 0; // 매뉴 컨트롤하기 위한 변수 (단계 설정)
        String nextTrue; // 매뉴 컨트롤 변수 (진행 여부)
        boolean infinityController = true; // 무한루프 탈출용.

        while (infinityController) {

            // 그냥 클라이언트에서 정보 입력 편할려고 만든 루프 문. -----------------------------------------------
            switch (controller) {
                case 0:
                    if (userINPUT[0] == null) {
                        System.out.println("SYSTEM) 사용자의 이름을 입력해 주세요.");
                        String name = sc.nextLine();
                        if (name.trim().isEmpty()) {
                            System.out.println("다시 입력해주세요.");
                            break;
                        } else {
                            userINPUT[0] = name;
                            System.out.println(userINPUT[0] + ") 사용자 등록 완료");
                        }
                    }
                    System.out.println("계속 진행하시겠습니까? (y/n)");
                    nextTrue = sc.nextLine();
                    if (nextTrue.equals("y")) {
                        controller = 1;
                        break;
                    } else if (nextTrue.equals("n")) {
                        userINPUT[0] = null;
                        System.out.println("다시 작성합니다.");
                        break;
                    } else {
                        System.out.println("y와 n 중에서다시 입력해주세요.");
                    }
                    break;

                case 1:
                    if (userINPUT[1] == null || userINPUT[1].equals("")) {
                        System.out.println("SYSTEM) 서버의 ip주소를 입력해 주세요.");
                        userINPUT[1] = sc.nextLine();
                        System.out.println(userINPUT[0] + ")" + userINPUT[1] + " <-서버주소");
                    }
                    System.out.println("계속 진행하시겠습니까? (y/n)");
                    nextTrue = sc.nextLine();
                    if (nextTrue.equals("y")) {
                        controller = 2;
                        break;
                    } else if (nextTrue.equals("n")) {
                        userINPUT[1] = null;
                        System.out.println("다시 작성합니다.");
                    } else {
                        System.out.println("y와 n 중에서다시 입력해주세요.");
                    }
                    break;

                case 2:
                    if (userINPUT[2] == null || userINPUT[2].equals("")) {
                        System.out.println("SYSTEM) 사용하실 서버의 포트를 입력해 주세요.");
                        userINPUT[2] = sc.nextLine();
                        System.out.println(userINPUT[0] + ")" + userINPUT[2] + " <-서버포트");
                    }
                    System.out.println("계속 진행하시겠습니까? (y/n)");
                    nextTrue = sc.nextLine();
                    if (nextTrue.equals("y")) {
                        controller = 3;
                        break;
                    } else if (nextTrue.equals("n")) {
                        userINPUT[2] = null;
                        System.out.println("다시 작성합니다.");
                    } else {
                        System.out.println("y와 n 중에서다시 입력해주세요.");
                    }
                    break;
                // 그냥 클라이언트에서 정보 입력 편할려고 만든 루프 문. -----------------------------------------------

                    // 입력할 정보 다 입력한 경우 클라이언트 객체 생성하고 무한루프 종료
                case 3:
                    Client c = new Client(userINPUT[0], userINPUT[1], Integer.parseInt(userINPUT[2])); // 클라이언트 객체 생성
                    c.runClient(); // 실행
                    infinityController = false; // 종료
                    break;
            }
        }
    }
}