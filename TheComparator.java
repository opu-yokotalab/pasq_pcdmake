import java.util.*;

public class TheComparator implements Comparator {

    // �\�[�g�Ώۂ̃J�����̈ʒu
    private int index = 0;

    // �\�[�g���邽�߂̃J�����ʒu���Z�b�g
    public void setIndex(int index) {
        this.index = index;
    }

    // String�^�̗v�f��Double�^�Ƃ݂Ȃ��ď����Ƀ\�[�g
    public int compare(Object a, Object b) {
        String[] dblA = (String[]) a;
        String[] dblB = (String[]) b;
        return (int)(Double.parseDouble(dblA[index]) - Double.parseDouble(dblB[index]));
    }
}