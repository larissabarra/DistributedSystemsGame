package cefetmg.br.sd.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import cefetmg.br.sd.R;

public class FailureControllerService extends IntentService {

    private Socket mCurrentSocket;
    private List<GossipRegister> mGossipRegisters;

    public FailureControllerService() {
        super("Failure Controller Service");
        mGossipRegisters = new ArrayList<GossipRegister>();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("FailureController", "Executando");
        enterToCluster();
    }

    private void enterToCluster() {
        Log.d("FailureController", "Iniciando entrada no cluster");
        String bootstrapIP = getResources().getString(R.string.bootstrapIP);
        int bootstrapPort = Integer.parseInt(getResources().getString(R.string.bootstrapPort));
        try {
            mCurrentSocket = new Socket(bootstrapIP, bootstrapPort);
            final PrintWriter socketWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mCurrentSocket.getOutputStream())), true);
            socketWriter.println("{\"type\": \"enter\"}");
            InputStream inputStream = mCurrentSocket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);ce

            socketWriter.close();
        } catch (Exception e) {
            Log.e("FailureController", "Falha ao entrar no cluster" + e.getMessage());
        }
    }

    private static class GossipRegister {
        public String cliente_id;
        public String cliente_ip;
        public int cliente_heartbeat;
        public String cliente_status;
        public int cliente_timestamp;
    }

}
