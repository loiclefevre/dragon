package com.oracle.dragon.model;

public class ORDSSQLServiceResponseItems {
    private int statementId;
    private String statementType;
    private int errorCode;
    private int errorLine;
    private String errorDetails;

    public ORDSSQLServiceResponseItems() {
    }

    public int getStatementId() {
        return statementId;
    }

    public void setStatementId(int statementId) {
        this.statementId = statementId;
    }

    public String getStatementType() {
        return statementType;
    }

    public void setStatementType(String statementType) {
        this.statementType = statementType;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorLine() {
        return errorLine;
    }

    public void setErrorLine(int errorLine) {
        this.errorLine = errorLine;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
