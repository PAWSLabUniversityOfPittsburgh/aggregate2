

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONObject;

public class PAWSRecInterface implements RecInterface {
	private boolean verbose = true;
	private String server = "http://localhost";
	
    private String RecServiceURL = server + "/recommendation/GetRecommendations";
	//private String RecServiceURL = "http://localhost:8080/recommendations/GetRecommendations";
   
	@Override
	public ArrayList<ArrayList<String[]>> getRecommendations(String usr,
            String grp, String sid, String cid, String domain, String lastContentId,
            String lastContentResult, String lastContentProvider,
            String contents, int maxReactiveRec,int maxProactiveRec,
            double reactiveRecThreshold, double proactiveRecThreshold, 
            String reactiveRecMethod, String proactiveRecMethod,
            HashMap<String, ArrayList<String>[]> topicContent,
            HashMap<String, double[]> userContentLevels) {
		
		ArrayList<ArrayList<String[]>> result = new ArrayList<ArrayList<String[]>>();
		ArrayList<String[]> reactive_list = new ArrayList<String[]>();
    	ArrayList<String[]> proactive_list = new ArrayList<String[]>();
		InputStream in = null;
		JSONObject json = null;
		// A JSON object is created to pass the required parameter to the recommendation service implemented by GetRecommendations.java
		try {
			HttpClient client = new HttpClient();
            PostMethod method = new PostMethod(RecServiceURL);
            String paramsJson = createParamJSON(usr, grp, sid, cid, domain, lastContentId, lastContentResult, lastContentProvider,
					contents, maxReactiveRec, maxProactiveRec, reactiveRecThreshold, proactiveRecThreshold,
					reactiveRecMethod, proactiveRecMethod, topicContent, userContentLevels);   
            method.setRequestBody(paramsJson);
            method.addRequestHeader("Content-type", "application/json");

            if (verbose) System.out.println("RECOMENDATION CALL:");
            if (verbose) System.out.println(method.getURI().toString()+"\n"+method.getRequestBodyAsString());  
            
            int statusCode = client.executeMethod(method);

            if (statusCode != -1) {
            	
                in = method.getResponseBodyAsStream();
//                InputStreamReader is = new InputStreamReader(in);
//                BufferedReader br = new BufferedReader(is);
//                String read = br.readLine();
//
//                while(read != null) {
//                    System.out.println(read);
//                    read =br.readLine();
//                }
                json =  readJsonFromStream(in);
                System.out.println("\n\n\n\n\nTHIS IS THE JSON FROM GetRecommendations: \n\n"+json.toString());
                in.close();
                if(json != null){
	                if (json.has("error")) {
	                    System.out.println("Error:[" + json.getString("errorMsg") + "]");
	                    System.out.println(json.toString());
	                } else {
	                	//System.out.println(json.toString());
	                	JSONObject reactive = json.getJSONObject("reactive");
	                	JSONObject proactive = json.getJSONObject("proactive");
	                	
	                	
	                	String r_recid = "";
	                	String p_recid = "";
	                	if (reactive.has("id")) r_recid = reactive.getString("id");
	                	if (proactive.has("id")) p_recid = proactive.getString("id");
	                	
	                	JSONArray json_reactive_list = null;
	                	JSONArray json_proactive_list = null;
	                	if (reactive.has("contentScores")) json_reactive_list = reactive.getJSONArray("contentScores");
	                	if (proactive.has("contentScores")) json_proactive_list = proactive.getJSONArray("contentScores");
	                	//System.out.println("Reactive rec");
	                	if(json_reactive_list != null){
	                		for (int i = 0; i < json_reactive_list.length(); i++) {
	                            JSONObject jsonrec = json_reactive_list.getJSONObject(i);
	                            String[] rec = new String[4];
	                            rec[0] = jsonrec.getString("rec_item_id");
	                            rec[1] = jsonrec.getString("approach");
	                            rec[2] = jsonrec.getString("content");
	                            rec[3] = jsonrec.getDouble("score") + "";
	                            reactive_list.add(rec);
	                            //System.out.println("  "+rec[2]);
	                		}
	                	}
	                	//System.out.println("Proactive rec");
	                	if(json_proactive_list != null){
	                		for (int i = 0; i < json_proactive_list.length(); i++) {
	                            JSONObject jsonrec = json_proactive_list.getJSONObject(i);
	                            String[] rec = new String[4];
	                            rec[0] = jsonrec.getString("rec_item_id");
	                            rec[1] = jsonrec.getString("approach");
	                            rec[2] = jsonrec.getString("content");
	                            rec[3] = jsonrec.getDouble("score") + "";
	                            proactive_list.add(rec);
	                            //System.out.println("  "+rec[2]);
	                		}
	                	}
	                }
                }else{
                	// json null
                }
               
            }

        } catch (Exception e) {
        	result = null;
        	System.out.println("JSON RESPONSE WITH ERROR: \n\n"+json+"\n\n");
            e.printStackTrace();
            return result;
        }finally{
        	
        }
		result.add(reactive_list);
    	result.add(proactive_list);
		return result;
	}



	private String createParamJSON(String usr, String grp, String sid, String cid, String domain, String lastContentId,
			String lastContentResult, String lastContentProvider, String contents,
			int maxReactiveRec, int maxProactiveRec, double reactiveRecThreshold, double proactiveRecThreshold,
			String reactiveRecMethod, String proactiveRecMethod, HashMap<String, ArrayList<String>[]> topicContent,
			HashMap<String, double[]> userContentLevels) throws UnsupportedEncodingException {
		
		JSONObject json = new JSONObject();
		json.put("usr", usr);
		json.put("grp", grp);
		json.put("sid", sid);
		json.put("cid", cid);
		json.put("domain", domain);
		json.put("lastContentId", lastContentId);
		json.put("lastContentResult", lastContentResult);
		json.put("lastContentProvider", lastContentProvider);
		if(maxReactiveRec > -1) json.put("reactive_max", maxReactiveRec+"");
		if(maxProactiveRec > -1) json.put("proactive_max", maxProactiveRec+"");
		if(reactiveRecThreshold > -1) json.put("reactive_threshold", reactiveRecThreshold+"");
		if(proactiveRecThreshold > -1) json.put("proactive_threshold", proactiveRecThreshold+"");
		if(reactiveRecMethod != null && reactiveRecMethod.length()>0) json.put("reactive_method", reactiveRecMethod);
		if(proactiveRecMethod != null && proactiveRecMethod.length()>0) json.put("proactive_method", proactiveRecMethod);
		json.put("contents", contents);
		json.put("topicContents",getTopicContentText(topicContent));
		json.put("userContentProgress", getUserContentProgressText(userContentLevels));
		return json.toString();
	}
	


//	private String getTopicContents(HashMap<String, ArrayList<String>[]> topicContent) {
//		String text = "";
//		ArrayList<String>[] contents;
//		for (String t : topicContent.keySet())
//		{
//			text += t + ","; // the first element in each commar separated list is the topic name;  topics are separated by ~
//			contents = topicContent.get(t);
//			for (ArrayList<String> c : contents)
//			{
//				text += c+ "," ;
//			}
//			text.substring(0, text.length()-1); // this is for ignoring the last comma 
//			text += "~"; // this is used for separating topics
//		}
//		text.substring(0, text.length()-1); // this is for ignoring the last ~ 
//		return text;
//	}

	//This method returns a string with this format : act1,1;act2,0
	private String getUserContentProgressText(HashMap<String, double[]> userContentLevels) {
		String contentLvl = "";
		if (userContentLevels != null) {
			for (Entry<String, double[]> e : userContentLevels.entrySet()) {
				contentLvl += e.getKey() + "," + e.getValue()[1] + ";"; //2nd value in the array is for progress
			}
			if(contentLvl.length()>0) contentLvl = contentLvl.substring(0, contentLvl.length()-1); //this is for ignoring the last ;
		}
		return contentLvl;
	}

	/*
	 * @returns string representation of topic-contents. Sample format is:
	 * T1:a,b,c|T2:d,e,f|T3:h,g,i
	 */
	private String getTopicContentText(HashMap<String, ArrayList<String>[]> topicContent) {
		String mainTxt = "";
		for (String topic : topicContent.keySet()) {
			String contentsTxt = "";
			for (ArrayList<String> contentList : topicContent.get(topic)) {
				for (String c : contentList) {
					contentsTxt += (contentsTxt.isEmpty()? "": ",") + c;
				}
				mainTxt += (mainTxt.isEmpty()? "" : "|") + topic + ":" + contentsTxt;				
			}
		}
		return mainTxt;
	}
	
	public static JSONObject readJsonFromStream(InputStream is)  throws Exception{
		JSONObject json = null;
		BufferedReader rd;
		String jsonText = "";
		try {
			rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			jsonText = readAll(rd);
			json = new JSONObject(jsonText);
		}catch(Exception e){
			System.out.println("JSON RESPONSE WITH ERROR: \n\n"+jsonText+"\n\n");
			e.printStackTrace();
			json = null;
		}
		return json;
	}
	
    private static String readAll(Reader rd) throws Exception {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

}
