package cn.zhaiyifan.lyric;

import android.text.TextUtils;
import android.util.Log;

import cn.zhaiyifan.lyric.model.Lyric;
import cn.zhaiyifan.lyric.model.Lyric.Sentence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LyricUtils {
    private static final String TAG = LyricUtils.class.getSimpleName();

    public static Lyric parseLyric(File file, String Encoding) {
        Lyric lyric = new Lyric();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Encoding));
            Log.i(TAG, String.format("parseLyric(%s, %s)", file.getPath(), Encoding));
            String line;
            while ((line = br.readLine()) != null) {
                parseLine(line, lyric);
            }
            Collections.sort(lyric.sentenceList, new Lyric.SentenceComparator());
        } catch (IOException e) {
            Log.e("ERROR", String.valueOf(e));
        }
        if (TextUtils.isEmpty(lyric.title) || TextUtils.isEmpty(lyric.artist)) {
            String title;
            String artist = null;
            String filename = file.getName();
            filename = filename.substring(0, filename.length() - 4);
            int index = filename.indexOf('-');
            if (index > 0) {
                artist = filename.substring(0, index).trim();
                title = filename.substring(index + 1).trim();
            } else {
                title = filename.trim();
            }
            if (TextUtils.isEmpty(lyric.title) && !TextUtils.isEmpty(title)) {
                lyric.title = title;
            } else if (!TextUtils.isEmpty(artist)) {
                lyric.artist = artist;
            }
        }
        return lyric;
    }

    /**
     * Get sentence according to timestamp.
     */
    public static Sentence getSentence(Lyric lyric, long ts) {
        return getSentence(lyric, ts, 0);
    }

    /**
     * Get sentence according to timestamp and current index.
     */
    public static Sentence getSentence(Lyric lyric, long ts, int index) {
        return getSentence(lyric, ts, index, 0);
    }

    /**
     * Get sentence according to timestamp, current index, offset.
     */
    public static Sentence getSentence(Lyric lyric, long ts, int index, int offset) {
        int found = getSentenceIndex(lyric, ts, index, offset);
        if (found == -1)
            return null;
        return lyric.sentenceList.get(found);
    }

    /**
     * Get current index of sentence list.
     *
     * @param lyric  Lyric file.
     * @param ts     Current timestamp.
     * @param index  Current index.
     * @param offset Lyric offset.
     * @return current sentence index, -1 if before first, -2 if not found.
     */
    public static int getSentenceIndex(Lyric lyric, long ts, int index, int offset) {
        if (lyric == null || ts < 0 || index < -1)
            return -1;
        List<Sentence> list = lyric.sentenceList;

        if (index >= list.size())
            index = list.size() - 1;
        if (index == -1)
            index = 0;

        int found = -2;

        if (list.get(index).fromTime + offset > ts) {
            for (int i = index; i > -1; --i) {
                if (list.get(i).fromTime + offset <= ts) {
                    found = i;
                    break;
                }
            }
            // First line of lyric is bigger than starting time.
            if (found == -2)
                found = -1;
        } else {
            for (int i = index; i < list.size() - 1; ++i) {
                //Log.d(TAG, String.format("ts: %d, offset: %d, curr_ts: %d, next_ts: %d", ts, offset, list.get(i).getFromTime(), list.get(i + 1).getFromTime()));
                if (list.get(i + 1).fromTime + offset > ts) {
                    found = i;
                    break;
                }
            }
            // If not found, return last mLyricIndex
            if (found == -2) {
                found = list.size() - 1;
            }
        }

        return found;
    }

    private static void parseLine(String line, Lyric lyric) {
        int lineLength = line.length();
        line = line.trim();
        int openBracketIndex, closedBracketIndex;
        openBracketIndex = line.indexOf('[');

        while (openBracketIndex != -1) {
            closedBracketIndex = line.indexOf(']', openBracketIndex);
            // (1) ']' does not exist, (2) is the first character
            if (closedBracketIndex < 1)
                return;
            String closedTag = line.substring(openBracketIndex + 1, closedBracketIndex);
            String[] colonSplit = closedTag.split(":", 2);
            if (colonSplit.length < 2)
                return;

            if (colonSplit[0].equalsIgnoreCase(Constants.ID_TAG_TITLE)) {
                lyric.title = colonSplit[1].trim();
            } else if (colonSplit[0].equalsIgnoreCase(Constants.ID_TAG_ARTIST)) {
                lyric.artist = colonSplit[1].trim();
            } else if (colonSplit[0].equalsIgnoreCase(Constants.ID_TAG_ALBUM)) {
                lyric.album = colonSplit[1].trim();
            } else if (colonSplit[0].equalsIgnoreCase(Constants.ID_TAG_CREATOR_LRCFILE)) {
                lyric.by = colonSplit[1].trim();
            } else if (colonSplit[0].equalsIgnoreCase(Constants.ID_TAG_CREATOR_SONGTEXT)) {
                lyric.author = colonSplit[1].trim();
            } else if (colonSplit[0].equalsIgnoreCase(Constants.ID_TAG_LENGTH)) {
                lyric.length = parseTime(colonSplit[1].trim(), lyric);
            } else if (colonSplit[0].equalsIgnoreCase(Constants.ID_TAG_OFFSET)) {
                lyric.offset = parseOffset(colonSplit[1].trim());
            } else {
                if (Character.isDigit(colonSplit[0].charAt(0))) {
                    List<Long> timestampList = new LinkedList<>();
                    long time = parseTime(closedTag, lyric);
                    if (time != -1) {
                        timestampList.add(time);
                    }
                    //Log.d(TAG, line);
                    // We may have line like [01:38.33][01:44.01][03:22.05]Test Test
                    // [03:55.00]
                    while ((lineLength > closedBracketIndex + 2)
                            && (line.charAt(closedBracketIndex + 1) == '[')) {
                        //Log.d(TAG, String.valueOf(closedBracketIndex));
                        int nextOpenBracketIndex = closedBracketIndex + 1;
                        int nextClosedBracketIndex = line.indexOf(']', nextOpenBracketIndex + 1);
                        time = parseTime(line.substring(nextOpenBracketIndex + 1, nextClosedBracketIndex), lyric);
                        if (time != -1) {
                            timestampList.add(time);
                        }
                        closedBracketIndex = nextClosedBracketIndex;
                    }

                    String content = line.substring(closedBracketIndex + 1);
                    for (long timestamp : timestampList) {
                        content = content.replace("&apos;", "'");
                        lyric.addSentence(content, timestamp);
                    }
                } else {
                    // Ignore unknown tag
                    return;
                }
            }
            // We may have line like [00:53.60]On a dark [00:54.85]desert highway
            openBracketIndex = line.indexOf('[', closedBracketIndex + 1);
        }
    }

    /**
     * 把如00:00.00这样的字符串转化成 毫秒数的时间，比如 01:10.34就是一分钟加上10秒再加上340毫秒 也就是返回70340毫秒
     *
     * @param time 字符串的时间
     * @return 此时间表示的毫秒
     */
    private static long parseTime(String time, Lyric lyric) {
        String[] ss = time.split("[:.]");
        // 如果 是两位以后，就非法了
        if (ss.length < 2) {
            return -1;
        } else if (ss.length == 2) {// 如果正好两位，就算分秒
            try {
                // 先看有没有一个是记录了整体偏移量的
                if (lyric.offset == 0 && ss[0].equalsIgnoreCase("offset")) {
                    lyric.offset = Integer.parseInt(ss[1]);
                    System.err.println("整体的偏移量：" + lyric.offset);
                    return -1;
                }
                int min = Integer.parseInt(ss[0]);
                int sec = Integer.parseInt(ss[1]);
                if (min < 0 || sec < 0 || sec >= 60) {
                    throw new RuntimeException("数字不合法!");
                }
                // System.out.println("time" + (min * 60 + sec) * 1000L);
                return (min * 60L + sec) * 1000L;
            } catch (Exception exe) {
                return -1;
            }
        } else if (ss.length == 3) {// 如果正好三位，就算分秒，毫秒
            try {
                int min = Integer.parseInt(ss[0]);
                int sec = Integer.parseInt(ss[1]);
                int mm = Integer.parseInt(ss[2]);
                if (min < 0 || sec < 0 || sec >= 60 || mm < 0 || mm > 999) {
                    throw new RuntimeException("数字不合法!");
                }
                // System.out.println("time" + (min * 60 + sec) * 1000L + mm);
                return (min * 60L + sec) * 1000L + mm;
            } catch (Exception exe) {
                return -1;
            }
        } else {// 否则也非法
            return -1;
        }
    }

    /**
     * 分析出整体的偏移量
     *
     * @param str 包含内容的字符串
     * @return 偏移量，当分析不出来，则返回最大的正数
     */
    private static int parseOffset(String str) {
        if (str.equalsIgnoreCase("0"))
            return 0;
        String[] ss = str.split(":");
        if (ss.length == 2) {
            if (ss[0].equalsIgnoreCase("offset")) {
                int os = Integer.parseInt(ss[1]);
                Log.i(TAG, "total offset：" + os);
                return os;
            } else {
                return Integer.MAX_VALUE;
            }
        } else {
            return Integer.MAX_VALUE;
        }
    }
}