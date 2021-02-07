package com.iyxan23.sketch.collab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.iyxan23.sketch.collab.adapters.ChangesAdapter;
import com.iyxan23.sketch.collab.adapters.SearchAdapter;
import com.iyxan23.sketch.collab.models.SearchItem;
import com.iyxan23.sketch.collab.models.SketchwareProject;
import com.iyxan23.sketch.collab.models.SketchwareProjectChanges;
import com.iyxan23.sketch.collab.online.BrowseActivity;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // List of local projects
    ArrayList<SketchwareProject> localProjects = new ArrayList<>();

    // List of local sketchcollab projects
    ArrayList<SketchwareProject> sketchcollabProjects = new ArrayList<>();

    // List of changes
    ArrayList<SketchwareProjectChanges> changes = new ArrayList<>();

    // The changes adapter
    ChangesAdapter adapter;

    // Firebase stuff
    FirebaseFirestore database = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Get rid of the flasing white color while doing shared element transition
        getWindow().setEnterTransition(null);
        getWindow().setExitTransition(null);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate the adapter
        adapter = new ChangesAdapter(this);

        // RecyclerView stuff
        RecyclerView changes_rv = findViewById(R.id.changes_main);
        changes_rv.setLayoutManager(new LinearLayoutManager(this));
        changes_rv.setAdapter(adapter);

        // OnClicks
        // When you click the "Sketchware Projects" item
        findViewById(R.id.projects_main).setOnClickListener(v -> {

            // Move to SketchwareProjectsListActivity (with some shared elements transition)
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    MainActivity.this,

                    new Pair<>(findViewById(R.id.imageView8), "code_icon"),
                    new Pair<>(findViewById(R.id.textView8), "sketchware_projects_text")
            );

            Intent intent = new Intent(MainActivity.this, SketchwareProjectsListActivity.class);
            startActivity(intent, options.toBundle());
        });

        findViewById(R.id.repositories_main).setOnClickListener(v -> {
            /* TODO: SHARED ELEMENT TRANSITION BETWEEN MAINACTIVITY AND BROWSEACTIVITY
            // Move to BrowseActivity (with some shared elements transition)
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    MainActivity.this,

                    new Pair<>(findViewById(R.id.imageView8), "code_icon"),
                    new Pair<>(findViewById(R.id.textView8), "sketchware_projects_text")
            );
            */

            Intent intent = new Intent(MainActivity.this, BrowseActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Clear before initializing
        changes.clear();
        sketchcollabProjects.clear();
        localProjects.clear();
        adapter.updateView(changes);
        findViewById(R.id.no_changes_text_main).setVisibility(View.VISIBLE);

        // Check if Read and Write external storage permission is granted
        // why scoped storage? :(
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

            // Tell the user first of why you need to grant the permission
            Toast.makeText(this, "We need storage permission to access your sketchware projects. SketchCollab can misbehave if you denied the permission.", Toast.LENGTH_LONG).show();
            // Request the permission(s)
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    100);
        } else {
            // Permission is already granted, initialize
            initialize();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == PackageManager.PERMISSION_GRANTED) {
                initialize();
            }
        }
    }

    private void seperateSketchCollabProjects() {
        for (SketchwareProject project: localProjects) {
            try {
                // Is this project a sketchcollab project?
                if (project.isSketchCollabProject()) {
                    // Alright add it to the arraylist
                    sketchcollabProjects.add(project);
                }
            } catch (JSONException e) {
                // Hmm weird, user's project is corrupted
                e.printStackTrace();
            }
        }
    }

    private SketchwareProject querySnapshotToSketchwareProject(QuerySnapshot snapshot) {
        SketchwareProject project = new SketchwareProject();
        // Loop through the document and get every data/ files
        for (DocumentSnapshot doc_snapshot : snapshot.getDocuments()) {
            if (doc_snapshot.getId().equals("logic")) {
                project.logic = doc_snapshot.getBlob("data").toBytes();

            } else if (doc_snapshot.getId().equals("view")) {
                project.view = doc_snapshot.getBlob("data").toBytes();

            } else if (doc_snapshot.getId().equals("file")) {
                project.file = doc_snapshot.getBlob("data").toBytes();

            } else if (doc_snapshot.getId().equals("mysc_project")) {
                project.mysc_project = doc_snapshot.getBlob("data").toBytes();

            } else if (doc_snapshot.getId().equals("library")) {
                project.library = doc_snapshot.getBlob("data").toBytes();

            } else if (doc_snapshot.getId().equals("resource")) {
                project.resource = doc_snapshot.getBlob("data").toBytes();
            }
        }

        return project;
    }

    private void initialize() {
        new Thread(() -> {
            // Fetch sketchware projects
            localProjects = Util.fetch_sketchware_projects();

            // Check if it's empty
            if (localProjects.isEmpty()) return;

            // Get sketchcollab projects
            seperateSketchCollabProjects();

            // Check if there isn't any sketchcollab projects
            if (sketchcollabProjects.size() == 0)
                // Ight ima head out
                return;

            for (SketchwareProject project: sketchcollabProjects) {
                try {
                    // Check every project if the project has changed yet
                    // Get the latest project commit, project visibility, and the author
                    String project_commit = project.getSketchCollabLatestCommitID();
                    String project_key = project.getSketchCollabKey();
                    boolean is_project_public = project.isSketchCollabProjectPublic();
                    String author = project.getSketchCollabAuthorUid();

                    if (!author.equals(auth.getUid())) {
                        // Hmm, the user "stole" another user's project
                        // Let's skip this one :e_sweat_smile:
                        // =========================================================================
                        // Anyway, don't worry the user cannot edit the data in the database, it's
                        // protected by the firebase firestore rules.

                        continue;
                    }

                    // Fetch the latest project commit in the database
                    Task<QuerySnapshot> task =
                            database.collection(is_project_public ? "projects" : "userdata/" + author + "/projects").document(project_key).collection("commits")
                                    .orderBy("timestamp", Query.Direction.DESCENDING) // Order by the timestamp
                                    .limit(1) // I just wanted the latest commit, not every commit
                                    .get(Source.SERVER); // Don't get the cache :/

                    // Wait for the task to finish, i don't want to query a lot of tasks in a short amount of time
                    QuerySnapshot snapshot = Tasks.await(task);

                    // Check if task is successful or not
                    if (!task.isSuccessful()) {
                        assert task.getException() != null; // Exception shouldn't be null if the task is not successful

                        Toast.makeText(MainActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    assert snapshot != null; // snapshot shouldn't be null

                    DocumentSnapshot commit_info = snapshot.getDocuments().get(0);

                    if (!project_commit.equals(commit_info.getId())) {
                        // Hmm, looks like this man's project has an older commit, tell him to update his project
                    } else {
                        // This mans project has the same commit
                        // Check if this project also has the same shasum
                        String local_shasum = project.sha512sum();
                        String server_shasum = commit_info.getString("sha512sum");

                        // Check if they're the same
                        if (!local_shasum.equals(server_shasum)) {
                            // Alright looks like he's got some local updates with the same head commit

                            // Fetch the project
                            Task<QuerySnapshot> project_data =
                                    database.collection(is_project_public ? "projects" : "userdata/" + author + "/projects").document(project_key).collection("snapshot")
                                            .get(Source.SERVER); // Don't get the cache :/

                            // Wait for the task to finish, i don't want to query a lot of tasks in a short amount of time,
                            // it can cause some performance issues
                            QuerySnapshot project_data_snapshot = Tasks.await(project_data);
                            SketchwareProject head_project = querySnapshotToSketchwareProject(project_data_snapshot);

                            // Add this to the changed sketchcollab sketchware projects arraylist
                            changes.add(new SketchwareProjectChanges(project, head_project));

                            // Update the adapter
                            runOnUiThread(() -> {
                                // Remove the "no changes yet" text
                                findViewById(R.id.no_changes_text_main).setVisibility(View.GONE);

                                // update the adapter
                                adapter.updateView(changes);
                            });
                        } /* else {
                            // Boom, it's the same project with no updates
                            // ight ima head out
                        } */
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "JSON Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // THE SEARCH / COMMANDS THING =================================================================

    boolean isOpened = false;

    SearchAdapter s_adapter;
    ArrayList<SearchItem> s_items;
    RecyclerView s_rv;
    EditText s_edittext;

    HashMap<String, Integer> index = new HashMap<>();

    String[] commands = new String[] {
            "upload",    // goes to UploadActivity
            "local",     // goes to ViewLocalProjectActivity (soon)
            "open",      // goes to ViewOnlineProjectActivity
            "browse",    // goes to BrowseActivity
            "show code", // goes to BrowseCodeActivity
            "commits"    // goes to CommitsActivity
    };

    /* Syntaxes:
     *
     * upload (project name)
     * local
     * open (project name)
     * browse
     * show code (project name)
     * commits (project name)
     */

    public void search_button_click(View view) {
        s_edittext = findViewById(R.id.search_edittext);

        View settings_button = findViewById(R.id.imageView);
        View home_textview = findViewById(R.id.home_textview);
        View search_autocomplete_layout = findViewById(R.id.inc_search);
        View main_content = findViewById(R.id.scrollView2);

        s_rv = findViewById(R.id.search_rv);

        if (isOpened) {
            // Do a search
        } else {
            // Open the thing
            settings_button.setVisibility(View.GONE);
            home_textview.setVisibility(View.GONE);
            s_edittext.setVisibility(View.VISIBLE);
            search_autocomplete_layout.setVisibility(View.VISIBLE);
            main_content.setVisibility(View.GONE);

            // Initialize some stuff
            s_items = new ArrayList<>();
            for (String item: commands) {
                s_items.add(new SearchItem(item));
            }

            // Index localProjects to be a HashMap<String: Name, Integer: id>
            // Oh yeah, we're including project name and app name
            new Thread(() -> {
                for (SketchwareProject project : localProjects) {
                    if (project.metadata == null)
                        continue;

                    index.put(project.metadata.project_name, project.metadata.id);
                    index.put(project.metadata.app_name, project.metadata.id);
                }
            }).start();

            s_adapter = new SearchAdapter(s_items, this);

            s_rv.setAdapter(s_adapter);
            s_rv.setLayoutManager(new LinearLayoutManager(this));

            s_edittext.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void afterTextChanged(Editable s) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    update_rv(s.toString());
                }
            });
        }
    }

    private void update_rv(String input) {
        s_items.clear();

        Pattern pattern = Pattern.compile(input);

        for (String command: commands) {
            if (command.matches(input)) {
                s_items.add(new SearchItem(command, "Command"));
            }
        }

        s_adapter.updateView(s_items);

        // Find in local project(s)
        for (String name: index.keySet()) {
            boolean matched = false;

            Matcher m = pattern.matcher(name);

            while (m.find()) {
                matched = true;
                // TODO: CREATE A SPANNABLE THING
                // m.start();
                // m.end();
            }

            if (matched)
                s_items.add(new SearchItem(name, "Open Local Project"));
        }

        s_adapter.updateView(s_items);

        // TODO: ADD PUBLIC PROJECTS FUNCTIONALITY
    }

    @Override
    public void onBackPressed() {
        s_edittext = findViewById(R.id.search_edittext);

        View settings_button = findViewById(R.id.imageView);
        View home_textview = findViewById(R.id.home_textview);
        View search_autocomplete_layout = findViewById(R.id.inc_search);
        View main_content = findViewById(R.id.scrollView2);

        if (isOpened) {
            // Close the search thing
            settings_button.setVisibility(View.VISIBLE);
            home_textview.setVisibility(View.VISIBLE);
            s_edittext.setVisibility(View.GONE);
            search_autocomplete_layout.setVisibility(View.GONE);
            main_content.setVisibility(View.VISIBLE);

            // Clear some stuff to reduce the memory usage
            s_items.clear();
            index.clear();

        } else {
            super.onBackPressed();
        }
    }
}