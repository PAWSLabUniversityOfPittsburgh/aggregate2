import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * This is the main class. An Aggregate object obtains the course structure
 * (topics, content) from aggregate DB and the levels of knowledge and progress
 * from the User Model using the UMInterface methods.<br />
 * First instantiate the Aggregate object, second use the method fillClassLevels
 * to load the precomputed UM models stored, third compute group levels (class
 * average, top students) using computeGroupLevels, then sequence using method
 * sequenceContent and load recommendations using fillRecommendations. Finally,
 * generate the JSON response using genAllJSON or genUserJSON
 *
 * @author Julio Guerra, PAWS lab
 *
 *         TO DO -
 */
public class Aggregate {
	private boolean verbose; // for "old-fashioned" debugging purposes

	public ConfigManager cm;

	private String usr;
	private String grp;
	private String sid;
	private String domain;
	private String usr_name;
	private String usr_email;
	private String grp_name;
	private String cid;

	// STRUCTURES STORING THE COURSE STRUCTURE
	// some use HashMap to speed up computations
	public ArrayList<String[]> topicList; // each has: topic name (string id), display name (string), order (int),
											// visibility (1/0)
	
	/**each key represent a content item, identified by the
    *  content_name and each value is an String[] with:<br />
    *  0: resource name (id) 1: display name 2: url 3: description 4:
    *  comment 5: provider id ...*/
	public HashMap<String, String[]> contentList; // each content has: resource name (string id), display name, url,
													// description, comment, provider_id (string id)
	public HashMap<String, String> mapContentTopic; // maps content id and topic id (the first topic containing the
													// content)
	public ArrayList<String[]> resourceList; // resource name , resource display name (ex: qz , question)
	public HashMap<String, String[]> providers;
	public HashMap<String, Integer> resourceMap; // store resource name and position in the resourceList
	public HashMap<String, ArrayList<String>[]> topicContent; // topic name , one arraylist for each resource (type) in
																// the order of resorceList

	public HashMap<String, ContentStats> statsContent;

	public HashMap<String, KnowledgeComponent> allKCList = null;
	public HashMap<String, KnowledgeComponent> singleKCList = null;
	public HashMap<String, KnowledgeComponentGroup> groupedKCList = null;

	public int nTopicLevels; // NResource X 2: 2 levels (K,P) for each resource type
	public int nContentLevels; // 9 levels (K,P,A,S,C,AN,L,T,Sub) knowledge,progress,N attempts, success rate,
										// completion, annotations, likes, time, subactivities
	public int nKCLevels; // 7 levels (K,P,A,SR,S,C,T ) knowledge,progress, attempts (raw), success rate,
									// problems solved, connections done, time

	// Ordered ArrayList of students in the class. Each elements has (strings):
	// learner id (or user id), name, email
	public ArrayList<String[]> class_list;
	public HashMap<String, String> non_students;

	public String[] groupParameters; // visualization, services
	public String[] userParameters; // visualization, services
	
	public Map<String, String> userVisParameterMap; 
	public Map<String, String> groupVisParameterMap;

	// proactive recommendation scores per content and per topic
	public HashMap<String, Double> contentSequencingScores;
	public HashMap<String, double[]> topicSequencingScores; // array of double corresponding to the dimension of
															// resources

	public HashMap<String, double[]> userTopicLevels;
	public HashMap<String, double[]> userContentLevels;
	public HashMap<String, String> userContentSequences;
	// public HashMap<String, Activity> userContentLevels;
	// public HashMap<String, Activity> userContentLevels2;
	public HashMap<String, double[]> userKCLevels;
	public HashMap<String, int[]> activityConceptCounts;

	public ArrayList<String[]> subgroups;
	// public ArrayList<String[]> subgroups;

	public ArrayList<Map<String, double[]>> subgroups_topic_levels;
	public ArrayList<Map<String, double[]>> subgroups_content_levels;
	public ArrayList<Map<String, double[]>> subgroups_kc_levels;
	public ArrayList<ArrayList<String>> subgroups_student_ids;
	public ArrayList<ArrayList<String>> subgroups_student_anonym_ids;
	public ArrayList<String> subgroups_names;

	// public Map<String, double[]> aggs1_topic_levels;
	// public Map<String, double[]> aggs2_topic_levels;
	// public Map<String, double[]> aggs1_content_levels;
	// public Map<String, double[]> aggs2_content_levels;

	public boolean includeOthers = true; // include other learners in the json
	public int topN = -1; // how many top students. Parameter load from database if defined there
	public ArrayList<String> top_students_ids;

	public Map<String, Map<String, double[]>> peers_topic_levels;
	public Map<String, Map<String, double[]>> peers_content_levels;
	public Map<String, Map<String, double[]>> peers_kc_levels;

	// recommendation set (reactive recommendations)
	public ArrayList<ArrayList<String>> recommendation_list;
	// recommendation set from KCModeler
	private ArrayList<ArrayList<String[]>> recommendedActivitiesKCModeler;

	// feedback set
	public ArrayList<ArrayList<String>> activity_feedback_form_items;
	public String activity_feedback_id;

	// public um2DBInterface um2_db;
	public UMInterface um_interface;
	public RecInterface rec_interface;
	public AggregateDB agg_db;

	public static DecimalFormat df = new DecimalFormat("#.##");
	public static DecimalFormat df2 = new DecimalFormat("#.####");

	private boolean kcJSONById = true;

	private String userManualFile = "";
	
	private HashMap<String,String> userPreferences = new HashMap<String,String>();
	private Map<String,Map<String,String>> groupPreferences;
	
	private List<String> global_new_badge_id = new ArrayList<>();

	

	/**
	 * The constructor load the course structure and content from DB and compute the
	 * aggregated user model levels (progress and knowledge) if parameter updateUM
	 * is true.
	 *
	 * @param usr
	 *            user id
	 * @param grp
	 *            group id
	 * @param cid
	 *            course id
	 * @param sid
	 *            session id
	 * @param updateUM
	 *            true: build the user model, false: build a null user model if the
	 *            user has no precomputed model stored in DB
	 * @param cm
	 *            a ConfigManager object contains configuration variables
	 */
	public Aggregate(String usr, String grp, String cid, String sid, boolean updateUM, ConfigManager cm) {
		this.usr = usr;
		this.grp = grp;
		this.sid = sid;
		this.cm = cm;

		verbose = cm.agg_verbose.equalsIgnoreCase("yes");

		openDBConnections();

		grp_name = agg_db.getGrpName(grp);

		if (cid == null || cid.length() == 0 || cid.equals("-1")) {
			this.cid = agg_db.getCourseId(grp);
		} else {
			this.cid = cid;
		}
		domain = agg_db.getDomain(cid);
		System.out.println("Course domain: "+domain);
		if (domain == null || domain.length() == 0) {
			this.cid = agg_db.getCourseId(grp);
			this.domain = "UNKNOWN";
		}
		try {
			um_interface = (UMInterface) Class.forName(cm.agg_uminterface_classname).newInstance();

		} catch (Exception e) {
			// @@@@ um_interface = new NullUMInterface();
			e.printStackTrace();
		}

		try {
			rec_interface = (RecInterface) Class.forName(cm.agg_recinterface_classname).newInstance();

		} catch (Exception e) {
			// @@@@ um_interface = new NullUMInterface();
			e.printStackTrace();
		}

		// the userdata array contains user name, email and parameters for configuration
		String[] userdata = um_interface.getUserInfo(usr, cm.agg_uminterface_key);
		if (userdata != null) {
			usr_name = userdata[0];
			usr_email = userdata[1];
		} else {
			usr_name = "unknown";
			usr_email = "unknown";
		}

		processParameters(agg_db.getParameters(usr, grp));
		
		this.userPreferences = agg_db.getLastUserPreferences(this.usr, this.grp, "MasteryGrids");
		this.groupPreferences = agg_db.getLastGroupPreferences(this.grp, "MasteryGrids");

		class_list = um_interface.getClassList(grp, cm.agg_uminterface_key);
		if (class_list == null) {
			class_list = new ArrayList<String[]>();
			String[] theUser = { usr, "unknown", "unknown", "0" };
			class_list.add(theUser);
		}
		non_students = agg_db.getNonStudents(grp); // special non students (instructor, researcher)

		resourceList = agg_db.getResourceList(cid);
		genResourceMap();

		nTopicLevels = resourceList.size() * 2; // the dimension of the topic levels array. For each resource there is a
												// level of knowledge and a level of progress
		nContentLevels = 9; // see Activity.levels
		//nKCLevels = 8; // the last one purely for BN //Commented by @Jordan
		nKCLevels = 12;// tHe last 3 are related to lastKprogress, lastKatt, lastKsr

		topicList = agg_db.getTopicList(cid);

		ArrayList<String> hidden_topics = agg_db.getHiddenTopics(grp);
		hideTopics(hidden_topics); // set the visibility attribute (topic[3]) to topics being invisible for this
									// group

		// add current, covered to the 5th attribute of each topic
		processTopicStates(agg_db.getTimeLine(grp));

		contentList = agg_db.getContent2(cid);
		// System.out.println("content got");

		topicContent = agg_db.getTopicContent2(cid, resourceMap);
		mapContentToTopic();
		// System.out.println("topic content");

		// providers info
		providers = agg_db.getProvidersInfo(cid);

		// content stats
		// if(!updateUM) getContentStats();

		// @@@ JULIO dealing with KnowledgeComponents

		if (cm.agg_kcmap) {
			// Get the KC from the database. It includes single and multiple components
			// groups
			System.out.println("agg_kcmap value: "+cm.agg_kcmap);
			allKCList = agg_db.getAllKCs(domain);
			separateKCList(allKCList);

		}

		userContentSequences = new HashMap<String, String>();
		// This part computed the user model if updateUM = true or if the user
		// has no pre-computed model stored in the db (first log in)
		System.out.println("Update UM: "+updateUM);
		if (updateUM) {
			computeUserLevels(usr, grp, sid, cid, domain);
			// computeUserBadges(usr,grp,domain);
			// computeUserPoints(usr,grp,domain);
			// storePrecomputedModel(usr);
			storeComputedModel(usr);
			// storeComputedBadges();
			// storeComputedPoints();
		} else{
			if (!agg_db.existComputedModel(usr, cid)) {
				System.out.println("Compute null levels");
				computeNullLevels();
				// storePrecomputedModel(usr);
				storeComputedModel(usr);	
			}else{
				System.out.println("Compute user levels");
				computeUserLevels(usr, grp, sid, cid, domain);
			}
		}

		subgroups = agg_db.getSubGroups(grp);

		closeDBConnections();
	}

	// @@@@ REVIEW!!!!
	// DEV: initialize the object with just the information needed
	// to retrieve class precomputed models stored
	public Aggregate(String grp, ConfigManager cm) {
		this.usr = null;
		this.grp = grp;
		this.sid = null;
		this.cm = cm;

		verbose = cm.agg_verbose.equalsIgnoreCase("yes");

		um_interface = new PAWSUMInterface();
		openDBConnections();

		grp_name = agg_db.getGrpName(grp);
		cid = agg_db.getCourseId(grp);
		domain = agg_db.getDomain(cid);

		usr_name = null;
		usr_email = null;

		resourceList = agg_db.getResourceList(cid);
		genResourceMap();

		nTopicLevels = resourceList.size() * 2; // the dimension of the topic levels array. For each resource there is a
												// level of knowledge and a level of progress
		nContentLevels = 9; // the dimension of the content levels array. For each resource there is a level
							// of knowledge and a level of progress

		// String[] userdata = um_interface.getUserInfo(usr, cm.agg_uminterface_key);
		// if(userdata.length > 2) overwriteConfigForUser(userdata[2]);

		class_list = um_interface.getClassList(grp, cm.agg_uminterface_key);

		topicList = agg_db.getTopicList(cid);

		contentList = agg_db.getContent2(cid);

		topicContent = agg_db.getTopicContent2(cid, resourceMap);
		mapContentToTopic();

		closeDBConnections();
	}

	public void openDBConnections() {
		agg_db = new AggregateDB(cm.agg_dbstring, cm.agg_dbuser, cm.agg_dbpass);
		agg_db.openConnection();
	}

	public void closeDBConnections() {
		agg_db.closeConnection();
	}

	// REVIEW
	public void hideTopics(ArrayList<String> hidden_topics) {
		if (hidden_topics != null && hidden_topics.size() > 0) {
			for (String hidden : hidden_topics) {
				for (String[] topic : topicList) {
					if (topic[0].equalsIgnoreCase(hidden))
						topic[3] = "0";
				}
			}

		}
	}

	public void computeNullLevels() {
		userTopicLevels = nullTopicLevels();
		userContentLevels = nullContentLevels();
		userKCLevels = nullKCLevels();
	}

	public void computeUserLevels() {
		computeUserLevels(usr, grp, sid, cid, domain);
		// storePrecomputedModel(usr);
		storeComputedModel(usr);
	}
	
	// computes user levels aggregating by topics and content
	public void computeUserLevels(String usr, String grp, String sid, String cid, String domain) {
		if (usr == null || domain == null || usr.length() == 0 || domain.length() == 0)
			return;

		userTopicLevels = new HashMap<String, double[]>();
		userContentLevels = new HashMap<String, double[]>();
		userKCLevels = new HashMap<String, double[]>();
		userContentSequences = new HashMap<String, String>();
		

		// fill the hash map with the knowledge(see note below) and progress computations from UM interface
		// contentSummary, each double[]: knowledge(see note below), progress, attempts/loads, success, rate, completion, other 1, other 2
		// Note: knowledge is not computed through um_interface, it is set to 0.
		HashMap<String, Activity> contentSummaryMap = um_interface.getContentSummaryV2(usr, grp, sid, cid, domain,
				contentList, providers, null, null);
		
		// COMPUTE LEVELS FOR KCs
		if (cm.agg_kcmap) {
			System.out.println("Entered to KC estimation...");
			KCModeler kcModeler = new KCModeler(usr, domain, cid, singleKCList, groupedKCList, contentList,
					cm.agg_kcmap_method, domain, cm.servletSource.getServletContext(), nKCLevels);

			System.out.println("agg_kcmap_method: "+cm.agg_kcmap_method);
			if(cm.agg_kcmap_method.equalsIgnoreCase("naive")){
				userKCLevels = kcModeler.computeNaiveKCModel(contentSummaryMap, domain);
			}else{
				if(cm.agg_kcmap_method.equalsIgnoreCase("cumulate")){
					System.out.println("verbose: "+verbose);
					userKCLevels = kcModeler.computeCUMULATEKCModel(contentSummaryMap, domain, grp);
					
				}
			}
			
			if(cm.agg_reflect_kc_estimate_to_items) {
				// Knowledge of each item is aggregated based on knowledge in related KCs (if userKCLevels is computed)
				for(Map.Entry<String, Activity> contentEntry : contentSummaryMap.entrySet()) {
					double contentAttempts = contentEntry.getValue().getLevels()[2];
					// cskamil TODO: How knowledge estimates should be done based on a parameter. Currently it is just a simple average
					if(contentAttempts > 0.0) { // Calculate estimates for only attempted content
						String[] contentArr = contentList.get(contentEntry.getKey());
						
						if(!contentArr[6].trim().isEmpty()) {
							String[] contentKCs = contentArr[6].split(",");
							
							double totalKCKnowledge = 0.0;
							
							for(String KC: contentKCs) {
								totalKCKnowledge += userKCLevels.get(KC)[0];
							}
							
							contentEntry.getValue().getLevels()[0] = totalKCKnowledge/contentKCs.length;
						} else {
							System.out.println("No KC for content: " + contentEntry);
						}
					}
				}
			}
		}
		
		for (Map.Entry<String, Activity> act : contentSummaryMap.entrySet()) {
			userContentLevels.put(act.getKey(), act.getValue().getLevels());
			userContentSequences.put(act.getKey(), act.getValue().getAttemptsResSeq());
		}
		

		// COMPUTE AGGREGATE LEVELS FOR TOPICS
		for (String[] topic : topicList) {

			double[] kpvalues = new double[nTopicLevels];

			// Topic knowledge and progress levels in concepts related with
			// questions
			// using userContentLevels and topic_content
			double user_topic_oneType_k = 0.0;
			double user_topic_oneType_p = 0.0;
			int i = 0;
			int contentsSize = 0;

			ArrayList<String>[] oneTypeContents = topicContent.get(topic[0]);
			if (oneTypeContents != null)
				for (ArrayList<String> contents : oneTypeContents) {
					user_topic_oneType_k = 0.0;
					user_topic_oneType_p = 0.0;
					if (contents != null) {
						for (String content : contents) {
							double[] contentKP = userContentLevels.get(content);
							if (contentKP != null) {
								user_topic_oneType_k += contentKP[0];
								user_topic_oneType_p += contentKP[1];
							}
						}
						contentsSize = contents.size();
						if (contents.size() == 0)
							contentsSize = 1;
						user_topic_oneType_k = user_topic_oneType_k / contentsSize;
						user_topic_oneType_p = user_topic_oneType_p / contentsSize;
					}
					kpvalues[i] = user_topic_oneType_k;
					kpvalues[i + 1] = user_topic_oneType_p;
					i += 2;
					if (i >= nTopicLevels)
						break;
				}
			userTopicLevels.put(topic[0], kpvalues);
		}
		
		
		
		
	}

	// public void computeUserLevels2(String usr, String grp, String sid, String
	// cid, String domain) {
	// if (usr == null || domain == null || usr.length() == 0 || domain.length() ==
	// 0) return;
	//
	// userTopicLevels = new HashMap<String, double[]>();
	// userContentLevels2 = new HashMap<String, Activity>();
	// userKCLevels = new HashMap<String, double[]>();
	//
	//
	// // fill the hash map with the knowledge and progress computations from UM
	// interface
	// // contentSummary, each double[]: knowledge, progress, attempts/loads,
	// success rate, completion, other 1, other 2
	// long time1 = Calendar.getInstance().getTimeInMillis();
	// userContentLevels2 = um_interface.getContentSummaryV2(usr, grp, sid, cid,
	// domain, contentList, providers, null, null);
	//
	//
	// if(verbose) System.out.println(" Get all form UM " +
	// (Calendar.getInstance().getTimeInMillis()-time1));
	//
	// // COMPUTE AGGREGATE LEVELS FOR TOPICS
	// for (String[] topic : topicList) {
	//
	// double[] kpvalues = new double[nTopicLevels];
	//
	// // Topic knowledge and progress levels in concepts related with
	// // questions
	// // using userContentLevels and topic_content
	// double user_topic_oneType_k = 0.0;
	// double user_topic_oneType_p = 0.0;
	// int i = 0;
	// int contentsSize = 0;
	//
	// ArrayList<String>[] oneTypeContents = topicContent.get(topic[0]);
	// if (oneTypeContents != null)
	// for (ArrayList<String> contents : oneTypeContents) {
	// user_topic_oneType_k = 0.0;
	// user_topic_oneType_p = 0.0;
	// if(contents != null){
	// for (String content : contents) {
	// Activity a = userContentLevels2.get(content);
	// if(a!=null){
	// double[] contentKP = a.getLevels();
	// if (contentKP != null) {
	// user_topic_oneType_k += contentKP[0];
	// user_topic_oneType_p += contentKP[1];
	// }
	// }
	// }
	// contentsSize = contents.size();
	// if (contents.size() == 0)
	// contentsSize = 1;
	// user_topic_oneType_k = user_topic_oneType_k / contentsSize;
	// user_topic_oneType_p = user_topic_oneType_p / contentsSize;
	// }
	// kpvalues[i] = user_topic_oneType_k;
	// kpvalues[i + 1] = user_topic_oneType_p;
	// i += 2;
	// if (i >= nTopicLevels)
	// break;
	// }
	// userTopicLevels.put(topic[0], kpvalues);
	// }
	//// TODO:
	// // @@@@ JULIO
	// // COMPUTE LEVELS FOR KCs
	// if(cm.agg_kcmap){
	// //HashMap<String, Activity> getContentSummaryV2(String usr, String grp,
	// String sid, String cid, String domain,
	// // HashMap<String, String[]> contentList, HashMap<String,String[]> providers,
	// ArrayList<String> options, String dateFrom)
	// // 1. get the last computed model for KC, and its last date
	// HashMap<String, String[]> allLastModels = agg_db.getComputedModels(cid, usr);
	// String[] userLastModel = allLastModels.get(usr);
	// String lastUpdateDate = null;
	// HashMap<String, double[]> lastModel = nullKCLevels();
	// if(userLastModel != null && userLastModel[2] != null &&
	// userLastModel[2].length() > 0){
	// lastUpdateDate = userLastModel[3];
	// lastModel = formatLevels(userLastModel[2], nKCLevels);
	// }
	//
	// // 2. if the model contains kcs (not empty string), then call
	// getContentSummaryV2 passing date, otherwise, pass null date
	// HashMap<String, Activity> activity = um_interface.getContentSummaryV2(usr,
	// grp, sid, cid, domain,
	// null, providers, null, lastUpdateDate);
	// KCModeler kcModeler = new KCModeler(usr, domain, cid, singleKCList,
	// groupedKCList, cm.agg_kcmap_method);
	// //System.out.println("####### ###### "+cm.agg_kcmap_method);
	// userKCLevels = kcModeler.computeKCModel(lastUpdateDate, lastModel, activity);
	// }
	//
	// }

	public double getTopicSequenceScore(String topic, String src) {
		if (topicSequencingScores == null)
			return 0;
		double[] scores = topicSequencingScores.get(topic);
		if (scores == null)
			return 0;
		double s = 0;
		Integer i = resourceMap.get(src);
		if (i != null)
			s = scores[i];

		// if (s > cm.agg_proactiverec_threshold)
		// s = 1.0;
		// else
		// s = 0.0;
		return s;
	}

	public double getContentSequenceScore(String content_name) {
		if (contentSequencingScores == null)
			return 0;
		Double score = contentSequencingScores.get(content_name);
		if (score == null)
			return 0;
		// if (score > cm.agg_proactiverec_threshold)
		// score = 1.0;
		// else
		// score = 0.0;
		return score;
	}

	// Get the precomputed models at level of content and topics for each
	// student in the list
	// Pass computePeerLevels as true to get all, or false to get only the user model
	public void fillClassLevels(boolean includeNullStudents, boolean removeZeroProgressStudents, boolean computePeerLevels) {
		if (this.class_list == null || this.class_list.size() == 0) {
			return;
		}
		this.peers_topic_levels = new HashMap<String, Map<String, double[]>>();
		this.peers_content_levels = new HashMap<String, Map<String, double[]>>();
		this.peers_kc_levels = new HashMap<String, Map<String, double[]>>();
		
		openDBConnections();
		// HashMap<String, String[]> precomp_models = agg_db.getPrecomputedModels(cid,
		// usr);
		// System.out.println("Getting all models formthe class. User: "+usr);
		
		HashMap<String, String[]> precomp_models = null; 
		
		if(computePeerLevels) {
			precomp_models = this.agg_db.getComputedModelsInCourse(this.cid);
		} else {
			precomp_models = this.agg_db.getComputedModels(this.cid, this.usr);
		}
		
		
		// for(String[] learner: class_list){
		// System.out.println("total students: "+class_list.size());
		for (Iterator<String[]> i = this.class_list.iterator(); i.hasNext();) {
			String[] learner = i.next();

			String learnerid = learner[0]; // the user login (username)
			String[] models = precomp_models.get(learnerid);
			if (models != null) {
				String model4topics = models[0];
				String model4content = models[1];
				String model4kc = models[2];
				if (model4topics != null && model4topics.length() > 0) {
					HashMap<String, double[]> learner_topic_levels = formatLevels(model4topics, this.nTopicLevels, false);
					
					if(removeZeroProgressStudents && !learnerid.equalsIgnoreCase(this.usr)) {
						
						boolean hasProgress = learner_topic_levels
							.values().stream()
							.anyMatch(arr -> Arrays.stream(arr).anyMatch(value -> value > 0));
			
						if(!hasProgress) {
							i.remove();
							continue;
						}
						
					}
					
					this.peers_topic_levels.put(learnerid, learner_topic_levels);
				}
				if (model4content != null && model4content.length() > 0) {
					if (learnerid.equalsIgnoreCase(this.usr)) {
						// userContentSequences = new HashMap<String, String>();
						HashMap<String, double[]> learner_content_levels = formatLevels(model4content, this.nContentLevels,
								true);
						this.peers_content_levels.put(learnerid, learner_content_levels);
					} else {
						HashMap<String, double[]> learner_content_levels = formatLevels(model4content, this.nContentLevels,
								false);
						this.peers_content_levels.put(learnerid, learner_content_levels);
					}

				}
				if (model4kc != null && model4kc.length() > 0) {
					HashMap<String, double[]> learner_kc_levels = formatLevels(model4kc, this.nKCLevels, false);
					this.peers_kc_levels.put(learnerid, learner_kc_levels);
				}
			} else {
				// take the non-activity users out, but leave the current user
				if (!includeNullStudents && !learnerid.equalsIgnoreCase(this.usr))
					i.remove();
			}

		}
		closeDBConnections();
		// @@@@ considering moving this out of here
		if (this.cm.agg_kcmap) {
			computeConceptCounts();
		}

	}

	public void computeGroupLevels(int top) {
		if (this.topN != -1)
			top = topN;
		
		if(doReportProgress()) {
			orderClassByProgress();
		} else {
			orderClassByKnowledge();
		}
		
		computeSubGroupsLevels(true, true, top);

		// comment following lines
		// computeAverageClassTopicLevels();
		// computeAverageTopStudentsTopicLevels(top);
		// computeAverageClassContentLevels();
		// computeAverageTopStudentsContentLevels(top);
	}
	
	public boolean doReportProgress() {
		String grpDefaultReportLevel = getGroupVisParameterMap().get("defValRepLvlId");
		String userDefaultReportLevel = getUserVisParameterMap().get("defValRepLvlId");
		String defaultReportLevel = userDefaultReportLevel != null ? userDefaultReportLevel:grpDefaultReportLevel;
		
		return defaultReportLevel == null || defaultReportLevel.equals("\"p\"");
	}

	//
	public HashMap<String, double[]> nullTopicLevels() {
		HashMap<String, double[]> res = new HashMap<String, double[]>();
		for (String[] topic : topicList) {
			String topic_id = topic[0];
			double[] levels = new double[nTopicLevels]; // @@@@
			res.put(topic_id, levels);
		}
		return res;
	}

	public HashMap<String, double[]> nullContentLevels() {
		HashMap<String, double[]> res = new HashMap<String, double[]>();
		for (Map.Entry<String, String[]> content : contentList.entrySet()) {
			String content_name = content.getKey();
			double[] levels = new double[nContentLevels]; // @@@@
			res.put(content_name, levels);
		}
		return res;
	}

	public HashMap<String, double[]> nullKCLevels() {
		HashMap<String, double[]> res = new HashMap<String, double[]>();
		if (allKCList != null) {
			for (Map.Entry<String, KnowledgeComponent> kcEntry : allKCList.entrySet()) {
				// String component_name = kcEntry.getKey();
				String component_name = kcEntry.getValue().getId() + "";
				double[] levels = new double[nKCLevels]; // @@@@
				res.put(component_name, levels);
			}
		}
		return res;
	}

	// take a string representing the levels in topics or contents (precomputed
	// model) and
	// returns a hashmap with the levels per topic/content
	public HashMap<String, double[]> formatLevels(String model, int nlevels, boolean lastElementIsAttSeq) {
		HashMap<String, double[]> res = new HashMap<String, double[]>();
		String[] model_arr = model.split("\\|");
		// System.out.println(" formatting model: "+model);
		for (int i = 0; i < model_arr.length; i++) {
			// System.out.println(model_arr[i]);
			String[] parts = model_arr[i].split(":");
			String name = parts[0];
			String[] str_levels = parts[1].split(",");
			double[] levels = new double[nlevels];
			int n = nlevels;
			if (str_levels.length < nlevels) {
				n = str_levels.length;
			}
			for (int j = 0; j < n; j++) {
				try {
					levels[j] = Double.parseDouble(str_levels[j]);
				} catch(Exception e) {
					e.printStackTrace();
				}
				
			}
			res.put(name, levels);

			if (lastElementIsAttSeq) {
				String attSeq = str_levels[str_levels.length - 1].replaceAll("~", ",").replaceAll("\"", "");
				if (userContentSequences != null)
					userContentSequences.put(name, attSeq);
			}
		}
		return res;
	}

	public void orderClassByProgress() {
		orderClassByScore(true);
	}

	public void orderClassByKnowledge() {
		orderClassByScore(false);
	}

	public void orderClassByScore(boolean usingProgress) {
		String learner1;
		String learner2;
		Map<String, double[]> learner1_levels;
		Map<String, double[]> learner2_levels;
		double learner1_sum = 0.0;
		double learner2_sum = 0.0;
		for (int i = 0; i < class_list.size() - 1; i++) {
			for (int j = 0; j < class_list.size() - 1; j++) {
				learner1 = class_list.get(j)[0];
				learner2 = class_list.get(j + 1)[0];
				learner1_levels = peers_topic_levels.get(learner1);
				learner2_levels = peers_topic_levels.get(learner2);
				learner1_sum = 0.0;
				learner2_sum = 0.0;
				// average across all topics. if no topic levels for the
				// students, returns 6 zeros
				double[] avgs1 = averageTopicLevels(learner1_levels, nTopicLevels);
				double[] avgs2 = averageTopicLevels(learner2_levels, nTopicLevels);

				for (int k = 0; k < nTopicLevels; k++) {
					if ((usingProgress && k % 2 == 1) || (!usingProgress && k % 2 == 0)) {
						learner1_sum += avgs1[k];
						learner2_sum += avgs2[k];

					}
				}
				// if learner 1 has lower average score tan learner 2, swap
				if (learner1_sum < learner2_sum) {
					// String[] tmp = class_list.get(j);
					// class_list.remove(j);
					// class_list.add(tmp);
					// System.out.println("swap "+learner1+" / "+learner2+" : "+learner1_sum + " < "
					// + learner2_sum);
					Collections.swap(class_list, j, j + 1);
				}
			}
		}
	}

	public static double[] averageTopicLevels(Map<String, double[]> topics, int nLevels) {
		double[] res = new double[nLevels];
		if (topics == null)
			return res;
		int i = 0;
		for (double[] levels : topics.values()) {
			for (int j = 0; j < nLevels; j++) {
				res[j] += levels[j];
			}
			i++;
		}
		if (i == 0)
			i = 1;
		for (int j = 0; j < nLevels; j++) {
			res[j] = res[j] / (1.0 * i);
		}

		return res;
	}

	// TODO nTop indicates
	public void computeSubGroupsLevels(boolean includeClassAverage, boolean includeTop, int nTop) {
		// System.out.println("computing subgroups");
		subgroups_topic_levels = new ArrayList<Map<String, double[]>>();
		subgroups_content_levels = new ArrayList<Map<String, double[]>>();
		subgroups_kc_levels = new ArrayList<Map<String, double[]>>();
		subgroups_student_ids = new ArrayList<ArrayList<String>>();
		subgroups_student_anonym_ids = new ArrayList<ArrayList<String>>();
		subgroups_names = new ArrayList<String>();
		// first subgroup is class average
		if (includeClassAverage) {
			ArrayList<String> allPeers = new ArrayList<String>();
			ArrayList<String> allPeersAnonym = new ArrayList<String>();
			for (int i = 0; i < class_list.size(); i++) {
				if (non_students.get(class_list.get(i)[0]) == null) {
					//
					allPeers.add(class_list.get(i)[0]);
					if (class_list.get(i)[0].equalsIgnoreCase(usr))
						allPeersAnonym.add(class_list.get(i)[0]);
					else
						allPeersAnonym.add(class_list.get(i)[3]);

					// else allPeers.add(class_list.get(i)[3]);
				}
			}

			subgroups_student_ids.add(allPeers);
			subgroups_student_anonym_ids.add(allPeersAnonym);
			subgroups_names.add("Class Average");
			subgroups_topic_levels.add(computeSubGroupTopicLevels(allPeers));
			subgroups_content_levels.add(computeSubGroupContentLevels(allPeers));
			subgroups_kc_levels.add(computeSubGroupKCLevels(allPeers));
		}

		// second subgroup is top students
		if (includeTop) {
			if (nTop < 1)
				nTop = 1;
			if (nTop > class_list.size())
				nTop = class_list.size();
			ArrayList<String> topPeers = new ArrayList<String>();
			ArrayList<String> topPeersAnonym = new ArrayList<String>();

			int i = 0;
			for (int j = 0; j < class_list.size() && i < nTop; j++) {
				String learner_id = class_list.get(j)[0];
				String learnerAnonymId = class_list.get(j)[3];
				if (non_students.get(learner_id) == null) {
					topPeers.add(learner_id);
					if (learner_id.equalsIgnoreCase(usr))
						topPeersAnonym.add(learner_id);
					else
						topPeersAnonym.add(learnerAnonymId);
					i++;
				}

			}

			subgroups_student_ids.add(topPeers);
			subgroups_student_anonym_ids.add(topPeersAnonym);
			subgroups_names.add("Top " + nTop);
			subgroups_topic_levels.add(computeSubGroupTopicLevels(topPeers));
			subgroups_content_levels.add(computeSubGroupContentLevels(topPeers));
			subgroups_kc_levels.add(computeSubGroupKCLevels(topPeers));

		}

		// other subgroups
		if (subgroups != null && subgroups.size() > 0) {
			for (String[] subgroup : subgroups) {
				String subgroupName = subgroup[0];
				if(!subgroupName.contains("_performance")){
					String[] peers = subgroup[1].split(",");
					ArrayList<String> sub_peers = new ArrayList<String>();
					ArrayList<String> sub_peers_anonym = new ArrayList<String>();
					for (String peer : peers) {
						sub_peers.add(peer);
						if (!peer.equalsIgnoreCase(usr))
							sub_peers_anonym.add(getAnonymIdByUser(peer));
						else
							sub_peers_anonym.add(peer);

					}
					if (sub_peers.size() > 0) {
						subgroups_student_ids.add(sub_peers);
						subgroups_student_anonym_ids.add(sub_peers_anonym);
						subgroups_names.add(subgroupName);
						subgroups_topic_levels.add(computeSubGroupTopicLevels(sub_peers));
						subgroups_content_levels.add(computeSubGroupContentLevels(sub_peers));
						subgroups_kc_levels.add(computeSubGroupKCLevels(sub_peers));
					}
				}else{
					List<String[]> studentList = class_list.stream()
							.filter(student -> non_students.containsKey(student[0]) == false)
							.collect(Collectors.toList());
					
					int halfIndex = studentList.size()/2;
					int oneThirdIndex = studentList.size()/3;
					
					ArrayList<String> perfomancePeers = new ArrayList<String>();
					ArrayList<String> performancePeersAnonym = new ArrayList<String>();
					
					//cskamil: TODO: The names below (e.g., higher performance/progress was given without thinking
					// the difference between progress and performance. Needs to be changed. 
					
					if(subgroupName.equals("higher_performance")){
						for (int j = 0; j < halfIndex; j++) {
							String learner_id = studentList.get(j)[0];
							String learnerAnonymId = studentList.get(j)[3];
							perfomancePeers.add(learner_id);
							if (learner_id.equalsIgnoreCase(usr))
								performancePeersAnonym.add(learner_id);
							else
								performancePeersAnonym.add(learnerAnonymId);
						}
						
						subgroups_names.add("Higher progress");

					} else if(subgroupName.equals("lower_performance")){
						for (int j = halfIndex; j < studentList.size(); j++) {
							String learner_id = studentList.get(j)[0];
							String learnerAnonymId = studentList.get(j)[3];
							perfomancePeers.add(learner_id);
							if (learner_id.equalsIgnoreCase(usr))
								performancePeersAnonym.add(learner_id);
							else
								performancePeersAnonym.add(learnerAnonymId);
						}

						subgroups_names.add("Lower progress");
					} else if(subgroupName.equals("lower_third_performance")){
						for (int j = oneThirdIndex*2; j < studentList.size(); j++) {
							String learner_id = studentList.get(j)[0];
							String learnerAnonymId = studentList.get(j)[3];
							perfomancePeers.add(learner_id);
							if (learner_id.equalsIgnoreCase(usr))
								performancePeersAnonym.add(learner_id);
							else
								performancePeersAnonym.add(learnerAnonymId);
						}

						subgroups_names.add("Lower third");
					} else if(subgroupName.equals("middle_third_performance")){
						for (int j = oneThirdIndex; j < oneThirdIndex*2; j++) {
							String learner_id = studentList.get(j)[0];
							String learnerAnonymId = studentList.get(j)[3];
							perfomancePeers.add(learner_id);
							if (learner_id.equalsIgnoreCase(usr))
								performancePeersAnonym.add(learner_id);
							else
								performancePeersAnonym.add(learnerAnonymId);
						}

						subgroups_names.add("Middle third");
					} else if(subgroupName.equals("higher_third_performance")){
						for (int j = 0; j < oneThirdIndex; j++) {
							String learner_id = studentList.get(j)[0];
							String learnerAnonymId = studentList.get(j)[3];
							perfomancePeers.add(learner_id);
							if (learner_id.equalsIgnoreCase(usr))
								performancePeersAnonym.add(learner_id);
							else
								performancePeersAnonym.add(learnerAnonymId);
						}

						subgroups_names.add("Higher third");
					}
					
					subgroups_student_ids.add(perfomancePeers);
					subgroups_student_anonym_ids.add(performancePeersAnonym);
					subgroups_topic_levels.add(computeSubGroupTopicLevels(perfomancePeers));
					subgroups_content_levels.add(computeSubGroupContentLevels(perfomancePeers));
					subgroups_kc_levels.add(computeSubGroupKCLevels(perfomancePeers));
				}
			}
		}
	}

	public String getAnonymIdByUser(String userId) {
		String res = null;
		for (String[] user : class_list) {
			if (user[0].equalsIgnoreCase(userId))
				return user[3];
		}

		return res;
	}

	public HashMap<String, double[]> computeSubGroupTopicLevels(ArrayList<String> learners) {
		HashMap<String, double[]> topicLevels = new HashMap<String, double[]>();

		for (String[] topic : topicList) {
			double[] avglevels = new double[nTopicLevels];
			int div = 0;
			for (String learner : learners) {
				if (non_students.get(learner) == null && !learner.equals(usr)) {
					double[] levels = null;
					Map<String, double[]> learner_topic_levels = peers_topic_levels.get(learner);
					// if the learner is not in the peer list (might never logged in in the system)
					if (learner_topic_levels != null) {
						levels = learner_topic_levels.get(topic[0]);
					}
					if (levels == null || levels.length == 0) {
						for (int j = 0; j < nTopicLevels; j++)
							avglevels[j] += 0.0;
					} else {
						for (int j = 0; j < nTopicLevels; j++)
							avglevels[j] += levels[j];
					}
					div++;
				}
			}
			if (div == 0)
				div = 1;
			for (int j = 0; j < nTopicLevels; j++)
				avglevels[j] = avglevels[j] / div;

			topicLevels.put(topic[0], avglevels);
		}
		return topicLevels;
	}

	public HashMap<String, double[]> computeSubGroupContentLevels(ArrayList<String> learners) {
		HashMap<String, double[]> contentLevels = new HashMap<String, double[]>();
		// aggs1_content_levels = new HashMap<String, double[]>();
		for (Map.Entry<String, String[]> content : contentList.entrySet()) {
			String content_name = content.getKey();
			double[] avglevels = new double[nContentLevels + 1]; // one more level to include how many students have
																	// attempted
			int div = 0;
			int attemptedCount = 0;
			for (String learner : learners) {
				if (non_students.get(learner) == null && !learner.equals(usr)) {
					double[] levels = null;
					Map<String, double[]> learner_levels = peers_content_levels.get(learner);
					if (learner_levels != null)
						levels = learner_levels.get(content_name);
					if (levels == null || levels.length == 0) {
						for (int j = 0; j < nContentLevels; j++)
							avglevels[j] += 0.0;
					} else {
						// for(int j=0;j<nContentLevels;j++) avglevels[j] += levels[j];

						// knowledge and progress are sum even if the user has not attempted
						avglevels[0] += levels[0]; // knowledge
						avglevels[1] += levels[1]; // progress

						// attempts, sr, etc. are summarized only for whom has tried
						if (levels[2] > 0) {
							// if(verbose) System.out.println("##### "+learner+" attepmts:"+levels[2]);
							avglevels[2] += levels[2]; // attempts
							avglevels[3] += levels[3]; // success rate
							avglevels[4] += levels[4]; // completion
							avglevels[5] += levels[5]; // annotations
							avglevels[6] += levels[6]; // likes
							avglevels[7] += levels[7]; // time
							avglevels[8] += levels[8]; // sub activities
							attemptedCount++;
						}
					}
				}
				div++;
			}
			if (div == 0)
				div = 1;
			avglevels[9] = attemptedCount; // store the number of students how attempted the activity

			if (attemptedCount == 0)
				attemptedCount = 1;
			// for(int j=0;j<nContentLevels;j++) avglevels[j] = avglevels[j]/div;
			avglevels[0] = avglevels[0] / div; // knowledge
			avglevels[1] = avglevels[1] / div; // progress

			// attempts, sr, etc. are summarized only for whom has tried
			avglevels[2] = avglevels[2] / attemptedCount; // attempts
			avglevels[3] = avglevels[3] / attemptedCount; // success rate
			avglevels[4] = avglevels[4] / attemptedCount; // completion
			avglevels[5] = avglevels[5] / attemptedCount; // annotations
			avglevels[6] = avglevels[6] / attemptedCount; // likes
			avglevels[7] = avglevels[7] / attemptedCount; // time
			avglevels[8] = avglevels[8] / attemptedCount; // sub activities

			contentLevels.put(content_name, avglevels);
		}
		return contentLevels;
	}

	public HashMap<String, double[]> computeSubGroupKCLevels(ArrayList<String> learners) {
		HashMap<String, double[]> kcLevels = new HashMap<String, double[]>();
		// aggs1_content_levels = new HashMap<String, double[]>();
		if (allKCList != null)
			for (Map.Entry<String, KnowledgeComponent> kcEntry : allKCList.entrySet()) {
				// String kc_name = kcEntry.getKey();

				KnowledgeComponent kc = kcEntry.getValue();
				String kc_id = kc.getId() + "";
				double[] avglevels = new double[nKCLevels];
				int div = 0;
				for (String learner : learners) {
					if (non_students.get(learner) == null && !learner.equals(usr)) {
						double[] levels = null;
						Map<String, double[]> learner_levels = peers_kc_levels.get(learner);
						if (learner_levels != null)
							levels = learner_levels.get(kc_id);
						if (levels == null || levels.length == 0) {
							for (int j = 0; j < nKCLevels; j++)
								avglevels[j] += 0.0;
						} else {
							for (int j = 0; j < nKCLevels; j++)
								avglevels[j] += levels[j];
						}
					}
					div++;
				}
				if (div == 0)
					div = 1;
				for (int j = 0; j < nKCLevels; j++)
					avglevels[j] = avglevels[j] / div;
				kcLevels.put(kc_id, avglevels);

			}

		return kcLevels;
	}

	public String precomputedTopicModel() {
		String user_levels = "";

		for (String[] topic : topicList) {

			double[] levels = userTopicLevels.get(topic[0]);
			if (levels != null && levels.length > 0) {
				user_levels += topic[0] + ":";
				for (int j = 0; j < nTopicLevels; j++)
					user_levels += df.format(levels[j]) + ",";
				user_levels = user_levels.substring(0, user_levels.length() - 1);
				user_levels += "|";
			}
		}

		if (user_levels.length() > 0) {
			user_levels = user_levels.substring(0, user_levels.length() - 1);
		}
		return user_levels;
	}

	public String precomputedContentModel() {
		String user_levels = "";
		for (Map.Entry<String, double[]> content : userContentLevels.entrySet()) {
			String content_name = content.getKey();
			double[] levels = content.getValue();
			if (levels != null && levels.length > 0) {
				user_levels += content_name + ":";
				for (int j = 0; j < nContentLevels; j++)
					user_levels += df.format(levels[j]) + ",";
				String attemptSeq = userContentSequences.get(content_name);
				if (attemptSeq == null)
					attemptSeq = "";
				user_levels += "\"" + attemptSeq.replaceAll(",", "~") + "\"";
				// user_levels = user_levels.substring(0, user_levels.length() - 1);
				user_levels += "|";
			}
		}
		if (user_levels.length() > 0) {
			user_levels = user_levels.substring(0, user_levels.length() - 1);
		}

		return user_levels;
	}

	public String precomputedKCModel() {
		String user_levels = "";
		for (Map.Entry<String, double[]> component : userKCLevels.entrySet()) {
			String component_name = component.getKey();
			double[] levels = component.getValue();
			if (levels != null && levels.length > 0) {
				user_levels += component_name + ":";
				for (int j = 0; j < nKCLevels; j++)
					user_levels += df2.format(levels[j]) + ",";
				user_levels = user_levels.substring(0, user_levels.length() - 1);
				user_levels += "|";
			}
		}
		if (user_levels.length() > 0) {
			user_levels = user_levels.substring(0, user_levels.length() - 1);
		}

		return user_levels;
	}

	public void storeComputedModel(String user) {
		String model4topics = this.precomputedTopicModel();
		String model4content = this.precomputedContentModel();
		String model4kc = this.precomputedKCModel();
		agg_db.storeComputedModel(user, cid, grp, sid, model4topics, model4content, model4kc);
	}

	public double getTopicDifficulty(String topic) {
		return 0.0;
	}

	public double getTopicImportance(String topic) {
		return 0.0;
	}

	public String precomputeClassModels() {
		String output = "";
		openDBConnections();
		sid = "UNKNOWN";
		for (String[] learner : class_list) {
			output += learner[0] + "\n";
			computeUserLevels(learner[0], grp, sid, cid, domain);
			// this.storePrecomputedModel(learner[0]);
			this.storeComputedModel(learner[0]);

		}
		closeDBConnections();
		return output;
	}

	// sequencing for the user (usr)
	// src: questions/examples

	public String getContentType(String content_name) {
		String[] content_data = contentList.get(content_name);
		if (content_data != null)
			return content_data[0];
		else
			return "";
	}

	public void genResourceMap() {
		if (resourceList == null)
			resourceMap = null;
		else {
			resourceMap = new HashMap<String, Integer>();

			for (int i = 0; i < resourceList.size(); i++) {
				// System.out.println(resourceList.get(i)[0]+" "+ i);
				resourceMap.put(resourceList.get(i)[0], i);
			}

		}
	}

	// TODO review
	
	public void updatePointsAndBadges(String last_content_id, String last_content_res, String last_content_seq,
			String last_content_old_progress) {
		int last_content_point = 0;
		boolean is_recommended = false;
		if (Double.parseDouble(last_content_res) > Double.parseDouble(last_content_old_progress)) { // check last progress with new progress to prevent give student point for repetitive solving
			String[] result = calculatePoint(last_content_id, last_content_res, last_content_seq);
			is_recommended = Boolean.valueOf(result[0]);
			last_content_point = Integer.parseInt(result[1]);
			String last_content_provider = getProviderByContentName(last_content_id);
			String badge_value;
			String badge_id;
			int newTotalRec= 0;

			openDBConnections();
			// update the total points and its Logs
			Logs recentLog = agg_db.getMostRecentLogForEachStudent(usr, grp);
			int newTotalPoint;
			if (recentLog != null) {
				newTotalPoint = Integer.parseInt(recentLog.getTotalPoint()) + last_content_point;
				// System.out.println("oldTotalPoint: " + recentLog.getTotalPoint());
			} else {
				newTotalPoint = last_content_point;
			}
			String description = "+" + String.valueOf(last_content_point) ;
			// " for activity "+ contentList.get(last_content_id)[1];
			if(last_content_provider.equalsIgnoreCase("pcrs"))
				description += " Coding Solved!";
			else if(last_content_provider.equalsIgnoreCase("pcex_ch"))
				description += " Challenge Solved!";
			else if(last_content_provider.equalsIgnoreCase("pcex"))
				description += " Example Line Clicked.";
			// System.out.println("description for getting point: "+ description);
			agg_db.insertRecentPoint(usr, grp, String.valueOf(last_content_point), description,
					String.valueOf(newTotalPoint));
			agg_db.insertRecentPointToHistoryTable(usr, grp, String.valueOf(last_content_point), description, String.valueOf(newTotalPoint), last_content_id, last_content_provider);
			// update the recommended badges if it is recommended and number of total_rec
			if (is_recommended) {
				String oldTotalRec = agg_db.getTotalRecForEachStudent(usr, grp);
				if (oldTotalRec == null)
					oldTotalRec = "0";
				newTotalRec = Integer.parseInt(oldTotalRec) + 1;
				agg_db.updateTotalRecForEachStudent(usr, grp, String.valueOf(newTotalRec));
				badge_value = "R" + String.valueOf(newTotalRec);
				// System.out.println("badge_value is :" + badge_value);
				badge_id = agg_db.getBadgeIDBasedOnValue(badge_value);
				if (!badge_id.equalsIgnoreCase("null")) {
					agg_db.insertNewBadgeForEachStudent(usr, grp, badge_id);
					
					global_new_badge_id.add(badge_id);
				}
			}

			int totalEx = 0;
			int totalCh = 0;
			int totalCo = 0;
			badge_value = null;
			badge_id = null;
			// calculating the total number of each content by iterating through
			// userContentLevels
			if (userContentLevels != null) {
				for (Entry<String, double[]> e : userContentLevels.entrySet()) {
					String content_id = e.getKey();
					String provider = getProviderByContentName(content_id);
					double progress = e.getValue()[1];
					if (progress > 0) {
						if(provider.equalsIgnoreCase("pcrs")) {
							totalCo += 1;
						}
						else if(provider.equalsIgnoreCase("pcex_ch")) {
							totalCh += 1;
						}
						else if(provider.equalsIgnoreCase("pcex")) {
							totalEx += 1;
						}	
					}
				}
			}
			System.out.println(
					"total_examples: " + totalEx + " total_challenges: " + totalCh + " total_codings: " + totalCo);

			if (last_content_point > 0) {
				if (last_content_provider.equalsIgnoreCase("pcrs")) {
					badge_value = "CO" + String.valueOf(totalCo);
				} else if (last_content_provider.equalsIgnoreCase("pcex_ch")) {
					badge_value = "CH" + String.valueOf(totalCh);
				} else if (last_content_provider.equalsIgnoreCase("pcex")) {
					badge_value = "E" + String.valueOf(totalEx);
				}
				// if student solve the determined number of examples or challenges or codings
				// their badges will be updated
				badge_id = agg_db.getBadgeIDBasedOnValue(badge_value);
				if (!badge_id.equals("null")) {
					agg_db.insertNewBadgeForEachStudent(usr, grp, badge_id);
					
					global_new_badge_id.add(badge_id);
				}
			}
			
			List<Badges> allBadges = agg_db.getBadgesForEachStudent(usr, grp);
			//this function check the existance of previous badges and may have not been added to the user profile
			checkExistanceOfPreviousBadges(allBadges, newTotalRec, totalCo, totalCh, totalEx);

			closeDBConnections();
		}
	}

	public String[] calculatePoint(String last_content_id, String last_content_res, String last_content_seq) {
		String[] res = new String[2]; // res[0] -> is_recommended, res[1] -> last_content_point
		double last_content_seq_double = Double.parseDouble(last_content_seq);
		double last_content_res_double = Double.parseDouble(last_content_res);
		String content_provider = getProviderByContentName(last_content_id);
		if (content_provider.equalsIgnoreCase("pcrs") || content_provider.equalsIgnoreCase("pcex_ch")) {
			if (last_content_res_double == 1) {
				if (last_content_seq_double == 1) {
					res[0] = "true";
					res[1] = "6";
				} else if (last_content_seq_double == 0.7) {
					res[0] = "true";
					res[1] = "4";
				} else if (last_content_seq_double == 0.3) {
					res[0] = "true";
					res[1] = "2";
				} else {
					res[0] = "false";
					res[1] = "1";
				}
			} else {
				res[0] = "false";
				res[1] = "0";
			}
		} else if (content_provider.equalsIgnoreCase("pcex")) {
			if (last_content_res_double > 0) {
				if (last_content_seq_double == 1) {
					res[0] = "true";
					res[1] = "6";
				} else if (last_content_seq_double == 0.7) {
					res[0] = "true";
					res[1] = "4";
				} else if (last_content_seq_double == 0.3) {
					res[0] = "true";
					res[1] = "2";
				} else {
					res[0] = "false";
					res[1] = "1";
				}
			} else {
				res[0] = "false";
				res[1] = "0";
			}
		}
		return res;
	}
	
	public void checkExistanceOfPreviousBadges(List<Badges>allBadges, int TotalRec, int TotalCo, int TotalCh, int TotalEx) {
		Map<String,Badges> badgeValues = new HashMap<>();
		for(Badges b: allBadges) {
			badgeValues.put(b.getValue(), b);
		}
		int[] recommended = {5,10,20,40,70,110,160,200};
		int[] coding = {1, 10, 25, 45};
		int[] challenge = {1, 10, 25, 45, 70};
		int[] example = {1, 10, 25 , 45};
		
		for(int i = 0; i< recommended.length; i++) {
			if(recommended[i] <= TotalRec) {
				if(badgeValues.get("R"+String.valueOf(recommended[i]))==null){
					String badge_id = agg_db.getBadgeIDBasedOnValue("R"+String.valueOf(recommended[i]));
					if(badge_id !=null) {
						agg_db.insertNewBadgeForEachStudent(usr, grp, badge_id);
						global_new_badge_id.add(badge_id);
					}
				}
			}
		}
		for(int i = 0 ; i< coding.length ; i++) {
			if(coding[i] <= TotalCo) {
				if(badgeValues.get("CO"+String.valueOf(coding[i]))==null){
					String badge_id = agg_db.getBadgeIDBasedOnValue("CO"+String.valueOf(coding[i]));
					if(badge_id != null) {
						agg_db.insertNewBadgeForEachStudent(usr, grp, badge_id);
						global_new_badge_id.add(badge_id);
					}
				}
			}
		}
		for(int i = 0 ; i< challenge.length; i++) {
			if(challenge[i] <= TotalCh) {
				if(badgeValues.get("CH"+String.valueOf(challenge[i]))==null){
					String badge_id = agg_db.getBadgeIDBasedOnValue("CH"+String.valueOf(challenge[i]));
					if(badge_id != null) {
						agg_db.insertNewBadgeForEachStudent(usr, grp, badge_id);
						global_new_badge_id.add(badge_id);
					}
				}
			}
		}
		for(int i = 0 ; i< example.length; i++) {
			if(example[i] <= TotalEx) {
				if(badgeValues.get("E"+String.valueOf(example[i]))==null){
					String badge_id = agg_db.getBadgeIDBasedOnValue("E"+String.valueOf(example[i]));
					if(badge_id !=null) {
					agg_db.insertNewBadgeForEachStudent(usr, grp, badge_id);
					global_new_badge_id.add(badge_id);
					}
				}
			}
		}
		
	}
	
	// TODO review
	// @@@@ FEEDBACK generation. For now it is hardcoded
	public void fillFeedbackForm(String last_content_id, String last_content_res) {
		String feedback_id = "" + (System.nanoTime() / 1000);

		activity_feedback_form_items = new ArrayList<ArrayList<String>>();

		// decide when to get the feedback
		// when last content visited was a question and was succeed
		if (last_content_id != null && last_content_id.length() > 0 && last_content_res.equals("1")) {
			activity_feedback_id = feedback_id;

			ArrayList<String> item1 = new ArrayList<String>();
			item1.add("content_difficulty"); // question id
			item1.add("How difficult was the content?"); // text
			item1.add("one"); // type
			item1.add("false"); // required
			item1.add("0;easy|1;medium|2;hard"); // response
			activity_feedback_form_items.add(item1);
		}
	}

	// @@@@ RECOMMENDATIONS getting the recommendations from the recommendation
	// interface
	// it will fill reactive recommendations and proactive scoring (sequencing)
	public void fillRecommendations(String last_content_id, String last_content_res, int n) {		// @@@ get the content provider
		String last_content_provider = "";
		if (last_content_id != null && last_content_id.length() > 0)
			last_content_provider = getProviderByContentName(last_content_id);
		// recommendation_list = um_interface.getRecommendations(usr, grp, sid, cid,
		// domain, last_content_id, last_content_res, last_content_provider, n,
		// contentList);
		
		String contentsString = getCommaSeparatedContentString(contentList);
		
		ArrayList<ArrayList<String[]>> all_rec = rec_interface.getRecommendations(usr, grp, sid, cid, domain,
				last_content_id, last_content_res, last_content_provider, contentsString, cm.agg_reactiverec_max,
				cm.agg_proactiverec_max, cm.agg_reactiverec_threshold, cm.agg_proactiverec_threshold,
				cm.agg_reactiverec_method, cm.agg_proactiverec_method, topicContent, userContentLevels);
		recommendation_list = new ArrayList<ArrayList<String>>();
		if (all_rec != null) {
			// reactive recommendations
			// recommendation_list = new ArrayList<ArrayList<String>>();
			if (cm.agg_reactiverec_enabled) {
				ArrayList<String[]> reactive_rec = all_rec.get(0);
				for (String[] rec : reactive_rec) {
					ArrayList<String> r = new ArrayList<String>();
					r.add(rec[0]); // rec item id
					String topic = "";
					// System.out.println(rec[2]);
					if (mapContentTopic != null)
						topic = mapContentTopic.get(rec[2]);
					if (topic == null)
						topic = "";
					r.add(topic); // topic id
					r.add(contentList.get(rec[2])[0]); // resource id
					r.add(rec[2]); // content id
					if (rec[3].length() > 5)
						r.add(rec[3].substring(0, 5)); // score
					else
						r.add(rec[3]);
					r.add("Do you think the above example will help you to solve the original problem?"); // feedback
																											// question
					r.add("-1"); // stored value of the feedback
					recommendation_list.add(r);
				}
			}

			// proactive recommendations
			if (cm.agg_proactiverec_enabled) {
				// System.out.println("Including sequencing");

				ArrayList<String[]> proactive_rec = all_rec.get(1);
				contentSequencingScores = new HashMap<String, Double>();
				topicSequencingScores = new HashMap<String, double[]>();

				for (String[] rec : proactive_rec) {
					double score = 0;
					try {
						score = Double.parseDouble(rec[3]);
					} catch (Exception e) {
					}
					contentSequencingScores.put(rec[2], score);
				}

				for (String[] topic_data : topicList) {
					ArrayList<String>[] topic_content = topicContent.get(topic_data[0]);
					double[] seqScores;
					if (topic_content == null) {
						seqScores = new double[resourceList.size()];
					} else {
						seqScores = new double[topic_content.length];
						for (int i = 0; i < topic_content.length; i++) {
							seqScores[i] = 0.0;
							ArrayList<String> contents = topic_content[i];
							for (String content_name : contents) {
								Double s = contentSequencingScores.get(content_name);
								if (s != null)
									if (s > seqScores[i]) {
										seqScores[i] = s;
									}
							}
						}

					}
					topicSequencingScores.put(topic_data[0], seqScores);
				}
			}
		}
	}
	
	// Step 4: call the student model asynchronously to update its belief
	// call student model only if the result is in valid range 0-1
	public void sendStudentModelUpdateRequest(String last_content_id, String last_content_res) {
		double result = Double.parseDouble(last_content_res);
		if((cm.agg_proactiverec_enabled && cm.agg_proactiverec_method.equals("bng")) || 
			(cm.agg_reactiverec_enabled && cm.agg_reactiverec_method.equals("pgsc") && result == 0.0) ||
			cm.agg_kcmap && cm.agg_kcmap_method.equals("bn")) {
			if (result >=0 && result <=1) {
				String event = "aggregate";
				String contentsString = getCommaSeparatedContentString(contentList);
				
				String params = createStudentModelUpdateParamJSON(usr, grp, cid, last_content_id, last_content_res, contentsString, event);
				sendPostRequest(params, cm.agg_bn_student_model_update_service_url, cm.agg_bn_student_model_request_sync);
			}
		}
	}
	
	private void sendPostRequest(String params, String URL, boolean sync) {
		try {
			if(sync) {
				HttpClient client = new HttpClient();
		        PostMethod method = new PostMethod(URL);
		        method.setRequestBody(params);
		        method.addRequestHeader("Content-type", "application/json");
		
		        int statusCode = client.executeMethod(method);
		        if (statusCode != -1) {
		        	InputStream in = method.getResponseBodyAsStream();
		        	org.json.JSONObject readJsonFromStream = PAWSRecInterface.readJsonFromStream(in);
		        }
			} else {
				HttpAsyncClientInterface.getInstance().sendHttpAsynchPostRequest(URL, params);	
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private String createStudentModelUpdateParamJSON(String usr, String grp,  String cid, String lastAct, String lastActResult, String contents,
			String event) {
		JSONObject json = new JSONObject();
		try {
			json.put("usr", usr);
			json.put("grp", grp);
			json.put("cid", cid);
			json.put("lastContentId", lastAct);
			json.put("lastContentResult", lastActResult);
			json.put("contents", contents);
			json.put("event", event);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json.toString();
	} 
	
	private String getCommaSeparatedContentString(HashMap<String, String[]> contentList) {
		String contents = "";
		for (String c : contentList.keySet())
			contents += c + ",";
		
		if(contents.length()>0) 
			contents = contents.substring(0, contents.length()-1); //this is for ignoring the last ,
		
		return contents;
	}

	public static int inStringArray(String[] array, String s) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equalsIgnoreCase(s))
				return i;
		}
		return -1;
	}

	public void mapContentToTopic() {
		mapContentTopic = new HashMap<String, String>();
		for (String[] topic : topicList) {
			ArrayList<String>[] oneTypeContents = topicContent.get(topic[0]);
			if (oneTypeContents != null) {
				for (int i = 0; i < oneTypeContents.length; i++) {
					for (String content_name : oneTypeContents[i]) {
						if (!mapContentTopic.containsKey(content_name)) {
							mapContentTopic.put(content_name, topic[0]);
						}
					}
				}

			}

		}

		// for (Map.Entry<String, String> content : mapContentTopic.entrySet()) {
		// String content_name = content.getKey();
		// String topic_name = content.getValue();
		// System.out.println(content_name + " IN " + topic_name);
		// }
	}

	// DEPRECATED
	public String[] getTopicByContentName(String content_name) {
		String[] res = null;
		for (String[] topic : topicList) {
			ArrayList<String>[] oneTypeContents = topicContent.get(topic[0]);
			ArrayList<String> questions = oneTypeContents[0];
			int i = 1;
			for (String question : questions) {
				if (content_name.equalsIgnoreCase(question)) {
					res = new String[2];
					res[0] = topic[0];
					res[1] = "" + i;
					return res;
				}
				i++;
			}
		}
		return null;
	}

	public String getProviderByContentName(String content_name) {
		String provider = "unknown";
		String[] contentData = contentList.get(content_name);
		if (contentData != null) {
			provider = contentData[5];
		}

		return provider;
	}

	// each String[] in the arraylist contains a set of 2 parameters string (comma
	// separated):
	// vis paameters and services parameters

	public void processParameters(ArrayList<String[]> parameters) {
		if (parameters == null)
			return;
		String manualFile = "";
		for (String[] p : parameters) {
			if (p[0].equalsIgnoreCase("user")) {
				userParameters = new String[2];
				if (p.length > 1 && p[1] != null)
					userParameters[0] = p[1];
				else
					userParameters[0] = "";
				if (p.length > 2 && p[2] != null)
					userParameters[1] = p[2];
				else
					userParameters[1] = "";
				if (p.length > 3 && p[3] != null)
					manualFile = p[3];
			}
			if (p[0].equalsIgnoreCase("group")) {
				groupParameters = new String[2];
				if (p.length > 1 && p[1] != null)
					groupParameters[0] = p[1];
				else
					groupParameters[0] = "";
				if (p.length > 2 && p[2] != null)
					groupParameters[1] = p[2];
				else
					groupParameters[1] = "";
				if (manualFile.length() == 0 && p.length > 3 && p[3] != null)
					manualFile = p[3];
			}
		}
		userManualFile = manualFile;
		if (verbose) {
			System.out.println("Getting Parameters");
			System.out.print("    userParameters : ");
			System.out.println((userParameters != null ? (userParameters[0] + " ||| " + userParameters[1]) : "NULL"));
			System.out.print("    groupParameters : ");
			System.out
					.println((groupParameters != null ? (groupParameters[0] + " ||| " + groupParameters[1]) : "NULL"));

		}

		// Overwrite the services configuration. Group and the user gives user's defined
		// parameters high priority
			
		if (groupParameters != null)
			overwriteServiceConfig(groupParameters[1]);
		if (userParameters != null)
			overwriteServiceConfig(userParameters[1]);
	}

	// parameters can be
	// proactive_rec=yes, reactive_rec=yes,
	// proactive_rec_threshold, reactive_rec_threshold,
	// proactive_rec_max, reactive_rec_max,
	// proactive_rec_method, reactive_rec_method,
	// TODO
	public void overwriteServiceConfig(String parameters) {
		if (parameters == null || parameters.length() == 0)
			return;
		String[] params = parameters.split(",");
		for (String param : params) {
			String[] pair = param.split(":");
			pair[0] = pair[0].trim();
			pair[1] = pair[1].trim();
			if (pair[0].equalsIgnoreCase("dataTopNGroup"))
				try {
					this.topN = Integer.parseInt(pair[1]);
				} catch (Exception e) {
				}
			if (pair[0].equalsIgnoreCase("dataIncOtherLearners"))
				this.includeOthers = pair[1].equalsIgnoreCase("true");

			if (pair[0].equalsIgnoreCase("proactiveRecOn"))
				cm.agg_proactiverec_enabled = pair[1].equalsIgnoreCase("true");
			if (pair[0].equalsIgnoreCase("reactiveRecOn"))
				cm.agg_reactiverec_enabled = pair[1].equalsIgnoreCase("true");
			if (pair[0].equalsIgnoreCase("proactiveRecThreshold"))
				try {
					cm.agg_proactiverec_threshold = Double.parseDouble(pair[1]);
				} catch (Exception e) {
				}
			if (pair[0].equalsIgnoreCase("reactiveRecThreshold"))
				try {
					cm.agg_reactiverec_threshold = Double.parseDouble(pair[1]);
				} catch (Exception e) {
				}
			if (pair[0].equalsIgnoreCase("proactiveRecMax"))
				try {
					cm.agg_proactiverec_max = Integer.parseInt(pair[1]);
				} catch (Exception e) {
				}
			if (pair[0].equalsIgnoreCase("reactiveRecMax"))
				try {
					cm.agg_reactiverec_max = Integer.parseInt(pair[1]);
				} catch (Exception e) {
				}
			if (pair[0].equalsIgnoreCase("proactiveRecMethod"))
				cm.agg_proactiverec_method = pair[1].trim();
			if (pair[0].equalsIgnoreCase("reactiveRecMethod"))
				cm.agg_reactiverec_method = pair[1].trim();
			if (pair[0].equalsIgnoreCase("lineRecOn"))
				try {
					cm.agg_line_rec_enabled = pair[1].equalsIgnoreCase("true");
				} catch (Exception e) {
				}
			
			if (pair[0].equalsIgnoreCase("reflectKcEstimateToItems"))
				try {
					cm.agg_reflect_kc_estimate_to_items = pair[1].equalsIgnoreCase("true");
				} catch (Exception e) {
				}

			if (pair[0].equalsIgnoreCase("kcMap"))
				try {
					cm.agg_kcmap = pair[1].equalsIgnoreCase("true");
				} catch (Exception e) {
				}
			if (pair[0].equalsIgnoreCase("kcMapMethod"))
				cm.agg_kcmap_method = pair[1].trim();
				
		}
	}

	public void processTopicStates(String[] topicStates) {
		if (topicStates != null) {
			if (topicStates.length > 0) {
				if (topicStates[0] == null)
					topicStates[0] = "";
			}
			if (topicStates.length > 1) {
				if (topicStates[1] == null)
					topicStates[1] = "";
			}
			for (String[] topic : topicList) {
				if (topicStates[1].contains(topic[0]))
					topic[4] = "covered";
				if (topicStates[0].contains(topic[0]))
					topic[4] = "current";
			}
		}
	}

	public boolean trackAction(String action, String comment) {
		boolean connection_was_open = false;
		try {
			connection_was_open = !agg_db.conn.isClosed();
		} catch (Exception e) {
		}
		boolean res = false;
		if (!connection_was_open)
			agg_db.openConnection();
		if (agg_db.insertTrackAction(usr, grp, sid, action, comment))
			res = true;
		if (!connection_was_open)
			agg_db.closeConnection();
		return res;
	}

	public void separateKCList(HashMap<String, KnowledgeComponent> allKCList) {
		singleKCList = new HashMap<String, KnowledgeComponent>();
		groupedKCList = new HashMap<String, KnowledgeComponentGroup>();
		for (Map.Entry<String, KnowledgeComponent> kcEntry : allKCList.entrySet()){
			KnowledgeComponent kc = kcEntry.getValue();
			// @@@@ for groups of KC, check if all kcs on the group are in the list
			if (kc instanceof KnowledgeComponentGroup) {
				KnowledgeComponentGroup kcG = ((KnowledgeComponentGroup) kc);
				ArrayList<KnowledgeComponent> innerKcs = kcG.getKcs();
				int countExisting = 0;
				for (KnowledgeComponent innerKc : innerKcs) {
					if (allKCList.get(innerKc.getIdName()) != null)
						countExisting++;
				}
				if (countExisting == kcG.cardinality())
					groupedKCList.put(kc.getIdName(), (KnowledgeComponentGroup) kc);
			} else
				singleKCList.put(kc.getIdName(), kc);

			for (Map.Entry<String, double[]> cEntry : kc.getContents().entrySet()) {
				// cEntry.getKey()
				String[] contentAttrs = contentList.get(cEntry.getKey());
				if (contentAttrs != null) {
					if (contentAttrs[6].length() == 0)
						contentAttrs[6] = kc.getId() + "";
					else
						contentAttrs[6] += "," + kc.getId();
				}

			}
		}

	}

	public void getContentStats() {
		// stats of the content
		statsContent = agg_db.getContentStats(this.domain);
	}

	public void computeConceptCounts() {
		if (contentList == null)
			return;
		if (!cm.agg_kcmap)
			return;

		activityConceptCounts = new HashMap<String, int[]>();

		for (Map.Entry<String, String[]> content : contentList.entrySet()) {
			String content_name = content.getKey();
			int[] levels = new int[3]; // @@@@ count concepts in three categories: not known, learning, mastered
			activityConceptCounts.put(content_name, levels);
		}
		
		// the model of the user
		Map<String, double[]> student_kc_l = peers_kc_levels.get(usr);
		if (student_kc_l == null)
			return;
		// go through all single KCs and get the level category. Then for each of its
		// activities, update the counting

		double[] levels = null;
		
		for (Map.Entry<String, KnowledgeComponent> kcEntry : singleKCList.entrySet()) {
			KnowledgeComponent kc = kcEntry.getValue();
			if (student_kc_l != null) {
				levels = student_kc_l.get(kc.getId() + "");
			}
			if (levels == null) {
				levels = new double[nKCLevels];
			}
			double k = levels[0];
			int cat = kc.getLevelCategory(k); // this is the category. 0: not known, 1: in learning, 2: known

			HashMap<String, double[]> contents = kc.getContents();
			if (contents != null && !contents.isEmpty()) {
				for (Map.Entry<String, double[]> content : contents.entrySet()) {
					String a = content.getKey();
					int[] counts = activityConceptCounts.get(a);
					// some concepts might be mapped to activities which are not in the course
					if (counts != null)
						counts[cat] = counts[cat] + 1; // increment 1 the count of the category of the knowledge of the
														// concept
				}
			}

		}
	}

	public String genJSONHeader() {
		String res = "{\n  version:\"0.0.3\",\n" + "  context:{ learnerId:\"" + usr + "\",group:{id:\"" + grp
				+ "\",name:\"" + grp_name + "\"}},\n"
				+ "  reportLevels:[{id:\"p\",name:\"Progress\"},{id:\"k\",name:\"Performance\"}],\n" + "  resources:[ \n";
		for (String[] r : resourceList) {
			if (r[4] == null || r[4].length() < 3)
				r[4] = "010";
			res += "    {id:\"" + r[0] + "\",name:\"" + r[1] + "\", " + "dim:{w:" + r[6] + ",h:" + r[7] + "},"
					+ "updateStateOn: {done: " + ((r[4].charAt(0) == '1') ? "true" : "false") + ", winClose: "
					+ ((r[4].charAt(1) == '1') ? "true" : "false") + ", winCloseIfAct: "
					+ ((r[4].charAt(2) == '1') ? "true" : "false") + "}},\n";
		}
		res = res.substring(0, res.length() - 2);

		res += "\n  ]";
		return res;
	}

	public String genJSONVisProperties() {
		String res = "vis:{\n  topicSizeAttr:[\"difficulty\",\"importance\"],\n  color:{binCount:7,value2color:function (x) { var y = Math.log(x)*0.25 + 1;  return (y < 0 ? 0 : y); }},";
		res += "\n  userManual:\"" + userManualFile + "\",";
		res += "\n  ui:{";
		res += "\n    params:{";
		res += "\n      group:{";
		if (groupParameters != null) {
			if (groupParameters[0] != null)
				res += groupParameters[0];
		}
		res += "},";
		res += "\n      user:{";
		
		Map<String, String> userParameterMap = getUserVisParameterMap();
		
		for (Map.Entry<String, String> parameterInfo : this.userPreferences.entrySet()) {
		    String parameterName = parameterInfo.getKey();
		    String parameterValue = "\"" + parameterInfo.getValue() + "\"";
		    userParameterMap.put(parameterName, parameterValue);
		}
		
		String joinedUserParameterString = userParameterMap.
				entrySet().stream().
				map(str -> str.toString().replace("=", ":")).collect(Collectors.joining(","));
		
		res += joinedUserParameterString;
				
		res += "}";
		res += "\n    }";
		res += "\n  }";
		res += "\n}";
		return res;
	}
	
	private Map<String, String> getUserVisParameterMap() {
		if(userVisParameterMap != null) {
			return userVisParameterMap;
		}
		
		userVisParameterMap = new HashMap<String, String>();
		
		if (userParameters != null) {
			if (userParameters[0] != null) {
				String[] userParams = userParameters[0].split(",");
				for(String param:userParams) {
					String[] keyValue = param.split(":");
					userVisParameterMap.put(keyValue[0], keyValue[1]);
				}
			}
		}
		
		return userVisParameterMap;
	}
	
	private Map<String, String> getGroupVisParameterMap() {
		if(groupVisParameterMap != null) {
			return groupVisParameterMap;
		}
		
		groupVisParameterMap = new HashMap<String, String>();
		
		if (groupParameters != null) {
			if (groupParameters[0] != null) {
				String groupParam = groupParameters[0];
				if(groupParam.contains("kcResouceIds")) {
					int startIndex = groupParam.indexOf("kcResouceIds");
					int endIndex = groupParam.indexOf("]", startIndex);
					String newParam = groupParam.substring(startIndex,endIndex+1).replaceAll(",",";");
					
					groupParam = groupParam.substring(0,startIndex) + newParam + groupParam.substring(endIndex+1);
				}
				
				String[] groupParams = groupParam.split(",");
				for(String param:groupParams) {
					String[] keyValue = param.split(":");
					groupVisParameterMap.put(keyValue[0], keyValue[1]);
				}
			}
		}
		
		return groupVisParameterMap;
	}
	
	public String genJSONConfigProperties() {
		String configProperties = "";
		try {
			JSONObject configuration = new JSONObject();
			
			configuration.put("agg_proactiverec_enabled", cm.agg_proactiverec_enabled);
			configuration.put("agg_proactiverec_threshold", cm.agg_proactiverec_threshold);
			configuration.put("agg_proactiverec_method", cm.agg_proactiverec_method);
			configuration.put("agg_proactiverec_max", cm.agg_proactiverec_max);
			configuration.put("agg_reactiverec_enabled", cm.agg_reactiverec_enabled);
			configuration.put("agg_reactiverec_threshold", cm.agg_reactiverec_threshold);
			configuration.put("agg_reactiverec_method", cm.agg_reactiverec_method);
			configuration.put("agg_reactiverec_max", cm.agg_reactiverec_max);
			configuration.put("agg_line_rec_enabled", cm.agg_line_rec_enabled);
			configuration.put("agg_kc_student_modeling", cm.agg_kcmap_method);
			
			configProperties = configuration.toString(4);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "configprops:" + configProperties;
	}

	public String genJSONTopics() {
		String topics = "  topics:[  \n";

		for (String[] topic : topicList) {
			String visible = "true";
			if (topic[3].equalsIgnoreCase("0"))
				visible = "false";
			topics += "  {\n    id:\"" + topic[0] + "\",name:\"" + topic[1] + "\",difficulty:"
					+ df.format(getTopicDifficulty(topic[1])) + ",importance:" + df.format(getTopicImportance(topic[1]))
					+ ",order:" + topic[2] + ",concepts:[";

			topics += "],isVisible:" + visible + ",\n";
			topics += "    timeline:{ covered:" + (topic[4].equalsIgnoreCase("covered") ? "true" : "false")
					+ ", current:" + (topic[4].equalsIgnoreCase("current") ? "true" : "false") + "},\n";
			topics += "    activities:{ \n";
			//

			ArrayList<String>[] content = topicContent.get(topic[0]);
			if (content == null) {
				for (int i = 0; i < resourceList.size(); i++) {
					String resourceName = resourceList.get(i)[0];
					topics += "      \"" + resourceName + "\":[],\n";
				}
			} else {
				String lineRecOn = "";
				if (cm.agg_line_rec_enabled)
					lineRecOn = "&lineRec=1";
				for (int i = 0; i < content.length; i++) {
					String resourceName = resourceList.get(i)[0];
					ArrayList<String> contentItems = content[i];
					topics += "      \"" + resourceName + "\":[\n";
					if (contentItems != null && contentItems.size() > 0) {
						for (String item : contentItems) {
							String[] content_data = this.contentList.get(item);
							topics += "        {id:\"" + item + "\",name:\"" + content_data[1] + "\",url:\""
									+ content_data[2] + lineRecOn + "&svc=masterygrids\"" + ",kcs:[" + content_data[6]
									+ "]";
							if (this.statsContent != null) {
								ContentStats stats = statsContent.get(item);
								if (stats != null) {
									topics += "," + stats.asPartialJSON();
								}
							}
							topics += "},\n";
						}
						topics = topics.substring(0, topics.length() - 2); // get rid of
						// the last
						// comma
					}
					topics += "\n      ],\n";

				}

			}
			topics = topics.substring(0, topics.length() - 2);

			topics += "\n    }\n  },\n";
		}

		topics = topics.substring(0, topics.length() - 2); // get rid of the
		// last comma
		// user_levels = user_levels.substring(0,user_levels.length()-1);
		topics += "\n  ]";
		return topics;
	}

	public String genJSONKCs() {
		String kcs = "  kcs:[  \n";
		if (singleKCList != null) {
			for (Map.Entry<String, KnowledgeComponent> kcEntry : singleKCList.entrySet()) {
				KnowledgeComponent kc = kcEntry.getValue();
				kcs += "    " + kc.toJsonString() + ",\n";
			}
		}
		kcs = kcs.substring(0, kcs.length() - 2);
		kcs += "\n  ],\n";
		kcs += "  kcgroups:[  \n";
		if (groupedKCList != null) {
			for (Map.Entry<String, KnowledgeComponentGroup> kcEntry : groupedKCList.entrySet()) {
				KnowledgeComponentGroup kc = kcEntry.getValue();
				kcs += "    " + kc.toJsonString() + ",\n";
			}
		}
		kcs = kcs.substring(0, kcs.length() - 2);

		kcs += "\n  ]";

		return kcs;
	}
	
	public String genJSONLearnerPreferences(String student) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("  preferences:[\n");
		
		Map<String, String> userPreference = this.groupPreferences.get(student);
		
		if(userPreference != null) {
			for(Entry<String,String> entry:userPreference.entrySet()) {
				builder.append("		{")
					   .append(entry.getKey()).append(":\"")
					   .append(entry.getValue()).append("\"},\n");
			}
			builder.deleteCharAt(builder.length()-2);
		}

		builder.append("   ]");
		return builder.toString();
	}

	public String genJSONLearnerState(String student) {

		String res = "  state:{\n";
		Map<String, double[]> student_t_l = null;
		if (peers_topic_levels != null)
			student_t_l = peers_topic_levels.get(student);

		Map<String, double[]> student_c_l = null;
		if (peers_content_levels != null)
			student_c_l = peers_content_levels.get(student);

		Map<String, double[]> student_kc_l = null;
		if (peers_kc_levels != null)
			student_kc_l = peers_kc_levels.get(student);

		String topic_levels = "      topics:{  \n";
		String content_levels = "      activities:{  \n";

		String seq = "";
		boolean sequencing = (student.equalsIgnoreCase(usr));

		for (String[] topic : topicList) {
			String topic_name = topic[0];
			double[] levels = null;
			String resourceName = "";
			seq = "";

			// Prepare the JSON for reporting content levels in each topic
			content_levels += "       \"" + topic_name + "\": {  \n";
			ArrayList<String>[] content = topicContent.get(topic_name);
			int[] nActByRes = null;
			int totalItems = 0;
			if (content != null && content.length > 0) {
				nActByRes = new int[content.length]; // for storing how many content items in each resource inside the
														// topic
				for (int i = 0; i < content.length; i++) {
					resourceName = resourceList.get(i)[0];
					ArrayList<String> contentItems = content[i];
					nActByRes[i] = contentItems.size();
					totalItems += nActByRes[i];
					content_levels += "        \"" + resourceName + "\":{";
					if (contentItems != null && contentItems.size() > 0) {
						content_levels += "\n";
						for (String item : contentItems) {
							// System.out.println("Q:"+q);
							seq = "";
							if (sequencing)
								seq = ",sequencing:" + df.format(getContentSequenceScore(item));
							if (student_c_l == null)
								levels = null;
							else
								levels = student_c_l.get(item);
							if (levels != null) {
								content_levels += "          \"" + item + "\": {values:{\"k\":" + df.format(levels[0])
										+ ",\"p\":" + df.format(levels[1]);
								if (student.equalsIgnoreCase(usr) && levels.length > 8) {
									content_levels += ",\"a\":" + df.format(levels[2]) + ",\"s\":"
											+ df.format(levels[3]) + ",\"t\":" + df.format(levels[7]);
									if (userContentSequences != null) {
										String attSeq = userContentSequences.get(item);
										if (attSeq != null)
											content_levels += ",\"aSeq\":\"" + attSeq + "\"";
									}
									if (activityConceptCounts != null && activityConceptCounts.size() > 0) {
										int[] counts = activityConceptCounts.get(item);
										if (counts == null)
											counts = new int[3];
										double diff = 0.5;
										if (counts[0] + counts[1] + counts[2] > 0) {
											diff = (0 * counts[2] + .5 * counts[1] + 1 * counts[0])
													/ (counts[0] + counts[1] + counts[2]);
										}

										content_levels += ",\"kcsNotKnown\":" + df.format(counts[0])
												+ ",\"kcsLearning\":" + df.format(counts[1]) + ",\"kcsKnown\":"
												+ df.format(counts[2]) + ",\"difficulty\":" + df.format(diff);
									}

								}
								content_levels += "}" + seq + "},\n";
							} else {
								content_levels += "          \"" + item + "\": {values:{\"k\":0,\"p\":0}" + seq
										+ "},\n";
							}

							// content_levels += " {},\n";
						}
						content_levels = content_levels.substring(0, content_levels.length() - 2); // get rid of the
																									// last comma
						content_levels += "\n        },\n";
					} else {
						content_levels += "},\n";
					}

				}

			} else {
				nActByRes = new int[resourceList.size()];
			}
			content_levels = content_levels.substring(0, content_levels.length() - 2);
			content_levels += "\n       },\n";

			// prepare the JSON for reporting the overall topic levels for each resource
			// and add the average across resources weighting by number of content items
			seq = "";
			if (sequencing) {
				seq = ",sequencing:{  ";
				for (int i = 0; i < resourceList.size(); i++) {
					resourceName = resourceList.get(i)[0];

					seq += "\"" + resourceName + "\":" + df.format(getTopicSequenceScore(topic_name, resourceName))
							+ " ,";

				}
				seq = seq.substring(0, seq.length() - 2);
				seq += "}";
			}
			if (student_t_l != null)
				levels = student_t_l.get(topic_name);
			if (levels != null) {
				topic_levels += "       \"" + topic_name + "\": {values:{  ";
				double overallK = 0.0;
				double overallP = 0.0;
				if (totalItems == 0)
					totalItems = 1;
				for (int i = 0; i < resourceList.size(); i++) {
					resourceName = resourceList.get(i)[0];
					overallK += levels[2 * i] * nActByRes[i] / totalItems;
					overallP += levels[2 * i + 1] * nActByRes[i] / totalItems;
					topic_levels += "\"" + resourceName + "\":{\"k\":" + df.format(levels[2 * i]) + ",\"p\":"
							+ df.format(levels[2 * i + 1]) + "},";
				}

				topic_levels = topic_levels.substring(0, topic_levels.length() - 1);
				topic_levels += "}" + seq + ", overall:{\"k\":" + df.format(overallK) + ",\"p\":" + df.format(overallP)
						+ "}},\n";

			} else {
				topic_levels += "       \"" + topic_name + "\": {values:{  ";
				for (int i = 0; i < resourceList.size(); i++) {
					resourceName = resourceList.get(i)[0];
					topic_levels += "\"" + resourceName + "\":{\"k\":" + df.format(0) + ",\"p\":" + df.format(0) + "},";
				}
				topic_levels = topic_levels.substring(0, topic_levels.length() - 1);

				topic_levels += "}" + seq + ", overall:{\"k\":" + df.format(0) + ",\"p\":" + df.format(0) + "} },\n";
			}

		}

		content_levels = content_levels.substring(0, content_levels.length() - 2);
		topic_levels = topic_levels.substring(0, topic_levels.length() - 2);

		topic_levels += "\n      }";
		content_levels += "\n      }";

		res += topic_levels + ",\n" + content_levels;

		
		if (student.equalsIgnoreCase(usr) && cm.agg_kcmap) {
			
			String kc_levels = "    kcs:{  \n";

			double[] levels = null;

			if (allKCList != null) {
				for (Map.Entry<String, KnowledgeComponent> kcEntry : allKCList.entrySet()) {
					KnowledgeComponent kc = kcEntry.getValue();
					if (student_kc_l != null) {
						levels = student_kc_l.get(kc.getId() + "");
					}
					if (levels == null) {

						levels = new double[nKCLevels];

					}
					System.out.println(Arrays.toString(levels));
					if (kcJSONById) {
						kc_levels += "       " + kc.getId() + ": {\"k\":" + df.format(levels[0]) + ",\"p\":"
								+ df.format(levels[1]) + ",\"sr\":" + df.format(levels[3]) + ",\"lastk-att\":"
								+ df.format(levels[10]) + ",\"lastk-sr\":" + df.format(levels[11]) +"},\n";
					} else
						kc_levels += "       \"" + kc.getIdName() + "\": {\"k\":" + df.format(levels[0]) + ",\"p\":"
								+ df.format(levels[1]) + ",\"sr\":" + df.format(levels[3]) + ",\"lastk-att\":"
								+ df.format(levels[10]) + ",\"lastk-sr\":" + df.format(levels[11]) + "},\n";

				}
			}

			kc_levels = kc_levels.substring(0, kc_levels.length() - 2);
			kc_levels += "\n    }";
			res += ",\n" + kc_levels;
		}

		res += "\n   }";

		return res;
	}

	//
	public String genJSONGroupState(Map<String, double[]> aggs_t_l, Map<String, double[]> aggs_c_l,
			Map<String, double[]> aggs_kc_l, ArrayList<String> learner_ids) {
		String res = "  state:{  \n";

		String topic_levels = "    topics:{  \n";
		String content_levels = "    activities:{  \n";

		String resourceName = null;

		for (String[] topic : topicList) {
			String topic_name = topic[0];
			double[] levels = null;
			if (aggs_t_l != null)
				levels = aggs_t_l.get(topic_name);
			// level k,p per topic
			if (levels != null) {
				topic_levels += "       \"" + topic_name + "\": {values:{  ";
				for (int i = 0; i < resourceList.size(); i++) {
					resourceName = resourceList.get(i)[0];
					topic_levels += "\"" + resourceName + "\":{\"k\":" + df.format(levels[2 * i]) + ",\"p\":"
							+ df.format(levels[2 * i + 1]) + "},";
				}
				topic_levels = topic_levels.substring(0, topic_levels.length() - 1);
				topic_levels += "}},\n";

			} else {
				topic_levels += "       \"" + topic_name + "\": {values:{  ";
				for (int i = 0; i < resourceList.size(); i++) {
					resourceName = resourceList.get(i)[0];
					topic_levels += "\"" + resourceName + "\":{\"k\":" + df.format(0) + ",\"p\":" + df.format(0) + "},";
				}
				topic_levels = topic_levels.substring(0, topic_levels.length() - 1);
				topic_levels += "}},\n";
			}

			content_levels += "      \"" + topic_name + "\": { \n";
			ArrayList<String>[] content = topicContent.get(topic_name);
			if (content != null) {
				for (int i = 0; i < content.length; i++) {
					resourceName = resourceList.get(i)[0];
					ArrayList<String> contentItems = content[i];
					content_levels += "        \"" + resourceName + "\":{  ";

					if (contentItems != null && contentItems.size() > 0) {
						content_levels += "\n";
						for (String item : contentItems) {
							if (aggs_c_l == null)
								levels = null;
							else
								levels = aggs_c_l.get(item);
							if (levels != null) {

								content_levels += "          " + "\"" + item + "\": {values:{\"k\":"
										+ df.format(levels[0]) + ",\"p\":" + df.format(levels[1]) + ",\"a\":"
										+ (levels[2] >= 0 ? df.format(levels[2]) : 0) + ",\"s\":"
										+ (levels[3] >= 0 ? df.format(levels[3]) : 0) + ",\"t\":"
										+ (levels[7] >= 0 ? df.format(levels[7]) : 0) + ",\"n\":" + df.format(levels[9])
										+ "}},\n";
								// content_levels += ",\"a\":" + df.format(levels[2]) + ",\"s\":" +
								// df.format(levels[3]) + ",\"t\":" + df.format(levels[7]);
							} else {
								content_levels += "          " + "\"" + item + "\": {values:{\"k\":0,\"p\":0}},\n";
							}

						}
						content_levels = content_levels.substring(0, content_levels.length() - 2); // get rid of the
																									// last comma
						content_levels += "\n        },\n";
					} else {
						content_levels += "},\n";
					}

				}
			}
			content_levels = content_levels.substring(0, content_levels.length() - 2);
			content_levels += "\n      },\n";
		}
		content_levels = content_levels.substring(0, content_levels.length() - 2);
		topic_levels = topic_levels.substring(0, topic_levels.length() - 2);

		topic_levels += "\n    }";
		content_levels += "\n    }";
		String kc_levels = "";

		if (cm.agg_kcmap) {
			kc_levels = "    kcs:{ \n";
			if (allKCList != null) {
				for (Map.Entry<String, KnowledgeComponent> kcEntry : allKCList.entrySet()) {
					KnowledgeComponent kc = kcEntry.getValue();
					double[] levels = aggs_kc_l.get(kc.getId() + "");
					if (levels == null) {
						levels = new double[nKCLevels];
						// System.out.println("levels for group not found!!");
					}
					if (kcJSONById) {
						kc_levels += "       " + kc.getId() + ": {\"k\":" + df.format(levels[0]) + ",\"p\":"
								+ df.format(levels[1]) + "},\n";
					} else
						kc_levels += "       \"" + kc.getIdName() + "\": {\"k\":" + df.format(levels[0]) + ",\"p\":"
								+ df.format(levels[1]) + "},\n";

				}
			}

			kc_levels = kc_levels.substring(0, kc_levels.length() - 2);

			kc_levels += "\n    }";
		}
		String learnersids = "learnerIds:[  ";
		for (String studentid : learner_ids) {
			learnersids += "\"" + studentid + "\", ";
		}
		learnersids = learnersids.substring(0, learnersids.length() - 2);

		learnersids += "]";
		res += topic_levels + ",\n" + content_levels + (cm.agg_kcmap ? ",\n" + kc_levels : "") + "\n },\n  "
				+ learnersids;

		return res;
	}

	public String genJSONRecommendation() {
		String res = "  recommendation:[\n";
		if (recommendation_list != null && recommendation_list.size() > 0) {
			for (ArrayList<String> rec : recommendation_list) {
				String stored_value = "-1";
				if (rec.get(6) != null)
					stored_value = rec.get(6);
				res += "    {recommendationId:\"" + rec.get(0) + "\",topicId:\"" + rec.get(1) + "\",resourceId:\""
						+ rec.get(2) + "\",activityId:\"" + rec.get(3) + "\",score:" + rec.get(4) + ",feedback:{text:\""
						+ rec.get(5) + "\", storedValue:" + stored_value + "}},\n";
			}
			res = res.substring(0, res.length() - 2);
		}

		res += "\n  ],";
		res += "\n  recommendationKC:[\n";
		if (this.recommendedActivitiesKCModeler != null && recommendedActivitiesKCModeler.size() > 0) {
			int i = 1;
			for (ArrayList<String[]> recs : recommendedActivitiesKCModeler) {
				res += "    {\"model" + i + "\" : [ ";
				for (String[] rec : recs) {
					res += "{\"qz\":\"" + rec[0] + "\",\"ex\":\"" + rec[1] + "\"},";
				}
				res = res.substring(0, res.length() - 1);
				res += "]},\n";

				i++;

			}
			res = res.substring(0, res.length() - 2);
		}

		res += "\n  ]";

		return res;
	}

	public String genJSONFeedback() {
		String res = "  feedback:{\n";
		if (activity_feedback_form_items != null && activity_feedback_form_items.size() > 0) {
			// the activity_feedback_form_id
			res += "    id:\"" + activity_feedback_id + "\",\n    items:[\n";
			for (ArrayList<String> fed : activity_feedback_form_items) {
				res += "      {id:\"" + fed.get(0) + "\",text:\"" + fed.get(1) + "\",type:\"" + fed.get(2)
						+ "\",required:\"" + fed.get(3) + "\",\n          response:[";
				String[] _response_items = fed.get(4).split("\\|");
				if (_response_items != null && _response_items.length > 0) {
					for (int i = 0; i < _response_items.length; i++) {
						String[] _response = _response_items[i].split(";");
						if (_response != null && _response.length == 2) {
							res += "{value:" + _response[0] + ",label:\"" + _response[1] + "\"},";
						}
					}
					res = res.substring(0, res.length() - 1);
				}
				res += "]},\n";
			}
			res = res.substring(0, res.length() - 2);
			res += "\n    ]\n";
		}

		res += "  }";
		return res;
	}

	// generate JSON output for all the data!!!!
	public String genAllJSON(int n, int top) {

		String header = genJSONHeader();
		String visprop = genJSONVisProperties();
		String configprop = genJSONConfigProperties();
		String topics = genJSONTopics();
		String kcs = genJSONKCs();
		
		String learners = getLearners(n);
		String aggs_levels = getAggLevels();

		String logs = genLogJSON();
		String badges = genBadgeJSON(true);
		String recCounts = genTotalRecJSON();

		return header + ",\n" + topics + ",\n" + (cm.agg_kcmap ? (kcs + ",\n") : "") + learners + ",\n" + aggs_levels
				+ ",\n" + visprop + ",\n" + configprop + ",\n" + logs + ",\n" + badges + ",\n" + recCounts + "\n}";
	}

	// REVIEW generate the main JSON response for the logged in user
	public String genUserJSON(boolean computeGroupLevels, String last_content_id, String last_content_res) {
		String output = "{\n  lastActivityId:\"" + last_content_id + "\",\n  lastActivityRes:" + last_content_res
				+ ",\n  learner:{\n    id:\"" + usr + "\",name:\"" + usr_name + "\",\n";
		output += genJSONLearnerState(usr);
		output += "\n  },\n"; // closing learner object
		
		String aggs_levels = getAggLevels();

		output += aggs_levels + ",\n";
		
		if(computeGroupLevels) {
			String learners = getLearners(-1);
			output += learners + ",\n";
		}
		
		output += genJSONRecommendation() + ",\n";
		output += genJSONFeedback() + ", \n";
		output += genLogJSON() + ",\n";
		output += genBadgeJSON(false) + ",\n";
		output += genTotalRecJSON();
		output += "\n}";
		return output;
	}
	
	private String getLearners(int n) {
		if (!this.includeOthers)
			n = 0;
		
		String learners = "learners:[ \n";
		int c = 0;
		for (String[] learner : class_list) {
			String ishidden = "false";
			if (non_students.get(learner[0]) != null)
				ishidden = "true";
			if (c < n - 1 || learner[0].equalsIgnoreCase(usr) || n == -1) {
				learners += "{\n  id:\"" + (learner[0].equalsIgnoreCase(usr) ? learner[0] : learner[3]) + "\",name:\""
						+ learner[1] + "\",isHidden:" + ishidden + ",\n  " + genJSONLearnerState(learner[0]) + ",\n " +
						genJSONLearnerPreferences(learner[0]) + "\n},\n";
			}
			c++;
		}
		learners = learners.substring(0, learners.length() - 2);
		learners += "\n]";
		
		return learners;
	}
	
	private String getAggLevels() {
		String aggs_levels = "groups:[ \n";
		if(subgroups_names != null) {
			for (int g = 0; g < subgroups_names.size(); g++) {
				String subgroup = "{\n  name:\"" + subgroups_names.get(g) + "\",\n";

				subgroup += genJSONGroupState(subgroups_topic_levels.get(g), subgroups_content_levels.get(g),
						subgroups_kc_levels.get(g), subgroups_student_anonym_ids.get(g)) + "\n},\n";
				aggs_levels += subgroup;
			}
			aggs_levels = aggs_levels.substring(0, aggs_levels.length() - 2);
		}
		
		aggs_levels += "]";
		
		return aggs_levels;
	}

	
	public String genLogJSON() {
		openDBConnections();
		String output = "logs:[\n  ";
		List<Logs> recentPoints = new ArrayList<Logs>();
		recentPoints = agg_db.getFiveRecentPoints(usr, grp);
		if(recentPoints != null) {
			for (Logs log : recentPoints) {
				if(Integer.parseInt(log.getTotalPoint()) > 0)
					output += "{ value: \"" + log.getValue() + "\", description: \"" + log.getDescription()
						+ "\", totalPoint: \"" + log.getTotalPoint() + "\"},\n";
			}
		}
		
		output = output.substring(0, output.length() - 2);
		output += "\n]";
		// closeDBConnections();
		return output;
	}

	public String genBadgeJSON(boolean allOrUser) { // true for mod=all, false for mod=user
		// openDBConnections();
		String output = "ownBadges:[\n  ";
		if (allOrUser) {
			List<Badges> badges = new ArrayList<Badges>();
			badges = agg_db.getBadgesForEachStudent(usr, grp);
			for (Badges bdg : badges) {
				output += "{id: \"" + bdg.getId() + "\", value: \"" + bdg.getValue() + "\", name: \"" + bdg.getName()
						+ "\", type: \"" + bdg.getType() + "\", img_URL: \"" + bdg.getImgURL()
						+ "\", congradulationMSG: \"" + bdg.getCongradualationMSG() + "\" },\n";
			}

			;
		}else if (!allOrUser && global_new_badge_id.size() > 0) {
			for(String bdgID: global_new_badge_id) {
				Badges bdg = agg_db.getBadgeById(bdgID);
				output += "{id: \"" + bdg.getId() + "\", value: \"" + bdg.getValue() + "\", name: \"" + bdg.getName()
						+ "\", type: \"" + bdg.getType() + "\", img_URL: \"" + bdg.getImgURL() + "\", congradulationMSG: \""
						+ bdg.getCongradualationMSG() + "\" },\n";
			}
		}

		output = output.substring(0, output.length() - 2);
		output += "\n]";
		// closeDBConnections();
		return output;
	}

	public String genTotalRecJSON() {
		// openDBConnections();
		String output = "rmcCount:{";
		String totalSolvedRecommendedActivity = agg_db.getTotalRecForEachStudent(usr, grp);
		if (totalSolvedRecommendedActivity != null)
			output += "value: \"" + totalSolvedRecommendedActivity + "\"}\n";
		else
			output += "}\n";
		closeDBConnections();
		return output;
	}
}

