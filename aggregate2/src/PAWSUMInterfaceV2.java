import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class PAWSUMInterfaceV2 implements UMInterface {
    //private static final int LEVELS = 9; // define how many leves will be for each content item. First level is knowledge, then progress, then ..  
	private static final int LEVELS = 12; // modified by @Jordan as we added three metrics for measuring performance on the last k attempts 
	private String server = "http://localhost";
//    private String server = "http://localhost:8080"; //Use for debugging locally

    private String userInfoServiceURL = server + "/aggregateUMServices/GetUserInfo";
    private String classListServiceURL = server + "/aggregateUMServices/GetClassList";

    private boolean contrainOutcomeLevel = true; // knowledge levels will be computed only on outcome concepts
    
    private HashMap<String, double[]> kcSummary; // knowledge components (concepts) and the level of knowledge of the user in each of them
    private HashMap<String, ArrayList<String[]>> kcByContent; // for each content there is an array list of kc (concepts) with id, weight (double) and direction (prerequisite/outcome)
    private HashMap<String, double[]> contentSummary;
    private HashMap<String, Activity> contentSummaryV2;

    static boolean verbose = true;	
    
    // SERVICE
    public String[] getUserInfo(String usr, String key) {
        String[] data = null;
        try {
            String url = userInfoServiceURL + "?usr=" + usr + "&key=" + key;
            JSONObject json = readJsonFromUrl(url);
            // System.out.println(json.toString());
            if (json.has("error")) {
                System.out.println("Error:[" + json.getString("errorMsg") + "]");
            } else {
                data = new String[2];
                data[0] = json.getString("learnerName");
                data[1] = json.getString("learnerEmail");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    // SERVICE
    // an arraylist with the students/users in the class. Each item has: userid,
    // login (username), name, email
    public ArrayList<String[]> getClassList(String grp, String key) {
        ArrayList<String[]> classList = null;
        try {
            String url = classListServiceURL + "?grp=" + grp + "&key=" + key;
            JSONObject json = readJsonFromUrl(url);
            // System.out.println(json.toString());
            if (json.has("error")) {
                System.out.println("Error:[" + json.getString("errorMsg") + "]");
            } else {
                classList = new ArrayList<String[]>();
                JSONArray learners = json.getJSONArray("learners");

                for (int i = 0; i < learners.length(); i++) {
                    JSONObject jsonobj = learners.getJSONObject(i);
                    String[] learner = new String[4];
                    learner[0] = jsonobj.getString("learnerId");
                    learner[1] = jsonobj.getString("name");
                    learner[2] = jsonobj.getString("email");
                    learner[3] = i+"";
                    classList.add(learner);
                    // System.out.println(jsonobj.getString("name"));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classList;
    }


    /**
     * This is the main method!!
     * @ return each double[]: knowledge, progress, attempts/loads, success rate, completion, annotation count, like count, time spent, sub activities
     */
    public HashMap<String, double[]> getContentSummary(String usr, String grp, String sid, String cid, String domain, 
            HashMap<String, String[]> contentList, HashMap<String,String[]> providers, ArrayList<String> options) {
    	
    	if(verbose){
    		System.out.println();
    		for (Map.Entry<String, String[]> prov : providers.entrySet()){
    			System.out.println(prov.getKey());
    		}
    	}
    	
    	contentSummary = new HashMap<String, double[]>(); 
    	
    	// 1. PUT ALL CONTENT IN SEPARATE ARRAYS DEPENDING ON THE PROVIDER
    	HashMap<String,String> contentByProvider = new HashMap<String,String>();
    	// these contain lists of different URLs
    	HashMap<String,String> umSvcURLs = new HashMap<String,String>();
    	HashMap<String,String> activitySvcURLs = new HashMap<String,String>();
    	
//    	HashMap<String,String> contentLists4UM = new HashMap<String,String>();
//    	HashMap<String,String> contentLists4ACT = new HashMap<String,String>();
    	
    	for (Map.Entry<String, String[]> content : contentList.entrySet()) {
    		String content_name = content.getKey();
    		String[] content_data = content.getValue();
            String content_provider = content_data[5];
            String[] urls = providers.get(content_provider); // 0: UM url, 1: ACT url
            if(umSvcURLs.get(urls[0]) == null) umSvcURLs.put(urls[0],urls[0]);
            if(activitySvcURLs.get(urls[1]) == null) activitySvcURLs.put(urls[1],urls[1]);
            
//            if(contentLists4UM.get(urls[0]) == null) contentLists4UM.put(urls[0],content_name);
//            else contentLists4UM.put(urls[0],contentLists4UM.get(urls[0])+","+content_name);
//
//            if(contentLists4ACT.get(urls[1]) == null) contentLists4ACT.put(urls[1],content_name);
//            else contentLists4ACT.put(urls[1],contentLists4ACT.get(urls[1])+","+content_name);

            if(contentByProvider.get(content_provider) == null) contentByProvider.put(content_provider,"\""+content_name+"\"");
            else contentByProvider.put(content_provider,contentByProvider.get(content_provider)+","+"\""+content_name+"\"");   
    	}

    	//System.out.println("GETTING THE KNOWLEDGE");
    	// 2. GENERATE CALLS FOR GET KNOWLEDGE LEVELS
    	for(Map.Entry<String, String> calls : umSvcURLs.entrySet()){ //
    		
    		
    		String svcURL = calls.getKey();
    		
    		//System.out.println(svcURL);
    		
    		if(!svcURL.equalsIgnoreCase("none") && !svcURL.equalsIgnoreCase("unknown") && !svcURL.equalsIgnoreCase("NA")){
        		String json = "{\n    \"user-id\" : \""+usr+"\",\n    \"group-id\" : \""+grp+"\",\n    \"domain\" : \""+domain+"\",\n    \"content-list-by-provider\" : [  \n";
        		for(Map.Entry<String,String[]> provider : providers.entrySet()){
        			if(provider.getValue()[0] != null && provider.getValue()[0].equals(svcURL) && contentByProvider.get(provider.getKey()) != null){
        				json += "        {\"provider-id\" : \""+provider.getKey()+"\", \"content-list\" : ["+contentByProvider.get(provider.getKey())+"]},\n";
        			}
        		}
        		json = json.substring(0,json.length()-2);
        		json += "\n    ]\n}";

        		// Make a call to svcURL and process data adding content to contentSummary if content does not exist already, or complete the corresponding element 
        		// @@@
        		
        		System.out.println("svcURL: "+svcURL);//Added by @Jordan for debugging

        		//System.out.println("Input: ");
        		//System.out.println(json);
        		
        		JSONObject jsonResponse = callService(svcURL, json);
//        		System.out.println("");
//        		//System.out.println("");
//        		System.out.println("");
//        		System.out.println("Output: ");
//        		System.out.println(jsonResponse.toString());
        		if(jsonResponse != null){
            		JSONArray jsonContentArray = jsonResponse.getJSONArray("content-list");
            		for(int i=0; i<jsonContentArray.length(); i++){
            			
            			JSONObject c = jsonContentArray.getJSONObject(i);
            			String contentId = c.getString("content-id");
            			double k = c.getDouble("knowledge");
            			//System.out.println(contentId + " : " + k);
            			
            			double[] values = contentSummary.get(contentId);
            			
            			if(values == null){
            				values = new double[LEVELS];
            				contentSummary.put(contentId,values);
            			}
            			
            			values[0] = k;
            			
            		}    			
        			
        		}
    		}

    	}
    	// 3. GENERATE CALLS FOR GET ACTIVITY LEVELS
    	
    	for(Map.Entry<String, String> calls : activitySvcURLs.entrySet()){
    		String svcURL = calls.getKey();
    		
    		String json = "{\n    \"user-id\" : \""+usr+"\",\n    \"group-id\" : \""+grp+"\",\n    \"domain\" : \""+domain+"\",\n    \"content-list-by-provider\" : [  \n";
    		for(Map.Entry<String,String[]> provider : providers.entrySet()){
    			if(provider.getValue()[1].equals(svcURL) && contentByProvider.get(provider.getKey()) != null){
    				json += "        {\"provider-id\" : \""+provider.getKey()+"\", \"content-list\" : ["+contentByProvider.get(provider.getKey())+"]},\n";
    			}
    		}
    		json = json.substring(0,json.length()-2);
    		json += "\n    ]\n}";

    		// Make a call to svcURL and process data adding content to contentSummary if content does not exist already, or complete the corresponding element 
    		// @@@

    		//System.out.println("Input: ");
    		//System.out.println("CALLING: "+ svcURL + "\n" + json);
    		
    		JSONObject jsonResponse = callService(svcURL, json);
//    		System.out.println("");
//    		//System.out.println("");
//    		System.out.println("");
//    		System.out.println("Output: ");
//    		System.out.println(jsonResponse.toString());
    		if(jsonResponse != null){
        		JSONArray jsonContentArray = jsonResponse.getJSONArray("content-list");
        		for(int i=0; i<jsonContentArray.length(); i++){
        			
        			JSONObject c = jsonContentArray.getJSONObject(i);
        			String contentId = c.getString("content-id");
        			double progress = 0.0;
        			double att = 0.0;
        			double sr = 0.0;
        			double anns = 0.0;
        			double likes = 0.0;
        			double time = 0.0;
        			double subs = 0;
        			try{ progress = c.getDouble("progress");}catch(Exception e){};
        			try{ att = c.getDouble("attempts");}catch(Exception e){};
        			try{ sr = c.getDouble("success-rate");}catch(Exception e){};
        			try{ anns = c.getDouble("annotation-count");}catch(Exception e){};
        			try{ likes = c.getDouble("like-count");}catch(Exception e){};
        			try{ time = c.getDouble("time-spent");}catch(Exception e){};
        			try{ subs = c.getDouble("sub-activities");}catch(Exception e){};
        			
        			//System.out.println(contentId + " : " + progress);
        			
        			double[] values = contentSummary.get(contentId);
        			
        			if(values == null){
        				values = new double[LEVELS];
        				values[0] = 0.0;
        				contentSummary.put(contentId,values);
        			}
        			
        			values[1] = progress;
        			values[2] = att;
        			values[3] = sr;
        			values[4] = progress; // completion for now is just progress
        			values[5] = anns;
        			values[6] = likes;
        			values[7] = time;
        			values[8] = subs;
        			
        		}
        	}
    	}
        return contentSummary;
    }

    
    /*
     * New version of the method contains more detailed information of the attempts. Also allows to call the method with no content list (contentList = null)
     */
    public HashMap<String, Activity> getContentSummaryV2(String usr, String grp, String sid, String cid, String domain, 
            HashMap<String, String[]> contentList, HashMap<String,String[]> providers, ArrayList<String> options, String dateFrom) {
    	
    	contentSummaryV2 = new HashMap<String, Activity>(); 
    	
    	// 1. PUT ALL CONTENT IN SEPARATE ARRAYS DEPENDING ON THE PROVIDER
    	HashMap<String,String> contentByProvider = new HashMap<String,String>();
    	// these contain lists of different URLs
    	HashMap<String,String> umSvcURLs = new HashMap<String,String>();
    	HashMap<String,String> activitySvcURLs = new HashMap<String,String>();
    	
//    	HashMap<String,String> contentLists4UM = new HashMap<String,String>();
//    	HashMap<String,String> contentLists4ACT = new HashMap<String,String>();
    	if(contentList != null){
        	for (Map.Entry<String, String[]> content : contentList.entrySet()) {
        		String content_name = content.getKey();
        		String[] content_data = content.getValue();
                String content_provider = content_data[5];
                String[] urls = providers.get(content_provider); // 0: UM url, 1: ACT url
                if(umSvcURLs.get(urls[0]) == null) umSvcURLs.put(urls[0],urls[0]);
                if(activitySvcURLs.get(urls[1]) == null) activitySvcURLs.put(urls[1],urls[1]);

                if(contentByProvider.get(content_provider) == null) contentByProvider.put(content_provider,"\""+content_name+"\"");
                else contentByProvider.put(content_provider,contentByProvider.get(content_provider)+","+"\""+content_name+"\"");   
        	}
    		
    	}else{ // this part looks for the unique urls
        	for (Map.Entry<String, String[]> provider : providers.entrySet()) {
                String[] urls = provider.getValue(); // 0: UM url, 1: ACT url
                if(umSvcURLs.get(urls[0]) == null) umSvcURLs.put(urls[0],urls[0]);
                if(activitySvcURLs.get(urls[1]) == null) activitySvcURLs.put(urls[1],urls[1]);
                if(contentByProvider.get(provider.getKey()) == null) contentByProvider.put(provider.getKey(),"");   
        	}
    	}

    	//System.out.println("GETTING THE KNOWLEDGE");
    	// 2. GENERATE CALLS FOR GET KNOWLEDGE LEVELS
    	// DEPRECATED!
    	
    	// 3. GENERATE CALLS FOR GET ACTIVITY LEVELS
    	
    	for(Map.Entry<String, String> calls : activitySvcURLs.entrySet()){
    		String svcURL = calls.getKey();
    		
    		String json = "{\n    \"user-id\" : \""+usr+"\",\n    \"group-id\" : \""+grp+"\",\n    \"domain\" : \""+domain+"\",\n"
    				+ (dateFrom != null ? "    \"date-from\" : \""+dateFrom+"\",\n" : "") +"    \"content-list-by-provider\" : [  \n";
    		for(Map.Entry<String,String[]> provider : providers.entrySet()){
    			if(provider.getValue()[1].equals(svcURL) && contentByProvider.get(provider.getKey()) != null){
    				json += "        {\"provider-id\" : \""+provider.getKey()+"\", \"content-list\" : ["+contentByProvider.get(provider.getKey())+"]},\n";
    			}
    		}
    		json = json.substring(0,json.length()-2);
    		json += "\n    ]\n}";
    		
    		JSONObject jsonResponse = callService(svcURL, json);

    		
    		if(jsonResponse != null){
        		JSONArray jsonContentArray = jsonResponse.getJSONArray("content-list");
        		for(int i=0; i<jsonContentArray.length(); i++){
        			
        			JSONObject c = jsonContentArray.getJSONObject(i);
        			String contentId = c.getString("content-id");
        			double progress = 0.0;
        			double att = 0.0;
        			double sr = 0.0;
        			double anns = 0.0;
        			double likes = 0.0;
        			double time = 0.0;
        			double subs = 0;
        			double lastKprogress = 0.0;
        			double lastKatt = 0.0;
        			double lastKsr = 0.0;
        			String attemptSeq = "";
        			try{ progress = c.getDouble("progress");}catch(Exception e){progress = 0;};
        			try{ att = c.getDouble("attempts");}catch(Exception e){};
        			try{ sr = c.getDouble("success-rate");}catch(Exception e){};
        			try{ anns = c.getDouble("annotation-count");}catch(Exception e){};
        			try{ likes = c.getDouble("like-count");}catch(Exception e){};
        			try{ time = c.getDouble("time-spent");}catch(Exception e){};
        			try{ subs = c.getDouble("sub-activities");}catch(Exception e){};
        			try{ attemptSeq = c.getString("attempts-seq");}catch(Exception e){attemptSeq = "";};
        			try{ lastKprogress = c.getDouble("lastk-progress");}catch(Exception e){lastKprogress = 0;};
        			try{ lastKatt = c.getDouble("lastk-attempts");}catch(Exception e){ lastKatt = -1.0;};
        			try{ lastKsr = c.getDouble("lastk-success-rate");}catch(Exception e){ lastKsr = -1.0;};
        			
        			//System.out.println(contentId + " : " + progress);
        			
        			Activity activity = contentSummaryV2.get(contentId);
        			double[] values = new double[LEVELS];
        			if(activity == null){
        				activity = new Activity(contentId);
        				contentSummaryV2.put(contentId,activity);
        			}
        			values[0] = 0.0;
        			values[1] = progress;
        			values[2] = att;
        			values[3] = sr;
        			values[4] = progress; // completion for now is just progress
        			values[5] = anns;
        			values[6] = likes;
        			values[7] = time;
        			values[8] = subs;
        			//Metrics added for getting the performance of the user in the last k attempts
        			values[9] = lastKprogress;
        			values[10] = lastKatt;
        			values[11] = lastKsr;
        			activity.setLevels(values);
        			
        			activity.setAttemptsResSeq(attemptSeq);
        		}
    		}
    	}
       
        return contentSummaryV2;
    }
    /* This implementation is temporal. It should use the URL of services defined for each content provider. 
     * And these services should extend the protocol to deliver activity record in "expanded" form, given 
     * for example, an extra parameter 
     * e.g., ArrayList<Attempt> lastAttempts = um_interface.getLastContentActivity(usr, grp, sid, cid, domain, contentList, providers, null, (lastUpdateDate != null ? lastUpdateDate : "2016-01-01"));

     * */
    
    public ArrayList<Attempt> getLastContentActivity(
            String usr, String grp, String sid, String cid, String domain,
            HashMap<String, String[]> contentList, HashMap<String,String[]> providers,
            ArrayList<String> options, String dateFrom){
    	ArrayList<Attempt> res = new ArrayList<Attempt>();
    	
    	String svcURL = server + "/aggregateUMServices/GetLastActivity"+"?usr="+usr+"&domain="+domain+"&dateFrom="+dateFrom;
    	JSONObject jsonResponse = callService(svcURL, "");
    	if(jsonResponse != null){
    		JSONArray jsonActivityArray = jsonResponse.getJSONArray("activity");
    		for(int i=0; i<jsonActivityArray.length(); i++){
    			
    			JSONObject c = jsonActivityArray.getJSONObject(i);
    			String contentName = c.getString("content-id");
    			double result = c.getDouble("r");
    			Attempt a = new Attempt(contentName,result);
    			res.add(a);
    			//if (verbose) System.out.println(contentName+" : "+result);
    			
    		}
    	}
    	
    	
    	return res;
    }
    
    
    private static JSONObject callService(String url, String json){
    	InputStream in = null;
    	JSONObject jsonResponse = null;
		// A JSON object is created to pass the required parameter to the recommendation service implemented by GetRecommendations.java
		try {
			HttpClient client = new HttpClient();
//			url = url.replace("http://adapt2.sis.pitt.edu", "http://localhost:8080"); // Use for debugging locally
            PostMethod method = new PostMethod(url);
            method.setRequestBody(json);
            method.addRequestHeader("Content-type", "application/json");
            if(verbose) System.out.println("Calling service "+url);
            int statusCode = client.executeMethod(method);

            if (statusCode != -1) {
            	
                in = method.getResponseBodyAsStream();
                jsonResponse =  readJsonFromStream(in);
                in.close();
            }else{
            	
            }
		}catch(Exception e){}
		return jsonResponse;
    }
    
    public static JSONObject readJsonFromStream(InputStream is)  throws Exception{
		JSONObject json = null;
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			json = new JSONObject(jsonText);
		}catch(Exception e){
			//e.printStackTrace();
			if(verbose) System.out.println("  error parsisng json from this service");
		}
		return json;
	}
    

    
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException,
            JSONException {
        InputStream is = new URL(url).openStream();
        JSONObject json = null;
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is,
                    Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            json = new JSONObject(jsonText);
        } finally {
            is.close();
        }
        return json;
    }

}
