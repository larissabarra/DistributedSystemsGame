package cefetmg.br.sd.services.P2P;


public class P2PNode {

    private String ip;
    private int port;

    public P2PNode(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIP() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

}
