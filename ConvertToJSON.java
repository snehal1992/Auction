import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ConvertToJSON {
	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory docBldrFactry = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBldrObj = docBldrFactry.newDocumentBuilder();
		Document document = docBldrObj.parse("convertTOJSON.xml");
		Element rootElement = document.getDocumentElement();
		NodeList ndList = rootElement.getChildNodes();
		if(ndList.getLength() == 1) {
			System.out.println((" {\" "+rootElement.getNodeName()+" \" : "+rootElement.getTextContent()+"}"));
		} else {
			System.out.println(" {\" "+rootElement.getNodeName()+" \":{"); 
			preorder(rootElement.getChildNodes());
			System.out.println("}");
			System.out.println("}");
		}
	}
	static void preorder(NodeList ndList) {
		int k = 0;
		Node current = null;
		for(int i = 1 ; i < ndList.getLength()-1; i += 2) {
			if(k==1){
				System.out.print(",");
			}
			current = ndList.item(i);
			dontWantToPrintArrayNameAgain(ndList,current,i);
			k = bracket(ndList,current,i,k);
			if(current.hasAttributes()){
				for(int j = 0;j<current.getAttributes().getLength();j++){
				String s = current.getAttributes().item(j).toString();
				String s1 [] = s.split("=");
				if(k==0){
					if(current.getChildNodes().getLength() <= 1){
						System.out.println("{\""+s1[0]+"\":"+s1[1]+"}");
					}else{
						System.out.println("\""+s1[0]+"\":"+s1[1]+",");
					}
				}else{
				if(j>1){
				System.out.println("{\""+s1[0]+"\":"+s1[1]+"},");
				}else{
					System.out.println("{\""+s1[0]+"\":"+s1[1]+"}");
				}
				}
			}
			}
			k = checkStatusOfCurrentNode(ndList,k,i,current);
			if(ndList.item(i).getChildNodes().getLength() > 1 ) {
				preorder(ndList.item(i).getChildNodes());
			}
		}
		if( k == 1) {
			System.out.println(" ],");
			k =0;
		} else {
			if(current.getChildNodes().getLength()!=1)
			System.out.println("}");
		}
	}
	static int  bracket(NodeList ndList, Node current, int i , int k) {
		if(i != ndList.getLength()-2) {// selection of bracket
			if(k == 0) {// no repetition of nodeName so decide which bracket to open with
				if(current.getNodeName() == ndList.item(i+2).getNodeName()) {
					System.out.println(" [  ");
					k = 1;
				} else {
					if(current.getChildNodes().getLength()!=1)
					System.out.print("{");
				}
			} else if(k == 1) {// if repeated node 
				if(current.getNodeName() != ndList.item(i+2).getNodeName()) {//if current does not match next
					System.out.println(" ]");
					k = 0;
				}
			}
		} else  {
			if(k==0)
		    if(current.getChildNodes().getLength() > 1)
			System.out.print("{");
		}
		return k;
	}
	static void dontWantToPrintArrayNameAgain(NodeList ndList , Node current , int i) {
		if(i>2) {
			if(ndList.item(i-2).getNodeName() != current.getNodeName()){
				System.out.print(" \" "+ndList.item(i).getNodeName()+" \":");
			} else {
				if(current.getChildNodes().getLength()!=1 )
				System.out.print("");
			}
		} else {
			System.out.print(" \" "+ndList.item(i).getNodeName()+" \":");
		}
	}
	static int  checkStatusOfCurrentNode(NodeList ndList, int k, int i , Node current){
		if(ndList.item(i).getChildNodes().getLength() <= 1 ) {
			if(!ndList.item(i).getTextContent().equals("")){
			System.out.print(" \" "+ndList.item(i).getTextContent()+ " \" ,");
			System.out.println();
			}
		} 
		return k;
	}
}