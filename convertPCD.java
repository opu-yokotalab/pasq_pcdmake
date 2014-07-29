import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.Arrays;

public class convertPCD {
	
	private static NodeList panoList;
	private static Document document;
	
	public static void main(String[] args) {
		try {
			String fileName = "international.xml";
			
			double f_base = 85;
			double f_next = 105;
			double pan = 0;
			double tilt = 0;
			
			// ドキュメントビルダーファクトリを生成
			DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
			// ドキュメントビルダーを生成
			DocumentBuilder builder = dbfactory.newDocumentBuilder();
			// パースを実行してDocumentオブジェクトを取得
			document = builder.parse(new File(fileName));
			// ルート要素を取得（タグ名：site）
			Element root = document.getDocumentElement();
			
			// Panoramas要素のリストを取得
			panoList = root.getElementsByTagName("Panorama");
			
			
			/*
			 * パノラマ画像間の距離・方位のリスト化
			 */
			
			// Panorama要素の数だけループ
			for (int i = 0; i < panoList.getLength(); i++) {		
				String arDist[][] = new String[panoList.getLength()][3];
				
				// 現在のパノラマ画像の位置情報
				Element panoramaElement = (Element)panoList.item(i);
				double lng1 = Double.parseDouble(attributeValue(panoramaElement, "coords", "lng")) * Math.PI / 180;
				double lat1 = Double.parseDouble(attributeValue(panoramaElement, "coords", "lat")) * Math.PI / 180;
				
				for (int j = 0; j < panoList.getLength(); j++){ //現在のパノラマ画像も計算対象に含んだ処理
					Element targetElement = (Element)panoList.item(j);
					// 計算対象のパノラマ画像の位置情報
					double lng2 = Double.parseDouble(attributeValue(targetElement, "coords", "lng")) * Math.PI / 180;
					double lat2 = Double.parseDouble(attributeValue(targetElement, "coords", "lat")) * Math.PI / 180;
					
					double dx = 6378137 * (lng2 - lng1) * Math.cos(lat1);
					double dy = 6378137 * (lat2 - lat1);
					
					double dist = Math.sqrt(dx * dx + dy * dy);
					double dir = Math.atan2(dx,dy);
					dir = dir * 180 / Math.PI; //ラジアンから360度法へ
					dir = (360+dir)%360; //0-360度へ
					
					// id、距離、方位をリスト化
					arDist[j][0] = attributeValue(targetElement, "panoid");
					arDist[j][1] = Double.toString(dist);
					arDist[j][2] = Double.toString(dir);
				}
				
		        TheComparator comparator = new TheComparator();
		        // 2番目の項目(距離)でソート
		        comparator.setIndex(1);
		        Arrays.sort(arDist, comparator);
		        
		        
		        /*
		         * 切替範囲の決定
		         */
		        
		    	String arLink[] = new String[360];
		    	for(int j = 1; j<arDist.length; j++){ // 現在のパノラマ画像も含むため j=1 から
		    		if((arLink[(int)Math.floor(Double.parseDouble(arDist[j][2]))] == null)){
		    			
		    			// 切替範囲の割当計算(要変更)
			    		double tmp = Math.pow(0.95, Double.parseDouble(arDist[j][1])) * 100 + 20;
			    		int offd = (int)Math.floor(tmp / 2);
			    		
			    		int sd = (int)Math.floor((360 + Double.parseDouble(arDist[j][2]) - offd) % 360);
			    		int ed = (int)Math.floor((360 + Double.parseDouble(arDist[j][2]) + offd) % 360);
			    		if(sd > ed){ // 0度をまたぐとき
			    			for(int k=sd; k<=359; k++){
			    				if(arLink[k] == null) arLink[k] = arDist[j][0];
			    			}
			    			for(int k=0; k<=ed; k++){
			    				if(arLink[k] == null) arLink[k] = arDist[j][0];
			    			}
			    		}else{
			    			for(int k=sd; k<=ed; k++){
			    				if(arLink[k] == null) arLink[k] = arDist[j][0];
			    			}
			    		}
		    		}
		    	}
		    	
		    	
		    	/*
		    	 * 切替範囲の抽出および書出
		    	 */
		    	
		    	// 近傍情報要素の作成
		    	Element chpanosElement = document.createElement("chpanos");
		    	panoramaElement.appendChild(chpanosElement);
		    	panoramaElement.appendChild(document.createTextNode("\n"));
		    	chpanosElement.appendChild(document.createTextNode("\n"));
		    	
		    	// 初期位置
		    	int startDir = 0;
		    	int k = 0;
		    	Element chpanoElement;
		    	
				// 切替範囲が0°をまたぐ場合の初期処理
				if((arLink[0] != null) && (arLink[0].equals(arLink[359]))){
					// 初期位置および切替範囲の開始位置
					startDir = 359;
					while(arLink[startDir].equals(arLink[startDir-1])){
						startDir--;
					}
					
					// 切替範囲の修了位置
					k = startDir;
					while((arLink[(k+1)%360] != null) && (arLink[k].equals(arLink[(k+1)%360]))){
						k = (k + 1) % 360;
					}
					
					// 切替範囲の設定 (startDir -> k)
					chpanoElement = document.createElement("chpano");
					chpanosElement.appendChild(chpanoElement);
					chpanosElement.appendChild(document.createTextNode("\n"));
					
					chpanoElement.setAttribute("panoid", arLink[k]);
					chpanoElement.appendChild(createEleAtt("range", "start", Double.toString(startDir), "end", Double.toString(k)));
					chpanoElement.appendChild(createEleAtt("fov", "base", Double.toString(f_base), "next", Double.toString(f_next)));
					chpanoElement.appendChild(createEleAtt("correct", "pan", Double.toString(pan), "tilt", Double.toString(tilt)));
					
					k = (k + 1) % 360;
				}
				
				// その他の切替範囲の処理
				do{
					if(arLink[k] != null){
						int tmp = k;
						while(arLink[k].equals(arLink[(k+1)%360])){
							k = (k + 1) % 360;
						}
						
						// 切替範囲の設定 (tmp -> k)
						chpanoElement = document.createElement("chpano");
						chpanosElement.appendChild(chpanoElement);
						chpanosElement.appendChild(document.createTextNode("\n"));
						
						chpanoElement.setAttribute("panoid", arLink[k]);
						chpanoElement.appendChild(createEleAtt("range", "start", Double.toString(tmp), "end", Double.toString(k)));
						chpanoElement.appendChild(createEleAtt("fov", "base", Double.toString(f_base), "next", Double.toString(f_next)));
						chpanoElement.appendChild(createEleAtt("correct", "pan", Double.toString(pan), "tilt", Double.toString(tilt)));
					}
					k = (k + 1) % 360;
				}while(k != startDir);
				
				// パノラマ画像を20個処理するごとにメッセージ(確認用)
				if(((i + 1) % 20) == 0) System.out.println("Calculate No." + (i + 1));
		    	
			}
			
			
			/*
			 * DOMの出力
			 */
			
			TransformerFactory tfactory = TransformerFactory.newInstance(); 
			Transformer transformer = tfactory.newTransformer(); 
			File outfile = new File("cnv_" + fileName);
			transformer.transform(new DOMSource(root), new StreamResult(outfile)); 
			
			// 処理終了のメッセージ(確認用)
			System.out.println("Complete.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// target要素の属性値を返す
	private static String attributeValue(Element target, String attName){
		String s = target.getAttribute(attName);
		return s;
	}
	
	// target要素の子要素の属性値を返す
	private static String attributeValue(Element target, String eleName, String attName){
		Element e = (Element)target.getElementsByTagName(eleName).item(0);
		String s = e.getAttribute(attName);
		return s;
	}
	
	// 属性を2つ持つ要素を生成する
	private static Element createEleAtt(String eleName, String attName1, String attValue1, String attName2, String attValue2){
		Element e = document.createElement(eleName);
		e.setAttribute(attName1, attValue1);
		e.setAttribute(attName2, attValue2);
		return e;
	}
}
