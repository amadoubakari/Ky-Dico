package com.flys.dico.dao.entities;

import com.flys.dico.fragments.adapters.Word;

import java.io.Serializable;
import java.util.List;

/**
 * @author AMADOU BAKARI
 * @version 1.0.0
 * @goal encapsulate dictionnary elements
 * @since 25/07/2020
 */
public class Dictionnaire implements Serializable {

    private List<Word> words;

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }
}
