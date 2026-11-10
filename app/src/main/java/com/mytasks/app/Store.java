package com.mytasks.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Хранилище данных: задачи, заказы, напоминания. Всё лежит в памяти телефона (SharedPreferences). */
public class Store {

    public static final String[] STATUSES = {"Янги", "Тайёрланмоқда", "Тайёр", "Етказилди"};
    public static final String[] PAYS = {"Нақд", "Карта", "Онлайн"};

    public static class Task {
        public long id;
        public String text = "";
        public String date = "";
        public boolean done;
    }

    public static class Order {
        public long id;
        public int number;
        public String customer = "";
        public String table = "";
        public String items = "";
        public int qty = 1;
        public long total;
        public String pay = PAYS[0];
        public int status;
        public long created;
        public String date = "";
    }

    public static class Reminder {
        public long id;
        public String text = "";
        public long time;
        public boolean done;
    }

    private static Store instance;
    private static long lastId = 0;

    public static synchronized Store get(Context c) {
        if (instance == null) {
            instance = new Store(c.getApplicationContext());
        }
        return instance;
    }

    public static synchronized long newId() {
        long id = System.currentTimeMillis();
        if (id <= lastId) {
            id = lastId + 1;
        }
        lastId = id;
        return id;
    }

    public static String dateKey(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String today() {
        return dateKey(Calendar.getInstance());
    }

    private final SharedPreferences prefs;
    public final List<Task> tasks = new ArrayList<>();
    public final List<Order> orders = new ArrayList<>();
    public final List<Reminder> reminders = new ArrayList<>();

    private Store(Context c) {
        prefs = c.getSharedPreferences("zamenki_data", Context.MODE_PRIVATE);
        load();
    }

    public int nextOrderNumber() {
        int max = 0;
        for (Order o : orders) {
            if (o.number > max) {
                max = o.number;
            }
        }
        return max + 1;
    }

    // ---------- сохранение ----------

    public void saveTasks() {
        JSONArray arr = new JSONArray();
        try {
            for (Task t : tasks) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("text", t.text);
                o.put("date", t.date);
                o.put("done", t.done);
                arr.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        prefs.edit().putString("tasks", arr.toString()).apply();
    }

    public void saveOrders() {
        JSONArray arr = new JSONArray();
        try {
            for (Order x : orders) {
                JSONObject o = new JSONObject();
                o.put("id", x.id);
                o.put("number", x.number);
                o.put("customer", x.customer);
                o.put("table", x.table);
                o.put("items", x.items);
                o.put("qty", x.qty);
                o.put("total", x.total);
                o.put("pay", x.pay);
                o.put("status", x.status);
                o.put("created", x.created);
                o.put("date", x.date);
                arr.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        prefs.edit().putString("orders", arr.toString()).apply();
    }

    public void saveReminders() {
        JSONArray arr = new JSONArray();
        try {
            for (Reminder r : reminders) {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("text", r.text);
                o.put("time", r.time);
                o.put("done", r.done);
                arr.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        prefs.edit().putString("reminders", arr.toString()).apply();
    }

    // ---------- загрузка ----------

    private void load() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("tasks", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Task t = new Task();
                t.id = o.optLong("id");
                t.text = o.optString("text", "");
                t.date = o.optString("date", today());
                t.done = o.optBoolean("done", false);
                tasks.add(t);
            }
        } catch (JSONException e) {
            tasks.clear();
        }

        try {
            JSONArray arr = new JSONArray(prefs.getString("orders", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Order x = new Order();
                x.id = o.optLong("id");
                x.number = o.optInt("number");
                x.customer = o.optString("customer", "");
                x.table = o.optString("table", "");
                x.items = o.optString("items", "");
                x.qty = o.optInt("qty", 1);
                x.total = o.optLong("total", 0);
                x.pay = o.optString("pay", PAYS[0]);
                x.status = Math.max(0, Math.min(STATUSES.length - 1, o.optInt("status", 0)));
                x.created = o.optLong("created");
                x.date = o.optString("date", today());
                orders.add(x);
            }
        } catch (JSONException e) {
            orders.clear();
        }

        try {
            JSONArray arr = new JSONArray(prefs.getString("reminders", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Reminder r = new Reminder();
                r.id = o.optLong("id");
                r.text = o.optString("text", "");
                r.time = o.optLong("time");
                r.done = o.optBoolean("done", false);
                reminders.add(r);
            }
        } catch (JSONException e) {
            reminders.clear();
        }
    }
}
