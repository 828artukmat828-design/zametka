package com.mytasks.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS = "mytasks_prefs";
    private static final String KEY_TASKS = "tasks";

    private static class Task {
        String text;
        boolean done;

        Task(String text, boolean done) {
            this.text = text;
            this.done = done;
        }
    }

    private final List<Task> tasks = new ArrayList<>();
    private TaskAdapter adapter;
    private EditText input;
    private TextView counter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadTasks();
        buildUi();
        updateCounter();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        // Строка ввода: поле + кнопка «Добавить»
        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(this);
        input.setHint("Новая задача или план");
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTaskFromInput();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputRow.addView(input, inputParams);

        Button addButton = new Button(this);
        addButton.setText("Добавить");
        addButton.setOnClickListener(v -> addTaskFromInput());
        inputRow.addView(addButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(inputRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Счётчик
        counter = new TextView(this);
        counter.setTextSize(14);
        counter.setPadding(dp(4), dp(8), dp(4), dp(8));
        root.addView(counter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Список
        ListView listView = new ListView(this);
        adapter = new TaskAdapter();
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Надпись, когда задач нет
        emptyView = new TextView(this);
        emptyView.setText("Пока нет задач. Добавьте первую выше.");
        emptyView.setTextSize(16);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(16), dp(32), dp(16), dp(32));
        root.addView(emptyView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Кнопка «Удалить выполненные»
        Button clearDone = new Button(this);
        clearDone.setText("Удалить все выполненные");
        clearDone.setOnClickListener(v -> confirmClearDone());
        root.addView(clearDone, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void addTaskFromInput() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        tasks.add(0, new Task(text, false));
        input.setText("");
        saveTasks();
        adapter.notifyDataSetChanged();
        updateCounter();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }

    private void confirmDelete(final int position) {
        if (position < 0 || position >= tasks.size()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Удалить задачу?")
                .setMessage(tasks.get(position).text)
                .setPositiveButton("Удалить", (d, w) -> {
                    if (position < tasks.size()) {
                        tasks.remove(position);
                        saveTasks();
                        adapter.notifyDataSetChanged();
                        updateCounter();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void confirmClearDone() {
        int doneCount = 0;
        for (Task t : tasks) {
            if (t.done) {
                doneCount++;
            }
        }
        if (doneCount == 0) {
            new AlertDialog.Builder(this)
                    .setMessage("Выполненных задач нет.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Удалить выполненные?")
                .setMessage("Будет удалено задач: " + doneCount)
                .setPositiveButton("Удалить", (d, w) -> {
                    for (int i = tasks.size() - 1; i >= 0; i--) {
                        if (tasks.get(i).done) {
                            tasks.remove(i);
                        }
                    }
                    saveTasks();
                    adapter.notifyDataSetChanged();
                    updateCounter();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateCounter() {
        int done = 0;
        for (Task t : tasks) {
            if (t.done) {
                done++;
            }
        }
        counter.setText("Выполнено: " + done + " из " + tasks.size());
        emptyView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveTasks() {
        JSONArray array = new JSONArray();
        try {
            for (Task t : tasks) {
                JSONObject obj = new JSONObject();
                obj.put("text", t.text);
                obj.put("done", t.done);
                array.put(obj);
            }
        } catch (JSONException e) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_TASKS, array.toString()).apply();
    }

    private void loadTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = prefs.getString(KEY_TASKS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                tasks.add(new Task(obj.getString("text"), obj.optBoolean("done", false)));
            }
        } catch (JSONException e) {
            tasks.clear();
        }
    }

    private class TaskAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return tasks.size();
        }

        @Override
        public Object getItem(int position) {
            return tasks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            CheckBox check;
            Button delete;

            if (convertView == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(4), dp(6), dp(4), dp(6));

                check = new CheckBox(MainActivity.this);
                check.setTextSize(17);
                check.setId(1);
                row.addView(check, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                delete = new Button(MainActivity.this);
                delete.setText("✕");
                delete.setId(2);
                delete.setMinWidth(dp(48));
                delete.setMinimumWidth(dp(48));
                row.addView(delete, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                row = (LinearLayout) convertView;
                check = row.findViewById(1);
                delete = row.findViewById(2);
            }

            final Task task = tasks.get(position);

            // Сначала убираем старый слушатель, чтобы не сработал при смене состояния
            check.setOnCheckedChangeListener(null);
            check.setText(task.text);
            check.setChecked(task.done);
            applyDoneStyle(check, task.done);

            check.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.done = isChecked;
                applyDoneStyle((CheckBox) buttonView, isChecked);
                saveTasks();
                updateCounter();
            });

            delete.setOnClickListener(v -> confirmDelete(position));

            return row;
        }

        private void applyDoneStyle(CheckBox check, boolean done) {
            if (done) {
                check.setPaintFlags(check.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                check.setAlpha(0.5f);
                check.setTypeface(Typeface.DEFAULT);
            } else {
                check.setPaintFlags(check.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                check.setAlpha(1f);
                check.setTypeface(Typeface.DEFAULT_BOLD);
            }
        }
    }
}
