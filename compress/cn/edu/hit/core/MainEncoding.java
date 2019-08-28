package cn.edu.hit.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.edu.hit.model.QualEnum;
import cn.edu.hit.model.VerticalEncodeResult;
import cn.edu.hit.util.Huffman2;

public class MainEncoding {
	public static void main(String[] args){
		
		long lstart1 = System.currentTimeMillis(); 
		
		MainEncoding encoding = new MainEncoding();
		VerticalEncoding vE = new VerticalEncoding();
		// 异常信息列表，测试可以完整通过
		byte[] exBy = encoding.EncodeExceptionList(vE);		//liyang:对于异常信息的处理是采用哈弗曼编码
		
		// 异常信息的质量分数，测试可以完整通过
		byte[] exQual = encoding.EncodeExceptionQual(vE);		//liyang：对于异常值的处理
		
		byte[] pbwtQual = encoding.EncodePbwtQual(vE);		//liyang:对于质量分数的处理
		
		// encodePBWT测试可以完整通过
		byte[] pbwt = encoding.EncodePbwt(vE);				//liyang：对于正常信息的处理
		
		System.out.println("The encoding is end");
		//进行一下写文件的操作
		String fileDestEx = "/home/liyang/Document/compress/output/50X/50X-all/exBy";
		String fileDestExQual ="/home/liyang/Document/compress/output/50X/50X-all/exQual";
		String fileDestPbwtQual = "/home/liyang/Document/compress/output/50X/50X-all/pbwtQual";
		String fileDestPbwt = "/home/liyang/Document/compress/output/50X/50X-all/pbwt";
//		String fileDestEx = "/home/yangli/Documents/compress/exBy1";
//		String fileDestExQual ="/home/yangli/Documents/compress/exQual1";
//		String fileDestPbwtQual = "/home/yangli/Documents/compress/pbwtQual1";
//		String fileDestPbwt = "/home/yangli/Documents/compress/pbwt1";
		try {
            Path path = Paths.get(fileDestEx);
            Files.write(path, exBy);
            path = Paths.get(fileDestExQual);
            Files.write(path, exQual);
            path = Paths.get(fileDestPbwtQual);
            Files.write(path, pbwtQual);
            path = Paths.get(fileDestPbwt);
//            Path path = Paths.get(fileDestPbwt);
            Files.write(path, pbwt);
            	
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		System.out.println("The writing is end");
		
		long lend1 = System.currentTimeMillis();
		long time = (lend1 - lstart1);
		System.out.println(time/1000);
		System.out.println("Using time："+time/1000/60/60+" h:"+time/1000/60%60+" m:"+time/1000%60+" s");
	}
	
	
	public byte[] EncodeExceptionList(VerticalEncoding vE){
		// get ExveptionList Data
//		VerticalEncoding vE = new VerticalEncoding();
		ArrayList<ArrayList<String>>  exceptionList = vE.getExceptionListData();
		
		// define huffman tree
		Huffman2 huffman = new Huffman2();
		String rateText = "AAACCCTTTGGG||DN";	//crazy：暂时把N的情况删除
//		String rawText = "ACCCGGGTTTTTTT||||AA";
		huffman.handleRate(rateText);	//liyang:建立哈弗曼树
		
//		System.out.println(exceptionList.size());
		// generate raw text, | 是间隔符号
		StringBuilder rawText = new StringBuilder();
		for(ArrayList<String> str: exceptionList){	//liyang:给每个异常值之间加上一个间隔符号
			rawText.append(str.get(0)+"|");
		}
		
	
//		//TODO:0527 看一下异常信息
		System.out.println("rawText.length:\t"+rawText.toString().length());
//		System.out.println(rawText.toString());
		
		// encoding Text
		// crazy:既然哈弗曼编码慢，那我们就直接不建立树，不去搜，直接if进行加，时间复杂度就是O（n）量级的
//		String encodedResult = Huffman2.encodeText(rawText.toString());			//liyang:完成了编码操作
		String[] str = Huffman2.encodeText2(rawText.toString());
//		char code;
//		String encodedResult = "";
//		for(int i=0;i<rawText.length();i++)
//		{
//			code = rawText.charAt(i);
//			if(code =='A')
//			{
//				encodedResult = encodedResult.concat("111");
//			}
//			else if(code =='T')
//			{
//				encodedResult = encodedResult.concat("110");
//			}
//			else if(code =='C')
//			{
//				encodedResult = encodedResult.concat("10");
//			}
//			else if(code =='G')
//			{
//				encodedResult = encodedResult.concat("00");
//			}
//			else if(code =='D')
//			{
//				encodedResult = encodedResult.concat("010");
//			}
//			else
//			{
//				encodedResult = encodedResult.concat("011");
//			}
//		}
		
		
		// TODO 这里临时性地进行强制填充
		/*System.out.println(encodedResult.length());
		while(encodedResult.length() %16 != 0){
			encodedResult += "1";
		}*/
		
		
//		System.out.println("encodedResult.length（the number of 1 or 0）:\t"+ encodedResult.length()+"\t");
		// store the result
		// 把正则表达式引入进来,然后进行处理,并切分,这样个数也会变得非常显然了.代码质量直接决定效率,生命.工程手法上面可以补充很多
//		String[] str =encodedResult.split("(?<=\\G.{16})");	//liyang:暂时理解为16个截取一下
		System.out.println("str.length:\t" + str.length);	//crazy:这里注意str.length进行过修改
		int length=0;	//crazy:用于记录str真正的长度；
		for(int i=0;i<str.length;i++)
		{
			if(str[i]=="")
			{
				length = i+1;
				break;
			}
		}
		System.out.println("str.length:\t" + length);
		ByteBuffer bytes = ByteBuffer.allocate(str.length*2);		//liyang:开辟双倍内存，short占用两个字节
		for(int i = 0; i < length - 1; i++){
//			System.out.println("bytes.postiton:\t"+bytes.position());
			bytes.putShort((short)(Integer.parseInt(str[i],2)));
		}
		//最后一个位置再拼凑一下然后放进去
		String strTemp = str[length-1];
		if(strTemp.length() < 16){
			for(int num = 16 - strTemp.length(); num  > 0; num-- ){
				strTemp +="1";
			}
		}
		bytes.putShort((short)(Integer.parseInt(strTemp, 2)));
		byte[] array = bytes.array();
		
		return array;
		/*
		//这里写一个写文件的操作就完整了,这样的话,
		String fileDest = "/home/rivers/riversdoc/compress/byException";
		try {
            Path path = Paths.get(fileDest);
            Files.write(path, array);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		try{
			TimeUnit.SECONDS.sleep(1);
		}catch(Exception e){}
		
		// 然后读文件,从文件里面读出内容来
		byte[] data = null;
		try{
			Path path = Paths.get(fileDest);
			data = Files.readAllBytes(path);
		}catch(IOException e){
			e.printStackTrace();
		}
		System.out.println("array.length:\t"+array.length);
		//现在把byte[]反转回来,这个一次性反转非常简单,但是处理数据却变得异常麻烦,最高位的符号位置需要丢掉
		String byte2EncodeResult = "";
		for(int i =0; i < array.length-2; i++){
			byte2EncodeResult += String.format("%8s", Integer.toBinaryString(data[i] & 0xFF)).replace(' ', '0');
		}
		String strTail = String.format("%8s", Integer.toBinaryString(data[data.length-2] & 0xFF)).replace(' ', '0')
							+ String.format("%8s", Integer.toBinaryString(data[data.length-1] & 0xFF)).replace(' ', '0');
		String[] res = strTail.split("000111");
		byte2EncodeResult += res[0];
		// 这里用正则表达式处理掉,效果会更好
		//最后一个结果拿出来单独处理一下就可以
		// decoding Text
		String decodedResult = huffman.decodeText(byte2EncodeResult);
//        vE.PrintResult();
//		System.out.println(decodedResult);
		System.out.print(decodedResult.replace("|", "\n"));*/
		
		
	}

	
	public byte[] EncodeExceptionQual(VerticalEncoding vE){
//		VerticalEncoding vE = new VerticalEncoding();	
		ArrayList<ArrayList<String>>  exceptionQual = vE.getExceptionQual();
//		vE.PrintResult();
		// 先对异常所在的区间做一次预处理，才好知道真正的压缩效果是否有更大改进空间
		/**
		 * 	0, 1, 6, 6, 6, 6, 6, 6, 6, 6, 15, 15, 15, 15, 15, 15, 15, 15, 15,
			15, 22, 22, 22, 22, 22, 27, 27, 27, 27, 27, 33, 33, 33, 33, 33, 37,
			37, 37, 37, 37, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
			40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
			40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
			40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
			40, 40, 40, 40, 40, 40 
		 */
		
		// 数据预处理阶段，其中6,15,22, 27,33,37,40分别用占位符表示，这里可以创建一个数据结构
		ArrayList<ArrayList<Integer>> exceptionQualProc = new ArrayList<ArrayList<Integer>>();
		for(ArrayList<String> qualList : exceptionQual){
			ArrayList<Integer> qualProc = new ArrayList<Integer>();
			for(String qual : qualList){
				char[] quals = qual.toCharArray();
				for(Character ch : quals){
					if(ch-33 < 10){
						qualProc.add(QualEnum.six.ordinal());
					}else if(ch-33 < 20){
						qualProc.add(QualEnum.fifteen.ordinal());
					}else if(ch-33 < 25){
						qualProc.add(QualEnum.twenty.ordinal());
					}else if(ch-33 < 30){
						qualProc.add(QualEnum.thirty_three.ordinal());
					}else if(ch -33 < 40){
						qualProc.add(QualEnum.thirty_seven.ordinal());
					}else{
						qualProc.add(QualEnum.forty.ordinal());
					}
				}
			}
			exceptionQualProc.add(qualProc);
		}
		
		// 打印输出一下处理后的结果
		/*for(ArrayList<Integer> list : exceptionQualProc){
			System.out.println(list.toString());
		}*/
		// 编码时，按照2^5(个数), 2^3(值)的顺序去编码解决一下,其中值0代表结束
		// 先统计一下Key Value的个数
		ArrayList<Integer> keys = new ArrayList<Integer>();
		ArrayList<Integer> values = new ArrayList<Integer>();
		for(ArrayList<Integer> list: exceptionQualProc){
			int pre = -1;
			int cur = -1;
			int count = 0;
			for(int i = 0; i < list.size(); i++){
				cur = list.get(i);
				if(i !=0 && cur != pre){
					keys.add(pre);
					values.add(count);
					count = 0;
				}
				if(i == list.size() - 1){
					// 处理到最后一个元素了
					keys.add(cur);
					values.add(++count);
					//结尾增加一个标识符
					keys.add(QualEnum.separator.ordinal());
					values.add(0);
				}
				
				count ++;
				pre = cur;
			}
		}
		
		//输出一下处理后的结果,结果验证是正确的
//		for(int i = 0; i < keys.size(); i++){
//			System.out.println(keys.get(i) + "\t" + values.get(i));
//		}
//		System.out.println(keys.size());
//		
		//构造成Byte数组
		ByteBuffer eQual = ByteBuffer.allocate(keys.size());
		for(int i = 0; i < keys.size(); i++){
			eQual.put((byte)(keys.get(i)*32 + values.get(i) - 128));
		}
		byte[] eQual2By = eQual.array();
		System.out.println("exception length:"+eQual2By.length);
		return eQual2By;
	}
	
	
	public byte[] EncodePbwtQual(VerticalEncoding vE){
//		VerticalEncoding vE = new VerticalEncoding();
		ArrayList<ArrayList<Character>>  exceptionQual = vE.getPbwtQual();
//		vE.PrintResult();
		
		ArrayList<ArrayList<Integer>> pbwtQualProc = new ArrayList<ArrayList<Integer>>();
		for(ArrayList<Character> qualList : exceptionQual){
			ArrayList<Integer> qualProc = new ArrayList<Integer>();
			for(Character ch : qualList){
					if(ch-33 < 10){
						qualProc.add(QualEnum.six.ordinal());	//1
					}else if(ch-33 < 20){
						qualProc.add(QualEnum.fifteen.ordinal());	//2
					}else if(ch-33 < 25){
						qualProc.add(QualEnum.twenty.ordinal());	//3
					}else if(ch-33 < 30){
						qualProc.add(QualEnum.thirty_three.ordinal());	//5
					}else if(ch -33 < 40){
						qualProc.add(QualEnum.thirty_seven.ordinal());		//6
					}else{
						qualProc.add(QualEnum.forty.ordinal());			//7
					}
			}
			pbwtQualProc.add(qualProc);
		}
		
//		System.out.println(QualEnum.six.ordinal());
//		System.out.println(QualEnum.fifteen.ordinal());
//		System.out.println(QualEnum.twenty.ordinal());
//		System.out.println(QualEnum.thirty_three.ordinal());
//		System.out.println(QualEnum.thirty_seven.ordinal());
//		System.out.println(QualEnum.forty.ordinal());
		// 打印输出一下处理后的结果
		/*for(ArrayList<Integer> list : pbwtQualProc){
			System.out.println(list.toString());
		}*/
		
		// 这里处理的时候，超过2^5要进行一下截断处理
		ArrayList<Integer> keys = new ArrayList<Integer>();
		ArrayList<Integer> values = new ArrayList<Integer>();
		for(ArrayList<Integer> list: pbwtQualProc){
			int pre = -1;
			int cur = -1;
			int count = 0;
			for(int i = 0; i < list.size(); i++){
				cur = list.get(i);
				//改变之后，这里的32处理策略也不需要的了
				/*if(count == 32){
					keys.add(pre);
					values.add(count);
					count = 0;
				}*/
				if(i !=0 && cur != pre){
					keys.add(pre);
					values.add(count);
					count = 0;
				}
				if(i == list.size() - 1){
					// 处理到最后一个元素了
					keys.add(cur);
					values.add(++count);
					//结尾增加一个标识符
					keys.add(QualEnum.separator.ordinal());
					values.add(0);
				}
				
				count ++;
				pre = cur;
			}
		}
		//输出一下处理后的结果
//		for(int i = 0; i < keys.size()/2; i++){
//			System.out.println(keys.get(i) + "\t" + values.get(i));
//		}
		//输出一下PBWTQUAL的大小
//		System.out.println("pbwtQual size :\t"+keys.size());
		//这里改变一下处理策略，按照原有的处理策略，这里只需要保留Keys值就足够了，结合PBWT就能找到Value,那这里就需要拼凑了
		// + 为了快捷拼凑，这里采用3×8的处理策略处理
		//构造成Byte数组
		ByteBuffer pbwtQual = ByteBuffer.allocate(keys.size()/8 * 3 + 8);
		for(int i = 0; i < keys.size()-8; i+=8){
			byte by1 = (byte)(keys.get(i)*32 + keys.get(i+1)*4 + keys.get(i+2)/2 -128);
			byte by2 = (byte)(keys.get(i+2)%2 * 64 + keys.get(i+3)* 16 + keys.get(i+4)*2 + keys.get(i+5)/4 - 128);
			byte by3 = (byte)(keys.get(i+5)%2 * 128 + keys.get(i+6)*8 + keys.get(i+7)-128 );
			pbwtQual.put(by1);
			pbwtQual.put(by2);
			pbwtQual.put(by3);
		}
		byte[] pbwtQual2By = pbwtQual.array();
		return pbwtQual2By;
	}
	
	public byte[] EncodePbwt(VerticalEncoding vE){
//		VerticalEncoding vE = new VerticalEncoding();
		VerticalEncodeResult result = vE.getVerticalEncodeREsult();		
		ArrayList<ArrayList<Integer>> listsPBWT = result.getListsPBWT();
		System.out.println(listsPBWT.size());
		
		//TODO:0527
//		for(ArrayList<Integer> pbwt : listsPBWT){
//			System.out.println(pbwt.toString());
//		}
		
		// 这部分想用正则来实现,但是目前没有实现思路,先用循环来实现一下吧
		// 参照之前的思路,先统计,再处理,终止符号也要记录一下(关于统计这部分的优化工作,稍后再开展)
		List<Integer> keys = new ArrayList<Integer>();
		List<Integer> values = new ArrayList<Integer>();
		//这里统计一下0、1、3的个数，然后进行游程编码
		int countZero=0, countOne = 0, countThree = 0;
		int preVal = -1;
		int curVal = -1;
		
		for(ArrayList<Integer> listPbwt : listsPBWT){	//liyang:横向进行
			//TODO; 0527
//			System.out.println(listPbwt);
			countZero = 0;
			countOne = 0;
			countThree = 0;
			for(int i = 0; i < listPbwt.size() ; i++){	//liyang:纵向进行
				curVal = listPbwt.get(i);
				if(i != 0 && curVal != preVal){	//把i为0的起始条件排除掉，因为其实位置之前没有preVal
					if (preVal == 0) {
						keys.add(0);
						values.add(countZero);
					}else if (preVal == 1){
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
				
				preVal = curVal;		//liyang:因为是起始位置，这样就会默认计数为1
				if(curVal == 0){
					countZero ++;
				}else if(curVal == 1){
					countOne ++;
				}else if (curVal == 3){
					countThree ++;
				}else{
					System.out.println("readsRL count Error....");
				}
				if(i == listPbwt.size() - 1){	//liyang：处理最后一个元素，同时也是退出这里垂直编码
					//处理到最后一个元素了
					if(curVal == 0){
						keys.add(0);
						values.add(countZero);
					}else if (curVal == 1){
						keys.add(1);
						values.add(countOne);
					}else{
						keys.add(3);
						values.add(countThree);
					}
					// 同时加入结束的分隔符
					//TODO 暂作为间隔符使用,在这里不会有2出现，用2来表示单条listpbwt的终止
					keys.add(2);
					values.add(1);
				}
			}
		}
		//结果输出一下,最终验证就是能还原回去就没有问题
		/*for(int i = 0; i < keys.size(); i++){
			System.out.println(keys.get(i) + "\t" + values.get(i));
		}*/
		// 0-1游程编码,把上面的统计结果进行编码,分别进行.key是2bit存储,value是1byte存储
		//+ 对于异常的情形，1Byte为11111111,后面连续取2Byte值作为结果
		//+ 所有结果对-128做一次差值处理，存储起来就会节约很多
		ArrayList<Byte> bVals = new ArrayList<Byte>();
		for(int i = 0; i < values.size(); i++){
			int val = values.get(i) - 128;
			if(val >= 127){		//liyang:这里是处理超过128大小的长度的
				//变成3部分呢，第一部分11111111作为指示，第二、第三byte则是真正的值拼接在一起
				bVals.add((byte)(-1));
				bVals.add((byte)((val >> 8) & 0xff));
				bVals.add((byte)(val & 0xff));
			}else{
				bVals.add((byte)(val));
			}
		}
		ByteBuffer bytes = ByteBuffer.allocate(bVals.size());
		for(byte by : bVals){
			bytes.put(by);
		}
		byte[] value2by = bytes.array();
		System.out.println("values size:\t" + value2by.length);
		//把keys每两个一拼接完成一下，同时注意最后 的结束符
		ArrayList<Byte> bKeys = new ArrayList<Byte>();	
		//对keys进行填充，填充元素2,直到keys的个数可以被4整除.2作为一个可以界定的元素
		keys.add(2);
		if(keys.size() % 4 != 0){	//liyang:这里我们用4的原因是首先2bit存储一个key，然后是2个key整合为一个
			int num = (keys.size()/4 + 1) * 4 - keys.size();	//liyang:这里所做的事情就是检测一下我到底需要加上几个2
			while(num -- > 0){
				keys.add(2);
			}
		}	//liyang:当走出这个循环的时候表明已经可以被4整除了
		for(int i = 0; i < keys.size() - 3; i+=4){	//liyang:减三的原因是4个为一个整体，所以说只要检测到第一个就可以，后面的没有必要了
			int key = keys.get(i)*64 + keys.get(i+1)* 16 + keys.get(i+2)*4 + keys.get(i+3) - 128;
			bKeys.add((byte)(key));
		}
		ByteBuffer keyBytes = ByteBuffer.allocate(bKeys.size());
		for(byte by: bKeys){
			keyBytes.put(by);
		}
		byte[] key2by = keyBytes.array();
		
		
		System.out.println("keys size:\t"+key2by.length);
//		for(byte by: bKeys)		//liyang:检测一下key转换之后的值
//		{
//			System.out.println("the keys:"+by);
//		}
		  /*
		//调用一下反解回来
		DecodePbwt(key2by, value2by);*/
//		DecodePbwt(key2by, value2by);
		return key2by;
		
		
		
	}
	
	//试一下把这些结果还原回来，看看有没有问题,完全可逆的一个过程
	public void DecodePbwt(byte[] keys,byte[] values){
		ArrayList<Byte> bKeysFromByte = new ArrayList<Byte>();
		int key = 0;
		for(int i = 0; i < keys.length; i++){
			// 移位+取余 处理最方便
			key = keys[i] + 128;
			bKeysFromByte.add((byte) ((key >> 6) % 4 ));
			bKeysFromByte.add((byte) ((key >> 4) % 4 ));
			bKeysFromByte.add((byte) ((key >> 2) % 4 ));
			bKeysFromByte.add((byte) ((key >> 0) % 4 ));
		}
		//把结果输出一下，最后末端的2不处理也是可以的
		System.out.println("bKeysFromByte.size:\t"+bKeysFromByte.size());
		// vlaue的结果反解回来,先得到具体值，发现特殊再反解回来
		ArrayList<Integer> bValsFromByte = new ArrayList<Integer>();
		for(int i = 0; i < values.length; i++){
			if(values[i] !=127 ){
				bValsFromByte.add(values[i] + 128);
			}else{
				bValsFromByte.add(values[i+1]* 256 + values[i+2]);
				i+=2;
			}
		}
		System.out.println(bValsFromByte.size());
		
		//还原成一条条PBWT的串形式
		ArrayList<ArrayList<Integer>> reListsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> pbwt = new ArrayList<Integer>();
		int num = 0;
		int keyTemp = 0;
		for(int i = 0; i < bValsFromByte.size(); i++){
			if(bKeysFromByte.get(i) == 2){
				reListsPBWT.add(pbwt);
				pbwt = new ArrayList<Integer>();
			}else{
				num = bValsFromByte.get(i);
				keyTemp = bKeysFromByte.get(i);
				while(num -- > 0){
					pbwt.add(keyTemp);
				}
			}
		}
		for(ArrayList<Integer> list : reListsPBWT){
			System.out.println(list.toString());
		}
		// 可以成功还原回来，效果还非常好
		
	}
	

}
