//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Arrays;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.Map;
//import java.util.Set;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import smile.Network;

//http://localhost:8080/Aggregate/GetContentLevels?usr=adl01&grp=ADL&sid=test&cid=13&mod=user

public class KCModeler {
	
	private boolean verbose = false;
	private boolean filterImpForSingleKC = true;
	private boolean filterImpForGroupedKC = false;
	private int contributesMatch = 1; //Here in kc_content_component, we only put QuizJET questions with 1, so this field can be used to keep only QuizJET questions.
//	private boolean forceKnowResultCons = true; //for adding heuristics
//	private double MIN_BN_KNOWLEDGE = 0.001; //for adding heuristic
//	private Set<String> bnNodes; 
	
	private ArrayList<ArrayList<String[]>> recommendedActivities;
	
	private String usr;
	private String grp;
	private String cid;
	
	private int nKCLevels;
	
	private HashMap<String,KnowledgeComponent> singleKCList;
	private HashMap<String,KnowledgeComponentGroup> groupedKCList;
	private HashMap<String, String[]> contentList; // this is the whole list of content in the course
	
	private String method;
	
	private String domain;
	private ServletContext context;
	//private HashMap<String, double[]> previousKCLevels;
//	private static final String bnFolder = "bn";
	
	private String conceptLevelsServiceURL = "http://localhost/cbum/ReportManager";
//	private String conceptLevelsServiceURL = "http://localhost:8080/cbum/ReportManager";
	
	
	
	
	/**
	 * Constructor 
	 * @param usr
	 * @param grp
	 * @param cid
	 * @param singleKCList
	 * @param groupedKCList
	 * @param method
	 * @param domain
	 * @param context
	 */
	public KCModeler(String usr, String grp, String cid,
			HashMap<String, KnowledgeComponent> singleKCList,
			HashMap<String, KnowledgeComponentGroup> groupedKCList,
			HashMap<String, String[]> contentList,
			String method, String domain, ServletContext context, int nKCLevels){
			//HashMap<String, double[]> previousKCLevels) {
		super();
		this.usr = usr;
		this.grp = grp;
		this.cid = cid;
		this.singleKCList = singleKCList; //from kc_component KC, kc_content_component
		this.groupedKCList = groupedKCList; //from kc_component KC, kc_content_component
		this.contentList = contentList;
		this.method = method;
		this.context = context;
		this.domain = domain;
		this.nKCLevels = nKCLevels;
		recommendedActivities = null;
		//this.previousKCLevels = previousKCLevels;
	}

	
	/*
	 * Overall both methods' debug:
	 * -- when there is not activity (since a date), does it work?
	 * 
	 */

	
	/* Input: 
	 * -- "allActivities": 
	 *    -- HashMap: "String" is the actual name, same as the getName from Activity
	 *    -- It has all the activity from the time the lastKCModel was computed. Corresponds to PAWSUMInterfaceV2
	 *    -- It can contain contents outside the course (e.g., from pretest), outside the group, examples, but only those that current user has ever attempted.
	 * -- "lastKCModel: 
	 *    -- HashMap: "String" is the int id converted to string; "double[]" corresponds to nKCLevels (5 levels (K,P,N,S,T) knowledge,progress,N attempts, success rate, time), but only needs to fill the first two levels;
	 *    -- If lastKCModel is null or empty, or lastUpdateDate is null or empty, then "activity" contains all activity of the user
	 *       -- [DISCUSS] isn't lastKCModel enough?
	 *    -- #of KCs should be the same as the active ones in kc_component table
	 * 
	 * Returned value: 
	 * -- HashMap: "String" is the int id converted to string; "double[]" corresponds to nKCLevels (5 levels (K,P,N,S,T) knowledge,progress,N attempts, success rate, time), but only needs to fill the first two levels;
	 * 
	 * TODO 1st, 2nd pt:
	 * -- See the google doc: https://docs.google.com/document/d/1v6BP_zQdCoNfxQXlA-CcpzyJ-hu35e4W2aL9ceTT_Pw/edit#
	 * 
	 * TODO: 3nd pt:
	 * -- For those todo within coding lines
	 * -- Change some variables to static type
	 * -- [DISCUSS] only include a grouped KC if its main component single kc is included (active and important) from the very beginning retrieving all kcs
	 * 
	 * */
	
//	public HashMap<String, double[]> computeKCModel(String lastUpdateDate, HashMap<String, double[]> lastKCModel, 
//			HashMap<String, Activity> allActivities, ArrayList<Attempt> lastAttempts){
//		try{
//			System.out.println("#####################\nCOMPUTING KC K,P USING METHOD "+this.method);
//			if (lastAttempts == null || lastAttempts.isEmpty()) {
//				System.out.println("\tlastAttempts null or empty! Return the the passed lastKCModel!");
//				return lastKCModel;
//			}
//		
//	        InputStream bnContent = context.getResourceAsStream("./" + bnFolder + "/" + domain + ".xdsl");
//	
//			// single kc knowledge part1 from bn, add heuristic: pass kc priors and activity list to bn; return per kc knowledge level (first level; hashmap)
//			HashMap<String, double[]> passedKCModel = lastKCModel;
//			if (lastKCModel == null || lastKCModel.isEmpty() || lastUpdateDate == null || lastUpdateDate.isEmpty()) passedKCModel = null;
//			HashSet<String> bnSingleKCIdList = new HashSet<String>();
//			
//			HashMap<String, Double> singleKCIdToBNKnowledge = computeBNKnowledge(passedKCModel, lastAttempts, bnContent, bnSingleKCIdList); 
//			
//			HashMap<String,KnowledgeComponent> bnSingleKCList = new HashMap<String,KnowledgeComponent>();
//			for (Map.Entry<String, KnowledgeComponent> kcEntry : singleKCList.entrySet()) 
//				if (bnSingleKCIdList.contains(kcEntry.getValue().getId() + "") )
//					bnSingleKCList.put(kcEntry.getKey(), kcEntry.getValue());
//	
//			return computeCombinedKCLevels(allActivities, groupedKCList, bnSingleKCList, singleKCIdToBNKnowledge);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//		}
//		return nullKCLevels();
//	}
	
	
//	public HashMap<String, double[]> computeKCModelComparison(String lastUpdateDate, HashMap<String, double[]> lastKCModel, 
//			HashMap<String, Activity> allActivities, ArrayList<Attempt> lastAttempts){
//		
//		this.recommendedActivities = new ArrayList<ArrayList<String[]>>();
//		ArrayList<String[]> model1 = new ArrayList<String[]>();
//		model1.add(new String[]{"prob1","ex1"});
//		model1.add(new String[]{"prob2","ex2"});
//		model1.add(new String[]{"prob3","ex3"});
//				
//		ArrayList<String[]> model2 = new ArrayList<String[]>();
//		model2.add(new String[]{"prob4","ex4"});
//		model2.add(new String[]{"prob5","ex5"});
//		model2.add(new String[]{"prob6","ex6"});
//			
//		recommendedActivities.add(model1);
//		recommendedActivities.add(model2);
//		
//		
//		try{
//			System.out.println("#####################\nCOMPUTING KC K,P USING METHOD "+this.method);
//			if (lastAttempts == null || lastAttempts.isEmpty()) {
//				System.out.println("\tlastAttempts null or empty! Return the the passed lastKCModel!");
//				return lastKCModel;
//			}
//		
//	        InputStream bnContent = context.getResourceAsStream("./" + bnFolder + "/" + domain + ".xdsl");
//	
//			// single kc knowledge part1 from bn, add heuristic: pass kc priors and activity list to bn; return per kc knowledge level (first level; hashmap)
//			HashMap<String, double[]> passedKCModel = lastKCModel;
//			if (lastKCModel == null || lastKCModel.isEmpty() || lastUpdateDate == null || lastUpdateDate.isEmpty()) passedKCModel = null;
//			HashSet<String> bnSingleKCIdList = new HashSet<String>();
//			
//			HashMap<String, Double> singleKCIdToBNKnowledge = computeBNKnowledge(passedKCModel, lastAttempts, bnContent, bnSingleKCIdList); 
//			
//			HashMap<String,KnowledgeComponent> bnSingleKCList = new HashMap<String,KnowledgeComponent>();
//			for (Map.Entry<String, KnowledgeComponent> kcEntry : singleKCList.entrySet()) 
//				if (bnSingleKCIdList.contains(kcEntry.getValue().getId() + "") )
//					bnSingleKCList.put(kcEntry.getKey(), kcEntry.getValue());
//	
//			return computeCombinedKCLevels(allActivities, groupedKCList, bnSingleKCList, singleKCIdToBNKnowledge);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//		}
//		
//		
//		return nullKCLevels();
//	}
	
	
	/* Returns all kcs' levels (including those kcs that the activities don't involve. */
	public HashMap<String, double[]> computeNaiveKCModel(HashMap<String, Activity> allActivities, String domain){
		try{
			System.out.println("#####################\nCOMPUTING KC K,P USING NAIVE METHOD");
			return computeCombinedKCLevels(allActivities, groupedKCList, singleKCList, null);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return nullKCLevels();
	}
	
	
//	public HashMap<String, Double> computeBNKnowledge(HashMap<String, double[]> lastKCModel, ArrayList<Attempt> lastAttempts, InputStream bnContent, HashSet<String> activeSingleKCIdList){
//		//TODO: Now, in order to avoid repeated iteration, the arguments are directly passed in a redundant format. In a simpler way, it should pass HashMap<String, Double> and ArrayList<Double>.
//		try {
//			if (verbose) System.out.println("Compute BN Updated Knowledge for single KC...");
//			Network net = new Network(); 
//            String bn = "";
//            String line = "";
//     		BufferedReader reader = new BufferedReader(new InputStreamReader(bnContent));
//     		while ((line = reader.readLine()) != null)
//     			bn += line + "\n";
//            //System.out.println(bn);
//			net.readString(bn);//.readFile(bnFile);
//			net.updateBeliefs();
//		
//			if (lastKCModel != null && !lastKCModel.isEmpty()){
//				if (verbose) System.out.println("\tUpdate network's prior for KCs:");
//				//HashSet<String> activeGroupedKCIdList = new HashSet<String>();//this contains all grouped KCs
//				HashSet<String> groupedKCIdList = new HashSet<String>();//this contains only those in the lastKCModel
//				for (Map.Entry<String, KnowledgeComponentGroup> kcEntry : groupedKCList.entrySet()) {
//				    String kcId = kcEntry.getValue().getId() + "";
//				    groupedKCIdList.add(kcId);
//			    }
//				bnNodes = new HashSet<String>(Arrays.asList(net.getAllNodeIds())); 
//				
//				for (Map.Entry<String, double[]> kcEntry : lastKCModel.entrySet()) {
//					String kcId = kcEntry.getKey();
//					String bnKCId = "KC" + kcId;
//					
//					//TODO: within code checking
//					if (groupedKCIdList.contains(kcId)){
//						//activeGroupedKCIdList.add(kcId);
//						System.out.println("\tWARNING: " + kcId + " belongs to groupedKC, not considered in BN!");
//						continue;
//					}
//					if (!bnNodes.contains(bnKCId)){
//						System.out.println("\tERROR: " + bnKCId + " (which is not a groupedKC) is not in bn!");
//						continue;
//					}
//					
//					activeSingleKCIdList.add(kcId);
//					if (kcEntry.getValue().length <= 7){
//						System.out.println("\tWARNING: bnknowledgelevel not stored in the lastmodel!");
//						continue;
//					}
//					double pLearnedPost = kcEntry.getValue()[7]; //the place to store BN values
//					pLearnedPost = Math.min(Math.max(pLearnedPost, MIN_BN_KNOWLEDGE), 1 - MIN_BN_KNOWLEDGE);
//					double[] aDef = { pLearnedPost, 1 - pLearnedPost};
//					net.setNodeDefinition(bnKCId, aDef);
//				}
//				//TODO: not sure whether this affects a lot the speed; if so, put it outside
//				net.updateBeliefs();
//			}
//		
//			//TODO:check whether I could enter several evidences and then update or not?
//			if (verbose) System.out.println("Enter activity evidence:");
//			for (Attempt a : lastAttempts) {
//				String itemId = a.getContentName();
//				//String[] attempts = act.getAttemptsResSeq().split(",");
//				//for (int i = 0; i < attempts.length; i++){
//				String itemValue = (a.getResult() > 0.99)? "Correct":"Wrong";
//				net = updateByOneEvidence(net, itemId, itemValue, activeSingleKCIdList);					
//				//}
//			}
//			net.updateBeliefs();
////			net.writeFile("/Users/yunhuang/Center/Study/Codes/CodingProjects/Java/Aggregate/WebContent/WEB-INF/bn_temp2.xdsl");
////			System.out.println("Saved new network!");
//			
//			if (verbose) System.out.print("\tLatest: ");
//			HashMap<String, Double> newModel = new HashMap<String, Double>();
//			for (String KCId : activeSingleKCIdList){
//				double knowledge = net.getNodeValue("KC" + KCId)[0];
//				newModel.put(KCId, knowledge);
//				if (verbose) {
//					System.out.print("KC" + KCId + ":"); 
//					System.out.printf("%.4f,", knowledge);
//				
//				}
//			}
//			if (verbose) System.out.println("\n");
//			return newModel;
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//		}
//		return null;
//	}
	
//	public Network updateByOneEvidence(Network net, String itemId, String itemValue, HashSet<String> KCIds) {
//		/* It is already guaranteed that all KCIds are in the bn. */
//		try {
//			if (!bnNodes.contains(itemId)){
//				System.out.println("\tERROR: " + itemId + " is not in bn, no single KC will be updated by BN!");
//				return net;
//			}
//			
//			if (verbose){
//				System.out.print("\tBefore: ");
//				for (String kcId : KCIds){
//					System.out.print("KC" + kcId + ":"); 
//					System.out.printf("%.4f,", net.getNodeValue("KC" + kcId)[0]);
//				}
//				System.out.println("\n\tEvidence: " + itemId + "=" + itemValue);
//			}
//			//TODO: some place to increase speed
//			HashMap<String, Double> knowledgeBeforeUpdate = new HashMap<String, Double>();
//			if (forceKnowResultCons){
//				for (String kcId : KCIds) {
//					String bnKCId = "KC" + kcId;
//					double pLearnedPrior = net.getNodeValue(bnKCId)[0];
//					knowledgeBeforeUpdate.put(bnKCId, pLearnedPrior);
//				}
//			}
//		
//			net.setEvidence(itemId, itemValue);
//			net.updateBeliefs();
//			if (verbose) System.out.print("\tAfter:  ");
//			HashSet<String> itemKCs = null; 
//			//TODO: now if an itemId is in the bn, then it will have single kcs; in the future, need to also check it.
//			if (forceKnowResultCons)
//				itemKCs = new HashSet<String>(Arrays.asList(net.getParentIds(itemId))); 
//			for (String kcId : KCIds) {
//				String bnKCId = "KC" + kcId;
//				double pLearnedPost = net.getNodeValue(bnKCId)[0];
//				boolean flag = false;
//				if (forceKnowResultCons && itemKCs.contains(bnKCId)){
//					double pLearnedPrior = knowledgeBeforeUpdate.get(bnKCId);
//					if (itemValue.equals("Correct") && (pLearnedPost - pLearnedPrior <= 0)){
//						pLearnedPost = pLearnedPrior + MIN_BN_KNOWLEDGE;
//						System.out.print("\t" + bnKCId + ":");
//						System.out.printf("%.4f,", pLearnedPost);
//						System.out.println("WARNING: Force KnowIncWithSucc! Before:" + pLearnedPrior + " itemId:" + itemId);
//						flag = true;
//					}
//					else if (itemValue.equals("Wrong") && (pLearnedPost - pLearnedPrior > 0)){
//						pLearnedPost = pLearnedPrior - MIN_BN_KNOWLEDGE;
//						System.out.print("\t" + bnKCId + ":");
//						System.out.printf("%.4f,", pLearnedPost);
//						System.out.println("WARNING: Force KnowDecWithWrong! Before:" + pLearnedPrior + " itemId:" + itemId);
//						flag = true;
//					}
//				}
//				pLearnedPost = Math.min(Math.max(pLearnedPost, MIN_BN_KNOWLEDGE), 1 - MIN_BN_KNOWLEDGE);
//				double[] aDef = { pLearnedPost, 1 - pLearnedPost };
//				net.setNodeDefinition(bnKCId, aDef);
//				if (verbose){
//					if (!forceKnowResultCons){
//						System.out.print(bnKCId + ":"); 
//						System.out.printf("%.4f,", pLearnedPost);
//					}
//					else if (!flag){
//						System.out.print("\t" + bnKCId + ":"); 
//						System.out.printf("%.4f\n", pLearnedPost);
//					}
//				}
//			}
//			if (verbose) System.out.println("\n");
//			net.updateBeliefs();
//			net.clearEvidence(itemId);
//			net.updateBeliefs();
//			return net;
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//		}
//		return net;
//	}
	
	
	/*
	 * singleKCIdToBNKnowledge is null for native method
	 */
	public HashMap<String, double[]> computeCombinedKCLevels(
			HashMap<String, Activity> allActivities,
			HashMap<String,KnowledgeComponentGroup> activeGroupedKCList, 
			HashMap<String,KnowledgeComponent> activeSingleKCList, 
			HashMap<String, Double> singleKCIdToBNKnowledge){
		try {
			if (verbose) System.out.println("Compute combined KC levels...");
			
			HashMap<String, double[]> res = new HashMap<String, double[]>();
			// 1 for each KC, count how many activities which contributes there are, and how many has been completed by student
			HashMap<String, int[]> sumKC = new HashMap<String, int[]>(); // int[]: 0: n act contributing, 1: N act done, 2: N connections, 3: N connections done
			//Set<String> activitiesInContentLists = new HashSet<String> ();
	 		// first for groups of kc
			//HashMap<String, int[]> sumKCGroups = new HashMap<String, int[]>(); // int[]: 0: n act contributing, 1: N act done,
			for (Map.Entry<String, KnowledgeComponentGroup> kcGroupEntry : activeGroupedKCList.entrySet()) {
				KnowledgeComponentGroup kc = kcGroupEntry.getValue();
				if (verbose) System.out.println("\t" + kc.getId()  + "," + kc.getDisplayName());
				ArrayList<String> contents = kc.getContentList(filterImpForGroupedKC, contributesMatch);//here it constrains to be quizjet because of we put in database contributesMatch=1 for only quizjet
				if (contents.size() == 0){
					System.out.println("\tWARNING: content.size=0 for current grouped kc (" + kc.getId()  + "," + kc.getDisplayName() + "). Skip it!");
					continue;
				}
				
				int nCnt = 0;
				int nCntDone = 0;
				int nCntTried = 0;
				int nAttempts = 0;
				double sr = 0.0;
				
				int nOverlapAct = 0;
				// count activities contributing to K and activities done
				for(String c:contents){
					// ignore if the content is not in the course
					if(contentList.get(c) == null) continue; 
					
					nCnt++; // increment the number of content 
					if (verbose) System.out.println("\t\tactivity from kc.getContentList:" + c);
					Activity a = allActivities.get(c);
					if(a != null){
						//activitiesInContentLists.add(c);
						nOverlapAct++;
						if (verbose) System.out.print("\t\t\tIn current list of all activities! ");
						double[] levels = a.getLevels(); // (K,P,A,S,C,AN,L,T,Sub) knowledge,progress,N attempts, success rate, completion, annotations, likes, time, subactivities 

						if(levels != null && levels.length>1){
							if(levels[3] > -1) nCntTried++;
							// check if progress is 1
							if(((int)levels[1]) == 1){
								nCntDone++; // increment counter of content done
							}
							nAttempts += levels[2];
							if(levels[3] > -1) sr += levels[3];
						}
					}
				}
				sr = sr / (nCntTried>0 ? nCntTried : 1); // average success rate on activities having the KC
				
				if (nOverlapAct == 0){
					System.out.println("\tWARNING: no overlap between kc.getContentList and passed allActivities for current grouped kc (" + kc.getId()  + "," + kc.getDisplayName() + "). Skip it!");
					continue;
				}
				
				int[] kcCounts = {nCnt,nCntDone,0,0};
			
				// count groups in which the kc participate
				sumKC.put(kc.getId()+"", kcCounts);
				
				// add the kc and its levels
				double[] levels = new double[nKCLevels]; 
				double div = (kcCounts[0] > 0 ? kcCounts[0] : 1);
				double contentCoverage = kcCounts[1] / div;
				levels[0] = contentCoverage;
				levels[1] = contentCoverage;
				levels[2] = nAttempts;
				levels[3] = sr;
				levels[4] = nCntDone;
				levels[5] = -1;
				levels[6] = -1;
				res.put(kc.getId()+"",levels);
				
				if (verbose){
					System.out.print("\tcontentCoverage:"); 
					System.out.printf("%.4f", contentCoverage); 
					System.out.print(", levels: ");
					for (double l : levels)
						System.out.printf("%.4f,", l);
					System.out.println("\n");
				}
			}
			for (Map.Entry<String, KnowledgeComponent> kcEntry : activeSingleKCList.entrySet()) {
				String kcName = kcEntry.getKey();
				KnowledgeComponent kc = kcEntry.getValue();
				if (verbose) System.out.println("\t" + kc.getId()  + "," + kc.getDisplayName());
				ArrayList<String> contents = kc.getContentList(filterImpForSingleKC, contributesMatch);
				if (contents.size() == 0){
					System.out.println("\tERROR: content.size=0 for current single kc (" + kc.getId()  + "," + kc.getDisplayName() + "). Skip it!");
					continue;
				}
				
				int nCnt = 0;
				int nCntDone = 0;
				int nCntTried = 0;
				int nAttempts = 0;
				double sr = 0.0;
				int[] kcCounts = new int[4];
				int nOverlapAct = 0;
				// count activities contributing to K and activities done
				for(String c:contents){
					if(contentList.get(c) == null) continue; 
					nCnt++;
					if (verbose) System.out.println("\t\tactivity from kc.getContentList:" + c);
					Activity a = allActivities.get(c);
					if(a != null){
						//activitiesInContentLists.add(c);
						nOverlapAct++;
						if (verbose) System.out.print("\t\t\tIn current list of all activities! ");
						double[] levels = a.getLevels();
						if (verbose){
							System.out.print("levels:");
							for (double l : levels)
								System.out.print(l + ",");
							System.out.println();
						}
						if(levels != null && levels.length>1){
							if(levels[3] > -1) nCntTried++; // only count the success rate when it is different than -1
							// check if progress is 1
							if(((int)levels[1]) == 1){
								nCntDone++;
								if (verbose) System.out.println("\t\t\tlevels[1]=1!");
							}
							nAttempts += levels[2];
							if(levels[3] > -1) sr += levels[3];
						}
					}
				}
				if (nOverlapAct == 0){
					if (singleKCIdToBNKnowledge != null)
						System.out.println("\tERROR: no overlap between kc.getContentList and passed allActivities for current active single kc (" + kc.getId()  + "," + kc.getDisplayName() + "). Skip it!");
					else
						System.out.println("\tWARNING: no overlap between kc.getContentList and passed allActivities for current active single kc (" + kc.getId()  + "," + kc.getDisplayName() + "). Skip it!");
					continue;
				}
				
				kcCounts[0] = nCnt;
				kcCounts[1] = nCntDone;
				// count groups in which the kc participate (hy: can be faster)
				int countKCGroups = 0;
				int countKCGroupsDone = 0;
				for (Map.Entry<String, KnowledgeComponentGroup> kcGroupEntry : groupedKCList.entrySet()) {
					KnowledgeComponentGroup kcGroup = kcGroupEntry.getValue();
					if(kcGroup.kcExist(kc)){
						if (verbose) System.out.println("\t\tcurrent kc involves kcgroup:" + kcGroup.getId() + "," + kcGroup.getDisplayName());
						countKCGroups++;
						int[] v = sumKC.get(kcGroup.getId()+"");
						// TODO Check formula to infer groups done
						if(v!=null && v[1] >= v[0]/(2.0)) {//when at least half of the content that has at least one success, the connection is considered done (less strict then computing the content coverage of the connection)
							countKCGroupsDone++; 
							if (verbose) System.out.println("\t\t\tcurrent kcgroup is done!");
						}
					}
				}
				kcCounts[2] = countKCGroups;
				kcCounts[3] = countKCGroupsDone;
				sumKC.put(kc.getId()+"", kcCounts);
				
				// add the kc and its levels
				double[] levels = new double[nKCLevels]; 
				double div1 = (kcCounts[0] > 0 ? kcCounts[0] : 1);
				double div2 = (kcCounts[2] > 0 ? kcCounts[2] : 1);
				
				double contentCoverage = nCntDone / div1;
				double connectionCoverage = countKCGroupsDone / div2;
				Double bnKnowledge = null;
				
				// @@@@ HERE CHECK FOR OTHER METHODS!
				if (singleKCIdToBNKnowledge != null && ((bnKnowledge = singleKCIdToBNKnowledge.get(kc.getId() + "")) != null)){
					if(countKCGroups>=1) levels[0] = (connectionCoverage + bnKnowledge) / 2.0; // knowledge is average of content covered and connections done
					else { //bn knowledge is usually quite high (median 0.85)
						double relaxedContentCoverage = Math.min(kcCounts[1] / (div1/2.0), 1.0); //only need to attempt half distinct content to reach relaxedContentCoverage=1.0
						levels[0] = (relaxedContentCoverage + bnKnowledge) / 2.0;
					}
					levels[7] = bnKnowledge;
				}
				else{
					if(countKCGroups>=1) levels[0] = (connectionCoverage + contentCoverage) / 2.0; // knowledge is average of content covered and connections done
					else levels[0] = contentCoverage;
					levels[7] = 0;
				}
					
				levels[1] = contentCoverage; // progress is content done / total content related to the kc
				levels[2] = nAttempts;
				levels[3] = sr;
				levels[4] = nCntDone;
				levels[5] = countKCGroupsDone;
				levels[6] = -1;
				res.put(kc.getId()+"",levels);
				if (verbose){
					System.out.print("\tcontentCoverage:");  
					System.out.printf("%.4f", contentCoverage);  
					System.out.print(", connectionCoverage:");  
					System.out.printf("%.4f", connectionCoverage);  
					System.out.print(", bnKnowledge:"); 
					System.out.printf("%.4f", bnKnowledge); 
					System.out.print(", \tlevels: ");
					for (double l : levels)
						System.out.printf("%.4f,", l);
					System.out.println("\n");
				}
			}
	//		if (activitiesInContentLists.size() != allActivities.size())
	//			System.out.println("\tWARNING: activitiesInContentLists.size() != allActivities.size()");
			return res; //place holder, returning all levels in 0
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return nullKCLevels();	
	}
	
	
	
	
//	/**
//	 * Returns 7 double (K,P,A,SR,S,C,T ) knowledge,progress, attempts (raw), success rate, problems solved, connections done, time 
//	 * add original model K, 3-levels level (Mastered,Learning,Unknown)
//
//	 * @param lastUpdateDate
//	 * @param lastKCModel
//	 * @param activity
//	 * @return
//	 */
//	public HashMap<String, double[]> computeKCModel(String lastUpdateDate, 
//			HashMap<String, double[]> lastKCModel, ArrayList<Attempt> activity){
//		
//		if(lastKCModel != null && !lastKCModel.isEmpty()) return lastKCModel;
//		
//		return nullKCLevels(); //place holder, returning all levels in 0
//	}
	
	
	public HashMap<String, double[]> computeCUMULATEKCModel(HashMap<String, Activity> activity, String domain, String grp){
		System.out.println("COMPUTING KC K,P USING CUMULATE");
		HashMap<String, Double> cbumK = null;
//		boolean useCBUM = domain.equalsIgnoreCase("java");
//		if(useCBUM){
		cbumK = getConceptLevels(usr, domain, grp);
//		}
		
		HashMap<String, double[]> res = new HashMap<String, double[]>();
		
		//All of this commented by @Jordan, as for CUMULATE we are not considering KCs combinations
		// 1 for each KC, count how many activities which contributes there are, and how many has been completed by student
		//HashMap<String, int[]> sumKCGroups = new HashMap<String, int[]>(); // int[]: 0: n act contributing, 1: N act done,
		// first for groups of kc
		
//		for (Map.Entry<String, KnowledgeComponentGroup> kcGroupEntry : groupedKCList.entrySet()) {
//			String kcName = kcGroupEntry.getKey();
//			KnowledgeComponentGroup kc = kcGroupEntry.getValue();
//			ArrayList<String> contents = kc.getContentList(false, 1);
//			int nCntDone = 0;
//			int nCntTried = 0;
//			int nAttempts = 0;
//			double sr = 0.0;
//			int[] kcCounts = new int[2];
//			// count activities contributing to K and activities done
//			for(String c:contents){
//				
//				Activity a = activity.get(c);
//				if(a != null){
//					double[] levels = a.getLevels();
//					if(levels != null && levels.length>1){
//						if(levels[3] > -1) nCntTried++;
//						// check if progress is 1
//						if(((int)levels[1]) == 1){
//							nCntDone++;
//						}
//						nAttempts += levels[2];
//						if(levels[3] > -1) sr += levels[3];
//					}
//				}
//			}
//			sr = sr / (nCntTried>0 ? nCntTried : 1); // average success rate on activities having the KC
//			
//			kcCounts[0] = contents.size();
//			kcCounts[1] = nCntDone;
//			// count groups in which the kc participate
//			sumKCGroups.put(kc.getId()+"", kcCounts);
//			
//			// add the kc and its levels
//			double[] levels = new double[Aggregate.nKCLevels]; // 7 levels (K,P,A,SR,S,C,T ) knowledge,progress, attempts (raw), success rate, problems solved, connections done, time
//			double div = (kcCounts[0] > 0 ? kcCounts[0] : 1);
//			levels[0] = kcCounts[1] / div;
//			levels[1] = levels[0];
//			levels[2] = nAttempts;
//			levels[3] = sr;
//			levels[4] = nCntDone;
//			levels[5] = -1;
//			levels[6] = -1;
//			res.put(kc.getId()+"",levels);
//		}
		for (Map.Entry<String, KnowledgeComponent> kcEntry : singleKCList.entrySet()) {
			String kcName = kcEntry.getKey();
			KnowledgeComponent kc = kcEntry.getValue();
			ArrayList<String> contents = kc.getContentList(false, 1);
			int nCntDone = 0;
			int nCntTried = 0;
			int nAttempts = 0;
			double sr = 0.0;
			//Metrics added by @Jordan for getting metrics related to the last k attempts of the students
			double lastKnAttempts = -1;
			double lastKsr = -1;
			int[] kcCounts = new int[4];
			// count activities contributing to K and activities done
			int nonNullActivities = 0;
			int nonNullLastKActivities = 0;
//			System.out.println("------------------------");
//			System.out.println("For kc "+kcName);
			for(String c:contents){
				Activity a = activity.get(c);
				if(a != null){
//					System.out.println("Activity "+a.getName());
//					System.out.println(Arrays.toString(a.getLevels()));
					double[] levels = a.getLevels();
					if(levels != null && levels.length>1){
						
						if((int)levels[3] > -1) nCntTried++; // only count the success rate when it is different than -1
						// check if progress is 1
						if(((int)levels[1]) == 1){
							nCntDone++;
						}
						nAttempts += levels[2];
						if((double)levels[3] > -1){
							sr += (double)levels[3];
							nonNullActivities++;
							//TODO: @Jordan should I consider when SR is 0 but they have not attempted the activity???
							//System.out.println("Activity "+a.getName()+" success rate: "+levels[3]);
						}
						//Added by @Jordan for calculating last k attempts activity summary
						//System.out.println(a.getName());
						//System.out.println("Last k attempts: "+levels[10]+" , sr: "+levels[11]);
						if((int)levels[10]>0  && (double)levels[11] >=0){
							if (lastKsr == -1) lastKsr = 0.0; //When at least one of the sr is different than -1.0 we have to change the value of the lastKsr to 0.0 in order to update it with the right accumulated value
							if (lastKnAttempts == -1) lastKnAttempts = 0.0;
							lastKsr += (double)levels[11];
							lastKnAttempts += (int) levels[10];
							nonNullLastKActivities++;
						}
						
					}
				}
			}
			if(sr>0.0) sr = sr/nonNullActivities;//Added by Jordan, it gets average success rate, as in the previous for loop they were only summed up
			if(lastKsr>=0.0) lastKsr = lastKsr/nonNullLastKActivities;
			if(lastKnAttempts>=0) lastKnAttempts = lastKnAttempts/nonNullLastKActivities;
			System.out.println("Last k success rate for "+kcName+" : "+lastKsr+", avg n of attempts: "+lastKnAttempts);
			
			kcCounts[0] = contents.size();
			kcCounts[1] = nCntDone;
			
			//All of this commented by @Jordan, as for CUMULATE we are not considering KCs combinations
			// count groups in which the kc participate
//			int countKCGroups = 0;
//			int countKCGroupsDone = 0;
//			for (Map.Entry<String, KnowledgeComponentGroup> kcGroupEntry : groupedKCList.entrySet()) {
//				KnowledgeComponentGroup kcGroup = kcGroupEntry.getValue();
//				if(kcGroup.kcExist(kc)){
//					countKCGroups++;
//					int[] v = sumKCGroups.get(kcGroup.getId()+"");
//					// TODO Check formula to infer groups done
//					if(v!=null && v[1] > v[0]/2) countKCGroupsDone++;
//				}
//			}
//			kcCounts[2] = countKCGroups;
//			kcCounts[3] = countKCGroupsDone;

			
			// add the kc and its levels
			double[] levels = new double[nKCLevels]; 
			double div1 = (kcCounts[0] > 0 ? kcCounts[0] : 1);
			//double div2 = (kcCounts[2] > 0 ? kcCounts[2] : 1);
			
			// single computed knowledge of the KC as a proportion of related activities done
			double singleK =  kcCounts[1] / div1;
			// replace singleK with the K of the concept reported by CBUM
//			if(useCBUM && cbumK != null && cbumK.size() > 0){
			try {
				Double kcK = cbumK.get(kc.getIdName());
				
				if(kcK != null){
					singleK = kcK;
					//System.out.println(kc.getIdName()+ " Kc level: "+singleK);
				}else{
					System.out.println(kc.getIdName()+ " it is not provided by CUMULATE");
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			
			
			
			//if(countKCGroups>1) levels[0] = (kcCounts[3] / div2 + singleK) / 2.0; // knowledge is average of content covered and connections done
			//else 
			levels[0] = singleK;
			levels[1] = kcCounts[1] / div1; // progress is content done / total content related to the kc
			levels[2] = nAttempts;
			levels[3] = sr;
			levels[4] = nCntDone;
			levels[5] = -1;//kcCounts[3];//Commented by @Jordan as in CUMULATE we do not consider KCs combinations
			levels[6] = -1;
			levels[10] = lastKnAttempts; //Added by @Jordan for adding the number of attempts within the last k activity attempts
			levels[11] = lastKsr; //Added by @Jordan for adding the success rate on the last k activity attempts
			
			res.put(kc.getId()+"",levels);
			
		}
		return res; //place holder, returning all levels in 0
	}
	
	
	//use numeric ids
	public  HashMap<String, double[]> nullKCLevels() {
		HashMap<String, double[]> res = new HashMap<String, double[]>();
		for (Map.Entry<String, KnowledgeComponent> kcEntry : singleKCList.entrySet()) {
			//String component_name = kcEntry.getKey();
			String component_id = kcEntry.getValue().getId() + "";
			double[] levels = new double[nKCLevels]; 
			res.put(component_id, levels);
		}
		//All of this commented by @Jordan, as for CUMULATE we are not considering KCs combinations
//		for (Map.Entry<String, KnowledgeComponentGroup> kcEntry : groupedKCList.entrySet()) {
//			//String component_name = kcEntry.getKey();
//			String component_id = kcEntry.getValue().getId() + "";
//			double[] levels = new double[Aggregate.nKCLevels]; 
//			res.put(component_id, levels);
//		}
		return res;
	}

	
	public String getUsr() {
		return usr;
	}

	public void setUsr(String usr) {
		this.usr = usr;
	}

	public String getGrp() {
		return grp;
	}

	public void setGrp(String grp) {
		this.grp = grp;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public HashMap<String, KnowledgeComponent> getSingleKCList() {
		return singleKCList;
	}

	public void setSingleKCList(HashMap<String, KnowledgeComponent> singleKCList) {
		this.singleKCList = singleKCList;
	}

	public HashMap<String, KnowledgeComponentGroup> getGroupedKCList() {
		return groupedKCList;
	}

	public void setGroupedKCList(
			HashMap<String, KnowledgeComponentGroup> groupedKCList) {
		this.groupedKCList = groupedKCList;
	}


	
	/**
	 * This is to process concepts reported by UM
	 * @param usr
	 * @param domain
	 * @param grp
	 * @return
	 */
	
	public HashMap<String, Double> getConceptLevels(String usr, String domain,
			String grp) {
		HashMap<String, Double> user_concept_knowledge_levels = new HashMap<String, Double>();
		try {
			URL url = null;
			if (domain.equalsIgnoreCase("java")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=25&dom=java_ontology"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}

			if (domain.equalsIgnoreCase("sql")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=23&dom=sql_ontology"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}
			// TODO @@@@
			if (domain.equalsIgnoreCase("c")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=23&dom=c_programming"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}
			
			if (domain.equalsIgnoreCase("py")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=41&dom=py"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}
			
			if (url != null)
				user_concept_knowledge_levels = processUserKnowledgeReport(url);
			System.out.println(url.toString());
		} catch (Exception e) {
			user_concept_knowledge_levels = null;
			System.out.println("UM: Error in reporting UM for user " + usr);
			// e.printStackTrace();
		}
		return user_concept_knowledge_levels;

	}

	public static HashMap<String, Double> processUserKnowledgeReport(URL url) {

		HashMap<String, Double> userKnowledgeMap = new HashMap<String, Double>();
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(url.openStream());
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("concept");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					NodeList cogLevels = eElement
							.getElementsByTagName("cog_level");
					for (int i = 0; i < cogLevels.getLength(); i++) {
						Node cogLevelNode = cogLevels.item(i);
						if (cogLevelNode.getNodeType() == Node.ELEMENT_NODE) {
							Element cogLevel = (Element) cogLevelNode;
							if (getTagValue("name", cogLevel).trim().equals(
									"application")) {

								double level = 0.0;
								level = Double.parseDouble(getTagValue("value", cogLevel).trim());
								
								userKnowledgeMap.put(getTagValue("name", eElement), level);
								//System.out.println("Name from xml: "+getTagValue("name", eElement)+" level: "+level);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println("UM: Error in reporting UM. URL = " + url);
			return null;
		}
		return userKnowledgeMap;
	}

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0)
				.getChildNodes();
		Node nValue = (Node) nlList.item(0);
		return nValue.getNodeValue();
	}


	public ArrayList<ArrayList<String[]>> getRecommendedActivities() {
		return recommendedActivities;
	}
	
	
	
}
