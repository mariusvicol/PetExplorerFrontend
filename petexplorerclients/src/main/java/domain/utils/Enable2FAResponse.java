package domain.utils;

public class Enable2FAResponse {
    private String secret;
    private String qrCodeDataUri;

    public Enable2FAResponse() {
    }

    public Enable2FAResponse(String secret, String qrCodeDataUri) {
        this.secret = secret;
        this.qrCodeDataUri = qrCodeDataUri;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getQrCodeDataUri() {
        return qrCodeDataUri;
    }

    public void setQrCodeDataUri(String qrCodeDataUri) {
        this.qrCodeDataUri = qrCodeDataUri;
    }
}

