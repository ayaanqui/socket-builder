package com.github.ayaanqui.socketbuilder.Server;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.lang.Thread;

public abstract class SocketBuilder {
    int count = 1;
    int port = 5555;
    ArrayList<ClientThreadBuilder> clientThreads = new ArrayList<ClientThreadBuilder>();
    ServerThread server;
    private Consumer<Serializable> callback;

    public SocketBuilder(Consumer<Serializable> call, int port) {
        this.callback = call;
        this.port = port;
        server = new ServerThread();
        server.start();
    }

    /**
     * Synchronized method to update all non-null clients on the clientThreads list.
     * Can be used if a new client joins the network, or if an existing client is
     * updated with new information.
     */
    public synchronized void updateChatClients(Serializable data) {
        clientThreads.stream().forEach(client -> {
            if (client == null)
                return;

            try {
                client.sendChatData(data);
            } catch (IOException e) {
                callback.accept("Error: Could not update client list...");
            }
        });
    }

    /**
     * Synchronized method to add new clients to chatThreads
     * 
     * @param client
     */
    public synchronized void addChatThreads(ClientThreadBuilder client) {
        clientThreads.add(client);
    }

    /**
     * Synchronized method to set a new value for the i-th element in chatThreads
     * 
     * @param i         Element to set new value on
     * @param newClient Object that replaces the current value
     */
    public synchronized void setChatThreads(int i, ClientThreadBuilder newClient) {
        clientThreads.set(i, newClient);
    }

    /**
     * Synchronized method to increment the count by i
     * 
     * @param i
     */
    public synchronized void incrementCount(int i) {
        count += i;
    }

    public abstract ClientThreadBuilder buildClientThread(Socket connection, int count);

    /**
     * ServerThread
     */
    class ServerThread extends Thread {
        ServerSocket socket;

        public void run() {
            try {
                this.socket = new ServerSocket(port);

                callback.accept("Server is running on port " + port);
                callback.accept("Waiting for client to connect...");

                while (true) {
                    Socket connection = this.socket.accept();
                    ClientThreadBuilder clientThread = buildClientThread(connection, count);

                    callback.accept("New client connected:");
                    callback.accept("\tClient #" + count);

                    // Manage chat client
                    addChatThreads(clientThread); // synchronously add chatThread to threads list
                    clientThread.start(); // Start chat thread for count client
                    incrementCount(1);
                }
            } catch (Exception e) {
                callback.accept("Something went wrong. Make sure port " + port + " is not in use.");
            }
        }

        public void close() throws IOException {
            this.socket.close();
        }
    }
}
