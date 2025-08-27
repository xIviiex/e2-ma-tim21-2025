package com.team21.questify.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.application.model.Task;
import com.team21.questify.application.model.TaskOccurrence;

import java.util.List;
import java.util.Map;

public class TaskForDayAdapter extends RecyclerView.Adapter<TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(TaskOccurrence occurrence);
    }
    private List<TaskOccurrence> occurrenceList;
    private Map<String, Task> tasksMap;
    private Map<String, Integer> categoryColorMap;
    private final OnTaskClickListener listener;
    public TaskForDayAdapter(List<TaskOccurrence> occurrenceList, Map<String, Task> tasksMap, Map<String, Integer> categoryColorMap, OnTaskClickListener listener) {
        this.occurrenceList = occurrenceList;
        this.tasksMap = tasksMap;
        this.categoryColorMap = categoryColorMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view, tasksMap, categoryColorMap, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(occurrenceList.get(position));
    }

    @Override
    public int getItemCount() {
        return occurrenceList.size();
    }

    public void setOccurrences(List<TaskOccurrence> newOccurrences) {
        this.occurrenceList = newOccurrences;
        notifyDataSetChanged();
    }
}
