package com.flys.dico.fragments.adapters;


/**
 * @author AMADOU BAKARI
 * @version 1.0.0
 * @goal encapsulate a dictonnary word
 * @email amadoubakari1992@gmail.com
 * @since 24/07/2020
 */
public class Word  {
    //Word to define
    private String title;
    //Description of the word
    private String description;

    public Word() {
    }

    public Word(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Word word = (Word) o;
        return title.equals(word.title) &&
                description.equals(word.description);
    }

    @Override
    public String toString() {
        return "Word{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
