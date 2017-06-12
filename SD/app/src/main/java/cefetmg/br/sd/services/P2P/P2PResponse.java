package cefetmg.br.sd.services.P2P;

public class P2PResponse {

    private int responseCode;
    private String responseMessage;
    private String responseData;

    public P2PResponse(int responseCode, String responseMessage, String responseData) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.responseData = responseData;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", responseCode, responseMessage, responseData);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getResponseData() {
        return responseData;
    }
}
