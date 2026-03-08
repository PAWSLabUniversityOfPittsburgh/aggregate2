import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

public class AggregateDB extends dbInterface {
    public static DecimalFormat df4 = new DecimalFormat("#.####");

    public AggregateDB(String connurl, String user, String pass) {
        super(connurl, user, pass);
    }

    // returns the name of the grp
    public String getGrpName(String grp) {
        try {
            String res = "";
            String query = "select G.group_name from ent_group G where G.group_id = '"
                    + grp + "';";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
//            System.out.println(query);

            while (rs.next()) {
                res = rs.getString("group_name");
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            this.releaseStatement(stmt, rs);
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }

    }

    public String getDomain(String course_id) {
        try {
            String res = "";
            stmt = conn.createStatement();
            String query = "select domain from ent_course  where course_id = '"
                    + course_id + "';";
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                res = rs.getString("domain");
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            this.releaseStatement(stmt, rs);
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }

    }

    // returns the name of the grp
    public String getCourseId(String grp) {
        try {
            String res = "";
            stmt = conn.createStatement();
            String query = "select G.course_id from ent_group G where G.group_id = '"
                    + grp + "';";
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                res = rs.getString("course_id");
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            this.releaseStatement(stmt, rs);
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }

    }

    public HashMap<String, String> getNonStudents(String group_id) {
        HashMap<String, String> res = new HashMap<String, String>();
        try {

            stmt = conn.createStatement();
            String query = "SELECT user_id, user_role FROM ent_non_student WHERE group_id = '"
                    + group_id + "';";

            rs = stmt.executeQuery(query);
            while (rs.next()) {
                res.put(rs.getString("user_id"), rs.getString("user_role"));
                // System.out.println(rs.getString("user_id"));
            }
            this.releaseStatement(stmt, rs);

        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());

        } finally {
            this.releaseStatement(stmt, rs);

        }
        return res;
    }

    // returns the ordered list of topics of a course corresponding to the group
    // (class)
    public ArrayList<String[]> getTopicList(String course_id) {
        try {
            ArrayList<String[]> res = new ArrayList<String[]>();
            stmt = conn.createStatement();
            String query = "SELECT T.topic_name, T.display_name, T.order, T.visible FROM ent_topic T "
                    + " WHERE T.course_id = '" + course_id + "' AND T.active=1 ORDER BY T.`order` ASC;";

            rs = stmt.executeQuery(query);
            while (rs.next()) {
                String[] topic = new String[5];
                topic[0] = rs.getString("topic_name");
                topic[1] = rs.getString("display_name");
                topic[2] = rs.getString("order");
                topic[3] = rs.getString("visible"); // visibility
                topic[4] = ""; // current / covered
                res.add(topic);
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }
    }

    public ArrayList<String> getHiddenTopics(String group_id) {
        try {
            ArrayList<String> res = new ArrayList<String>();
            stmt = conn.createStatement();
            String query = "SELECT topic_name " + " FROM ent_hidden_topics "
                    + " WHERE group_id = '" + group_id + "'";

            rs = stmt.executeQuery(query);
            while (rs.next()) {
                res.add(rs.getString("topic_name"));
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            this.releaseStatement(stmt, rs);
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        }
    }

    // returns all content for each topic -> questions, examples, readings as
    // arrays of string
    // @@@@ JG FIXED
    public ArrayList<String[]> getContentByTopic(String topic) {
        try {
            ArrayList<String[]> res = new ArrayList<String[]>();
            stmt = conn.createStatement();
            String query = "SELECT C.content_id,C.content_name,C.content_type,C.display_name,C.url, C.desc, C.comment "
                    + " FROM ent_content C, rel_topic_content TC, ent_topic T "
                    + " WHERE T.topic_name='"
                    + topic
                    + "' and T.topic_id = TC.topic_id and TC.content_id=C.content_id and C.visible = 1 and TC.visible = 1 and T.active = 1 "
                    + " ORDER by C.content_type desc, TC.display_order asc";
            rs = stmt.executeQuery(query);
            int i = 0;
            while (rs.next()) {
                String[] content = new String[7];
                content[0] = rs.getString("content_id");
                content[1] = rs.getString("content_name");
                content[2] = rs.getString("content_type");
                content[3] = rs.getString("display_name");
                content[4] = rs.getString("url");
                content[5] = rs.getString("desc");
                content[6] = rs.getString("comment");
                res.add(content);
                // res.put(content);
                // System.out.println(content[0]+" "+content[2]);
                i++;
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }

    }


    // now get data with resource instead of content_type
    public HashMap<String, String[]> getContent2(String course_id) {
        try {
            HashMap<String, String[]> res = new HashMap<String, String[]>();
            stmt = conn.createStatement();
            String query = "SELECT C.content_id,C.content_name,R.resource_name,C.display_name,C.url, C.desc, C.comment, C.provider_id "
                    + " FROM ent_content C, rel_topic_content TC, ent_topic T, ent_resource R "
                    + " WHERE T.course_id='"
                    + course_id
                    + "' and T.topic_id=TC.topic_id and TC.content_id=C.content_id and C.visible = 1 and TC.visible = 1 and T.active = 1 "
                    + " and R.resource_id = TC.resource_id "
                    + " ORDER by T.`order`, R.`order` desc, TC.display_order asc";
            rs = stmt.executeQuery(query);
            int i = 0;
            while (rs.next()) {
                String[] content = new String[7];
                String content_name = rs.getString("content_name");
                content[0] = rs.getString("resource_name");
                content[1] = rs.getString("display_name");
                content[2] = rs.getString("url");
                content[3] = rs.getString("desc");
                content[4] = rs.getString("comment");
                content[5] = rs.getString("provider_id");
                content[6] = "";
                res.put(content_name, content);
                //System.out.println(content_name + " " + content[0]+" "+content[2]);
                i++;
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }

    }


    // Now with resource instead of content_type
    // @@@@ #### TEST!!
    public HashMap<String, ArrayList<String>[]> getTopicContent2(String course_id, HashMap<String, Integer> resourceMap) {
        try {
            int n = resourceMap.size();

            HashMap<String, ArrayList<String>[]> res = new HashMap<String, ArrayList<String>[]>();
            stmt = conn.createStatement();
            String query = "SELECT T.topic_name, group_concat(C.content_name , ',' , R.resource_name order by C.content_type, TC.display_order separator ';') as content "
                    + "FROM ent_topic T, rel_topic_content TC, ent_content C, ent_resource R   "
                    + "WHERE T.course_id = '"
                    + course_id
                    + "' "
                    + "and T.active=1 and C.visible = 1 and TC.visible = 1 "
                    + "and TC.topic_id=T.topic_id and C.content_id = TC.content_id and R.resource_id = TC.resource_id "
                    + "group by T.topic_id";

            //System.out.println("query:");
            //System.out.println(query);
            //System.out.println();
            rs = stmt.executeQuery(query);
            String topic = "";

            while (rs.next()) {
                topic = rs.getString("topic_name");
                String allcontent = rs.getString("content");
                //System.out.println(" "+topic+" : ");
                ArrayList<String>[] all_content = new ArrayList[n];
                for(int i = 0;i<n;i++){
                    all_content[i] = new ArrayList<String>();
                }

                if (allcontent == null || allcontent.equalsIgnoreCase("[null]")
                        || allcontent.length() == 0) {
                    //
                } else {
                    String[] content = allcontent.split(";");
                    for (int i = 0; i < content.length; i++) {
                        String[] item = content[i].split(",");
                        System.out.println(item[0]+","+item[1]);
                        int resourceIndex = resourceMap.get(item[1]);
                        if (resourceIndex>=0 && resourceIndex<n) all_content[resourceIndex].add(item[0]);
                    }
                }
                res.put(topic, all_content);
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }

    }



    //
    public void storeComputedModel(String user, String course_id,
    		String group_id, String sid, String model4topics, String model4content, String model4kc) {
        try {
        	String query = "";

            if(this.existComputedModel(user, course_id)){
                query = "UPDATE ent_computed_models SET model4topics='"
                        + model4topics + "', model4content='" + model4content + "', model4kc='" + model4kc
                        + "', last_update=now() WHERE user_id = '" + user
                        + "' and course_id='" + course_id + "';";
            }else{
                query = "INSERT INTO ent_computed_models (user_id,course_id,last_update,model4topics,model4content,model4kc) VALUES " +
                		"('" +user+ "'," +course_id+ ",now(),'" + model4topics + "','" + model4content + "','" + model4kc + "');";
            }
            stmt = conn.createStatement();
            //System.out.println(query);
            stmt.execute(query);
            query = "INSERT INTO ent_computed_models_history (user_id,course_id,group_id,session_id,computedon,model4topics,model4content,model4kc) values " +
                		"('"+ user+ "',"+ course_id+ ",'"+ group_id+ "','"+ sid+ "',now(),'"+ model4topics+ "','"+ model4content+ "','" + model4kc + "');";
            //System.out.println(query);
            stmt.execute(query);

            // System.out.println(query);

            this.releaseStatement(stmt, rs);
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
        } finally {
            this.releaseStatement(stmt, rs);
        }

    }




    // Sees if a user has a model for a specific course id
    public boolean existComputedModel(String user, String course_id) {
        int n = 0;
        try {
            stmt = conn.createStatement();
            String query = "SELECT count(*) as npm "
                    + "FROM ent_computed_models  " + "WHERE user_id='" + user
                    + "' and course_id='"+ course_id + "';";
            //System.out.println(query);
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                n = rs.getInt("npm");
            }
            this.releaseStatement(stmt, rs);
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
        } finally {
            this.releaseStatement(stmt, rs);
        }
        return n > 0;
    }
    
    public HashMap<String, String[]> getComputedModelsInCourse(String course_id) {
        return getComputedModels(course_id, null);
    }

    // give usr == null or usr == "" to look for all precomputed models within a course
    public HashMap<String, String[]> getComputedModels(String course_id, String usr) {
        try {
            HashMap<String, String[]> res = new HashMap<String, String[]>();
            stmt = conn.createStatement();
            String query = "SELECT user_id,model4topics,model4content,model4kc,last_update FROM ent_computed_models WHERE course_id='" + course_id + "'";
            if (usr != null && usr.length()>0) query += " and user_id = '"+usr+"'";
            query += ";";
            rs = stmt.executeQuery(query);
            //System.out.println(query);
            String user = "";
            String[] models;
            while (rs.next()) {
                user = rs.getString("user_id");
                //System.out.println("Adding user "+user);
                models = new String[4];
                models[0] = rs.getString("model4topics");
                models[1] = rs.getString("model4content");
                models[2] = rs.getString("model4kc");
                models[3] = rs.getString("last_update");
                res.put(user, models);
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }
    }



    public boolean insertUsrFeedback(String usr, String grp, String sid,
            String srcActivityId, String srcActivityRes, String fbId,
            String fbItemIds, String responses, String recId) {
        String query = "";
        String[] fbItemArray = { "" };
        String[] resArray = { "" };
        // System.out.println(responses);
        try {
            stmt = conn.createStatement();
            if (fbItemIds != null && fbItemIds.length() != 0) {
                fbItemArray = fbItemIds.split("\\|");
                resArray = responses.split("\\|");
                if (fbItemArray.length != resArray.length) {
                    //
                }
            }
            for (int i = 0; i < fbItemArray.length; i++) {
                query = "INSERT INTO ent_user_feedback (user_id,session_id,group_id,src_content_name, src_content_res, fb_id, fb_item_id, fb_response_value, item_rec_id, datentime) values ('"
                        + usr
                        + "','"
                        + sid
                        + "','"
                        + grp
                        + "','"
                        + srcActivityId
                        + "','"
                        + srcActivityRes
                        + "','"
                        + fbId
                        + "','"
                        + fbItemArray[i]
                        + "','"
                        + resArray[i]
                        + "','"
                        + recId + "'," + "now());";
                // System.out.println(query);
                stmt.executeUpdate(query);
            }

            // System.out.println(System.nanoTime()/1000);
            this.releaseStatement(stmt, rs);
            return true;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            System.out.println(query);
            releaseStatement(stmt, rs);
            return false;
        }
    }

    public boolean insertTrackAction(String usr, String grp, String sid,
            String action, String comment) {
        String query = "";
        try {
            stmt = conn.createStatement();
            query = "INSERT INTO ent_tracking (datentime, user_id, session_id, group_id, action, comment) values ("
                    + "now(3), '"
                    + usr
                    + "','"
                    + sid
                    + "','"
                    + grp
                    + "','"
                    + action + "','" + comment + "');";

            stmt.executeUpdate(query);
            // System.out.println(query);
            this.releaseStatement(stmt, rs);
            // System.out.println(query);
            return true;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            releaseStatement(stmt, rs);
            return false;
        }
    }



    // resource name , resource display name,desc,visible,update_state_on
    // Example:  qz , question, "this is the description" , 1, 101
    // update_state_on: digits represent in order the options for updating the user model:
    //          1: activity done, 2: in window close, and 3: window close if activity done.
    //          For example 010 will update UM when the content window is closed.
    public ArrayList<String[]> getResourceList(String course_id) {
        try {
            ArrayList<String[]> res = new ArrayList<String[]>();
            stmt = conn.createStatement();
            String query = "select resource_name,display_name,`desc`,visible,update_state_on,`order`,window_width,window_height from ent_resource " +
            		   "where course_id=\'"+course_id+"\' order by `order`;";
            rs = stmt.executeQuery(query);
            int i = 0;
            while (rs.next()) {
                String[] resource = new String[8];
                resource[0] = rs.getString("resource_name");
                resource[1] = rs.getString("display_name");
                resource[2] = rs.getString("desc");
                resource[3] = rs.getString("visible");
                resource[4] = rs.getString("update_state_on");
                resource[5] = rs.getString("order");
                resource[6] = rs.getString("window_width");
                resource[7] = rs.getString("window_height");
                    res.add(resource);
                //System.out.println(resource[0]+" | "+resource[1]+" | "+resource[2]+" | "+resource[3]+" | "+resource[4]);
                i++;
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        }

    }

    public ArrayList<String[]> getSubGroups(String group_id) {
        try {
            ArrayList<String[]> res = new ArrayList<String[]>();
            stmt = conn.createStatement();
            String query = "SELECT subgroup_name,subgroup_users,type from ent_subgroups " +
            		   "where group_id=\'"+group_id+"\';";
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                String[] subgroup = new String[3];
                subgroup[0] = rs.getString("subgroup_name");
                subgroup[1] = rs.getString("subgroup_users");
                subgroup[2] = rs.getString("type");
                res.add(subgroup);
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        }

    }

    public ArrayList<String[]> getParameters(String user_id, String group_id) {
        try {
            ArrayList<String[]> res = new ArrayList<String[]>();
            stmt = conn.createStatement();
            String query = "SELECT level, params_vis, params_svcs, user_manual FROM ent_parameters WHERE " +
            		   " (group_id=\'"+group_id+"\' AND level='group') OR (user_id=\'"+user_id+"\' AND group_id=\'"+group_id+"\') ;";
            // (user_id='dguerra' AND group_id='ADL') or ((isnull(user_id) or user_id='') AND group_id='ADL')
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                String[] parameters = new String[5];
                parameters[0] = rs.getString("level");
                parameters[1] = rs.getString("params_vis");
                parameters[2] = rs.getString("params_svcs");
                parameters[3] = rs.getString("user_manual");
                res.add(parameters);
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        }

    }

    public String[] getTimeLine(String group_id) {
        try {
            String[] res = new String[2];
            stmt = conn.createStatement();
            String query = "SELECT currentTopics, coveredTopics FROM ent_timeline WHERE " +
            		   " group_id=\'"+group_id+"\' ;";
            // (user_id='dguerra' AND group_id='ADL') or ((isnull(user_id) or user_id='') AND group_id='ADL')
            rs = stmt.executeQuery(query);
            while (rs.next()) {
            	res[0] = rs.getString("currentTopics");
            	res[1] = rs.getString("coveredTopics");
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        }

    }


    // Other methods to insert Groups, non students, select current/covered topics,

    // Add a group
    public boolean registerGroup(String grp, String grpName, String cid, String term, String year, String creatorId) {
        String query = "";
        try {
            stmt = conn.createStatement();
            query = "INSERT INTO ent_group (group_id, group_name, course_id, creation_date, term, year, creator_id) values " +
                    "('" + grp + "','" + grpName + "','" + cid + "',now(),'" + term + "','" + year + "','" + creatorId + "');";

            stmt.executeUpdate(query);
            // System.out.println(query);
            this.releaseStatement(stmt, rs);
            // System.out.println(query);
            return true;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            releaseStatement(stmt, rs);
            return false;
        }
    }

    public boolean addNonStudent(String grp, String usr, String role) {
        String query = "";
        try {
            stmt = conn.createStatement();
            query = "INSERT INTO ent_non_student (group_id, user_id, user_role) values " +
                    "('" + grp + "','" + usr + "','" + role + "');";

            stmt.executeUpdate(query);
            // System.out.println(query);
            this.releaseStatement(stmt, rs);
            // System.out.println(query);
            return true;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            releaseStatement(stmt, rs);
            return false;
        }
    }

    public HashMap<String,String[]> getProvidersInfo(String courseId){
    	try {
    		HashMap<String,String[]> res = new HashMap<String,String[]>();
            stmt = conn.createStatement();
            //String query = "SELECT provider_id, um_svc_url, activity_svc_url FROM ent_provider;";
            String query = "SELECT P.provider_id, P.um_svc_url, P.activity_svc_url FROM ent_provider P, ent_resource R, rel_resource_provider RP " +
            			" WHERE R.resource_id = RP.resource_id AND RP.provider_id = P.provider_id and R.course_id = "+courseId+";";
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                String id = rs.getString("provider_id");
                String[] urls = new String[4];

                urls[0] = rs.getString("um_svc_url");
                urls[1] = rs.getString("activity_svc_url");
                res.put(id,urls);
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        }
    }


    // @@@@ JULIO KCMAP
	// Read the list of concepts which are "active" (would be shown)
	public HashMap<String,KnowledgeComponent> getAllKCs(String domain) {
		HashMap<String,KnowledgeComponent> res = new HashMap<String,KnowledgeComponent>();
		try {

			stmt = conn.createStatement();
			String query = "SELECT KC.id, KC.component_name, KC.cardinality, KC.display_name, threshold1, threshold2, "
						+ " GROUP_CONCAT(CC.content_name, ',', cast(CC.weight as char), ',', cast(CC.importance as char), ',', cast(CC.contributesK as char) ORDER BY CC.content_name SEPARATOR '|' ) as contents, "
						+ " KC.main_topic, KC.main_component "
						+ " FROM kc_component KC, kc_content_component CC"
						+ " WHERE  KC.component_name = CC.component_name and KC.active=1 AND CC.active=1 and KC.domain='"+domain+"' AND CC.domain = KC.domain"
						+ " GROUP BY KC.cardinality ASC, CC.component_name ASC";
			rs = stmt.executeQuery(query);

			while (rs.next()) {
				int id = rs.getInt("id");
				int cardinality = rs.getInt("cardinality");
				String name = rs.getString("component_name");
				System.out.println(name);//added by @Jordan
				double th1 = rs.getDouble("threshold1");
				double th2 = rs.getDouble("threshold2");
				KnowledgeComponent c;
				if(cardinality == 1){
					c = new KnowledgeComponent(id,name,rs.getString("display_name"),rs.getString("main_topic"), th1, th2);
				}else{
					c = new KnowledgeComponentGroup(id,name,rs.getString("display_name"),rs.getString("main_topic"));
					String[] kcNames = rs.getString("component_name").split("~"); // this supposes to be the format
					if(kcNames != null) for(String kcName : kcNames){
						KnowledgeComponent kc = res.get(kcName);
						if(kc != null && !((KnowledgeComponentGroup)c).kcExist(kc)) ((KnowledgeComponentGroup)c).getKcs().add(kc);
					}
					String mainComponent = rs.getString("main_component");
					if(mainComponent != null && mainComponent.length()>0){
						KnowledgeComponent mainKc = res.get(mainComponent);
						if(mainKc != null) ((KnowledgeComponentGroup)c).setMainKc(mainKc);
					}
				}

				String[] contentRels = rs.getString("contents").split("\\|");
				//System.out.println(name);
				if(contentRels != null) {
					for(String cr : contentRels){
						String[] cn = cr.split(",");
						//System.out.println(name+" : "+cn[0]);
						double[] v = new double[3];
						//System.out.println("    "+cn[0] + " :a " +cr);
						v[0] = Double.parseDouble(cn[1]);
						v[1] = Double.parseDouble(cn[2]);
						v[2] = Double.parseDouble(cn[3]);
						try{c.getContents().put(cn[0],v);}catch(Exception e){}
					}
				}
				res.put(name,c);
			}
			this.releaseStatement(stmt, rs);

		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());

		} finally {
			this.releaseStatement(stmt, rs);
		}
		return res;

	}

    // @@@@ JULIO
	// Get stats of content
	public HashMap<String,ContentStats> getContentStats(String domain) {
		HashMap<String,ContentStats> res = new HashMap<String,ContentStats>();
		try {

			stmt = conn.createStatement();
			String query = "SELECT content_name, provider_id, "
					+ "`a_p10`,`a_p25`,`a_p33`,`a_p50`,`a_p66`,`a_p75`,`a_p80`,`a_p85`,`a_p90`,"
					+ "`t_p10`,`t_p25`,`t_p33`,`t_p50`,`t_p66`,`t_p75`,`t_p80`,`t_p85`,`t_p90`,"
					+ "`sr_p10`,`sr_p25`,`sr_p33`,`sr_p50`,`sr_p66`,`sr_p75`,`sr_p80`,`sr_p85`,`sr_p90` "
					+ " FROM stats_content WHERE domain = '"+domain+"'";
			//System.out.println(query);
			rs = stmt.executeQuery(query);

			while (rs.next()) {

				String name = rs.getString("content_name");
				ContentStats c = new ContentStats(rs.getString("content_name"),rs.getString("provider_id"),
						 rs.getDouble("a_p10"),rs.getDouble("a_p25"),rs.getDouble("a_p33"),rs.getDouble("a_p50"),rs.getDouble("a_p66"),rs.getDouble("a_p75"),rs.getDouble("a_p80"),rs.getDouble("a_p85"),rs.getDouble("a_p90"),
						 rs.getDouble("t_p10"),rs.getDouble("t_p25"),rs.getDouble("t_p33"),rs.getDouble("t_p50"),rs.getDouble("t_p66"),rs.getDouble("t_p75"),rs.getDouble("t_p80"),rs.getDouble("t_p85"),rs.getDouble("t_p90"),
						 rs.getDouble("sr_p10"),rs.getDouble("sr_p25"),rs.getDouble("sr_p33"),rs.getDouble("sr_p50"),rs.getDouble("sr_p66"),rs.getDouble("sr_p75"),rs.getDouble("sr_p80"),rs.getDouble("sr_p85"),rs.getDouble("sr_p90"));

				res.put(name,c);
			}
			this.releaseStatement(stmt, rs);

		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());

		} finally {
			this.releaseStatement(stmt, rs);
		}
		return res;

	}

  public List<Logs> getFiveRecentPoints(String user_id, String group_id){
    List<Logs> res = new ArrayList<Logs>();
    try{
      stmt= conn.createStatement();
      String query = "select * from ent_point e where e.user_id = '"+user_id+"' and e.group_id = '"+group_id+"' and e.total_point + 0 > 0 order by e.total_point + 0 desc limit 5 ;";
      rs = stmt.executeQuery(query);
      while(rs.next()){
        Logs l = new Logs(rs.getString("recent_point"), rs.getString("description"),rs.getString("total_point"));
        res.add(l);
      }
      this.releaseStatement(stmt, rs);
    } catch(SQLException ex){
        System.out.println("SQLException: " + ex.getMessage());
        System.out.println("SQLState: " + ex.getSQLState());
        System.out.println("VendorError: " + ex.getErrorCode());
    } finally {
      this.releaseStatement(stmt, rs);
    }
    return res;
  }
  public Logs getMostRecentLogForEachStudent(String user_id, String group_id){
    Logs res = null ;
    try{
      stmt = conn.createStatement();
      System.out.println("stmt:"+ stmt.toString());
      //String query = "select e.recent_point, e.description, MAX(e.total_point + 0) from ent_point e where e.user_id = '" + user_id + "' and e.group_id = '" + group_id + "' group by e.user_id, e.group_id;";
      String query = "select * from ent_point where user_id = '"+user_id+"' and group_id = '"+group_id+"' order by total_point + 0 desc limit 1;";
      System.out.println("query:"+ query);
      rs = stmt.executeQuery(query);
      while(rs.next()){
    	  	System.out.println("recent_point: " + rs.getString("recent_point")+" description: "+ rs.getString("description")+ " total_point: "+ rs.getString("total_point"));
        res = new Logs(rs.getString("recent_point"), rs.getString("description"),rs.getString("total_point"));
      }
      this.releaseStatement(stmt,rs);

    } catch (SQLException ex){
        System.out.println("SQLException: " + ex.getMessage());
        System.out.println("SQLState: " + ex.getSQLState());
        System.out.println("VendorError: " + ex.getErrorCode());
    } finally {
        this.releaseStatement(stmt, rs);
    }
    return res;
  }
  public List<Badges> getBadgesForEachStudent(String user_id, String group_id){
    List<Badges> res = new ArrayList<Badges>();
    try{
      stmt = conn.createStatement();
      String query = "select e.* from rel_user_badge r , ent_badge e where r.user_id = '" + user_id + "' and r.group_id = '" + group_id + "' and r.badge_id = e.badge_id;";

      rs = stmt.executeQuery(query);
      while(rs.next()){
        Badges b = new Badges(rs.getString("badge_id"),rs.getString("value"),rs.getString("name"), rs.getString("type"), rs.getString("img_URL"), rs.getString("congrat_description"));
        res.add(b);
      }
      this.releaseStatement(stmt, rs);

    } catch(SQLException ex){
        System.out.println("SQLException: " + ex.getMessage());
        System.out.println("SQLState: " + ex.getSQLState());
        System.out.println("VendorError: " + ex.getErrorCode());
    } finally {
      this.releaseStatement(stmt, rs);
    }
    return res;
  }
  
  public String getTotalRecForEachStudent(String user_id, String group_id){
    String res = null;
    try{
      stmt = conn.createStatement();
      String query = "select e.total_rec from ent_user_rec e where e.user_id = '"+user_id+"' and e.group_id = '" + group_id + "' ;";
      rs = stmt.executeQuery(query);
      while(rs.next()) {
    	  	res = rs.getString("total_rec");
      }
      this.releaseStatement(stmt, rs);
       
    } catch(SQLException ex){
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    } finally {
      this.releaseStatement(stmt,rs);
    }
    return res;
  }
  public boolean updateTotalRecForEachStudent(String user_id, String group_id, String newTotal){
    try{
      stmt = conn.createStatement();
      String query = "update ent_user_rec set total_rec = '"+newTotal+"' where user_id = '"+user_id+"' and group_id='" + group_id + "';";
      int count = stmt.executeUpdate(query);
      if( count == 0 ) {
    	  	query = "insert into ent_user_rec(user_id,group_id, total_rec)  values('"+user_id+"','"+group_id+"','"+newTotal+"');";
    	  	stmt.executeUpdate(query);
      }

      this.releaseStatement(stmt, rs);
      return true;
    } catch(SQLException ex){
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
      this.releaseStatement(stmt, rs);
      return false;

    }
  }

  public boolean insertTotalRecForEachStudent(String user_id, String group_id, String newTotal){
    try{
      stmt = conn.createStatement();
      String query = "insert into ent_user_rec(user_id,group_id, total_rec)  values('"+user_id+"','"+group_id+"','"+newTotal+"');";
      stmt.executeUpdate(query);
      this.releaseStatement(stmt, rs);
      return true;
      
    } catch(SQLException ex){
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());

      this.releaseStatement(stmt, rs);
      return false;
    }
  }
  
  public String getBadgeIDBasedOnValue(String value){
    String res = "null";
    try{
      stmt = conn.createStatement();
      String query = "select e.* from ent_badge e  where e.value = '" + value + "';";
      rs = stmt.executeQuery(query);
      while(rs.next()){
        res = rs.getString("badge_id");
      }
      this.releaseStatement( stmt , rs );
    } catch(SQLException ex){
        System.out.println("SQLException: " + ex.getMessage());
        System.out.println("SQLState: " + ex.getSQLState());
        System.out.println("VendorError: " + ex.getErrorCode());
    } finally{
      this.releaseStatement(stmt, rs);
    }
    return res;
  }
  public Badges getBadgeById(String id) {
	  Badges res = null;
	    try{
	      stmt = conn.createStatement();
	      String query = "select e.* from ent_badge e  where e.badge_id = '" + id + "';";
	      rs = stmt.executeQuery(query);
	      while(rs.next()){
	        res =new Badges(rs.getString("badge_id"), rs.getString("value"), rs.getString("name"), rs.getString("type"), rs.getString("img_URL"), rs.getString("congrat_description"));
	      }
	      this.releaseStatement( stmt , rs );
	    } catch(SQLException ex){
	        System.out.println("SQLException: " + ex.getMessage());
	        System.out.println("SQLState: " + ex.getSQLState());
	        System.out.println("VendorError: " + ex.getErrorCode());
	    } finally{
	      this.releaseStatement(stmt, rs);
	    }
	    return res;
  }
  public boolean insertNewBadge(String badge_id,String value, String name, String type, String img_URL, String congrat_description){
    try {
      stmt = conn.createStatement();
      String query = "insert into ent_badge(badge_id, value, name, type, img_URL, congrat_description) values('"+badge_id+"','"+value+"','"+name+"','"+type+"','"+img_URL+"','"+congrat_description+"');";
      stmt.executeUpdate(query);
      this.releaseStatement(stmt, rs);
      return true;
    } catch(SQLException ex){
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());

      this.releaseStatement(stmt, rs);
      return false;
    }
  }
  public boolean insertNewBadgeForEachStudent(String user_id, String group_id, String badge_id){
    try {
      stmt = conn.createStatement();
      String query = "insert into rel_user_badge(user_id, group_id, badge_id) values('"+user_id+"','"+group_id+"','"+badge_id+"');";
      System.out.println("The Query for insert new badge is: "+ query);
      stmt.executeUpdate(query);
      this.releaseStatement(stmt,rs);
      return true;
    } catch(SQLException ex){
      System.out.println("SQLException11: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());

      this.releaseStatement(stmt, rs);
      return false;
    }
  }
  public boolean insertRecentPoint(String user_id, String group_id, String recent_point, String description, String total_point){
	  try {
		  stmt = conn.createStatement();
		  String query = "select COUNT(*) AS total from ent_point where user_id = '"+user_id+"' and group_id = '"+group_id+"'";
		  rs = stmt.executeQuery(query);
		  int total=-1;
		  while(rs.next()) {
			  total = rs.getInt("total");
		  }
		  if(total != 5) {
			  query = "insert into ent_point(user_id, group_id, recent_point, description, total_point) values('"+user_id+"', '"+group_id+"','"+recent_point+"','"+description+"','"+total_point+"')";
			  stmt.executeUpdate(query);
			  for(int i =0 ; i<4 ; i++) {
				  query = "insert into ent_point(user_id, group_id, recent_point, description, total_point) values('"+user_id+"', '"+group_id+"','0',' ','"+String.valueOf((-1)*(i+1))+"')";
				  stmt.executeUpdate(query);
			  }
		  }
		  else {
			  query = "select min(total_point+0) as minimum from ent_point where user_id = '"+user_id+"' and group_id = '"+group_id+"'";
			  rs = stmt.executeQuery(query);
			  int minimum = 0;
			  while(rs.next()) {
				  minimum = rs.getInt("minimum");
			  }
			  String query2 = "update ent_point set recent_point = '"+recent_point+"' , description ='"+description+"' , total_point ='"+total_point+"'  where user_id = '"+user_id+"' and group_id='" + group_id + "' and total_point = '"+String.valueOf(minimum)+"'";
			  stmt.executeUpdate(query2);
		  }
		  this.releaseStatement(stmt, rs);
		  return true;
	  }catch(SQLException ex) {
		  System.out.println("SQLException: " + ex.getMessage());
	      System.out.println("SQLState: " + ex.getSQLState());
	      System.out.println("VendorError: " + ex.getErrorCode());
	      this.releaseStatement(stmt, rs);
	      return false;
	  }
  }
  public boolean insertRecentPointToHistoryTable(String user_id, String group_id, String recent_point, String description, String total_point, String content_name, String provider_id) {
	  try {
	      stmt =conn.createStatement();
	      String query = "insert into ent_point_history(user_id, group_id, recent_point, description, total_point, content_name, provider_id) values('"+user_id+"', '"+group_id+"','"+recent_point+"','"+description+"','"+total_point+"','"+content_name+"','"+provider_id+"');";
	      stmt.executeUpdate(query);
	      this.releaseStatement(stmt,rs);
	      return true;
	    } catch(SQLException ex){
	      System.out.println("SQLException: " + ex.getMessage());
	      System.out.println("SQLState: " + ex.getSQLState());
	      System.out.println("VendorError: " + ex.getErrorCode());

	      this.releaseStatement(stmt, rs);
	      return false;
	    }
  }
  
  //Register a change in the users preference regarding the GUI they are using for accessing the educational content
  public boolean updateUserPreference(String userId, String groupId, String sessionId, String appName, String parameterName, String parameterValue, String userContext) {
	  try {
	      stmt =conn.createStatement();
	      String query = "INSERT INTO ent_user_preferences(user_id, group_id, session_id, app_name, parameter_name, parameter_value, user_context, datetime) values('"+userId+"', '"+groupId+"','"+sessionId+"','"+appName+"','"+parameterName+"','"+parameterValue+"','"+userContext+"',CURRENT_TIMESTAMP(3));";
	      stmt.executeUpdate(query);
	      this.releaseStatement(stmt,rs);
	      return true;
	    } catch(SQLException ex){
	      System.out.println("SQLException: " + ex.getMessage());
	      System.out.println("SQLState: " + ex.getSQLState());
	      System.out.println("VendorError: " + ex.getErrorCode());

	      this.releaseStatement(stmt, rs);
	      return false;
	    }
  }
  
  public HashMap<String,String> getLastUserPreferences(String userId, String groupId, String appName) {
	  HashMap<String,String> userPreferences = new HashMap<String,String>();
	  try {
          stmt = conn.createStatement();
          //String query = "SELECT id, parameter_name, parameter_value, MAX(datetime) from ent_user_preferences  WHERE user_id = '"
          //        + userId + "' AND group_id='"+groupId+"' AND app_name='"+appName+"' GROUP BY id;";
          String query = "SELECT pref.* FROM (SELECT parameter_name,MAX(datetime) AS latest_date FROM ent_user_preferences WHERE user_id = '"+ userId + "' AND group_id='"+groupId+"' AND app_name='"+appName+"' GROUP BY parameter_name) latest "+
        		         "JOIN ent_user_preferences pref ON latest.parameter_name =pref.parameter_name "+
        		         "AND latest.latest_date =pref.datetime "+
        		         "WHERE user_id = '"+ userId + "' AND group_id='"+groupId+"' AND app_name='"+appName+"';";

          rs = stmt.executeQuery(query);
          while (rs.next()) {
              String parameterName = rs.getString("parameter_name");
              String parameterValue = rs.getString("parameter_value");
              userPreferences.put(parameterName, parameterValue);
          }
          this.releaseStatement(stmt, rs);
          
      } catch (SQLException ex) {
          this.releaseStatement(stmt, rs);
          System.out.println("SQLException: " + ex.getMessage());
          System.out.println("SQLState: " + ex.getSQLState());
          System.out.println("VendorError: " + ex.getErrorCode());
      } finally {
          this.releaseStatement(stmt, rs);
      }
	  return userPreferences;
  }

  public Map<String,Map<String,String>> getLastGroupPreferences(String groupId, String appName) {
	  Map<String,Map<String,String>> groupPreferences = new HashMap<String,Map<String,String>>();
	  try {
          stmt = conn.createStatement();
          
          String query = "SELECT pref.* FROM (SELECT user_id,parameter_name,MAX(datetime) AS latest_date FROM ent_user_preferences WHERE group_id='"+groupId+"' AND app_name='"+appName+"' GROUP BY user_id,parameter_name) latest "+
        		         "JOIN ent_user_preferences pref ON latest.user_id = pref.user_id AND latest.parameter_name =pref.parameter_name "+
        		         "AND latest.latest_date =pref.datetime "+
        		         "WHERE group_id='"+groupId+"' AND app_name='"+appName+"';";
          rs = stmt.executeQuery(query);
          while (rs.next()) {
        	  String userId = rs.getString("user_id");
              String parameterName = rs.getString("parameter_name");
              String parameterValue = rs.getString("parameter_value");
              
              groupPreferences.putIfAbsent(userId, new HashMap<String,String>());
              groupPreferences.get(userId).put(parameterName,parameterValue);
          }
          this.releaseStatement(stmt, rs);
          
      } catch (SQLException ex) {
          this.releaseStatement(stmt, rs);
          ex.printStackTrace();
      } finally {
          this.releaseStatement(stmt, rs);
      }
	  return groupPreferences;
  }
  
  
}
