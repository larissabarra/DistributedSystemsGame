package cefetmg.br.sd.services.P2P;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Hashtable;

public class P2PPeerNode {


    public static final int CONTINGENCY_OFF = 1;
    public static final int CONTINGENCY_OPTIONAL = 2;
    public static final int CONTINGENCY_MANDATORY = 3;

    private Hashtable<Integer, String> localDataTable;
    private Hashtable<Integer, String> tempDataTable;
    private Hashtable<Integer, String> replicaDataTable;
    private int peerId, defaultPort, peerContingency;
    private P2PNode predecessorP2PNode, successorP2PNode;
    private int predecessorId, sucessorId;


    public P2PPeerNode(String peerId, int peerPort, int peerContingency) {
        this.peerId = peerId.hashCode();
        this.peerContingency = peerContingency;
        this.defaultPort = peerPort;
        localDataTable = new Hashtable<>();
        tempDataTable = new Hashtable<>();
        replicaDataTable = new Hashtable<>();
    }

    public P2PResponse processCommandLine(String commandLine, String senderIP) {

        P2PResponse response = null;

        String[] splittedCommand = commandLine.split(" ");

        switch (splittedCommand[0]) {
            case "ENTER":
                int newNodeId = splittedCommand[1].hashCode();
                if (newNodeId > peerId) {
                    successorP2PNode = new P2PNode(senderIP, defaultPort);
                    sucessorId = newNodeId;
                    response = new P2PResponse(200, "Nó adicionado a rede P2P com sucesso",
                            String.valueOf(peerId));
                } else if (newNodeId < peerId) {
                    predecessorP2PNode = new P2PNode(senderIP, defaultPort);
                    predecessorId = newNodeId;
                    response = new P2PResponse(200, "Nó adicionado a rede P2P com sucesso",
                            String.valueOf(peerId));
                } else {
                    response = new P2PResponse(500, "ID duplicado para o nó, utilize outro " +
                            "ID e tente novamente", splittedCommand[1]);
                }
                break;
            case "STORE":
                int keyStore = splittedCommand[1].hashCode();
                String valueStore = Arrays.toString(Arrays.copyOfRange(splittedCommand, 2, splittedCommand.length));
                if (successorP2PNode != null && keyStore >= sucessorId) {
                    response = forwardToNode(commandLine, successorP2PNode);
                    if (response == null) {
                        tempDataTable.put(keyStore, valueStore);
                        response = new P2PResponse(201, "Dado salvo com sucesso em nó antecessor provisório", String.valueOf(peerId));
                    }
                } else if (predecessorP2PNode != null && keyStore < predecessorId) {
                    response = forwardToNode(commandLine, predecessorP2PNode);
                    if (response == null) {
                        tempDataTable.put(keyStore, valueStore);
                        response = new P2PResponse(201, "Dado salvo com sucesso em nó sucessor provisório", String.valueOf(peerId));
                    }
                } else {
                    localDataTable.put(keyStore, valueStore);
                    if (handleContingency(commandLine, keyStore)) {
                        response = new P2PResponse(200, "Dado salvo com sucesso", String.valueOf(peerId));
                    } else {
                        response = new P2PResponse(500, "Erro ao salvar devido a contingência", String.valueOf(peerId));
                    }
                }
                break;
            case "FIND":
                int keyFind = splittedCommand[1].hashCode();
                if (successorP2PNode != null && keyFind >= sucessorId) {
                    response = forwardToNode(commandLine, successorP2PNode);
                    if (response == null) {
                        String searchResult = findInDatatables(keyFind, false);
                        if (searchResult != null) {
                            response = new P2PResponse(201, "Chave encontrada em nó antecessor", searchResult);
                        } else {
                            response = new P2PResponse(404, "Chave não encontrada em nó antecessor", String.valueOf(peerId));
                        }
                    }
                } else if (predecessorP2PNode != null && keyFind < predecessorId) {
                    response = forwardToNode(commandLine, predecessorP2PNode);
                    if (response == null) {
                        String searchResult = findInDatatables(keyFind, false);
                        if (searchResult != null) {
                            response = new P2PResponse(201, "Chave encontrada em nó sucessor", searchResult);
                        } else {
                            response = new P2PResponse(404, "Chave não encontrada em nó sucessor", String.valueOf(peerId));
                        }
                    }
                } else {
                    String searchResult = findInDatatables(keyFind, true);
                    if (searchResult != null) {
                        response = new P2PResponse(201, "Chave encontrada em nó detendor", searchResult);
                    } else {
                        response = new P2PResponse(404, "Chave não encontrada em nó detendor", String.valueOf(peerId));
                    }
                }
                break;
            case "REPLICA":
                int keyReplica = splittedCommand[1].hashCode();
                String valueReplica = Arrays.toString(Arrays.copyOfRange(splittedCommand, 2, splittedCommand.length));
                replicaDataTable.put(keyReplica, valueReplica);

                response = new P2PResponse(200, "Réplica salva com sucesso", String.valueOf(peerId));
                break;
            case "BYE":
                response = new P2PResponse(-1, "Desconectando", String.valueOf(peerId));
                break;
        }

        return response;
    }

    private P2PResponse forwardToNode(String request, P2PNode node) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(node.getIP(), 9999), 1000);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println(request);
            String[] response = reader.readLine().split(" ");

            writer.close();
            reader.close();
            socket.close();

            return new P2PResponse(Integer.valueOf(response[0]), response[1], response[2]);
        } catch (IOException e) {
            Log.e("P2PPeerNode", "Falha ao executar redirecionamento no cluster " + e.getMessage());
            return null;
        }
    }

    private String findInDatatables(int key, boolean justLocal) {
        if (localDataTable.containsKey(key)) {
            return localDataTable.get(key);
        } else if (!justLocal) {
            if (tempDataTable.containsKey(key)) {
                return tempDataTable.get(key);
            } else if (replicaDataTable.containsKey(key)) {
                return replicaDataTable.get(key);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean handleContingency(String request, int key) {
        if (peerContingency >= CONTINGENCY_OPTIONAL) {
            if (predecessorP2PNode != null) {
                P2PResponse response = forwardToNode(request, predecessorP2PNode);
                if (response == null && peerContingency == CONTINGENCY_MANDATORY) {
                    localDataTable.remove(key);
                    return false;
                }
            }
            if (successorP2PNode != null) {
                P2PResponse response = forwardToNode(request, successorP2PNode);
                if (response == null && peerContingency == CONTINGENCY_MANDATORY) {
                    localDataTable.remove(key);
                    return false;
                }
            }
        }
        return true;
    }

}
