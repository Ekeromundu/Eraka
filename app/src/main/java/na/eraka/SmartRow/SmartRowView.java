package na.eraka.SmartRow;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import na.eraka.R;

import java.util.List;

public class SmartRowView extends LinearLayout {

    public enum Scope {
        GENERIC
    }

    private RecyclerView recyclerView;
    private android.view.View moreLeft, moreRight;
    private SmartRowAdapter adapter;
    private SmartRowDataSource dataSource;

    public interface OnItemClickListener {
        void onItemClick(SmartRowItem item);
    }

    public SmartRowView(Context context) {
        super(context);
        init(context);
    }

    public SmartRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.smart_row_view, this);
        recyclerView = findViewById(R.id.smartRowRecycler);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        adapter = new SmartRowAdapter();
        recyclerView.setAdapter(adapter);
        setUpMoreArrows();
    }

    /**
     * Eraka 27 Oct 2026: round arrows at the ends of the row show that more suggestions are
     * hidden to the side. Each is shown only while the row can still scroll that way, and a tap
     * on it scrolls most of a screen width.
     */
    private void setUpMoreArrows() {
        moreLeft = findViewById(R.id.smartRowMoreLeft);
        moreRight = findViewById(R.id.smartRowMoreRight);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) { updateMoreArrows(); }
        });
        recyclerView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateMoreArrows());
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() { recyclerView.post(SmartRowView.this::updateMoreArrows); }
        });
        moreRight.setOnClickListener(v -> recyclerView.smoothScrollBy((int) (recyclerView.getWidth() * 0.7f), 0));
        moreLeft.setOnClickListener(v -> recyclerView.smoothScrollBy(-(int) (recyclerView.getWidth() * 0.7f), 0));
    }

    private void updateMoreArrows() {
        if (moreLeft == null || moreRight == null) return;
        moreLeft.setVisibility(recyclerView.canScrollHorizontally(-1) ? VISIBLE : GONE);
        moreRight.setVisibility(recyclerView.canScrollHorizontally(1) ? VISIBLE : GONE);
    }

    public void setDataSource(SmartRowDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        adapter.setOnItemClickListener(listener);
    }

    public void loadSuggestions(Scope scope, String userId) {
        if (dataSource == null) return;
        long now = System.currentTimeMillis();
        dataSource.getSuggestions(scope, now, userId, this::onSuggestionsLoaded);
    }

    /** Eraka: the suggestions currently shown in the smart row. */
    public List<SmartRowItem> getCurrentItems() {
        return adapter.getItems();
    }

    private void onSuggestionsLoaded(List<SmartRowItem> items) {
        adapter.setItems(items);
    }

    public interface SmartRowDataSource {
        void getSuggestions(Scope scope, long timestamp, String userId, Callback callback);

        interface Callback {
            void onResult(List<SmartRowItem> items);
        }
    }
}