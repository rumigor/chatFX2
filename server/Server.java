package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class Server {
    protected List<ClientHandler> clients;
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public Server() {
        clients = new Vector<>();
        authService = new SimpleAuthService();

        ServerSocket server = null;
        Socket socket;

        final int PORT = 8189;

        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер запущен!");

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                System.out.println("socket.getRemoteSocketAddress(): "+socket.getRemoteSocketAddress());
                System.out.println("socket.getLocalSocketAddress() "+socket.getLocalSocketAddress());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void broadcastMsg(String msg, ClientHandler clientHandler){
        if (!msg.startsWith(clientHandler.getNick())) {
            msg = clientHandler.getNick() + ": " + msg;
        }
        for (ClientHandler client : clients) {
                client.sendMsg(msg);
            }
    }

    void privateMsg(String nickname, ClientHandler client, String msg){
        if (nickname.equals("сервера")) {
            client.sendMsg("Сообщение от " + nickname + ": "+ msg);
            return;
        }
        else {
            boolean isNickNameValid = false;
            for (ClientHandler anotherClient : clients) {
                if (nickname.equals(anotherClient.getNick())) {
                    anotherClient.sendMsg("Сообщение от " + client.getNick() + ": "+ msg);
                    client.sendMsg("Вы приватно для " + anotherClient.getNick() + ": " + msg);
                    isNickNameValid = true;
                    break;
                }
            }
            if (!isNickNameValid) {
                client.sendMsg("В чате нет пользователя с таким ником!");
            }
        }

    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastMsg(clientHandler.getNick() + " подключился к чату!", clientHandler);
        privateMsg("сервера", clientHandler, "Добропожаловать в чат!\nДля смены ника направьте на сервер команду: /chgnick NewNickName\n" +
                "Для отправки приватного сообщения перед текстом сообщения введите: /w usernickname\nДля выхода из чата направьте команду: /end");
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler){
        broadcastMsg(clientHandler.getNick() + " вышел из чата", clientHandler);
        clients.remove(clientHandler);
        broadcastClientsList();
    }
    public void changeNick(ClientHandler client, String newNick) {
        broadcastMsg(client.getNick() + " сменил ник на " +newNick, client);
        client.setNick(newNick);
        broadcastClientsList();
    }

    public void broadcastClientsList() {
        StringBuilder sb = new StringBuilder("/clients ");
        for (ClientHandler o : clients) {
            sb.append(o.getNick() + " ");
        }
        for (ClientHandler client : clients) {
            client.sendMsg("/clientList " + sb.toString());
        }
    }


}
