package cefetmg.br.sd.services.P2P;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

import cefetmg.br.sd.R;

import static cefetmg.br.sd.services.P2P.P2PPeerNode.CONTINGENCY_OPTIONAL;

public class P2PStorageService extends IntentService {

    private int mPeerPort, mPeerContingency;
    private boolean mIsFirstNode;
    private String mPeerAddress, mPeerId;
    private P2PPeerNode mP2PPeerNode;
    private ServerSocket mServerSocket;

    public P2PStorageService() {
        super("P2P Storage Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d("P2PStorage", "Executando");
        mPeerPort = Integer.parseInt(getResources().getString(R.string.defaultP2PPort));
        mPeerId = intent.getExtras().getString("P2P_NODEID", "");
        mIsFirstNode = intent.getExtras().getBoolean("P2P_FIRSTNODE", true);
        mPeerAddress = intent.getExtras().getString("P2P_PEERNODE", "");
        mPeerContingency = intent.getExtras().getInt("P2P_PEERCONTINGENCY", CONTINGENCY_OPTIONAL);
        initP2PServer();
    }

    private void initP2PServer() {
        try {
            mP2PPeerNode = new P2PPeerNode(mPeerId, mPeerPort, mPeerContingency);
            mServerSocket = new ServerSocket(mPeerPort);

            while(true) {
                new P2PStorageThread(mServerSocket.accept(), mP2PPeerNode).start();
            }

        } catch (IOException e) {
            Log.e("P2PStorageService", "Falha ao iniciar peer node " + e.getMessage());
        }
    }

}
