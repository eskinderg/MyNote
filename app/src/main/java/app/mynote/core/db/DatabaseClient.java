package app.mynote.core.db;

import static app.mynote.core.db.NoteContract.DB_NAME;
import static app.mynote.core.db.NoteContract.DB_VERSION;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DatabaseClient extends SQLiteOpenHelper {
    private static volatile DatabaseClient instance;
    private final SQLiteDatabase db;


    private DatabaseClient(Context c) {
        super(c, DB_NAME, null, DB_VERSION);
        this.db = getWritableDatabase();
    }

    /**
     * We use a Singleton to prevent leaking the SQLiteDatabase or Context.
     *
     * @return {@link DatabaseClient}
     */
    public static DatabaseClient getInstance(Context c) {
        if (instance == null) {
            synchronized (DatabaseClient.class) {
                if (instance == null) {
                    instance = new DatabaseClient(c);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create any SQLite tables here
        createArticlesTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Update any SQLite tables here
        db.execSQL("DROP TABLE IF EXISTS [" + NoteContract.Notes.NAME + "];");
        onCreate(db);
    }

    /**
     * Provide access to our database.
     */
    public SQLiteDatabase getDb() {
        return db;
    }

    /**
     * Creates our 'articles' SQLite database table.
     *
     * @param db {@link SQLiteDatabase}
     */
    private void createArticlesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE [" + NoteContract.Notes.NAME + "] ([" +
                NoteContract.Notes.COL_ID + "] TEXT UNIQUE PRIMARY KEY,[" +
                NoteContract.Notes.COL_HEADER + "] TEXT ,[" +
                NoteContract.Notes.COL_TEXT + "] TEXT ,[" +
                NoteContract.Notes.COL_USER_ID + "] TEXT,[" +
                NoteContract.Notes.COL_COLOUR + "] TEXT,[" +
                NoteContract.Notes.COL_SELECTION + "] TEXT,[" +
                NoteContract.Notes.COL_ARCHIVED + "] TINYINT,[" +
                NoteContract.Notes.COL_FAVORITE + "] TINYINT,[" +
                NoteContract.Notes.COL_PINNED + "] TINYINT,[" +
                NoteContract.Notes.COL_ACTIVE + "] TINYINT NOT NULL ON CONFLICT REPLACE DEFAULT 1 ,[" +
                NoteContract.Notes.COL_SPELL_CHECK + "] TINYINT,[" +
                NoteContract.Notes.COL_PIN_ORDER + "] INTEGER,[" +
                NoteContract.Notes.COL_DATE_CREATED + "] TEXT,[" +
                NoteContract.Notes.COL_DATE_MODIFIED + "] TEXT,[" +
                NoteContract.Notes.COL_DATE_ARCHIVED + "] TEXT,[" +
                NoteContract.Notes.COL_DATE_DELETED + "] TEXT,[" +
                NoteContract.Notes.COL_LAST_MODIFIED_DATE + "] TEXT,[" +
                NoteContract.Notes.COL_DATE_SYNC + "] TEXT,[" +
                NoteContract.Notes.COL_SYNCED + "] TINYINT,[" +
                NoteContract.Notes.COL_OWNER + "] TEXT);");
    }
}
