package com.muhtasim.facerecognition.utility;

public class Staff {

    /* Follow the staff table in database */
    String staffID, staffName, staffMatricNumber, staffCentre, staffPosition;

    public Staff(String staffID, String staffName, String staffMatricNumber, String staffCentre, String staffPosition) {
        this.staffID = staffID;
        this.staffName = staffName;
        this.staffMatricNumber = staffMatricNumber;
        this.staffCentre = staffCentre;
        this.staffPosition = staffPosition;
    }

    public String getStaffID() {
        return staffID;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getStaffMatricNumber() {
        return staffMatricNumber;
    }

    public String getStaffCentre() {
        return staffCentre;
    }

    public String getStaffPosition() {
        return staffPosition;
    }
}
