package com.github.ayaanqui.socketbuilder.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;

public abstract class ClientThreadBuilder extends Thread {
    private Socket connection;
    private int id;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Serializable> callback;

    public ClientThreadBuilder(Socket connection, int id, Consumer<Serializable> callback) {
        this.connection = connection;
        this.id = id;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            this.out = new ObjectOutputStream(this.connection.getOutputStream());
            this.in = new ObjectInputStream(this.connection.getInputStream());
            connection.setTcpNoDelay(true);
        } catch (Exception e) {
            callback.accept("Chat client #" + id);
            callback.accept("\tError: Could not open streams");
        }

        while (!this.connection.isClosed()) {
            try {
                Serializable req = (Serializable) in.readObject();
                callback.accept(String.format("Client #%d sent a request", this.id));

                handleRequests(req);
            } catch (Exception e) {
                callback.accept("Error: Could not fetch request from chat client #" + this.id);

                try {
                    closeConnection();
                } catch (IOException e1) {
                    callback.accept("Error: Could not close connection for chat client #" + this.id);
                }
            }
        }
        callback.accept("Connection closed for chat client #" + this.id);
    }

    /**
     * <pre>
     * <code>this.connection.close();</code>
     * <code>setChatThreads(id - 1, null)</code>
     * <code>updateChatClients();</code>
     * </pre>
     * 
     * @throws IOException
     */
    public abstract void closeConnection() throws IOException;

    public abstract void handleRequests(Serializable req) throws Exception;

    public void sendChatData(Serializable res) throws IOException {
        out.writeObject(res);
    }
}
