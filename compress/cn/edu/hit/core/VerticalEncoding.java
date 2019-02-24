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
		String filePath = "/home/rivers/riversdoc/test.sorted.bam";
//		String filePath = "/home/rivers/riversdoc/compress/chr21.fa.fasta.sam.copy2";
		List<List<ReadInfo>> readInfos = readPreProcess.splitBamFile(filePath);
		ReadsPreProcessResult reads = readPreProcess.readsProc(readInfos.get(0));
		result = this.pbwtEncode(reads);
		//现在是要把这个result信息进行编码，只要能把这个信息编码成功，就一定能够解压回来,看一下调用PBWTRE需要的参数就能明白了
		System.out.println(result.getListsPBWT().size());
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
		
/*		for(ArrayList<String> excep : exceptionList){
			System.out.println(excep);
		}*/
		
	/*	for(ArrayList<String> excapQ: exceptionListQual){
			System.out.println(excapQ);
		}*/
		
	for(ArrayList<Character> pbwtQ: listsPbwtQual){
//		System.out.println(pbwtQ);
		//输出一个各个list的长度
//		System.out.println(pbwtQ.size());
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
	public VerticalEncodeResult pbwtEncode(ReadsPreProcessResult reads) {
		//TODO:先对序列按起始位置排序一下，不然无法处理
//		Collections.sort(reads.getReadsHorizon());
		
		Integer[] start = new Integer[reads.getReadsHorizon().size()];
		Integer[] end = new Integer[reads.getReadsHorizon().size()];
		
		
		for (int i = 0; i < reads.getReadsHorizon().size(); i++) {
			start[i] = reads.getReadsHorizon().get(i).getAlignmentStart();
			end[i] = reads.getReadsHorizon().get(i).getAlignmentEnd();
			System.out.println(start[i]+"\t" + end[i]);
		}
		// 这里专门写一个适配的函数,来把过程改变一下,以便适配需要的结果
		ArrayList<ReadStruct> readsList = new ArrayList<ReadStruct>();
		for (int i = 0; i < reads.getReadsInfo().size(); i++) {
			ReadStruct struct = new ReadStruct();
			struct.setStartAlignment(reads.getReadsInfo().get(i).getAlignmentStart());
			struct.setEndAlignment(reads.getReadsInfo().get(i).getAlignmentEnd());
			// 重要的是把Reads的编码情况更改一下,变成只含有0,1,2的情形
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (int j = 0; j < reads.getReadsInfo().get(i).getReads().size(); j++) {
				READSYMBOL val = reads.getReadsInfo().get(i).getReads().get(j);
				list.add(val == READSYMBOL.E ? 0 : (val == READSYMBOL.END ? 3 : 1));
			}
			struct.setReads(list);
			struct.setException(reads.getReadsInfo().get(i).getExceptionInfo());
			struct.setReadQuality(reads.getReadsInfo().get(i).getReadQuality());
			struct.setExceptionQuality(reads.getReadsInfo().get(i).getExceptionQuality());
			readsList.add(struct);
		}

		VerticalEncodeResult res = new VerticalEncodeResult();
		//TODO 0527
		/*for(int i = 0; i < readsList.size(); i++){
			System.out.println(readsList.get(i).getReads());
			System.out.println(readsList.get(i).getReadQuality());
			System.out.println(readsList.get(i).getStartAlignment());
		}*/
		
		ArrayList<ArrayList<Integer>> pbwtResult = PBWTAlgo(readsList, start, end);
		res.setListsPBWT(pbwtResult);
		//TODO:0527 这里是有问题的，可以拿数据对比一下
		/*for(ArrayList<Integer> pbwt : pbwtResult){
			System.out.println(pbwt.toString());
		}*/
		// 这里加入一个排序，按照EndAlignment的顺序进行一次排序操作
		// 输出一下比较的前后
		Collections.sort(reads.getReadsHorizon());
		/*
		 List<ReadsHorizonModel> readsSortByEnd = reads.getReadsHorizon();
		 for(ReadsHorizonModel m : reads.getReadsHorizon()){
		 System.out.println(m.getAlignmentStart() + " : " +
		 m.getAlignmentEnd()); }
		 */
		// 测试恢复解码
//		PBWTAlgoRe(pbwtResult, reads.getReadsHorizon());
		return res;
	}

	// pbwt算法，进行pbwt转化
	public ArrayList<ArrayList<Integer>> PBWTAlgo(ArrayList<ReadStruct> readsList, Integer[] start, Integer[] end) {
		System.out.println("Enter PBWTAlog");
		/**
		 * 从头到结尾依次进行扫描，结点额外出现的点是易于得到的
		 */
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分
		//这里加入对质量数的处理
		ArrayList<Character> qualA = new ArrayList<Character>();
		ArrayList<Character> qualB = new ArrayList<Character>();
		ArrayList<Character> qualC = new ArrayList<Character>();
		ArrayList<Integer> removeIndex = new ArrayList<Integer>();
		// listsPBWT就是经过编码后的结果。最终，还要根据这个结果来进行还原
		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		// 这个依然有其存在的必要性，有一个列表存储着当前有效的的处理reads序列的话，方便去其中去数据
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>(); // 用来存储当前位点有效的reads序列
		
		//TODO:0527 确认一下start、end是按照顺序来的
		/*for(int i = 0; i < start.length; i++){
			System.out.println(start[i]+"\t" + end[i]);
		}*/
		// 质量数这里也要处理一下,这里暂时不方便处理，就变成全局变量了，方便测试结果
//		ArrayList<ArrayList<Character>> listsPbwtQual = new ArrayList<ArrayList<Character>>();
		// 定义两个变量，用来表征何时要进行新序列进入和旧序列出去的计数值
		int enter = 0, currPosDist = 0; // currPos是想用来避免无意义的比较处理
		int readsIndex = 0;
		// 现在思路很清晰，这部分完全可以重新改写,这部分的结束位置处理好就可以了
		for (int pos = start[0]; pos <= end[end.length - 1] + 1; pos++) {
			//TODO:这里针对为0 的情形特殊处理一下
			if(pos == 0){
				pos = start[++enter]-1;
				continue;
			}
//			System.out.println("enter\t"+enter+"\tend.length:\t"+end.length+ "\tstart enter:\t" + start[enter] + "\tpos:\t"+ pos);
//			System.out.println("currPosDist\t"+currPosDist);
			// 进行入列表操作
			if (currPosDist == 0) {
				while (enter < end.length && start[enter] == pos) { // 有新元素进队列的情形
					readsCurrList.add(readsList.get(readsIndex++));
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
					
					if (readsCurrList.get(i).getStartAlignment() == pos) {
						c.add(curVal);
						qualC.add(curValQual);
					} else {
						int preVal = readsCurrList.get(i).getReads()
								.get(pos - readsCurrList.get(i).getStartAlignment() - 1);

						if (preVal == 0) {
							if (curVal == ReadElemEnum.END.ordinal()) {
								removeIndex.add(i);
							}
							a.add(curVal);
							qualA.add(curValQual);
							
						} else if (preVal == 1) {
							if (curVal == ReadElemEnum.END.ordinal()) {
								removeIndex.add(i);
							}
							b.add(curVal);
							qualB.add(curValQual);
							// 这里加入异常信息,一个位点的位置,就算是一个异常信息了
							ArrayList<String> exception = new ArrayList<String>();
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
						char chT= (char)(avg/qualA.size());
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
				for (Integer ins : removeIndex) {
					readsCurrList.remove(ins - offset);
					offset++;
				}
				removeIndex.clear();
			} // End if
		}

		System.out.println("listsPBWT size:\t" + listsPBWT.size());
		// 输出一下编码的结果
		
		 /*for (int i = 0; i < listsPBWT.size(); i++) { for (Integer ins :
		 listsPBWT.get(i)) { System.out.print(ins); } System.out.println(); }*/
		 
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
			currSize = listsPBWT.get(col).size();
			currAddSize = currSize > preSize ? (currSize - preSize) : 0;
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
		for (int i = 0; i < readsResult.size()/3; i++) {
			System.out.print(startAlignment.get(i) + " : " + endAlignment.get(i) + "\t" + (endAlignment.get(i)- startAlignment.get(i)+2)+"\t");
			for (Integer ins : readsResult.get(i)) {
				System.out.print(ins);
			}
			System.out.println("\t"+ readsResult.get(i).size());
			for(Character chr : qualReadsResult.get(i)){
				System.out.print(chr);
			}
			System.out.println("\t"+ qualReadsResult.get(i).size());
			// 试着把异常信息也输出来看一下
			if (exceptionResult.get(i).size() != 0) {
				System.out.println("Exception:\t"+ exceptionResult.get(i));
				System.out.println("ExceptionQual:\t" + qualExceptionResult.get(i));
			}
			
		}

		
		// 查看一下endIndex的个数是否匹配得上
		System.out.println("endIndex num:\t" + endIndex);
		System.out.println("readsResult.size:\t" + readsResult.size());
		System.out.println("exceptionResult.size:\t" + exceptionResult.size());
		
		// 这里试着按照StartAlignment的顺序，把结果进行一次排序吧
		
		return readsResult;
	}

}
