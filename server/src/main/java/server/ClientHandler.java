package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    socket.setSoTimeout(120000);
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();


                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s");
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    System.out.println("client " + nickname + " connected " + socket.getRemoteSocketAddress());
                                    server.logInfo("Клиент " + nickname + " подключился " + socket.getRemoteSocketAddress());
                                    socket.setSoTimeout(0);
                                    //==============//
//                                    sendMsg(SQLHandler.getMessageForNick(nickname));
                                    //==============//
                                    break;
                                } else {
                                    sendMsg("С этим логином уже авторизовались");
                                    server.logInfo("Попытка регистрации с уже авторизированным логином");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                                server.logInfo("Попытка авторизации с неверными данными");
                            }
                        }

                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            server.logInfo("Клиент " + nickname + " отключился");
                            throw new RuntimeException("client " + nickname + " disconnected");
                        }

                        if (str.startsWith(Command.REG)) {
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }
                            boolean isRegistered = server.getAuthService().registration(token[1], token[2], token[3]);
                            if (isRegistered) {
                                sendMsg(Command.REG_OK);
                            } else {
                                sendMsg(Command.REG_NO);
                            }
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();
                        server.logInfo("Получено сообщение от " + nickname + ": " + str);

                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                sendMsg(Command.END);
                                break;
                            }
                            if (str.startsWith(Command.PRV_MSG)) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }

                            //==============//
                            if (str.startsWith("/chnick ")) {
                                String[] token = str.split("\\s+", 2);
                                if (token.length < 2) {
                                    continue;
                                }
                                if (token[1].contains(" ")) {
                                    sendMsg("Ник не может содержать пробелов");
                                    continue;
                                }
                                if (server.getAuthService().changeNick(this.nickname, token[1])) {
                                    sendMsg("/yournickis " + token[1]);
                                    sendMsg("Ваш ник изменен на " + token[1]);
                                    server.logInfo("Ник клиента " + nickname + " изменен на " + token[1]);
                                    this.nickname = token[1];
                                    server.broadcastClientList();
                                } else {
                                    sendMsg("Не удалось изменить ник. Ник " + token[1] + " уже существует");
                                    server.logInfo("Неудачная попытка изменения никнейма");
                                }
                            }
                            //==============//
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    server.logExcOrError("Exception happened", e);
                    sendMsg(Command.END);
                } catch (RuntimeException e) {
                    server.logExcOrError("Exception happened", e);
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    server.logExcOrError("Exception happened", e);
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("client disconnected");
                    server.logInfo("Клиент отключен");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        server.logExcOrError("Exception happened", e);
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            server.logExcOrError("Exception happened", e);
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            server.logExcOrError("Exception happened", e);
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
