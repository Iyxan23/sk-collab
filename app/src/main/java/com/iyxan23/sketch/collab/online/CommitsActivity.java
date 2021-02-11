package com.iyxan23.sketch.collab.online;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.iyxan23.sketch.collab.R;
import com.iyxan23.sketch.collab.adapters.CommitAdapter;
import com.iyxan23.sketch.collab.models.Commit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class CommitsActivity extends AppCompatActivity {

    public static final String TAG = "CommitsActivity";

    HashMap<String, String> cached_usernames = new HashMap<>();

    ArrayList<Commit> commits_ = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commits);

        String project_key = getIntent().getStringExtra("project_key");

        // One liner go brrr
        ((TextView) findViewById(R.id.commits_project_name)).setText("on " + getIntent().getStringExtra("project_name"));

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference commits_reference = firestore.collection("projects").document(project_key).collection("commits");

        RecyclerView commit_rv = findViewById(R.id.commits_rv);
        CommitAdapter adapter = new CommitAdapter(this);

        commit_rv.setLayoutManager(new LinearLayoutManager(this));
        commit_rv.setAdapter(adapter);

        // TODO: PAGINATION
        commits_reference
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    new Thread(() -> {
                        Log.d(TAG, "onCreate: onCompleteListener called.");
                        if (!task.isSuccessful()) {
                            Log.d(TAG, "onCreate: Task unsuccessful");
                            Toast.makeText(this, "Error while fetching commits: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }

                        QuerySnapshot commits = task.getResult();

                        for (QueryDocumentSnapshot commit : commits) {
                            String username;
                            String author_uid = commit.getString("author");

                            Log.d(TAG, "onCreate: author: " + author_uid);

                            if (!cached_usernames.containsKey(author_uid)) {
                                // Fetch the username

                                Log.d(TAG, "onCreate: Isn't cached");
                                Log.d(TAG, "onCreate: Fetching userdata for " + author_uid);

                                DocumentReference userdata = firestore.collection("userdata").document(author_uid);

                                try {
                                    DocumentSnapshot userdata_snapshot = Tasks.await(userdata.get());
                                    username = userdata_snapshot.getString("name");

                                    Log.d(TAG, "onCreate: Complete");
                                    Log.d(TAG, "onCreate: uid: " + author_uid + " username: " + username);

                                    cached_usernames.put(author_uid, username);

                                } catch (ExecutionException | InterruptedException e) {
                                    Log.e(TAG, "onCreate: Failed while fetching userdata of " + author_uid);
                                    e.printStackTrace();
                                    Toast.makeText(this, "Error while fetching userdata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    return;
                                }

                            } else {
                                Log.d(TAG, "onCreate: Username exists in cache");
                                username = cached_usernames.get(author_uid);
                            }

                            Commit c = new Commit();

                            c.id = commit.getId();
                            c.author_username = username;
                            c.author = commit.getString("author");
                            c.name = commit.getString("name");
                            c.sha512sum = commit.getString("sha512sum");
                            c.patch = (Map<String, String>) commit.get("patch");
                            c.timestamp = commit.getTimestamp("timestamp");

                            Log.d(TAG, "onCreate: Adding commit " + c.id + "to the list ");

                            commits_.add(c);
                            runOnUiThread(() -> adapter.updateView(commits_));
                        }

                        Log.d(TAG, "onCreate: Finished, exiting");
                        runOnUiThread(() -> findViewById(R.id.progressBar_commits).setVisibility(View.GONE));
                    }).start();
        });
    }
}