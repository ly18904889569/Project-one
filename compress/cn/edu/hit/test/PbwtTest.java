package cn.edu.hit.test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import cn.edu.hit.model.ReadElemEnum;

public class PbwtTest {

	// 设置一下Reads序列的长度
	static int length = 30;
	// 这里的reads是一整条的read序列，放在readsList里面进行存储
	List<ReadStruct> readsList = new ArrayList<ReadStruct>();
	// listsOri是将原始的一行一行的Reads转化成列的形式，因为在这里处理的时候，是按照列在进行处理的
	List<List<Integer>> listsOri = new ArrayList();

	// 创建一个序列，对start和end位点依次进行记录。。。这里是人为假定的一个过程，只是为了方便处理
	int[] start = new int[] { 1, 1, 1, 1, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
	// 若按照这种思路进行，最不容易出错的方式，就是在end记录位点的时候，多记录两个，这样改动最少，也最统一了
	int[] end = new int[] { 31-10, 31-10, 31-10, 31-10, 35-10, 36-10, 37-10, 38-10, 39-10, 40-10, 41-10, 42-10, 43-10, 44-10, 45-10 }; // 这里的end相当于多加了一位了

	// 写一个测试用例，使得Alignment不是对齐的。
	{
		Random rand = new Random();
		// 现在是一个listsOri里面嵌套着好几个list，总计有15条reads
		for (int i = 0; i < 15; i++) {
			ArrayList<Integer> list = new ArrayList();
			list.add(ReadElemEnum.START.ordinal());
			for (int j = 0; j < 30 - 2 -10 ; j++) {
				list.add(rand.nextInt(2));
			}
			list.add(ReadElemEnum.END.ordinal());
//			System.out.println(list.size());
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
			} else {
				readStruct.setStartAlignment(i + 1);
				readStruct.setReads(listsOri.get(i));
			}
			readsList.add(readStruct);
		}

	}

	// pbwt算法，进行pbwt转化
	public void PBWTAlgo() {
		System.out.println("Enter PBWTAlgo");
		/**
		 * 从头到结尾依次进行扫描，结点额外出现的点是易于得到的
		 */
		ArrayList<Integer> a = new ArrayList(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList(); // 存储新加入的值，在这里，就是ReadElemEnum.START部分
		ArrayList<Integer> removeIndex = new ArrayList();
		// listsPBWT就是经过编码后的结果。最终，还要根据这个结果来进行还原
		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList();
		// 这个依然有其存在的必要性，有一个列表存储着当前有效的的处理reads序列的话，方便去其中去数据
		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>(); // 用来存储当前位点有效的reads序列
		// 定义两个变量，用来表征何时要进行新序列进入和旧序列出去的计数值
		int enter = 0, delete = 0, currPosDist = 0; // currPos是想用来避免无意义的比较处理
		int readsIndex = 0;
		// 这部分的操作不具有通用性，这里仅仅是为了方便测试一下对数据的处理，稍晚一些时候修改一下
		// +?当时的不通用性主要体现在哪里呢?
		for (int i = start[0]; i < end[end.length - 1]; i++) {
			// 进行入列表操作
			if (currPosDist == 0) {
				// System.out.println("start[enter]--"+end.length);
				// 这里的enter < end.length-1
				// 主要是因为enter也不能无限制下去，不然的话，很容易溢出，毕竟有个终止的位置
				while (enter < end.length && start[enter] == i) { // 有新元素进队列的情形
					readsCurrList.add(readsList.get(readsIndex++));
					enter++;
				}
				while (end[delete] == i) { // 有新元素出队列的情形
					// 其实这样处理，是有默认先进来的Reads会优先结束这么个假设.但是这里并没有起到任何作用，需要注意一下
					// 整个删除策略都有变动了，稍后把这部分的代码精简一下 + 更加准确一些 的处理方案,取出第0个元素验证一下,如果不是的话,就再遍历一遍进行确认
					System.out.println("remove:" + delete);
					readsCurrList.remove(0);
					delete++;
				}
				// currPosDist用来表示还需要历经多少个位点就该进行入队和出队操作了
				// 这里有一个需要额外处理的地方，就是可能处理到序列的最后一列了
				if (enter < end.length) {
					currPosDist = start[enter] < end[delete] ? start[enter] - i : end[delete] - i;
				}
			}
			currPosDist--;
			// 对readsCurrList里面的值进行处理
			// 第一列的元素是直接存储的，后面的元素才是根据PBWT进行编码工作的
			if (!readsCurrList.isEmpty()) {
				ArrayList<Integer> listPBWT = new ArrayList();
				// i是指代具体哪一行的位点。j是指代针对Reads序列的按行的处理
				for (int j = 0; j < readsCurrList.size(); j++) {
					if (i == start[0]) {
						// 元素逐一加入进去。这里对第一列的数据，初始位置#就被加入进去了。这部分i-start[j]虽然写的不优雅，但是也确实是那么回事
						listPBWT.add(readsCurrList.get(j).getReads().get(i - start[j]));
					} else {
						if (listsPBWT.get(i - start[0] - 1).size() <= j) {
							// 这里对前一列并没有PBWT数据的情形进行处理。这里才是真正的在新加入read序列
							// 这里的j算什么？这么处理的深意在哪里？相当于是pbwt里面前一列的长度和当前处理到第几行作比较，
							// 比这个小了，则不处理了，直接追加在后面了。目前没有发现问题的话，就先这么进行下去
							if (readsCurrList.get(j).getReads().get(
									i - readsCurrList.get(j).getStartAlignment()) != ReadElemEnum.START.ordinal()) {
								c.add(readsCurrList.get(j).getReads()
										.get(i - readsCurrList.get(j).getStartAlignment()));
							}
						} else if (ReadElemEnum.START.ordinal() == listsPBWT.get(i - start[0] - 1).get(j)) {
							// c里记录的都是所有新加入序列的元素
							c.add(readsCurrList.get(j).getReads().get(i - readsCurrList.get(j).getStartAlignment()));
						} else if (listsPBWT.get(i - start[0] - 1).get(j) == 0) {
							// 这条语句是在干什么？去Reads里面读取指定位置的位点值。这里的处理是没有问题的，可以仔细分析一下
							if (readsCurrList.get(j).getReads()
									.get(i - readsCurrList.get(j).getStartAlignment()) == ReadElemEnum.END.ordinal()) {
								removeIndex.add(j);
							} else {
								a.add(readsCurrList.get(j).getReads()
										.get(i - readsCurrList.get(j).getStartAlignment()));
							}
						} else if (listsPBWT.get(i - start[0] - 1).get(j) == 1) {
							if (readsCurrList.get(j).getReads()
									.get(i - readsCurrList.get(j).getStartAlignment()) == ReadElemEnum.END.ordinal()) {
								removeIndex.add(j);
							} else {
								b.add(readsCurrList.get(j).getReads()
										.get(i - readsCurrList.get(j).getStartAlignment()));
							}
						} else if (ReadElemEnum.END.ordinal() == listsPBWT.get(i - start[0] - 1).get(j)) {
							removeIndex.add(j);
							// 这是针对获得前面一列是#的情形，那这里不处理就OK了，这样也就自动脱落了
							System.out.println("ReadElemEnum.END.ordinal:" + j);
						} else {
							// 这里应该是永远不会出现才对
							System.out.println("Enter else...");
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
				// System.out.println("listPBWT:"+listPBWT.size());
				// System.out.println("removeIndex.size:"+removeIndex.size());
				// int num = removeIndex.size();
				// while(num-->0){
				// readsCurrList.remove(0);
				// }
				removeIndex.clear();
			} // End if
		}

//		System.out.println(listsPBWT.size());
		// 输出一下编码的结果
		for (int i = 0; i < listsPBWT.size(); i++) {
			for (Integer ins : listsPBWT.get(i)) {
				System.out.print(ins);
			}
			System.out.println();
		}

		// 这部分是还原回来
		List<List<Integer>> listsPbwtRe = new ArrayList();
		List<Integer> c1 = new ArrayList<Integer>();
		List<Integer> d = new ArrayList<Integer>();
		// i row size, j col size
		for (int i = 1; i < listsPBWT.size(); i++) {
			List<Integer> listPbwtRe = new ArrayList<Integer>();
			for (int j = 0; j < listsPBWT.get(i).size(); j++) {
				if (i == 1) {
					listPbwtRe.add(listsPBWT.get(i).get(j));
				} else if (listsPBWT.get(i - 1).size() >= j + 1 && listsPBWT.get(i - 1).get(j) == 0) {
					// store the location
					c1.add(j);
				} else {
					d.add(j);
				}
			}
			Integer[] e = new Integer[listsPBWT.get(i).size()];
			int k = 0;
			for (Integer ins : c1) {
				e[ins] = listsPBWT.get(i).get(k++);
			}
			for (Integer ins : d) {
				e[ins] = listsPBWT.get(i).get(k++);
			}
			c1.clear();
			d.clear();
			if (i != 1) {
				listPbwtRe = Arrays.asList(e);
			}
			listsPbwtRe.add(listPbwtRe);
		}
		System.out.println("PBWT Convert Re.");
		for (int i = 0; i < listsPbwtRe.size(); i++) {
			for (Integer ins : listsPbwtRe.get(i)) {
				System.out.print(ins);
			}
			System.out.println();
		}
		// 这里再做一次截断，按照start和end
	}

	public static void main(String[] args) {

		PbwtTest test = new PbwtTest();
		test.PBWTAlgo();
		System.out.println("PBWTAlgo End...");

		try {
			Thread.sleep(10000);
		} catch (Exception e) {

		}
		/**
		 * 这个下面就是原生的PBWT的实现方法
		 */
		// random a serious number
		Random rand = new Random();

		// initial list
		List<List<Integer>> listsOri = new ArrayList();
		List<List<Integer>> listsConvert = new ArrayList();
		List<List<Integer>> listsPBWT = new ArrayList();
		List<List<Integer>> listsPbwtRe = new ArrayList();
		for (int i = 0; i < 15; i++) {
			List<Integer> list = new ArrayList();
			for (int j = 0; j < 30; j++) {
				list.add(rand.nextInt(2));
			}
			listsOri.add(list);
		}

		// write into file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("/home/rivers/riversdoc/compress/pbwt.txt", "UTF-8");
		} catch (Exception e) {
		}

		writer.flush();
		writer.println("Origional data:");
		for (int i = 0; i < listsOri.size(); i++) {
			for (Integer ins : listsOri.get(i)) {
				writer.print(ins);
			}
			writer.println();
		}
		writer.flush();
		// writer.close();

		// row convert to col
		// i row size, j col size
		for (int j = 0; j < listsOri.get(0).size(); j++) {
			ArrayList<Integer> listConvert = new ArrayList<Integer>();
			for (int i = 0; i < listsOri.size(); i++) {
				listConvert.add(listsOri.get(i).get(j));
			}
			listsConvert.add(listConvert);
		}

		writer.println("Convert data:");
		for (int i = 0; i < listsConvert.size(); i++) {
			for (Integer ins : listsConvert.get(i)) {
				System.out.print(ins);
				writer.print(ins);
			}
			System.out.println();
			writer.println();
		}
		writer.flush();

		System.out.println("Write Convert data OK.");

		// Encode pbwt 最后编写完成了，是按列在存储的
		// init array a,b to story data
		List<Integer> a = new ArrayList();
		List<Integer> b = new ArrayList();
		// i row size, j col size
		for (int i = 0; i < listsConvert.size(); i++) {
			List<Integer> listPBWT = new ArrayList();
			for (int j = 0; j < listsConvert.get(0).size(); j++) {
				// the first column
				if (i == 0) {
					listPBWT.add(listsConvert.get(i).get(j));
				}
				// not first column
				else if (listsPBWT.get(i - 1).get(j) == 0) {
					a.add(listsConvert.get(i).get(j));
				} else {
					b.add(listsConvert.get(i).get(j));
				}
			}
			// add pbwt value to listsPBWT
			for (Integer ins : a) {
				listPBWT.add(ins);
			}
			for (Integer ins : b) {
				listPBWT.add(ins);
			}
			a.clear();
			b.clear();
			listsPBWT.add(listPBWT);
		}

		// write pbwt file
		writer.println("PBWT Convert.");
		for (int i = 0; i < listsPBWT.size(); i++) {
			for (Integer ins : listsPBWT.get(i)) {
				System.out.print(ins);
				writer.print(ins);
			}
			System.out.println();
			writer.println();
		}
		writer.flush();

		System.out.println("Write PBWT Convert OK.");

		// pbwt file convert to Original file
		List<Integer> c = new ArrayList();
		List<Integer> d = new ArrayList();
		// i row size, j col size
		for (int i = 0; i < listsPBWT.size(); i++) {
			List<Integer> listPbwtRe = new ArrayList();
			for (int j = 0; j < listsPBWT.get(0).size(); j++) {
				if (i == 0) {
					listPbwtRe.add(listsPBWT.get(i).get(j));
				} else if (listsPBWT.get(i - 1).get(j) == 0) {
					// store the location
					c.add(j);
				} else {
					d.add(j);
				}
			}
			Integer[] e = new Integer[listsPBWT.get(0).size()];
			int k = 0;
			for (Integer ins : c) {
				e[ins] = listsPBWT.get(i).get(k++);
			}
			for (Integer ins : d) {
				e[ins] = listsPBWT.get(i).get(k++);
			}
			c.clear();
			d.clear();
			if (i != 0) {
				listPbwtRe = Arrays.asList(e);
			}
			listsPbwtRe.add(listPbwtRe);
		}

		// write pbwt file re
		writer.println("PBWT Convert Re.");
		for (int i = 0; i < listsPbwtRe.size(); i++) {
			for (Integer ins : listsPbwtRe.get(i)) {
				System.out.print(ins);
				writer.print(ins);
			}
			System.out.println();
			writer.println();
		}
		writer.flush();

		System.out.println("Write PBWT  Re Convert OK.");

		System.out.println("END");

	}
}
