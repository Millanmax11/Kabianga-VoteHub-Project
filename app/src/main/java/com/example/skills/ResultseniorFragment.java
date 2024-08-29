package com.example.skills;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ResultseniorFragment extends Fragment {
    private String username;
    private FirebaseAuth auth;

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ResultseniorAdapter resultseniorAdapter;
    private ProgressDialog progressDialog;
    private List<Object> resultList = new ArrayList<>();

    public ResultseniorFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resultsenior, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }

        toolbar.setNavigationOnClickListener(v -> {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerViewResults);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        resultseniorAdapter = new ResultseniorAdapter(resultList);
        recyclerView.setAdapter(resultseniorAdapter);

        SeniorViewModel viewModel;
        viewModel = new ViewModelProvider(this).get(SeniorViewModel.class);
        viewModel.getResults().observe(getViewLifecycleOwner(), new Observer<List<Object>>() {
            @Override
            public void onChanged(List<Object> results) {
                resultseniorAdapter.updateResults(results);
            }
        });

        // Get the username from FirebaseAuth or Bundle
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            username = user.getDisplayName(); // Assuming username is stored in displayName
        } else {
            // Get username from Bundle if passed as an argument
            Bundle args = getArguments();
            if (args != null) {
                username = args.getString("username");
            }
        }
        fetchResults();
        return view;
    }

    private void fetchResults() {
        // Check network connectivity
        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username == null) {
            Toast.makeText(getContext(), "Username not found. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Loading results...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        db.collection("SeniorVotes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Clear the resultList before adding new data
                resultList.clear();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String position = document.getId();
                    db.collection("SeniorVotes").document(position).collection("Student").get().addOnCompleteListener(subTask -> {
                        if (subTask.isSuccessful()) {
                            List<Result> results = new ArrayList<>();
                            for (QueryDocumentSnapshot subDocument : subTask.getResult()) {
                                Result result = subDocument.toObject(Result.class);
                                result.setPosition(position);
                                results.add(result);
                            }
                            updateUIWithResults(position, results);
                        } else {
                            hideProgressDialog();
                        }
                    });
                }
            } else {
                hideProgressDialog();
                Toast.makeText(getContext(), "Failed to load results.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateUIWithResults(String position, List<Result> results) {
        resultList.add(position);
        resultList.addAll(results);
        resultseniorAdapter.notifyDataSetChanged();
        hideProgressDialog();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork != null && activeNetwork.isConnected()){
            try {
                //performing a ping to the server
                InetAddress ipAddr = InetAddress.getByName("8.8.8.8");
                return !ipAddr.equals((""));
            }
            catch (Exception e){
                return false;
            }
        }
        else {
            return false;
        }
    }
}
