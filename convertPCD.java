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
			
			// �h�L�������g�r���_�[�t�@�N�g���𐶐�
			DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
			// �h�L�������g�r���_�[�𐶐�
			DocumentBuilder builder = dbfactory.newDocumentBuilder();
			// �p�[�X�����s����Document�I�u�W�F�N�g���擾
			document = builder.parse(new File(fileName));
			// ���[�g�v�f���擾�i�^�O���Fsite�j
			Element root = document.getDocumentElement();
			
			// Panoramas�v�f�̃��X�g���擾
			panoList = root.getElementsByTagName("Panorama");
			
			
			/*
			 * �p�m���}�摜�Ԃ̋����E���ʂ̃��X�g��
			 */
			
			// Panorama�v�f�̐��������[�v
			for (int i = 0; i < panoList.getLength(); i++) {		
				String arDist[][] = new String[panoList.getLength()][3];
				
				// ���݂̃p�m���}�摜�̈ʒu���
				Element panoramaElement = (Element)panoList.item(i);
				double lng1 = Double.parseDouble(attributeValue(panoramaElement, "coords", "lng")) * Math.PI / 180;
				double lat1 = Double.parseDouble(attributeValue(panoramaElement, "coords", "lat")) * Math.PI / 180;
				
				for (int j = 0; j < panoList.getLength(); j++){ //���݂̃p�m���}�摜���v�Z�ΏۂɊ܂񂾏���
					Element targetElement = (Element)panoList.item(j);
					// �v�Z�Ώۂ̃p�m���}�摜�̈ʒu���
					double lng2 = Double.parseDouble(attributeValue(targetElement, "coords", "lng")) * Math.PI / 180;
					double lat2 = Double.parseDouble(attributeValue(targetElement, "coords", "lat")) * Math.PI / 180;
					
					double dx = 6378137 * (lng2 - lng1) * Math.cos(lat1);
					double dy = 6378137 * (lat2 - lat1);
					
					double dist = Math.sqrt(dx * dx + dy * dy);
					double dir = Math.atan2(dx,dy);
					dir = dir * 180 / Math.PI; //���W�A������360�x�@��
					dir = (360+dir)%360; //0-360�x��
					
					// id�A�����A���ʂ����X�g��
					arDist[j][0] = attributeValue(targetElement, "panoid");
					arDist[j][1] = Double.toString(dist);
					arDist[j][2] = Double.toString(dir);
				}
				
		        TheComparator comparator = new TheComparator();
		        // 2�Ԗڂ̍���(����)�Ń\�[�g
		        comparator.setIndex(1);
		        Arrays.sort(arDist, comparator);
		        
		        
		        /*
		         * �͈ؑ֔͂̌���
		         */
		        
		    	String arLink[] = new String[360];
		    	for(int j = 1; j<arDist.length; j++){ // ���݂̃p�m���}�摜���܂ނ��� j=1 ����
		    		if((arLink[(int)Math.floor(Double.parseDouble(arDist[j][2]))] == null)){
		    			
		    			// �͈ؑ֔͂̊����v�Z(�v�ύX)
			    		double tmp = Math.pow(0.95, Double.parseDouble(arDist[j][1])) * 100 + 20;
			    		int offd = (int)Math.floor(tmp / 2);
			    		
			    		int sd = (int)Math.floor((360 + Double.parseDouble(arDist[j][2]) - offd) % 360);
			    		int ed = (int)Math.floor((360 + Double.parseDouble(arDist[j][2]) + offd) % 360);
			    		if(sd > ed){ // 0�x���܂����Ƃ�
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
		    	 * �͈ؑ֔͂̒��o����я��o
		    	 */
		    	
		    	// �ߖT���v�f�̍쐬
		    	Element chpanosElement = document.createElement("chpanos");
		    	panoramaElement.appendChild(chpanosElement);
		    	panoramaElement.appendChild(document.createTextNode("\n"));
		    	chpanosElement.appendChild(document.createTextNode("\n"));
		    	
		    	// �����ʒu
		    	int startDir = 0;
		    	int k = 0;
		    	Element chpanoElement;
		    	
				// �͈ؑ֔͂�0�����܂����ꍇ�̏�������
				if((arLink[0] != null) && (arLink[0].equals(arLink[359]))){
					// �����ʒu����ѐ͈ؑ֔͂̊J�n�ʒu
					startDir = 359;
					while(arLink[startDir].equals(arLink[startDir-1])){
						startDir--;
					}
					
					// �͈ؑ֔͂̏C���ʒu
					k = startDir;
					while((arLink[(k+1)%360] != null) && (arLink[k].equals(arLink[(k+1)%360]))){
						k = (k + 1) % 360;
					}
					
					// �͈ؑ֔͂̐ݒ� (startDir -> k)
					chpanoElement = document.createElement("chpano");
					chpanosElement.appendChild(chpanoElement);
					chpanosElement.appendChild(document.createTextNode("\n"));
					
					chpanoElement.setAttribute("panoid", arLink[k]);
					chpanoElement.appendChild(createEleAtt("range", "start", Double.toString(startDir), "end", Double.toString(k)));
					chpanoElement.appendChild(createEleAtt("fov", "base", Double.toString(f_base), "next", Double.toString(f_next)));
					chpanoElement.appendChild(createEleAtt("correct", "pan", Double.toString(pan), "tilt", Double.toString(tilt)));
					
					k = (k + 1) % 360;
				}
				
				// ���̑��͈̐ؑ֔͂̏���
				do{
					if(arLink[k] != null){
						int tmp = k;
						while(arLink[k].equals(arLink[(k+1)%360])){
							k = (k + 1) % 360;
						}
						
						// �͈ؑ֔͂̐ݒ� (tmp -> k)
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
				
				// �p�m���}�摜��20�������邲�ƂɃ��b�Z�[�W(�m�F�p)
				if(((i + 1) % 20) == 0) System.out.println("Calculate No." + (i + 1));
		    	
			}
			
			
			/*
			 * DOM�̏o��
			 */
			
			TransformerFactory tfactory = TransformerFactory.newInstance(); 
			Transformer transformer = tfactory.newTransformer(); 
			File outfile = new File("cnv_" + fileName);
			transformer.transform(new DOMSource(root), new StreamResult(outfile)); 
			
			// �����I���̃��b�Z�[�W(�m�F�p)
			System.out.println("Complete.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// target�v�f�̑����l��Ԃ�
	private static String attributeValue(Element target, String attName){
		String s = target.getAttribute(attName);
		return s;
	}
	
	// target�v�f�̎q�v�f�̑����l��Ԃ�
	private static String attributeValue(Element target, String eleName, String attName){
		Element e = (Element)target.getElementsByTagName(eleName).item(0);
		String s = e.getAttribute(attName);
		return s;
	}
	
	// ������2���v�f�𐶐�����
	private static Element createEleAtt(String eleName, String attName1, String attValue1, String attName2, String attValue2){
		Element e = document.createElement(eleName);
		e.setAttribute(attName1, attValue1);
		e.setAttribute(attName2, attValue2);
		return e;
	}
}
