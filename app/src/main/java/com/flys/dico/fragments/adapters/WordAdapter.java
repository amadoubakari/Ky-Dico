package com.flys.dico.fragments.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.flys.dico.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author AMADOU BAKARI
 * @version 1.0.0
 * @email amadoubakari1992@gmail.com
 * @goal adapte a component word to view
 * @since 24/07/2020
 */
public class WordAdapter extends RecyclerView.Adapter<WordAdapter.Holder> {

    private List<Word> words;
    private Context context;
    private WordOnclickListener onclickListener;

    public WordAdapter(Context context) {
        this.context = context;
    }

    public WordAdapter(List<Word> words, Context context) {
        this.words = words;
        this.context = context;
    }

    public WordAdapter(Context context, List<Word> words, WordOnclickListener onclickListener) {
        this.words = words;
        this.context = context;
        this.onclickListener = onclickListener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_word_item, parent, false);
        return new Holder(view, this.onclickListener);
    }


    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Word word = words.get(position);
        holder.title.setText(word.getTitle());
        holder.description.setText(word.getDescription());

    }

    @Override
    public int getItemCount() {
        return words!=null?  words.size():0;
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

    public void reload(){
        notifyDataSetChanged();
    }

    public void refreshAdapter() {
        notifyDataSetChanged();
    }

    public void addWords(List<Word> words1) {
        this.words.addAll(words1) ;
        notifyDataSetChanged();
    }

    public void removeAllWords() {
        words.clear();
        notifyDataSetChanged();
    }
}
