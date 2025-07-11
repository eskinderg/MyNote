package app.mynote.fragments.note;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONObject;

import app.mynote.LoginActivity;
import app.mynote.core.callback.IAppCallback;
import app.mynote.core.utils.AppDate;
import app.mynote.core.utils.AppTimestamp;
import app.mynote.core.utils.GsonParser;
import mynote.R;
import retrofit2.Call;
import retrofit2.Response;

public class NoteFragment extends Fragment implements IAppCallback<Note>, MenuProvider {

    Note note;
    //    public EditText txtNoteText;
    public WebView webView;
    public EditText txtNoteHeader;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        Bundle args = getArguments();
        String personJsonString = args.getString("note");
        this.note = GsonParser.getGsonParser().fromJson(personJsonString, Note.class);

        getActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        this.webView = view.findViewById(R.id.webViewNoteText);
        this.txtNoteHeader = view.findViewById(R.id.txtNoteHeader);

        if (this.note.getHeader() != null) {
            txtNoteHeader.setText(this.note.getHeader());
        }

//            NoteFragment.this.txtNoteText.setText(Html.fromHtml(this.note.getText(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV).toString());
//            NoteFragment.this.webView.loadDataWithBaseURL(null, note.getText(), "text/html", "UTF-8", null);

        String html = "";
        if(note.getText() != null) {
            html = note.getText();
        }
        String htmlEditor = "<html><body contenteditable='true' style='padding:5px; padding-top:10px; border-top:1px solid lightgray;'>" +
                html +
                "</body></html>";
        NoteFragment.this.webView.getSettings().setJavaScriptEnabled(true);
        NoteFragment.this.webView.loadDataWithBaseURL(null, htmlEditor, "text/html", "UTF-8", null);

        this.txtNoteHeader.addTextChangedListener(new EditTextChangedListener<EditText>(txtNoteHeader) {
            @Override
            public void onTextChanged(EditText target, Editable s) {
                String header = txtNoteHeader.getText().toString().isEmpty() ? "" : txtNoteHeader.getText().toString();
                note.setHeader(header);
                NoteService.update(getContext(),note, true);

            }
        });

//        this.txtNoteText.addTextChangedListener(new EditTextChangedListener<EditText>(txtNoteText) {
//            @Override
//            public void onTextChanged(EditText target, Editable s) {
//                String body = txtNoteText.getText().toString().isEmpty() ? "" : Html.toHtml(txtNoteText.getText(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV);
//                note.setText(body);
//                NoteService.update(getContext(), note, true);
//            }
//        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                htmlSyncHandler.post(syncNoteRunnable);
            }
        });

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(this.note.getHeader());
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

    }

    private final Handler htmlSyncHandler = new Handler();
    private final int syncIntervalMs = 2000; // every 2 seconds

    private final Runnable syncNoteRunnable = new Runnable() {
        @Override
        public void run() {
            webView.evaluateJavascript(
                    "(function() { return document.body.innerHTML; })();",
                    html -> {
                        String cleanedHtml = decodeHtmlFromWebView(html);

                        // If you want to save it as raw HTML (not just plain text):
                        if(!note.getText().equals(cleanedHtml)) {
                            note.setText(cleanedHtml);
                            NoteService.update(getContext(), note, true);
                        }
                    }
            );
            htmlSyncHandler.postDelayed(this, syncIntervalMs);
        }
    };

    private String decodeHtmlFromWebView(String html) {
        if (html == null) return "";

        try {
            // The string returned is a JSON string, so wrap it in a dummy JSON object to decode
            return new JSONObject("{\"html\":" + html + "}").getString("html");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        htmlSyncHandler.removeCallbacks(syncNoteRunnable);
    }

    @Override
    public void onResponse(Call<Note> call, Response<Note> response) {
        if (response.isSuccessful()) {
            Toast.makeText(getContext(), "Saved", Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(getContext(), LoginActivity.class);
            getContext().startActivity(intent);
        }
    }

    @Override
    public void onFailure(Call<Note> call, Throwable t) {
        Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.note, menu);
    }
    private void saveHtmlThen(Runnable afterSave) {
        webView.evaluateJavascript(
                "(function() { return document.body.innerHTML; })();",
                html -> {
                    String cleanedHtml = decodeHtmlFromWebView(html);

                    // If you want to save it as raw HTML (not just plain text):
                    if(!note.getText().equals(cleanedHtml)) {
                        note.setText(cleanedHtml);
                        NoteService.update(getContext(), note, true);
                    }
                    if (afterSave != null) afterSave.run();
                }
        );
    }
    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
//        if (menuItem.getItemId() == R.id.action_save) {
//
//            String body = txtNoteText.getText().toString().isEmpty() ? "" : Html.toHtml(txtNoteText.getText(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV);
//            String header = txtNoteHeader.getText().toString().isEmpty() ? "" : txtNoteHeader.getText().toString();
////            note.setText(Html.toHtml( txtNoteText.getText(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV));
////            note.setHeader(txtNoteHeader.getText().toString());
//            note.setText(body);
//            note.setHeader(header);
//            noteService.update(note, true);
//            Toast.makeText(getContext(), "Note Saved", Toast.LENGTH_LONG).show();
//            return true;
//        }

        if (menuItem.getItemId() == 16908332) {
            saveHtmlThen(()->{
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_nav_note_to_nav_notes);
            });
            return true;
        }

        if(menuItem.getItemId() == R.id.action_archive){
            saveHtmlThen(()->{
                this.note.setArchived(true);
                this.note.setDateArchived(AppTimestamp.convertStringToTimestamp(AppDate.Now()));
                NoteService.update(getContext(), note, false);
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_nav_note_to_nav_notes);
            });
            return true;
        }

        return false;
    }

}