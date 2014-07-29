import java.util.*;

public class TheComparator implements Comparator {

    // ソート対象のカラムの位置
    private int index = 0;

    // ソートするためのカラム位置をセット
    public void setIndex(int index) {
        this.index = index;
    }

    // String型の要素をDouble型とみなして昇順にソート
    public int compare(Object a, Object b) {
        String[] dblA = (String[]) a;
        String[] dblB = (String[]) b;
        return (int)(Double.parseDouble(dblA[index]) - Double.parseDouble(dblB[index]));
    }
}