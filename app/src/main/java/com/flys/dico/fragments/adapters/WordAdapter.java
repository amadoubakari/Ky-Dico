package com.flys.dico.fragments.adapters;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.flys.dico.R;
import com.flys.dico.architecture.custom.IMainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author AMADOU BAKARI
 * @version 1.0.0
 * @email amadoubakari1992@gmail.com
 * @goal adapte a component word to view
 * @since 24/07/2020
 */
public class WordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_ITEM = 1;
    private final int VIEW_PROGRESS = 0;
    private int itemPerDisplay = 0;
    private List<Word> words;
    private Context context;
    private WordOnclickListener onclickListener;
    private OnLoadMoreListener onLoadMoreListener = null;
    private boolean loading;
    private final String TAG = "WordAdapter";
    private String searchText;

    public WordAdapter(Context context) {
        this.context = context;
    }

    public WordAdapter(List<Word> words, Context context) {
        this.words = words;
        this.context = context;
    }

    public WordAdapter(Context context, List<Word> words, int itemPerDisplay) {
        this.itemPerDisplay = itemPerDisplay;
        this.words = words;
        this.context = context;
    }

    public WordAdapter(Context context, List<Word> words, WordOnclickListener onclickListener) {
        this.words = words;
        this.context = context;
        this.onclickListener = onclickListener;
    }

    public WordAdapter(Context context, List<Word> words, String searchText, WordOnclickListener onclickListener) {
        this.words = words;
        this.context = context;
        this.searchText = searchText;
        this.onclickListener = onclickListener;
    }

    public WordAdapter(Context context, List<Word> words, int itemPerDisplay, WordOnclickListener onclickListener) {
        this.itemPerDisplay = itemPerDisplay;
        this.words = words;
        this.context = context;
        this.onclickListener = onclickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_ITEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.view_word_item, parent, false);
            return new Holder(view, this.onclickListener);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_progress, parent, false);
            return new HolderProgress(view, this.onclickListener);
        }

    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Word word = words.get(position);
        if (holder instanceof Holder) {
            Holder view = (Holder) holder;
            view.title.setText(word.getTitle());
            higherLightSearchedWord(word, view);
        } else {
            HolderProgress holderProgress = (HolderProgress) holder;
            holderProgress.progressBar.setIndeterminate(true);
        }
    }


    @Override
    public int getItemCount() {
        return words != null ? words.size() : 0;
    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        lastItemViewDetector(recyclerView);
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public int getItemViewType(int position) {
        return this.words.get(position).getTitle().isEmpty() && this.words.get(position).getDescription().isEmpty() ? VIEW_PROGRESS : VIEW_ITEM;
    }

    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title;
        TextView description;
        WordOnclickListener wordOnclickListener;

        public Holder(@NonNull View itemView, WordOnclickListener onclickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            wordOnclickListener = onclickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //wordOnclickListener.onWordClickListener(v, getAdapterPosition());
        }
    }


    class HolderProgress extends RecyclerView.ViewHolder implements View.OnClickListener {
        ProgressBar progressBar;

        public HolderProgress(@NonNull View itemView, WordOnclickListener onclickListener) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //wordOnclickListener.onWordClickListener(v, getAdapterPosition());
        }
    }


    public interface WordOnclickListener {
        void onWordClickListener(View v, int position);
    }

    /**
     * @param listModelsTasks
     */
    public void setFilter(List<Word> listModelsTasks) {
        words = new ArrayList<>();
        words.addAll(listModelsTasks);
        notifyDataSetChanged();
    }

    /**
     * @param listModelsTasks
     */
    public void setFilter(List<Word> listModelsTasks, String searchText) {
        words = new ArrayList<>();
        words.addAll(listModelsTasks);
        this.searchText = searchText;
        notifyDataSetChanged();
    }

    public void reload() {
        notifyDataSetChanged();
    }

    public void refreshAdapter() {
        notifyDataSetChanged();
    }

    public void addWords(List<Word> words1) {
        this.words.addAll(words1);
        notifyDataSetChanged();
    }

    public void removeAllWords() {
        words.clear();
        notifyDataSetChanged();
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int currentPage);
    }


    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {
        final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int lastPos = layoutManager.findLastVisibleItemPosition();
                    if (!loading && lastPos == getItemCount() - 1 && onLoadMoreListener != null) {
                        int current_page = getItemCount() / itemPerDisplay;
                        onLoadMoreListener.onLoadMore(current_page);
                        loading = true;
                    }
                    IMainActivity mainActivity = (IMainActivity) context;
                    if (dy > 0) {
                        mainActivity.hideBottomNavigation(View.GONE);
                    } else {
                        // Scrolling down
                        mainActivity.hideBottomNavigation(View.VISIBLE);
                    }
                }
            });
        }

    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.words.add(new Word("", ""));
            this.notifyItemInserted(getItemCount() - 1);
            loading = true;
        }
    }

    public void insertData(List<Word> wordList) {
        setLoaded();
        if (!wordList.isEmpty()) {
            int positionStart = getItemCount();
            int itemCount = wordList.size();
            this.words.addAll(wordList);
            notifyItemRangeInserted(positionStart, itemCount);
        }
    }

    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (words.get(i).getTitle().isEmpty() && words.get(i).getDescription().isEmpty()) {
                words.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    /**
     * Highlight searched word
     *
     * @param word
     * @param view
     */
    private void higherLightSearchedWord(Word word, Holder view) {
        if (searchText != null) {
            SpannableStringBuilder sb = new SpannableStringBuilder(word.getDescription());
            Pattern wordPattern = Pattern.compile(Pattern.quote(searchText.toLowerCase()));
            Matcher match = wordPattern.matcher(word.getDescription().toLowerCase());

            while (match.find()) {
                ForegroundColorSpan fcs = new ForegroundColorSpan(
                        ContextCompat.getColor(context, R.color.blue_500)); //specify color here
                sb.setSpan(fcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            view.description.setText(sb);
        } else {
            view.description.setText(word.getDescription());
        }
    }

}
