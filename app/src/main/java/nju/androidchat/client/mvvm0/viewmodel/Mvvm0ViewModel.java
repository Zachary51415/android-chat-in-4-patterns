package nju.androidchat.client.mvvm0.viewmodel;

import android.os.AsyncTask;
import android.view.LayoutInflater;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.extern.java.Log;
import nju.androidchat.client.mvvm0.model.ClientMessageObservable;
import nju.androidchat.client.socket.MessageListener;
import nju.androidchat.client.socket.SocketClient;
import nju.androidchat.shared.message.ClientSendMessage;
import nju.androidchat.shared.message.ErrorMessage;
import nju.androidchat.shared.message.Message;
import nju.androidchat.shared.message.RecallMessage;
import nju.androidchat.shared.message.ServerSendMessage;

@Log
public class Mvvm0ViewModel extends BaseObservable implements MessageListener {

    @Bindable
    @Getter
    private String message;
    @Getter
    private List<ClientMessageObservable> messageObservableList;
    @Getter
    private SocketClient client;


    public void setMessage(String message) {
        this.message = message;
        notifyChange();
    }

    public Mvvm0ViewModel() {
        message = "";
        messageObservableList = new ArrayList<>();
        client = SocketClient.getClient();
        client.setMessageListener(this);
        client.startListening();
    }

    public void sendMessage() {
        LocalDateTime now = LocalDateTime.now();
        UUID uuid = UUID.randomUUID();
        String senderUsername = client.getUsername();
        ClientSendMessage clientSendMessage = new ClientSendMessage(uuid, now, message);
        messageObservableList.add(new ClientMessageObservable(clientSendMessage, senderUsername));
        AsyncTask.execute(() -> client.writeToServer(clientSendMessage));
    }

    @Override
    public void onMessageReceived(Message message) {
        if (message instanceof ServerSendMessage) {
            // 接受到其他设备发来的消息
            // 增加到自己的消息列表里，并通知UI修改
            ServerSendMessage serverSendMessage = (ServerSendMessage) message;
            log.info(String.format("%s sent a message: %s",
                    serverSendMessage.getSenderUsername(),
                    serverSendMessage.getMessage()
            ));
            messageObservableList.add(new ClientMessageObservable(serverSendMessage));
        } else if (message instanceof ErrorMessage) {
            // 接收到服务器的错误消息
            log.severe("Server error: " + ((ErrorMessage) message).getErrorMessage());

        } else if (message instanceof RecallMessage) {
            // 接受到服务器的撤回消息，MVVM-0不实现
        } else {
            // 不认识的消息
            log.severe("Unsupported message received: " + message.toString());

        }
    }

    public void disconnect() {
        AsyncTask.execute(() -> client.disconnect());
    }
}
