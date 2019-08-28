package cn.edu.hit.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cn.edu.hit.model.PBWTReadRe;
import cn.edu.hit.model.READSYMBOL;
import cn.edu.hit.model.ReadElemEnum;
import cn.edu.hit.model.ReadInfo;
import cn.edu.hit.model.ReadsHorizonModel;
import cn.edu.hit.model.ReadsPreProcessResult;
import cn.edu.hit.model.VerticalEncodeResult;
import cn.edu.hit.test.ReadStruct;

/**
 * 利用PBWT进行垂直编码
 * 
 * @author rivers May 29, 2017 10:20:32 PM
 *
 */
public class VerticalEncoding {

	public static void main(String[] args) {
		VerticalEncoding encoding = new VerticalEncoding();
		encoding.init();
	}
	
	public void init(){
		ReadPreProcess readPreProcess = new ReadPreProcess();
//		String filePath = "/home/rivers/riversdoc/test.sorted.bam";
//		String filePath = "/home/yangli/Documents/compress/exceptionInfo/read2.fastq.sorted.bam";
//		String filePath = "/home/yangli/Documents/compress/10X/NC_10X.fastq.sorted.bam";
//		String filePath = "/home/rivers/riversdoc/compress/chr21.fa.fasta.sam.copy2";
		String filePath = "/home/liyang/Document/compress/input/50X/NC_50X.fastq.sorted.bam";
		List<List<ReadInfo>> readInfos = readPreProcess.splitBamFile(filePath);	
		ReadsPreProcessResult reads = readPreProcess.readsProc(readInfos.get(0));
		result = this.pbwtEncode(reads);
		//现在是要把这个result信息进行编码，只要能把这个信息编码成功，就一定能够解压回来,看一下调用PBWTRE需要的参数就能明白了
//		System.out.println(result.getListsPBWT().size());
		System.out.println("ReadPreProcess and PBWT is over");
//		this.PrintResult();
	}

	static VerticalEncodeResult result = null;
	
	// TODO 这里有一个疑惑,为何不改成Character,而是用String?貌似都是像Character一样在处理
	// 写一个对异常信息的处理,这里先简易处理
	static ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
	// 这里是对异常信息的质量数的处理，也是先简单写写吧
	static ArrayList<ArrayList<String>> exceptionListQual = new ArrayList<ArrayList<String>>();
	// 质量数这里也要处理一下,这里暂时不方便处理，就变成全局变量了，方便测试结果
	static ArrayList<ArrayList<Character>> listsPbwtQual = new ArrayList<ArrayList<Character>>();
	
	public void PrintResult(){
		
		for(ArrayList<String> excep : exceptionList){
			System.out.println(excep);
		}
		
		for(ArrayList<String> excapQ: exceptionListQual){
			System.out.println(excapQ);
		}
		
	for(ArrayList<Character> pbwtQ: listsPbwtQual){
		System.out.println(pbwtQ);
		//输出一个各个list的长度
		System.out.println(pbwtQ.size());
	}
		
		
	}
	
	public ArrayList<ArrayList<String>> getExceptionListData(){
		VerticalEncoding encoding = new VerticalEncoding();
		encoding.init();
		return this.exceptionList;
	}
	
	public ArrayList<ArrayList<String>> getExceptionQual(){
		VerticalEncoding encoding = new VerticalEncoding();
		encoding.init();
		return this.exceptionListQual;
	}
	
	public ArrayList<ArrayList<Character>> getPbwtQual(){
		VerticalEncoding encoding = new VerticalEncoding();
		encoding.init();
		return this.listsPbwtQual;
	}
	
	public VerticalEncodeResult getVerticalEncodeREsult(){
		VerticalEncoding encoding = new VerticalEncoding();
		encoding.init();
		return this.result;
	}
	
	
	
	/**
	 * 利用PBWT进行垂直编码
	 * 
	 * @param reads
	 * @return
	 */
	//liyang：这一段程序把getread是horizon全部注释掉了
	public VerticalEncodeResult pbwtEncode(ReadsPreProcessResult reads) {
		//TODO:先对序列按起始位置排序一下，不然无法处理
//		Collections.sort(reads.getReadsHorizon());	//liyang:实现了按照结束位置end进行排序
//		liyang:在samtools已经实现了按照start进行排序
		Integer[] start = new Integer[reads.getReadsHorizon().size()];
		Integer[] end = new Integer[reads.getReadsHorizon().size()];
		
		
		for (int i = 0; i < reads.getReadsHorizon().size(); i++) {
			start[i] = reads.getReadsHorizon().get(i).getAlignmentStart();
			end[i] = reads.getReadsHorizon().get(i).getAlignmentEnd();
//			System.out.println(start[i]+"\t" + end[i]);
		}
		// 这里专门写一个适配的函数,来把过程改变一下,以便适配需要的结果
		ArrayList<ReadStruct> readsList = new ArrayList<ReadStruct>();
		for (int i = 0; i < reads.getReadsInfo().size(); i++) {
			ReadStruct struct = new ReadStruct();
			struct.setStartAlignment(reads.getReadsInfo().get(i).getAlignmentStart());
			struct.setEndAlignment(reads.getReadsInfo().get(i).getAlignmentEnd());
			// 重要的是把Reads的编码情况更改一下,变成只含有0,1,2的情形		//liyang:出现了一次2的问题，下面存储的是3
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (int j = 0; j < reads.getReadsInfo().get(i).getReads().size(); j++) {//liyang:getReadsInfo()函数是readProProcess中的一个函数，这个函数是预处理生成
				READSYMBOL val = reads.getReadsInfo().get(i).getReads().get(j);			//readMatchResult列表和readHorion，其中getread就是readmatchresult的函数
				list.add(val == READSYMBOL.E ? 0 : (val == READSYMBOL.END ? 3 : 1));//liyang:相同0，不同1，结束3
			}
			struct.setReads(list);	//liyang：list是用来存放readsymbol信息的列表容器，
			struct.setException(reads.getReadsInfo().get(i).getExceptionInfo());	//liiyang:存储预处理后比对后的信息
			struct.setReadQuality(reads.getReadsInfo().get(i).getReadQuality());
			struct.setExceptionQuality(reads.getReadsInfo().get(i).getExceptionQuality());
			readsList.add(struct);
		}

		VerticalEncodeResult res = new VerticalEncodeResult();	//liyang:为了输出专门设置的一个变量
		//TODO 0527		//liyang：下面一段代码在之前是注释掉的，起作用就是输出查看一下二进制编码之后的cigar
//		for(int i = 0; i < readsList.size(); i++){
//			System.out.println(readsList.get(i).getReads());
//			System.out.println(readsList.get(i).getReadQuality());
//			System.out.println(readsList.get(i).getStartAlignment());
//		}
		//liyang:这里只是将预处理的reads中的read（cigar）加入到了verticalencoderesult的res中
		ArrayList<ArrayList<Integer>> pbwtResult = PBWTAlgo(readsList, start, end);	//liyang：进行pbwt转化，这里说白了就是存放的转换为数字的cigar
		res.setListsPBWT(pbwtResult);
		//TODO:0527 这里是有问题的，可以拿数据对比一下
		System.out.println("PBWTAlgo is OK!");
//		for(ArrayList<Integer> pbwt : pbwtResult){
//			System.out.println(pbwt.toString());
//		}
		// 这里加入一个排序，按照EndAlignment的顺序进行一次排序操作
		// 输出一下比较的前后
		Collections.sort(reads.getReadsHorizon());	//liyang:这个排序是按照end进行的排序,这里为什么要用end，进行排序呢，非常的奇怪
		
//		 List<ReadsHorizonModel> readsSortByEnd = reads.getReadsHorizon();
//		 res.setReadsHorizon(readsSortByEnd);
//		 for(ReadsHorizonModel m : reads.getReadsHorizon()){
//		 System.out.println(m.getAlignmentStart() + " : " +
//		 m.getAlignmentEnd()); }	//liyang:zhushidiao
		 
		// 测试恢复解码
//		PBWTAlgoRe(pbwtResult, reads.getReadsHorizon());
		return res;//liyang:这个函数主要是首先将read存储到readstruct这个结构体中，然后处理每个read放到readslist中
	}				//最后交给PBWT进行编码。

	// pbwt算法，进行pbwt转化
	public ArrayList<ArrayList<Integer>> PBWTAlgo(ArrayList<ReadStruct> readsList, Integer[] start, Integer[] end) {
		System.out.println("Enter PBWTAlog");
		/**
		 * 从头到结尾依次进行扫描，结点额外出现的点是易于得到的
		 */
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分	liyang:应该是存储3
		//这里加入对质量数的处理
		ArrayList<Character> qualA = new ArrayList<Character>();
		ArrayList<Character> qualB = new ArrayList<Character>();
		ArrayList<Character> qualC = new ArrayList<Character>();
		ArrayList<Integer> removeIndex = new ArrayList<Integer>();
		// listsPBWT就是经过编码后的结果。最终，还要根据这个结果来进行还原
		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		// 这个依然有其存在的必要性，有一个列表存储着当前有效的的处理reads序列的话，方便去其中取数据
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>(); // 用来存储当前位点有效的reads序列
		
		//TODO:0527 确认一下start、end是按照顺序来的
		for(int i = 0; i < start.length; i++){
//			System.out.println(start[i]+"\t" + end[i]);
			if(i>1&&start[i]<start[i-1])
			{
				System.out.println("It doesn't sort by start");
				System.exit(0);
			}
//			System.out.println("The sort is OK");
		}
		// 质量数这里也要处理一下,这里暂时不方便处理，就变成全局变量了，方便测试结果
//		ArrayList<ArrayList<Character>> listsPbwtQual = new ArrayList<ArrayList<Character>>();
		// 定义两个变量，用来表征何时要进行新序列进入和旧序列出去的计数值
		int enter = 0, currPosDist = 0; // liyang:enter表示进入垂直压缩序列的数目，currPosDist表示当前索引到下一个read距离
		int readsIndex = 0;	
		// 现在思路很清晰，这部分完全可以重新改写,这部分的结束位置处理好就可以了
		int endindex=end.length - 1;	//liyang:这么处理的原因是因为发现其实最后一个位置会出现0，而且在预处理过程中会将一些read认为是"*"，从而导致最终数量减少
		//crazy:因为处理三代数据过程中数据长度变化大，并非最后一个end的就是整个reads的最大点位，需要找到最长
		int tmp=-1;
		for(int i=end.length-1;i>=0;i--)
		{
			if(end[i]>=tmp)
			{
				tmp = end[i];
				endindex = i;
			}
		}
//		System.out.println(end[endindex]);
//		while(end[endindex]==0)
//		{
//			endindex--;	//liyang:必须保证最后能够能够进入到PBWT中
//		}
		System.out.println("endIndex:"+endindex);
//		System.out.println(end.length);
		System.out.println("prstartIndex:"+start.length);	//注意一点，在这里预处理的过程中会在最后加上一个3，所以说会长一些
		System.out.println("The length of pbwt:"+(end[endindex]-start[0]+1+1));

		
		for (int pos = start[0]; pos <= end[endindex] + 1; pos++) {			//liyang:大循环遍历所有的位置
			//TODO:这里针对为0 的情形特殊处理一下,为什么这里要对起始位置为0的特殊处理一下？
			if(pos == 0){
				pos = start[++enter]-1;
				continue;
			}
//			System.out.println("enter\t"+enter+"\tend.length:\t"+end.length+ "\tstart enter:\t" + start[enter] + "\tpos:\t"+ pos);
//			System.out.println("currPosDist\t"+currPosDist);
			// 进行入列表操作
			if (currPosDist == 0) {	//liyang：这里是每条新的序列才能够进行的，只有currposdist减到0,才能加入下一条序列
				while (enter < end.length && start[enter] == pos) { // 有新元素进队列的情形 liyang：这里这个设计非常的完美pos一直加，而currposdist一直减
					readsCurrList.add(readsList.get(readsIndex++));	//liyang:序列从横向的排列变成了纵向的排列，把所有的开头位置一样的先存储一下
					enter++;
				}
				if (enter < end.length) {
					currPosDist = start[enter] - pos;
				}
			}
			currPosDist--;
			if (!readsCurrList.isEmpty()) {
				ArrayList<Integer> listPBWT = new ArrayList<Integer>();
				// 这是对质量数的处理
				ArrayList<Character> listPbwtQual = new ArrayList<Character>();
				// i是指代具体哪一行的位点。j是指代针对Reads序列的按行的处理
				for (int i = 0; i < readsCurrList.size(); i++) {
//					System.out.println(readsCurrList.get(i).getReads().size() + "\t" + readsCurrList.get(i).getReadQuality().length());
//					System.out.println(pos - readsCurrList.get(i).getStartAlignment());
//					System.out.println("pos\t"+pos+"\t alignmet start\t"+readsCurrList.get(i).getStartAlignment());
					int curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());
					char curValQual = readsCurrList.get(i).getReadQuality().charAt(pos - readsCurrList.get(i).getStartAlignment());
					
					if (readsCurrList.get(i).getStartAlignment() == pos) {	//liyang：如果不进入这个循环说明已经不是开始位置了,只有起始存放到c中
						c.add(curVal);		//liyang:为什么要单独设置一个c用于记录开始，不用和b合并处理？，因为起始位置无法记录之前的位置
						qualC.add(curValQual);
					} else {
						int preVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment() - 1);	//liyang：这里是要记录前一个字符的数值
						if (preVal == 0) {
							if (curVal == ReadElemEnum.END.ordinal()) {
								removeIndex.add(i);
							}
							a.add(curVal);	//crazy:这里注意一下当为结束符号3的时候同样也会加入到其中
							qualA.add(curValQual);
							
						} else if (preVal == 1) {
							if (curVal == ReadElemEnum.END.ordinal()) {
								removeIndex.add(i);
							}
							b.add(curVal);
							qualB.add(curValQual);
							// 这里加入异常信息,一个位点的位置,就算是一个异常信息了
							ArrayList<String> exception = new ArrayList<String>();	//liyang：当前点位是0,前一个点位是1,但是却要把当前点位的东西加入
//							System.out.println("startAlignment\t"+readsCurrList.get(i).getStartAlignment()+"\tException size:\t"+readsCurrList.get(i).getException().size());
							exception.add(readsCurrList.get(i).getException().get(0));
							exceptionList.add(exception);
							// 这里也把质量数也加入进去
							ArrayList<String> exceptionQual = new ArrayList<String>();
							exceptionQual.add(readsCurrList.get(i).getExceptionQuality().get(0));
							exceptionListQual.add(exceptionQual);
							readsCurrList.get(i).getException().remove(0);
							readsCurrList.get(i).getExceptionQuality().remove(0);
						} else {
							System.out.println("Invalid ELSE readsCurrList process..");
						}
					}
				} // End for
				
				// add pbwt value to listsPBWT
				for (Integer ins : a) {
					listPBWT.add(ins);
				}
				for (Integer ins : b) {
					listPBWT.add(ins);
				}
				for (Integer ins : c) {
					listPBWT.add(ins);
				}
				listsPBWT.add(listPBWT);
				//TODO:0527
//				System.out.println(b);
				// 质量数的处理加入进来
				int avg = 0;
				for(Character chr : qualA){
					avg += chr;
				}
				//TODO 后期这里针对质量分数,单独做一个处理
				for(Character chr : qualA){
					if(qualA.size() == 0){
						listPbwtQual.add(chr);
					}else{
						char chT= (char)(avg/qualA.size());	//liyang：将数字转换为了字符
						listPbwtQual.add(chT);
					}
				}
				for(Character chr : qualB){
					listPbwtQual.add(chr);
				}
				for(Character chr : qualC){
					listPbwtQual.add(chr);
				}
				listsPbwtQual.add(listPbwtQual);
				
				a.clear();
				b.clear();
				c.clear();
				qualA.clear();
				qualB.clear();
				qualC.clear();
				
				// 从readsCurrList里面移出结束符的数据
				int offset = 0;
				for (Integer ins : removeIndex) {	//liyang：将处理的临时存取序列的数组清空
					readsCurrList.remove(ins - offset);
					offset++;
				}
				removeIndex.clear();
			} // End if
		}

		System.out.println("listsPBWT size:\t" + listsPBWT.size());
		// 输出一下编码的结果
		
//		for (int i = 0; i < listsPBWT.size(); i++){ 
//			for (Integer ins :listsPBWT.get(i)) 
//				{ 
//					System.out.print(ins); 
//				} 
//			System.out.println(); 
//			}
		 
		return listsPBWT;
	}

	/**
	 * 这部分是进行PBWT编码的解码工作
	 * 
	 */
	public ArrayList<ArrayList<Integer>> PBWTAlgoRe(ArrayList<ArrayList<Integer>> listsPBWT,
			List<ReadsHorizonModel> startAndEndList) {
		// 往回解的时候，可以在外面再包装一层，这样的话，异常信息也就有地方放置了
		PBWTReadRe pbwtReadRe = new PBWTReadRe();
		ArrayList<ArrayList<Integer>> readsResult = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<String>> exceptionResult = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Integer>> readsCurrListTemp = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<String>> exceptionResultTemp = new ArrayList<ArrayList<String>>();
		pbwtReadRe.setReadsResult(readsResult);
		pbwtReadRe.setException(exceptionResult);
		ArrayList<Integer> startAlignment = new ArrayList<Integer>();
		ArrayList<Integer> endAlignment = new ArrayList<Integer>();
		pbwtReadRe.setStartAlignment(startAlignment);
		pbwtReadRe.setEndAlignment(endAlignment);
		ArrayList<Integer> d = new ArrayList<Integer>();
		ArrayList<Integer> e = new ArrayList<Integer>();
		// 这里加入对质量数的处理
		ArrayList<ArrayList<Character>> qualReadsResult = new ArrayList<ArrayList<Character>>();
		ArrayList<ArrayList<String>> qualExceptionResult = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Character>> qualReadsCurrListTemp = new ArrayList<ArrayList<Character>>();
		ArrayList<ArrayList<String>> qualExceptionResultTemp = new ArrayList<ArrayList<String>>();
		
		int preSize = 0, currSize = 0, currAddSize = 0;
		int preVal = 0, currVal = 0;
		ArrayList<Integer> removeIndexRe = new ArrayList<Integer>();
		Boolean removeFlag = false;
		// exceptionList 的唯一index
		int exceptionIndex = 0;
		int endIndex = 0;
		
		for (int col = 0; col < listsPBWT.size(); col++) {
			preSize = readsCurrListTemp.size();
			currSize = listsPBWT.get(col).size();	//liyang：没有currsize的大小是1,这就说明了这里只是拿出了第一个容器
			currAddSize = currSize > preSize ? (currSize - preSize) : 0;	//crazy：这里的目的是为了后面构造还原空间的
			for (int i = 0; i < currAddSize; i++) {
				ArrayList<Integer> listTemp = new ArrayList<Integer>();
				readsCurrListTemp.add(listTemp);
				ArrayList<String> listExceptionTemp = new ArrayList<String>();
				exceptionResultTemp.add(listExceptionTemp);
				
				ArrayList<Character> listTemp2 = new ArrayList<Character>();
				qualReadsCurrListTemp.add(listTemp2);
				ArrayList<String> listExceptionTemp2 = new ArrayList<String>();
				qualExceptionResultTemp.add(listExceptionTemp2);
			}
			if (col == 0) {
				// 针对第一列的情形，做一下特殊化的处理
				for (int i = 0; i < readsCurrListTemp.size(); i++) {
					readsCurrListTemp.get(i).add(listsPBWT.get(col).get(i));
					//这是质量数部分的处理
					qualReadsCurrListTemp.get(i).add(listsPbwtQual.get(col).get(i));
				}
				continue;
			}
			for (int row = 0; row < preSize; row++) {
				preVal = readsCurrListTemp.get(row).get(readsCurrListTemp.get(row).size() - 1);
				currVal = listsPBWT.get(col).get(row);
				if (currVal == ReadElemEnum.END.ordinal()) {
					removeFlag = true;
				}
				if (preVal == 0) {
					d.add(row);
				} else if (preVal == 1) {
					e.add(row);
				} else {
					System.out.println("Invalid ELSE");
				}

				// 单独写一下对当前位点的处理，把异常信息值写入进来
				if (preVal == 1) {
					// 这里这么处理，是由于exceptionList的数据特点来决定的，这部分可以仔细想一下
					qualExceptionResultTemp.get(row).addAll(exceptionListQual.get(exceptionIndex));
					exceptionResultTemp.get(row).addAll(exceptionList.get(exceptionIndex++));
				}
			}

			Integer[] f = new Integer[currSize];
			// 这里加入对质量数的处理, 异常的质量数，稍后再加进去吧，现在有点搞不明白了
			Character[] qualF = new Character[currSize];
			int k = 0;
			for (Integer ins : d) {
				qualF[ins] = listsPbwtQual.get(col).get(k);
				f[ins] = listsPBWT.get(col).get(k++);
				
			}
			for (Integer ins : e) {
				qualF[ins] = listsPbwtQual.get(col).get(k);
				f[ins] = listsPBWT.get(col).get(k++);
			}
			for (; k < currSize; k++) {
				qualF[k] = listsPbwtQual.get(col).get(k);
				f[k] = listsPBWT.get(col).get(k);
			}
			for (int m = 0; m < currSize; m++) {
				readsCurrListTemp.get(m).add(f[m]);
				qualReadsCurrListTemp.get(m).add(qualF[m]);
			}
			
			
			
			if (removeFlag) {
				for (int i = 0; i < readsCurrListTemp.size(); i++) {
					if (readsCurrListTemp.get(i).get(readsCurrListTemp.get(i).size() - 1) == ReadElemEnum.END
							.ordinal()) {
						removeIndexRe.add(i);
					}
				}
			}

			
			int offset = 0;
			for (Integer ins : removeIndexRe) {
				readsResult.add(readsCurrListTemp.get(ins - offset));
				readsCurrListTemp.remove(ins - offset);
				exceptionResult.add(exceptionResultTemp.get(ins - offset));
				exceptionResultTemp.remove(ins - offset);
				// startAlignment 和 endAlignment都处理一下
				startAlignment.add(startAndEndList.get(endIndex).getAlignmentStart());
				endAlignment.add(startAndEndList.get(endIndex++).getAlignmentEnd());
				// 这里把质量数加入进来
				qualReadsResult.add(qualReadsCurrListTemp.get(ins - offset));
				qualReadsCurrListTemp.remove(ins - offset);
				qualExceptionResult.add(qualExceptionResultTemp.get(ins - offset));
				qualExceptionResultTemp.remove(ins - offset);
				offset++;
			}
			removeIndexRe.clear();
			d.clear();
			e.clear();
			removeFlag = false;

		}

		
		System.out.println("PBWT Convert Re.");
//		for (int i = 0; i < readsResult.size(); i++) {
//			System.out.println(i+"	"+startAlignment.get(i) + " : " + endAlignment.get(i) + "\t" + (endAlignment.get(i)- startAlignment.get(i)+2)+"\t");
//			for (Integer ins : readsResult.get(i)) {
//				System.out.print(ins);
//			}
//			System.out.println("\t"+ readsResult.get(i).size());
//			for(Character chr : qualReadsResult.get(i)){
//				System.out.print(chr);
//			}
//			System.out.println("\t"+ qualReadsResult.get(i).size());
//			// 试着把异常信息也输出来看一下
//			if (exceptionResult.get(i).size() != 0) {
//				System.out.println("Exception:\t"+ exceptionResult.get(i));
//				System.out.println("ExceptionQual:\t" + qualExceptionResult.get(i));
//			}
//			
//		}

		
		// 查看一下endIndex的个数是否匹配得上
		System.out.println("endIndex num:\t" + endIndex);
		System.out.println("readsResult.size:\t" + readsResult.size());
		System.out.println("exceptionResult.size:\t" + exceptionResult.size());
		
		// 这里试着按照StartAlignment的顺序，把结果进行一次排序吧
		
		return readsResult;
	}
	
}
