package cn.edu.hit.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import cn.edu.hit.model.ExceptionSYMBOL;
import cn.edu.hit.model.ReadInfo;
import cn.edu.hit.model.ReadsHorizonModel;
import cn.edu.hit.model.ReadsPreProcessResult;
import cn.edu.hit.model.VerticalEncodeResult;

/**
 * 水平编码
 * @author rivers
 * Jun 17, 2017 9:05:09 AM
 *
 */
public class HorizonEncoding {

	public static void main(String[] args) {
		VerticalEncoding encoding = new VerticalEncoding();
		ReadPreProcess readPreProcess = new ReadPreProcess();
		String filePath = "/home/rivers/riversdoc/test.sorted.bam";
		List<List<ReadInfo>> readInfos = readPreProcess.splitBamFile(filePath);
		ReadsPreProcessResult reads = readPreProcess.readsProc(readInfos.get(0));
		VerticalEncodeResult verRes = encoding.pbwtEncode(reads);
		
		
		HorizonEncoding hor = new HorizonEncoding();
		//对这里的处理，逐个验证有效性
		Byte[] by1 = hor.readsRL(verRes);
		System.out.println("by1.length:\t" + by1.length);
		Byte[] by2 = hor.exceptionRL(reads);
//		Byte[] by3 = hor.horizonRL(reads.getReadsHorizon());
		
		
		//写入文件操作
		String fileDest1 = "/home/rivers/riversdoc/compress/by1.txt";
		String fileDest2 = "/home/rivers/riversdoc/compress/by2.txt";
		String fileDest3 = "/home/rivers/riversdoc/compress/by3.txt";
		
		HorizonEncoding.writeBytesToFileNio(ArrayUtils.toPrimitive(by1), fileDest1);
//		HorizonEncoding.writeBytesToFileNio(ArrayUtils.toPrimitive(by2), fileDest2);
//		HorizonEncoding.writeBytesToFileNio(ArrayUtils.toPrimitive(by3), fileDest3);
		
	}
	
	//写入byte[]到文件里
	private static void writeBytesToFileNio(byte[] bFile, String fileDest) {

        try {
            Path path = Paths.get(fileDest);
            Files.write(path, bFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
	
	/**
	 * 对reads纵向游程编码
	 * @return
	 */
	Byte[] readsRL(VerticalEncodeResult res){
		//这里问题还是很大的，垂直编码这个结果根本就顺不过来
		ArrayList<ArrayList<Integer>> listsPBWT = res.getListsPBWT();
		List<Integer> keys = new ArrayList<Integer>();
		List<Integer> values = new ArrayList<Integer>();
		//游程编码部分编一下
		//这里统计一下0、1、3的个数，然后进行游程编码
		int countZero=0, countOne = 0, countThree = 0;
		int currentNum = 0;
		for(ArrayList<Integer> listPbwt : listsPBWT){
			for(Integer val : listPbwt){
				if(val != currentNum){
					if (currentNum == 0) {
						keys.add(0);
						values.add(countZero);
					}else if (currentNum == 1){
						keys.add(1);
						values.add(countOne);
					}else{
						keys.add(3);
						values.add(countThree);
					}
					//计数器清零
					countZero = 0;
					countOne = 0;
					countThree = 0;
				}
				currentNum = val;
				if(val == 0) {
					countZero ++;
				}else if(val == 1){
					countOne ++;
				}else if (val == 3){
					countThree ++;
				}else{
					//这里的问题在于后来采用了3作为end的处理方式
					System.out.println("readsRL count Error....");
				}
			}
		}
		
		// TODO 这部分内容要改，垂直编码的东西，在这部分进行压缩编码的时候，需要调整的东西还很多
		
		//可以输出一下编码的长度
		/*for(int i = 0; i < 20; i++){
			System.out.print(keys.get(i)+" ");
		}
		System.out.println();
		for(int i = 0; i < 20; i++){
			System.out.print(values.get(i)+" ");
		}
		System.out.println();
		System.out.println(listsPBWT.get(0));
		*/
		
		//对结果游程编码
		Byte[] byRes = convert2ByteRL(keys, values);
		//这部分基本是完成了，就差一个写入文件的操作了
		
		return byRes;
	}
	
	static Byte[] convert2ByteRL(List<Integer> keys, List<Integer> values){
		// 高位为0,表示编码结束，高位为1,则要与后续的编码结合,最大范围可到4096
		//+ 最后两位表示存储的值是0、1、2
		// 为0的情形智能存储32大小的数值
		int  orNum1 = -128, num1 = 0, num2 = 0;
		byte by = 0, by1 = 0;
		List<Byte> bys = new ArrayList<Byte>();
		for(int i = 0; i < keys.size(); i++) {
			if(values.get(i) < 32){
				by = (byte)(values.get(i) <<2 | keys.get(i));
				bys.add(by);
			}else {
				num1 = values.get(i) / 32;
				num2 = values.get(i) % 32;
				by1 = (byte)(num1 | orNum1);
				by = (byte)(num2 << 2 | keys.get(i));
				bys.add(by1);
				bys.add(by);
			}
		}
		
		//打印看一下结果 
		for(int i = 0; i < 10; i++){
			System.out.print(bys.get(i)+" ");
		}
		
		//反解回去一下，验证结果的正确性
		rlByteConvertRe(bys);
		
		
		return bys.toArray(new Byte[bys.size()]);
	}
	
	static void rlByteConvertRe(List<Byte>  bys) {
		List<Integer> keysRe = new ArrayList<Integer>();
		List<Integer> valuesRe = new ArrayList<Integer>();
		for(int i = 0; i < bys.size(); i++){
			if(bys.get(i) >=0){
				keysRe.add(bys.get(i) & 3);
				valuesRe.add(bys.get(i) >>2);
			}else{
				int by1 = (bys.get(i++) & 127) * 32;
				int by2 = bys.get(i) >> 2;
				keysRe.add(bys.get(i) & 3);
				valuesRe.add(by1 + by2);
			}
		}
		
//		System.out.println();
//		for(int i = 0; i < 20; i++){
//			System.out.print(keysRe.get(i)+" ");
//		}
//		System.out.println();
//		for(int i = 0; i < 20; i++){
//			System.out.print(valuesRe.get(i)+" ");
//		}
	}
	
	/**
	 * 对异常信息进行编码
	 * @return
	 * 每4位表示一个异常信息的值，高位1表示未结束，0表示结束了
	 */
	Byte[] exceptionRL(ReadsPreProcessResult reads){
		List<Byte> bys = new ArrayList<Byte>();
		List<Byte> byTemp = new ArrayList<Byte>();
		int xorNum = 0;
		for(int i = 0; i < reads.getReadsInfo().size(); i++) {
			List<String> exception = reads.getReadsInfo().get(i).getExceptionInfo();
			if (exception != null){
				for(String strEx : exception){
					for(int j = 0; j < strEx.length(); j++){
						if (j == strEx.length() - 1){
							xorNum = 0;
						}else{
							xorNum = 8;
						}
						switch(strEx.charAt(j)){
							case 'A':
								byTemp.add((byte)(ExceptionSYMBOL.A.ordinal() ^ xorNum));
								break;
							case 'C':
								byTemp.add((byte)(ExceptionSYMBOL.C.ordinal() ^ xorNum));
								break;
							case 'G':
								byTemp.add((byte)(ExceptionSYMBOL.G.ordinal() ^ xorNum));
								break;
							case 'T':
								byTemp.add((byte)(ExceptionSYMBOL.T.ordinal() ^ xorNum));
								break;
							case 'S':	//AI -S, CI -V, GI -H, TI -Y
								byTemp.add((byte)(ExceptionSYMBOL.AI.ordinal() ^ xorNum));
								break;
							case 'V':
								byTemp.add((byte)(ExceptionSYMBOL.CI.ordinal() ^ xorNum));
								break;
							case 'H':
								byTemp.add((byte)(ExceptionSYMBOL.GI.ordinal() ^ xorNum));
								break;
							case 'Y':
								byTemp.add((byte)(ExceptionSYMBOL.TI.ordinal() ^ xorNum));
								break;
							case 'D':
								byTemp.add((byte)(ExceptionSYMBOL.D.ordinal() ^ xorNum));
								break;
							default:
								System.out.println("...case Error...");
						}
					}//END for j
				}//END foreach
			}//END if
		}//END for i
		
		//将每两个byte拼凑在一起
		int by0 = 0;
		int by1 = 0;
		int index  = 0;
		for( ;index < byTemp.size() - 1; index++){
			by0 = byTemp.get(index++);
			by1 = byTemp.get(index);
			bys.add((byte)((by0 << 4) + by1));
		}
		by0 = byTemp.get(index++);
		if(index < byTemp.size()){
			by1 = byTemp.get(index);
		}else {
			by1 = 0;
		}
		bys.add((byte)(by0 << 4 + by1));
		
		System.out.println();
		for(int i = 0; i< 50; i++){
			if (null != reads.getReadsInfo().get(i).getExceptionInfo()){
				System.out.println(reads.getReadsInfo().get(i).getExceptionInfo());
			}
		}
		
		System.out.println();
		//输出一下结果看一下
		for(int i = 0; i < 300; i++){
			String s2 = String.format("%8s", Integer.toBinaryString(bys.get(i) & 0xFF)).replace(' ', '0');
			System.out.print(s2+" ");
		}
		
		
		// 反解回来，就可以验证其正确性了
		//+ 经过验证，这里的正确性是没有问题的
		//TODO 对这里是有疑问的,这里怎么验证过去的,明明可能出现的情况个数是大于8种的,怎么实现表示的?我觉得这里有问题,问题一直没有暴露出来的原因是处理D的情形过少
		reExceptionRL(bys);
		return bys.toArray(new Byte[bys.size()]);
	}
	
	
	//反解一下对异常信息两个拼凑的效果
	static void reExceptionRL(List<Byte> bys){
		List<String> exceptions = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for(Byte by : bys){
			byte bt1 = (byte)(by >> 4);
			byte bt2 = (byte)(by & 15);
			//为负， 则还需要继续处理，否则直接就处理完成了
			
			switch(bt1 & 7){
				case 0:
					sb.append('A');
					break;
				case 1:
					sb.append('C');
					break;
				case 2:
					sb.append('G');
					break;
				case 3:
					sb.append('T');
					break;
				case 4:
					sb.append('S');
					break;
				case 5:
					sb.append('V');
					break;
				case 6:
					sb.append('H');
					break;
				case 7:
					sb.append('Y');
					break;
				case 8:
					sb.append('D');
					System.out.println(".......impossible....");
					break;
				default:
					System.out.println("reException case ERROR...");
			}
			if ( (bt1 & 8) == 0){
				exceptions.add(sb.toString());
				sb = new StringBuilder();
			}
			
			switch(bt2 & 7){
			case 0:
				sb.append('A');
				break;
			case 1:
				sb.append('C');
				break;
			case 2:
				sb.append('G');
				break;
			case 3:
				sb.append('T');
				break;
			case 4:
				sb.append('S');
				break;
			case 5:
				sb.append('V');
				break;
			case 6:
				sb.append('H');
				break;
			case 7:
				sb.append('Y');
				break;
			case 8:
				sb.append('D');
				break;
			default:
				System.out.println("reException case ERROR...");
			}
			
			if((bt2 & 8) == 0){
				exceptions.add(sb.toString());
				sb = new StringBuilder();
			}
			
		}//END FOR
		
		System.out.println();
		for(int i = 0; i< 20; i++){
			System.out.println(exceptions.get(i)+" ");
		}
	}

	/**
	 * 对水平横向编码部分游程处理
	 * @return
	 */
	Byte[] horizonRL(List<ReadsHorizonModel> horizonLists){
		//设置一个baseIndex。这样方便处理，就用第一个元素的值作为base吧
		List<Byte> bys = new ArrayList<Byte>();
		int alignmentStart = 0, alignmentEnd = 0, flag =0, distance = 0;
		int start0 = 0, start1 = 0;
		for(int i = 0; i < horizonLists.size(); i++){
			alignmentStart = horizonLists.get(i).getAlignmentStart();
			alignmentEnd = horizonLists.get(i).getAlignmentEnd() - alignmentStart;
			flag = horizonLists.get(i).getFlag();
			distance = horizonLists.get(i).getMateAlignmentDistance();
			//对值编码处理一下
			start0 = alignmentStart >> 8;
			start1 = alignmentStart % 256;
			bys.add((byte)start0);
			bys.add((byte)start1);
			bys.add((byte)alignmentEnd);
			bys.add((byte)flag);
			bys.add((byte)distance);
		}
		System.out.println("horionRL bys size\t"+bys.size());
		return bys.toArray(new Byte[bys.size()]);
	}
	
}








