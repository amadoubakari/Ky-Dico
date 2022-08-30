package com.flys.dico.fragments.adapters;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.flys.dico.R;
import com.flys.dico.architecture.custom.IMainActivity;
import com.flys.dico.dao.entities.WordToShare;
import com.flys.dico.utils.Utils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
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
    private String searchText;
    private OnSearchActionListener onSearchActionListener;

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

    public WordAdapter(Context context, List<Word> words, WordOnclickListener onclickListener, OnSearchActionListener searchActionListener) {
        this.words = words;
        this.context = context;
        this.onclickListener = onclickListener;
        this.onSearchActionListener = searchActionListener;
    }

    public WordAdapter(Context context, List<Word> words, String searchText, WordOnclickListener onclickListener, OnSearchActionListener searchActionListener) {
        this.words = words;
        this.context = context;
        this.searchText = searchText;
        this.onclickListener = onclickListener;
        this.onSearchActionListener = searchActionListener;
    }

    public WordAdapter(Context context, List<Word> words, int itemPerDisplay, WordOnclickListener onclickListener, OnSearchActionListener searchActionListener) {
        this.itemPerDisplay = itemPerDisplay;
        this.words = words;
        this.context = context;
        this.onclickListener = onclickListener;
        this.onSearchActionListener = searchActionListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_ITEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.view_word_item, parent, false);
            return new Holder(view, this.onclickListener);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_progress, parent, false);
            return new HolderProgress(view);
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

    class Holder extends RecyclerView.ViewHolder implements View.OnLongClickListener, View.OnClickListener {
        TextView title;
        TextView description;
        MaterialCardView word;
        //Simple view to attach popup menu
        View attachOfPopMenu;
        WordOnclickListener wordOnclickListener;

        public Holder(@NonNull View itemView, WordOnclickListener onclickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            word = itemView.findViewById(R.id.word_card_id);
            attachOfPopMenu = itemView.findViewById(R.id.view_to_attach_popup_menu_id);
            wordOnclickListener = onclickListener;
            word.setOnLongClickListener(this);
            word.setOnClickListener(this);

        }

        /**
         * Called when a view has been clicked and held.
         *
         * @param v The view that was clicked and held.
         * @return true if the callback consumed the long click, false otherwise.
         */
        @Override
        public boolean onLongClick(View v) {
            word.setChecked(!word.isChecked());
            return wordOnclickListener.onWordLongClickListener(new WordToShare(word.isChecked(), words.get(getAdapterPosition()), getAdapterPosition()));
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            wordOnclickListener.onWordClickListener(attachOfPopMenu, getAdapterPosition(), words.get(getAdapterPosition()));
        }
    }


    class HolderProgress extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public HolderProgress(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }

    public interface WordOnclickListener {

        void onWordClickListener(View v, int position, Word word);

        boolean onWordLongClickListener(WordToShare wordToShare);
    }


    public interface OnSearchActionListener {
        void search(String wordToSearch);
    }


    public void reload() {
        notifyDataSetChanged();
    }

    public void addWords(List<Word> words1) {
        this.words.addAll(words1);
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
                        mainActivity.scrollUp();
                    } else {
                        // Scrolling down
                        mainActivity.scrollDown();
                    }
                }
            });
        }

    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.words.add(new Word("", ""));
            notifyItemInserted(getItemCount() - 1);
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
        Spannable spannable = new SpannableStringBuilder(word.getDescription());
        if (searchText != null) {
            Pattern wordPattern = Pattern.compile(Pattern.quote(searchText.toLowerCase()));
            Matcher match = wordPattern.matcher(word.getDescription().toLowerCase());

            while (match.find()) {
                ForegroundColorSpan fcs = new ForegroundColorSpan(
                        ContextCompat.getColor(context, R.color.color_secondary)); //specify color here
                spannable.setSpan(fcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }

        Arrays.asList("number", "location", "storage").stream().parallel().forEach(s -> Linkify.addLinks(spannable, Pattern.compile(s), ""));
        Utils.stripUnderlines(spannable, onSearchActionListener);

        view.description.setMovementMethod(LinkMovementMethod.getInstance());

        view.description.setText(spannable, TextView.BufferType.SPANNABLE);
    }
}
