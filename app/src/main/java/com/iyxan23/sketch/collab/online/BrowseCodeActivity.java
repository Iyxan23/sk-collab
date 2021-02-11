package com.iyxan23.sketch.collab.online;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.iyxan23.sketch.collab.R;
import com.iyxan23.sketch.collab.Util;
import com.iyxan23.sketch.collab.helpers.PatchHelper;
import com.iyxan23.sketch.collab.models.Commit;
import com.iyxan23.sketch.collab.models.SketchwareProject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import name.fraser.neil.plaintext.diff_match_patch;

public class BrowseCodeActivity extends AppCompatActivity {

    TextView code_logic;
    TextView code_view;
    TextView code_file;
    TextView code_library;
    TextView code_resource;

    ArrayList<Commit> c_commits = new ArrayList<>();
    ArrayList<String> commit_ids = new ArrayList<>();

    // Project data in their decrypted string format
    HashMap<String, String> project_data = new HashMap<>();

    int current_commit_index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_code);

        // Bind these textviews
        code_logic      = findViewById(R.id.code_logic      );
        code_view       = findViewById(R.id.code_view       );
        code_file       = findViewById(R.id.code_file       );
        code_library    = findViewById(R.id.code_library    );
        code_resource   = findViewById(R.id.code_resource   );

        // Get the project key and project name
        Intent intent = getIntent();
        String project_key  = intent.getStringExtra("project_key"   );
        String project_name = intent.getStringExtra("project_name"  );
        
        ((TextView) findViewById(R.id.project_name)).setText("on " + project_name);

        // Fetch the snapshot of the project
        new Thread(() -> {
            try {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                CollectionReference project_snapshot = firestore.collection("projects").document(project_key).collection("snapshot");
                CollectionReference project_commits  = firestore.collection("projects").document(project_key).collection("commits");

                String[] keys = new String[] {"mysc_project", "logic", "view", "library", "resource", "file"};

                // Get the snapshot, get the commits, and apply the commits to the snapshot
                QuerySnapshot snapshot = Tasks.await(project_snapshot.get());
                QuerySnapshot commits  = Tasks.await(project_commits .orderBy("timestamp", Query.Direction.ASCENDING).get());

                for (DocumentSnapshot doc: snapshot.getDocuments()) {
                    project_data.put(doc.getId(), Util.decrypt(doc.getBlob("data").toBytes()));
                }

                diff_match_patch dmp = new diff_match_patch();

                int index = -1;  // -1 coz we're adding the index in the start of the loop

                // Apply the patch
                for (DocumentSnapshot commit: commits) {
                    index++;

                    HashMap<String, String> patch = (HashMap<String, String>) commit.get("patch");

                    if (patch == null) continue;

                    for (String key: keys) {
                        if (!patch.containsKey(key)) continue;

                        LinkedList<diff_match_patch.Patch> patches = (LinkedList<diff_match_patch.Patch>) dmp.patch_fromText(patch.get(key));
                        // TODO: CHECK PATCH STATUSES
                        Object[] result = dmp.patch_apply(patches, project_data.get(key));

                        project_data.put(key, (String) result[0]);
                    }

                    // Also save the commits, we can use this for the commit history jumping
                    Commit c = new Commit();

                    c.id = commit.getId();
                    c.patch = patch;

                    c_commits.add(c);

                    // Ah yes, save it to the index so we can find a commit faster
                    commit_ids.add(commit.getId());
                }

                updateCode(project_data);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();

                Toast.makeText(this, "An error occured: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            current_commit_index = commit_ids.size() - 1;
        }).start();
    }

    @SuppressLint("WrongConstant")
    public void chevron_logic(View view) {
        // Kinda hacky, but this code is to toggle the view's visibility between VISIBLE (0x0) and GONE (0x8)
        // I don't really like the branching way of doing it, so I'm doing it mathematically
        code_logic.setVisibility(code_logic.getVisibility() ^ View.GONE);

        // Rotate the chevron 180 degree(s)
        view.setRotation((view.getRotation() + 180) % 360);
    }

    @SuppressLint("WrongConstant")
    public void chevron_view(View view) {
        code_view.setVisibility(code_view.getVisibility() ^ View.GONE);

        // Rotate the chevron 180 degree(s)
        view.setRotation((view.getRotation() + 180) % 360);
    }

    @SuppressLint("WrongConstant")
    public void chevron_file(View view) {
        code_file.setVisibility(code_file.getVisibility() ^ View.GONE);

        // Rotate the chevron 180 degree(s)
        view.setRotation((view.getRotation() + 180) % 360);
    }

    @SuppressLint("WrongConstant")
    public void chevron_library(View view) {
        code_library.setVisibility(code_library.getVisibility() ^ View.GONE);

        // Rotate the chevron 180 degree(s)
        view.setRotation((view.getRotation() + 180) % 360);
    }

    @SuppressLint("WrongConstant")
    public void chevron_resource(View view) {
        code_resource.setVisibility(code_resource.getVisibility() ^ View.GONE);

        // Rotate the chevron 180 degree(s)
        view.setRotation((view.getRotation() + 180) % 360);
    }

    private void updateCode(HashMap<String, String> project_data) {
        runOnUiThread(() -> {
            code_logic.setText(project_data.get("logic"));
            code_view.setText(project_data.get("view"));
            code_file.setText(project_data.get("file"));
            code_library.setText(project_data.get("library"));
            code_resource.setText(project_data.get("resource"));
        });
    }

    public void open_commit_history(View view) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle("Select a commit to jump into: ");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        arrayAdapter.addAll(commit_ids);

        builderSingle.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builderSingle.setAdapter(arrayAdapter, (dialog, which) -> {
            Toast.makeText(this, "Please wait", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                updateCode(PatchHelper.go_to_commit(project_data, c_commits, current_commit_index, which));

                runOnUiThread(() -> {
                    TextView commit_id = findViewById(R.id.commit_id_browse_code);
                    commit_id.setText("At: " + c_commits.get(which).id);
                    commit_id.setVisibility(View.VISIBLE);
                });

                current_commit_index = which;

                runOnUiThread(dialog::dismiss);
            }).start();
        });

        builderSingle.show();
    }
}