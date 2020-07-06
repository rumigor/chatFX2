package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandler {
    Server server;
    Socket socket = null;
    DataInputStream in;
    DataOutputStream out;

    private String nick;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    String str = "";
                    while (true) {
                        str = in.readUTF();
                        if (str.startsWith("/auth")) {
                            String[] token = str.split("\\s");
                            System.out.println(Arrays.toString(token));
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server
                                    .getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            if (newNick != null) {
                                boolean isNickBusy = false;
                                for (int i = 0; i < server.clients.size(); i++) {
                                    if (newNick.equals(server.clients.get(i).getNick())) {
                                        sendMsg("Пользователь с таким логином и паролем уже вошел в чат!");
                                        isNickBusy = true;
                                        break;
                                    }
                                }
                                if (!isNickBusy) {
                                    sendMsg("/authok " + newNick);
                                    nick = newNick;
                                    login = token[2];
                                    server.subscribe(this);
                                    System.out.printf("Клиент %s подключился \n", nick);
                                    break;
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                    }
                    //цикл работы
                    while (true) {
                        str = in.readUTF();
                        String [] msg = str.split("\\s");
                        if (msg[0].equals("/end")) {
                            out.writeUTF("/end");
                            break;
                        }
                        if (msg[0].equals("/w") && msg.length >= 3) {
                            String privateMsg = "";
                            for (int i = 2; i < msg.length; i++) {
                                privateMsg += msg[i] + " ";
                            }
                            server.privateMsg(msg[1], this,  privateMsg);
                        }
                        else if (msg[0].equals("/chgnick")) {
                            server.changeNick(this, msg[1]);
                        }
                        else {
                            server.broadcastMsg(str, this);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Клиент отключился");
                    server.unsubscribe(this);
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }
}
