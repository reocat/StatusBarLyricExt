package cn.zhaiyifan.lyric.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Lyric {
    static {
        Lyric.class.getSimpleName();
    }

    public String title;
    public String artist;
    public String album;
    public String by;
    public String author;
    public int offset;
    public long length;
    public List<Sentence> sentenceList = new ArrayList<>(100);

    @NonNull
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Title: " + title + "\n")
                .append("Artist: " + artist + "\n")
                .append("Album: " + album + "\n")
                .append("By: " + by + "\n")
                .append("Author: " + author + "\n")
                .append("Length: " + length + "\n")
                .append("Offset: " + offset + "\n");
        if (sentenceList != null) {
            for (Sentence sentence : sentenceList) {
                stringBuilder.append(sentence.toString() + "\n");
            }
        }
        return stringBuilder.toString();
    }

    public void addSentence(String content, long time) {
        sentenceList.add(new Sentence(content, time));
    }

    public static class SentenceComparator implements Comparator<Sentence> {
        @Override
        public int compare(Sentence sent1, Sentence sent2) {
            return (int) (sent1.fromTime - sent2.fromTime);
        }
    }

    public static class Sentence {
        public String content;
        public long fromTime;

        public Sentence(String content, long fromTime) {
            this.content = content;
            this.fromTime = fromTime;
        }

        @NonNull
        public String toString() {
            return fromTime + ": " + content;
        }
    }
}