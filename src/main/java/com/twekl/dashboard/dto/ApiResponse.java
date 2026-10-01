package com.twekl.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success = true;
    private int status = 200;
    private String message;
    private T data;
    private String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    private List<String> errors;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, int status, String message, T data, String timestamp, List<String> errors) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.errors = errors;
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setStatus(200);
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setStatus(200);
        resp.setMessage(message);
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setStatus(201);
        resp.setMessage(message);
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(false);
        resp.setStatus(status);
        resp.setMessage(message);
        return resp;
    }

    public static <T> ApiResponse<T> error(String message, int status, List<String> errors) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(false);
        resp.setStatus(status);
        resp.setMessage(message);
        resp.setErrors(errors);
        return resp;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
