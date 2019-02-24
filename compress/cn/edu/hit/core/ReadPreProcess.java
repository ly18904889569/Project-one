package cn.edu.hit.core;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import cn.edu.hit.model.QualitySymbol;
import cn.edu.hit.model.READSYMBOL;
import cn.edu.hit.model.ReadInfo;
import cn.edu.hit.model.ReadMatchResult;
import cn.edu.hit.model.ReadsHorizonModel;
import cn.edu.hit.model.ReadsPreProcessResult;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.ValidationStringency;

/**
 * 读取BAM文件，并将数据处理成ReadPreProcessModel集合形式
 * @author rivers
 * May 29, 2017 4:15:48 PM
 */

public class ReadPreProcess {
	
	public static void main(String[] args) {
		ReadPreProcess readPreProcess = new ReadPreProcess();
		String filePath = "/home/rivers/riversdoc/test.sorted.bam";
//		String filePath = "/home/rivers/riversdoc/compress/chr21.fa.fasta.sam.copy2";
		List<List<ReadInfo>> readInfos = readPreProcess.splitBamFile(filePath);
		//就在这里，把结果输出去看一下
		try{
			//这部分内容输出的很OK，没有任何的问题
			PrintWriter writer = new PrintWriter("/home/rivers/riversdoc/compress/readResult.txt", "UTF-8");
			System.out.println("list.size:\t" + readInfos.get(0).size());
			for(ReadInfo model : readInfos.get(0)){
				writer.println("reads:\t" + model.getReadString());
				writer.println("cigar:\t"+ model.getCigarString());
				writer.println("startAlignmemt:\t" + model.getAlignmentStart());
				writer.println("endAlignment:\t" + model.getAlignmentEnd());
				writer.println("length:\t" + model.getReadLength());
				writer.println("quality:\t"+ model.getQuality());
				writer.println();
			}
			writer.close();
			System.out.println("write OK");
		}catch(Exception e) {}
		
		//测试一下喂给VerticalEncoding的数据
		ReadsPreProcessResult processResult = readPreProcess.readsProc(readInfos.get(0));
		try {
			//这部分是有问题的，问题主要在于最后的END，在SS的情形下，处理是不正确的
			//上面注释的这个问题应该解决了，后续再验证一遍，同时去掉无意义的注释
			PrintWriter writer = new PrintWriter("/home/rivers/riversdoc/compress/readProcessResult.txt", "UTF-8");
			for (int i =0; i < processResult.getReadsInfo().size(); i++){
				writer.println("reads After process:\t" + processResult.getReadsInfo().get(i).getReads());
				writer.println("startAlignmemt:\t" + processResult.getReadsInfo().get(i).getAlignmentStart());
				writer.println("endAlignment:\t" + processResult.getReadsInfo().get(i).getAlignmentEnd());
				writer.println("ExceptionInfo:\t" + processResult.getReadsInfo().get(i).getExceptionInfo());
				writer.println("readsQuality:\t" + processResult.getReadsInfo().get(i).getReadQuality());
				writer.println("exceptionQuality:\t" + processResult.getReadsInfo().get(i).getExceptionQuality());
				writer.println();
			}
			writer.close();
		} catch (Exception e1){}
		
		
	}


	/**
	 * 将BAM文件分区域进行切割
	 * @param filePath
	 * @return List<List<ReadInfoModel>>
	 */
	public List<List<ReadInfo>> splitBamFile(String filePath) {
		
		File sortedBamFile = new File(filePath);
		SAMFileReader inputSam = new SAMFileReader(sortedBamFile);
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
		
		SAMRecordIterator samit = inputSam.iterator();
		SAMRecord rec = null;
		List<List<ReadInfo>> readInfos = new ArrayList<List<ReadInfo>>();
		int count = 0;
		int kMod = 200000000;	//每1000条reads一截断
		List<ReadInfo> readInfoList = new ArrayList<ReadInfo>();
		
		
		while (samit.hasNext()) {
			rec = samit.next();
//			count++;
//			ReadInfo readInfo = new ReadInfo();
//			readInfo.setAlignmentStart(rec.getAlignmentStart());
//			readInfo.setMateAlignmentStart(rec.getMateAlignmentStart());
//			readInfo.setAlignmentEnd(rec.getAlignmentEnd());
//			readInfo.setCigarString(rec.getCigarString());
//			readInfo.setReadString(rec.getReadString());
//			readInfo.setFlag(rec.getFlags());
//			readInfo.setReadLength(rec.getReadLength());
//			readInfo.setQuality(rec.getBaseQualityString());
//			readInfoList.add(readInfo);
			
			if (++count % kMod == 0) {
				readInfos.add(readInfoList);
				readInfoList = new ArrayList<ReadInfo>();
				
				// TODO 目前为了方便测试，读取1000条就进行这个区域块的处理
				break;
			}
		}
		//最后一次的结果单独处理一下
		readInfos.add(readInfoList);
		System.out.println("count:\t"+count);
		return readInfos;
	}
	
	/**
	 * 读取BAM文件，并处理成ReadPreProcessModel的集合形式
	 * @param readInfo
	 * @return ReadsPreProcessResult
	 */
	public ReadsPreProcessResult readsProc(List<ReadInfo> readInfo) {
		ReadsPreProcessResult result =  new ReadsPreProcessResult();
		//对每一条reads序列单独处理一下
		List<ReadMatchResult> readsInfo = new ArrayList<ReadMatchResult>();
		List<ReadsHorizonModel> readsHorizon = new ArrayList<ReadsHorizonModel>();
		for (ReadInfo read : readInfo) {
			ReadMatchResult readsPreProcessModel = new ReadMatchResult();
			ReadsHorizonModel horizonModel = new ReadsHorizonModel();
			//根据cigar和readString进行编码处理

			//TODO: 把没有质量数的情况剔除掉
			if(read.getQuality().length() == 1)
				continue;
			
			ReadMatchResult readMatchResult = readMatch(read.getCigarString(), read.getReadString(), read.getQuality());
			
			readsPreProcessModel.setAlignmentEnd(read.getAlignmentEnd());
			horizonModel.setAlignmentEnd(read.getAlignmentEnd());
			horizonModel.setMateAlignmentDistance(read.getMateAlignmentStart() - read.getAlignmentEnd());
			//对插入Insert和*的情形处理,reCheck==true时, 主要目的是用来缩短长度的，真正与endAlignment对应上
			if (readMatchResult.isReCheck() == true) {
				//连有效长度都要一起修改，这就涉及到横向编码的部分了
				// 对reads和质量数都需要做进一步的处理
				List<READSYMBOL> newSymbol = new ArrayList<READSYMBOL>();
				StringBuilder readQuality = new StringBuilder();
				for (int i = 0; i < readMatchResult.getReads().size(); i++) {
					READSYMBOL symbol =  readMatchResult.getReads().get(i);
					Character qualChr = readMatchResult.getReadQuality().charAt(i);
					// * 的情形
					if (symbol.equals(READSYMBOL.STAR)) {
						newSymbol.add(READSYMBOL.STAR);
						readQuality.append(QualitySymbol.x);
						// 对于插入的情形，end是否有变化，中间的位点是否直接略过去了
						readsPreProcessModel.setAlignmentEnd(read.getAlignmentStart() + readMatchResult.getReads().size() - 1);
						horizonModel.setAlignmentEnd(read.getAlignmentStart() + readMatchResult.getReads().size() - 1);
						horizonModel.setMateAlignmentDistance(read.getMateAlignmentStart() - readsPreProcessModel.getAlignmentEnd());
						break;
					}
					
					// Insert的情形
					// 好像这里有点这个意思了，直接往里面塞数据就可以了
					if (symbol.equals(READSYMBOL.S) || symbol.equals(READSYMBOL.H) 
							|| symbol.equals(READSYMBOL.Y) ||symbol.equals(READSYMBOL.V)) {
						newSymbol.add(READSYMBOL.I);
						readQuality.append(QualitySymbol.x.name());
						i++;
						continue;
					}
					newSymbol.add(symbol);
					readQuality.append(qualChr);
					
				}// END for
				newSymbol.add(READSYMBOL.END);
				readsPreProcessModel.setReads(newSymbol);
				readQuality.append(QualitySymbol.z.name());
				readsPreProcessModel.setReadQuality(readQuality.toString());
				
			} else {
				readMatchResult.getReads().add(READSYMBOL.END);
				readsPreProcessModel.setReads(readMatchResult.getReads());
				readsPreProcessModel.setReadQuality(readMatchResult.getReadQuality() + QualitySymbol.z.name());
			}
			
			if (readMatchResult.getExceptionInfo().size() != 0) {
				readsPreProcessModel.setExceptionInfo(readMatchResult.getExceptionInfo());
				readsPreProcessModel.setExceptionQuality(readMatchResult.getExceptionQuality());
			}
			
			
			
			readsPreProcessModel.setAlignmentStart(read.getAlignmentStart());
			readsPreProcessModel.setExceptionQuality(readMatchResult.getExceptionQuality());
			readsInfo.add(readsPreProcessModel);
			
			horizonModel.setAlignmentStart(read.getAlignmentStart());
			horizonModel.setFlag(read.getFlag());
			
			readsHorizon.add(horizonModel);
		}
		
		result.setReadsInfo(readsInfo);
		result.setReadsHorizon(readsHorizon);
		return result;
	}
	
	
	
	/**
	 * 返回匹配后的位点信息
	 * @param cigar
	 * @param readString
	 * @return 
	 */
	ReadMatchResult readMatch(String cigar, String readString, String readsQualityString){
		
		ReadMatchResult readMatchResult = new ReadMatchResult();
		List<READSYMBOL> readList = new ArrayList<READSYMBOL>();
		List<String> exceptionInfo = new ArrayList<String>();
		StringBuilder readQuality = new StringBuilder();
		List<String> exceptionQuality = new ArrayList<String>();
		boolean reCheck = false;
		
		//对cigar== * 的情况单独处理一下
		if ("*".equals(cigar)) {
			readList.add(READSYMBOL.STAR);
			reCheck = true;
			exceptionInfo.add(readString.substring(0,readString.length()));
			readMatchResult.setReads(readList);
			readMatchResult.setExceptionInfo(exceptionInfo);
			readMatchResult.setReCheck(reCheck);
			readMatchResult.setReadQuality(readQuality.toString()+"z");
			exceptionQuality.add(readsQualityString);
			readMatchResult.setExceptionQuality(exceptionQuality);
			return readMatchResult;
		}
		
		String[] cigars =cigar.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
		
		
		int num;
		String alphabet;
		int index = 0;
		int indexTemp = 0;
		int end = 0;
		for(int i = 0; i< cigars.length; i++){
			if(i%2 == 0)
				continue;
			num = Integer.parseInt(cigars[i-1]);
			alphabet = cigars[i];
			//判断一下字母的类型
			switch(alphabet){
				case "m":
				case "M":
					if(i-2 >=0 && cigars[i-2].equals("S")){
						num --;
					}
					end = index+num;
					for(;index<end;index++){
						readList.add(READSYMBOL.E);
						readQuality.append(readsQualityString.charAt(index));
					}
					break;
				case "i":
				case "I":
					reCheck = true;
					// 这部分重新改写一下，20171223
					// 针对I在最后的情形，单独处理一下
					String value = null;
					if((index + num) == readString.length()){	//I 在结尾的情形
						readList.remove(readList.size() - 1);
						value = readString.substring(index - 1, index);
						exceptionInfo.add(readString.substring(index - 1, index+num));
						readQuality.append(QualitySymbol.x);
						exceptionQuality.add(readsQualityString.substring(index - 1, index+num));
					}else{
						value = readString.substring(index+num, index+num+1);
						exceptionInfo.add(readString.substring(index, index+num+1));
						readQuality.append(QualitySymbol.x);
						exceptionQuality.add(readsQualityString.substring(index, index+num+1));
					}
					
					if("A".equalsIgnoreCase(value)){
						readList.add(READSYMBOL.S);
					}else if("C".equalsIgnoreCase(value)){
						readList.add(READSYMBOL.H);
					}else if("G".equalsIgnoreCase(value)){
						readList.add(READSYMBOL.Y);
					}else if("T".equalsIgnoreCase(value)){
						readList.add(READSYMBOL.V);
					}
					
					index += num;
					break;
				case "d":
				case "D":
					indexTemp = index;
					end = index+num;
					for(;indexTemp < end;indexTemp++){
						readList.add(READSYMBOL.D);
						exceptionInfo.add(READSYMBOL.D.name());
						readQuality.append(QualitySymbol.y.name());
						exceptionQuality.add(QualitySymbol.y.name());
					}
					break;
				case "n":
				case "N":
					end = index+num;
					for(;index < end;index++){
						//先把N也当作和D一样的处理吧0507
//						readList.add(READSYMBOL.N);
						readList.add(READSYMBOL.D);
						exceptionInfo.add(READSYMBOL.D.name());
						readQuality.append(QualitySymbol.y.name());
						exceptionQuality.add(QualitySymbol.y.name());
					}
					System.out.println("skip..."+num);
					break;
				case "s":
				case "S":
					//针对S的情形，有这样一个处理策略：如果S位于起始，则向后多取一个，否则则向前多取一个
					if (0 == index) {
						exceptionInfo.add(readString.substring(index,index + num + 1));
						exceptionQuality.add(readsQualityString.substring(index,index + num + 1));
						index = index + num + 1;
					} else {
						readList.remove(readList.size() - 1);
						exceptionInfo.add(readString.substring(index - 1,index + num));
						//这里对质量数的处理特别化一下，用一个字符去占位处理,而且特意是字母z
						readQuality.deleteCharAt(readQuality.length() - 1);
						exceptionQuality.add(readsQualityString.substring(index - 1,index + num));
						index += num;
					}
					//这里对质量数的处理特别化一下，用一个字符去占位处理,而且特意是字母z
					readQuality.append(QualitySymbol.x);
					readList.add(READSYMBOL.SS);
//					reCheck = true;
					break;
				//对于H的情形，根本不用去理会。6H5M，实际上就是只有5M的有效数据
				case "h":
				case "H":
					break;
				// P也是可以直接或略掉的,原本这里是增加了元素的
				case "p":
				case "P":
					indexTemp = index;
					end = index + num;
					for(;indexTemp < end; indexTemp++){
//						readList.add(READSYMBOL.P);
					}
					break;
				case "=":
					end = index + num;
					for(;index < end; index++){
						readList.add(READSYMBOL.E);
					}
					break;
				case "x":
				case "X":
					// x 作为0I的特殊情形来进行处理
					end = index + num;
					for(;index < end;index++){
						value = readString.substring(index,index+1);
						readList.add(READSYMBOL.X);
						readQuality.append(QualitySymbol.x);
						exceptionQuality.add(readsQualityString.charAt(index)+"");
					}
					break;
				default:
					System.out.println("Exception occurs alphabet match.");
			}
		}
		
		readMatchResult.setReads(readList);
		readMatchResult.setExceptionInfo(exceptionInfo);
		readMatchResult.setReCheck(reCheck);
		readMatchResult.setReadQuality(readQuality.toString());
		readMatchResult.setExceptionQuality(exceptionQuality);
		return readMatchResult;
	}

	
	
	
	
	
}
