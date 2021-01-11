package com.flys.dico.dao.entities;

import com.flys.dico.fragments.adapters.Word;

import java.io.Serializable;

public class WordToShare implements Serializable {
    private boolean status;
    private Word word;
    private int position;

    public WordToShare(boolean status, Word word) {
        this.status = status;
        this.word = word;
    }

    public WordToShare(boolean status, Word word, int position) {
        this.status = status;
        this.word = word;
        this.position = position;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WordToShare that = (WordToShare) o;
        return
                position == that.position;
    }


    @Override
    public String toString() {
        return "WordToShare{" +
                "status=" + status +
                ", word=" + word +
                ", position=" + position +
                '}';
    }
}
