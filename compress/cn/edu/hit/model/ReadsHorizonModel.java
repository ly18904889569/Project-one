package cn.edu.hit.model;

/**
 * 水平编码对象信息
 * 
 * @author rivers May 29, 2017 10:35:34 PM
 *
 */
public class ReadsHorizonModel implements Comparable{
	int alignmentStart; // 起始位置
	int alignmentEnd; // 结束位置
	int mateAlignmentDistance; // 距离mateReads的距离
	int flag; // flag

	public int getAlignmentStart() {
		return alignmentStart;
	}

	public void setAlignmentStart(int alignmentStart) {
		this.alignmentStart = alignmentStart;
	}

	public int getAlignmentEnd() {
		return alignmentEnd;
	}

	public void setAlignmentEnd(int alignmentEnd) {
		this.alignmentEnd = alignmentEnd;
	}

	public int getMateAlignmentDistance() {
		return mateAlignmentDistance;
	}

	public void setMateAlignmentDistance(int mateAlignmentDistance) {
		this.mateAlignmentDistance = mateAlignmentDistance;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	@Override
	public int compareTo(Object o) {
		ReadsHorizonModel m = (ReadsHorizonModel) o;
		return this.alignmentEnd - m.alignmentEnd;
//		return this.alignmentStart - m.alignmentStart;
	}

}
