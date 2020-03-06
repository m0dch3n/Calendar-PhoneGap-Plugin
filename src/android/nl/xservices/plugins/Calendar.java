package nl.xservices.plugins;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import static android.provider.CalendarContract.Events;

public class Calendar extends CordovaPlugin {
    private static final String HAS_READ_PERMISSION = "hasReadPermission";
    private static final String REQUEST_READ_PERMISSION = "requestReadPermission";

    private static final String HAS_WRITE_PERMISSION = "hasWritePermission";
    private static final String REQUEST_WRITE_PERMISSION = "requestWritePermission";

    private static final String HAS_READWRITE_PERMISSION = "hasReadWritePermission";
    private static final String REQUEST_READWRITE_PERMISSION = "requestReadWritePermission";

    private static final String ACTION_CREATE_EVENT_WITH_OPTIONS = "createEventWithOptions";
    private static final String ACTION_DELETE_EVENT_BY_ID = "deleteEventById";
    private static final String ACTION_LIST_EVENTS_IN_RANGE = "listEventsInRange";
    private static final String ACTION_LIST_CALENDARS = "listCalendars";
    private static final String ACTION_CREATE_CALENDAR = "createCalendar";
    private static final String ACTION_DELETE_CALENDAR = "deleteCalendar";

    // write permissions
    private static final int PERMISSION_REQCODE_CREATE_CALENDAR = 100;
    private static final int PERMISSION_REQCODE_DELETE_CALENDAR = 101;
    private static final int PERMISSION_REQCODE_CREATE_EVENT = 102;
    private static final int PERMISSION_REQCODE_DELETE_EVENT_BY_ID = 104;

    // read permissions
    private static final int PERMISSION_REQCODE_LIST_CALENDARS = 201;
    private static final int PERMISSION_REQCODE_LIST_EVENTS_IN_RANGE = 202;

    private static final Integer RESULT_CODE_CREATE = 0;
    private static final Integer RESULT_CODE_OPENCAL = 1;

    private JSONArray requestArgs;
    private CallbackContext callback;

    private static final String LOG_TAG = Calendar.class.getCanonicalName();

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callback = callbackContext;
        this.requestArgs = args;

        if (ACTION_CREATE_EVENT_WITH_OPTIONS.equals(action)) {
            createEvent(args);
            return true;
        } else if (ACTION_LIST_EVENTS_IN_RANGE.equals(action)) {
            listEventsInRange(args);
            return true;
        } else if (ACTION_DELETE_EVENT_BY_ID.equals(action)) {
            deleteEventById(args);
            return true;
        } else if (ACTION_LIST_CALENDARS.equals(action)) {
            listCalendars();
            return true;
        } else if (ACTION_CREATE_CALENDAR.equals(action)) {
            createCalendar(args);
            return true;
        } else if (ACTION_DELETE_CALENDAR.equals(action)) {
            deleteCalendar(args);
            return true;
        } else if (HAS_READ_PERMISSION.equals(action)) {
            hasReadPermission();
            return true;
        } else if (HAS_WRITE_PERMISSION.equals(action)) {
            hasWritePermission();
            return true;
        } else if (HAS_READWRITE_PERMISSION.equals(action)) {
            hasReadWritePermission();
            return true;
        } else if (REQUEST_READ_PERMISSION.equals(action)) {
            requestReadPermission(0);
            return true;
        } else if (REQUEST_WRITE_PERMISSION.equals(action)) {
            requestWritePermission(0);
            return true;
        } else if (REQUEST_READWRITE_PERMISSION.equals(action)) {
            requestReadWritePermission(0);
            return true;
        }
        return false;
    }

    private void hasReadPermission() {
        this.callback.sendPluginResult(new PluginResult(PluginResult.Status.OK,
                calendarPermissionGranted(Manifest.permission.READ_CALENDAR)));
    }

    private void hasWritePermission() {
        this.callback.sendPluginResult(new PluginResult(PluginResult.Status.OK,
                calendarPermissionGranted(Manifest.permission.WRITE_CALENDAR)));
    }

    private void hasReadWritePermission() {
        this.callback.sendPluginResult(new PluginResult(PluginResult.Status.OK,
                calendarPermissionGranted(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)));
    }

    private void requestReadPermission(int requestCode) {
        requestPermission(requestCode, Manifest.permission.READ_CALENDAR);
    }

    private void requestWritePermission(int requestCode) {
        requestPermission(requestCode, Manifest.permission.WRITE_CALENDAR);
    }

    private void requestReadWritePermission(int requestCode) {
        requestPermission(requestCode, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
    }

    private boolean calendarPermissionGranted(String... types) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        for (final String type : types) {
            if (!PermissionHelper.hasPermission(this, type)) {
                return false;
            }
        }
        return true;
    }

    private void requestPermission(int requestCode, String... types) {
        if (!calendarPermissionGranted(types)) {
            PermissionHelper.requestPermissions(this, requestCode, types);
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d(LOG_TAG, "Permission Denied!");
                this.callback.error("Please allow access to the Calendar and try again.");
                return;
            }
        }

        // now call the originally requested actions
        if (requestCode == PERMISSION_REQCODE_CREATE_CALENDAR) {
            createCalendar(requestArgs);
        } else if (requestCode == PERMISSION_REQCODE_DELETE_CALENDAR) {
            deleteCalendar(requestArgs);
        } else if (requestCode == PERMISSION_REQCODE_CREATE_EVENT) {
            createEvent(requestArgs);
        } else if (requestCode == PERMISSION_REQCODE_DELETE_EVENT_BY_ID) {
            deleteEventById(requestArgs);
        } else if (requestCode == PERMISSION_REQCODE_LIST_CALENDARS) {
            listCalendars();
        } else if (requestCode == PERMISSION_REQCODE_LIST_EVENTS_IN_RANGE) {
            listEventsInRange(requestArgs);
        }
    }

    private void listCalendars() {
        // note that if the dev didn't call requestReadPermission before calling this method and calendarPermissionGranted returns false,
        // the app will ask permission and this method needs to be invoked again (done for backward compat).
        if (!calendarPermissionGranted(Manifest.permission.READ_CALENDAR)) {
            requestReadPermission(PERMISSION_REQCODE_LIST_CALENDARS);
            return;
        }
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray activeCalendars = Calendar.this.getActiveCalendars();
                    if (activeCalendars == null) {
                        activeCalendars = new JSONArray();
                    }
                    callback.sendPluginResult(new PluginResult(PluginResult.Status.OK, activeCalendars));
                } catch (JSONException e) {
                    System.err.println("JSONException: " + e.getMessage());
                    callback.error(e.getMessage());
                } catch (Exception ex) {
                    System.err.println("Exception: " + ex.getMessage());
                    callback.error(ex.getMessage());
                }
            }
        });
    }

    public final JSONArray getActiveCalendars() throws JSONException {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.NAME,
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                        CalendarContract.Calendars.CALENDAR_COLOR,
                        CalendarContract.Calendars.VISIBLE,
                        CalendarContract.Calendars.IS_PRIMARY
                },
                CalendarContract.Calendars.VISIBLE + "=1", null, null
        );
        if (cursor == null) {
            return null;
        }
        JSONArray calendarsWrapper = new JSONArray();
        int primaryColumnIndex;
        if (cursor.moveToFirst()) {
            do {
                JSONObject calendar = new JSONObject();
                calendar.put("id", cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars._ID)));
                calendar.put(CalendarContract.Calendars.NAME, cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.NAME)));
                calendar.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)));
                calendar.put(CalendarContract.Calendars.CALENDAR_COLOR, CalendarUtils.getDisplayColorHex(cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR))));
                calendar.put("visible", cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)));
                primaryColumnIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY);
                if (primaryColumnIndex == -1) {
                    primaryColumnIndex = cursor.getColumnIndex("COALESCE(isPrimary, ownerAccount = account_name)");
                }
                calendar.put(CalendarContract.Calendars.IS_PRIMARY, "1".equals(cursor.getString(primaryColumnIndex)));
                calendarsWrapper.put(calendar);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return calendarsWrapper;
    }

    private ContentResolver getContentResolver() {
        return Calendar.this.cordova.getActivity().getContentResolver();
    }

    private void createCalendar(JSONArray args) {
        if (args.length() == 0) {
            System.err.println("Exception: No Arguments passed");
            return;
        }

        if (!calendarPermissionGranted(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)) {
            requestReadWritePermission(PERMISSION_REQCODE_CREATE_CALENDAR);
            return;
        }

        try {
            final JSONObject jsonFilter = args.getJSONObject(0);
            final String calendarColor = getPossibleNullString("calendarColor", jsonFilter);
            final String calendarName = getPossibleNullString("calendarName", jsonFilter);
            if (calendarName == null) {
                callback.error("calendarName is mandatory");
                return;
            }

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    String createdId = null; // getCalendarAccessor().createCalendar(calendarName, calendarColor);
                    callback.sendPluginResult(new PluginResult(PluginResult.Status.OK, createdId));
                }
            });
        } catch (JSONException e) {
            System.err.println("Exception: " + e.getMessage());
            callback.error(e.getMessage());
        }
    }

    private void deleteCalendar(JSONArray args) {
        if (args.length() == 0) {
            System.err.println("Exception: No Arguments passed");
            return;
        }

        if (!calendarPermissionGranted(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)) {
            requestReadWritePermission(PERMISSION_REQCODE_DELETE_CALENDAR);
            return;
        }

        try {
            final JSONObject jsonFilter = args.getJSONObject(0);
            final String calendarName = getPossibleNullString("calendarName", jsonFilter);
            if (calendarName == null) {
                callback.error("calendarName is mandatory");
                return;
            }

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // getCalendarAccessor().deleteCalendar(calendarName);
                        callback.sendPluginResult(new PluginResult(PluginResult.Status.OK, "yes"));
                    } catch (Exception e) {
                        System.err.println("Exception: " + e.getMessage());
                        callback.error(e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            System.err.println("Exception: " + e.getMessage());
            callback.error(e.getMessage());
        }
    }

    private void deleteEventById(final JSONArray args) {

        // note that if the dev didn't call requestWritePermission before calling this method and calendarPermissionGranted returns false,
        // the app will ask permission and this method needs to be invoked again (done for backward compat).
        if (!calendarPermissionGranted(Manifest.permission.WRITE_CALENDAR)) {
            requestWritePermission(PERMISSION_REQCODE_DELETE_EVENT_BY_ID);
            return;
        }

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject opts = args.optJSONObject(0);
                    final long id = opts != null ? opts.optLong("id", -1) : -1;

                    if (id == -1)
                        throw new IllegalArgumentException("Event id not specified.");

                    ContentResolver cr = getContentResolver();
                    Uri deleteUri = ContentUris.withAppendedId(Events.CONTENT_URI, id);
                    int rows = cr.delete(deleteUri, null, null);

                    boolean deleteResult = rows > 0;

                    callback.sendPluginResult(new PluginResult(PluginResult.Status.OK, deleteResult));
                } catch (Exception e) {
                    System.err.println("Exception: " + e.getMessage());
                    callback.error(e.getMessage());
                }
            }
        });
    }

    private void createEvent(JSONArray args) {
        // note that if the dev didn't call requestWritePermission before calling this method and calendarPermissionGranted returns false,
        // the app will ask permission and this method needs to be invoked again (done for backward compat).
        if (!calendarPermissionGranted(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)) {
            requestReadWritePermission(PERMISSION_REQCODE_CREATE_EVENT);
            return;
        }

        try {
            final JSONObject argObject = args.getJSONObject(0);
            final JSONObject argOptionsObject = argObject.getJSONObject("options");

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
//                    try {
                    final String createdEventID = null; // getCalendarAccessor().createEvent(
//                                null,
//                                getPossibleNullString(Events.TITLE, argObject),
//                                argObject.getLong("startTime"),
//                                argObject.getLong("endTime"),
//                                getPossibleNullString("notes", argObject),
//                                getPossibleNullString("location", argObject),
//                                argOptionsObject.optLong("firstReminderMinutes", -1),
//                                argOptionsObject.optLong("secondReminderMinutes", -1),
//                                getPossibleNullString("recurrence", argOptionsObject),
//                                argOptionsObject.optInt("recurrenceInterval", -1),
//                                getPossibleNullString("recurrenceWeekstart", argOptionsObject),
//                                getPossibleNullString("recurrenceByDay", argOptionsObject),
//                                getPossibleNullString("recurrenceByMonthDay", argOptionsObject),
//                                argOptionsObject.optLong("recurrenceEndTime", -1),
//                                argOptionsObject.optLong("recurrenceCount", -1),
//                                getPossibleNullString(Events.ALL_DAY, argOptionsObject),
//                                argOptionsObject.optInt("calendarId", 1),
//                                getPossibleNullString("url", argOptionsObject));
                    if (createdEventID != null) {
                        callback.success(createdEventID);
                    } else {
                        callback.error("Fail to create an event");
                    }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error creating event. Invoking error callback.", e);
            callback.error(e.getMessage());
        }
    }

    private static String getPossibleNullString(String param, JSONObject from) {
        return from.isNull(param) || "null".equals(from.optString(param)) ? null : from.optString(param);
    }

    private void listEventsInRange(final JSONArray args) {
        // note that if the dev didn't call requestReadPermission before calling this method and calendarPermissionGranted returns false,
        // the app will ask permission and this method needs to be invoked again (done for backward compat).
        if (!calendarPermissionGranted(Manifest.permission.READ_CALENDAR)) {
            requestReadPermission(PERMISSION_REQCODE_LIST_EVENTS_IN_RANGE);
            return;
        }
        try {
            final JSONObject jsonFilter = args.getJSONObject(0);

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    ContentResolver contentResolver = getContentResolver();

                    JSONArray result = new JSONArray();
                    long input_start_date = jsonFilter.optLong("startTime");
                    long input_end_date = jsonFilter.optLong("endTime");
                    Uri l_eventUri = Uri.parse(CalendarContract.Instances.CONTENT_URI + "/" + String.valueOf(input_start_date) + "/" + String.valueOf(input_end_date));

                    //prepare start date
                    java.util.Calendar calendar_start = java.util.Calendar.getInstance();
                    Date date_start = new Date(input_start_date);
                    calendar_start.setTime(date_start);

                    //prepare end date
                    java.util.Calendar calendar_end = java.util.Calendar.getInstance();
                    Date date_end = new Date(input_end_date);
                    calendar_end.setTime(date_end);

                    //projection of DB columns
                    String[] l_projection = new String[]{Events.CALENDAR_ID, Events.DELETED, Events.TITLE, "begin", "end", Events.EVENT_LOCATION, Events.ALL_DAY, "_id", Events.RRULE, Events.RDATE, Events.EXDATE, "event_id", Events.EVENT_COLOR, Events.DISPLAY_COLOR};

                    //actual query
                    Cursor cursor = contentResolver.query(
                            l_eventUri,
                            l_projection,
                            "(deleted = 0 AND" +
                                    "   (" +
                                    // all day events are stored in UTC, others in the user's timezone
                                    "     (eventTimezone  = 'UTC' AND begin >=" + (calendar_start.getTimeInMillis() + TimeZone.getDefault().getOffset(calendar_start.getTimeInMillis())) + " AND end <=" + (calendar_end.getTimeInMillis() + TimeZone.getDefault().getOffset(calendar_end.getTimeInMillis())) + ")" +
                                    "     OR " +
                                    "     (eventTimezone <> 'UTC' AND begin >=" + calendar_start.getTimeInMillis() + " AND end <=" + calendar_end.getTimeInMillis() + ")" +
                                    "   )" +
                                    ")",
                            null,
                            "begin ASC");

                    int i = 0;
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            try {
                                result.put(
                                        i++,
                                        new JSONObject()
                                                .put(Events.CALENDAR_ID, cursor.getString(cursor.getColumnIndex(Events.CALENDAR_ID)))
                                                .put("id", cursor.getString(cursor.getColumnIndex("_id")))
                                                .put("event_id", cursor.getString(cursor.getColumnIndex("event_id")))
                                                .put(Events.DELETED, cursor.getString(cursor.getColumnIndex(Events.DELETED)))
                                                .put(Events.RRULE, cursor.getString(cursor.getColumnIndex(Events.RRULE)))
                                                .put(Events.RDATE, cursor.getString(cursor.getColumnIndex(Events.RDATE)))
                                                .put(Events.EXDATE, cursor.getString(cursor.getColumnIndex(Events.EXDATE)))
                                                .put(Events.TITLE, cursor.getString(cursor.getColumnIndex(Events.TITLE)))
                                                .put(Events.DTSTART, cursor.getLong(cursor.getColumnIndex("begin")))
                                                .put(Events.DTEND, cursor.getLong(cursor.getColumnIndex("end")))
                                                .put(Events.EVENT_LOCATION, cursor.getString(cursor.getColumnIndex(Events.EVENT_LOCATION)) != null ? cursor.getString(cursor.getColumnIndex(Events.EVENT_LOCATION)) : "")
                                                .put(Events.EVENT_COLOR, cursor.getLong(cursor.getColumnIndex(Events.EVENT_COLOR)))
                                                .put(Events.DISPLAY_COLOR, CalendarUtils.getDisplayColorHex(cursor.getInt(cursor.getColumnIndex(Events.DISPLAY_COLOR))))
                                                .put(Events.ALL_DAY, cursor.getInt(cursor.getColumnIndex(Events.ALL_DAY)))
                                );
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        cursor.close();
                    }

                    callback.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                }
            });
        } catch (JSONException e) {
            System.err.println("Exception: " + e.getMessage());
            callback.error(e.getMessage());
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CODE_CREATE) {
            if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED) {
                // resultCode may be 0 (RESULT_CANCELED) even when it was created, so passing nothing is the clearest option here
                Log.d(LOG_TAG, "onActivityResult resultcode: " + resultCode);
                callback.success();
            } else {
                // odd case
                Log.d(LOG_TAG, "onActivityResult weird resultcode: " + resultCode);
                callback.success();
            }
        } else if (requestCode == RESULT_CODE_OPENCAL) {
            Log.d(LOG_TAG, "onActivityResult requestCode: " + RESULT_CODE_OPENCAL);
            callback.success();
        } else {
            Log.d(LOG_TAG, "onActivityResult error, resultcode: " + resultCode);
            callback.error("Unable to add event (" + resultCode + ").");
        }
    }
}
