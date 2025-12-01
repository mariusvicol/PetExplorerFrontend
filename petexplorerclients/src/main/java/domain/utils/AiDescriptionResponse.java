package domain.utils;

public class AiDescriptionResponse {
    private String status;
    private String description;
    private String message;
    private String details;

    public AiDescriptionResponse() {
    }

    public AiDescriptionResponse(String status, String description, String message, String details) {
        this.status = status;
        this.description = description;
        this.message = message;
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

