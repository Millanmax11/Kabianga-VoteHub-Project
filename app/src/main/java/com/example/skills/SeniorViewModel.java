/*THIS IS FOR RESULTSENIOR to manage lifecycle of data from firebase to UI. */
package com.example.skills;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class SeniorViewModel extends ViewModel {
    private MutableLiveData<List<Object>> results;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Object>> getResults() {
        if (results == null) {
            results = new MutableLiveData<>();
            loadResults();
        }
        return results;
    }

    private void loadResults() {
        db.collection("SeniorVotes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Object> resultList = new ArrayList<>();
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
                            resultList.add(position);
                            resultList.addAll(results);
                            this.results.setValue(resultList); // Update LiveData
                        }
                    });
                }
            }
        });
    }
}
