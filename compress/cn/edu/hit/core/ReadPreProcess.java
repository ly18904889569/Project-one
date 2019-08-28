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
		String filePath = "/home/yangli/Documents/compress/50X/NC_50X.fastq.sorted.bam";//"/home/rivers/riversdoc/test.sorted.bam";
//		String filePath = "/home/rivers/riversdoc/compress/chr21.fa.fasta.sam.copy2";
//		String filePath = "/home/liyang/Document/compress/input/50X/NC_50X.fastq.sorted.bam";
		List<List<ReadInfo>> readInfos = readPreProcess.splitBamFile(filePath);
		//就在这里，把结果输出去看一下
		try{
			//这部分内容输出的很OK，没有任何的问题
			PrintWriter writer = new PrintWriter("/home/yangli/Documents/compress/documents/50X-readResult2.txt", "UTF-8");	///home/rivers/riversdoc/compress/readResult.txt
//			PrintWriter writer = new PrintWriter("/home/liyang/Document/compress/output/50X/50X-all/50X-readResult2.txt", "UTF-8");
			System.out.println("The first list.size:\t" + readInfos.get(0).size());	//liyang：取第一份kmod
			System.out.println("The second list.size:\t" + readInfos.get(1).size());	//liyang:这个是自己加的
			System.out.println("The last list.size:\t" + readInfos.get(readInfos.size()-1).size());	//liyang:这个是自己加的
			for(ReadInfo model : readInfos.get(0)){		//liyang:二代之后取出其中的6条属性进行处理
				writer.println("reads:\t" + model.getReadString());
				//System.out.println(model.getReadString());
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
			PrintWriter writer = new PrintWriter("/home/yangli/Documents/compress/documents/50X-readProcessResult2.txt", "UTF-8");	///home/rivers/riversdoc/compress/readProcessResult.txt
//			PrintWriter writer = new PrintWriter("/home/liyang/Document/compress/output/50X/50X-all/50X-readProcessResult2.txt", "UTF-8");
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
	 * 将BAM文件分区域进行切割	liyang：确实只是对BAM文件进行了区域分割
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
		int kMod = 31776;	//每1000条reads一截断		这里为什么是20000000，不是说好的1000条为一截段		//liyang：这里原先是200000000，但是如果是1000条算作是一个的话这个数量有点大，我打算改为1000
		List<ReadInfo> readInfoList = new ArrayList<ReadInfo>();
		
		
		while (samit.hasNext()) {	//liyang：这一段的作用就是把SAM文件的信息截取出来
		rec = samit.next();
			
		//下面这一段本来是注释掉的，我又给加上了，因为如果不加上，是不会有数据的。
		//count++;				//liyang:这里要是不注释掉的话，count会在这里和接下来连续进行两次++
			ReadInfo readInfo = new ReadInfo();		//liyang：这里注释掉的话，就不会将Sam文件格式中的信息分割
			readInfo.setAlignmentStart(rec.getAlignmentStart());
			readInfo.setMateAlignmentStart(rec.getMateAlignmentStart());
			readInfo.setAlignmentEnd(rec.getAlignmentEnd());
			readInfo.setCigarString(rec.getCigarString());
			readInfo.setReadString(rec.getReadString());
			readInfo.setFlag(rec.getFlags());
			readInfo.setReadLength(rec.getReadLength());
			readInfo.setQuality(rec.getBaseQualityString());
			readInfoList.add(readInfo);		//readInfoList存放的就是多个readInfo也就是说
			
			if (++count % kMod == 0) {
				readInfos.add(readInfoList);	//liyang：可以理解成为一列用来存储SAM的基本信息，然后成为一条记录，而这种记录有多条
				readInfoList = new ArrayList<ReadInfo>();
				//System.out.println("count:\t"+count);	//liyang:这是我加的一段测试语句，从这里可以看出确实是分了好几个区域
				// TODO 目前为了方便测试，读取1000条就进行这个区域块的处理
//				break;	//liyang:这里有个跳出循环，上面的if语句就是说明了（在break不被注释掉的情况下）跳出了循环，这个while其实自己能够走出循环
				//liyang:对于之后处理，需要在这里需要设计成多线程？
			}
		}
		//最后一次的结果单独处理一下
		readInfos.add(readInfoList);
		System.out.println("precount:"+readInfos.get(0).size());
		System.out.println("count:\t"+count);
		return readInfos;
	}
	
	/**
	 * 读取BAM文件，并处理成ReadPreProcessModel的集合形式
	 * @param readInfo
	 * @return ReadsPreProcessResult//包含供垂直编码和水平编码使用的信息
	 */
		//liyang:预处理一共有两个比较主要的函数，一个是splitBAM函数主要的作用就是分割文件，另一个比较重要的函数就是readPro函数，其主要的作用就是生成水平编码和垂直编码信息
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
			//liyang:这里就会导致最后将要处理的read数量并不是count值
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
						newSymbol.add(READSYMBOL.I);			//liyang:如果有插入的话，就在转化之后的后面加上I
						readQuality.append(QualitySymbol.x.name());		//liyang:并且在最后的质量分数加上x
						i++;
						continue;
					}
					newSymbol.add(symbol);
					readQuality.append(qualChr);
					
				}// END for
				newSymbol.add(READSYMBOL.END);		//liyang:在结束加END
				readsPreProcessModel.setReads(newSymbol);
				readQuality.append(QualitySymbol.z.name());		//liyang:在结束的质量分数上加z
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
			readsPreProcessModel.setExceptionQuality(readMatchResult.getExceptionQuality());	//liyang:感觉这句话并没有什么用处
			readsInfo.add(readsPreProcessModel);
			
			horizonModel.setAlignmentStart(read.getAlignmentStart());
			horizonModel.setFlag(read.getFlag());
			
			readsHorizon.add(horizonModel);
		}
		
		result.setReadsInfo(readsInfo);
		result.setReadsHorizon(readsHorizon);
		return result;
	}
	
	
	//liyang:readMatch就是一个根据cigar，readstring对整个readString进行字母转换，把异常信息和正常信息进行分离，质量分数顺便页进行记录。
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
		
		//对cigar== * 的情况单独处理一下		liyang：起作用就是把各种信息进行存储
		if ("*".equals(cigar)) {
			readList.add(READSYMBOL.STAR);
			reCheck = true;
			exceptionInfo.add(readString.substring(0,readString.length()));
			readMatchResult.setReads(readList);		//liyang:readlist仅仅只有一个star，也就是说长度就是1
			readMatchResult.setExceptionInfo(exceptionInfo);	//liyang:异常值将所有的*表示的字符串全部加入
			readMatchResult.setReCheck(reCheck);
			readMatchResult.setReadQuality(readQuality.toString()+"z");		//liyang:质量分数为空，因为全部都是异常质量分数，且设置为z
			exceptionQuality.add(readsQualityString);
			readMatchResult.setExceptionQuality(exceptionQuality);
			return readMatchResult;
		}
		
		String[] cigars =cigar.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");//liyang：split的作用是切割成单个字符
																		//正则表达式
		
		int num;		//liyang:这个就是定义5M前面的那个5，就是cigar的数字部分
		String alphabet;	//liyang:这个就是定义的5M的那个M
		int index = 0;		//liyang:index是转换之后的EEEEIIII的那个索引
		int indexTemp = 0;
		int end = 0;
		for(int i = 0; i< cigars.length; i++){		//liyang:这个就是找到基数位置，就是把字母信息检索出来
			if(i%2 == 0)
				continue;
			num = Integer.parseInt(cigars[i-1]);//liyang:parseInt() 函数可解析一个字符串，并返回一个整数。,不加最后一个参数，默认为10
			alphabet = cigars[i];
			//判断一下字母的类型
			switch(alphabet){
				case "m":
				case "M":
					//crazy:这里将>=分成两种情况
					if(i-2>=0 && cigars[i-2].equals("S")){		//liyang:如果说真的进入了这个if语句中，就说明了开始是S，那么这就会导致转换的E少一个
						num --;		//liyang:这里应该默认ss后面必然接M
					}
					end = index+num;
					for(;index<end;index++){
						readList.add(READSYMBOL.E);
						readQuality.append(readsQualityString.charAt(index));//charAt是返回位置所在的字符
					}								//将该位置开始的子串加到前一个字符串
					break;
				case "i":
				case "I":
					reCheck = true;
					// 这部分重新改写一下，20171223
					// 针对I在最后的情形，单独处理一下
					String value = null;
					if((index + num) == readString.length()){	//I 在结尾的情形
						readList.remove(readList.size() - 1);	//liyang:这个处理很有意思，就是把最后一个信息删除，然后再加入一个插入信息
						value = readString.substring(index - 1, index);		//liyang:这个就是取索引位置，前闭后开
						exceptionInfo.add(readString.substring(index - 1, index+num));	//liyang:当插入在最后的时候取前面一个字符
						readQuality.append(QualitySymbol.x);
						exceptionQuality.add(readsQualityString.substring(index - 1, index+num));
					}else{
						value = readString.substring(index+num, index+num+1);	//liyang:这里是将插入点后面的字符加入到插入异常信息中
						exceptionInfo.add(readString.substring(index, index+num+1));
						readQuality.append(QualitySymbol.x);
						exceptionQuality.add(readsQualityString.substring(index, index+num+1));		//liyang:将插入点以及后面一点质量分数加入
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
					indexTemp = index;		//liyang:indexTemp作为删除的索引
					end = index+num;
					for(;indexTemp < end;indexTemp++){
						readList.add(READSYMBOL.D);
						exceptionInfo.add(READSYMBOL.D.name());		//liyang:删除的异常信息记录的有意思，直接就是一个D，人家插入时候还会记录插入的异常信息
						readQuality.append(QualitySymbol.y.name());	//liyang:一直加y在质量分数中
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
				case "s":		//liyang:我感觉ss的情况就是会占去一个E的位置，但是会加入到异常信息中，应该在最后能够还原出来
				case "S":
					//针对S的情形，有这样一个处理策略：如果S位于起始，则向后多取一个，否则则向前多取一个
					if (0 == index) {
						exceptionInfo.add(readString.substring(index,index + num + 1));
						exceptionQuality.add(readsQualityString.substring(index,index + num + 1));
						index = index + num + 1;
					} 
					//crazy:我认为ss应该不会出现在中间
					else {
						readList.remove(readList.size() - 1);
						exceptionInfo.add(readString.substring(index - 1,index + num));
						//这里对质量数的处理特别化一下，用一个字符去占位处理,而且特意是字母z
						readQuality.deleteCharAt(readQuality.length() - 1);
						exceptionQuality.add(readsQualityString.substring(index - 1,index + num));	//liyang:index+num正好可以取到.
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
