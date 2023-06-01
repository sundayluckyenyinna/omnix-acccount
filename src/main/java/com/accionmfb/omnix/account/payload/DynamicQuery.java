package com.accionmfb.omnix.account.payload;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// This class encapsulates all the query parameters for the dynamic filtering of account statement
public class DynamicQuery
{
    private Integer size;
    private String startDate;
    private String endDate;
    private String timeQuery;
    private Integer backTimeValue;
    private String errorMessage;
    private boolean hasError = false;

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getTimeQuery() {
        return timeQuery;
    }

    public void setTimeQuery(String timeQuery) {
        this.timeQuery = timeQuery;
    }

    public Integer getBackTimeValue() {
        return backTimeValue;
    }

    public void setBackTimeValue(Integer backTimeValue) {
        this.backTimeValue = backTimeValue;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public DynamicQuery(Integer size, String startDate,
                        String endDate, String timeQuery, Integer backTimeValue) {
        if(size == null)
            this.size = 20;
        else
            this.size = size;
        this.startDate = startDate;
        this.endDate = endDate;
        this.timeQuery = timeQuery;
        if(backTimeValue == null)
            this.backTimeValue = 1;
        else
            this.backTimeValue = backTimeValue;
    }

    public void validateDynamicQuery(){

        // Validate the start date and the end date
        if(this.startDate != null){
            DynamicDataCheck isValidStartDate = isValidInputDate(startDate);
            if ( !isValidStartDate.check ){
                this.hasError = true;
                this.errorMessage = isValidStartDate.reason;
            }
            return;
        }
        if(this.endDate != null){
            DynamicDataCheck isValidEndDate = isValidInputDate(endDate);
            if ( !isValidEndDate.check ){
                this.hasError = true;
                this.errorMessage = isValidEndDate.reason;
            }
            return;
        }

        // validate the timeQuery
        if( timeQuery != null ){
            DynamicDataCheck isValidTimeQuery = isValidTimeQuery(timeQuery);
            if(!isValidTimeQuery.check ){
                this.hasError = true;
                this.errorMessage = isValidTimeQuery.reason;
            }
        }

    }

    private DynamicDataCheck isValidInputDate(String inputDateString){
        DynamicDataCheck dynamicDataCheck = new DynamicDataCheck();
        inputDateString = inputDateString.trim();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        try {
            LocalDate localDate = LocalDate.parse(inputDateString, formatter);
            dynamicDataCheck.check = true;
        }catch (Exception exception){
            dynamicDataCheck.check = false;
            dynamicDataCheck.reason = exception.getMessage();
        }
        return dynamicDataCheck;
    }

    private DynamicDataCheck isValidTimeQuery(String timeQuery){
        DynamicDataCheck dynamicDataCheck = new DynamicDataCheck();
        timeQuery = timeQuery.trim();
        if(timeQuery.equalsIgnoreCase("DAY") ||
                timeQuery.equalsIgnoreCase("WEEK") ||
                timeQuery.equalsIgnoreCase("MONTH")
        ){
            dynamicDataCheck.check = true;
            return dynamicDataCheck;
        }
        dynamicDataCheck.check = false;
        dynamicDataCheck.reason = "Invalid time query. Time query should be DAY, WEEK or MONTH";
        return dynamicDataCheck;
    }

    class DynamicDataCheck{
        public boolean check;
        public String reason;
    }
}
