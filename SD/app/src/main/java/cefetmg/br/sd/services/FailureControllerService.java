package cefetmg.br.sd.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import cefetmg.br.sd.R;

import static cefetmg.br.sd.MainActivity.SERVER_IP;

public class FailureControllerService extends IntentService {

    private Socket mCurrentSocket;
    private String mBootstrapIP;
    private int mBootstrapPort;
    private int mDefaultGossipPort;

    Timer mFailTimer;
    int mDefaultGossipVerifications;
    int mDefaultGossipFailWait;

    private String mClienteId;
    private int mCurrentHeartBeat;
    private List<GossipRegister> mGossipRegisters;

    public FailureControllerService() {
        super("Failure Controller Service");
        mGossipRegisters = new ArrayList<>();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("FailureController", "Executando");
        mBootstrapIP = SERVER_IP;
        Toast.makeText(this, "ip intent: " + intent.getStringExtra("SERVER_IP"), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "ip servico: " + mBootstrapIP, Toast.LENGTH_SHORT).show();
        enterToCluster();
        setupTimerToGossip();
    }

    private boolean enterToCluster() {
        Log.d("FailureController", "Iniciando entrada no cluster");
        mBootstrapPort = Integer.parseInt(getResources().getString(R.string.bootstrapPort));
        mDefaultGossipPort = Integer.parseInt(getResources().getString(R.string.defaultGossipPort));
        try {
            mCurrentSocket = new Socket(mBootstrapIP, mBootstrapPort);
            PrintWriter socketWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mCurrentSocket.getOutputStream())), true);
            BufferedReader socketReader = new BufferedReader(new InputStreamReader(mCurrentSocket.getInputStream()));

            socketWriter.println("{\"type\": \"enter\"}");

            String stringReturn = socketReader.readLine();
            parseGossipRegisters(stringReturn);

            socketWriter.close();

            return true;
        } catch (Exception e) {
            Log.e("FailureController", "Falha ao entrar no cluster" + e.getMessage());
            return false;
        }
    }

    private void setupTimerToGossip() {
        mDefaultGossipVerifications = Integer.parseInt(getResources().getString(R.string.defaultGossipVerifications));
        mDefaultGossipFailWait = Integer.parseInt(getResources().getString(R.string.defaultGossipFailWait));

        mFailTimer = new Timer();
        mFailTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkFailures();
                gossipStatus();
            }
        }, 0, mDefaultGossipVerifications);
    }

    private void gossipStatus() {
        mCurrentHeartBeat++;
        //Envia para 2 nodes aleatórios
        Random random = new Random();
        int clusterSize = mGossipRegisters.size();
        int randomNodeIndex1 = random.nextInt(clusterSize);
        int randomNodeIndex2 = random.nextInt(clusterSize);
        while (randomNodeIndex2 == randomNodeIndex1) {
            randomNodeIndex2 = random.nextInt(clusterSize);
        }
        GossipRegister randomNode1 = mGossipRegisters.get(randomNodeIndex1);
        gossipToNode(randomNode1.cliente_ip, mDefaultGossipPort);
        GossipRegister randomNode2 = mGossipRegisters.get(randomNodeIndex2);
        gossipToNode(randomNode2.cliente_ip, mDefaultGossipPort);
    }

    private void checkFailures() {
        Date date = new Date();
        long timestampSeconds = (date.getTime()/1000);

        for(GossipRegister register : mGossipRegisters) {
            long delta = timestampSeconds - register.cliente_timestamp;
            if (delta > mDefaultGossipFailWait) {
                mGossipRegisters.remove(register);
            }
        }
    }

    private void gossipToNode(String nodeIp, int nodePort) {
        try {
            String gossipMessage = "{\"cliente_id\": \"%s\", \"heartbeat\": %s}";
            gossipMessage = String.format(gossipMessage, mClienteId, Integer.toString(mCurrentHeartBeat));

            mCurrentSocket = new Socket(nodeIp, nodePort);
            PrintWriter socketWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mCurrentSocket.getOutputStream())), true);
            BufferedReader socketReader = new BufferedReader(new InputStreamReader(mCurrentSocket.getInputStream()));

            socketWriter.println(gossipMessage);

            String stringReturn = socketReader.readLine();

            parseGossipRegisters(stringReturn);

        } catch (Exception e) {
            Log.e("FailureController", "Falha ao enviar heartbeat" + e.getMessage());
        }
    }

    private void parseGossipRegisters(String registers) throws JSONException {
        JSONObject jsonReturn = new JSONObject(registers);
        mClienteId = jsonReturn.getString("node_id");

        JSONArray clusterRegisters = jsonReturn.getJSONArray("cluster");

        for (int i = 0; i < clusterRegisters.length(); i++) {
            JSONArray clusterRegister = clusterRegisters.getJSONArray(i);
            GossipRegister gossipRegister = new GossipRegister();
            gossipRegister.cliente_id = clusterRegister.getString(0);
            gossipRegister.cliente_ip = clusterRegister.getString(1);
            gossipRegister.cliente_heartbeat = clusterRegister.getInt(2);
            gossipRegister.cliente_status = clusterRegister.getString(3);
            gossipRegister.cliente_timestamp = clusterRegister.getLong(4);

            boolean merge = false;
            for (GossipRegister register : mGossipRegisters) {
                if (register.cliente_id == gossipRegister.cliente_id) {
                    register.cliente_heartbeat = gossipRegister.cliente_heartbeat;
                    register.cliente_status = gossipRegister.cliente_status;
                    register.cliente_timestamp = gossipRegister.cliente_timestamp;
                    merge = true;
                    break;
                }
            }
            if (!merge) {
                mGossipRegisters.add(gossipRegister);
            }
        }
    }

    private static class GossipRegister {
        public String cliente_id;
        public String cliente_ip;
        public int cliente_heartbeat;
        public String cliente_status;
        public long cliente_timestamp;
    }

}