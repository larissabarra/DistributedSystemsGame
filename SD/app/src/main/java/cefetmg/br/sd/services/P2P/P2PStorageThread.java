package cefetmg.br.sd.services.P2P;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class P2PStorageThread {


    private Socket socket = null;
    private P2PPeerNode p2PPeerNode = null;

    public P2PStorageThread(Socket socket, P2PPeerNode p2PPeerNode) {
        this.socket = socket;
        this.p2PPeerNode = p2PPeerNode;
    }

    public void start() {
        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String lineReceived;

            while ((lineReceived = reader.readLine()) != null) {
                String remoteIP = ((InetSocketAddress) socket.getRemoteSocketAddress())
                        .getAddress().toString().split("/")[1];
                P2PResponse response = p2PPeerNode.processCommandLine(lineReceived, remoteIP);
                writer.println(response.toString());
                if (response.getResponseCode() == -1) {
                    break;
                }
            }

            writer.close();
            reader.close();
            socket.close();

        } catch (IOException e) {
            Log.e("P2PServerThread", "Falha ao executar thread P2P " + e.getMessage());
        }
    }

}
