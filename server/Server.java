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

    void broadcastMsg(String msg, ClientHandler sender){
        String message = msg;
        if (!msg.startsWith(sender.getNick())) {
             message = String.format("%s: %s", sender.getNick(), msg);
        }
        for (ClientHandler client : clients) {
                client.sendMsg(message);
            }
    }

    void privateMsg(String nickname, ClientHandler client, String msg){
        if (nickname.equals("сервера")) {
            client.sendMsg("Сообщение от " + nickname + ": "+ msg);
            return;
        }
        else {
            if (nickname.equals(client.getNick())) {return;}
            boolean isNickNameValid = false;
            String message = String.format("%s %s %s: %s", client.getNick(), "приватно для", nickname, msg);
            for (ClientHandler anotherClient : clients) {
                if (nickname.equals(anotherClient.getNick())) {
                    anotherClient.sendMsg(message);
                    client.sendMsg(message);
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
        for (ClientHandler c : clients) {
            if (c.getNick().equals(newNick)) {
                privateMsg("сервера", client, "данный никнейм уже занят");
                return;
            }
        }
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

    public boolean isLoginAuthorized(String login){
        for (ClientHandler c : clients) {
            if(c.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }


}
