
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class Server extends Frame implements ActionListener {
    static JTextArea display;
    static int port; // 서버, 나의 포트
    JScrollPane sp;
    JTextField text;
    JLabel lword;
    Socket connection;
    String serverdata = "";
    int sport = 0;

    static Vector<Socket> conn = new Vector<>();
    static HashMap<String, Integer> userINFO_0 = new HashMap<>(); // 접속한 유저의 주소, 포트로 구성
    static HashMap<Integer, String> userINFO_1 = new HashMap<>(); // 접속한 유저의 포트, 닉네임으로 구성

    public Server(int port) {
        super("서버");
        sport = port;
        display = new JTextArea(0, 0);
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
        setSize(500, 200);
        setVisible(true);
    }

    // ---------------------------------------------------------------------------------------------------

    // 런 서버 메소드에서 만든 쓰레드 객체 클래스
    public static class UserThread implements Runnable {
        Socket socket;

        public UserThread() {
        }

        // 런 서버 메소드에서 생성한 쓰레드 객체의 메소드
        public void addUser(Socket s) {
            this.socket = s;
        }


        // ---------------------------------------------------------------------------------------------------------
        // 쓰레드 실행
        @Override
        public void run() {

            StringTokenizer stringTokenizer;
            BufferedWriter output;
            BufferedReader input;
            try {
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, "euc-kr");
                input = new BufferedReader(isr);
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, "euc-kr");
                output = new BufferedWriter(osw);

                while (true) {

                    String str = input.readLine();
                    stringTokenizer = new StringTokenizer(str, "|");
                    String protocol = stringTokenizer.nextToken();

                    int cnt = 0;
                    String[] msg = new String[stringTokenizer.countTokens()];
                    while (stringTokenizer.hasMoreTokens()) {
                        msg[cnt] = stringTokenizer.nextToken();
                        cnt++;
                    }

                    // 클라이언트의 포트 식별. 클라이언트가 일반 폼으로 채팅치면 자동으로 자기 주소와 포트가 프로토콜로 넘어옴.
                    //주소와 포트 검증. protocal=주소, msg[0]=포트
                    if (userINFO_0.containsKey(protocol)) {
                        if (userINFO_0.get(protocol) == Integer.parseInt(msg[0])) {
                            if (msg[1].equals("quit")) {
                                display.append("클라이언트 " + userINFO_1.get(userINFO_0.get(protocol)) + "님이 연결 해제되었습니다.\n");
                                output.write("1002|" + userINFO_1.get(userINFO_0.get(protocol)) + "님이 퇴장하셨습니다.\n");
                                output.flush();
                            } else {
                                display.append(userINFO_1.get(userINFO_0.get(protocol)) + " : " + msg[1] + "\n");
                                output.write("2000|" + userINFO_1.get(userINFO_0.get(protocol)) + "|" + msg[1] + "\n");
                                output.flush();
                            }
                        }
                    }
                    switch (protocol) {
                        // 최초 실행 1000
                        case "1000": // 클라이언트와 연결됨.
                            System.out.println(socket); // 접속한 클라이언트 정보 로그
                            display.append("SYSTEM) " + msg[0] + "님이 연결되었습니다.\n"); // 내 서버 폼에 띄울 메세지

                            userINFO_0.put(socket.getInetAddress().getHostAddress(), socket.getPort()); // 사용자 주소, 포트
                            userINFO_1.put(socket.getPort(), msg[0]); // 사용자 포트, 이름

                            //유저의 local 정보인 주소, 포트를 보내줌.
                            output.write("9999|" + socket.getInetAddress().getHostAddress() + "|" + socket.getPort() + "\n");
                            output.flush();

                            String broadMsg = "1001|" + socket.getInetAddress().getHostAddress() + "|" + socket.getPort() + "|" + userINFO_1.get(userINFO_0.get(socket.getInetAddress().getHostAddress())) + "님이 입장하셨습니다.\n";

                            for (int i=0; i < conn.size(); i++){
                                OutputStream broad_os = conn.get(i).getOutputStream();
                                OutputStreamWriter broad_osw = new OutputStreamWriter(broad_os, "euc-kr");
                                BufferedWriter broad_output = new BufferedWriter(broad_osw);
                                broad_output.write(broadMsg);
                                broad_output.flush();
                            }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // 스레드 유저와 대응되는 쓰레드 생성
    public void runServer() {
        ServerSocket server;
        try {
            server = new ServerSocket(sport, 100);
            display.append("SYSTEM) 서버 실행\n");

            while (true) {
                try {
                    connection = server.accept();
                    conn.add(connection);

                    UserThread userThread = new UserThread(); // 유저에 대응하는 쓰레드
                    Thread th = new Thread(userThread);
                    userThread.addUser(connection); // 유저 추가
                    th.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------------------
    // 서버 gui 채팅 폼에서 입력한 경우. 일반채팅
    public void actionPerformed(ActionEvent ae) {
        serverdata = text.getText();
        BufferedWriter soutput;

        try {
            soutput = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "euc-kr"));

            display.append("서버 : " + serverdata + "\n");
            soutput.write("server|" + serverdata + "\r\n");
            soutput.flush();

            text.setText("");
            if (serverdata.equals("quit")) {
                connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // 메인문. 우리 서버 포트 10000으로 서버 객체 생성
    public static void main(String args[]) {
        try {
            port = 10000;
            Server s = new Server(port);
            s.runServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------------------------------------------


    class WinListener extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
    }
}