package lk.ijse.dep8.todo.dto;

import java.io.Serializable;

public class ItemDTO implements Serializable {
    private String userEmail;
    private String description;
    private String status;

    public ItemDTO() {
    }

    public ItemDTO(String userEmail, String description, String status) {
        this.userEmail = userEmail;
        this.description = description;
        this.status = status;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ItemDTO{" +
                "userEmail='" + userEmail + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
