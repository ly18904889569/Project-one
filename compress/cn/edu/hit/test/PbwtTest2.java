package cn.edu.hit.test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.edu.hit.model.ReadElemEnum;

public class PbwtTest2 {

	// 这里的reads是一整条的read序列，放在readsList里面进行存储
	List<ReadStruct> readsList = new ArrayList<ReadStruct>();
	// listsOri是将原始的一行一行的Reads转化成列的形式，因为在这里处理的时候，是按照列在进行处理的
	// + 上面的注释写的简直是一坨屎，当初并不是这么处理的，而且感觉这里面有不少的荣誉信息,也不是，大部分还是基于十分有用的信息来处理的
	List<List<Integer>> listsOri = new ArrayList<List<Integer>>();

	// 创建一个序列，对start和end位点依次进行记录。。。这里是人为假定的一个过程，只是为了方便处理
	int[] start = new int[] { 1, 1, 1, 1, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
	// 若按照这种思路进行，最不容易出错的方式，就是在end记录位点的时候，多记录两个，这样改动最少，也最统一了
	int[] end = new int[] { 31, 31, 31, 31, 35, 36, 30, 38, 39, 40, 41, 42, 43, 44, 45 }; // 这里的end相当于多加了一位了

	// 写一个测试用例，使得Alignment不是对齐的。
	{
		Random rand = new Random();
		// 现在是一个listsOri里面嵌套着好几个list，总计有15条reads,每一条有31个元素
		for (int i = 0; i < 15; i++) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			if(i == 6){
				for(int j = 0; j < 30 - 7 + 1; j++){
					list.add(rand.nextInt(2));
				}
			}else{
				for (int j = 0; j < 31; j++) {
					list.add(rand.nextInt(2));
				}
			}
			
			list.add(ReadElemEnum.END.ordinal()); // 这样结果是具有一致性的，现在保证这样的PBWT能够调通，后面的就都简单了
			listsOri.add(list);
		}
		// 打印输出一下数据，看一下效果
		for (List<Integer> listOri : listsOri) {
			for (Integer ori : listOri)
				System.out.print(ori);
			System.out.println();
		}

		// 这里是为了方便初始化才这么做的，仅仅是为了方便测试，没有其他的任何意义
		// 这样整个reads也就有了.这部分是用来完善Reads的信息的
		for (int i = 0; i < 15; i++) {
			ReadStruct readStruct = new ReadStruct();
			if (i < 4) {
				readStruct.setStartAlignment(1);
				readStruct.setReads(listsOri.get(i));
				readStruct.setEndAlignment(31);
			} else if(i == 6){
				readStruct.setStartAlignment(7);
				readStruct.setReads(listsOri.get(i));
				readStruct.setEndAlignment(30);
			}else{
				readStruct.setStartAlignment(i + 1);
				readStruct.setReads(listsOri.get(i));
				readStruct.setEndAlignment(i + 31);
			}
			readsList.add(readStruct);
		}

	}

	// pbwt算法，进行pbwt转化
	public ArrayList<ArrayList<Integer>> PBWTAlgo() {
		System.out.println("Enter PBWTAlog");
		/**
		 * 从头到结尾依次进行扫描，结点额外出现的点是易于得到的
		 */
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分
		ArrayList<Integer> removeIndex = new ArrayList<Integer>();
		// listsPBWT就是经过编码后的结果。最终，还要根据这个结果来进行还原
		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		// 这个依然有其存在的必要性，有一个列表存储着当前有效的的处理reads序列的话，方便去其中去数据
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>(); // 用来存储当前位点有效的reads序列
		// 定义两个变量，用来表征何时要进行新序列进入和旧序列出去的计数值
		int enter = 0, currPosDist = 0; // currPos是想用来避免无意义的比较处理
		int readsIndex = 0;
		// 现在思路很清晰，这部分完全可以重新改写,这部分的结束位置处理好就可以了
		for (int pos = start[0]; pos <= end[end.length - 1] + 1; pos++) {
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
				// i是指代具体哪一行的位点。j是指代针对Reads序列的按行的处理
				for (int i = 0; i < readsCurrList.size(); i++) {
					int curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());

					if (readsCurrList.get(i).getStartAlignment() == pos) {
						c.add(curVal);
					} else {

						int preVal = readsCurrList.get(i).getReads()
								.get(pos - readsCurrList.get(i).getStartAlignment() - 1);

						if (preVal == 0) {
							if (curVal == ReadElemEnum.END.ordinal()) {
								removeIndex.add(i);
							}
							a.add(curVal);
						} else if (preVal == 1) {
							if (curVal == ReadElemEnum.END.ordinal()) {
								removeIndex.add(i);
							}
							b.add(curVal);
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
				a.clear();
				b.clear();
				c.clear();
				listsPBWT.add(listPBWT);
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
		for (int i = 0; i < listsPBWT.size(); i++) {
			for (Integer ins : listsPBWT.get(i)) {
				System.out.print(ins);
			}
			System.out.println();
		}
		return listsPBWT;
	}
	
		/**
		 * 这部分是进行PBWT编码的解码工作
		 * 
		 */
	public ArrayList<ArrayList<Integer>> PBWTAlgoRe(ArrayList<ArrayList<Integer>> listsPBWT) {
		ArrayList<ArrayList<Integer>> readsResult = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> readsCurrListTemp = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> d = new ArrayList<Integer>();
		ArrayList<Integer> e = new ArrayList<Integer>();
		int preSize = 0, currSize = 0, currAddSize = 0;
		int preVal = 0, currVal = 0;
		ArrayList<Integer> removeIndexRe = new ArrayList<Integer>();
		Boolean removeFlag = false;
		for (int col = 0; col < listsPBWT.size(); col++) {
			preSize = readsCurrListTemp.size();
			currSize = listsPBWT.get(col).size();
			currAddSize = currSize > preSize ? (currSize - preSize) : 0;
			for (int i = 0; i < currAddSize; i++) {
				ArrayList<Integer> listTemp = new ArrayList<Integer>();
				readsCurrListTemp.add(listTemp);
			}
			if (col == 0) {
				// 针对第一列的情形，做一下特殊化的处理
				for (int i = 0; i < readsCurrListTemp.size(); i++) {
					readsCurrListTemp.get(i).add(listsPBWT.get(col).get(i));
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
			}

			Integer[] f = new Integer[currSize];
			int k = 0;
			for (Integer ins : d) {
				f[ins] = listsPBWT.get(col).get(k++);
			}
			for (Integer ins : e) {
				f[ins] = listsPBWT.get(col).get(k++);
			}
			for (; k < currSize; k++) {
				f[k] = listsPBWT.get(col).get(k);
			}
			for (int m = 0; m < currSize; m++) {
				readsCurrListTemp.get(m).add(f[m]);
			}
			if(removeFlag){
				for(int i = 0; i <  readsCurrListTemp.size(); i++){
					if(readsCurrListTemp.get(i).get(readsCurrListTemp.get(i).size() - 1) == ReadElemEnum.END.ordinal()){
						removeIndexRe.add(i);
					}
				}
			}
			
			int offset = 0;
			for (Integer ins : removeIndexRe) {
				readsResult.add(readsCurrListTemp.get(ins - offset));
				readsCurrListTemp.remove(ins - offset);
				offset++;
			}
			removeIndexRe.clear();
			d.clear();
			e.clear();
			removeFlag = false;

		}
		System.out.println("PBWT Convert Re.");
		for (int i = 0; i < readsResult.size(); i++) {
			for (Integer ins : readsResult.get(i)) {
				System.out.print(ins);
			}
			System.out.println();
		}
		return readsResult;
	}

	public static void main(String[] args) {

		PbwtTest2 test = new PbwtTest2();
		ArrayList<ArrayList<Integer>> listsPBWT = test.PBWTAlgo();
		ArrayList<ArrayList<Integer>> listsPBWTre = test.PBWTAlgoRe(listsPBWT);
		// 这部分代码居然调试通了，实属不易，确实是个好消息,再稍微精简一下代码即可，这个实现的思想还是相当可以的
	}
}
