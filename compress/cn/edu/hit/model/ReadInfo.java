package cn.edu.hit.model;

/**
 * 最原始的Reads信息，读取自BAM文件
 * @author rivers
 * May 29, 2017 4:27:25 PM
 *
 */
public class ReadInfo {

	int alignmentStart;			//起始位置
	int alignmentEnd;			//结束位置
	String cigarString;			//cigar
	String readString;			//reads
	int mateAlignmentStart;		//mate reads 起始位置
	int flag;					//flag
	int readLength;				//reads的长度
	String quality;				//质量数

	
	

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public int getReadLength() {
		return readLength;
	}

	public void setReadLength(int readLength) {
		this.readLength = readLength;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public int getMateAlignmentStart() {
		return mateAlignmentStart;
	}

	public void setMateAlignmentStart(int mateAlignmentStart) {
		this.mateAlignmentStart = mateAlignmentStart;
	}

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

	public String getCigarString() {
		return cigarString;
	}

	public void setCigarString(String cigarString) {
		this.cigarString = cigarString;
	}

	public String getReadString() {
		return readString;
	}

	public void setReadString(String readString) {
		this.readString = readString;
	}

}
