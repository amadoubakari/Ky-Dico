package com.flys.dico.fragments.state;

import com.flys.dico.architecture.custom.CoreState;
import com.flys.dico.fragments.adapters.Word;

import java.util.ArrayList;
import java.util.List;

public class HomeFragmentState extends CoreState {

    private List<Word> words = new ArrayList<>();

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }
}
