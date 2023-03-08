package com.muhtasim.facerecognition.utility;

public class Constants {

    /* MISCELLANEOUS */
    public static final String API_LINK = "https://hadirukm-api.herokuapp.com/";
    public static final String API_LINK_CONVO_PTM = "http://api.ukm.my/kehadiran/senarai/log_hadir/?api_key=konvo50&aktiviti=350&sidang=";
    public static final String EXTRA_USER_TYPE = "extraUserType";
    public static final String EXTRA_STAFF_ID = "extraStaffID";
    public static final String EXTRA_STAFF_FULL_NAME = "extraStaffFullName";
    public static final String EXTRA_STAFF_MATRIC_NUMBER = "extraStaffMatricNumber";
    public static final String EXTRA_STAFF_CENTRE = "extraStaffCentre";
    public static final String EXTRA_STAFF_POSITION = "extraStaffPosition";
    public static final String VAL_TYPE_STAFF = "staff";
    public static final String VAL_TYPE_GUEST = "guest";

    /* API FIELDS */
    public static final String API_VAR_SUCCESS = "success";
    public static final String API_VAR_DATA = "data";
    public static final String API_VAR_ERROR = "error";
    public static final String API_VAR_STATUS = "status";

    /* STAFF FIELDS */
    public static final String FIELD_STAFF_ID = "staffID";
    public static final String FIELD_STAFF_FULL_NAME = "staffFullName";
    public static final String FIELD_STAFF_IS_REGISTERED_WITH_FACE = "staffIsRegisteredWithFace";
    public static final String FIELD_STAFF_MATRIC_NUMBER = "staffMatricNumber";
    public static final String FIELD_STAFF_CENTRE = "staffCentre";
    public static final String FIELD_STAFF_POSITION = "staffPosition";

    /* REGISTER CONVO LOG FIELDS - PROVIDED BY PTM */
    public static final String FIELD_CONVO_LOG_UKMPER = "ukmper";
    public static final String FIELD_CONVO_LOG_NAMA_PENUH = "nama_penuh";
    public static final String FIELD_CONVO_LOG_TARIKH_MASUK = "tarikh_masuk";
    public static final String FIELD_CONVO_LOG_MASA_MASUK = "masa_masuk";
    public static final String FIELD_CONVO_LOG_NAMA_AKTIVITI = "nama_aktiviti";

    /* REGISTER CONVO LOG VALUES - PROVIDED BY PTM */
    public static final String VAL_CONVO_LOG_STATUS_BERJAYA = "Rekod kehadiran berjaya didaftarkan";
    public static final String VAL_CONVO_LOG_STATUS_TELAH_WUJUD = "Rekod kehadiran telah wujud";

    /* DATES AND TIMES FOR CONVOCATION */
    public static final String[] UKM_CONVO50_DATETIME_SLOTS = {"2022-11-26 07:00:00", "2022-11-26 13:00:00", "2022-11-26 19:00:00", "2022-11-27 07:00:00", "2022-11-27 13:00:00", "2022-11-27 19:00:00", "2022-11-28 07:00:00", "2022-11-28 13:00:00", "2022-11-28 19:00:00", "2022-11-29 07:00:00", "2022-11-29 13:00:00", "2022-11-29 19:00:00", "2022-11-30 07:00:00", "2022-11-30 13:00:00", "2022-11-30 19:00:00", "2022-12-01 07:00:00", "2022-12-01 13:00:00"};

    /* ERROR CODES */
    public static final String ERR_UNKNOWN_ERROR = "UNKNOWN_ERROR";

}
