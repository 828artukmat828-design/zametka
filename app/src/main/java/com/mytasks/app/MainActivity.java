package com.mytasks.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String[] NAV_ICONS = {"🏠", "🛒", "✅", "🔔", "📊"};
    private static final String[] NAV_LABELS = {"Бош саҳифа", "Заказлар", "Режа", "Эслатма", "Статистика"};
    private static final String[] MONTHS = {"январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"};
    private static final String[] DAYS = {"Якшанба", "Душанба", "Сешанба", "Чоршанба",
            "Пайшанба", "Жума", "Шанба"};
    private static final int[] STATUS_BG = {0xFFEDE9FE, 0xFFFFF1D6, 0xFFDDF5E9, 0xFFE8E8EE};
    private static final int[] STATUS_FG = {0xFF5B3FE0, 0xFFB26A00, 0xFF1E8E5A, 0xFF6B6B80};

    private Store store;
    private FrameLayout content;
    private final TextView[] navIcons = new TextView[5];
    private final TextView[] navLabels = new TextView[5];

    private int tab = 0;
    private String planDate = Store.today();
    private int orderFilter = -1; // -1 = барчаси

    // ============================================================
    // Запуск и общий каркас
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = Store.get(this);
        getWindow().setStatusBarColor(Ui.PRIMARY_DARK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        root.addView(buildNav(), new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        setContentView(root);
        showTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private int dp(int v) {
        return Ui.dp(this, v);
    }

    private View buildNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Ui.WHITE);
        nav.setPadding(0, dp(4), 0, dp(4));

        for (int i = 0; i < 5; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(6), 0, dp(6));

            TextView icon = Ui.text(this, NAV_ICONS[i], 20, Ui.TEXT, false);
            icon.setGravity(Gravity.CENTER);
            TextView label = Ui.text(this, NAV_LABELS[i], 11, Ui.MUTED, true);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);

            item.addView(icon, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            item.addView(label, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            item.setOnClickListener(v -> showTab(index));

            navIcons[i] = icon;
            navLabels[i] = label;
            nav.addView(item, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        }
        return nav;
    }

    private void showTab(int index) {
        tab = index;
        for (int i = 0; i < 5; i++) {
            boolean on = i == tab;
            navLabels[i].setTextColor(on ? Ui.PRIMARY : Ui.MUTED);
            navIcons[i].setAlpha(on ? 1f : 0.55f);
        }
        content.removeAllViews();
        refresh();
    }

    /** Перестраивает текущую вкладку, сохраняя положение прокрутки. */
    private void refresh() {
        int scrollY = 0;
        if (content.getChildCount() > 0 && content.getChildAt(0) instanceof ScrollView) {
            scrollY = content.getChildAt(0).getScrollY();
        }
        content.removeAllViews();

        View v;
        switch (tab) {
            case 1:
                v = buildOrders();
                break;
            case 2:
                v = buildPlan();
                break;
            case 3:
                v = buildReminders();
                break;
            case 4:
                v = buildStats();
                break;
            default:
                v = buildHome();
                break;
        }
        content.addView(v, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        if (v instanceof ScrollView) {
            final ScrollView sv = (ScrollView) v;
            final int y = scrollY;
            sv.post(() -> sv.scrollTo(0, y));
        }
    }

    private LinearLayout body() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(24));
        return l;
    }

    private ScrollView page(LinearLayout body) {
        ScrollView sv = new ScrollView(this);
        sv.addView(body, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        return sv;
    }

    private void confirm(String title, String message, final Runnable onYes) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ўчириш", (dialog, which) -> onYes.run())
                .setNegativeButton("Бекор қилиш", null)
                .show();
    }

    // ============================================================
    // Работа с датами
    // ============================================================

    private Calendar calOf(String key) {
        Calendar c = Calendar.getInstance();
        String[] p = key.split("-");
        c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 12, 0, 0);
        return c;
    }

    private String prettyDate(String key) {
        Calendar c = calOf(key);
        return c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)]
                + ", " + DAYS[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private String prettyDateTime(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)] + " "
                + c.get(Calendar.YEAR) + ", "
                + String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private void shiftPlanDate(int days) {
        Calendar c = calOf(planDate);
        c.add(Calendar.DAY_OF_MONTH, days);
        planDate = Store.dateKey(c);
        refresh();
    }

    // ============================================================
    // Задачи (общие помощники)
    // ============================================================

    /** Задачи дня: сначала невыполненные, выполненные автоматически уходят вниз. */
    private List<Store.Task> tasksFor(String date) {
        List<Store.Task> open = new ArrayList<>();
        List<Store.Task> closed = new ArrayList<>();
        for (Store.Task t : store.tasks) {
            if (t.date.equals(date)) {
                if (t.done) {
                    closed.add(t);
                } else {
                    open.add(t);
                }
            }
        }
        open.addAll(closed);
        return open;
    }

    private int countDone(List<Store.Task> list) {
        int n = 0;
        for (Store.Task t : list) {
            if (t.done) {
                n++;
            }
        }
        return n;
    }

    private View taskRow(final Store.Task t, boolean withDelete) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox cb = new CheckBox(this);
        cb.setText(t.text);
        cb.setTextSize(16);
        cb.setTextColor(Ui.TEXT);
        cb.setChecked(t.done);
        if (t.done) {
            cb.setPaintFlags(cb.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            cb.setAlpha(0.5f);
        }
        cb.setOnCheckedChangeListener((button, checked) -> {
            t.done = checked;
            store.saveTasks();
            content.post(this::refresh);
        });
        row.addView(cb, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));

        if (withDelete) {
            TextView del = Ui.text(this, "✕", 18, Ui.MUTED, true);
            del.setPadding(dp(14), dp(10), dp(6), dp(10));
            del.setOnClickListener(v -> confirm("Вазифани ўчирасизми?", t.text, () -> {
                store.tasks.remove(t);
                store.saveTasks();
                refresh();
            }));
            row.addView(del);
        }
        return row;
    }

    private void addTask(String text, String date) {
        String s = text.trim();
        if (s.isEmpty()) {
            return;
        }
        Store.Task t = new Store.Task();
        t.id = Store.newId();
        t.text = s;
        t.date = date;
        t.done = false;
        store.tasks.add(t);
        store.saveTasks();
    }

    private void showAddTaskDialog(final String date) {
        final EditText input = new EditText(this);
        input.setHint("Вазифа ёки режа");
        input.setSingleLine(true);
        FrameLayout box = new FrameLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(input, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        new AlertDialog.Builder(this)
                .setTitle("Режа қўшиш")
                .setView(box)
                .setPositiveButton("Қўшиш", (dialog, which) -> {
                    addTask(input.getText().toString(), date);
                    refresh();
                })
                .setNegativeButton("Бекор қилиш", null)
                .show();
    }

    // ============================================================
    // 1. Бош саҳифа
    // ============================================================

    private View buildHome() {
        LinearLayout b = body();
        b.addView(Ui.text(this, "Салом, Артухмат! 👋", 24, Ui.TEXT, true));

        TextView dateBar = Ui.text(this, "📅  " + prettyDate(Store.today()), 16, Ui.WHITE, true);
        dateBar.setGravity(Gravity.CENTER);
        dateBar.setPadding(dp(12), dp(14), dp(12), dp(14));
        dateBar.setBackground(Ui.bg(this, Ui.PRIMARY, 16));
        b.addView(dateBar, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 12, 0, 12));

        // Сегодняшний план
        List<Store.Task> list = tasksFor(Store.today());
        int done = countDone(list);

        LinearLayout card = Ui.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.addView(Ui.text(this, "Бугунги режа:", 17, Ui.TEXT, true), Ui.lpw(this, 0, Ui.WRAP, 1f));
        head.addView(Ui.text(this, done + " / " + list.size(), 17, Ui.PRIMARY, true));
        card.addView(head);
        card.addView(Ui.progress(this, done, list.size()), Ui.lp(this, Ui.MATCH, 10, 0, 10, 0, 6));

        if (list.isEmpty()) {
            card.addView(Ui.text(this, "Бугунга вазифа йўқ. «Режа қўшиш» тугмасини босинг.",
                    14, Ui.MUTED, false));
        }
        int shown = Math.min(5, list.size());
        for (int i = 0; i < shown; i++) {
            card.addView(taskRow(list.get(i), false));
        }
        if (list.size() > 5) {
            card.addView(Ui.text(this, "… ва яна " + (list.size() - 5) + " та", 14, Ui.MUTED, false));
        }
        if (!list.isEmpty() && done == list.size()) {
            card.addView(Ui.text(this, "🎉 Бугунги барча вазифалар бажарилди!", 15, Ui.GREEN, true),
                    Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 8, 0, 0));
        }
        b.addView(card, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        // Заказлар
        int newCount = 0;
        for (Store.Order o : store.orders) {
            if (o.status == 0) {
                newCount++;
            }
        }
        LinearLayout oc = Ui.card(this);
        oc.setBackground(Ui.bg(this, Ui.PRIMARY_LIGHT, 16));
        oc.setOrientation(LinearLayout.HORIZONTAL);
        oc.setGravity(Gravity.CENTER_VERTICAL);
        oc.addView(Ui.text(this, "🛒  Заказлар", 17, Ui.TEXT, true), Ui.lpw(this, 0, Ui.WRAP, 1f));
        TextView badge = Ui.text(this, newCount > 0 ? newCount + " янги" : "Янги йўқ",
                13, newCount > 0 ? Ui.WHITE : Ui.MUTED, true);
        badge.setPadding(dp(12), dp(5), dp(12), dp(5));
        badge.setBackground(Ui.bg(this, newCount > 0 ? Ui.RED : Ui.WHITE, 14));
        oc.addView(badge);
        oc.setOnClickListener(v -> showTab(1));
        b.addView(oc, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        // Кнопки
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView newOrder = Ui.button(this, "🛒  Янги заказ", true);
        newOrder.setOnClickListener(v -> showOrderDialog(null));
        TextView newTask = Ui.button(this, "📝  Режа қўшиш", false);
        newTask.setOnClickListener(v -> showAddTaskDialog(Store.today()));
        buttons.addView(newOrder, Ui.lp(this, 0, Ui.WRAP, 0, 0, 6, 0));
        ((LinearLayout.LayoutParams) newOrder.getLayoutParams()).weight = 1f;
        buttons.addView(newTask, Ui.lp(this, 0, Ui.WRAP, 6, 0, 0, 0));
        ((LinearLayout.LayoutParams) newTask.getLayoutParams()).weight = 1f;
        b.addView(buttons, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        return page(b);
    }

    // ============================================================
    // 2. Кундалик режа
    // ============================================================

    private View buildPlan() {
        LinearLayout b = body();
        b.addView(Ui.text(this, "Кундалик режа", 24, Ui.TEXT, true));

        // Переключатель даты
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setBackground(Ui.bg(this, Ui.PRIMARY, 16));
        nav.setPadding(dp(6), dp(6), dp(6), dp(6));

        TextView prev = Ui.text(this, "‹", 28, Ui.WHITE, true);
        prev.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> shiftPlanDate(-1));
        TextView next = Ui.text(this, "›", 28, Ui.WHITE, true);
        next.setGravity(Gravity.CENTER);
        next.setOnClickListener(v -> shiftPlanDate(1));

        String label = "📅  " + prettyDate(planDate) + (planDate.equals(Store.today()) ? " (бугун)" : "");
        TextView dateText = Ui.text(this, label, 16, Ui.WHITE, true);
        dateText.setGravity(Gravity.CENTER);
        dateText.setPadding(0, dp(8), 0, dp(8));
        dateText.setOnClickListener(v -> {
            Calendar c = calOf(planDate);
            new DatePickerDialog(this, (picker, y, m, day) -> {
                planDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, day);
                refresh();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        nav.addView(prev, new LinearLayout.LayoutParams(dp(48), Ui.WRAP));
        nav.addView(dateText, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        nav.addView(next, new LinearLayout.LayoutParams(dp(48), Ui.WRAP));
        b.addView(nav, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 12, 0, 12));

        // Ввод новой задачи
        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        final EditText input = new EditText(this);
        input.setHint("Янги вазифа…");
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        TextView addBtn = Ui.button(this, "Қўшиш", true);

        final Runnable doAdd = () -> {
            addTask(input.getText().toString(), planDate);
            refresh();
        };
        addBtn.setOnClickListener(v -> doAdd.run());
        input.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doAdd.run();
                return true;
            }
            return false;
        });
        addRow.addView(input, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        addRow.addView(addBtn, Ui.lp(this, Ui.WRAP, Ui.WRAP, 8, 0, 0, 0));
        b.addView(addRow, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        // Список
        List<Store.Task> list = tasksFor(planDate);
        int done = countDone(list);

        LinearLayout card = Ui.card(this);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.addView(Ui.text(this, "Бажарилди", 16, Ui.TEXT, true), Ui.lpw(this, 0, Ui.WRAP, 1f));
        head.addView(Ui.text(this, done + " / " + list.size(), 16, Ui.PRIMARY, true));
        card.addView(head);
        card.addView(Ui.progress(this, done, list.size()), Ui.lp(this, Ui.MATCH, 10, 0, 10, 0, 6));

        if (list.isEmpty()) {
            card.addView(Ui.text(this, "Бу кунга вазифа йўқ.", 14, Ui.MUTED, false));
        }
        for (Store.Task t : list) {
            card.addView(taskRow(t, true));
        }
        if (!list.isEmpty() && done == list.size()) {
            card.addView(Ui.text(this, "🎉 Ҳамма вазифа бажарилди!", 15, Ui.GREEN, true),
                    Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 8, 0, 0));
        }
        b.addView(card, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        return page(b);
    }

    // ============================================================
    // 3. Заказлар
    // ============================================================

    private View buildOrders() {
        LinearLayout b = body();
        b.addView(Ui.text(this, "Заказлар", 24, Ui.TEXT, true));

        TextView newBtn = Ui.button(this, "＋  Янги заказ", true);
        newBtn.setOnClickListener(v -> showOrderDialog(null));
        b.addView(newBtn, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 12, 0, 12));

        // Фильтр по статусу
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(chips);

        chips.addView(chip("Барчаси " + store.orders.size(), -1));
        for (int s = 0; s < Store.STATUSES.length; s++) {
            int n = 0;
            for (Store.Order o : store.orders) {
                if (o.status == s) {
                    n++;
                }
            }
            chips.addView(chip(Store.STATUSES[s] + " " + n, s));
        }
        b.addView(hs, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        // Список (новые сверху)
        int shown = 0;
        for (int i = store.orders.size() - 1; i >= 0; i--) {
            Store.Order o = store.orders.get(i);
            if (orderFilter != -1 && o.status != orderFilter) {
                continue;
            }
            b.addView(orderCard(o), Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
            shown++;
        }
        if (shown == 0) {
            TextView empty = Ui.text(this, "Заказлар йўқ.", 15, Ui.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            b.addView(empty);
        }
        return page(b);
    }

    private View chip(String label, final int filter) {
        boolean on = orderFilter == filter;
        TextView t = Ui.text(this, label, 14, on ? Ui.WHITE : Ui.PRIMARY, true);
        t.setPadding(dp(14), dp(8), dp(14), dp(8));
        t.setBackground(Ui.bg(this, on ? Ui.PRIMARY : Ui.PRIMARY_LIGHT, 18));
        t.setOnClickListener(v -> {
            orderFilter = filter;
            refresh();
        });
        t.setLayoutParams(Ui.lp(this, Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
        return t;
    }

    private View orderCard(final Store.Order o) {
        LinearLayout card = Ui.card(this);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = Ui.text(this, "🛍", 20, Ui.TEXT, false);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(this, 0xFFFFE5E7, 24));
        top.addView(icon, Ui.lp(this, 44, 44, 0, 0, 12, 0));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        String title = String.format(Locale.US, "№%03d", o.number)
                + (o.customer.isEmpty() ? "" : "  " + o.customer);
        mid.addView(Ui.text(this, title, 16, Ui.TEXT, true));
        String sub = (o.table.isEmpty() ? "" : "Стол " + o.table + " · ")
                + o.qty + " дона · " + Ui.money(o.total) + " · " + o.pay;
        mid.addView(Ui.text(this, sub, 13, Ui.MUTED, false));
        top.addView(mid, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));

        TextView badge = Ui.text(this, Store.STATUSES[o.status], 12, STATUS_FG[o.status], true);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        badge.setBackground(Ui.bg(this, STATUS_BG[o.status], 12));
        top.addView(badge);
        card.addView(top);

        if (!o.items.isEmpty()) {
            TextView items = Ui.text(this, o.items, 14, Ui.TEXT, false);
            items.setPadding(0, dp(8), 0, 0);
            card.addView(items);
        }

        if (o.status < Store.STATUSES.length - 1) {
            TextView next = Ui.button(this, "▶  " + Store.STATUSES[o.status + 1], false);
            next.setOnClickListener(v -> {
                o.status++;
                store.saveOrders();
                refresh();
            });
            card.addView(next, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 10, 0, 0));
        }

        card.setOnClickListener(v -> showOrderDialog(o));
        return card;
    }

    private static long num(EditText e, long def) {
        try {
            return Long.parseLong(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private EditText addField(LinearLayout form, String label, String hint, int inputType) {
        TextView l = Ui.text(this, label, 13, Ui.MUTED, true);
        form.addView(l, Ui.lp(this, Ui.WRAP, Ui.WRAP, 0, 10, 0, 0));
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(inputType);
        form.addView(e, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        return e;
    }

    private Spinner addSpinner(LinearLayout form, String label, String[] values, int selected) {
        TextView l = Ui.text(this, label, 13, Ui.MUTED, true);
        form.addView(l, Ui.lp(this, Ui.WRAP, Ui.WRAP, 0, 10, 0, 0));
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, values));
        s.setSelection(selected);
        form.addView(s, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        return s;
    }

    /** Окно «Янги заказ» (existing == null) или редактирования. */
    private void showOrderDialog(final Store.Order existing) {
        final boolean edit = existing != null;

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(8));

        final EditText number = addField(form, "Заказ рақами", "№", InputType.TYPE_CLASS_NUMBER);
        final EditText customer = addField(form, "Мижоз исми", "Исм",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final EditText table = addField(form, "Стол рақами", "Масалан: 5", InputType.TYPE_CLASS_TEXT);
        final EditText items = addField(form, "Буюртма таомлари", "Масалан: 2 ош, 1 чой",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText qty = addField(form, "Сони", "1", InputType.TYPE_CLASS_NUMBER);
        final EditText total = addField(form, "Умумий нархи (сўм)", "0", InputType.TYPE_CLASS_NUMBER);
        final Spinner pay = addSpinner(form, "Тўлов тури", Store.PAYS,
                edit ? indexOf(Store.PAYS, existing.pay) : 0);
        final Spinner status = addSpinner(form, "Заказ ҳолати", Store.STATUSES,
                edit ? existing.status : 0);

        if (edit) {
            number.setText(String.valueOf(existing.number));
            customer.setText(existing.customer);
            table.setText(existing.table);
            items.setText(existing.items);
            qty.setText(String.valueOf(existing.qty));
            total.setText(String.valueOf(existing.total));
        } else {
            number.setText(String.valueOf(store.nextOrderNumber()));
            qty.setText("1");
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(form, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(edit ? "Заказни таҳрирлаш" : "Янги заказ")
                .setView(sv)
                .setPositiveButton("Сақлаш", null)
                .setNegativeButton("Бекор қилиш", null);
        if (edit) {
            builder.setNeutralButton("Ўчириш", (dialog, which) ->
                    confirm("Заказни ўчирасизми?",
                            String.format(Locale.US, "№%03d", existing.number), () -> {
                                store.orders.remove(existing);
                                store.saveOrders();
                                refresh();
                            }));
        }
        final AlertDialog dlg = builder.create();
        dlg.show();

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cust = customer.getText().toString().trim();
            String its = items.getText().toString().trim();
            if (cust.isEmpty() && its.isEmpty()) {
                Toast.makeText(this, "Мижоз исми ёки таомларни киритинг", Toast.LENGTH_SHORT).show();
                return;
            }
            Store.Order o = edit ? existing : new Store.Order();
            if (!edit) {
                o.id = Store.newId();
                o.created = System.currentTimeMillis();
                o.date = Store.today();
            }
            o.number = (int) num(number, store.nextOrderNumber());
            o.customer = cust;
            o.table = table.getText().toString().trim();
            o.items = its;
            o.qty = (int) Math.max(1, num(qty, 1));
            o.total = num(total, 0);
            o.pay = Store.PAYS[pay.getSelectedItemPosition()];
            o.status = status.getSelectedItemPosition();
            if (!edit) {
                store.orders.add(o);
            }
            store.saveOrders();
            dlg.dismiss();
            refresh();
        });
    }

    // ============================================================
    // 4. Эслатмалар
    // ============================================================

    private View buildReminders() {
        LinearLayout b = body();
        b.addView(Ui.text(this, "Эслатмалар", 24, Ui.TEXT, true));

        TextView addBtn = Ui.button(this, "＋  Эслатма қўшиш", true);
        addBtn.setOnClickListener(v -> showReminderDialog());
        b.addView(addBtn, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 12, 0, 12));

        long now = System.currentTimeMillis();
        List<Store.Reminder> upcoming = new ArrayList<>();
        List<Store.Reminder> past = new ArrayList<>();
        for (Store.Reminder r : store.reminders) {
            if (!r.done && r.time > now) {
                upcoming.add(r);
            } else {
                past.add(r);
            }
        }
        upcoming.sort((x, y) -> Long.compare(x.time, y.time));
        past.sort((x, y) -> Long.compare(y.time, x.time));

        if (store.reminders.isEmpty()) {
            TextView empty = Ui.text(this, "Эслатмалар йўқ.", 15, Ui.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            b.addView(empty);
        }
        for (Store.Reminder r : upcoming) {
            b.addView(reminderCard(r, false), Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        }
        for (Store.Reminder r : past) {
            b.addView(reminderCard(r, true), Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        }
        return page(b);
    }

    private View reminderCard(final Store.Reminder r, boolean past) {
        LinearLayout card = Ui.card(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        card.addView(Ui.text(this, past ? "✓" : "🔔", 20, past ? Ui.GREEN : Ui.TEXT, true),
                Ui.lp(this, 32, Ui.WRAP, 0, 0, 8, 0));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.addView(Ui.text(this, r.text, 16, Ui.TEXT, true));
        mid.addView(Ui.text(this, prettyDateTime(r.time) + (past ? " · ўтди" : ""),
                13, Ui.MUTED, false));
        card.addView(mid, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));

        TextView del = Ui.text(this, "✕", 18, Ui.MUTED, true);
        del.setPadding(dp(14), dp(10), dp(6), dp(10));
        del.setOnClickListener(v -> confirm("Эслатмани ўчирасизми?", r.text, () -> {
            ReminderReceiver.cancel(this, r.id);
            store.reminders.remove(r);
            store.saveReminders();
            refresh();
        }));
        card.addView(del);

        if (past) {
            card.setAlpha(0.6f);
        }
        return card;
    }

    private void showReminderDialog() {
        // Android 13+ требует разрешения на показ уведомлений
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        final Calendar picked = Calendar.getInstance();
        picked.add(Calendar.HOUR_OF_DAY, 1);
        picked.set(Calendar.SECOND, 0);
        picked.set(Calendar.MILLISECOND, 0);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(8));

        final EditText text = new EditText(this);
        text.setHint("Нимани эслатиш керак?");
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(text, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        final TextView dateTv = Ui.button(this, "", false);
        final TextView timeTv = Ui.button(this, "", false);
        form.addView(dateTv, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        form.addView(timeTv, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 8, 0, 0));

        final Runnable update = () -> {
            dateTv.setText("📅  " + prettyDate(Store.dateKey(picked)));
            timeTv.setText(String.format(Locale.US, "⏰  %02d:%02d",
                    picked.get(Calendar.HOUR_OF_DAY), picked.get(Calendar.MINUTE)));
        };
        update.run();

        dateTv.setOnClickListener(v -> new DatePickerDialog(this, (picker, y, m, day) -> {
            picked.set(y, m, day);
            update.run();
        }, picked.get(Calendar.YEAR), picked.get(Calendar.MONTH),
                picked.get(Calendar.DAY_OF_MONTH)).show());

        timeTv.setOnClickListener(v -> new TimePickerDialog(this, (picker, hour, minute) -> {
            picked.set(Calendar.HOUR_OF_DAY, hour);
            picked.set(Calendar.MINUTE, minute);
            update.run();
        }, picked.get(Calendar.HOUR_OF_DAY), picked.get(Calendar.MINUTE), true).show());

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Янги эслатма")
                .setView(form)
                .setPositiveButton("Сақлаш", null)
                .setNegativeButton("Бекор қилиш", null)
                .create();
        dlg.show();

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = text.getText().toString().trim();
            if (s.isEmpty()) {
                Toast.makeText(this, "Эслатма матнини киритинг", Toast.LENGTH_SHORT).show();
                return;
            }
            if (picked.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(this, "Вақт ўтиб кетган, келажак вақтни танланг", Toast.LENGTH_SHORT).show();
                return;
            }
            Store.Reminder r = new Store.Reminder();
            r.id = Store.newId();
            r.text = s;
            r.time = picked.getTimeInMillis();
            r.done = false;
            store.reminders.add(r);
            store.saveReminders();
            ReminderReceiver.schedule(this, r);
            dlg.dismiss();
            refresh();
        });
    }

    // ============================================================
    // 5. Статистика
    // ============================================================

    private LinearLayout section(String title) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, title, 17, Ui.TEXT, true), Ui.lp(this, Ui.WRAP, Ui.WRAP, 0, 0, 0, 6));
        return c;
    }

    private View statRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));
        row.addView(Ui.text(this, label, 15, Ui.MUTED, false), Ui.lpw(this, 0, Ui.WRAP, 1f));
        row.addView(Ui.text(this, value, 15, Ui.TEXT, true));
        return row;
    }

    private View buildStats() {
        LinearLayout b = body();
        b.addView(Ui.text(this, "Статистика", 24, Ui.TEXT, true));

        String today = Store.today();

        // --- Сегодня ---
        List<Store.Task> tt = tasksFor(today);
        int done = countDone(tt);
        int todayOrders = 0;
        long todaySum = 0;
        for (Store.Order o : store.orders) {
            if (o.date.equals(today)) {
                todayOrders++;
                todaySum += o.total;
            }
        }

        LinearLayout c1 = section("Бугун");
        c1.addView(statRow("Бажарилган вазифалар", done + " / " + tt.size()));
        c1.addView(Ui.progress(this, done, tt.size()), Ui.lp(this, Ui.MATCH, 10, 0, 4, 0, 8));
        c1.addView(statRow("Заказлар сони", String.valueOf(todayOrders)));
        c1.addView(statRow("Заказлар суммаси", Ui.money(todaySum)));
        for (String p : Store.PAYS) {
            long s = 0;
            for (Store.Order o : store.orders) {
                if (o.date.equals(today) && o.pay.equals(p)) {
                    s += o.total;
                }
            }
            c1.addView(statRow("   " + p, Ui.money(s)));
        }
        b.addView(c1, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 12, 0, 12));

        // --- Заказы по статусам (все время) ---
        LinearLayout c2 = section("Заказлар ҳолати (жами)");
        long allSum = 0;
        for (Store.Order o : store.orders) {
            allSum += o.total;
        }
        for (int s = 0; s < Store.STATUSES.length; s++) {
            int n = 0;
            for (Store.Order o : store.orders) {
                if (o.status == s) {
                    n++;
                }
            }
            c2.addView(statRow(Store.STATUSES[s], String.valueOf(n)));
        }
        c2.addView(statRow("Барча заказлар", String.valueOf(store.orders.size())));
        c2.addView(statRow("Умумий сумма", Ui.money(allSum)));
        b.addView(c2, Ui.lp(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        // --- Последние 7 дней ---
        LinearLayout c3 = section("Охирги 7 кун (вазифалар)");
        for (int i = 6; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, -i);
            List<Store.Task> day = tasksFor(Store.dateKey(c));
            int d = countDone(day);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));
            String lbl = String.format(Locale.US, "%02d.%02d",
                    c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1);
            row.addView(Ui.text(this, lbl, 14, Ui.MUTED, false), new LinearLayout.LayoutParams(dp(56), Ui.WRAP));
            row.addView(Ui.progress(this, d, day.size()), Ui.lp(this, 0, 10, 0, 0, 0, 0));
            ((LinearLayout.LayoutParams) row.getChildAt(1).getLayoutParams()).weight = 1f;
            row.addView(Ui.text(this, d + "/" + day.size(), 14, Ui.TEXT, true),
                    Ui.lp(this, 48, Ui.WRAP, 8, 0, 0, 0));
            ((TextView) row.getChildAt(2)).setGravity(Gravity.END);
            c3.addView(row);
        }
        b.addView(c3, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        return page(b);
    }
}
