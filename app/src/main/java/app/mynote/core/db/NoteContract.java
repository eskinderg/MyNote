package app.mynote.core.db;

import android.net.Uri;

public final class NoteContract {
    public static final String CONTENT_AUTHORITY = "app.mynote.sync";
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    static final String PATH_NOTES = "notes";

    // Database info
    static final String DB_NAME = "notes_db";
    static final int DB_VERSION = 1;


    public static abstract class Notes {
        public static final String NAME = "notes";
        public static final String COL_ID = "note_id";
        public static final String COL_HEADER = "header";
        public static final String COL_TEXT = "text";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_COLOUR = "colour";
        public static final String COL_SELECTION = "selection";
        public static final String COL_ARCHIVED = "archived";
        public static final String COL_FAVORITE = "favorite";
        public static final String COL_PINNED = "pinned";
        public static final String COL_ACTIVE = "active";
        public static final String COL_SPELL_CHECK = "spell_check";
        public static final String COL_PIN_ORDER = "pin_order";
        public static final String COL_DATE_CREATED = "date_created";
        public static final String COL_DATE_MODIFIED = "date_modified";
        public static final String COL_DATE_ARCHIVED = "date_archived";
        public static final String COL_DATE_DELETED = "date_deleted";
        public static final String COL_DATE_SYNC = "date_sync";
        public static final String COL_SYNCED = "synced";
        public static final String COL_OWNER = "owner";


        // ContentProvider information for notes
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_NOTES).build();
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_URI + "/" + PATH_NOTES;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_URI + "/" + PATH_NOTES;
    }
    private static final String[] PROJECTION = {
            Notes.COL_ID,
            Notes.COL_HEADER,
            Notes.COL_TEXT,
            Notes.COL_USER_ID,
            Notes.COL_COLOUR,
            Notes.COL_SELECTION,
            Notes.COL_ARCHIVED,
            Notes.COL_PINNED,
            Notes.COL_ACTIVE,
            Notes.COL_SPELL_CHECK,
            Notes.COL_PIN_ORDER,
            Notes.COL_DATE_CREATED,
            Notes.COL_DATE_MODIFIED,
            Notes.COL_DATE_ARCHIVED,
            Notes.COL_DATE_DELETED,
            Notes.COL_DATE_SYNC,
            Notes.COL_OWNER
    };
}
